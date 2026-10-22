use crate::{MAX_PAYLOAD_BYTES, session::SessionCiphers};
use openrs_protocol::registry::{
    ClientPacket, GamePacket, RegistryDecoder, RegistryEncoder, ServerPacket,
};
use openrs_protocol::{Error as ProtocolError, Feed};
use std::fmt;

pub const KEEPALIVE_IDLE_TICKS: u8 = 50;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum StreamState {
    Open,
    Closed,
    Failed,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum StreamError {
    Protocol(ProtocolError),
    Closed,
    Failed,
}

impl fmt::Display for StreamError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Protocol(error) => error.fmt(f),
            Self::Closed => f.write_str("lobby packet stream is closed"),
            Self::Failed => f.write_str("lobby packet stream cannot resume after an error"),
        }
    }
}

impl std::error::Error for StreamError {}

impl From<ProtocolError> for StreamError {
    fn from(error: ProtocolError) -> Self {
        Self::Protocol(error)
    }
}

struct Codecs {
    encoder: RegistryEncoder<ClientPacket>,
    decoder: RegistryDecoder<ServerPacket>,
}

pub struct LobbyStream {
    codecs: Option<Codecs>,
    state: StreamState,
    idle_ticks: u8,
    ping_pending: bool,
}

impl LobbyStream {
    pub fn new(ciphers: SessionCiphers, max_payload_bytes: usize) -> Result<Self, StreamError> {
        if !(1..=MAX_PAYLOAD_BYTES).contains(&max_payload_bytes) {
            return Err(ProtocolError::InvalidLimit.into());
        }
        Ok(Self {
            codecs: Some(Codecs {
                encoder: RegistryEncoder::new(Some(ciphers.outgoing), max_payload_bytes)?,
                decoder: RegistryDecoder::new(Some(ciphers.incoming), max_payload_bytes)?,
            }),
            state: StreamState::Open,
            idle_ticks: 0,
            ping_pending: false,
        })
    }

    pub fn state(&self) -> StreamState {
        self.state
    }

    pub fn ping_pending(&self) -> bool {
        self.ping_pending
    }

    pub fn encode(&mut self, opcode: ClientPacket, payload: &[u8]) -> Result<Vec<u8>, StreamError> {
        Ok(self.codecs()?.encoder.encode(opcode, payload)?)
    }

    pub fn request_world_list(&mut self, version: i32) -> Result<Vec<u8>, StreamError> {
        self.encode(ClientPacket::REQUEST_WORLD_LIST, &version.to_be_bytes())
    }

    pub fn tick(&mut self) -> Result<Option<Vec<u8>>, StreamError> {
        self.codecs()?;
        let idle_ticks = self.idle_ticks.saturating_add(1);
        if idle_ticks > KEEPALIVE_IDLE_TICKS && !self.ping_pending {
            let ping = self.encode(ClientPacket::PING, &[])?;
            self.idle_ticks = idle_ticks;
            self.ping_pending = true;
            Ok(Some(ping))
        } else {
            self.idle_ticks = idle_ticks;
            Ok(None)
        }
    }

    pub fn mark_flushed(&mut self) -> Result<(), StreamError> {
        self.codecs()?;
        self.idle_ticks = 0;
        self.ping_pending = false;
        Ok(())
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<Feed<GamePacket<ServerPacket>>, StreamError> {
        let result = self.codecs()?.decoder.feed(bytes);
        if result.is_err() {
            self.clear(StreamState::Failed);
        }
        result.map_err(StreamError::from)
    }

    pub fn finish(&mut self) -> Result<(), StreamError> {
        let result = self.codecs()?.decoder.finish();
        self.clear(if result.is_ok() {
            StreamState::Closed
        } else {
            StreamState::Failed
        });
        result.map_err(StreamError::from)
    }

    pub fn close(&mut self) {
        self.clear(StreamState::Closed);
    }

    fn codecs(&mut self) -> Result<&mut Codecs, StreamError> {
        match self.state {
            StreamState::Open => self.codecs.as_mut().ok_or(StreamError::Failed),
            StreamState::Closed => Err(StreamError::Closed),
            StreamState::Failed => Err(StreamError::Failed),
        }
    }

    fn clear(&mut self, state: StreamState) {
        self.codecs = None;
        self.state = state;
        self.idle_ticks = 0;
        self.ping_pending = false;
    }
}
