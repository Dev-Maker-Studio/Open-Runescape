use super::huffman::Huffman;
use super::writer::{ClientPacketError, Text, Writer};

fn one(f: impl FnOnce(&mut Writer)) -> Vec<u8> {
    let mut w = Writer::new();
    f(&mut w);
    w.into_bytes()
}

pub fn empty() -> Vec<u8> {
    Vec::new()
}

pub fn int(v: i32) -> Vec<u8> {
    one(|w| {
        w.int(v);
    })
}

pub fn short(v: i32) -> Vec<u8> {
    one(|w| {
        w.short(v);
    })
}

pub fn byte(v: i32) -> Vec<u8> {
    one(|w| {
        w.byte(v);
    })
}

pub fn flag(v: bool) -> Vec<u8> {
    byte(i32::from(v))
}

pub fn song_loaded(music_id: i32) -> Vec<u8> {
    int(music_id)
}

pub fn sound_effect_music_ended(archive: i32) -> Vec<u8> {
    int(archive)
}

pub fn send_fps(key: i32, flags: i32, fps: i32) -> Vec<u8> {
    one(|w| {
        w.int_inverse_middle(key)
            .int_middle(flags)
            .byte_subtract(fps.min(255));
    })
}

pub fn play_vorbis(file_id: i32) -> Vec<u8> {
    short(file_id)
}

pub fn unk_82() -> Vec<u8> {
    int(1_057_001_181)
}

pub fn request_world_list(version: i32) -> Vec<u8> {
    int(version)
}

pub fn resume_countdialog(count: i32) -> Vec<u8> {
    int(count)
}

pub fn resume_hsldialog(hsl: i32) -> Vec<u8> {
    short(hsl)
}

pub fn grand_exchange_item_select(obj: i32) -> Vec<u8> {
    short(obj)
}

pub fn write_ping(ping: i32) -> Vec<u8> {
    short(ping)
}

pub fn chat_type(mode: i32) -> Vec<u8> {
    byte(mode)
}

pub fn chat_setfilter(public: i32, private: i32, trade: i32) -> Vec<u8> {
    one(|w| {
        w.byte(public).byte(private).byte(trade);
    })
}

pub fn cutscene_finished(force: bool) -> Vec<u8> {
    flag(force)
}

pub fn transmitvar_verifyid(id: i32) -> Vec<u8> {
    int(id)
}

pub fn account_creation_stage(stage: i32) -> Vec<u8> {
    byte(stage)
}

pub fn screen_size(window_mode: i32, width: i32, height: i32, aa: i32) -> Vec<u8> {
    one(|w| {
        w.byte(window_mode).short(width).short(height).byte(aa);
    })
}

pub fn screen_size_native(window_mode: i32, width: i32, height: i32, aa: i32) -> Vec<u8> {
    screen_size(window_mode, width, height.wrapping_mul(-969_250_379), aa)
}

pub fn world_map_click(z: i32, x: i32, y: i32) -> Vec<u8> {
    one(|w| {
        w.int_little((z << 28) | (x << 14) | y);
    })
}

pub fn move_camera(angle_x: f32, angle_y: f32) -> Vec<u8> {
    one(|w| {
        w.short_add_little((angle_x as i32) >> 3)
            .short_add((angle_y as i32) >> 3);
    })
}

pub fn string_only(text: impl Into<Text>) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(text)?;
    Ok(w.into_bytes())
}

pub fn fc_leave() -> Vec<u8> {
    Vec::new()
}

pub fn fc_set_rank(rank: i32, username: impl Into<Text>) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.byte_subtract(rank).string(username)?;
    Ok(w.into_bytes())
}

pub fn add_ignore(name: impl Into<Text>, flag: bool) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(name)?.byte(i32::from(flag));
    Ok(w.into_bytes())
}

pub fn command(
    client_command: bool,
    save: bool,
    command: impl Into<Text>,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.byte(i32::from(client_command))
        .byte(i32::from(save))
        .string(command)?;
    Ok(w.into_bytes())
}

pub fn report_abuse(
    name: impl Into<Text>,
    rule_id: i32,
    mute: i32,
    comment: Option<Text>,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut comment = comment.unwrap_or_default();
    comment.0.truncate(80);
    let mut w = Writer::new();
    w.string(name)?
        .byte(rule_id - 1)
        .byte(mute)
        .string(comment)?;
    Ok(w.into_bytes())
}

pub fn send_clan_operation(index: i32, name: impl Into<Text>) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.short(index).string(name)?;
    Ok(w.into_bytes())
}

pub fn kick_player(main: bool, index: i32, name: impl Into<Text>) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.byte(i32::from(main)).short(index).string(name)?;
    Ok(w.into_bytes())
}

pub fn lobby_hyperlink(
    url: impl Into<Text>,
    text: impl Into<Text>,
    fallback: impl Into<Text>,
    new_window: bool,
    external: bool,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(url)?.string(text)?.string(fallback)?;
    w.byte(i32::from(new_window) | (i32::from(external) << 1));
    Ok(w.into_bytes())
}

pub fn bug_report(
    bug_type: i32,
    title: impl Into<Text>,
    description: impl Into<Text>,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.byte(bug_type).jag_string(description)?.jag_string(title)?;
    Ok(w.into_bytes())
}

pub fn email_add_new_address(
    email: impl Into<Text>,
    bit0: bool,
    bit1: bool,
    bit2: bool,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(email)?
        .byte(i32::from(bit0) | (i32::from(bit1) << 1) | (i32::from(bit2) << 2));
    Ok(w.into_bytes())
}

pub fn email_change_address(
    email: impl Into<Text>,
    confirm: impl Into<Text>,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(email)?.string(confirm)?;
    Ok(w.into_bytes())
}

pub fn chat(
    huffman: &Huffman,
    color: i32,
    effect: i32,
    message: impl Into<Text>,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.byte(color).byte(effect);
    huffman.chat_message(&mut w, message)?;
    Ok(w.into_bytes())
}

pub fn send_private_message(
    huffman: &Huffman,
    recipient: impl Into<Text>,
    message: impl Into<Text>,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(recipient)?;
    huffman.chat_message(&mut w, message)?;
    Ok(w.into_bytes())
}

pub const QUICKCHAT_TRANSMIT_SIZE: [(i32, i32); 14] = [
    (0, 2),
    (1, 2),
    (2, 4),
    (4, 1),
    (6, 0),
    (7, 0),
    (8, 0),
    (9, 0),
    (10, 2),
    (11, 0),
    (12, 0),
    (13, 0),
    (14, 0),
    (15, 0),
];

pub fn quickchat_transmit_size(value_type: i32) -> Option<i32> {
    QUICKCHAT_TRANSMIT_SIZE
        .iter()
        .find(|(t, _)| *t == value_type)
        .map(|(_, s)| *s)
}

fn encode_values(
    w: &mut Writer,
    value_types: &[i32],
    values: &[i32],
) -> Result<(), ClientPacketError> {
    for (index, value) in values.iter().enumerate() {
        let Some(value_type) = value_types.get(index) else {
            break;
        };
        let size = quickchat_transmit_size(*value_type)
            .ok_or(ClientPacketError::UnknownValueType(*value_type))?;
        if size > 0 {
            w.variable_length_long(i64::from(*value), size)?;
        }
    }
    Ok(())
}

pub fn quickchat_public(
    mode: i32,
    message_id: i32,
    value_types: &[i32],
    values: &[i32],
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.byte(mode).short(message_id);
    encode_values(&mut w, value_types, values)?;
    Ok(w.into_bytes())
}

pub fn quickchat_private(
    recipient: impl Into<Text>,
    message_id: i32,
    value_types: &[i32],
    values: &[i32],
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(recipient)?.short(message_id);
    encode_values(&mut w, value_types, values)?;
    Ok(w.into_bytes())
}
