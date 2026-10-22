use super::world::host_error;
use super::{pop, push, Vm};
use crate::login::*;
use crate::{
    ClientState, HostError, LobbyPacket, LoginConnection, LoginEffect, Opcode, Secret, StackKind,
    VarHost, VmErrorKind,
};
use std::sync::Arc;

fn null_pointer() -> VmErrorKind {
    VmErrorKind::Host(HostError::NoEntity)
}

fn units(text: &str) -> usize {
    text.encode_utf16().count()
}

fn text(value: Option<Arc<str>>) -> String {
    value.map_or_else(String::new, |value| value.to_string())
}

fn set_login_response(client: &mut ClientState, code: i32) {
    match client.login.lobby_stage {
        LOBBY_STAGE_LOGGING_IN_LOBBY => client.lobby_login_response = code,
        LOBBY_STAGE_LOBBY_TO_WORLD => client.world_login_response = code,
        _ => {}
    }
}

fn reset_login_state(client: &mut ClientState) {
    client.login.login_stage = LOGIN_STAGE_IDLE;
    client.world_login_response = -2;
    client.lobby_login_response = -2;
}

fn initialize_login(
    client: &mut ClientState,
    social_network: bool,
    stored: bool,
    username: &str,
    password: &str,
    token: i64,
) -> Result<(), VmErrorKind> {
    client.login.social_network_login = social_network;
    if !stored {
        client.login.selected_world_id = -1;
    }
    client.login.has_stored_credentials = stored;
    client.login.login_username = username.to_string();
    client.login.login_password = Secret(password.to_string());
    client.login.social_network_token = token;
    if stored || (!username.is_empty() && !password.is_empty()) {
        if client.login.lobby_stage != LOBBY_STAGE_LOGGING_IN_LOBBY {
            client.login_queue_position = 0;
            client.login_response_detail = -1;
            client.login_response_extra_info = -1;
        }
        match client.login.current_connection {
            Some(LoginConnection::Lobby) => client.login.lobby_dropped = false,
            Some(LoginConnection::Game) => client.login.game_dropped = false,
            None => return Err(null_pointer()),
        }
        set_login_response(client, -3);
        client.login.login_stage = LOGIN_STAGE_OPENING_CONNECTION;
        client.login.connection_tick_count = 0;
        client.login.connection_retry_count = 0;
    } else {
        set_login_response(client, 3);
    }
    Ok(())
}

fn start_login(client: &mut ClientState, lobby: bool) -> Result<(), VmErrorKind> {
    let (stage, connection) = if lobby {
        (LOBBY_STAGE_LOGGING_IN_LOBBY, LoginConnection::Lobby)
    } else {
        (LOBBY_STAGE_LOBBY_TO_WORLD, LoginConnection::Game)
    };
    client.login.lobby_stage = stage;
    client.login.current_connection = Some(connection);
    if let Some((token, nonce)) = client.login.base64_url {
        client.login.social_network_token = token;
        client.login.social_network_nonce = nonce;
        if token < 0 {
            set_login_response(client, 35);
            return Ok(());
        }
        return initialize_login(client, false, true, "", "", token);
    }
    if client.login.selected_world_id != -1 {
        let token = client.login.social_network_token;
        return initialize_login(client, token == -1, true, "", "", token);
    }
    let username = client.login.login_username.clone();
    let password = client.login.login_password.0.clone();
    initialize_login(client, false, false, &username, &password, -1)
}

fn set_game_state(client: &mut ClientState, state: i32) -> Result<(), VmErrorKind> {
    let previous = client.login.game_state;
    if state == previous {
        return Ok(());
    }
    client.login.effects.push(LoginEffect::GameState(state));
    if state == GAME_STATE_WORLD_LOGIN_A || state == GAME_STATE_WORLD_LOGIN_B {
        start_login(client, false)?;
    }
    if state == GAME_STATE_LOGIN_SCREEN {
        client.login.effects.push(LoginEffect::PreparePlayer);
    }
    if state == GAME_STATE_LOBBY_LOADING {
        client.login.effects.push(LoginEffect::OpenLobby);
    }
    if state != GAME_STATE_LOBBY_LOGIN && state != GAME_STATE_RECONNECT {
        if state == GAME_STATE_WORLD_LOGIN_C
            || (state == GAME_STATE_LOBBY && previous != GAME_STATE_LOBBY_LOGIN_PENDING)
        {
            start_login(client, false)?;
        }
        if state == GAME_STATE_CREATE_CONNECT {
            client.login.create_stage = CreateStage::Request;
            initialize_account_creation(client);
        }
    } else {
        start_login(client, true)?;
    }
    client.login.game_state = state;
    Ok(())
}

fn handle_disconnection(client: &mut ClientState) -> Result<(), VmErrorKind> {
    let state = client.login.game_state;
    let target = if state == GAME_STATE_LOBBY || state == GAME_STATE_LOBBY_LOGIN_PENDING {
        Some(if client.login.lobby_connection_open {
            GAME_STATE_LOBBY_LOADING
        } else {
            GAME_STATE_LOBBY_LOGIN
        })
    } else if state != GAME_STATE_LOBBY_LOGIN && state != GAME_STATE_WORLD_LOGIN_C {
        (state == GAME_STATE_RECONNECT).then_some(GAME_STATE_LOGIN_SCREEN)
    } else {
        Some(GAME_STATE_LOGIN_SCREEN)
    };
    match target {
        Some(target) => set_game_state(client, target),
        None => Ok(()),
    }
}

fn logout(client: &mut ClientState, reconnect: bool) -> Result<(), VmErrorKind> {
    client.login.close(LoginConnection::Game);
    client.login.close(LoginConnection::Lobby);
    reset_login_state(client);
    client.login.effects.push(LoginEffect::Logout { reconnect });
    set_game_state(
        client,
        if reconnect {
            GAME_STATE_RECONNECT
        } else {
            GAME_STATE_LOGIN_SCREEN
        },
    )
}

fn direct_login(client: &mut ClientState, username: &str, password: &str) -> Result<(), VmErrorKind> {
    if units(username) <= MAX_CREDENTIAL_UNITS && client.login.ready_for_login() {
        client.login.reset_credentials();
        client.login.login_username = username.to_string();
        client.login.login_password = Secret(password.to_string());
        set_game_state(client, GAME_STATE_WORLD_LOGIN_C)?;
    }
    Ok(())
}

fn select_world(client: &mut ClientState, world: i32) {
    if world != client.login.selected_world_id {
        client.login.social_network_token = -1;
    }
    client.login.selected_world_id = world;
}

fn switch_and_login(
    client: &mut ClientState,
    username: &str,
    password: &str,
) -> Result<(), VmErrorKind> {
    if units(username) <= MAX_CREDENTIAL_UNITS && client.login.ready_for_login() {
        client.login.close(LoginConnection::Lobby);
        client.login.reset_credentials();
        client.login.login_username = username.to_string();
        client.login.login_password = Secret(password.to_string());
        set_game_state(client, GAME_STATE_LOBBY_LOGIN)?;
    }
    Ok(())
}

fn queue(client: &mut ClientState, packet: LobbyPacket) {
    client.login.effects.push(LoginEffect::LobbyPacket(packet));
}

fn initialize_account_creation(client: &mut ClientState) {
    client.login.received_response = Some(-3);
    client.login.create_request = Some(-2);
    client.login.check_email_status = Some(-2);
}

impl Vm {
    pub(super) fn is_login_effect_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            LOGIN_REQUEST
                | LOGIN_CONTINUE
                | LOGIN_RESETREPLY
                | LOGIN_CANCEL
                | LOGIN_INPROGRESS
                | LOGIN_LAST_TRANSFER_REPLY
                | LOGIN_REQUEST_SOCIAL_NETWORK
                | CREATE_AVAILABLEREQUEST
                | CREATE_CREATEREQUEST
                | CREATE_CONNECTREQUEST
                | CREATE_STEP_REACHED
                | CREATE_REPLY
                | CREATE_EMAIL_VALIDATE_REPLY
                | CREATE_CONNECT_REPLY
                | LOBBY_ENTERGAME
                | LOBBY_ENTERLOBBY
                | LOBBY_LEAVELOBBY
                | LOBBY_ENTERLOBBY_SOCIAL_NETWORK
        )
    }

    pub(super) fn execute_login_effect(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let client = host.client().map_err(host_error(opcode))?;
        match opcode {
            LOGIN_REQUEST | LOBBY_ENTERLOBBY => {
                let password = pop(&mut self.strings, Text)?;
                let username = pop(&mut self.strings, Text)?;
                let (Some(username), Some(password)) = (username, password) else {
                    return Err(null_pointer());
                };
                if opcode == LOGIN_REQUEST {
                    direct_login(client, &username, &password)?;
                } else {
                    switch_and_login(client, &username, &password)?;
                }
            }
            LOGIN_CONTINUE => {
                if client.login.login_stage == LOGIN_STAGE_AWAITING_CLIENT_CONFIRM {
                    client.login.login_stage = LOGIN_STAGE_SENDING_LOGIN_CONTINUE;
                }
            }
            LOGIN_RESETREPLY => {
                if !client.login.in_progress() {
                    reset_login_state(client);
                }
            }
            LOGIN_CANCEL => {
                if client.login.in_progress() {
                    match client.login.current_connection {
                        Some(connection) => client.login.close(connection),
                        None => return Err(null_pointer()),
                    }
                    reset_login_state(client);
                    handle_disconnection(client)?;
                }
            }
            LOGIN_INPROGRESS => push(&mut self.ints, i32::from(client.login.in_progress()), Int)?,
            LOGIN_LAST_TRANSFER_REPLY => {
                push(&mut self.ints, client.login.last_disconnect_response, Int)?;
                push(&mut self.ints, client.login.last_disconnect_arg2, Int)?;
                push(&mut self.ints, client.login.last_disconnect_arg1, Int)?;
                client.login.last_disconnect_response = -2;
                client.login.last_disconnect_arg2 = -1;
                client.login.last_disconnect_arg1 = -1;
            }
            LOGIN_REQUEST_SOCIAL_NETWORK | LOBBY_ENTERLOBBY_SOCIAL_NETWORK => {
                let world = pop(&mut self.ints, Int)?;
                if client.login.ready_for_login() {
                    select_world(client, world);
                    if opcode == LOGIN_REQUEST_SOCIAL_NETWORK {
                        set_game_state(client, GAME_STATE_WORLD_LOGIN_C)?;
                    } else {
                        client.login.close(LoginConnection::Lobby);
                        set_game_state(client, GAME_STATE_LOBBY_LOGIN)?;
                    }
                }
            }
            CREATE_AVAILABLEREQUEST => {
                let email = pop(&mut self.strings, Text)?;
                if client.login.game_state == GAME_STATE_ACCOUNT_CREATION {
                    if client.login.login_xteas.is_none() {
                        return Err(null_pointer());
                    }
                    queue(
                        client,
                        LobbyPacket::CheckEmail {
                            email: text(email),
                        },
                    );
                    client.login.check_email_status = Some(-3);
                }
            }
            CREATE_CREATEREQUEST => {
                let agreed = pop(&mut self.ints, Int)?;
                let age = pop(&mut self.ints, Int)?;
                let password = pop(&mut self.strings, Text)?;
                let email = pop(&mut self.strings, Text)?;
                if client.login.game_state == GAME_STATE_ACCOUNT_CREATION {
                    if client.login.login_xteas.is_none() {
                        return Err(null_pointer());
                    }
                    queue(
                        client,
                        LobbyPacket::SignUpForm {
                            email: text(email),
                            password: Secret(text(password)),
                            age: age as u8,
                            agreed: agreed == 1,
                        },
                    );
                    if age < 13 {
                        client.just_created_account = true;
                    }
                    client.login.create_request = Some(-3);
                }
            }
            CREATE_CONNECTREQUEST => {
                if client.login.ready_for_login() {
                    set_game_state(client, GAME_STATE_CREATE_CONNECT)?;
                }
            }
            CREATE_STEP_REACHED => {
                let stage = pop(&mut self.ints, Int)?;
                if client.login.game_state == GAME_STATE_ACCOUNT_CREATION {
                    queue(client, LobbyPacket::AccountCreationStage { stage: stage as u8 });
                }
            }
            CREATE_REPLY => push(
                &mut self.ints,
                client.login.create_request.unwrap_or(-2),
                Int,
            )?,
            CREATE_EMAIL_VALIDATE_REPLY => push(
                &mut self.ints,
                client.login.check_email_status.unwrap_or(-2),
                Int,
            )?,
            CREATE_CONNECT_REPLY => push(
                &mut self.ints,
                client.login.received_response.unwrap_or(-2),
                Int,
            )?,
            LOBBY_ENTERGAME => {
                let login = &client.login;
                if login.game_state == GAME_STATE_LOBBY_LOADING
                    && !login.in_progress()
                    && !login.creating_account()
                {
                    set_game_state(client, GAME_STATE_LOBBY)?;
                }
            }
            LOBBY_LEAVELOBBY => logout(client, false)?,
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }
}
