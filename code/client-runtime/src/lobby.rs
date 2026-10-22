use openrs_login::lobby_stream::LobbyStream;
use openrs_login::response::LoginResponse;
use openrs_login::session::{LoginAttempt, Phase};
use openrs_login::world_list::{WorldList, WorldListDecoder};
use openrs_login::{LobbyIdentity, LobbyRequest, SensitiveString};
use openrs_protocol::registry::ServerPacket;
use serde::de::{IgnoredAny, SeqAccess, Visitor};
use serde::{Deserialize, Deserializer, Serialize};
use std::fmt;
use std::marker::PhantomData;
use zeroize::{Zeroize, Zeroizing};

pub(crate) const MAX_CONFIG_BYTES: usize = 64 * 1024;
pub(crate) const MAX_FEED_BYTES: usize = 64 * 1024;
pub(crate) const MAX_INCOMING_BYTES: usize = 1024 * 1024;
pub(crate) const MAX_EVENTS: usize = 4096;
pub(crate) const MAX_OPERATIONS: usize = 65536;
const MAX_LIFETIME_SECONDS: u64 = 300;

pub(crate) struct SecretArray<T: Zeroize, const N: usize>(pub(crate) Zeroizing<[T; N]>);

impl<'de, T, const N: usize> Deserialize<'de> for SecretArray<T, N>
where
    T: Deserialize<'de> + Zeroize + Default + Copy,
{
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        struct ArrayVisitor<T, const N: usize>(PhantomData<T>);

        impl<'de, T, const N: usize> Visitor<'de> for ArrayVisitor<T, N>
        where
            T: Deserialize<'de> + Zeroize + Default + Copy,
        {
            type Value = SecretArray<T, N>;

            fn expecting(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
                formatter.write_str("an exact-length unsigned integer array")
            }

            fn visit_seq<A: SeqAccess<'de>>(
                self,
                mut sequence: A,
            ) -> Result<Self::Value, A::Error> {
                let mut values = Zeroizing::new([T::default(); N]);
                for value in values.iter_mut() {
                    *value = sequence
                        .next_element()?
                        .ok_or_else(|| serde::de::Error::custom("invalid array length"))?;
                }
                if sequence.next_element::<IgnoredAny>()?.is_some() {
                    return Err(serde::de::Error::custom("invalid array length"));
                }
                Ok(SecretArray(values))
            }
        }

        deserializer.deserialize_seq(ArrayVisitor(PhantomData))
    }
}

impl SecretArray<u32, 2> {
    pub(crate) fn word(&self) -> u64 {
        (u64::from(self.0[0]) << 32) | u64::from(self.0[1])
    }
}

#[derive(Deserialize)]
#[serde(tag = "kind", rename_all = "camelCase", deny_unknown_fields)]
pub(crate) enum ConfigIdentity {
    Username { username: Zeroizing<String> },
    Token { token: SecretArray<u32, 2> },
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LobbyConfig {
    identity: ConfigIdentity,
    password: Zeroizing<String>,
    seed: SecretArray<u32, 4>,
    social_nonce: SecretArray<u32, 2>,
    client_random_seed: SecretArray<u32, 2>,
    game_id: u8,
    language: u8,
    random24: SecretArray<u8, 24>,
    affiliate_p: Zeroizing<String>,
    preferences: SecretArray<u8, 35>,
    login_token: Zeroizing<String>,
    affiliate: i32,
    session_token: Zeroizing<i32>,
    js5_auth_token: Zeroizing<String>,
    index_crcs: SecretArray<u32, 36>,
}

impl LobbyConfig {
    pub fn from_json(json: &str) -> Result<Self, &'static str> {
        if json.len() > MAX_CONFIG_BYTES {
            return Err("lobby configuration byte limit");
        }
        serde_json::from_str(json).map_err(|_| "invalid lobby configuration")
    }

    fn attempt(&self) -> Result<LoginAttempt, &'static str> {
        let identity = match &self.identity {
            ConfigIdentity::Username { username } => LobbyIdentity::Username(username),
            ConfigIdentity::Token { token } => LobbyIdentity::Token(token.word()),
        };
        let strings = [
            match &self.identity {
                ConfigIdentity::Username { username } => username.len(),
                ConfigIdentity::Token { .. } => 0,
            },
            self.password.len(),
            self.affiliate_p.len(),
            self.login_token.len(),
            self.js5_auth_token.len(),
        ];
        if strings
            .into_iter()
            .try_fold(0usize, usize::checked_add)
            .is_none_or(|n| n > MAX_CONFIG_BYTES)
        {
            return Err("lobby configuration byte limit");
        }
        LoginAttempt::lobby(&LobbyRequest {
            identity,
            password: &self.password,
            seed: &self.seed.0,
            social_nonce: self.social_nonce.word(),
            client_random_seed: self.client_random_seed.word(),
            game_id: self.game_id,
            language: self.language,
            random24: &self.random24.0,
            affiliate_p: &self.affiliate_p,
            preferences: &self.preferences.0,
            login_token: &self.login_token,
            affiliate: self.affiliate,
            session_token: *self.session_token,
            js5_auth_token: &self.js5_auth_token,
            index_crcs: &self.index_crcs.0,
        })
        .map_err(|_| "invalid lobby request configuration")
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum LobbyPhase {
    SendHandshake,
    AwaitHandshake,
    SendRequest,
    AwaitResponse,
    AwaitConfirmation,
    SendConfirmation,
    Lobby,
    Reconnect,
    Retry,
    Queued,
    Rejected,
    Closed,
    Failed,
}

impl LobbyPhase {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::SendHandshake => "sendHandshake",
            Self::AwaitHandshake => "awaitHandshake",
            Self::SendRequest => "sendRequest",
            Self::AwaitResponse => "awaitResponse",
            Self::AwaitConfirmation => "awaitConfirmation",
            Self::SendConfirmation => "sendConfirmation",
            Self::Lobby => "lobby",
            Self::Reconnect => "reconnect",
            Self::Retry => "retry",
            Self::Queued => "queued",
            Self::Rejected => "rejected",
            Self::Closed => "closed",
            Self::Failed => "failed",
        }
    }
}

impl From<Phase> for LobbyPhase {
    fn from(phase: Phase) -> Self {
        match phase {
            Phase::SendHandshake => Self::SendHandshake,
            Phase::AwaitHandshake => Self::AwaitHandshake,
            Phase::SendRequest => Self::SendRequest,
            Phase::AwaitResponse => Self::AwaitResponse,
            Phase::AwaitConfirmation => Self::AwaitConfirmation,
            Phase::SendConfirmation => Self::SendConfirmation,
            Phase::Accepted => Self::Lobby,
            Phase::Reconnect => Self::Reconnect,
            Phase::Retry => Self::Retry,
            Phase::Queued => Self::Queued,
            Phase::Rejected => Self::Rejected,
            Phase::Closed => Self::Closed,
            Phase::Failed => Self::Failed,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
pub enum LobbyEvent {
    LoginAccepted,
    ConfirmationRequired,
    TimeSync,
    ReconnectRequired,
    RetryRequested,
    Queued,
    Rejected {
        #[serde(rename = "responseCode")]
        response_code: u8,
    },
    WorldListPending,
    WorldListUpdated,
    OpaquePacket {
        opcode: u16,
        name: &'static str,
        #[serde(rename = "payloadBytes")]
        payload_bytes: usize,
    },
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LobbyFeed {
    pub consumed: usize,
    pub event: Option<LobbyEvent>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LobbyLocationReport {
    pub country_id: u16,
    pub activity: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LobbyWorldReport {
    pub raw_id: u16,
    pub world_number: u16,
    pub location_index: u8,
    pub flags: i32,
    pub activity: String,
    pub host: String,
    pub port: Option<i32>,
    pub player_count: i32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LobbyWorldListReport {
    pub version: Option<i32>,
    pub locations: Vec<LobbyLocationReport>,
    pub start: u16,
    pub size_plus_one: u16,
    pub descriptor_slots: usize,
    pub worlds: Vec<LobbyWorldReport>,
}

impl From<&WorldList> for LobbyWorldListReport {
    fn from(list: &WorldList) -> Self {
        Self {
            version: list.version,
            locations: list
                .locations
                .iter()
                .map(|location| LobbyLocationReport {
                    country_id: location.country_id,
                    activity: location.activity.clone(),
                })
                .collect(),
            start: list.start,
            size_plus_one: list.size_plus_one,
            descriptor_slots: list.descriptor_slots,
            worlds: list
                .worlds
                .iter()
                .map(|world| LobbyWorldReport {
                    raw_id: world.raw_id,
                    world_number: world.world_number,
                    location_index: world.location_index,
                    flags: world.flags,
                    activity: world.activity.clone(),
                    host: world.host.clone(),
                    port: world.port,
                    player_count: world.player_count,
                })
                .collect(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LobbyReport {
    pub phase: LobbyPhase,
    pub accepted: bool,
    pub world_list: Option<LobbyWorldListReport>,
}

pub(crate) struct Lifetime {
    #[cfg(not(target_arch = "wasm32"))]
    started: std::time::Instant,
    #[cfg(target_arch = "wasm32")]
    started: f64,
}

impl Lifetime {
    pub(crate) fn new() -> Result<Self, &'static str> {
        Ok(Self {
            #[cfg(not(target_arch = "wasm32"))]
            started: std::time::Instant::now(),
            #[cfg(target_arch = "wasm32")]
            started: browser::now().ok_or("lobby monotonic clock unavailable")?,
        })
    }

    pub(crate) fn elapsed_ms(&self) -> Option<u64> {
        #[cfg(not(target_arch = "wasm32"))]
        {
            self.started.elapsed().as_millis().try_into().ok()
        }
        #[cfg(target_arch = "wasm32")]
        {
            let elapsed = browser::now()? - self.started;
            (elapsed.is_finite() && (0.0..u64::MAX as f64).contains(&elapsed))
                .then_some(elapsed as u64)
        }
    }

    pub(crate) fn expired(&self) -> bool {
        self.elapsed_ms()
            .is_none_or(|elapsed| elapsed >= MAX_LIFETIME_SECONDS * 1000)
    }
}

pub struct LobbySession {
    attempt: Option<LoginAttempt>,
    stream: Option<LobbyStream>,
    world_token: Option<SensitiveString>,
    default_world_id: Option<u16>,
    worlds: WorldListDecoder,
    pending: Zeroizing<Vec<u8>>,
    terminal: Option<LobbyPhase>,
    incoming_bytes: usize,
    events: usize,
    operations: usize,
    lifetime: Lifetime,
}

impl LobbySession {
    pub fn new(config: LobbyConfig) -> Result<Self, &'static str> {
        let attempt = config.attempt()?;
        Ok(Self {
            attempt: Some(attempt),
            stream: None,
            world_token: None,
            default_world_id: None,
            worlds: WorldListDecoder::new(),
            pending: Zeroizing::new(Vec::new()),
            terminal: None,
            incoming_bytes: 0,
            events: 0,
            operations: 0,
            lifetime: Lifetime::new()?,
        })
    }

    pub fn from_json(json: &str) -> Result<Self, &'static str> {
        Self::new(LobbyConfig::from_json(json)?)
    }

    pub fn phase(&mut self) -> LobbyPhase {
        self.expire();
        self.current_phase()
    }

    pub fn report(&mut self) -> LobbyReport {
        self.expire();
        LobbyReport {
            phase: self.current_phase(),
            accepted: self.stream.is_some(),
            world_list: self.worlds.snapshot().map(LobbyWorldListReport::from),
        }
    }

    pub fn take_outgoing(&mut self) -> Result<Zeroizing<Vec<u8>>, &'static str> {
        self.guard()?;
        if !self.pending.is_empty() {
            return Ok(std::mem::replace(
                &mut self.pending,
                Zeroizing::new(Vec::new()),
            ));
        }
        match self.attempt.as_mut() {
            Some(attempt) => match attempt.take_outgoing() {
                Ok(Some(bytes)) => Ok(Zeroizing::new(bytes.as_bytes().to_vec())),
                Ok(None) => Ok(Zeroizing::new(Vec::new())),
                Err(_) => self.fail("invalid lobby outgoing phase"),
            },
            None => Ok(Zeroizing::new(Vec::new())),
        }
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<LobbyFeed, &'static str> {
        self.guard()?;
        if bytes.len() > MAX_FEED_BYTES {
            return self.fail("lobby incoming chunk byte limit");
        }
        if bytes.len() > MAX_INCOMING_BYTES - self.incoming_bytes {
            return self.fail("lobby incoming total byte limit");
        }
        let result = self.feed_inner(bytes);
        let feed = match result {
            Ok(feed) => feed,
            Err(error) => return self.fail(error),
        };
        self.incoming_bytes += feed.consumed;
        if feed.event.is_some() {
            self.events += 1;
            if self.events > MAX_EVENTS {
                return self.fail("lobby packet event limit");
            }
        }
        Ok(feed)
    }

    fn feed_inner(&mut self, bytes: &[u8]) -> Result<LobbyFeed, &'static str> {
        if let Some(stream) = self.stream.as_mut() {
            let feed = stream.feed(bytes).map_err(|_| "invalid lobby packet")?;
            let event = if let Some(packet) = feed.item {
                let payload = Zeroizing::new(packet.payload);
                Some(if packet.opcode == ServerPacket::WORLD_LIST {
                    if self
                        .worlds
                        .feed(&payload)
                        .map_err(|_| "invalid lobby world list")?
                        .is_some()
                    {
                        LobbyEvent::WorldListUpdated
                    } else {
                        LobbyEvent::WorldListPending
                    }
                } else {
                    LobbyEvent::OpaquePacket {
                        opcode: packet.opcode.wire(),
                        name: packet.opcode.metadata().name,
                        payload_bytes: payload.len(),
                    }
                })
            } else {
                None
            };
            return Ok(LobbyFeed {
                consumed: feed.consumed,
                event,
            });
        }
        let attempt = self.attempt.as_mut().ok_or("invalid lobby receive phase")?;
        let feed = attempt
            .feed(bytes)
            .map_err(|_| "invalid lobby login response")?;
        let event = match feed.event {
            Some(LoginResponse::LobbySuccess(success)) => {
                let ciphers = attempt
                    .take_ciphers()
                    .map_err(|_| "invalid lobby cipher handoff")?;
                self.stream = Some(
                    LobbyStream::new(ciphers, openrs_login::MAX_PAYLOAD_BYTES)
                        .map_err(|_| "invalid lobby stream configuration")?,
                );
                self.world_token = Some(success.world_login_token);
                self.default_world_id = success.default_world_id;
                self.attempt = None;
                Some(LobbyEvent::LoginAccepted)
            }
            Some(LoginResponse::ConfirmationRequired) => Some(LobbyEvent::ConfirmationRequired),
            Some(LoginResponse::TimeSync { .. }) => Some(LobbyEvent::TimeSync),
            Some(LoginResponse::ReconnectRequired) => {
                self.clear(LobbyPhase::Reconnect);
                Some(LobbyEvent::ReconnectRequired)
            }
            Some(LoginResponse::RetryRequested) => {
                self.clear(LobbyPhase::Retry);
                Some(LobbyEvent::RetryRequested)
            }
            Some(LoginResponse::Queue { .. }) => {
                self.clear(LobbyPhase::Queued);
                Some(LobbyEvent::Queued)
            }
            Some(
                LoginResponse::Rejected { response_code }
                | LoginResponse::RejectedDetail { response_code, .. },
            ) => {
                self.clear(LobbyPhase::Rejected);
                Some(LobbyEvent::Rejected { response_code })
            }
            Some(LoginResponse::WorldSuccess(_)) => return Err("unexpected world login response"),
            None => None,
        };
        Ok(LobbyFeed {
            consumed: feed.consumed,
            event,
        })
    }

    pub fn start_world(
        &mut self,
        mut config: crate::world_session::WorldConfig,
    ) -> Result<crate::world_session::WorldSession, &'static str> {
        self.guard()?;
        if self.stream.is_none() {
            return Err("world login requires an accepted lobby session");
        }
        let token = self
            .world_token
            .as_ref()
            .ok_or("lobby world token unavailable")?;
        config.set_lobby_default_world(self.default_world_id);
        crate::world_session::WorldSession::from_lobby(config, token)
    }

    pub fn start_world_state(
        &mut self,
        mut config: crate::world_session::WorldConfig,
        varp_count: usize,
    ) -> Result<crate::world_session::WorldSession, &'static str> {
        self.guard()?;
        if self.stream.is_none() {
            return Err("world login requires an accepted lobby session");
        }
        let token = self
            .world_token
            .as_ref()
            .ok_or("lobby world token unavailable")?;
        config.set_lobby_default_world(self.default_world_id);
        crate::world_session::WorldSession::from_lobby_with_vars(config, token, varp_count)
    }

    pub fn confirm(&mut self) -> Result<(), &'static str> {
        self.guard()?;
        if self
            .attempt
            .as_mut()
            .is_none_or(|attempt| attempt.confirm().is_err())
        {
            return self.fail("invalid lobby confirmation phase");
        }
        Ok(())
    }

    pub fn request_world_list(&mut self, version: i32) -> Result<(), &'static str> {
        self.guard()?;
        if !self.pending.is_empty() {
            return self.fail("lobby outgoing buffer must be drained");
        }
        let Some(stream) = self.stream.as_mut() else {
            return self.fail("invalid world list request phase");
        };
        match stream.request_world_list(version) {
            Ok(bytes) => self.pending = Zeroizing::new(bytes),
            Err(_) => return self.fail("lobby world list request failed"),
        }
        Ok(())
    }

    pub fn tick(&mut self) -> Result<(), &'static str> {
        self.guard()?;
        if !self.pending.is_empty() {
            return self.fail("lobby outgoing buffer must be drained");
        }
        let Some(stream) = self.stream.as_mut() else {
            return self.fail("invalid lobby keepalive phase");
        };
        match stream.tick() {
            Ok(Some(bytes)) => self.pending = Zeroizing::new(bytes),
            Ok(None) => {}
            Err(_) => return self.fail("lobby keepalive failed"),
        }
        Ok(())
    }

    pub fn mark_flushed(&mut self) -> Result<(), &'static str> {
        self.guard()?;
        if !self.pending.is_empty() {
            return self.fail("lobby outgoing buffer must be drained");
        }
        let Some(stream) = self.stream.as_mut() else {
            return self.fail("invalid lobby flush phase");
        };
        if stream.mark_flushed().is_err() {
            return self.fail("lobby flush failed");
        }
        Ok(())
    }

    pub fn finish(&mut self) -> Result<(), &'static str> {
        if self.current_phase() == LobbyPhase::Closed {
            return Ok(());
        }
        self.guard()?;
        if !self.pending.is_empty() {
            return self.fail("lobby connection ended with unsent bytes");
        }
        if let Some(attempt) = self.attempt.as_mut()
            && attempt.finish().is_err()
        {
            return self.fail("lobby connection ended during login");
        }
        if let Some(stream) = self.stream.as_mut()
            && stream.finish().is_err()
        {
            return self.fail("lobby connection ended during packet");
        }
        if self.worlds.finish().is_err() {
            return self.fail("lobby connection ended during world list");
        }
        self.clear(LobbyPhase::Closed);
        Ok(())
    }

    pub fn close(&mut self) {
        self.clear(LobbyPhase::Closed);
    }

    fn current_phase(&self) -> LobbyPhase {
        if let Some(terminal) = self.terminal {
            terminal
        } else if self.stream.is_some() {
            LobbyPhase::Lobby
        } else {
            self.attempt
                .as_ref()
                .map_or(LobbyPhase::Failed, |attempt| attempt.phase().into())
        }
    }

    fn expire(&mut self) -> bool {
        if self.terminal.is_none() && self.lifetime.expired() {
            self.clear(LobbyPhase::Failed);
            true
        } else {
            false
        }
    }

    fn guard(&mut self) -> Result<(), &'static str> {
        if self.expire() {
            return Err("lobby session lifetime limit");
        }
        match self.current_phase() {
            LobbyPhase::Closed => return Err("lobby session is closed"),
            LobbyPhase::Failed => return Err("lobby session has failed"),
            _ => {}
        }
        self.operations += 1;
        if self.operations > MAX_OPERATIONS {
            return self.fail("lobby session operation limit");
        }
        Ok(())
    }

    fn fail<T>(&mut self, error: &'static str) -> Result<T, &'static str> {
        self.clear(LobbyPhase::Failed);
        Err(error)
    }

    fn clear(&mut self, phase: LobbyPhase) {
        if let Some(attempt) = self.attempt.as_mut() {
            attempt.abort();
        }
        if let Some(stream) = self.stream.as_mut() {
            stream.close();
        }
        self.attempt = None;
        self.stream = None;
        self.world_token = None;
        self.default_world_id = None;
        self.worlds.reset();
        self.pending.zeroize();
        self.terminal = Some(phase);
    }
}

#[cfg(target_arch = "wasm32")]
pub(crate) mod browser {
    use super::*;
    use wasm_bindgen::prelude::*;

    pub(crate) fn bounded_json(
        value: JsValue,
        invalid: &'static str,
        limit: &'static str,
    ) -> Result<Zeroizing<String>, JsValue> {
        let string = value
            .dyn_ref::<js_sys::JsString>()
            .ok_or_else(|| JsValue::from_str(invalid))?;
        if string.length() as usize > MAX_CONFIG_BYTES {
            return Err(JsValue::from_str(limit));
        }
        let json = Zeroizing::new(
            value
                .as_string()
                .ok_or_else(|| JsValue::from_str(invalid))?,
        );
        if json.len() > MAX_CONFIG_BYTES {
            return Err(JsValue::from_str(limit));
        }
        Ok(json)
    }

    pub(crate) fn bounded_input(
        value: JsValue,
        invalid: &'static str,
        limit: &'static str,
    ) -> Result<Zeroizing<Vec<u8>>, &'static str> {
        let array = value.dyn_ref::<js_sys::Uint8Array>().ok_or(invalid)?;
        let length = array.length() as usize;
        if length > MAX_FEED_BYTES {
            return Err(limit);
        }
        let mut bytes = Zeroizing::new(vec![0; length]);
        array.copy_to(&mut bytes);
        Ok(bytes)
    }

    pub(super) fn now() -> Option<f64> {
        let global = js_sys::global();
        let performance = js_sys::Reflect::get(&global, &JsValue::from_str("performance")).ok()?;
        let method = js_sys::Reflect::get(&performance, &JsValue::from_str("now")).ok()?;
        method
            .dyn_ref::<js_sys::Function>()?
            .call0(&performance)
            .ok()?
            .as_f64()
            .filter(|now| now.is_finite() && *now >= 0.0)
    }

    #[wasm_bindgen(js_name = OriginalLobbySession)]
    pub struct BrowserLobbySession(LobbySession);

    #[wasm_bindgen(js_class = OriginalLobbySession)]
    impl BrowserLobbySession {
        #[wasm_bindgen(constructor)]
        pub fn new(config_json: JsValue) -> Result<BrowserLobbySession, JsValue> {
            let json = bounded_json(
                config_json,
                "invalid lobby configuration type",
                "lobby configuration byte limit",
            )?;
            LobbySession::from_json(&json)
                .map(Self)
                .map_err(JsValue::from_str)
        }

        pub fn outgoing(&mut self) -> Result<js_sys::Uint8Array, JsValue> {
            let bytes = self.0.take_outgoing().map_err(JsValue::from_str)?;
            Ok(js_sys::Uint8Array::from(bytes.as_slice()))
        }

        pub fn feed(&mut self, bytes: JsValue) -> Result<String, JsValue> {
            let bytes = match bounded_input(
                bytes,
                "invalid lobby incoming type",
                "lobby incoming chunk byte limit",
            ) {
                Ok(bytes) => bytes,
                Err(error) => return self.0.fail(error).map_err(JsValue::from_str),
            };
            let report = self.0.feed(&bytes).map_err(JsValue::from_str)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("lobby feed serialization failed"))
        }

        pub fn phase(&mut self) -> String {
            self.0.phase().as_str().into()
        }

        pub fn report(&mut self) -> Result<String, JsValue> {
            serde_json::to_string(&self.0.report())
                .map_err(|_| JsValue::from_str("lobby report serialization failed"))
        }

        #[wasm_bindgen(js_name = startWorld)]
        pub fn start_world(
            &mut self,
            config_json: JsValue,
        ) -> Result<crate::world_session::browser::BrowserWorldSession, JsValue> {
            let json = bounded_json(
                config_json,
                "invalid world configuration type",
                "world configuration byte limit",
            )?;
            let config =
                crate::world_session::WorldConfig::from_json(&json).map_err(JsValue::from_str)?;
            self.0
                .start_world(config)
                .map(crate::world_session::browser::BrowserWorldSession)
                .map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = startWorldState)]
        pub fn start_world_state(
            &mut self,
            config_json: JsValue,
            varp_count: JsValue,
        ) -> Result<crate::world_session::browser::BrowserWorldSession, JsValue> {
            let count = crate::world_session::browser::bounded_varp_number(
                varp_count,
                crate::vars::MAX_VARPS,
                "invalid world variable count",
            )?;
            let json = bounded_json(
                config_json,
                "invalid world configuration type",
                "world configuration byte limit",
            )?;
            let config =
                crate::world_session::WorldConfig::from_json(&json).map_err(JsValue::from_str)?;
            self.0
                .start_world_state(config, count)
                .map(crate::world_session::browser::BrowserWorldSession)
                .map_err(JsValue::from_str)
        }

        pub fn confirm(&mut self) -> Result<(), JsValue> {
            self.0.confirm().map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = requestWorldList)]
        pub fn request_world_list(&mut self, version: JsValue) -> Result<(), JsValue> {
            let valid = version.as_f64().filter(|value| {
                value.is_finite()
                    && value.fract() == 0.0
                    && (i32::MIN as f64..=i32::MAX as f64).contains(value)
            });
            match valid {
                Some(version) => self
                    .0
                    .request_world_list(version as i32)
                    .map_err(JsValue::from_str),
                None => self
                    .0
                    .fail("invalid world list version")
                    .map_err(JsValue::from_str),
            }
        }

        pub fn tick(&mut self) -> Result<(), JsValue> {
            self.0.tick().map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = markFlushed)]
        pub fn mark_flushed(&mut self) -> Result<(), JsValue> {
            self.0.mark_flushed().map_err(JsValue::from_str)
        }

        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(JsValue::from_str)
        }

        pub fn close(&mut self) {
            self.0.close();
        }
    }
}

#[cfg(test)]
pub(crate) mod tests;
