use super::world::host_error;
use super::{check_string, push, Vm};
use crate::quest::indexed;
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn out_of_bounds(index: i32) -> VmErrorKind {
    VmErrorKind::Host(HostError::State(format!("array index {index} out of bounds")))
}

fn count<T>(values: &Option<Vec<T>>) -> i32 {
    values.as_ref().map_or(0, |values| values.len() as i32)
}

impl Vm {
    pub(super) fn execute_quest(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        let host: &dyn VarHost = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let defs = host.quest_defs().map_err(&unavailable)?;
        let limit = self.limits.max_string_bytes;
        if opcode == QUEST_PARAM {
            let [quest_id, param_id] = self.pop_arguments()?;
            let definition = host
                .param_definition(param_id)
                .map_err(&unavailable)?
                .unwrap_or_default();
            if definition.is_string {
                let default = Arc::clone(&definition.type_name);
                let value = if quest_id == -1 {
                    default
                } else {
                    match defs.list(quest_id).map_err(&unavailable)?.param_text(param_id) {
                        None => default,
                        Some(Ok(text)) => text,
                        Some(Err(())) => return Err(VmErrorKind::InvalidOperand),
                    }
                };
                check_string(Some(&value), limit)?;
                return push(&mut self.strings, Some(value), Text);
            }
            let value = if quest_id == -1 {
                definition.default_int
            } else {
                match defs.list(quest_id).map_err(&unavailable)?.param_int(param_id) {
                    None => definition.default_int,
                    Some(Ok(number)) => number,
                    Some(Err(())) => return Err(VmErrorKind::InvalidOperand),
                }
            };
            return push(&mut self.ints, value, Int);
        }
        let indexed_opcode = matches!(
            opcode,
            QUEST_QUESTREQ
                | QUEST_QUESTREQ_MET
                | QUEST_STATREQ_STAT
                | QUEST_STATREQ_LEVEL
                | QUEST_STATREQ_MET
                | QUEST_VARPREQ_DESC
                | QUEST_VARPREQ_MET
                | QUEST_VARBITREQ_DESC
                | QUEST_VARBITREQ_MET
        );
        let (quest_id, index) = if indexed_opcode {
            let [quest_id, index] = self.pop_arguments()?;
            (quest_id, index)
        } else {
            let [quest_id] = self.pop_arguments()?;
            (quest_id, 0)
        };
        let quest = defs.list(quest_id).map_err(&unavailable)?;
        let state = |error| unavailable(error);
        let number = match opcode {
            QUEST_GETNAME | QUEST_GETSORTNAME => {
                let text = if opcode == QUEST_GETNAME {
                    quest.name.clone()
                } else {
                    quest.sort_name.clone()
                };
                check_string(text.as_deref(), limit)?;
                return push(&mut self.strings, text, Text);
            }
            QUEST_VARPREQ_DESC | QUEST_VARBITREQ_DESC => {
                let requirements = if opcode == QUEST_VARPREQ_DESC {
                    &quest.varp_requirements
                } else {
                    &quest.varbit_requirements
                };
                let name = indexed(requirements, index)
                    .map(|requirement| Arc::clone(&requirement.name))
                    .ok_or_else(|| out_of_bounds(index))?;
                check_string(Some(&name), limit)?;
                return push(&mut self.strings, Some(name), Text);
            }
            QUEST_TYPE => quest.kind,
            QUEST_GETDIFFICULTY => quest.difficulty,
            QUEST_GETMEMBERS => i32::from(quest.members),
            QUEST_POINTS => quest.quest_point_reward,
            QUEST_POINTSREQ => quest.quest_point_requirement,
            QUEST_QUESTREQ_COUNT => count(&quest.prerequisites),
            QUEST_STATREQ_COUNT => count(&quest.level_requirements),
            QUEST_VARPREQ_COUNT => count(&quest.varp_requirements),
            QUEST_VARBITREQ_COUNT => count(&quest.varbit_requirements),
            QUEST_QUESTREQ => *indexed(&quest.prerequisites, index)
                .ok_or_else(|| out_of_bounds(index))?,
            QUEST_STATREQ_STAT => indexed(&quest.level_requirements, index)
                .ok_or_else(|| out_of_bounds(index))?[0],
            QUEST_STATREQ_LEVEL => indexed(&quest.level_requirements, index)
                .ok_or_else(|| out_of_bounds(index))?[1],
            QUEST_QUESTREQ_MET => i32::from(defs.prerequisite_met(host, quest, index).map_err(state)?),
            QUEST_POINTSREQ_MET => i32::from(defs.points_requirement_met(host, quest).map_err(state)?),
            QUEST_STATREQ_MET => i32::from(quest.level_requirement_met(host, index).map_err(state)?),
            QUEST_VARPREQ_MET => i32::from(quest.varp_requirement_met(host, index).map_err(state)?),
            QUEST_VARBITREQ_MET => {
                i32::from(quest.varbit_requirement_met(host, index).map_err(state)?)
            }
            QUEST_ALLREQMET => i32::from(defs.all_requirements_met(host, quest).map_err(state)?),
            QUEST_STARTED => i32::from(quest.is_started(host).map_err(state)?),
            QUEST_FINISHED => i32::from(quest.is_complete(host).map_err(state)?),
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        };
        push(&mut self.ints, number, Int)
    }

    pub(super) fn is_quest_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            QUEST_GETNAME
                | QUEST_GETSORTNAME
                | QUEST_TYPE
                | QUEST_GETDIFFICULTY
                | QUEST_GETMEMBERS
                | QUEST_POINTS
                | QUEST_QUESTREQ_COUNT
                | QUEST_QUESTREQ
                | QUEST_QUESTREQ_MET
                | QUEST_POINTSREQ
                | QUEST_POINTSREQ_MET
                | QUEST_STATREQ_COUNT
                | QUEST_STATREQ_STAT
                | QUEST_STATREQ_LEVEL
                | QUEST_STATREQ_MET
                | QUEST_VARPREQ_COUNT
                | QUEST_VARPREQ_DESC
                | QUEST_VARPREQ_MET
                | QUEST_VARBITREQ_COUNT
                | QUEST_VARBITREQ_DESC
                | QUEST_VARBITREQ_MET
                | QUEST_ALLREQMET
                | QUEST_STARTED
                | QUEST_FINISHED
                | QUEST_PARAM
        )
    }
}
