use super::{check_string, push, Vm};
use crate::{HostError, Opcode, QueryParam, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn lookup<T>(opcode: Opcode, result: Result<T, HostError>) -> Result<T, VmErrorKind> {
    result.map_err(|error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    })
}

impl Vm {
    pub(super) fn execute_inventory(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        lookup(opcode, host.items().map(|_| ()))?;
        let (ints, strings) = match opcode {
            INV_GETOBJ | INV_GETNUM | INV_TOTAL | INVOTHER_GETOBJ | INVOTHER_GETNUM
            | INVOTHER_TOTAL | INV_GETTOOLTIP | INV_OTHER_GETTOOLTIP | INV_STOCKBASE
            | INV_TOTALPARAM | INV_TOTALPARAM_STACK => (2, 0),
            OC_FIND => (1, 1),
            INV_SIZE | INV_FREESPACE => (1, 0),
            ITEM_FIND_PARAMINT => (3, 1),
            ITEM_FIND_PARAMSTR => (2, 2),
            OC_FINDNEXT | OC_FINDRESTART => (0, 0),
            _ => unreachable!(),
        };
        if self.ints.len() < ints {
            return Err(VmErrorKind::StackUnderflow(Int));
        }
        if self.strings.len() < strings {
            return Err(VmErrorKind::StackUnderflow(Text));
        }
        let int_base = self.ints.len() - ints;
        let string_base = self.strings.len() - strings;
        let a: Vec<i32> = self.ints[int_base..].to_vec();
        let s: Vec<Option<Arc<str>>> = self.strings[string_base..].to_vec();
        self.ints.truncate(int_base);
        self.strings.truncate(string_base);
        let null = || VmErrorKind::Host(HostError::NoEntity);
        let text_result = match opcode {
            INV_GETTOOLTIP | INV_OTHER_GETTOOLTIP => {
                let model = lookup(opcode, host.items())?;
                Some(model.tooltip_at(a[0], a[1], opcode == INV_OTHER_GETTOOLTIP))
            }
            _ => None,
        };
        if let Some(value) = text_result {
            check_string(Some(&value), self.limits.max_string_bytes)?;
            return push(&mut self.strings, Some(value), Text);
        }
        let value = match opcode {
            INV_GETOBJ => lookup(opcode, host.items())?.item_at(a[0], a[1], false),
            INVOTHER_GETOBJ => lookup(opcode, host.items())?.item_at(a[0], a[1], true),
            INV_GETNUM => lookup(opcode, host.items())?.amount_at(a[0], a[1], false),
            INVOTHER_GETNUM => lookup(opcode, host.items())?.amount_at(a[0], a[1], true),
            INV_TOTAL => lookup(opcode, host.items())?.total(a[0], a[1], false),
            INVOTHER_TOTAL => lookup(opcode, host.items())?.total(a[0], a[1], true),
            INV_SIZE => lookup(opcode, host.items())?
                .container_type(a[0])
                .max_item_size,
            INV_FREESPACE => lookup(opcode, host.items())?.free_slots(a[0]),
            INV_STOCKBASE => lookup(opcode, host.items())?
                .stock_base(a[0], a[1])
                .map_err(VmErrorKind::Host)?,
            INV_TOTALPARAM | INV_TOTALPARAM_STACK => {
                let default = lookup(opcode, host.param_definition(a[1]))?
                    .map(|definition| definition.default_int)
                    .unwrap_or(0);
                match lookup(opcode, host.items())?.param_total(
                    a[0],
                    a[1],
                    default,
                    opcode == INV_TOTALPARAM_STACK,
                ) {
                    Ok(total) => total,
                    Err(error) => {
                        self.ints.push(a[0]);
                        return Err(VmErrorKind::Host(error));
                    }
                }
            }
            OC_FINDNEXT => lookup(opcode, host.items())?.find_next(),
            OC_FINDRESTART => {
                lookup(opcode, host.items())?.find_restart();
                return Ok(());
            }
            OC_FIND => {
                let name = s[0].clone().ok_or_else(null)?;
                lookup(opcode, host.items())?
                    .query(&name, a[0] == 1, QueryParam::None)
                    .map_err(VmErrorKind::Host)?;
                lookup(opcode, host.items())?.query.count
            }
            ITEM_FIND_PARAMINT => {
                let name = s[0].clone().ok_or_else(null)?;
                let (tradeable, param_id, query) = (a[0], a[1], a[2]);
                self.run_query(opcode, host, &name, tradeable == 1, param_id, query)?
            }
            ITEM_FIND_PARAMSTR => {
                let name = s[0].clone().ok_or_else(null)?;
                let wanted = s[1].clone().ok_or_else(null)?;
                let (tradeable, param_id) = (a[0], a[1]);
                self.run_text_query(opcode, host, &name, tradeable == 1, param_id, &wanted)?
            }
            _ => unreachable!(),
        };
        push(&mut self.ints, value, Int)
    }

    fn run_query(
        &mut self,
        opcode: Opcode,
        host: &mut dyn VarHost,
        name: &str,
        tradeable_only: bool,
        param_id: i32,
        query: i32,
    ) -> Result<i32, VmErrorKind> {
        let definition = if param_id != -1 {
            Some(lookup(opcode, host.param_definition(param_id))?.unwrap_or_default())
        } else {
            None
        };
        let model = lookup(opcode, host.items())?;
        match definition {
            Some(definition) if definition.is_string => {}
            Some(definition) => model
                .query(
                    name,
                    tradeable_only,
                    QueryParam::Int {
                        id: param_id,
                        default: definition.default_int,
                        query,
                    },
                )
                .map_err(VmErrorKind::Host)?,
            None => model
                .query(name, tradeable_only, QueryParam::None)
                .map_err(VmErrorKind::Host)?,
        }
        Ok(model.query.count)
    }

    fn run_text_query(
        &mut self,
        opcode: Opcode,
        host: &mut dyn VarHost,
        name: &str,
        tradeable_only: bool,
        param_id: i32,
        wanted: &str,
    ) -> Result<i32, VmErrorKind> {
        let definition = if param_id != -1 {
            Some(lookup(opcode, host.param_definition(param_id))?.unwrap_or_default())
        } else {
            None
        };
        let model = lookup(opcode, host.items())?;
        match definition {
            Some(definition) if !definition.is_string => {}
            Some(definition) => model
                .query(
                    name,
                    tradeable_only,
                    QueryParam::Text {
                        id: param_id,
                        default: &definition.type_name,
                        query: wanted,
                    },
                )
                .map_err(VmErrorKind::Host)?,
            None => model
                .query(name, tradeable_only, QueryParam::None)
                .map_err(VmErrorKind::Host)?,
        }
        Ok(model.query.count)
    }
}
