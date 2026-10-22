use super::{check_string, push, Vm};
use crate::{
    EnumDefinition, EnumFailure, EnumValue, HostError, Opcode, StackKind, VarHost, VmErrorKind,
};
use std::sync::Arc;

const STRING_TYPE: i32 = 115;

enum Output {
    Int(i32),
    Text(Arc<str>),
}

impl Vm {
    pub(super) fn execute_enum(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let (ints, strings, id_slot, checks_id) = match opcode {
            ENUM => (4, 0, 2, false),
            ENUM_STRING => (2, 0, 0, false),
            ENUM_HASOUTPUT => (3, 0, 1, true),
            ENUM_HASOUTPUT_STRING => (1, 1, 0, true),
            ENUM_GETOUTPUTCOUNT => (1, 0, 0, false),
            ENUM_GETREVERSECOUNT => (3, 0, 1, true),
            ENUM_GETREVERSECOUNT_STRING => (1, 1, 0, true),
            ENUM_GETREVERSEINDEX => (5, 0, 2, true),
            ENUM_GETREVERSEINDEX_STRING => (3, 1, 1, true),
            _ => unreachable!(),
        };
        let host = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let underflow = if self.ints.len() < ints {
            Some(StackKind::Int)
        } else if self.strings.len() < strings {
            Some(StackKind::String)
        } else {
            None
        };
        if let Some(kind) = underflow {
            return Err(match host.enum_definition(0) {
                Err(HostError::Unavailable) => VmErrorKind::UnsupportedOpcode(opcode),
                _ => VmErrorKind::StackUnderflow(kind),
            });
        }
        let int_base = self.ints.len() - ints;
        let string_base = self.strings.len() - strings;
        let arguments: Vec<i32> = self.ints[int_base..].to_vec();
        let text = self.strings.get(string_base).filter(|_| strings == 1);
        let text: Option<Option<Arc<str>>> = text.cloned();
        let enum_id = arguments[id_slot];
        let definition = if checks_id && enum_id == -1 {
            None
        } else {
            Some(match host.enum_definition(enum_id) {
                Ok(Some(definition)) => definition,
                Ok(None) => Arc::new(EnumDefinition::default()),
                Err(HostError::Unavailable) => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
                Err(error) => return Err(VmErrorKind::Host(error)),
            })
        };
        self.ints.truncate(int_base);
        self.strings.truncate(string_base);
        let output = match evaluate(opcode, &arguments, text, definition.as_deref()) {
            Ok(output) => output,
            Err(error) => {
                let kind = match error {
                    Failure::Enum(failure) => VmErrorKind::Enum(failure),
                    Failure::Operand => VmErrorKind::InvalidOperand,
                };
                let stale_slot = matches!(opcode, ENUM | ENUM_HASOUTPUT | ENUM_HASOUTPUT_STRING)
                    && matches!(
                        kind,
                        VmErrorKind::InvalidOperand
                            | VmErrorKind::Enum(
                                EnumFailure::ValueClass | EnumFailure::Inconsistent
                            )
                    );
                if stale_slot {
                    self.ints.push(arguments[0]);
                }
                return Err(kind);
            }
        };
        match output {
            Output::Int(value) => push(&mut self.ints, value, StackKind::Int),
            Output::Text(value) => {
                check_string(Some(&value), self.limits.max_string_bytes)?;
                push(&mut self.strings, Some(value), StackKind::String)
            }
        }
    }
}

enum Failure {
    Enum(EnumFailure),
    Operand,
}

impl From<EnumFailure> for Failure {
    fn from(failure: EnumFailure) -> Self {
        Self::Enum(failure)
    }
}

fn text_value(definition: &EnumDefinition, key: i32) -> Arc<str> {
    match definition.value(key) {
        Some(EnumValue::Text(text)) => Arc::clone(text),
        Some(EnumValue::Int(number)) => Arc::from(number.to_string()),
        None => Arc::clone(&definition.default_text),
    }
}

fn int_value(definition: &EnumDefinition, key: i32) -> Result<i32, EnumFailure> {
    match definition.value(key) {
        Some(EnumValue::Int(number)) => Ok(*number),
        Some(EnumValue::Text(_)) => Err(EnumFailure::ValueClass),
        None => Ok(definition.default_int),
    }
}

fn references(definition: &EnumDefinition, target: &EnumValue) -> Result<Vec<i32>, EnumFailure> {
    definition
        .references(target)
        .ok_or(EnumFailure::Inconsistent)
}

fn evaluate(
    opcode: Opcode,
    a: &[i32],
    text: Option<Option<Arc<str>>>,
    definition: Option<&EnumDefinition>,
) -> Result<Output, Failure> {
    use Opcode::*;
    let Some(d) = definition else {
        return Err(EnumFailure::NoEnum.into());
    };
    let text_target = || -> Result<EnumValue, Failure> {
        match &text {
            Some(Some(value)) => Ok(EnumValue::Text(Arc::clone(value))),
            _ => Err(Failure::Operand),
        }
    };
    let index_into = |refs: Vec<i32>, index: i32| -> Result<Output, Failure> {
        usize::try_from(index)
            .ok()
            .and_then(|index| refs.get(index).copied())
            .map(Output::Int)
            .ok_or(EnumFailure::ReverseIndex.into())
    };
    Ok(match opcode {
        ENUM => {
            if a[0] == d.key_type && a[1] == d.value_type {
                if a[1] == STRING_TYPE {
                    Output::Text(text_value(d, a[3]))
                } else {
                    Output::Int(int_value(d, a[3])?)
                }
            } else {
                return Err(EnumFailure::Lookup {
                    enum_id: a[2],
                    key: a[3],
                }
                .into());
            }
        }
        ENUM_STRING => Output::Text(text_value(d, a[1])),
        ENUM_HASOUTPUT => {
            if a[0] != d.value_type {
                return Err(EnumFailure::ValueType.into());
            }
            Output::Int(i32::from(!references(d, &EnumValue::Int(a[2]))?.is_empty()))
        }
        ENUM_HASOUTPUT_STRING => {
            if d.value_type != STRING_TYPE {
                return Err(EnumFailure::ValueType.into());
            }
            Output::Int(i32::from(!references(d, &text_target()?)?.is_empty()))
        }
        ENUM_GETOUTPUTCOUNT => Output::Int(d.size),
        ENUM_GETREVERSECOUNT => {
            if a[0] != d.value_type {
                return Err(EnumFailure::ValueType.into());
            }
            Output::Int(references(d, &EnumValue::Int(a[2]))?.len() as i32)
        }
        ENUM_GETREVERSECOUNT_STRING => {
            if d.value_type != STRING_TYPE {
                return Err(EnumFailure::ValueType.into());
            }
            Output::Int(references(d, &text_target()?)?.len() as i32)
        }
        ENUM_GETREVERSEINDEX => {
            if a[1] != d.key_type {
                return Err(EnumFailure::KeyType.into());
            }
            if a[0] != d.value_type {
                return Err(EnumFailure::ValueType.into());
            }
            index_into(references(d, &EnumValue::Int(a[3]))?, a[4])?
        }
        ENUM_GETREVERSEINDEX_STRING => {
            if a[0] != d.key_type {
                return Err(EnumFailure::KeyType.into());
            }
            if d.value_type != STRING_TYPE {
                return Err(EnumFailure::ValueType.into());
            }
            index_into(references(d, &text_target()?)?, a[2])?
        }
        _ => unreachable!(),
    })
}
