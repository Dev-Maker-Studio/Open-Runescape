use crate::{cdp::Cdp, state};
use serde_json::{json, Value};

pub struct Page {
    cdp: Cdp,
    pub frames: i64,
}

pub fn expression(cmd: &str, args: &[String]) -> String {
    format!("window.openrs ? window.openrs.run({}, {}) : {{ ok: false, error: 'page bridge not loaded', frames: -1 }}", json!(cmd), json!(args))
}

pub fn unwrap(v: &Value) -> Result<Value, String> {
    match v["ok"].as_bool() {
        Some(true) => Ok(v["data"].clone()),
        _ => Err(v["error"].as_str().unwrap_or("unknown error").to_string()),
    }
}

impl Page {
    pub fn open() -> Result<Page, String> {
        let b = state::browser().ok_or("web client not running, start it with openrs web up")?;
        Ok(Page { cdp: Cdp::attach(&b.ws)?, frames: -1 })
    }

    pub fn at(ws: &str) -> Result<Page, String> {
        Ok(Page { cdp: Cdp::attach(ws)?, frames: -1 })
    }

    pub fn call(&mut self, method: &str, params: Value) -> Result<Value, String> {
        self.cdp.call(method, params)
    }

    pub fn run(&mut self, cmd: &str, args: &[String]) -> Result<Value, String> {
        let v = self.cdp.eval(&expression(cmd, args))?;
        self.frames = v["frames"].as_i64().unwrap_or(-1);
        unwrap(&v)
    }
}
