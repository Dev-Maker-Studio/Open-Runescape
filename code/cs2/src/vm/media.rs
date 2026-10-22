use super::{pop, push, Vm};
use crate::fonts::Surroundings;
use crate::{
    ComponentError, ComponentFailure, ComponentModel, HostError, Opcode, StackKind, VarHost,
    VmErrorKind,
};

fn lookup<T>(opcode: Opcode, result: Result<T, HostError>) -> Result<T, VmErrorKind> {
    result.map_err(|error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    })
}

impl Vm {
    pub(super) fn is_media_query_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            CC_GETGRAPHICDIMENSIONS
                | IF_GETGRAPHICDIMENSIONS
                | CC_GETCHARINDEXATPOS
                | IF_GETCHARINDEXATPOS
                | CC_GETCHARPOSATINDEX
                | IF_GETCHARPOSATINDEX
        )
    }

    pub(super) fn execute_media_query(
        &mut self,
        opcode: Opcode,
        secondary: bool,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let by_hash = matches!(
            opcode,
            IF_GETGRAPHICDIMENSIONS | IF_GETCHARINDEXATPOS | IF_GETCHARPOSATINDEX
        );
        let arguments = match opcode {
            CC_GETGRAPHICDIMENSIONS | IF_GETGRAPHICDIMENSIONS => 0,
            CC_GETCHARINDEXATPOS | IF_GETCHARINDEXATPOS => 2,
            _ => 1,
        };
        let view = {
            let model: &mut ComponentModel = host
                .components()
                .map_err(|_| VmErrorKind::UnsupportedOpcode(opcode))?;
            let target = if by_hash {
                let Some(hash) = self.ints.last().copied() else {
                    return Err(VmErrorKind::StackUnderflow(StackKind::Int));
                };
                if let Err(ComponentError::Unavailable) = model.resolve(hash) {
                    return Err(VmErrorKind::UnsupportedOpcode(opcode));
                }
                self.ints.pop();
                match model.resolve(hash) {
                    Ok(Some(target)) => target,
                    Ok(None) => return Err(VmErrorKind::Component(ComponentFailure::NoComponent)),
                    Err(ComponentError::Failure(failure)) => {
                        return Err(VmErrorKind::Component(failure))
                    }
                    Err(ComponentError::Unavailable) => {
                        return Err(VmErrorKind::UnsupportedOpcode(opcode))
                    }
                }
            } else {
                match model.hook(secondary) {
                    Some(hook) => hook.component,
                    None => return Err(VmErrorKind::Component(ComponentFailure::NoComponent)),
                }
            };
            model.text_view(target)
        };
        if self.ints.len() < arguments {
            return Err(VmErrorKind::StackUnderflow(StackKind::Int));
        }
        let host: &dyn VarHost = host;
        match opcode {
            CC_GETGRAPHICDIMENSIONS | IF_GETGRAPHICDIMENSIONS => {
                let size = lookup(opcode, host.sprite_dimensions(view.sprite_id))?.unwrap_or((-1, -1));
                push(&mut self.ints, size.0, StackKind::Int)?;
                push(&mut self.ints, size.1, StackKind::Int)
            }
            CC_GETCHARINDEXATPOS | IF_GETCHARINDEXATPOS => {
                let y = pop(&mut self.ints, StackKind::Int)?;
                let x = pop(&mut self.ints, StackKind::Int)?;
                let font = lookup(opcode, host.font_metrics(view.font_id))?
                    .ok_or(VmErrorKind::Component(ComponentFailure::NoComponent))?;
                let text: Vec<u16> = view.text.encode_utf16().collect();
                let mut around = Surroundings::new(host, true);
                let index =
                    font.index_at_position(&text, view.width, view.line_spacing, x, y, &mut around);
                if let Some(error) = around.error {
                    return Err(match error {
                        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                        error => VmErrorKind::Host(error),
                    });
                }
                let index = index.map_err(VmErrorKind::Component)?;
                push(&mut self.ints, index, StackKind::Int)
            }
            _ => {
                let index = pop(&mut self.ints, StackKind::Int)?;
                let font = lookup(opcode, host.font_metrics(view.font_id))?
                    .ok_or(VmErrorKind::Component(ComponentFailure::NoComponent))?;
                let text: Vec<u16> = view.text.encode_utf16().collect();
                let mut around = Surroundings::new(host, true);
                let position =
                    font.position_at_index(&text, view.width, view.line_spacing, index, &mut around);
                if let Some(error) = around.error {
                    return Err(match error {
                        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                        error => VmErrorKind::Host(error),
                    });
                }
                let Some((x, y)) = position else {
                    push(&mut self.ints, index, StackKind::Int)?;
                    return Err(VmErrorKind::Component(ComponentFailure::NoComponent));
                };
                push(&mut self.ints, x, StackKind::Int)?;
                push(&mut self.ints, y, StackKind::Int)
            }
        }
    }
}
