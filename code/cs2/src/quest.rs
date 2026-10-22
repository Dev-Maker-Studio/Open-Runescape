use crate::{EnumValue, HostError, ParamTable, VarHost, Varbit};
use std::sync::Arc;

const WINDOWS_1252: [u16; 32] = [
    0x20ac, 0, 0x201a, 0x0192, 0x201e, 0x2026, 0x2020, 0x2021, 0x02c6, 0x2030, 0x0160, 0x2039,
    0x0152, 0, 0x017d, 0, 0, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014, 0x02dc,
    0x2122, 0x0161, 0x203a, 0x0153, 0, 0x017e, 0x0178,
];

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct QuestDecodeError;

impl std::fmt::Display for QuestDecodeError {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter.write_str("quest definition is malformed")
    }
}

impl std::error::Error for QuestDecodeError {}

struct Reader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl Reader<'_> {
    fn byte(&mut self) -> Result<u8, QuestDecodeError> {
        let value = *self.bytes.get(self.position).ok_or(QuestDecodeError)?;
        self.position += 1;
        Ok(value)
    }

    fn unsigned_short(&mut self) -> Result<i32, QuestDecodeError> {
        let high = i32::from(self.byte()?);
        Ok((high << 8) | i32::from(self.byte()?))
    }

    fn unsigned_medium(&mut self) -> Result<i32, QuestDecodeError> {
        let high = i32::from(self.byte()?);
        Ok((high << 16) | self.unsigned_short()?)
    }

    fn int(&mut self) -> Result<i32, QuestDecodeError> {
        let high = self.unsigned_short()?;
        Ok((high << 16) | self.unsigned_short()?)
    }

    fn big_smart(&mut self) -> Result<i32, QuestDecodeError> {
        let peek = *self.bytes.get(self.position).ok_or(QuestDecodeError)?;
        if peek >= 128 {
            return Ok(self.int()? & 0x7fff_ffff);
        }
        let value = self.unsigned_short()?;
        Ok(if value == 32767 { -1 } else { value })
    }

    fn string(&mut self) -> Result<Arc<str>, QuestDecodeError> {
        let mut units: Vec<u16> = Vec::new();
        loop {
            let value = self.byte()?;
            if value == 0 {
                break;
            }
            let unit = if (128..160).contains(&value) {
                match WINDOWS_1252[usize::from(value) - 128] {
                    0 => 63,
                    mapped => mapped,
                }
            } else {
                u16::from(value)
            };
            units.push(unit);
        }
        Ok(Arc::from(String::from_utf16_lossy(&units)))
    }

    fn jag_string(&mut self) -> Result<Arc<str>, QuestDecodeError> {
        if self.byte()? != 0 {
            return Err(QuestDecodeError);
        }
        self.string()
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct QuestVarRange {
    pub id: i32,
    pub start: i32,
    pub complete: i32,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct QuestVarRequirement {
    pub id: i32,
    pub min: i32,
    pub max: i32,
    pub name: Arc<str>,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct QuestDefinition {
    pub name: Option<Arc<str>>,
    pub sort_name: Option<Arc<str>>,
    pub kind: i32,
    pub difficulty: i32,
    pub members: bool,
    pub quest_point_reward: i32,
    pub prerequisites: Option<Vec<i32>>,
    pub level_requirements: Option<Vec<[i32; 2]>>,
    pub quest_point_requirement: i32,
    pub graphic_id: i32,
    pub varp_requirements: Option<Vec<QuestVarRequirement>>,
    pub varbit_requirements: Option<Vec<QuestVarRequirement>>,
    pub varp_values: Option<Vec<QuestVarRange>>,
    pub varbit_values: Option<Vec<QuestVarRange>>,
    pub params: Option<ParamTable>,
    pub param_buckets: usize,
}

fn skill_level(host: &dyn VarHost, skill: i32) -> Result<i32, HostError> {
    let state = host.player_state()?;
    usize::try_from(skill)
        .ok()
        .and_then(|index| state.skill_level_actual.get(index))
        .copied()
        .ok_or_else(|| HostError::State(format!("array index {skill} out of bounds")))
}

fn varbit_value(host: &dyn VarHost, id: i32) -> Result<i32, HostError> {
    let definition = Varbit::lookup(host, id)?;
    let value = host.varp(i32::from(definition.varp))?;
    Ok(definition.read(value))
}

fn in_range(value: i32, requirement: &QuestVarRequirement) -> bool {
    value >= requirement.min && value <= requirement.max
}

fn ranges_reach(
    ranges: &Option<Vec<QuestVarRange>>,
    read: impl Fn(i32) -> Result<i32, HostError>,
    threshold: impl Fn(&QuestVarRange) -> i32,
) -> Result<bool, HostError> {
    for range in ranges.iter().flatten() {
        if read(range.id)? >= threshold(range) {
            return Ok(true);
        }
    }
    Ok(false)
}

impl QuestDefinition {
    pub fn decode(bytes: &[u8]) -> Result<Self, QuestDecodeError> {
        let mut reader = Reader { bytes, position: 0 };
        let mut quest = Self {
            graphic_id: -1,
            ..Self::default()
        };
        loop {
            let opcode = reader.byte()?;
            match opcode {
                0 => break,
                1 => quest.name = Some(reader.jag_string()?),
                2 => quest.sort_name = Some(reader.jag_string()?),
                3 | 4 => {
                    let count = reader.byte()?;
                    let mut ranges = Vec::with_capacity(usize::from(count));
                    for _ in 0..count {
                        ranges.push(QuestVarRange {
                            id: reader.unsigned_short()?,
                            start: reader.int()?,
                            complete: reader.int()?,
                        });
                    }
                    if opcode == 3 {
                        quest.varp_values = Some(ranges);
                    } else {
                        quest.varbit_values = Some(ranges);
                    }
                }
                5 => {
                    reader.unsigned_short()?;
                }
                6 => quest.kind = i32::from(reader.byte()?),
                7 => quest.difficulty = i32::from(reader.byte()?),
                8 => quest.members = true,
                9 => quest.quest_point_reward = i32::from(reader.byte()?),
                10 => {
                    for _ in 0..reader.byte()? {
                        reader.int()?;
                    }
                }
                12 => {
                    reader.int()?;
                }
                13 => {
                    let count = reader.byte()?;
                    let mut ids = Vec::with_capacity(usize::from(count));
                    for _ in 0..count {
                        ids.push(reader.unsigned_short()?);
                    }
                    quest.prerequisites = Some(ids);
                }
                14 => {
                    let count = reader.byte()?;
                    let mut levels = Vec::with_capacity(usize::from(count));
                    for _ in 0..count {
                        levels.push([i32::from(reader.byte()?), i32::from(reader.byte()?)]);
                    }
                    quest.level_requirements = Some(levels);
                }
                15 => quest.quest_point_requirement = reader.unsigned_short()?,
                17 => quest.graphic_id = reader.big_smart()?,
                18 | 19 => {
                    let count = reader.byte()?;
                    let mut requirements = Vec::with_capacity(usize::from(count));
                    for _ in 0..count {
                        requirements.push(QuestVarRequirement {
                            id: reader.int()?,
                            min: reader.int()?,
                            max: reader.int()?,
                            name: reader.string()?,
                        });
                    }
                    if opcode == 18 {
                        quest.varp_requirements = Some(requirements);
                    } else {
                        quest.varbit_requirements = Some(requirements);
                    }
                }
                249 => {
                    let count = reader.byte()?;
                    if quest.params.is_none() {
                        quest.param_buckets = usize::from(count).next_power_of_two() * usize::from(count > 0);
                    }
                    if count > 0 && quest.param_buckets == 0 {
                        return Err(QuestDecodeError);
                    }
                    let params = quest.params.get_or_insert_with(ParamTable::new);
                    for _ in 0..count {
                        let is_text = reader.byte()? == 1;
                        let key = reader.unsigned_medium()?;
                        let value = if is_text {
                            EnumValue::Text(reader.string()?)
                        } else {
                            EnumValue::Int(reader.int()?)
                        };
                        params.insert(key, value);
                    }
                }
                _ => {}
            }
        }
        if quest.sort_name.is_none() {
            quest.sort_name.clone_from(&quest.name);
        }
        Ok(quest)
    }

    pub fn is_started(&self, host: &dyn VarHost) -> Result<bool, HostError> {
        Ok(ranges_reach(&self.varp_values, |id| host.varp(id), |r| r.start)?
            || ranges_reach(&self.varbit_values, |id| varbit_value(host, id), |r| r.start)?)
    }

    pub fn is_complete(&self, host: &dyn VarHost) -> Result<bool, HostError> {
        Ok(
            ranges_reach(&self.varp_values, |id| host.varp(id), |r| r.complete)?
                || ranges_reach(&self.varbit_values, |id| varbit_value(host, id), |r| r.complete)?,
        )
    }

    pub fn varp_requirement_met(&self, host: &dyn VarHost, index: i32) -> Result<bool, HostError> {
        let Some(requirement) = indexed(&self.varp_requirements, index) else {
            return Ok(false);
        };
        Ok(in_range(host.varp(requirement.id)?, requirement))
    }

    pub fn varbit_requirement_met(
        &self,
        host: &dyn VarHost,
        index: i32,
    ) -> Result<bool, HostError> {
        let Some(requirement) = indexed(&self.varbit_requirements, index) else {
            return Ok(false);
        };
        Ok(in_range(varbit_value(host, requirement.id)?, requirement))
    }

    pub fn level_requirement_met(&self, host: &dyn VarHost, index: i32) -> Result<bool, HostError> {
        let Some([skill, level]) = indexed(&self.level_requirements, index) else {
            return Ok(false);
        };
        Ok(skill_level(host, *skill)? >= *level)
    }

    pub fn param_text(&self, key: i32) -> Option<Result<Arc<str>, ()>> {
        if self.params.is_some() && self.param_buckets == 0 {
            return Some(Err(()));
        }
        match self.params.as_ref()?.get(&key)? {
            EnumValue::Text(text) => Some(Ok(Arc::clone(text))),
            EnumValue::Int(_) => Some(Err(())),
        }
    }

    pub fn param_int(&self, key: i32) -> Option<Result<i32, ()>> {
        if self.params.is_some() && self.param_buckets == 0 {
            return Some(Err(()));
        }
        match self.params.as_ref()?.get(&key)? {
            EnumValue::Int(value) => Some(Ok(*value)),
            EnumValue::Text(_) => Some(Err(())),
        }
    }
}

pub(crate) fn indexed<T>(values: &Option<Vec<T>>, index: i32) -> Option<&T> {
    let index = usize::try_from(index).ok()?;
    values.as_ref()?.get(index)
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct QuestDefs {
    quests: Vec<QuestDefinition>,
}

impl QuestDefs {
    pub fn new(quests: Vec<QuestDefinition>) -> Self {
        Self { quests }
    }

    pub fn from_files(files: &[Option<Vec<u8>>]) -> Result<Self, QuestDecodeError> {
        let mut quests = Vec::with_capacity(files.len());
        for file in files {
            quests.push(match file {
                Some(bytes) => QuestDefinition::decode(bytes)?,
                None => QuestDefinition::decode(&[0])?,
            });
        }
        Ok(Self { quests })
    }

    pub fn len(&self) -> usize {
        self.quests.len()
    }

    pub fn is_empty(&self) -> bool {
        self.quests.is_empty()
    }

    pub fn list(&self, id: i32) -> Result<&QuestDefinition, HostError> {
        usize::try_from(id)
            .ok()
            .and_then(|index| self.quests.get(index))
            .ok_or_else(|| HostError::State(format!("quest {id} out of bounds")))
    }

    pub fn completed_points(&self, host: &dyn VarHost) -> Result<i32, HostError> {
        let mut total = 0i32;
        for quest in &self.quests {
            if quest.is_complete(host)? {
                total = total.wrapping_add(quest.quest_point_reward);
            }
        }
        Ok(total)
    }

    pub fn points_requirement_met(
        &self,
        host: &dyn VarHost,
        quest: &QuestDefinition,
    ) -> Result<bool, HostError> {
        Ok(self.completed_points(host)? >= quest.quest_point_requirement)
    }

    pub fn prerequisite_met(
        &self,
        host: &dyn VarHost,
        quest: &QuestDefinition,
        index: i32,
    ) -> Result<bool, HostError> {
        let Some(id) = indexed(&quest.prerequisites, index) else {
            return Ok(false);
        };
        self.list(*id)?.is_complete(host)
    }

    pub fn all_requirements_met(
        &self,
        host: &dyn VarHost,
        quest: &QuestDefinition,
    ) -> Result<bool, HostError> {
        if !self.points_requirement_met(host, quest)? {
            return Ok(false);
        }
        for [skill, level] in quest.level_requirements.iter().flatten() {
            if skill_level(host, *skill)? < *level {
                return Ok(false);
            }
        }
        for id in quest.prerequisites.iter().flatten() {
            if !self.list(*id)?.is_complete(host)? {
                return Ok(false);
            }
        }
        for requirement in quest.varp_requirements.iter().flatten() {
            if !in_range(host.varp(requirement.id)?, requirement) {
                return Ok(false);
            }
        }
        for requirement in quest.varbit_requirements.iter().flatten() {
            if !in_range(varbit_value(host, requirement.id)?, requirement) {
                return Ok(false);
            }
        }
        Ok(true)
    }
}
