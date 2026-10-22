use super::{pop, push, Vm};
use crate::{ComponentError, HostError, Opcode, StackKind, VarHost, VmErrorKind};

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

impl Vm {
    pub(super) fn is_appearance_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            BASEIDKIT | BASECOLOUR | SETGENDER | SETOBJ | GET_NPC_VISLEVEL
        )
    }

    pub(super) fn execute_appearance(
        &mut self,
        opcode: Opcode,
        secondary: bool,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        if opcode == GET_NPC_VISLEVEL {
            let component = pop(&mut self.ints, Int)?;
            let model = host.components().map_err(&unavailable)?;
            let loaded = model.entity_load(secondary, component);
            return match loaded {
                Ok(loaded) => push(&mut self.ints, i32::from(loaded), Int),
                Err(ComponentError::Unavailable) => {
                    self.ints.push(component);
                    Err(VmErrorKind::UnsupportedOpcode(opcode))
                }
                Err(ComponentError::Failure(failure)) => Err(VmErrorKind::Component(failure)),
            };
        }
        host.appearance_state_mut().map_err(&unavailable)?;
        let result = match opcode {
            SETOBJ => {
                let item = pop(&mut self.ints, Int)?;
                let slot = pop(&mut self.ints, Int)?;
                host.appearance_state_mut()
                    .map_err(&unavailable)?
                    .set_item(slot, item)
            }
            BASEIDKIT => {
                let id = pop(&mut self.ints, Int)?;
                let part = pop(&mut self.ints, Int)?;
                host.appearance_state_mut()
                    .map_err(&unavailable)?
                    .set_base_identikit(part, id)
            }
            BASECOLOUR => {
                let color = pop(&mut self.ints, Int)?;
                let slot = pop(&mut self.ints, Int)?;
                host.appearance_state_mut()
                    .map_err(&unavailable)?
                    .set_base_colour(slot, color)
            }
            SETGENDER => {
                let female = pop(&mut self.ints, Int)? != 0;
                host.appearance_state_mut()
                    .map_err(&unavailable)?
                    .set_gender(female)
            }
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        };
        result.map_err(VmErrorKind::Host)
    }
}
