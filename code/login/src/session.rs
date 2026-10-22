use crate::response::{
    LoginKind, LoginResponse, LoginResponseDecoder, MAX_SUCCESS_BYTES, ResponseFeed,
};
use crate::{
    Error, LobbyRequest, SensitiveBytes, WorldRequest, accept_handshake, buffer,
    encode_lobby_request, encode_world_request, handshake_bytes, isaac_ciphers,
};
use openrs_protocol::isaac::Isaac;
use std::fmt;

const MAX_RESPONSE_EVENTS: usize = 64;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Phase {
    SendHandshake,
    AwaitHandshake,
    SendRequest,
    AwaitResponse,
    AwaitConfirmation,
    SendConfirmation,
    Accepted,
    Reconnect,
    Retry,
    Queued,
    Rejected,
    Closed,
    Failed,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SessionError {
    Codec(Error),
    InvalidPhase,
    ResponseEventLimit,
}

impl fmt::Display for SessionError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Codec(error) => error.fmt(f),
            Self::InvalidPhase => f.write_str("invalid login attempt phase"),
            Self::ResponseEventLimit => f.write_str("login response event limit"),
        }
    }
}

impl std::error::Error for SessionError {}

impl From<Error> for SessionError {
    fn from(error: Error) -> Self {
        Self::Codec(error)
    }
}

pub struct SessionCiphers {
    pub outgoing: Isaac,
    pub incoming: Isaac,
}

pub struct LoginAttempt {
    kind: LoginKind,
    phase: Phase,
    request: Option<SensitiveBytes>,
    decoder: LoginResponseDecoder,
    ciphers: Option<SessionCiphers>,
    events: usize,
}

impl LoginAttempt {
    pub fn lobby(request: &LobbyRequest<'_>) -> Result<Self, SessionError> {
        let encoded = encode_lobby_request(request)?;
        Self::new(LoginKind::Lobby, encoded.wire_bytes(), request.seed)
    }

    pub fn world(request: &WorldRequest<'_>) -> Result<Self, SessionError> {
        let encoded = encode_world_request(request)?;
        Self::new(LoginKind::World, encoded.wire_bytes(), request.seed)
    }

    fn new(kind: LoginKind, wire: &[u8], seed: &[u32; 4]) -> Result<Self, SessionError> {
        let mut bytes = buffer(wire.len())?;
        bytes.extend_from_slice(wire);
        let (outgoing, incoming) = isaac_ciphers(seed)?;
        Ok(Self {
            kind,
            phase: Phase::SendHandshake,
            request: Some(SensitiveBytes(bytes)),
            decoder: LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES)?,
            ciphers: Some(SessionCiphers { outgoing, incoming }),
            events: 0,
        })
    }

    pub fn phase(&self) -> Phase {
        self.phase
    }

    pub fn confirm(&mut self) -> Result<(), SessionError> {
        if self.phase != Phase::AwaitConfirmation {
            return Err(SessionError::InvalidPhase);
        }
        self.phase = Phase::SendConfirmation;
        Ok(())
    }

    pub fn take_outgoing(&mut self) -> Result<Option<SensitiveBytes>, SessionError> {
        match self.phase {
            Phase::SendHandshake => {
                let mut bytes = buffer(1)?;
                bytes.extend_from_slice(&handshake_bytes());
                self.phase = Phase::AwaitHandshake;
                Ok(Some(SensitiveBytes(bytes)))
            }
            Phase::SendRequest => {
                self.phase = Phase::AwaitResponse;
                Ok(self.request.take())
            }
            Phase::SendConfirmation => {
                let mut bytes = buffer(1)?;
                let decoder = LoginResponseDecoder::new(self.kind, MAX_SUCCESS_BYTES)?;
                let ciphers = self.ciphers.as_mut().ok_or(SessionError::InvalidPhase)?;
                bytes.push(26_u8.wrapping_add(ciphers.outgoing.next_u32() as u8));
                self.decoder = decoder;
                self.phase = Phase::AwaitResponse;
                Ok(Some(SensitiveBytes(bytes)))
            }
            Phase::Failed | Phase::Closed => Err(SessionError::InvalidPhase),
            _ => Ok(None),
        }
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<ResponseFeed, SessionError> {
        let result = self.feed_inner(bytes);
        if result.is_err() {
            self.fail();
        }
        result
    }

    fn feed_inner(&mut self, bytes: &[u8]) -> Result<ResponseFeed, SessionError> {
        match self.phase {
            Phase::AwaitHandshake => {
                if let Some(&response) = bytes.first() {
                    accept_handshake(response)?;
                    self.phase = Phase::SendRequest;
                    Ok(ResponseFeed {
                        consumed: 1,
                        event: None,
                    })
                } else {
                    Ok(ResponseFeed {
                        consumed: 0,
                        event: None,
                    })
                }
            }
            Phase::AwaitResponse => {
                let result = self.decoder.feed(bytes)?;
                if let Some(event) = &result.event {
                    self.events += 1;
                    if self.events > MAX_RESPONSE_EVENTS {
                        return Err(SessionError::ResponseEventLimit);
                    }
                    self.phase = match event {
                        LoginResponse::LobbySuccess(_) | LoginResponse::WorldSuccess(_) => {
                            Phase::Accepted
                        }
                        LoginResponse::TimeSync { .. } => Phase::AwaitResponse,
                        LoginResponse::ConfirmationRequired => Phase::AwaitConfirmation,
                        LoginResponse::ReconnectRequired => Phase::Reconnect,
                        LoginResponse::RetryRequested => Phase::Retry,
                        LoginResponse::Queue { .. } => Phase::Queued,
                        _ => Phase::Rejected,
                    };
                    if matches!(
                        self.phase,
                        Phase::Rejected | Phase::Retry | Phase::Queued | Phase::Reconnect
                    ) {
                        self.ciphers = None;
                    }
                }
                Ok(result)
            }
            _ => Err(SessionError::InvalidPhase),
        }
    }

    pub fn take_ciphers(&mut self) -> Result<SessionCiphers, SessionError> {
        if self.phase != Phase::Accepted {
            return Err(SessionError::InvalidPhase);
        }
        let ciphers = self.ciphers.take().ok_or(SessionError::InvalidPhase)?;
        self.phase = Phase::Closed;
        Ok(ciphers)
    }

    pub fn finish(&mut self) -> Result<(), SessionError> {
        if matches!(
            self.phase,
            Phase::Accepted
                | Phase::Rejected
                | Phase::Retry
                | Phase::Queued
                | Phase::Reconnect
                | Phase::Closed
        ) {
            return Ok(());
        }
        self.fail();
        Err(SessionError::Codec(Error::UnexpectedEof))
    }

    pub fn abort(&mut self) {
        let _ = self.decoder.finish();
        self.request = None;
        self.ciphers = None;
        self.phase = Phase::Closed;
    }

    fn fail(&mut self) {
        let _ = self.decoder.finish();
        self.request = None;
        self.ciphers = None;
        self.phase = Phase::Failed;
    }
}
