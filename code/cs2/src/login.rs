use std::fmt;

pub const GAME_STATE_LOBBY_LOADING: i32 = 0;
pub const GAME_STATE_ACCOUNT_CREATION: i32 = 3;
pub const GAME_STATE_LOGIN_SCREEN: i32 = 5;
pub const GAME_STATE_LOBBY_LOGIN_PENDING: i32 = 6;
pub const GAME_STATE_LOBBY: i32 = 7;
pub const GAME_STATE_CREATE_CONNECT: i32 = 8;
pub const GAME_STATE_WORLD_LOGIN_A: i32 = 10;
pub const GAME_STATE_RECONNECT: i32 = 12;
pub const GAME_STATE_LOBBY_LOGIN: i32 = 14;
pub const GAME_STATE_WORLD_LOGIN_B: i32 = 17;
pub const GAME_STATE_WORLD_LOGIN_C: i32 = 19;

pub const LOGIN_STAGE_IDLE: i32 = 0;
pub const LOGIN_STAGE_OPENING_CONNECTION: i32 = 1;
pub const LOGIN_STAGE_AWAITING_CLIENT_CONFIRM: i32 = 9;
pub const LOGIN_STAGE_SENDING_LOGIN_CONTINUE: i32 = 10;

pub const LOBBY_STAGE_LOGGED_OUT: i32 = 0;
pub const LOBBY_STAGE_LOGGING_IN_LOBBY: i32 = 1;
pub const LOBBY_STAGE_LOBBY_TO_WORLD: i32 = 2;

pub const MAX_CREDENTIAL_UNITS: usize = 320;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum LoginConnection {
    Lobby,
    Game,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub enum CreateStage {
    #[default]
    None,
    Request,
    Response,
}

#[derive(Clone, Eq, PartialEq)]
pub struct Secret(pub String);

impl fmt::Debug for Secret {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter.write_str("Secret(..)")
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum LobbyPacket {
    CheckEmail {
        email: String,
    },
    SignUpForm {
        email: String,
        password: Secret,
        age: u8,
        agreed: bool,
    },
    AccountCreationStage {
        stage: u8,
    },
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum LoginEffect {
    GameState(i32),
    CloseConnection(LoginConnection),
    PreparePlayer,
    OpenLobby,
    Logout { reconnect: bool },
    LobbyPacket(LobbyPacket),
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct LoginState {
    pub game_state: i32,
    pub login_stage: i32,
    pub lobby_stage: i32,
    pub create_stage: CreateStage,
    pub create_request: Option<i32>,
    pub check_email_status: Option<i32>,
    pub received_response: Option<i32>,
    pub selected_world_id: i32,
    pub social_network_token: i64,
    pub social_network_nonce: i64,
    pub social_network_login: bool,
    pub has_stored_credentials: bool,
    pub login_username: String,
    pub login_password: Secret,
    pub last_disconnect_response: i32,
    pub last_disconnect_arg1: i32,
    pub last_disconnect_arg2: i32,
    pub lobby_connection_open: bool,
    pub game_connection_open: bool,
    pub lobby_dropped: bool,
    pub game_dropped: bool,
    pub current_connection: Option<LoginConnection>,
    pub base64_url: Option<(i64, i64)>,
    pub login_xteas: Option<[i32; 4]>,
    pub connection_tick_count: i32,
    pub connection_retry_count: i32,
    pub effects: Vec<LoginEffect>,
}

impl LoginState {
    pub fn new() -> Self {
        Self {
            game_state: 4,
            login_stage: LOGIN_STAGE_IDLE,
            lobby_stage: LOBBY_STAGE_LOGGED_OUT,
            create_stage: CreateStage::None,
            create_request: None,
            check_email_status: None,
            received_response: None,
            selected_world_id: -1,
            social_network_token: -1,
            social_network_nonce: 0,
            social_network_login: false,
            has_stored_credentials: false,
            login_username: String::new(),
            login_password: Secret(String::new()),
            last_disconnect_response: -2,
            last_disconnect_arg1: -1,
            last_disconnect_arg2: -1,
            lobby_connection_open: false,
            game_connection_open: false,
            lobby_dropped: false,
            game_dropped: false,
            current_connection: None,
            base64_url: None,
            login_xteas: None,
            connection_tick_count: 0,
            connection_retry_count: 0,
            effects: Vec::new(),
        }
    }

    pub fn in_progress(&self) -> bool {
        self.login_stage != LOGIN_STAGE_IDLE
    }

    pub fn creating_account(&self) -> bool {
        self.create_stage != CreateStage::None
    }

    pub fn ready_for_login(&self) -> bool {
        self.game_state == GAME_STATE_LOGIN_SCREEN && !self.in_progress() && !self.creating_account()
    }

    pub fn close(&mut self, connection: LoginConnection) {
        let open = match connection {
            LoginConnection::Lobby => &mut self.lobby_connection_open,
            LoginConnection::Game => &mut self.game_connection_open,
        };
        if *open {
            *open = false;
            self.effects.push(LoginEffect::CloseConnection(connection));
        }
    }

    pub fn reset_credentials(&mut self) {
        self.social_network_token = -1;
        self.social_network_nonce = 0;
        self.selected_world_id = -1;
    }
}

impl Default for LoginState {
    fn default() -> Self {
        Self::new()
    }
}
