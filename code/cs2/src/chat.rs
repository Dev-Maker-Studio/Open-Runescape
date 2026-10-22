pub const CHAT_LINE_CAPACITY: usize = 100;

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ChatLine {
    pub identifier: i32,
    pub time: i32,
    pub kind: Option<i32>,
    pub effect_flags: i32,
    pub crowned_name: Option<String>,
    pub name: Option<String>,
    pub simple_name: Option<String>,
    pub clan: Option<String>,
    pub quickchat_message_id: i32,
    pub message: Option<String>,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ChatState {
    pub lines: Vec<Option<ChatLine>>,
    pub line_count: i32,
    pub public_filter: i32,
    pub private_filter: Option<i32>,
    pub trade_filter: i32,
}

impl Default for ChatState {
    fn default() -> Self {
        Self {
            lines: vec![None; CHAT_LINE_CAPACITY],
            line_count: 0,
            public_filter: 0,
            private_filter: None,
            trade_filter: 0,
        }
    }
}

impl ChatState {
    pub fn line(&self, index: i32) -> Option<&ChatLine> {
        if (0..CHAT_LINE_CAPACITY as i32).contains(&index) {
            self.lines.get(index as usize).and_then(Option::as_ref)
        } else {
            None
        }
    }

    pub fn set_filters(&mut self, public: i32, private: i32, trade: i32) -> i32 {
        let private = match private {
            0..=2 => private,
            _ => 1,
        };
        self.public_filter = public;
        self.private_filter = Some(private);
        self.trade_filter = trade;
        private
    }
}
