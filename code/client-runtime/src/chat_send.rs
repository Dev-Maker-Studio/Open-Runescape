use openrs_cs2::ChatSend;
use openrs_protocol::client_packets::{ClientBody, Huffman, bodies};
use openrs_protocol::registry::ClientPacket;

pub fn to_body(huffman: &Huffman, send: &ChatSend) -> Result<ClientBody, String> {
    let (packet, body) = match send {
        ChatSend::Public {
            color,
            effect,
            message,
        } => (
            ClientPacket::CHAT,
            bodies::chat(
                huffman,
                i32::from(*color),
                i32::from(*effect),
                message.as_str(),
            ),
        ),
        ChatSend::Private { recipient, message } => (
            ClientPacket::SEND_PRIVATE_MESSAGE,
            bodies::send_private_message(huffman, recipient.as_str(), message.as_str()),
        ),
    };
    let body = body.map_err(|error| error.to_string())?;
    if matches!(send, ChatSend::Public { .. }) && body.len() > usize::from(u8::MAX) {
        return Err(format!(
            "chat body of {} bytes exceeds the frame",
            body.len()
        ));
    }
    Ok(ClientBody { packet, body })
}
