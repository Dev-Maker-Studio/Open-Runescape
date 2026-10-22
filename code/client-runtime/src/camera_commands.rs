use openrs_login::world_packets::{decode, misc::Misc, WorldPacket};
use openrs_protocol::registry::ServerPacket;

pub const FORCE_ANGLE: i32 = 1;
pub const SMOOTH_RESET: i32 = 2;
pub const RESET: i32 = 3;
pub const LOOK_AT: i32 = 4;
pub const MOVE_TO: i32 = 5;
pub const SHAKE: i32 = 6;
pub const TARGET: i32 = 7;
pub const WIDTH: usize = 6;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CameraCommand {
    ForceAngle {
        angle_x: i32,
        angle_y: i32,
    },
    SmoothReset,
    Reset,
    LookAt {
        view_x: i32,
        view_y: i32,
        view_z: i32,
        constant_speed: i32,
        variable_speed: i32,
    },
    MoveTo {
        local_x: i32,
        local_y: i32,
        move_z: i32,
        constant_speed: i32,
        variable_speed: i32,
    },
    Shake {
        slot: i32,
        amplitude: i32,
        phase: i32,
        jitter: i32,
        frequency: i32,
    },
    Target {
        x: i32,
        y: i32,
    },
}

impl CameraCommand {
    pub fn to_ints(self) -> [i32; WIDTH] {
        match self {
            Self::ForceAngle { angle_x, angle_y } => [FORCE_ANGLE, angle_x, angle_y, 0, 0, 0],
            Self::SmoothReset => [SMOOTH_RESET, 0, 0, 0, 0, 0],
            Self::Reset => [RESET, 0, 0, 0, 0, 0],
            Self::LookAt {
                view_x,
                view_y,
                view_z,
                constant_speed,
                variable_speed,
            } => [
                LOOK_AT,
                view_x,
                view_z,
                view_y,
                variable_speed,
                constant_speed,
            ],
            Self::MoveTo {
                local_x,
                local_y,
                move_z,
                constant_speed,
                variable_speed,
            } => [
                MOVE_TO,
                local_x,
                local_y,
                move_z,
                constant_speed,
                variable_speed,
            ],
            Self::Shake {
                slot,
                amplitude,
                phase,
                jitter,
                frequency,
            } => [SHAKE, slot, amplitude, phase, jitter, frequency],
            Self::Target { x, y } => [TARGET, x, y, 0, 0, 0],
        }
    }
}

pub fn is_camera_packet(packet: ServerPacket) -> bool {
    matches!(
        packet,
        ServerPacket::CAM_FORCEANGLE
            | ServerPacket::CAM_SMOOTHRESET
            | ServerPacket::CAM_RESET
            | ServerPacket::CAM_LOOKAT
            | ServerPacket::CAM_MOVETO
            | ServerPacket::CAM_SHAKE
            | ServerPacket::SET_CAMERA_TARGET
    )
}

pub fn camera_command(
    packet: ServerPacket,
    payload: &[u8],
    base: (i32, i32),
    scene_size: i32,
) -> Result<Option<CameraCommand>, &'static str> {
    if !is_camera_packet(packet) {
        return Ok(None);
    }
    let decoded = decode(packet, payload).map_err(|_| "invalid camera packet")?;
    let command = match decoded {
        WorldPacket::Empty => match packet {
            ServerPacket::CAM_SMOOTHRESET => CameraCommand::SmoothReset,
            ServerPacket::CAM_RESET => CameraCommand::Reset,
            _ => return Ok(None),
        },
        WorldPacket::Misc(Misc::CamForceAngle { angle_y, angle_x }) => CameraCommand::ForceAngle {
            angle_x: i32::from(angle_x),
            angle_y: i32::from(angle_y),
        },
        WorldPacket::Misc(Misc::CamLookAt {
            view_x,
            view_y,
            view_z,
            constant_speed,
            variable_speed,
        }) => CameraCommand::LookAt {
            view_x: i32::from(view_x),
            view_y: view_y as i32,
            view_z: i32::from(view_z),
            constant_speed: i32::from(constant_speed),
            variable_speed: i32::from(variable_speed),
        },
        WorldPacket::Misc(Misc::CamMoveTo {
            local_y,
            local_x,
            constant_speed,
            move_z,
            variable_speed,
        }) => CameraCommand::MoveTo {
            local_x: i32::from(local_x),
            local_y: i32::from(local_y),
            move_z: move_z as i32,
            constant_speed: i32::from(constant_speed),
            variable_speed: i32::from(variable_speed),
        },
        WorldPacket::Misc(Misc::CamShake {
            key,
            flags,
            shake_type,
            jitter,
            frequency,
        }) => CameraCommand::Shake {
            slot: i32::from(shake_type),
            amplitude: i32::from(key),
            phase: i32::from(flags),
            jitter: i32::from(jitter),
            frequency: i32::from(frequency),
        },
        WorldPacket::Misc(Misc::CameraTarget { clear, x, y }) => {
            if clear {
                CameraCommand::Target { x: -1, y: -1 }
            } else {
                let local_x = (x - base.0).clamp(0, scene_size.max(0));
                let local_y = (y - base.1).clamp(0, scene_size.max(0));
                CameraCommand::Target {
                    x: (local_x << 9) + 256,
                    y: (local_y << 9) + 256,
                }
            }
        }
        _ => return Ok(None),
    };
    Ok(Some(command))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn add(v: u8) -> u8 {
        v.wrapping_add(128)
    }

    fn sub(v: u8) -> u8 {
        128u8.wrapping_sub(v)
    }

    fn inverse(v: u8) -> u8 {
        v.wrapping_neg()
    }

    #[test]
    fn force_angle_reads_y_then_x_with_add_offsets() {
        let angle_y: u16 = 0x1234;
        let angle_x: u16 = 0x0abc;
        let payload = [
            (angle_y >> 8) as u8,
            add(angle_y as u8),
            add(angle_x as u8),
            (angle_x >> 8) as u8,
        ];
        assert_eq!(
            camera_command(ServerPacket::CAM_FORCEANGLE, &payload, (0, 0), 104),
            Ok(Some(CameraCommand::ForceAngle {
                angle_x: 0x0abc,
                angle_y: 0x1234
            }))
        );
    }

    #[test]
    fn empty_packets_reset_the_camera() {
        assert_eq!(
            camera_command(ServerPacket::CAM_SMOOTHRESET, &[], (0, 0), 104),
            Ok(Some(CameraCommand::SmoothReset))
        );
        assert_eq!(
            camera_command(ServerPacket::CAM_RESET, &[], (0, 0), 104),
            Ok(Some(CameraCommand::Reset))
        );
    }

    #[test]
    fn look_at_widens_the_height_before_the_shift() {
        let view_y: u16 = 0xfffe;
        let payload = [
            40,
            (view_y >> 8) as u8,
            add(view_y as u8),
            add(7),
            inverse(25),
            sub(100),
        ];
        let command = camera_command(ServerPacket::CAM_LOOKAT, &payload, (0, 0), 104)
            .unwrap()
            .unwrap();
        assert_eq!(
            command,
            CameraCommand::LookAt {
                view_x: 40,
                view_y: 0xfffe << 2,
                view_z: 7,
                constant_speed: 25,
                variable_speed: 100
            }
        );
        assert_eq!(command.to_ints(), [LOOK_AT, 40, 7, 0xfffe << 2, 100, 25]);
    }

    #[test]
    fn move_to_reads_local_y_before_x() {
        let move_z: u16 = 0x9000;
        let payload = [12, 34, sub(5), move_z as u8, (move_z >> 8) as u8, add(60)];
        let command = camera_command(ServerPacket::CAM_MOVETO, &payload, (0, 0), 104)
            .unwrap()
            .unwrap();
        assert_eq!(
            command,
            CameraCommand::MoveTo {
                local_x: 34,
                local_y: 12,
                move_z: 0x9000 << 2,
                constant_speed: 5,
                variable_speed: 60
            }
        );
        assert_eq!(command.to_ints(), [MOVE_TO, 34, 12, 0x9000 << 2, 5, 60]);
    }

    #[test]
    fn shake_maps_the_key_to_the_amplitude_and_the_flags_to_the_phase() {
        let flags: u16 = 0x0102;
        let payload = [
            sub(9),
            (flags >> 8) as u8,
            flags as u8,
            inverse(3),
            inverse(17),
            sub(50),
        ];
        let command = camera_command(ServerPacket::CAM_SHAKE, &payload, (0, 0), 104)
            .unwrap()
            .unwrap();
        assert_eq!(
            command,
            CameraCommand::Shake {
                slot: 3,
                amplitude: 9,
                phase: 0x0102,
                jitter: 17,
                frequency: 50
            }
        );
        assert_eq!(command.to_ints(), [SHAKE, 3, 9, 0x0102, 17, 50]);
    }

    #[test]
    fn camera_target_subtracts_the_base_and_clamps_to_the_scene() {
        let key: i32 = (3222 << 14) | 3218;
        let payload = key.to_le_bytes();
        let command = camera_command(ServerPacket::SET_CAMERA_TARGET, &payload, (3200, 3200), 104)
            .unwrap()
            .unwrap();
        assert_eq!(
            command,
            CameraCommand::Target {
                x: (22 << 9) + 256,
                y: (18 << 9) + 256
            }
        );
        let outside: i32 = (2000 << 14) | 4000;
        let payload = outside.to_le_bytes();
        let command = camera_command(ServerPacket::SET_CAMERA_TARGET, &payload, (3200, 3200), 104)
            .unwrap()
            .unwrap();
        assert_eq!(
            command,
            CameraCommand::Target {
                x: 256,
                y: (104 << 9) + 256
            }
        );
        assert_eq!(
            camera_command(
                ServerPacket::SET_CAMERA_TARGET,
                &(-1i32).to_le_bytes(),
                (3200, 3200),
                104
            ),
            Ok(Some(CameraCommand::Target { x: -1, y: -1 }))
        );
    }

    #[test]
    fn other_packets_and_trailing_bytes_are_not_camera_commands() {
        assert_eq!(
            camera_command(ServerPacket::PING, &[], (0, 0), 104),
            Ok(None)
        );
        assert!(camera_command(ServerPacket::CAM_RESET, &[1], (0, 0), 104).is_err());
    }
}
