use std::sync::Arc;

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct LobbyState {
    pub membership_end: i64,
    pub membership_time_correction: i64,
    pub is_member_subscription: bool,
    pub recovery_day: i32,
    pub messages: i32,
    pub last_login_day: i32,
    pub last_login_host: Option<Arc<str>>,
    pub email_status: i32,
    pub credit_card_expiry: i32,
    pub grace_expiry: i32,
    pub dob_requested: bool,
    pub dob: i32,
    pub members_stats: i32,
    pub play_age: i32,
    pub jcoins: i32,
    pub loyalty_enabled: bool,
    pub loyalty_balance: i32,
}
