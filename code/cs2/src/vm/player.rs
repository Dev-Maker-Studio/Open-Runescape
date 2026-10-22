use super::{pop, push, Vm};
use crate::{HostError, Opcode, Operand, StackKind, VarHost, VmErrorKind};

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

impl Vm {
    pub(super) fn execute_player(
        &mut self,
        opcode: Opcode,
        operand: &Operand,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let host = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        match opcode {
            GET_VARN_OLD | GET_VARNBIT_OLD => {
                let Operand::Int(id) = operand else {
                    return Err(VmErrorKind::InvalidOperand);
                };
                let npc = host.npc_state().map_err(&unavailable)?;
                let value = if opcode == GET_VARN_OLD {
                    npc.varn(*id)
                } else {
                    npc.varn_bit(*id)
                };
                push(&mut self.ints, value, Int)
            }
            NPC_TYPE => {
                let npc = host.npc_state().map_err(&unavailable)?;
                push(&mut self.ints, npc.npc_type().map_err(&unavailable)?, Int)
            }
            GET_NPC_STAT => {
                let npc = host.npc_state();
                if matches!(npc, Err(HostError::Unavailable)) {
                    return Err(VmErrorKind::UnsupportedOpcode(opcode));
                }
                let index = pop(&mut self.ints, Int)?;
                let (maximum, current) = npc
                    .and_then(|npc| npc.stat_pair(index))
                    .map_err(&unavailable)?;
                push(&mut self.ints, maximum, Int)?;
                push(&mut self.ints, current, Int)
            }
            STAT | STAT_BASE | STAT_VISIBLE_XP => {
                let player = host.player_state().map_err(&unavailable)?;
                let skill = pop(&mut self.ints, Int)?;
                let value = match opcode {
                    STAT => player.stat(skill),
                    STAT_BASE => player.stat_base(skill),
                    _ => player.stat_xp(skill),
                }
                .map_err(&unavailable)?;
                push(&mut self.ints, value, Int)
            }
            _ => {
                let player = host.player_state().map_err(&unavailable)?;
                let value = match opcode {
                    MAP_MEMBERS => i32::from(player.member_world),
                    STAFFMODLEVEL => player.staff_mod_level(),
                    REBOOTTIMER => player.reboot_timer,
                    MAP_WORLD => player.world_id().map_err(&unavailable)?,
                    RUNENERGY_VISIBLE => player.run_energy,
                    RUNWEIGHT_VISIBLE => player.run_weight,
                    PLAYERMOD => i32::from(player.is_player_mod()),
                    PLAYERMODLEVEL => player.player_mod_level(),
                    PLAYERMEMBER => i32::from(player.is_member),
                    COMLEVEL_ACTIVE => player.my_player().map_err(&unavailable)?.combat_level,
                    GENDER => i32::from(
                        player
                            .my_player()
                            .map_err(&unavailable)?
                            .female
                            .unwrap_or(false),
                    ),
                    PLAYERDEMO => 0,
                    GET_SELFYANGLE => player.my_player().map_err(&unavailable)?.yaw_rotation >> 3 & 2047,
                    SELF_PLAYER_UID => player.my_player_index,
                    _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
                };
                push(&mut self.ints, value, Int)
            }
        }
    }

    pub(super) fn is_player_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            GET_VARN_OLD
                | GET_VARNBIT_OLD
                | NPC_TYPE
                | GET_NPC_STAT
                | STAT
                | STAT_BASE
                | STAT_VISIBLE_XP
                | MAP_MEMBERS
                | STAFFMODLEVEL
                | REBOOTTIMER
                | MAP_WORLD
                | RUNENERGY_VISIBLE
                | RUNWEIGHT_VISIBLE
                | PLAYERMOD
                | PLAYERMODLEVEL
                | PLAYERMEMBER
                | COMLEVEL_ACTIVE
                | GENDER
                | PLAYERDEMO
                | GET_SELFYANGLE
                | SELF_PLAYER_UID
        )
    }
}
