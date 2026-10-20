use serde_json::{json, Value};
use std::net::TcpStream;
use std::time::Duration;
use tungstenite::protocol::WebSocketConfig;
use tungstenite::{Message, WebSocket};

pub fn request(id: u64, method: &str, params: Value, session: Option<&str>) -> String {
    let mut v = json!({ "id": id, "method": method, "params": params });
    if let Some(s) = session {
        v["sessionId"] = s.into();
    }
    v.to_string()
}

pub fn reply(text: &str, id: u64) -> Option<Result<Value, String>> {
    let v: Value = serde_json::from_str(text).ok()?;
    (v["id"].as_u64() == Some(id)).then(|| match v.get("error") {
        Some(e) => Err(e["message"].as_str().unwrap_or("devtools error").to_string()),
        None => Ok(v["result"].clone()),
    })
}

pub fn evaluated(result: Value) -> Result<Value, String> {
    match result.get("exceptionDetails") {
        Some(e) => Err(e["exception"]["description"].as_str().or(e["text"].as_str()).unwrap_or("exception").to_string()),
        None => Ok(result["result"]["value"].clone()),
    }
}

pub fn page_target(targets: &Value) -> Option<String> {
    targets["targetInfos"].as_array()?.iter().find(|t| t["type"] == "page" && t["url"].as_str().is_some_and(|u| u.starts_with("http")))?["targetId"].as_str().map(String::from)
}

pub struct Cdp {
    ws: WebSocket<TcpStream>,
    id: u64,
    session: Option<String>,
}

impl Cdp {
    pub fn connect(url: &str) -> Result<Cdp, String> {
        let addr = url.strip_prefix("ws://").and_then(|r| r.split('/').next()).ok_or_else(|| format!("bad devtools url {url}"))?;
        let stream = TcpStream::connect(addr).map_err(|e| format!("devtools {addr}: {e}"))?;
        let _ = stream.set_nodelay(true);
        let _ = stream.set_read_timeout(Some(Duration::from_secs(300)));
        let config = WebSocketConfig::default().max_message_size(None).max_frame_size(None);
        let (ws, _) = tungstenite::client::client_with_config(url, stream, Some(config)).map_err(|e| format!("devtools handshake: {e}"))?;
        Ok(Cdp { ws, id: 0, session: None })
    }

    pub fn attach(url: &str) -> Result<Cdp, String> {
        let mut c = Cdp::connect(url)?;
        let targets = c.call("Target.getTargets", json!({}))?;
        let id = page_target(&targets).ok_or("no page target in the browser")?;
        let r = c.call("Target.attachToTarget", json!({ "targetId": id, "flatten": true }))?;
        c.session = r["sessionId"].as_str().map(String::from);
        Ok(c)
    }

    pub fn call(&mut self, method: &str, params: Value) -> Result<Value, String> {
        self.id += 1;
        let text = request(self.id, method, params, self.session.as_deref());
        self.ws.send(Message::text(text)).map_err(|e| e.to_string())?;
        loop {
            match self.ws.read().map_err(|e| format!("devtools: {e}"))? {
                Message::Text(t) => {
                    if let Some(r) = reply(t.as_str(), self.id) {
                        return r;
                    }
                }
                Message::Close(_) => return Err("devtools connection closed".into()),
                _ => {}
            }
        }
    }

    pub fn eval(&mut self, expression: &str) -> Result<Value, String> {
        evaluated(self.call("Runtime.evaluate", json!({ "expression": expression, "awaitPromise": true, "returnByValue": true }))?)
    }
}
