use super::Vm;
use crate::outbox::ClientPacket;
use crate::{
    ComponentError, ComponentFailure, ComponentId, ComponentModel, Opcode, StackKind, VarHost,
    VmErrorKind,
};

const IF_CONTINUE: u8 = 49;
const CLOSE_INTERFACE: u8 = 60;

fn int_middle(value: i32) -> [u8; 4] {
    [(value >> 8) as u8, value as u8, (value >> 24) as u8, (value >> 16) as u8]
}

impl Vm {
    pub(super) fn is_pause_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(opcode, CC_RESUME_PAUSEBUTTON | IF_RESUME_PAUSEBUTTON | IF_CLOSE)
    }

    pub(super) fn execute_pause(
        &mut self,
        opcode: Opcode,
        secondary: bool,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let unsupported = || VmErrorKind::UnsupportedOpcode(opcode);
        let host = host.as_deref_mut().ok_or_else(unsupported)?;
        if opcode == IF_CLOSE {
            if let Ok(model) = host.components() {
                if model.has_open_interfaces() {
                    return Err(unsupported());
                }
                model.clear_dialogue_continue();
            }
            let outbox = host.outbox().map_err(|_| unsupported())?;
            outbox.push_packet(ClientPacket {
                opcode: CLOSE_INTERFACE,
                payload: Vec::new(),
            });
            outbox.interface_closes += 1;
            return Ok(());
        }
        let model: &mut ComponentModel = host.components().map_err(|_| unsupported())?;
        let target: ComponentId = if opcode == IF_RESUME_PAUSEBUTTON {
            let Some(hash) = self.ints.last().copied() else {
                return Err(VmErrorKind::StackUnderflow(StackKind::Int));
            };
            if let Err(ComponentError::Unavailable) = model.resolve(hash) {
                return Err(unsupported());
            }
            self.ints.pop();
            match model.resolve(hash) {
                Ok(Some(target)) => target,
                Ok(None) => return Err(VmErrorKind::Component(ComponentFailure::NoComponent)),
                Err(ComponentError::Failure(failure)) => {
                    return Err(VmErrorKind::Component(failure))
                }
                Err(ComponentError::Unavailable) => return Err(unsupported()),
            }
        } else {
            match model.hook(secondary) {
                Some(hook) => hook.component,
                None => return Err(VmErrorKind::Component(ComponentFailure::NoComponent)),
            }
        };
        let pressed = model.resume_pause_button(target).map_err(VmErrorKind::Component)?;
        if let Some(press) = pressed {
            let (hash, slot) = (press.hash, press.slot);
            let mut payload = int_middle(hash).to_vec();
            payload.push((slot as u8).wrapping_add(128));
            payload.push((slot >> 8) as u8);
            let outbox = host.outbox().map_err(|_| unsupported())?;
            outbox.console_lines.push(format!(
                "IF_CONTINUE interfaceId: {} componentId: {} slotId: {}",
                (hash as u32) >> 16,
                hash & 0xffff,
                slot
            ));
            outbox.push_packet(ClientPacket {
                opcode: IF_CONTINUE,
                payload,
            });
            if let Some(failure) = press.failure {
                return Err(VmErrorKind::Component(failure));
            }
        }
        Ok(())
    }
}
