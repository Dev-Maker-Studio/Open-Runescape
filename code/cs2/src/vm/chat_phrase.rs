use super::{pair, pop, Vm};
use crate::outbox::{quick_chat_packet, QuickChatTarget};
use crate::{HostError, Opcode, QuickChatValueType, StackKind, VarHost, VmErrorKind};

#[derive(Clone, Debug, Default)]
pub(super) struct Phrase {
    id: i32,
    sizes: Option<Vec<Option<i32>>>,
    values: Option<Vec<i32>>,
}

fn null() -> VmErrorKind {
    VmErrorKind::Host(HostError::NoEntity)
}

fn lookup<T>(opcode: Opcode, result: Result<T, HostError>) -> Result<T, VmErrorKind> {
    result.map_err(|error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    })
}

impl Vm {
    pub fn chat_phrase(&self) -> Option<(i32, bool, Option<&[i32]>)> {
        self.chat_phrase
            .as_ref()
            .map(|phrase| (phrase.id, phrase.sizes.is_some(), phrase.values.as_deref()))
    }

    pub(super) fn is_chat_phrase_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            ACTIVECHATPHRASE_PREPARE
                | ACTIVECHATPHRASE_SETDYNAMICINT
                | ACTIVECHATPHRASE_SETDYNAMICOBJ
                | ACTIVECHATPHRASE_SENDPUBLIC
                | ACTIVECHATPHRASE_SENDCLAN
                | ACTIVECHATPHRASE_SENDCLANCHANNEL_AFFINED
                | ACTIVECHATPHRASE_SENDCLANCHANNEL_LISTENED
                | ACTIVECHATPHRASE_SENDPRIVATE
        )
    }

    pub(super) fn execute_chat_phrase(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        match opcode {
            ACTIVECHATPHRASE_PREPARE => {
                self.chat_phrase = Some(Phrase::default());
                let id = pop(&mut self.ints, StackKind::Int)?;
                let defs = lookup(opcode, host.quick_chat())?;
                let message = defs.message(id);
                let sizes = message.value_types.as_ref().map_or_else(Vec::new, |types| {
                    types
                        .iter()
                        .map(|kind| QuickChatValueType::get(*kind).map(|kind| kind.client_transmit_size))
                        .collect()
                });
                self.chat_phrase = Some(Phrase {
                    id,
                    sizes: Some(sizes),
                    values: Some(vec![0; message.value_type_count() as usize]),
                });
                Ok(())
            }
            ACTIVECHATPHRASE_SETDYNAMICINT | ACTIVECHATPHRASE_SETDYNAMICOBJ => {
                let (index, value) = pair(&mut self.ints, StackKind::Int)?;
                let values = self
                    .chat_phrase
                    .as_mut()
                    .and_then(|phrase| phrase.values.as_mut())
                    .ok_or_else(null)?;
                let slot = usize::try_from(index)
                    .ok()
                    .and_then(|index| values.get_mut(index))
                    .ok_or_else(|| {
                        VmErrorKind::Host(HostError::State("array index out of bounds".to_string()))
                    })?;
                *slot = value;
                Ok(())
            }
            _ => {
                let recipient = if opcode == ACTIVECHATPHRASE_SENDPRIVATE {
                    Some(pop(&mut self.strings, StackKind::String)?)
                } else {
                    None
                };
                host.outbox()
                    .map_err(|error| lookup::<()>(opcode, Err(error)).unwrap_err())?;
                let target = match (&recipient, opcode) {
                    (Some(None), _) => return Err(null()),
                    (Some(Some(name)), _) => QuickChatTarget::Private(name),
                    (None, ACTIVECHATPHRASE_SENDPUBLIC) => QuickChatTarget::Channel(0),
                    (None, ACTIVECHATPHRASE_SENDCLAN) => QuickChatTarget::Channel(1),
                    (None, ACTIVECHATPHRASE_SENDCLANCHANNEL_AFFINED) => QuickChatTarget::Channel(2),
                    _ => QuickChatTarget::Channel(3),
                };
                let Some(phrase) = self.chat_phrase.as_ref() else {
                    quick_chat_packet(target, None, &[], &[]).map_err(VmErrorKind::Host)?;
                    return Err(null());
                };
                let (Some(sizes), Some(values)) = (phrase.sizes.as_ref(), phrase.values.as_ref())
                else {
                    quick_chat_packet(target, Some(phrase.id), &[], &[]).map_err(VmErrorKind::Host)?;
                    return Err(null());
                };
                let packet = quick_chat_packet(target, Some(phrase.id), sizes, values)
                    .map_err(VmErrorKind::Host)?;
                lookup(opcode, host.outbox())?.push_packet(packet);
                Ok(())
            }
        }
    }
}
