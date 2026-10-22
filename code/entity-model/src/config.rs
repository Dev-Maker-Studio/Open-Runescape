use crate::mesh::Mesh;
use crate::reader::{at_slice, Buf, ModelError, R};

pub trait Resources {
    fn model(&self, id: i32) -> Option<Vec<u8>>;
    fn item(&self, id: i32) -> Option<Vec<u8>>;
    fn idk(&self, id: i32) -> Option<Vec<u8>>;
    fn bas(&self, id: i32) -> Option<Vec<u8>>;
    fn npc(&self, id: i32) -> Option<Vec<u8>>;
}

pub fn load_mesh(res: &dyn Resources, id: i32) -> R<Option<Mesh>> {
    match res.model(id) {
        None => Ok(None),
        Some(data) => Mesh::decode(&data).map(Some),
    }
}

fn upscaled(mesh: &mut Mesh) -> R<()> {
    if mesh.version < 13 {
        mesh.upscale()
    } else {
        Ok(())
    }
}

fn required(res: &dyn Resources, id: i32) -> R<Mesh> {
    let mut mesh = load_mesh(res, id)?.ok_or(ModelError::Null)?;
    upscaled(&mut mesh)?;
    Ok(mesh)
}

fn merged(meshes: &[Option<Mesh>], count: usize) -> R<Mesh> {
    let refs: Vec<Option<&Mesh>> = meshes.iter().map(Option::as_ref).collect();
    Mesh::merge(&refs, count)
}

fn pairs(buf: &mut Buf) -> R<(Vec<i16>, Vec<i16>)> {
    let count = buf.u8()?;
    let mut src = Vec::with_capacity(count as usize);
    let mut dst = Vec::with_capacity(count as usize);
    for _ in 0..count {
        src.push(buf.u16()? as i16);
        dst.push(buf.u16()? as i16);
    }
    Ok((src, dst))
}

fn apply_colors(
    mesh: &mut Mesh,
    src: &Option<Vec<i16>>,
    dst: &Option<Vec<i16>>,
    textures: bool,
) -> R<()> {
    if let Some(src) = src {
        for (index, from) in src.iter().enumerate() {
            let to = at_slice(dst.as_deref().ok_or(ModelError::Null)?, index as i64)?;
            if textures {
                mesh.retexture(*from, to)?;
            } else {
                mesh.recolor(*from, to)?;
            }
        }
    }
    Ok(())
}

#[derive(Clone, Debug)]
pub struct ItemType {
    pub id: i32,
    pub model_id: i32,
    pub model_zoom: i32,
    pub rot_x: i32,
    pub rot_y: i32,
    pub rot_z: i32,
    pub offset_x: i32,
    pub offset_y: i32,
    pub name: Vec<u8>,
    pub members_only: bool,
    pub wear_pos: i32,
    pub wear_pos2: i32,
    pub wear_pos3: i32,
    pub team_id: i32,
    pub stackable: i32,
    pub multi_stack_size: i32,
    pub tradeable: bool,
    pub value: i32,
    pub cert_id: i32,
    pub cert_template_id: i32,
    pub lend_id: i32,
    pub lend_template_id: i32,
    pub bind_id: i32,
    pub bind_template_id: i32,
    pub recolor_src: Option<Vec<i16>>,
    pub recolor_dst: Option<Vec<i16>>,
    pub recolor_dpalette: Option<Vec<i8>>,
    pub retex_src: Option<Vec<i16>>,
    pub retex_dst: Option<Vec<i16>>,
    pub male_equip: [i32; 3],
    pub female_equip: [i32; 3],
    pub male_head: [i32; 2],
    pub female_head: [i32; 2],
    pub male_offset: [i32; 3],
    pub female_offset: [i32; 3],
    pub resize: [i32; 3],
    pub stack_ids: Option<Vec<i32>>,
    pub stack_trigger: Option<Vec<i32>>,
    pub ambient: i32,
    pub contrast: i32,
    pub quests: Option<Vec<i32>>,
    pub ground_options: [Option<Vec<u8>>; 5],
    pub inventory_options: [Option<Vec<u8>>; 5],
    pub has_tooltip_color: bool,
    pub tooltip_color: i32,
    pub primary_cursor_index: i32,
    pub primary_cursor: i32,
    pub secondary_cursor_index: i32,
    pub secondary_cursor: i32,
}

impl ItemType {
    pub fn new(id: i32) -> Self {
        Self {
            id,
            model_id: 0,
            model_zoom: 2000,
            rot_x: 0,
            rot_y: 0,
            rot_z: 0,
            offset_x: 0,
            offset_y: 0,
            name: b"null".to_vec(),
            members_only: false,
            wear_pos: -1,
            wear_pos2: -1,
            wear_pos3: -1,
            team_id: 0,
            stackable: 0,
            multi_stack_size: -1,
            tradeable: false,
            value: 1,
            cert_id: -1,
            cert_template_id: -1,
            lend_id: -1,
            lend_template_id: -1,
            bind_id: -1,
            bind_template_id: -1,
            recolor_src: None,
            recolor_dst: None,
            recolor_dpalette: None,
            retex_src: None,
            retex_dst: None,
            male_equip: [-1; 3],
            female_equip: [-1; 3],
            male_head: [-1; 2],
            female_head: [-1; 2],
            male_offset: [0; 3],
            female_offset: [0; 3],
            resize: [128; 3],
            stack_ids: None,
            stack_trigger: None,
            ambient: 0,
            contrast: 0,
            quests: None,
            ground_options: Default::default(),
            inventory_options: Default::default(),
            has_tooltip_color: false,
            tooltip_color: 0,
            primary_cursor_index: -1,
            primary_cursor: -1,
            secondary_cursor_index: -1,
            secondary_cursor: -1,
        }
    }

    pub fn decode(id: i32, data: &[u8]) -> R<Self> {
        let mut item = Self::new(id);
        let mut buf = Buf::new(data);
        loop {
            let opcode = buf.u8()?;
            if opcode == 0 {
                return Ok(item);
            }
            item.opcode(opcode, &mut buf)?;
        }
    }

    fn opcode(&mut self, opcode: i32, buf: &mut Buf) -> R<()> {
        match opcode {
            1 => self.model_id = buf.big_smart()?,
            2 => self.name = buf.string()?,
            4 => self.model_zoom = buf.u16()?,
            5 => self.rot_x = buf.u16()?,
            6 => self.rot_y = buf.u16()?,
            7 => self.offset_x = buf.i16()?,
            8 => self.offset_y = buf.i16()?,
            11 => self.stackable = 1,
            12 => self.value = buf.i32()?,
            13 => self.wear_pos = buf.u8()?,
            14 => self.wear_pos2 = buf.u8()?,
            16 => self.members_only = true,
            18 => self.multi_stack_size = buf.u16()?,
            23 => self.male_equip[0] = buf.big_smart()?,
            24 => self.male_equip[1] = buf.big_smart()?,
            25 => self.female_equip[0] = buf.big_smart()?,
            26 => self.female_equip[1] = buf.big_smart()?,
            27 => self.wear_pos3 = buf.u8()?,
            30..=34 => self.ground_options[(opcode - 30) as usize] = Some(buf.string()?),
            35..=39 => self.inventory_options[(opcode - 35) as usize] = Some(buf.string()?),
            40 => {
                let (src, dst) = pairs(buf)?;
                self.recolor_src = Some(src);
                self.recolor_dst = Some(dst);
            }
            41 => {
                let (src, dst) = pairs(buf)?;
                self.retex_src = Some(src);
                self.retex_dst = Some(dst);
            }
            42 => {
                let count = buf.u8()?;
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    list.push(buf.i8()? as i8);
                }
                self.recolor_dpalette = Some(list);
            }
            43 => {
                self.tooltip_color = buf.i32()?;
                self.has_tooltip_color = true;
            }
            65 => self.tradeable = true,
            78 => self.male_equip[2] = buf.big_smart()?,
            79 => self.female_equip[2] = buf.big_smart()?,
            90 => self.male_head[0] = buf.big_smart()?,
            91 => self.female_head[0] = buf.big_smart()?,
            92 => self.male_head[1] = buf.big_smart()?,
            93 => self.female_head[1] = buf.big_smart()?,
            95 => self.rot_z = buf.u16()?,
            96 => {
                buf.u8()?;
            }
            97 => self.cert_id = buf.u16()?,
            98 => self.cert_template_id = buf.u16()?,
            100..=109 => {
                let slot = (opcode - 100) as usize;
                let ids = self.stack_ids.get_or_insert_with(|| vec![0; 10]);
                ids[slot] = buf.u16()?;
                let triggers = self.stack_trigger.get_or_insert_with(|| vec![0; 10]);
                triggers[slot] = buf.u16()?;
            }
            110 => self.resize[0] = buf.u16()?,
            111 => self.resize[1] = buf.u16()?,
            112 => self.resize[2] = buf.u16()?,
            113 => self.ambient = buf.i8()?,
            114 => self.contrast = buf.i8()?,
            115 => self.team_id = buf.u8()?,
            121 => self.lend_id = buf.u16()?,
            122 => self.lend_template_id = buf.u16()?,
            125 => {
                for axis in 0..3 {
                    self.male_offset[axis] = buf.i8()?.wrapping_shl(2);
                }
            }
            126 => {
                for axis in 0..3 {
                    self.female_offset[axis] = buf.i8()?.wrapping_shl(2);
                }
            }
            127 => {
                self.primary_cursor_index = buf.u8()?;
                self.primary_cursor = buf.u16()?;
            }
            128 => {
                self.secondary_cursor_index = buf.u8()?;
                self.secondary_cursor = buf.u16()?;
            }
            129 | 130 => {
                buf.u8()?;
                buf.u16()?;
            }
            132 => {
                let count = buf.u8()?;
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    list.push(buf.u16()?);
                }
                self.quests = Some(list);
            }
            134 => {
                buf.u8()?;
            }
            139 => self.bind_id = buf.u16()?,
            140 => self.bind_template_id = buf.u16()?,
            249 => {
                let count = buf.u8()?;
                for _ in 0..count {
                    let is_string = buf.u8()? == 1;
                    buf.u24()?;
                    if is_string {
                        buf.string()?;
                    } else {
                        buf.i32()?;
                    }
                }
            }
            _ => {}
        }
        Ok(())
    }

    fn copy_display(&mut self, template: &ItemType) {
        self.model_id = template.model_id;
        self.model_zoom = template.model_zoom;
        self.rot_x = template.rot_x;
        self.rot_y = template.rot_y;
        self.rot_z = template.rot_z;
        self.offset_x = template.offset_x;
        self.offset_y = template.offset_y;
    }

    fn copy_worn(&mut self, original: &ItemType) {
        self.recolor_src = original.recolor_src.clone();
        self.recolor_dst = original.recolor_dst.clone();
        self.recolor_dpalette = original.recolor_dpalette.clone();
        self.retex_src = original.retex_src.clone();
        self.retex_dst = original.retex_dst.clone();
        self.name = original.name.clone();
        self.members_only = original.members_only;
        self.wear_pos = original.wear_pos;
        self.wear_pos2 = original.wear_pos2;
        self.wear_pos3 = original.wear_pos3;
        self.male_equip = original.male_equip;
        self.female_equip = original.female_equip;
        self.male_offset = original.male_offset;
        self.female_offset = original.female_offset;
        self.male_head = original.male_head;
        self.female_head = original.female_head;
        self.team_id = original.team_id;
    }

    pub fn generate_note(&mut self, template: &ItemType, original: &ItemType) {
        self.copy_display(template);
        self.recolor_src = template.recolor_src.clone();
        self.recolor_dst = template.recolor_dst.clone();
        self.recolor_dpalette = template.recolor_dpalette.clone();
        self.retex_src = template.retex_src.clone();
        self.retex_dst = template.retex_dst.clone();
        self.name = original.name.clone();
        self.members_only = original.members_only;
        self.value = original.value;
        self.stackable = 1;
    }

    pub fn generate_lend(&mut self, template: &ItemType, original: &ItemType) {
        self.value = 0;
        self.copy_display(template);
        self.copy_worn(original);
    }

    pub fn generate_bind(&mut self, template: &ItemType, original: &ItemType) {
        self.generate_lend(template, original);
        self.stackable = original.stackable;
    }

    pub fn body_mesh(
        &self,
        res: &dyn Resources,
        female: bool,
        modifier: Option<&Customisation>,
    ) -> R<Option<Mesh>> {
        let [primary, secondary, tertiary] = match (female, modifier) {
            (true, Some(m)) => m.female_body,
            (false, Some(m)) => m.male_body,
            (true, None) => self.female_equip,
            (false, None) => self.male_equip,
        };
        if primary == -1 {
            return Ok(None);
        }
        let Some(mut mesh) = load_mesh(res, primary)? else {
            return Ok(None);
        };
        upscaled(&mut mesh)?;
        if secondary != -1 {
            let second = required(res, secondary)?;
            if tertiary != -1 {
                let third = required(res, tertiary)?;
                mesh = Mesh::merge(&[Some(&mesh), Some(&second), Some(&third)], 3)?;
            } else {
                mesh = Mesh::merge(&[Some(&mesh), Some(&second)], 2)?;
            }
        }
        let offset = if female {
            self.female_offset
        } else {
            self.male_offset
        };
        if offset != [0; 3] {
            mesh.translate(offset[0], offset[1], offset[2])?;
        }
        self.finish(&mut mesh, modifier)?;
        Ok(Some(mesh))
    }

    pub fn head_mesh(
        &self,
        res: &dyn Resources,
        female: bool,
        modifier: Option<&Customisation>,
    ) -> R<Option<Mesh>> {
        let [primary, secondary] = match (female, modifier) {
            (true, Some(m)) => m.female_head,
            (false, Some(m)) => m.male_head,
            (true, None) => self.female_head,
            (false, None) => self.male_head,
        };
        if primary == -1 {
            return Ok(None);
        }
        let mut mesh = required(res, primary)?;
        if secondary != -1 {
            let second = required(res, secondary)?;
            mesh = Mesh::merge(&[Some(&mesh), Some(&second)], 2)?;
        }
        self.finish(&mut mesh, modifier)?;
        Ok(Some(mesh))
    }

    fn finish(&self, mesh: &mut Mesh, modifier: Option<&Customisation>) -> R<()> {
        if self.recolor_src.is_some() {
            let dst = match modifier {
                Some(m) if m.recol_dst.is_some() => &m.recol_dst,
                _ => &self.recolor_dst,
            };
            apply_colors(mesh, &self.recolor_src, dst, false)?;
        }
        if self.retex_src.is_some() {
            let dst = match modifier {
                Some(m) if m.retex_dst.is_some() => &m.retex_dst,
                _ => &self.retex_dst,
            };
            apply_colors(mesh, &self.retex_src, dst, true)?;
        }
        Ok(())
    }
}

const MAX_DEPTH: u32 = 8;

pub fn item(res: &dyn Resources, id: i32) -> R<ItemType> {
    item_at(res, id, 0)
}

fn item_at(res: &dyn Resources, id: i32, depth: u32) -> R<ItemType> {
    if depth > MAX_DEPTH {
        return Err(ModelError::Invalid);
    }
    let mut ty = match res.item(id) {
        Some(data) => ItemType::decode(id, &data)?,
        None => ItemType::new(id),
    };
    if ty.cert_template_id != -1 {
        let template = item_at(res, ty.cert_template_id, depth + 1)?;
        let original = item_at(res, ty.cert_id, depth + 1)?;
        ty.generate_note(&template, &original);
    }
    if ty.lend_template_id != -1 {
        let template = item_at(res, ty.lend_template_id, depth + 1)?;
        let original = item_at(res, ty.lend_id, depth + 1)?;
        ty.generate_lend(&template, &original);
    }
    if ty.bind_template_id != -1 {
        let template = item_at(res, ty.bind_template_id, depth + 1)?;
        let original = item_at(res, ty.bind_id, depth + 1)?;
        ty.generate_bind(&template, &original);
    }
    Ok(ty)
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Customisation {
    pub male_body: [i32; 3],
    pub female_body: [i32; 3],
    pub male_head: [i32; 2],
    pub female_head: [i32; 2],
    pub recol_dst: Option<Vec<i16>>,
    pub retex_dst: Option<Vec<i16>>,
}

impl Customisation {
    pub fn new(item: &ItemType) -> Self {
        Self {
            male_body: item.male_equip,
            female_body: item.female_equip,
            male_head: item.male_head,
            female_head: item.female_head,
            recol_dst: item.recolor_dst.clone(),
            retex_dst: item.retex_dst.clone(),
        }
    }

    pub fn decode_effects(item: &ItemType, data: &[u8]) -> R<Self> {
        let mut c = Self::new(item);
        let mut buf = Buf::new(data);
        let flags = buf.u8()?;
        if flags & 1 != 0 {
            c.male_body[0] = buf.big_smart()?;
            c.female_body[0] = buf.big_smart()?;
            if item.male_equip[1] != -1 || item.female_equip[1] != -1 {
                c.male_body[1] = buf.big_smart()?;
                c.female_body[1] = buf.big_smart()?;
            }
            if item.male_equip[2] != -1 || item.female_equip[2] != -1 {
                c.male_body[2] = buf.big_smart()?;
                c.female_body[2] = buf.big_smart()?;
            }
        }
        if flags & 2 != 0 {
            c.male_head[0] = buf.big_smart()?;
            c.female_head[0] = buf.big_smart()?;
            if item.male_head[1] != -1 || item.female_head[1] != -1 {
                c.male_head[1] = buf.big_smart()?;
                c.female_head[1] = buf.big_smart()?;
            }
        }
        if flags & 4 != 0 {
            let packed = buf.u16()?;
            for shift in [0, 4, 8, 12] {
                let slot = (packed >> shift) & 15;
                if slot != 15 {
                    let value = buf.u16()? as i16;
                    let list = c.recol_dst.as_mut().ok_or(ModelError::Null)?;
                    *list.get_mut(slot as usize).ok_or(ModelError::Bounds)? = value;
                }
            }
        }
        if flags & 8 != 0 {
            let packed = buf.u8()?;
            for shift in [0, 4] {
                let slot = (packed >> shift) & 15;
                if slot != 15 {
                    let value = buf.u16()? as i16;
                    let list = c.retex_dst.as_mut().ok_or(ModelError::Null)?;
                    *list.get_mut(slot as usize).ok_or(ModelError::Bounds)? = value;
                }
            }
        }
        Ok(c)
    }
}

#[derive(Clone, Debug, Default)]
pub struct IdkType {
    pub body_models: Option<Vec<i32>>,
    pub recolor_src: Option<Vec<i16>>,
    pub recolor_dst: Option<Vec<i16>>,
    pub retex_src: Option<Vec<i16>>,
    pub retex_dst: Option<Vec<i16>>,
    pub head_models: [i32; 5],
}

impl IdkType {
    pub fn new() -> Self {
        Self {
            head_models: [-1; 5],
            ..Self::default()
        }
    }

    pub fn decode(data: &[u8]) -> R<Self> {
        let mut idk = Self::new();
        let mut buf = Buf::new(data);
        loop {
            let opcode = buf.u8()?;
            match opcode {
                0 => return Ok(idk),
                1 => {
                    buf.u8()?;
                }
                2 => {
                    let count = buf.u8()?;
                    let mut list = Vec::with_capacity(count as usize);
                    for _ in 0..count {
                        list.push(buf.big_smart()?);
                    }
                    idk.body_models = Some(list);
                }
                40 => {
                    let (src, dst) = pairs(&mut buf)?;
                    idk.recolor_src = Some(src);
                    idk.recolor_dst = Some(dst);
                }
                41 => {
                    let (src, dst) = pairs(&mut buf)?;
                    idk.retex_src = Some(src);
                    idk.retex_dst = Some(dst);
                }
                60..=69 => {
                    let value = buf.big_smart()?;
                    let slot = at_slice(&[0usize, 1, 2, 3, 4], i64::from(opcode - 60))?;
                    idk.head_models[slot] = value;
                }
                _ => {}
            }
        }
    }

    pub fn load(res: &dyn Resources, id: i32) -> R<Self> {
        match res.idk(id) {
            Some(data) => Self::decode(&data),
            None => Ok(Self::new()),
        }
    }

    fn finish(&self, mesh: &mut Mesh) -> R<()> {
        apply_colors(mesh, &self.recolor_src, &self.recolor_dst, false)?;
        apply_colors(mesh, &self.retex_src, &self.retex_dst, true)
    }

    pub fn body_mesh(&self, res: &dyn Resources) -> R<Option<Mesh>> {
        let Some(ids) = &self.body_models else {
            return Ok(None);
        };
        let mut meshes = Vec::with_capacity(ids.len());
        for id in ids {
            meshes.push(load_mesh(res, *id)?);
        }
        for mesh in &mut meshes {
            let mesh = mesh.as_mut().ok_or(ModelError::Null)?;
            upscaled(mesh)?;
        }
        let mut mesh = if meshes.len() == 1 {
            meshes.remove(0).ok_or(ModelError::Null)?
        } else {
            merged(&meshes, meshes.len())?
        };
        self.finish(&mut mesh)?;
        Ok(Some(mesh))
    }

    pub fn head_mesh(&self, res: &dyn Resources) -> R<Mesh> {
        let mut meshes: Vec<Option<Mesh>> = Vec::new();
        for id in self.head_models {
            if id != -1 {
                meshes.push(load_mesh(res, id)?);
            }
        }
        for mesh in meshes.iter_mut().flatten() {
            upscaled(mesh)?;
        }
        let count = meshes.len();
        let mut mesh = merged(&meshes, count)?;
        self.finish(&mut mesh)?;
        Ok(mesh)
    }
}

#[derive(Clone, Debug, Default)]
pub struct EquipmentDefaults {
    pub customizable_obj_slots: Vec<i32>,
    pub shield_slot: i32,
    pub weapon_slot: i32,
    pub hidden_shield_slots: Option<Vec<i32>>,
    pub hidden_weapon_slots: Option<Vec<i32>>,
}

impl EquipmentDefaults {
    pub fn decode(data: &[u8]) -> R<Self> {
        let mut slots = None;
        let mut out = Self {
            shield_slot: -1,
            weapon_slot: -1,
            ..Self::default()
        };
        let mut buf = Buf::new(data);
        let bytes = |buf: &mut Buf| -> R<Vec<i32>> {
            let count = buf.u8()?;
            let mut list = Vec::with_capacity(count as usize);
            for _ in 0..count {
                list.push(buf.u8()?);
            }
            Ok(list)
        };
        loop {
            match buf.u8()? {
                0 => break,
                1 => slots = Some(bytes(&mut buf)?),
                3 => out.shield_slot = buf.u8()?,
                4 => out.weapon_slot = buf.u8()?,
                5 => out.hidden_shield_slots = Some(bytes(&mut buf)?),
                6 => out.hidden_weapon_slots = Some(bytes(&mut buf)?),
                _ => {}
            }
        }
        out.customizable_obj_slots = slots.ok_or(ModelError::Null)?;
        Ok(out)
    }
}

#[derive(Clone, Debug)]
pub struct BasType {
    pub stand_sequence: i32,
    pub walk_sequence: i32,
    pub teleport_sequence: i32,
    pub tele_dir: [i32; 3],
    pub running_sequence: i32,
    pub run_dir: [i32; 3],
    pub model_width: i32,
    pub model_length: i32,
    pub obj_vertex_transforms: Option<Vec<Option<[i32; 6]>>>,
    pub yaw_acceleration: i32,
    pub yaw_max_velocity: i32,
    pub roll_acceleration: i32,
    pub roll_max_velocity: i32,
    pub roll_target_angle: i32,
    pub pitch_acceleration: i32,
    pub pitch_max_velocity: i32,
    pub pitch_target_angle: i32,
    pub unused: i32,
    pub stand_turn_ccw: i32,
    pub stand_turn_cw: i32,
    pub walk_dir: [i32; 3],
    pub icon_height_override: i32,
    pub tele_turn_ccw: i32,
    pub tele_turn_cw: i32,
    pub run_turn_ccw: i32,
    pub run_turn_cw: i32,
    pub walk_turn_ccw: i32,
    pub walk_turn_cw: i32,
    pub random_stand_sequences: Option<Vec<i32>>,
    pub random_stand_chances: Option<Vec<i32>>,
    pub random_stand_chance_total: i32,
    pub renders_shadow: bool,
    pub hill_rotate_x: i32,
    pub hill_rotate_z: i32,
    pub turn_angle_adjustment: Option<Vec<i32>>,
    pub projection_offset: Option<Vec<Option<[i32; 3]>>>,
}

impl BasType {
    pub fn new() -> Self {
        Self {
            stand_sequence: -1,
            walk_sequence: -1,
            teleport_sequence: -1,
            tele_dir: [-1; 3],
            running_sequence: -1,
            run_dir: [-1; 3],
            model_width: 0,
            model_length: 0,
            obj_vertex_transforms: None,
            yaw_acceleration: 0,
            yaw_max_velocity: 0,
            roll_acceleration: 0,
            roll_max_velocity: 0,
            roll_target_angle: 0,
            pitch_acceleration: 0,
            pitch_max_velocity: 0,
            pitch_target_angle: 0,
            unused: -1,
            stand_turn_ccw: -1,
            stand_turn_cw: -1,
            walk_dir: [-1; 3],
            icon_height_override: -1,
            tele_turn_ccw: -1,
            tele_turn_cw: -1,
            run_turn_ccw: -1,
            run_turn_cw: -1,
            walk_turn_ccw: -1,
            walk_turn_cw: -1,
            random_stand_sequences: None,
            random_stand_chances: None,
            random_stand_chance_total: 0,
            renders_shadow: true,
            hill_rotate_x: 0,
            hill_rotate_z: 0,
            turn_angle_adjustment: None,
            projection_offset: None,
        }
    }

    pub fn decode(data: &[u8], defaults: &EquipmentDefaults) -> R<Self> {
        let mut bas = Self::new();
        let slots = defaults.customizable_obj_slots.len();
        let mut buf = Buf::new(data);
        loop {
            let opcode = buf.u8()?;
            match opcode {
                0 => return Ok(bas),
                1 => {
                    bas.stand_sequence = buf.big_smart()?;
                    bas.walk_sequence = buf.big_smart()?;
                }
                2 => bas.teleport_sequence = buf.big_smart()?,
                3 => bas.tele_dir[2] = buf.big_smart()?,
                4 => bas.tele_dir[1] = buf.big_smart()?,
                5 => bas.tele_dir[0] = buf.big_smart()?,
                6 => bas.running_sequence = buf.big_smart()?,
                7 => bas.run_dir[2] = buf.big_smart()?,
                8 => bas.run_dir[1] = buf.big_smart()?,
                9 => bas.run_dir[0] = buf.big_smart()?,
                26 => {
                    bas.model_width = i32::from((buf.u8()? * 4) as i16);
                    bas.model_length = i32::from((buf.u8()? * 4) as i16);
                }
                27 => {
                    let list = bas
                        .obj_vertex_transforms
                        .get_or_insert_with(|| vec![None; slots]);
                    let slot = buf.u8()?;
                    let mut entry = [0; 6];
                    for value in &mut entry {
                        *value = buf.i16()?;
                    }
                    if slot as usize >= list.len() {
                        return Err(ModelError::Bounds);
                    }
                    list[slot as usize] = Some(entry);
                }
                28 => {
                    let count = buf.u8()?;
                    let mut seqs = Vec::with_capacity(count as usize);
                    for _ in 0..count {
                        let value = buf.u8()?;
                        seqs.push(if value == 255 { -1 } else { value });
                    }
                    bas.random_stand_sequences = Some(seqs);
                }
                29 => bas.yaw_acceleration = buf.u8()?,
                30 => bas.yaw_max_velocity = buf.u16()?,
                31 => bas.roll_acceleration = buf.u8()?,
                32 => bas.roll_max_velocity = buf.u16()?,
                33 => bas.roll_target_angle = buf.i16()?,
                34 => bas.pitch_acceleration = buf.u8()?,
                35 => bas.pitch_max_velocity = buf.u16()?,
                36 => bas.pitch_target_angle = buf.i16()?,
                37 => bas.unused = buf.u8()?,
                38 => bas.stand_turn_ccw = buf.big_smart()?,
                39 => bas.stand_turn_cw = buf.big_smart()?,
                40 => bas.walk_dir[2] = buf.big_smart()?,
                41 => bas.walk_dir[1] = buf.big_smart()?,
                42 => bas.walk_dir[0] = buf.big_smart()?,
                43 | 44 => {
                    buf.u16()?;
                }
                45 => bas.icon_height_override = buf.u16()?,
                46 => bas.tele_turn_ccw = buf.big_smart()?,
                47 => bas.tele_turn_cw = buf.big_smart()?,
                48 => bas.run_turn_ccw = buf.big_smart()?,
                49 => bas.run_turn_cw = buf.big_smart()?,
                50 => bas.walk_turn_ccw = buf.big_smart()?,
                51 => bas.walk_turn_cw = buf.big_smart()?,
                52 => {
                    let count = buf.u8()?;
                    let mut seqs = Vec::with_capacity(count as usize);
                    let mut chances = Vec::with_capacity(count as usize);
                    for _ in 0..count {
                        seqs.push(buf.big_smart()?);
                        let chance = buf.u8()?;
                        chances.push(chance);
                        bas.random_stand_chance_total += chance;
                    }
                    bas.random_stand_sequences = Some(seqs);
                    bas.random_stand_chances = Some(chances);
                }
                53 => bas.renders_shadow = false,
                54 => {
                    bas.hill_rotate_x = buf.u8()? << 6;
                    bas.hill_rotate_z = buf.u8()? << 6;
                }
                55 => {
                    let list = bas
                        .turn_angle_adjustment
                        .get_or_insert_with(|| vec![0; slots]);
                    let slot = buf.u8()?;
                    let value = buf.u16()?;
                    if slot as usize >= list.len() {
                        return Err(ModelError::Bounds);
                    }
                    list[slot as usize] = value;
                }
                56 => {
                    let list = bas
                        .projection_offset
                        .get_or_insert_with(|| vec![None; slots]);
                    let slot = buf.u8()?;
                    let mut entry = [0; 3];
                    for value in &mut entry {
                        *value = buf.i16()?;
                    }
                    if slot as usize >= list.len() {
                        return Err(ModelError::Bounds);
                    }
                    list[slot as usize] = Some(entry);
                }
                _ => {}
            }
        }
    }

    pub fn load(res: &dyn Resources, id: i32, defaults: &EquipmentDefaults) -> R<Self> {
        match res.bas(id) {
            Some(data) => Self::decode(&data, defaults),
            None => Ok(Self::new()),
        }
    }
}

impl Default for BasType {
    fn default() -> Self {
        Self::new()
    }
}

fn apply_transform(mesh: &mut Mesh, entry: &Option<[i32; 6]>) -> R<()> {
    let [tx, ty, tz, rx, ry, rz] = entry.unwrap_or([0; 6]);
    let (rx, ry, rz) = (rx.wrapping_shl(3), ry.wrapping_shl(3), rz.wrapping_shl(3));
    if rx != 0 || ry != 0 || rz != 0 {
        mesh.rotate_without_normals(rx, ry, rz)?;
    }
    if tx != 0 || ty != 0 || tz != 0 {
        mesh.translate(tx, ty, tz)?;
    }
    Ok(())
}

#[derive(Clone, Debug)]
pub struct NpcType {
    pub id: i32,
    pub name: Vec<u8>,
    pub size: i32,
    pub model_ids: Option<Vec<i32>>,
    pub head_models: Option<Vec<i32>>,
    pub bas_id: i32,
    pub recolor_src: Option<Vec<i16>>,
    pub recolor_dst: Option<Vec<i16>>,
    pub retex_src: Option<Vec<i16>>,
    pub retex_dst: Option<Vec<i16>>,
    pub recolor_dpalette: Option<Vec<i8>>,
    pub model_offsets: Option<Vec<Option<[i32; 3]>>>,
    pub display_on_minimap: bool,
    pub combat_level: i32,
    pub scale_xz: i32,
    pub scale_y: i32,
    pub ambient: i32,
    pub contrast: i32,
    pub head_icons: i32,
    pub turn_direction: i32,
    pub varpbit: i32,
    pub varp: i32,
    pub alternative_ids: Option<Vec<i32>>,
    pub visible: bool,
    pub delay_movement: bool,
    pub shadowed: bool,
    pub shadow_color: [i32; 2],
    pub shadow_alpha: [i32; 2],
    pub walking_flags: i32,
    pub icon_height: i32,
    pub overhead_sprite: i32,
    pub sounds: [i32; 4],
    pub ambient_sound_min_hear: i32,
    pub ambient_sound_max_hear: i32,
    pub ambient_sound_volume: i32,
    pub ambient_sound_delay: [i32; 2],
    pub has_tint: bool,
    pub tint: [i32; 4],
    pub game_type: i32,
    pub priorities: [bool; 3],
    pub instrument_sound_effect: bool,
    pub mec_id: i32,
    pub shadow_size: i32,
    pub size_shift: i32,
    pub quests: Option<Vec<i32>>,
    pub options: [Option<Vec<u8>>; 5],
    pub primary_cursor_index: i32,
    pub primary_cursor: i32,
    pub secondary_cursor_index: i32,
    pub secondary_cursor: i32,
    pub attack_cursor: i32,
}

impl NpcType {
    pub fn new(id: i32) -> Self {
        Self {
            id,
            name: b"null".to_vec(),
            size: 1,
            model_ids: None,
            head_models: None,
            bas_id: -1,
            recolor_src: None,
            recolor_dst: None,
            retex_src: None,
            retex_dst: None,
            recolor_dpalette: None,
            model_offsets: None,
            display_on_minimap: true,
            combat_level: -1,
            scale_xz: 128,
            scale_y: 128,
            ambient: 0,
            contrast: 0,
            head_icons: -1,
            turn_direction: 32,
            varpbit: -1,
            varp: -1,
            alternative_ids: None,
            visible: true,
            delay_movement: true,
            shadowed: true,
            shadow_color: [0, 0],
            shadow_alpha: [-96, -16],
            walking_flags: 0,
            icon_height: -1,
            overhead_sprite: -1,
            sounds: [-1; 4],
            ambient_sound_min_hear: 0,
            ambient_sound_max_hear: 0,
            ambient_sound_volume: 255,
            ambient_sound_delay: [0, 0],
            has_tint: false,
            tint: [0; 4],
            game_type: 0,
            priorities: [false; 3],
            instrument_sound_effect: false,
            mec_id: 0,
            shadow_size: 0,
            size_shift: 0,
            quests: None,
            options: Default::default(),
            primary_cursor_index: -1,
            primary_cursor: -1,
            secondary_cursor_index: -1,
            secondary_cursor: -1,
            attack_cursor: -1,
        }
    }

    pub fn decode(id: i32, data: &[u8]) -> R<Self> {
        let mut npc = Self::new(id);
        let mut buf = Buf::new(data);
        loop {
            let opcode = buf.u8()?;
            if opcode == 0 {
                break;
            }
            npc.opcode(opcode, &mut buf)?;
        }
        if npc.model_ids.is_none() {
            npc.model_ids = Some(Vec::new());
        }
        if npc.game_type == -1 {
            npc.game_type = 1;
        }
        Ok(npc)
    }

    pub fn load(res: &dyn Resources, id: i32) -> R<Self> {
        match res.npc(id) {
            Some(data) => Self::decode(id, &data),
            None => {
                let mut npc = Self::new(id);
                npc.model_ids = Some(Vec::new());
                Ok(npc)
            }
        }
    }

    fn opcode(&mut self, opcode: i32, buf: &mut Buf) -> R<()> {
        let optional = |value: i32| if value == 65535 { -1 } else { value };
        match opcode {
            1 => {
                let count = buf.u8()?;
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    list.push(buf.big_smart()?);
                }
                self.model_ids = Some(list);
            }
            2 => self.name = buf.string()?,
            12 => self.size = buf.u8()?,
            30..=34 => self.options[(opcode - 30) as usize] = Some(buf.string()?),
            150..=154 => self.options[(opcode - 150) as usize] = Some(buf.string()?),
            40 => {
                let (src, dst) = pairs(buf)?;
                self.recolor_src = Some(src);
                self.recolor_dst = Some(dst);
            }
            41 => {
                let (src, dst) = pairs(buf)?;
                self.retex_src = Some(src);
                self.retex_dst = Some(dst);
            }
            42 => {
                let count = buf.u8()?;
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    list.push(buf.i8()? as i8);
                }
                self.recolor_dpalette = Some(list);
            }
            60 => {
                let count = buf.u8()?;
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    list.push(buf.big_smart()?);
                }
                self.head_models = Some(list);
            }
            93 => self.display_on_minimap = false,
            95 => self.combat_level = buf.u16()?,
            97 => self.scale_xz = buf.u16()?,
            98 => self.scale_y = buf.u16()?,
            99 => self.priorities[2] = true,
            100 => self.ambient = buf.i8()?,
            101 => self.contrast = buf.i8()?,
            102 => self.head_icons = buf.u16()?,
            103 => self.turn_direction = buf.u16()?,
            106 | 118 => {
                self.varpbit = optional(buf.u16()?);
                self.varp = optional(buf.u16()?);
                let last = if opcode == 118 {
                    optional(buf.u16()?)
                } else {
                    -1
                };
                let length = buf.u8()?;
                let mut ids = vec![0; length as usize + 2];
                for slot in ids.iter_mut().take(length as usize + 1) {
                    *slot = optional(buf.u16()?);
                }
                ids[length as usize + 1] = last;
                self.alternative_ids = Some(ids);
            }
            107 => self.visible = false,
            109 => self.delay_movement = false,
            111 => self.shadowed = false,
            113 => {
                self.shadow_color = [i32::from(buf.u16()? as i16), i32::from(buf.u16()? as i16)];
            }
            114 => self.shadow_alpha = [buf.i8()?, buf.i8()?],
            119 => self.walking_flags = buf.i8()?,
            121 => {
                let size = self.model_ids.as_ref().ok_or(ModelError::Null)?.len();
                let mut list: Vec<Option<[i32; 3]>> = vec![None; size];
                let count = buf.u8()?;
                for _ in 0..count {
                    let model = buf.u8()? as usize;
                    let entry = [buf.i8()?, buf.i8()?, buf.i8()?];
                    *list.get_mut(model).ok_or(ModelError::Bounds)? = Some(entry);
                }
                self.model_offsets = Some(list);
            }
            123 => self.icon_height = buf.u16()?,
            125 => {
                buf.i8()?;
            }
            127 => self.bas_id = buf.u16()?,
            128 => {
                buf.u8()?;
            }
            134 => {
                for slot in 0..4 {
                    self.sounds[slot] = optional(buf.u16()?);
                }
                self.ambient_sound_min_hear = buf.u8()?;
            }
            135 => {
                self.primary_cursor_index = buf.u8()?;
                self.primary_cursor = buf.u16()?;
            }
            136 => {
                self.secondary_cursor_index = buf.u8()?;
                self.secondary_cursor = buf.u16()?;
            }
            137 => self.attack_cursor = buf.u16()?,
            138 => self.overhead_sprite = buf.big_smart()?,
            140 => self.ambient_sound_volume = buf.u8()?,
            141 => self.priorities[0] = true,
            142 => self.mec_id = buf.u16()?,
            143 => self.priorities[1] = true,
            155 => {
                self.has_tint = true;
                for slot in 0..4 {
                    self.tint[slot] = buf.i8()?;
                }
            }
            158 => self.game_type = 1,
            159 => self.game_type = 0,
            160 => {
                let count = buf.u8()?;
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    list.push(buf.u16()?);
                }
                self.quests = Some(list);
            }
            162 => self.instrument_sound_effect = true,
            163 => self.shadow_size = buf.u8()?,
            164 => {
                self.ambient_sound_delay[1] = buf.u16()?;
                self.ambient_sound_delay[0] = buf.u16()?;
            }
            165 => self.size_shift = buf.u8()?,
            168 => self.ambient_sound_max_hear = buf.u8()?,
            169 => self.has_tint = false,
            249 => {
                let count = buf.u8()?;
                for _ in 0..count {
                    let is_string = buf.u8()? == 1;
                    buf.u24()?;
                    if is_string {
                        buf.string()?;
                    } else {
                        buf.i32()?;
                    }
                }
            }
            _ => {}
        }
        Ok(())
    }

    pub fn body_mesh(&self, res: &dyn Resources, bas: Option<&BasType>) -> R<Mesh> {
        let ids = self.model_ids.as_ref().ok_or(ModelError::Null)?;
        let mut meshes: Vec<Option<Mesh>> = vec![None; ids.len()];
        for (index, id) in ids.iter().enumerate() {
            if *id == -1 {
                continue;
            }
            let mut mesh = load_mesh(res, *id)?;
            if let Some(mesh) = mesh.as_mut() {
                upscaled(mesh)?;
                if let Some(offsets) = &self.model_offsets {
                    if let Some(Some(offset)) = offsets.get(index) {
                        mesh.translate(offset[0], offset[1], offset[2])?;
                    }
                }
            }
            meshes[index] = mesh;
        }
        if let Some(transforms) = bas.and_then(|b| b.obj_vertex_transforms.as_ref()) {
            for (index, entry) in transforms.iter().enumerate() {
                if let Some(Some(mesh)) = meshes.get_mut(index) {
                    apply_transform(mesh, entry)?;
                }
            }
        }
        if meshes.len() == 1 {
            meshes.remove(0).ok_or(ModelError::Null)
        } else {
            let count = meshes.len();
            merged(&meshes, count)
        }
    }

    pub fn head_mesh(&self, res: &dyn Resources) -> R<Option<Mesh>> {
        let Some(ids) = &self.head_models else {
            return Ok(None);
        };
        let mut meshes = Vec::with_capacity(ids.len());
        for id in ids {
            meshes.push(load_mesh(res, *id)?);
        }
        for mesh in meshes.iter_mut().flatten() {
            upscaled(mesh)?;
        }
        if meshes.len() == 1 {
            Ok(Some(meshes.remove(0).ok_or(ModelError::Null)?))
        } else {
            let count = meshes.len();
            Ok(Some(merged(&meshes, count)?))
        }
    }
}

pub const IDK_PART_TABLE: [i32; 8] = [8, 11, 4, 6, 9, 7, 10, 0];

pub struct PlayerAppearance {
    pub appearance: Vec<i32>,
    pub modifiers: Option<Vec<Option<Customisation>>>,
    pub female: bool,
    pub bas_id: i32,
    pub recolor_dst_indices: [i32; 10],
}

impl PlayerAppearance {
    pub fn hash(&self) -> i64 {
        let table = crc64_table();
        let mut hash: i64 = -1;
        let mut feed = |byte: i32| {
            hash = ((hash as u64) >> 8) as i64 ^ table[((hash ^ i64::from(byte)) & 0xff) as usize];
        };
        feed(self.bas_id >> 8);
        feed(self.bas_id);
        for part in &self.appearance {
            feed(part >> 24);
            feed(part >> 16);
            feed(part >> 8);
            feed(*part);
        }
        if let Some(modifiers) = &self.modifiers {
            for m in modifiers.iter().flatten() {
                let (body, head): (&[i32], &[i32]) = if self.female {
                    (&m.female_body, &m.female_head)
                } else {
                    (&m.male_body, &m.male_head)
                };
                for v in body.iter().chain(head) {
                    feed(v >> 8);
                    feed(*v);
                }
                for v in m
                    .recol_dst
                    .iter()
                    .flatten()
                    .chain(m.retex_dst.iter().flatten())
                {
                    feed(i32::from(*v) >> 8);
                    feed(i32::from(*v));
                }
            }
        }
        for index in self.recolor_dst_indices {
            feed(index);
        }
        feed(i32::from(self.female));
        hash
    }

    fn modifier(&self, slot: usize) -> Option<&Customisation> {
        self.modifiers.as_ref()?.get(slot)?.as_ref()
    }

    pub fn body_mesh(
        &self,
        res: &dyn Resources,
        bas: Option<&BasType>,
        defaults: &EquipmentDefaults,
        replacement_shield: i32,
        replacement_weapon: i32,
    ) -> R<Mesh> {
        let mut parts = self.appearance.clone();
        let mut shield = false;
        let mut weapon = false;
        if replacement_shield >= 0 || replacement_weapon >= 0 {
            if replacement_shield >= 0 && defaults.shield_slot != -1 {
                shield = true;
                let value = if replacement_shield == 65535 {
                    0
                } else {
                    replacement_shield | 0x4000_0000
                };
                crate::reader::put_slice(&mut parts, i64::from(defaults.shield_slot), value)?;
                for slot in defaults
                    .hidden_shield_slots
                    .as_ref()
                    .ok_or(ModelError::Null)?
                {
                    crate::reader::put_slice(&mut parts, i64::from(*slot), 0)?;
                }
            }
            if replacement_weapon >= 0 && defaults.weapon_slot != -1 {
                weapon = true;
                let value = if replacement_weapon == 65535 {
                    0
                } else {
                    replacement_weapon | 0x4000_0000
                };
                crate::reader::put_slice(&mut parts, i64::from(defaults.weapon_slot), value)?;
                for slot in defaults
                    .hidden_weapon_slots
                    .as_ref()
                    .ok_or(ModelError::Null)?
                {
                    crate::reader::put_slice(&mut parts, i64::from(*slot), 0)?;
                }
            }
        }
        let mut meshes: Vec<Option<Mesh>> = vec![None; parts.len()];
        for (slot, part) in parts.iter().enumerate() {
            let replaced = (slot == 5 && shield) || (slot == 3 && weapon);
            if part & 0x4000_0000 != 0 {
                let modifier = if replaced { None } else { self.modifier(slot) };
                let ty = item(res, part & 0x3fff_ffff)?;
                meshes[slot] = ty.body_mesh(res, self.female, modifier)?;
            } else if *part < 0 {
                let idk = IdkType::load(res, part & 0x3fff_ffff)?;
                meshes[slot] = idk.body_mesh(res)?;
            }
        }
        if let Some(transforms) = bas.and_then(|b| b.obj_vertex_transforms.as_ref()) {
            for (index, entry) in transforms.iter().enumerate() {
                if at_slice(&vec![0u8; meshes.len()], index as i64).is_err() {
                    return Err(ModelError::Bounds);
                }
                if let Some(mesh) = meshes[index].as_mut() {
                    apply_transform(mesh, entry)?;
                }
            }
        }
        let count = meshes.len();
        merged(&meshes, count)
    }

    pub fn head_mesh(&self, res: &dyn Resources) -> R<Mesh> {
        let mut meshes: Vec<Option<Mesh>> = vec![None; self.appearance.len()];
        let mut count = 0;
        for (slot, part) in self.appearance.iter().enumerate() {
            let mesh = if part & 0x4000_0000 != 0 {
                let ty = item(res, part & 0x3fff_ffff)?;
                ty.head_mesh(res, self.female, self.modifier(slot))?
            } else if *part < 0 {
                Some(IdkType::load(res, part & 0x3fff_ffff)?.head_mesh(res)?)
            } else {
                None
            };
            if let Some(mesh) = mesh {
                meshes[count] = Some(mesh);
                count += 1;
            }
        }
        merged(&meshes, count)
    }
}

pub fn plain_head_mesh(res: &dyn Resources, idks: [i32; 3]) -> R<Mesh> {
    let mut meshes: Vec<Option<Mesh>> = vec![None; 3];
    let mut count = 0;
    for id in idks {
        if id != -1 {
            meshes[count] = Some(IdkType::load(res, id)?.head_mesh(res)?);
            count += 1;
        }
    }
    merged(&meshes, count)
}

#[derive(Clone, Debug)]
pub struct SpotAnimType {
    pub sequence_id: i32,
    pub replay: bool,
    pub model_id: i32,
    pub scale_xz: i32,
    pub scale_y: i32,
    pub rotation: i32,
    pub brightness: i32,
    pub contrast: i32,
    pub contour_type: i8,
    pub contour_modifier: i32,
    pub recolor_src: Option<Vec<i16>>,
    pub recolor_dst: Option<Vec<i16>>,
    pub retex_src: Option<Vec<i16>>,
    pub retex_dst: Option<Vec<i16>>,
}

impl Default for SpotAnimType {
    fn default() -> Self {
        Self {
            sequence_id: -1,
            replay: false,
            model_id: 0,
            scale_xz: 128,
            scale_y: 128,
            rotation: 0,
            brightness: 0,
            contrast: 0,
            contour_type: 0,
            contour_modifier: -1,
            recolor_src: None,
            recolor_dst: None,
            retex_src: None,
            retex_dst: None,
        }
    }
}

impl SpotAnimType {
    pub fn decode(data: &[u8]) -> R<Self> {
        let mut ty = Self::default();
        let mut buf = Buf::new(data);
        loop {
            let opcode = buf.u8()?;
            if opcode == 0 {
                return Ok(ty);
            }
            ty.decode_opcode(&mut buf, opcode)?;
        }
    }

    fn decode_opcode(&mut self, buf: &mut Buf, opcode: i32) -> R<()> {
        match opcode {
            1 => self.model_id = buf.big_smart()?,
            2 => self.sequence_id = buf.big_smart()?,
            4 => self.scale_xz = buf.u16()?,
            5 => self.scale_y = buf.u16()?,
            6 => self.rotation = buf.u16()?,
            7 => self.brightness = buf.u8()?,
            8 => self.contrast = buf.u8()?,
            9 => {
                self.contour_type = 3;
                self.contour_modifier = 8224;
            }
            10 => self.replay = true,
            11 => self.contour_type = 1,
            12 => self.contour_type = 4,
            13 => self.contour_type = 5,
            14 => {
                self.contour_type = 2;
                self.contour_modifier = buf.u8()?.wrapping_mul(256);
            }
            15 => {
                self.contour_type = 3;
                self.contour_modifier = buf.u16()?;
            }
            16 => {
                self.contour_type = 3;
                self.contour_modifier = buf.i32()?;
            }
            40 => {
                let (src, dst) = pairs(buf)?;
                self.recolor_src = Some(src);
                self.recolor_dst = Some(dst);
            }
            41 => {
                let (src, dst) = pairs(buf)?;
                self.retex_src = Some(src);
                self.retex_dst = Some(dst);
            }
            _ => {}
        }
        Ok(())
    }

    pub fn mesh(&self, res: &dyn Resources) -> R<Option<Mesh>> {
        let Some(mut mesh) = load_mesh(res, self.model_id)? else {
            return Ok(None);
        };
        upscaled(&mut mesh)?;
        apply_colors(&mut mesh, &self.recolor_src, &self.recolor_dst, false)?;
        apply_colors(&mut mesh, &self.retex_src, &self.retex_dst, true)?;
        Ok(Some(mesh))
    }

    pub fn fixed_rotation(&self) -> i32 {
        match self.rotation {
            90 => 4096,
            180 => 8192,
            270 => 12288,
            _ => 0,
        }
    }
}

fn crc64_table() -> [i64; 256] {
    let mut table = [0i64; 256];
    for (index, slot) in table.iter_mut().enumerate() {
        let mut hash = index as i64;
        for _ in 0..8 {
            hash = if hash & 1 == 1 {
                (((hash as u64) >> 1) as i64) ^ -3_932_672_073_523_589_310
            } else {
                ((hash as u64) >> 1) as i64
            };
        }
        *slot = hash;
    }
    table
}
