use crate::packet::{Packet, PacketError};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct LocPlacement {
    pub id: i32,
    pub plane: i32,
    pub local_x: i32,
    pub local_y: i32,
    pub kind: i32,
    pub rotation: i32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ScenePlacement {
    pub id: i32,
    pub plane: i32,
    pub scene_x: i32,
    pub scene_y: i32,
    pub kind: i32,
    pub rotation: i32,
}

pub fn decode_region_locs(data: &[u8]) -> Result<Vec<LocPlacement>, PacketError> {
    let mut packet = Packet::new(data);
    let mut placements = Vec::new();
    let mut id = -1i32;
    loop {
        let id_offset = packet.sum()?;
        if id_offset == 0 {
            return Ok(placements);
        }
        id = id.wrapping_add(id_offset);
        let mut packed = 0i32;
        loop {
            let position_offset = packet.usmart()?;
            if position_offset == 0 {
                break;
            }
            packed = packed.wrapping_add(position_offset - 1);
            let description = packet.u8()?;
            placements.push(LocPlacement {
                id,
                plane: packed >> 12,
                local_x: (packed >> 6) & 0x3f,
                local_y: packed & 0x3f,
                kind: description >> 2,
                rotation: description & 3,
            });
        }
    }
}

pub fn region_offset(region_id: i32, base_x: i32, base_y: i32) -> (i32, i32) {
    (
        (region_id >> 8) * 64 - base_x,
        (region_id & 0xff) * 64 - base_y,
    )
}

pub fn place_in_scene(
    region_id: i32,
    placements: &[LocPlacement],
    base_x: i32,
    base_y: i32,
    scene_size: i32,
) -> Vec<ScenePlacement> {
    let (offset_x, offset_y) = region_offset(region_id, base_x, base_y);
    placements
        .iter()
        .filter_map(|placement| {
            let scene_x = placement.local_x + offset_x;
            let scene_y = placement.local_y + offset_y;
            (scene_x > 0 && scene_y > 0 && scene_x < scene_size - 1 && scene_y < scene_size - 1)
                .then_some(ScenePlacement {
                    id: placement.id,
                    plane: placement.plane,
                    scene_x,
                    scene_y,
                    kind: placement.kind,
                    rotation: placement.rotation,
                })
        })
        .collect()
}

pub fn decode_scene_locs(
    regions: &[(i32, &[u8])],
    base_x: i32,
    base_y: i32,
    scene_size: i32,
) -> Result<Vec<ScenePlacement>, PacketError> {
    let mut scene = Vec::new();
    for (region_id, data) in regions {
        let placements = decode_region_locs(data)?;
        scene.extend(place_in_scene(
            *region_id,
            &placements,
            base_x,
            base_y,
            scene_size,
        ));
    }
    Ok(scene)
}
