use openrs_login::{REVISION, SUB_REVISION, accept_handshake, handshake_bytes};
use serde::Serialize;

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GameHandshakeReport {
    pub revision: u32,
    pub subrevision: u32,
    pub response_code: u8,
    pub accepted: bool,
    pub authenticated: bool,
    pub scope: &'static str,
}

#[derive(PartialEq, Eq)]
enum Phase {
    Send,
    Await,
    Complete,
    Failed,
}

pub struct GameHandshakeProbe {
    phase: Phase,
    report: Option<GameHandshakeReport>,
}

impl Default for GameHandshakeProbe {
    fn default() -> Self {
        Self::new()
    }
}

impl GameHandshakeProbe {
    pub fn new() -> Self {
        Self {
            phase: Phase::Send,
            report: None,
        }
    }

    pub fn take_outgoing(&mut self) -> Result<Vec<u8>, String> {
        match self.phase {
            Phase::Send => {
                self.phase = Phase::Await;
                Ok(handshake_bytes().to_vec())
            }
            Phase::Failed => Err("game handshake probe has failed".into()),
            _ => Ok(Vec::new()),
        }
    }

    pub fn complete(&self) -> bool {
        self.phase == Phase::Complete
    }

    pub fn report(&self) -> Option<&GameHandshakeReport> {
        self.report.as_ref().filter(|_| self.complete())
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        if self.phase != Phase::Await || bytes.len() > 1 {
            self.fail();
            return Err("invalid game handshake response boundary".into());
        }
        if let Some(&response) = bytes.first() {
            if accept_handshake(response).is_err() {
                self.fail();
                return Err(format!("game handshake rejected: {response}"));
            }
            self.report = Some(GameHandshakeReport {
                revision: REVISION,
                subrevision: SUB_REVISION,
                response_code: response,
                accepted: true,
                authenticated: false,
                scope: "original-login-handshake-only",
            });
            self.phase = Phase::Complete;
        }
        Ok(())
    }

    pub fn finish(&mut self) -> Result<(), String> {
        if self.complete() {
            return Ok(());
        }
        self.fail();
        Err("connection ended before the game handshake response".into())
    }

    fn fail(&mut self) {
        self.phase = Phase::Failed;
        self.report = None;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn handshake_is_one_byte_and_does_not_authenticate() {
        let mut probe = GameHandshakeProbe::new();
        assert_eq!(probe.take_outgoing().unwrap(), [14]);
        assert!(probe.take_outgoing().unwrap().is_empty());
        probe.feed(&[]).unwrap();
        assert!(!probe.complete());
        probe.feed(&[0]).unwrap();
        let report = probe.report().unwrap();
        assert!(report.accepted);
        assert!(!report.authenticated);
        assert_eq!(report.revision, 727);
        assert_eq!(report.subrevision, 1);
        assert_eq!(report.response_code, 0);
        assert_eq!(report.scope, "original-login-handshake-only");
        assert!(probe.take_outgoing().unwrap().is_empty());
        probe.finish().unwrap();
    }

    #[test]
    fn every_rejected_status_is_terminal() {
        for code in 1..=255 {
            let mut probe = GameHandshakeProbe::default();
            probe.take_outgoing().unwrap();
            assert!(probe.feed(&[code]).is_err());
            assert!(!probe.complete());
            assert!(probe.report().is_none());
            assert!(probe.take_outgoing().is_err());
            assert!(probe.feed(&[0]).is_err());
        }
    }

    #[test]
    fn truncation_and_unsolicited_bytes_are_terminal() {
        for outgoing in [false, true] {
            let mut probe = GameHandshakeProbe::new();
            if outgoing {
                probe.take_outgoing().unwrap();
            }
            assert!(probe.finish().is_err());
            assert!(probe.feed(&[0]).is_err());
        }
        for bytes in [&[0][..], &[0, 1][..]] {
            let mut probe = GameHandshakeProbe::new();
            assert!(probe.feed(bytes).is_err());
            assert!(probe.report().is_none());
        }
        let mut probe = GameHandshakeProbe::new();
        probe.take_outgoing().unwrap();
        assert!(probe.feed(&[0, 1]).is_err());
        assert!(probe.report().is_none());
    }

    #[test]
    fn bytes_after_completion_invalidate_the_report() {
        let mut probe = GameHandshakeProbe::new();
        probe.take_outgoing().unwrap();
        probe.feed(&[0]).unwrap();
        assert!(probe.feed(&[0]).is_err());
        assert!(!probe.complete());
        assert!(probe.report().is_none());
    }
}
