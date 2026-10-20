pub fn words(line: &str) -> Vec<String> {
    let (mut out, mut cur, mut quoted, mut any) = (Vec::new(), String::new(), false, false);
    for c in line.chars() {
        match c {
            '"' => {
                quoted = !quoted;
                any = true;
            }
            c if c.is_whitespace() && !quoted => {
                if any {
                    out.push(std::mem::take(&mut cur));
                    any = false;
                }
            }
            c => {
                cur.push(c);
                any = true;
            }
        }
    }
    if any {
        out.push(cur);
    }
    out
}

pub fn lines(text: &str) -> Vec<Vec<String>> {
    text.lines().filter(|l| !l.trim_start().starts_with('#')).map(words).filter(|w| !w.is_empty()).collect()
}

pub fn split(args: &[String]) -> Vec<Vec<String>> {
    args.split(|a| a == ";").filter(|c| !c.is_empty()).map(<[String]>::to_vec).collect()
}

pub fn load(args: &[String]) -> Result<Vec<Vec<String>>, String> {
    load_from(args, std::io::stdin())
}

pub fn load_from(args: &[String], mut stdin: impl std::io::Read) -> Result<Vec<Vec<String>>, String> {
    match args.first().map(String::as_str) {
        Some("-") => {
            let mut text = String::new();
            stdin.read_to_string(&mut text).map_err(|e| e.to_string())?;
            Ok(lines(&text))
        }
        Some(f) if f.starts_with('@') => Ok(lines(&std::fs::read_to_string(&f[1..]).map_err(|e| format!("{}: {e}", &f[1..]))?)),
        _ => Ok(split(args)),
    }
}
