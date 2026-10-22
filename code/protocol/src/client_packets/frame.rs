use super::menu::ClientBody;
use super::writer::ClientPacketError;
use crate::framing::PacketSize;
use crate::isaac::Isaac;

pub fn frame_body(isaac: &mut Isaac, body: &ClientBody) -> Result<Vec<u8>, ClientPacketError> {
    let metadata = body.packet.metadata();
    let length = body.body.len();
    let prefix = match metadata.size {
        PacketSize::Fixed(expected) => {
            if length != expected {
                return Err(ClientPacketError::TooLong(length));
            }
            0
        }
        PacketSize::VarByte => {
            if length > usize::from(u8::MAX) {
                return Err(ClientPacketError::TooLong(length));
            }
            1
        }
        PacketSize::VarShort => {
            if length > usize::from(u16::MAX) {
                return Err(ClientPacketError::TooLong(length));
            }
            2
        }
    };
    let mut out = Vec::with_capacity(length + prefix + 1);
    out.push((body.packet.wire() as u8).wrapping_add(isaac.next_u32() as u8));
    match metadata.size {
        PacketSize::Fixed(_) => {}
        PacketSize::VarByte => out.push(length as u8),
        PacketSize::VarShort => out.extend_from_slice(&(length as u16).to_be_bytes()),
    }
    out.extend_from_slice(&body.body);
    Ok(out)
}
