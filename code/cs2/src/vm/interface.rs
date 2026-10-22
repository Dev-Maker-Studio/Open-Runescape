use super::{push, Vm};
use crate::{
    ComponentError, ComponentFailure, ComponentModel, HostError, Opcode, StackKind, VarHost,
    VmErrorKind,
};

fn component(error: ComponentFailure) -> VmErrorKind {
    VmErrorKind::Component(error)
}

fn model<'a>(
    host: &'a mut Option<&mut dyn VarHost>,
    opcode: Opcode,
) -> Result<&'a mut ComponentModel, VmErrorKind> {
    host.as_deref_mut()
        .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?
        .components()
        .map_err(|error| match error {
            HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
            error => VmErrorKind::Host(error),
        })
}

fn button_option(opcode: Opcode) -> Option<i32> {
    use Opcode::*;
    Some(match opcode {
        IF_DEBUG_BUTTON1 => 1,
        IF_DEBUG_BUTTON2 => 2,
        IF_DEBUG_BUTTON3 => 3,
        IF_DEBUG_BUTTON4 => 4,
        IF_DEBUG_BUTTON5 => 5,
        IF_DEBUG_BUTTON6 => 6,
        IF_DEBUG_BUTTON7 => 7,
        IF_DEBUG_BUTTON8 => 8,
        IF_DEBUG_BUTTON9 => 9,
        IF_DEBUG_BUTTON10 => 10,
        _ => return None,
    })
}

impl Vm {
    fn interface_ints(&mut self, count: usize) -> Result<Vec<i32>, VmErrorKind> {
        if self.ints.len() < count {
            return Err(VmErrorKind::StackUnderflow(StackKind::Int));
        }
        let base = self.ints.len() - count;
        let values = self.ints[base..].to_vec();
        self.ints.truncate(base);
        Ok(values)
    }

    fn interface_failure(
        &mut self,
        opcode: Opcode,
        popped: &[i32],
        error: ComponentError,
    ) -> VmErrorKind {
        match error {
            ComponentError::Unavailable => {
                self.ints.extend_from_slice(popped);
                VmErrorKind::UnsupportedOpcode(opcode)
            }
            ComponentError::Failure(failure) => component(failure),
        }
    }

    pub(super) fn execute_interface(
        &mut self,
        opcode: Opcode,
        operand: i32,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        model(host, opcode)?;
        match opcode {
            IF_CREATE => {
                let values = self.interface_ints(3)?;
                let result = model(host, opcode)?.entity_create(
                    operand == 1,
                    values[0],
                    values[1],
                    values[2],
                );
                result.map_err(|error| self.interface_failure(opcode, &values, error))
            }
            IF_DELETEALL => {
                let values = self.interface_ints(1)?;
                let components = model(host, opcode)?;
                let bound = components.entity_is_bound();
                let result = components.entity_delete_all(values[0]);
                result.map_err(|error| {
                    if !bound && matches!(error, ComponentError::Failure(_)) {
                        self.ints.extend_from_slice(&values);
                    }
                    self.interface_failure(opcode, &values, error)
                })
            }
            CC_LOADENTITYMODEL => {
                let values = self.interface_ints(2)?;
                let result = model(host, opcode)?.entity_load(operand == 1, values[0]);
                match result {
                    Ok(loaded) => push(&mut self.ints, i32::from(loaded), StackKind::Int),
                    Err(error) => {
                        if matches!(error, ComponentError::Failure(_)) {
                            self.ints.push(values[0]);
                        }
                        Err(self.interface_failure(opcode, &values, error))
                    }
                }
            }
            IF_DEBUG_TARGET => {
                let values = self.interface_ints(3)?;
                let hash = (values[0].wrapping_shl(16)) | values[1];
                let result = model(host, opcode)?.debug_target(hash, values[2]);
                result.map_err(|error| self.interface_failure(opcode, &values, error))
            }
            button => {
                let Some(option) = button_option(button) else {
                    return Err(VmErrorKind::UnsupportedOpcode(opcode));
                };
                let values = self.interface_ints(3)?;
                let hash = (values[0].wrapping_shl(16)) | values[1];
                let result = model(host, opcode)?.option_interact(option, hash, values[2]);
                let interaction = match result {
                    Ok(interaction) => interaction,
                    Err(error) => return Err(self.interface_failure(opcode, &values, error)),
                };
                if let Some(interaction) = interaction {
                    let sent = host
                        .as_deref_mut()
                        .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?
                        .interface_option(interaction, "");
                    if let Err(error) = sent {
                        return Err(match error {
                            HostError::Unavailable => {
                                self.ints.extend_from_slice(&values);
                                VmErrorKind::UnsupportedOpcode(opcode)
                            }
                            error => VmErrorKind::Host(error),
                        });
                    }
                }
                Ok(())
            }
        }
    }
}
