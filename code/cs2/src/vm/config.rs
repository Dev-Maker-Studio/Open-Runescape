use super::{push, Vm};
use crate::{
    ConfigKind, EnumValue, HostError, ItemDefinition, Opcode, ParamDefinition, ParamTable,
    StackKind, VarHost, VmErrorKind,
};
use std::sync::Arc;

enum Output {
    Int(i32),
    Text(Arc<str>),
}

fn lookup<T>(opcode: Opcode, result: Result<T, HostError>) -> Result<T, VmErrorKind> {
    result.map_err(|error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    })
}

fn parameter(
    table: Option<&ParamTable>,
    definition: &ParamDefinition,
    key: i32,
) -> Result<Output, VmErrorKind> {
    let entry = table.and_then(|table| table.get(&key));
    if definition.is_string {
        match entry {
            None => Ok(Output::Text(Arc::clone(&definition.type_name))),
            Some(EnumValue::Text(text)) => Ok(Output::Text(Arc::clone(text))),
            Some(EnumValue::Int(_)) => Err(VmErrorKind::InvalidOperand),
        }
    } else {
        match entry {
            None => Ok(Output::Int(definition.default_int)),
            Some(EnumValue::Int(number)) => Ok(Output::Int(*number)),
            Some(EnumValue::Text(_)) => Err(VmErrorKind::InvalidOperand),
        }
    }
}

fn action(actions: &[Option<Arc<str>>], op: i32) -> Arc<str> {
    if (1..=5).contains(&op) {
        if let Some(Some(text)) = actions.get((op - 1) as usize) {
            return Arc::clone(text);
        }
    }
    Arc::from("")
}

impl Vm {
    fn leave_stale_slot(&mut self, is_string: bool, stale: i32) {
        if is_string {
            self.strings.push(None);
        } else {
            self.ints.push(stale);
        }
    }

    pub(super) fn execute_config(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let ints = match opcode {
            OC_OP | OC_IOP | OC_ICURSOR | OC_PARAM | NC_PARAM | STRUCT_PARAM | SEQ_PARAM => 2,
            _ => 1,
        };
        let host = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        if self.ints.len() < ints {
            return Err(match host.item_definition(0) {
                Err(HostError::Unavailable) => VmErrorKind::UnsupportedOpcode(opcode),
                _ => VmErrorKind::StackUnderflow(StackKind::Int),
            });
        }
        let base = self.ints.len() - ints;
        let arguments: Vec<i32> = self.ints[base..].to_vec();
        let id = arguments[0];
        let output = match opcode {
            NC_PARAM | STRUCT_PARAM | SEQ_PARAM => {
                let kind = match opcode {
                    NC_PARAM => ConfigKind::Npc,
                    STRUCT_PARAM => ConfigKind::Struct,
                    _ => ConfigKind::Seq,
                };
                let definition = lookup(opcode, host.param_definition(arguments[1]))?
                    .unwrap_or_default();
                let table = lookup(opcode, host.config_params(kind, id))?;
                self.ints.truncate(base);
                match parameter(table.as_deref(), &definition, arguments[1]) {
                    Ok(output) => output,
                    Err(error) => {
                        self.leave_stale_slot(definition.is_string, id);
                        return Err(error);
                    }
                }
            }
            _ => {
                let definition: Arc<ItemDefinition> =
                    lookup(opcode, host.item_definition(id))?.unwrap_or_default();
                match opcode {
                    OC_PARAM => {
                        let parameter_definition =
                            lookup(opcode, host.param_definition(arguments[1]))?
                                .unwrap_or_default();
                        self.ints.truncate(base);
                        match parameter(
                            definition.params.as_ref(),
                            &parameter_definition,
                            arguments[1],
                        ) {
                            Ok(output) => output,
                            Err(error) => {
                                self.leave_stale_slot(parameter_definition.is_string, id);
                                return Err(error);
                            }
                        }
                    }
                    OC_MINIMENU_COLOUR => {
                        let colours = if definition.has_tooltip_color {
                            None
                        } else {
                            Some(lookup(opcode, host.interaction_colours())?)
                        };
                        self.ints.truncate(base);
                        Output::Int(match colours {
                            None => definition.tooltip_color,
                            Some(colours) if definition.members_only => colours.members,
                            Some(colours) => colours.free,
                        })
                    }
                    _ => {
                        self.ints.truncate(base);
                        match opcode {
                            OC_NAME => Output::Text(Arc::clone(&definition.name)),
                            OC_OP => Output::Text(action(
                                &definition.ground_actions,
                                arguments[1],
                            )),
                            OC_IOP => Output::Text(action(
                                &definition.inventory_actions,
                                arguments[1],
                            )),
                            OC_COST => Output::Int(definition.value),
                            OC_STACKABLE => Output::Int(i32::from(definition.stackable == 1)),
                            OC_CERT => Output::Int(
                                if definition.cert_template_id == -1 && definition.cert_id >= 0 {
                                    definition.cert_id
                                } else {
                                    id
                                },
                            ),
                            OC_UNCERT => Output::Int(
                                if definition.cert_template_id >= 0 && definition.cert_id >= 0 {
                                    definition.cert_id
                                } else {
                                    id
                                },
                            ),
                            OC_WEARPOS => Output::Int(definition.wear_pos),
                            OC_WEARPOS2 => Output::Int(definition.wear_pos2),
                            OC_WEARPOS3 => Output::Int(definition.wear_pos3),
                            OC_MEMBERS => Output::Int(i32::from(definition.members_only)),
                            OC_MULTISTACKSIZE => Output::Int(definition.multi_stack_size),
                            OC_MINIMENU_COLOUR_OVERRIDDEN => {
                                Output::Int(i32::from(definition.has_tooltip_color))
                            }
                            OC_ICURSOR => {
                                let op = arguments[1] - 1;
                                Output::Int(if op == definition.cursor_op1 {
                                    definition.cursor_id1
                                } else if op == definition.cursor_op2 {
                                    definition.cursor_id2
                                } else {
                                    -1
                                })
                            }
                            _ => unreachable!(),
                        }
                    }
                }
            }
        };
        match output {
            Output::Int(value) => push(&mut self.ints, value, StackKind::Int),
            Output::Text(value) => push(&mut self.strings, Some(value), StackKind::String),
        }
    }
}
