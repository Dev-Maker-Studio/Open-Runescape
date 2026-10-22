use std::path::{Path, PathBuf};

pub fn save(source: &Path, root: &Path) -> Result<Option<PathBuf>, String> {
    if !source.is_file() {
        return Ok(None);
    }
    let folder = root.join("code/harness/target/session-logs");
    std::fs::create_dir_all(&folder).map_err(|e| e.to_string())?;
    let time = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_err(|e| e.to_string())?.as_nanos();
    let target = folder.join(format!("native-{time}-{}.log", std::process::id()));
    std::fs::copy(source, &target).map_err(|e| e.to_string())?;
    Ok(Some(target))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn archives_survive_runtime_directory_removal() {
        let temp = std::env::temp_dir().join(format!("openrs-log-archive-{}", std::process::id()));
        let runtime = temp.join("runtime");
        let root = temp.join("project");
        std::fs::create_dir_all(&runtime).unwrap();
        let src = runtime.join("client.log");
        std::fs::write(&src, "numeric diagnostic\n").unwrap();
        let saved = save(&src, &root).unwrap().unwrap();
        let another = save(&src, &root).unwrap().unwrap();
        assert_ne!(saved, another);
        std::fs::remove_dir_all(&runtime).unwrap();
        assert_eq!(std::fs::read_to_string(saved).unwrap(), "numeric diagnostic\n");
        assert_eq!(save(&src, &root).unwrap(), None);
        std::fs::remove_dir_all(temp).unwrap();
    }
}
