use crate::{Error, SensitiveString, strings};
use zeroize::{Zeroize, Zeroizing};

pub const MAX_SUCCESS_BYTES: usize = u8::MAX as usize;
pub const MIN_LOBBY_SUCCESS_BYTES: usize = 61;
pub const MIN_WORLD_SUCCESS_BYTES: usize = 14;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LoginKind {
    Lobby,
    World,
}

impl LoginKind {
    fn minimum_payload(self) -> usize {
        match self {
            Self::Lobby => MIN_LOBBY_SUCCESS_BYTES,
            Self::World => MIN_WORLD_SUCCESS_BYTES,
        }
    }
}

pub struct LobbySuccess {
    pub rights: u8,
    pub mod_level: u8,
    pub quick_chat: bool,
    pub date_of_birth: i32,
    pub gender: i8,
    pub verified_email: bool,
    pub account_security: bool,
    pub membership_end: i64,
    pub membership_time_offset: u64,
    pub membership_flags: u8,
    pub jcoins: i32,
    pub loyalty_enabled: bool,
    pub loyalty_balance: i32,
    pub recovery_questions_set_date: u16,
    pub messages: u16,
    pub last_login_day: u16,
    pub last_ip_address: u32,
    pub world_login_token: SensitiveString,
    pub email_status: u8,
    pub credit_card_expiry: u16,
    pub grace_expiry: u16,
    pub dob_requested: bool,
    pub display_name: SensitiveString,
    pub membership_status: u8,
    pub play_age: i32,
    pub display_name_pending: bool,
    pub default_world_id: Option<u16>,
    pub default_world_host: SensitiveString,
}

impl LobbySuccess {
    pub fn is_member(&self) -> bool {
        self.membership_flags & 1 != 0
    }

    pub fn has_member_subscription(&self) -> bool {
        self.membership_flags & 2 != 0
    }
}

pub struct WorldSuccess {
    pub rights: u8,
    pub mod_level: u8,
    pub quick_chat: bool,
    pub verified_email: bool,
    pub account_security: bool,
    pub quick_chat_only: bool,
    pub player_index: u16,
    pub members: bool,
    pub date_of_birth: i32,
    pub member_world: bool,
    pub map_owner_identifier: SensitiveString,
}

pub enum LoginResponse {
    LobbySuccess(LobbySuccess),
    WorldSuccess(WorldSuccess),
    ConfirmationRequired,
    ReconnectRequired,
    RetryRequested,
    Queue {
        position: u8,
        delay_ticks: u16,
    },
    Rejected {
        response_code: u8,
    },
    RejectedDetail {
        response_code: u8,
        detail: u8,
        extra_info: Option<u16>,
    },
    TimeSync {
        code: u16,
    },
}

pub struct ResponseFeed {
    pub consumed: usize,
    pub event: Option<LoginResponse>,
}

pub struct LoginResponseDecoder {
    kind: LoginKind,
    limit: usize,
    state: State,
    payload: Zeroizing<[u8; MAX_SUCCESS_BYTES]>,
}

enum State {
    Code,
    Length,
    Payload {
        length: usize,
        used: usize,
    },
    Detail {
        code: u8,
        length: usize,
        used: usize,
    },
    Finished,
    Failed,
}

impl LoginResponseDecoder {
    pub fn new(kind: LoginKind, max_success_bytes: usize) -> Result<Self, Error> {
        if !(kind.minimum_payload()..=MAX_SUCCESS_BYTES).contains(&max_success_bytes) {
            return Err(Error::InvalidLimit);
        }
        Ok(Self {
            kind,
            limit: max_success_bytes,
            state: State::Code,
            payload: Zeroizing::new([0; MAX_SUCCESS_BYTES]),
        })
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<ResponseFeed, Error> {
        match self.state {
            State::Finished => return Err(Error::DecoderFinished),
            State::Failed => return Err(Error::DecoderFailed),
            _ => {}
        }
        match self.feed_inner(bytes) {
            Ok(result) => Ok(result),
            Err(error) => {
                self.state = State::Failed;
                self.payload.zeroize();
                Err(error)
            }
        }
    }

    pub fn finish(&mut self) -> Result<(), Error> {
        match self.state {
            State::Finished => Ok(()),
            State::Failed => Err(Error::DecoderFailed),
            _ => {
                self.state = State::Failed;
                self.payload.zeroize();
                Err(Error::UnexpectedEof)
            }
        }
    }

    fn terminal(&mut self, consumed: usize, event: LoginResponse) -> ResponseFeed {
        self.state = State::Finished;
        self.payload.zeroize();
        ResponseFeed {
            consumed,
            event: Some(event),
        }
    }

    fn feed_inner(&mut self, bytes: &[u8]) -> Result<ResponseFeed, Error> {
        let mut consumed = 0;
        while consumed < bytes.len() {
            match self.state {
                State::Code => {
                    let code = bytes[consumed];
                    consumed += 1;
                    match code {
                        2 => self.state = State::Length,
                        21 | 29 => {
                            self.state = State::Detail {
                                code,
                                length: 1,
                                used: 0,
                            }
                        }
                        45 => {
                            self.state = State::Detail {
                                code,
                                length: 3,
                                used: 0,
                            }
                        }
                        42 => {
                            self.state = State::Detail {
                                code,
                                length: 2,
                                used: 0,
                            }
                        }
                        1 => {
                            return Ok(self.terminal(consumed, LoginResponse::ConfirmationRequired));
                        }
                        15 => return Ok(self.terminal(consumed, LoginResponse::ReconnectRequired)),
                        23 => return Ok(self.terminal(consumed, LoginResponse::RetryRequested)),
                        response_code => {
                            return Ok(
                                self.terminal(consumed, LoginResponse::Rejected { response_code })
                            );
                        }
                    }
                }
                State::Length => {
                    let length = bytes[consumed] as usize;
                    consumed += 1;
                    if length < self.kind.minimum_payload() || length > self.limit {
                        return Err(Error::LengthLimitExceeded);
                    }
                    self.state = State::Payload { length, used: 0 };
                }
                State::Payload { length, used } => {
                    let count = (length - used).min(bytes.len() - consumed);
                    self.payload[used..used + count]
                        .copy_from_slice(&bytes[consumed..consumed + count]);
                    consumed += count;
                    let used = used + count;
                    if used == length {
                        let event = match self.kind {
                            LoginKind::Lobby => LoginResponse::LobbySuccess(decode_lobby_success(
                                &self.payload[..length],
                            )?),
                            LoginKind::World => LoginResponse::WorldSuccess(decode_world_success(
                                &self.payload[..length],
                            )?),
                        };
                        return Ok(self.terminal(consumed, event));
                    }
                    self.state = State::Payload { length, used };
                }
                State::Detail { code, length, used } => {
                    let count = (length - used).min(bytes.len() - consumed);
                    self.payload[used..used + count]
                        .copy_from_slice(&bytes[consumed..consumed + count]);
                    consumed += count;
                    let used = used + count;
                    if used == length {
                        let first = self.payload[0];
                        if code == 42 {
                            let code = u16::from_be_bytes([first, self.payload[1]]);
                            self.payload.zeroize();
                            self.state = State::Code;
                            return Ok(ResponseFeed {
                                consumed,
                                event: Some(LoginResponse::TimeSync { code }),
                            });
                        }
                        let event = if code == 21 {
                            LoginResponse::Queue {
                                position: first,
                                delay_ticks: u16::from(first) * 50,
                            }
                        } else {
                            LoginResponse::RejectedDetail {
                                response_code: code,
                                detail: first,
                                extra_info: (code == 45).then(|| {
                                    u16::from_be_bytes([self.payload[1], self.payload[2]])
                                }),
                            }
                        };
                        return Ok(self.terminal(consumed, event));
                    }
                    self.state = State::Detail { code, length, used };
                }
                State::Finished => return Err(Error::DecoderFinished),
                State::Failed => return Err(Error::DecoderFailed),
            }
        }
        Ok(ResponseFeed {
            consumed,
            event: None,
        })
    }
}

pub fn decode_lobby_success(payload: &[u8]) -> Result<LobbySuccess, Error> {
    validate_payload(payload, MIN_LOBBY_SUCCESS_BYTES)?;
    let mut cursor = Cursor {
        bytes: payload,
        position: 0,
    };
    let value = LobbySuccess {
        rights: cursor.u8()?,
        mod_level: cursor.u8()?,
        quick_chat: cursor.boolean()?,
        date_of_birth: cursor.i24()?,
        gender: cursor.u8()? as i8,
        verified_email: cursor.boolean()?,
        account_security: cursor.boolean()?,
        membership_end: i64::from_be_bytes(cursor.take()?),
        membership_time_offset: cursor.u40()?,
        membership_flags: cursor.u8()?,
        jcoins: i32::from_be_bytes(cursor.take()?),
        loyalty_enabled: cursor.boolean()?,
        loyalty_balance: i32::from_be_bytes(cursor.take()?),
        recovery_questions_set_date: cursor.u16()?,
        messages: cursor.u16()?,
        last_login_day: cursor.u16()?,
        last_ip_address: u32::from_be_bytes(cursor.take()?),
        world_login_token: cursor.string(false)?,
        email_status: cursor.u8()?,
        credit_card_expiry: cursor.u16()?,
        grace_expiry: cursor.u16()?,
        dob_requested: cursor.boolean()?,
        display_name: cursor.string(true)?,
        membership_status: cursor.u8()?,
        play_age: i32::from_be_bytes(cursor.take()?),
        display_name_pending: cursor.boolean()?,
        default_world_id: match cursor.u16()? {
            u16::MAX => None,
            id => Some(id),
        },
        default_world_host: cursor.string(true)?,
    };
    cursor.finish()?;
    Ok(value)
}

pub fn decode_world_success(payload: &[u8]) -> Result<WorldSuccess, Error> {
    validate_payload(payload, MIN_WORLD_SUCCESS_BYTES)?;
    let mut cursor = Cursor {
        bytes: payload,
        position: 0,
    };
    let value = WorldSuccess {
        rights: cursor.u8()?,
        mod_level: cursor.u8()?,
        quick_chat: cursor.boolean()?,
        verified_email: cursor.boolean()?,
        account_security: cursor.boolean()?,
        quick_chat_only: cursor.boolean()?,
        player_index: cursor.u16()?,
        members: cursor.boolean()?,
        date_of_birth: cursor.i24()?,
        member_world: cursor.boolean()?,
        map_owner_identifier: cursor.string(false)?,
    };
    cursor.finish()?;
    Ok(value)
}

fn validate_payload(payload: &[u8], minimum: usize) -> Result<(), Error> {
    if payload.len() > MAX_SUCCESS_BYTES {
        Err(Error::LengthLimitExceeded)
    } else if payload.len() < minimum {
        Err(Error::UnexpectedEof)
    } else {
        Ok(())
    }
}

struct Cursor<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl Cursor<'_> {
    fn take<const N: usize>(&mut self) -> Result<[u8; N], Error> {
        let selected = self
            .bytes
            .get(self.position..self.position + N)
            .ok_or(Error::UnexpectedEof)?;
        let output = std::array::from_fn(|index| selected[index]);
        self.position += N;
        Ok(output)
    }

    fn u8(&mut self) -> Result<u8, Error> {
        Ok(self.take::<1>()?[0])
    }

    fn boolean(&mut self) -> Result<bool, Error> {
        Ok(self.u8()? == 1)
    }

    fn u16(&mut self) -> Result<u16, Error> {
        Ok(u16::from_be_bytes(self.take()?))
    }

    fn i24(&mut self) -> Result<i32, Error> {
        let [first, second, third] = self.take()?;
        Ok(i32::from_be_bytes([
            if first & 0x80 != 0 { 255 } else { 0 },
            first,
            second,
            third,
        ]))
    }

    fn u40(&mut self) -> Result<u64, Error> {
        let [first, second, third, fourth, fifth] = self.take()?;
        Ok(u64::from_be_bytes([
            0, 0, 0, first, second, third, fourth, fifth,
        ]))
    }

    fn string(&mut self, jag: bool) -> Result<SensitiveString, Error> {
        let remaining = &self.bytes[self.position..];
        let value = if jag {
            strings::decode_jag(remaining, remaining.len())?
        } else {
            strings::decode(remaining, remaining.len())?
        };
        self.position += value.consumed;
        Ok(value.value)
    }

    fn finish(&self) -> Result<(), Error> {
        if self.position == self.bytes.len() {
            Ok(())
        } else {
            Err(Error::TrailingData)
        }
    }
}
