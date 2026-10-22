use super::{check_string, push, Vm};
use crate::components::OpenIdMiss;
use crate::{
    ComponentError, ComponentFailure, ComponentModel, HostError, Opcode, StackKind, VarHost,
    VmErrorKind,
};

fn component(error: ComponentFailure) -> VmErrorKind {
    VmErrorKind::Component(error)
}

impl Vm {
    fn query_ints(&mut self, count: usize) -> Result<Vec<i32>, VmErrorKind> {
        if self.ints.len() < count {
            return Err(VmErrorKind::StackUnderflow(StackKind::Int));
        }
        let base = self.ints.len() - count;
        let values = self.ints[base..].to_vec();
        self.ints.truncate(base);
        Ok(values)
    }

    fn query_text(&mut self, text: std::sync::Arc<str>) -> Result<(), VmErrorKind> {
        check_string(Some(&text), self.limits.max_string_bytes)?;
        push(&mut self.strings, Some(text), StackKind::String)
    }

    fn query_parent_by_hash(
        &mut self,
        opcode: Opcode,
        model: &ComponentModel,
    ) -> Result<(), VmErrorKind> {
        let hash = self.query_ints(1)?[0];
        let target = match model.resolve(hash) {
            Ok(target) => target,
            Err(ComponentError::Unavailable) => {
                self.ints.push(hash);
                return Err(VmErrorKind::UnsupportedOpcode(opcode));
            }
            Err(ComponentError::Failure(failure)) => return Err(component(failure)),
        };
        let Some(target) = target else {
            return Err(component(if model.in_range(hash) {
                ComponentFailure::NoComponent
            } else {
                ComponentFailure::Index
            }));
        };
        let value = model.parent_layer(hash >> 16, target).map_err(component)?;
        push(&mut self.ints, value, StackKind::Int)
    }

    pub(super) fn execute_query(
        &mut self,
        opcode: Opcode,
        operand: i32,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let model = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?
            .components()
            .map_err(|error| match error {
                HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                error => VmErrorKind::Host(error),
            })?;
        match opcode {
            CC_GETPARENTLAYER => {
                let hook = model
                    .hook(operand == 1)
                    .ok_or(component(ComponentFailure::NoComponent))?;
                let value = model
                    .parent_layer(hook.interface, hook.component)
                    .map_err(component)?;
                push(&mut self.ints, value, StackKind::Int)
            }
            IF_GETPARENTLAYER => self.query_parent_by_hash(opcode, model),
            IF_GET_GAMESCREEN => {
                let value = model.game_screen_hash();
                push(&mut self.ints, value, StackKind::Int)
            }
            IF_DEBUG_GETOPENIFCOUNT => {
                let value = model.open_count();
                push(&mut self.ints, value, StackKind::Int)
            }
            IF_DEBUG_GETOPENIFID => {
                let index = self.query_ints(1)?[0];
                match model.open_id_at(index) {
                    Ok(value) => push(&mut self.ints, value, StackKind::Int),
                    Err(miss) => {
                        if matches!(miss, OpenIdMiss::Null) {
                            self.ints.push(index);
                        }
                        Err(component(ComponentFailure::NoComponent))
                    }
                }
            }
            IF_DEBUG_GETNAME => {
                let interface = self.query_ints(1)?[0];
                let text = model.debug_interface_name(interface).map_err(component)?;
                self.query_text(text)
            }
            IF_DEBUG_GETCOMCOUNT => {
                let interface = self.query_ints(1)?[0];
                let value = model.debug_component_count(interface).map_err(component)?;
                push(&mut self.ints, value, StackKind::Int)
            }
            IF_DEBUG_GETCOMNAME => {
                let values = self.query_ints(2)?;
                let text = model
                    .debug_component_name(values[0], values[1])
                    .map_err(component)?;
                self.query_text(text)
            }
            IF_DEBUG_GETSERVERTRIGGERS => {
                let values = self.query_ints(2)?;
                match model.debug_server_triggers(values[0], values[1]) {
                    Ok(value) => push(&mut self.ints, value, StackKind::Int),
                    Err(error) => {
                        if model.debug_interface_loaded(values[0]) {
                            self.ints.push(values[0]);
                        }
                        Err(component(error))
                    }
                }
            }
            _ => Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
    }
}
