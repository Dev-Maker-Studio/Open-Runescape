use super::{pop, push, Vm};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};

impl Vm {
    pub(super) fn is_bas_opcode(opcode: Opcode) -> bool {
        opcode == Opcode::BAS_GETANIM_READY
    }

    pub(super) fn execute_bas(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let id = pop(&mut self.ints, StackKind::Int)?;
        let definition = host
            .bas_definition(id)
            .map_err(|error| match error {
                HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                error => VmErrorKind::Host(error),
            })?
            .ok_or(VmErrorKind::Host(HostError::NoEntity))?;
        let value = match definition.random_stand_sequences.as_deref() {
            Some(sequences) if !sequences.is_empty() => {
                let chances = definition
                    .random_stand_chances
                    .as_deref()
                    .ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                let at = |index: usize| {
                    chances
                        .get(index)
                        .copied()
                        .ok_or(VmErrorKind::ArrayIndex(index as i32))
                };
                let mut best_index = 0;
                let mut best_chance = at(0)?;
                for index in 1..sequences.len() {
                    let chance = at(index)?;
                    if chance > best_chance {
                        best_index = index;
                        best_chance = chance;
                    }
                }
                sequences[best_index]
            }
            _ => definition.stand_sequence,
        };
        push(&mut self.ints, value, StackKind::Int)
    }
}
