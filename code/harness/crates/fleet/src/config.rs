use std::collections::HashMap;
use std::path::PathBuf;

pub struct Slot {
    pub n: usize,
    pub role: String,
    pub account: String,
    pub server: String,
    pub creds: String,
    pub start: Vec<i32>,
    pub heap: String,
}

pub const PER_INSTANCE: usize = 3;

impl Slot {
    pub fn instance(&self) -> Option<usize> {
        (self.server == "local").then(|| (self.n - 1) / PER_INSTANCE)
    }

    pub fn target(&self) -> String {
        self.instance().map_or_else(|| self.server.clone(), |i| format!("local{i}"))
    }
}

pub struct Cfg(HashMap<String, String>);

impl Cfg {
    pub fn load() -> Result<Cfg, String> {
        let root = display::root()?;
        let path = std::env::var_os("OPENRS_FLEET").map(PathBuf::from).unwrap_or_else(|| {
            let real = root.join("code/harness/fleet.toml");
            if real.exists() { real } else { root.join("code/harness/fleet.toml.example") }
        });
        std::fs::read_to_string(&path).map(|t| Cfg::parse(&t)).map_err(|e| format!("{}: {e}", path.display()))
    }

    pub fn parse(text: &str) -> Cfg {
        let (mut section, mut map) = (String::new(), HashMap::new());
        for line in text.lines() {
            let mut quoted = false;
            let line = line.split(|c| { quoted ^= c == '"'; c == '#' && !quoted }).next().unwrap_or("").trim();
            if let Some(s) = line.strip_prefix('[').and_then(|s| s.strip_suffix(']')) {
                section = s.into();
            } else if let Some((k, v)) = line.split_once('=') {
                map.insert(format!("{section}.{}", k.trim()), v.trim().trim_matches('"').to_string());
            }
        }
        Cfg(map)
    }

    pub fn get(&self, key: &str, default: &str) -> String {
        self.0.get(key).cloned().unwrap_or_else(|| default.into())
    }

    pub fn slot(&self, n: usize) -> Result<Slot, String> {
        let k = |f: &str| format!("slot.{n}.{f}");
        let get = |f: &str| self.0.get(&k(f)).or_else(|| self.0.get(&format!("defaults.{f}"))).cloned();
        let need = |f: &str| get(f).ok_or_else(|| format!("slot {n}: missing {f}"));
        Ok(Slot {
            n,
            server: get("server").unwrap_or_else(|| "local".into()),
            role: need("role")?,
            account: need("account")?,
            creds: get("credentials").unwrap_or_else(|| "local".into()),
            start: need("start")?.split_whitespace().filter_map(|v| v.parse().ok()).collect(),
            heap: get("heap").unwrap_or_else(|| "512m".into()),
        })
    }

    pub fn server(&self, name: &str) -> Result<(String, String, String), String> {
        let g = |f: &str| self.0.get(&format!("server.{name}.{f}")).cloned().ok_or_else(|| format!("server {name}: missing {f}"));
        Ok((g("host")?, g("lobby")?, g("world")?))
    }

    pub fn endpoint(&self, slot: &Slot) -> Result<(String, String, String), String> {
        let (host, lobby, world) = self.server(&slot.server)?;
        let Some(i) = slot.instance() else { return Ok((host, lobby, world)) };
        let shift = |p: String| p.parse::<u16>().map(|v| (v + playerstore::STRIDE * i as u16).to_string()).map_err(|_| format!("server local: bad port {p}"));
        Ok((host, shift(lobby)?, shift(world)?))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn example() -> Cfg {
        Cfg::parse(include_str!("../../../fleet.toml.example"))
    }

    #[test]
    fn local_slots_map_to_instances_three_at_a_time() {
        let cfg = example();
        let got: Vec<(Option<usize>, String)> = (1..=9).map(|n| cfg.slot(n).unwrap()).map(|s| (s.instance(), s.target())).collect();
        let want: Vec<(Option<usize>, String)> = [0, 0, 0, 1, 1, 1, 2, 2, 2].into_iter().map(|i| (Some(i), format!("local{i}"))).collect();
        assert_eq!(got, want);
    }

    #[test]
    fn local_endpoints_shift_per_instance() {
        let cfg = example();
        let ports = |n| { let (_, l, w) = cfg.endpoint(&cfg.slot(n).unwrap()).unwrap(); (l, w) };
        assert_eq!(ports(3), ("44594".into(), "44595".into()));
        assert_eq!(ports(4), ("44604".into(), "44605".into()));
        assert_eq!(ports(9), ("44614".into(), "44615".into()));
    }

    #[test]
    fn explicit_servers_and_overrides_are_kept() {
        let cfg = Cfg::parse("[defaults]\nheap = \"256m\" # small\n[server.shared]\nhost = \"h\"\nlobby = \"1\"\nworld = \"2\"\n[slot.7]\nrole = \"water\"\naccount = \"me\"\nserver = \"shared\"\ncredentials = \"env:P\"\nstart = \"1 2 0\"\nheap = \"1g\"\n");
        let s = cfg.slot(7).unwrap();
        assert_eq!((s.instance(), s.target(), s.heap.as_str(), s.creds.as_str()), (None, "shared".to_string(), "1g", "env:P"));
        assert_eq!(cfg.endpoint(&s).unwrap(), ("h".into(), "1".into(), "2".into()));
        assert!(cfg.slot(1).is_err());
    }
}
