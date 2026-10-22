use crate::{
    Counts, HostError, Instruction, Opcode, Operand, Script, StackKind, VarHost, Varbit,
    VarbitLookup, VmError, VmErrorKind,
};
use std::{collections::BTreeMap, sync::Arc};

mod appearance;
mod chat;
mod chat_send;
mod message;
mod player_op;
mod chat_phrase;
mod dialog;
mod clan;
mod client;
mod clock;
mod components;
mod config;
mod entity;
mod enums;
mod font;
mod media;
mod pause;
mod interface;
mod inventory;
mod outbound;
mod player;
mod bounds;
mod projection;
mod quick_chat;
mod query;
mod quest;
mod scene;
mod social;
mod text;
mod userdetail;
mod world;
mod world_map;
mod mini_menu;
mod platform;
mod world_map_effects;
mod login_effects;
mod bas;
mod sub_interface;

const STACK_CAPACITY: usize = 1000;
const CALL_CAPACITY: usize = 50;
const ARRAY_CAPACITY: usize = 5000;

pub trait ScriptRepository {
    fn script(&self, id: i32) -> Option<&Script>;
}

impl ScriptRepository for BTreeMap<i32, Script> {
    fn script(&self, id: i32) -> Option<&Script> {
        self.get(&id)
    }
}

#[derive(Clone, Copy, Debug, Default)]
pub struct Arguments<'a> {
    pub ints: &'a [i32],
    pub strings: &'a [Option<Arc<str>>],
    pub longs: &'a [i64],
}

#[derive(Clone, Copy, Debug)]
pub struct VmLimits {
    pub max_instructions: u64,
    pub max_local_values: usize,
    pub max_string_bytes: usize,
    pub max_join_bytes: usize,
}

impl Default for VmLimits {
    fn default() -> Self {
        Self {
            max_instructions: 200_000,
            max_local_values: 100_000,
            max_string_bytes: 1024 * 1024,
            max_join_bytes: 16 * 1024 * 1024,
        }
    }
}

#[derive(Debug, Eq, PartialEq)]
pub struct Locals {
    pub ints: Vec<i32>,
    pub strings: Vec<Option<Arc<str>>>,
    pub longs: Vec<i64>,
}

impl Locals {
    fn new(counts: Counts) -> Self {
        Self {
            ints: vec![0; usize::from(counts.ints)],
            strings: vec![None; usize::from(counts.strings)],
            longs: vec![0; usize::from(counts.longs)],
        }
    }
}

#[derive(Debug, Eq, PartialEq)]
pub struct Execution {
    pub instructions_executed: u64,
    pub joined_string_bytes: usize,
    pub locals: Locals,
}

pub struct Vm {
    limits: VmLimits,
    ints: Vec<i32>,
    strings: Vec<Option<Arc<str>>>,
    longs: Vec<i64>,
    arrays: [Vec<i32>; 5],
    array_lengths: [usize; 5],
    clan_channel_selection: Option<clan::Selection>,
    clan_settings_selection: Option<clan::Selection>,
    chat_phrase: Option<chat_phrase::Phrase>,
}

impl Default for Vm {
    fn default() -> Self {
        Self::new(VmLimits::default())
    }
}

struct Activation<'a> {
    id: i32,
    script: &'a Script,
    next: usize,
    locals: Locals,
}

struct Budget {
    locals: usize,
    join_bytes: usize,
}

impl Vm {
    pub fn new(limits: VmLimits) -> Self {
        Self {
            limits,
            ints: Vec::with_capacity(STACK_CAPACITY),
            strings: Vec::with_capacity(STACK_CAPACITY),
            longs: Vec::with_capacity(STACK_CAPACITY),
            arrays: std::array::from_fn(|_| vec![0; ARRAY_CAPACITY]),
            array_lengths: [0; 5],
            clan_channel_selection: None,
            clan_settings_selection: None,
            chat_phrase: None,
        }
    }

    pub fn int_stack(&self) -> &[i32] {
        &self.ints
    }
    pub fn string_stack(&self) -> &[Option<Arc<str>>] {
        &self.strings
    }
    pub fn long_stack(&self) -> &[i64] {
        &self.longs
    }
    pub fn array(&self, slot: usize) -> Option<&[i32]> {
        self.arrays
            .get(slot)
            .map(|values| &values[..self.array_lengths[slot]])
    }

    pub fn reset(&mut self) {
        self.ints.clear();
        self.strings.clear();
        self.longs.clear();
        self.array_lengths.fill(0);
        for values in &mut self.arrays {
            values.fill(0);
        }
    }

    pub fn run<R: ScriptRepository + ?Sized>(
        &mut self,
        id: i32,
        repository: &R,
        arguments: Arguments<'_>,
    ) -> Result<Execution, VmError> {
        self.run_inner(id, repository, arguments, None)
    }

    pub fn run_with_host<R: ScriptRepository + ?Sized>(
        &mut self,
        id: i32,
        repository: &R,
        arguments: Arguments<'_>,
        host: &mut dyn VarHost,
    ) -> Result<Execution, VmError> {
        self.run_inner(id, repository, arguments, Some(host))
    }

    fn run_inner<R: ScriptRepository + ?Sized>(
        &mut self,
        id: i32,
        repository: &R,
        arguments: Arguments<'_>,
        mut host: Option<&mut dyn VarHost>,
    ) -> Result<Execution, VmError> {
        let root_error = |kind| VmError {
            script_id: id,
            instruction: 0,
            kind,
        };
        let script = repository
            .script(id)
            .ok_or_else(|| root_error(VmErrorKind::MissingScript(id)))?;
        let counts = script.arguments();
        for (actual, expected, kind) in [
            (arguments.ints.len(), counts.ints, StackKind::Int),
            (arguments.strings.len(), counts.strings, StackKind::String),
            (arguments.longs.len(), counts.longs, StackKind::Long),
        ] {
            if actual != usize::from(expected) {
                return Err(root_error(VmErrorKind::ArgumentCount(kind)));
            }
        }
        let mut budget = Budget {
            locals: script.locals().total(),
            join_bytes: 0,
        };
        if budget.locals > self.limits.max_local_values {
            return Err(root_error(VmErrorKind::LocalLimit));
        }
        for value in arguments.strings {
            check_string(value.as_deref(), self.limits.max_string_bytes).map_err(root_error)?;
        }
        let mut locals = Locals::new(script.locals());
        locals.ints[..arguments.ints.len()].copy_from_slice(arguments.ints);
        locals.strings[..arguments.strings.len()].clone_from_slice(arguments.strings);
        locals.longs[..arguments.longs.len()].copy_from_slice(arguments.longs);
        self.ints.clear();
        self.strings.clear();
        let mut current = Activation {
            id,
            script,
            next: 0,
            locals,
        };
        let mut calls = Vec::with_capacity(CALL_CAPACITY);
        let mut executed = 0u64;
        loop {
            let id = current.id;
            let pc = current.next;
            let error = |kind| VmError {
                script_id: id,
                instruction: pc,
                kind,
            };
            if executed >= self.limits.max_instructions {
                return Err(error(VmErrorKind::InstructionLimit));
            }
            let instruction = current
                .script
                .instructions()
                .get(pc)
                .ok_or_else(|| error(VmErrorKind::InstructionPointer))?;
            current.next += 1;
            executed += 1;
            if self
                .step(
                    &mut current,
                    &mut calls,
                    repository,
                    instruction,
                    &mut budget,
                    &mut host,
                )
                .map_err(error)?
            {
                return Ok(Execution {
                    instructions_executed: executed,
                    joined_string_bytes: budget.join_bytes,
                    locals: current.locals,
                });
            }
        }
    }

    fn step<'a, R: ScriptRepository + ?Sized>(
        &mut self,
        current: &mut Activation<'a>,
        calls: &mut Vec<Activation<'a>>,
        repository: &'a R,
        instruction: &Instruction,
        budget: &mut Budget,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<bool, VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, Long, String as Text};
        let opcode = instruction.opcode;
        let operand = &instruction.operand;
        let int = || match operand {
            Operand::Int(n) => Ok(*n),
            _ => Err(VmErrorKind::InvalidOperand),
        };
        match opcode {
            PUSH_VAR | POP_VAR | PUSH_VARBIT | POP_VARBIT => {
                let host = host
                    .as_deref_mut()
                    .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
                let value = if matches!(opcode, POP_VAR | POP_VARBIT) {
                    Some(pop(&mut self.ints, Int)?)
                } else {
                    None
                };
                let id = int()?;
                if matches!(opcode, PUSH_VAR | POP_VAR) {
                    if id < 0 {
                        return Err(VmErrorKind::Host(HostError::InvalidVarp(id)));
                    }
                    if let Some(value) = value {
                        host.set_varp(id, value).map_err(VmErrorKind::Host)?;
                    } else {
                        let value = host.varp(id).map_err(VmErrorKind::Host)?;
                        push(&mut self.ints, value, Int)?;
                    }
                } else {
                    let definition = Varbit::lookup(host, id).map_err(VmErrorKind::Host)?;
                    let varp = i32::from(definition.varp);
                    let previous = host.varp(varp).map_err(VmErrorKind::Host)?;
                    if let Some(value) = value {
                        host.set_varp(varp, definition.write(previous, value))
                            .map_err(VmErrorKind::Host)?;
                    } else {
                        push(&mut self.ints, definition.read(previous), Int)?;
                    }
                }
            }
            GET_VARP_OLD | GET_VARPBIT_OLD => {
                let host = host
                    .as_deref_mut()
                    .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
                let id = int()?;
                let value = if opcode == GET_VARP_OLD {
                    host.entity_varp(id)
                } else {
                    host.entity_varbit(id).and_then(|lookup| {
                        let definition = match lookup {
                            VarbitLookup::Defined(definition) => definition,
                            VarbitLookup::Absent => Varbit {
                                varp: 0,
                                start: 0,
                                end: 0,
                            },
                        };
                        host.entity_varp(i32::from(definition.varp))
                            .map(|value| definition.entity_read(value))
                    })
                }
                .map_err(|error| match error {
                    HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                    error => VmErrorKind::Host(error),
                })?;
                push(&mut self.ints, value, Int)?;
            }
            PUSH_VARC_INT | POP_VARC_INT | PUSH_VARC_STRING | POP_VARC_STRING => {
                let state = host
                    .as_deref_mut()
                    .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?
                    .varcs()
                    .map_err(|error| match error {
                        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                        error => VmErrorKind::Host(error),
                    })?;
                let id = int()?;
                let host_error = |error| VmErrorKind::Host(HostError::Varc(error));
                match opcode {
                    PUSH_VARC_INT => {
                        let value = state.int(id).map_err(host_error)?;
                        push(&mut self.ints, value, Int)?;
                    }
                    POP_VARC_INT => {
                        let value = *self.ints.last().ok_or(VmErrorKind::StackUnderflow(Int))?;
                        state.set_int(id, value).map_err(host_error)?;
                        self.ints.pop();
                    }
                    PUSH_VARC_STRING => {
                        let value = state
                            .string(id)
                            .map_err(host_error)?
                            .unwrap_or_else(|| Arc::from(""));
                        check_string(Some(&value), self.limits.max_string_bytes)?;
                        push(&mut self.strings, Some(value), Text)?;
                    }
                    POP_VARC_STRING => {
                        let value = self
                            .strings
                            .last()
                            .ok_or(VmErrorKind::StackUnderflow(Text))?;
                        check_string(value.as_deref(), self.limits.max_string_bytes)?;
                        state.set_string(id, value.clone()).map_err(host_error)?;
                        self.strings.pop();
                    }
                    _ => unreachable!(),
                }
            }
            PUSH_CONSTANT_INT => push(&mut self.ints, int()?, Int)?,
            PUSH_CONSTANT_STRING => {
                let Operand::String(value) = operand else {
                    return Err(VmErrorKind::InvalidOperand);
                };
                check_string(Some(value), self.limits.max_string_bytes)?;
                push(&mut self.strings, Some(Arc::clone(value)), Text)?;
            }
            PUSH_LONG_CONSTANT => {
                let Operand::Long(value) = operand else {
                    return Err(VmErrorKind::InvalidOperand);
                };
                push(&mut self.longs, *value, Long)?;
            }
            PUSH_INT_LOCAL => {
                let value = *local(&current.locals.ints, int()?, Int)?;
                push(&mut self.ints, value, Int)?;
            }
            PUSH_STRING_LOCAL => {
                let value = local(&current.locals.strings, int()?, Text)?;
                check_string(value.as_deref(), self.limits.max_string_bytes)?;
                push(&mut self.strings, value.clone(), Text)?;
            }
            PUSH_LONG_LOCAL => {
                let value = *local(&current.locals.longs, int()?, Long)?;
                push(&mut self.longs, value, Long)?;
            }
            POP_INT_LOCAL => {
                let value = pop(&mut self.ints, Int)?;
                *local_mut(&mut current.locals.ints, int()?, Int)? = value;
            }
            POP_STRING_LOCAL => {
                let value = pop(&mut self.strings, Text)?;
                *local_mut(&mut current.locals.strings, int()?, Text)? = value;
            }
            POP_LONG_LOCAL => {
                let value = pop(&mut self.longs, Long)?;
                *local_mut(&mut current.locals.longs, int()?, Long)? = value;
            }
            POP_INT_DISCARD => {
                pop(&mut self.ints, Int)?;
            }
            POP_STRING_DISCARD => {
                pop(&mut self.strings, Text)?;
            }
            POP_LONG_DISCARD => {
                pop(&mut self.longs, Long)?;
            }
            JOIN_STRING => {
                let count = usize::try_from(int()?).map_err(|_| VmErrorKind::InvalidOperand)?;
                let start = self
                    .strings
                    .len()
                    .checked_sub(count)
                    .ok_or(VmErrorKind::StackUnderflow(Text))?;
                let parts = &self.strings[start..];
                let length = parts
                    .iter()
                    .try_fold(0usize, |total, part| {
                        total.checked_add(part.as_ref().map_or(4, |s| s.len()))
                    })
                    .filter(|length| *length <= self.limits.max_string_bytes)
                    .ok_or(VmErrorKind::StringLimit)?;
                let total = budget
                    .join_bytes
                    .checked_add(length)
                    .filter(|n| *n <= self.limits.max_join_bytes)
                    .ok_or(VmErrorKind::StringLimit)?;
                if start >= STACK_CAPACITY {
                    return Err(VmErrorKind::StackOverflow(Text));
                }
                let mut result = std::string::String::with_capacity(length);
                for part in parts {
                    result.push_str(part.as_deref().unwrap_or("null"));
                }
                self.strings.truncate(start);
                self.strings.push(Some(Arc::from(result)));
                budget.join_bytes = total;
            }
            BRANCH => jump(current, int()?)?,
            BRANCH_NOT
            | BRANCH_EQUALS
            | BRANCH_LESS_THAN
            | BRANCH_GREATER_THAN
            | BRANCH_LESS_THAN_OR_EQUALS
            | BRANCH_GREATER_THAN_OR_EQUALS => {
                let (left, right) = pair(&mut self.ints, Int)?;
                if comparison(opcode, left, right) {
                    jump(current, int()?)?;
                }
            }
            LONG_BRANCH_NOT
            | LONG_BRANCH_EQUALS
            | LONG_BRANCH_LESS_THAN
            | LONG_BRANCH_GREATER_THAN
            | LONG_BRANCH_LESS_THAN_OR_EQUALS
            | LONG_BRANCH_GREATER_THAN_OR_EQUALS => {
                let (left, right) = pair(&mut self.longs, Long)?;
                if comparison(opcode, left, right) {
                    jump(current, int()?)?;
                }
            }
            BRANCH_IF_TRUE | BRANCH_IF_FALSE => {
                let value = pop(&mut self.ints, Int)?;
                if value == i32::from(opcode == BRANCH_IF_TRUE) {
                    jump(current, int()?)?;
                }
            }
            SWITCH => {
                let index = int()?;
                let table = usize::try_from(index)
                    .ok()
                    .and_then(|n| current.script.switches().get(n))
                    .ok_or(VmErrorKind::SwitchIndex(index))?;
                if table.cases().is_empty() {
                    return Err(VmErrorKind::EmptySwitchTable);
                }
                let key = pop(&mut self.ints, Int)?;
                if let Some(offset) = table.lookup(key) {
                    jump(current, offset)?;
                }
            }
            DEFINE_ARRAY => {
                let packed = int()?;
                let length = pop(&mut self.ints, Int)?;
                if !(0..=ARRAY_CAPACITY as i32).contains(&length) {
                    return Err(VmErrorKind::ArrayLength(length));
                }
                let slot = array_slot(packed >> 16)?;
                self.array_lengths[slot] = length as usize;
                self.arrays[slot][..length as usize].fill(if packed & 0xffff == 105 {
                    0
                } else {
                    -1
                });
            }
            PUSH_ARRAY_INT => {
                let index = pop(&mut self.ints, Int)?;
                let slot = array_slot(int()?)?;
                let index = array_index(index, self.array_lengths[slot])?;
                push(&mut self.ints, self.arrays[slot][index], Int)?;
            }
            POP_ARRAY_INT => {
                let (index, value) = pair(&mut self.ints, Int)?;
                let slot = array_slot(int()?)?;
                let index = array_index(index, self.array_lengths[slot])?;
                self.arrays[slot][index] = value;
            }
            ARRAY_SORT => {
                let start = self
                    .ints
                    .len()
                    .checked_sub(3)
                    .ok_or(VmErrorKind::StackUnderflow(Int))?;
                let count = self.ints[start];
                let key_slot = self.ints[start + 1];
                let value_slot = self.ints[start + 2];
                self.ints.truncate(start);
                let key_slot = array_slot(key_slot)?;
                if count > self.array_lengths[key_slot] as i32 {
                    return Err(VmErrorKind::ArrayLength(count));
                }
                let value_slot = array_slot(value_slot)?;
                if count > self.array_lengths[value_slot] as i32 {
                    return Err(VmErrorKind::ArrayLength(count));
                }
                if key_slot == value_slot {
                    return Err(VmErrorKind::ArrayAlias(key_slot as i32));
                }
                let end = count.wrapping_sub(1);
                if end > 0 {
                    if end as usize >= ARRAY_CAPACITY {
                        return Err(VmErrorKind::ArrayIndex(end / 2));
                    }
                    let (keys, values) = if key_slot < value_slot {
                        let (left, right) = self.arrays.split_at_mut(value_slot);
                        (&mut left[key_slot], &mut right[0])
                    } else {
                        let (left, right) = self.arrays.split_at_mut(key_slot);
                        (&mut right[0], &mut left[value_slot])
                    };
                    paired_quicksort(keys, values, end as usize);
                }
            }
            GOSUB_WITH_PARAMS => {
                let id = int()?;
                let script = repository
                    .script(id)
                    .ok_or(VmErrorKind::MissingScript(id))?;
                if calls.len() == CALL_CAPACITY {
                    return Err(VmErrorKind::CallDepth);
                }
                let total = budget
                    .locals
                    .checked_add(script.locals().total())
                    .filter(|n| *n <= self.limits.max_local_values)
                    .ok_or(VmErrorKind::LocalLimit)?;
                let args = script.arguments();
                let i = argument_start(self.ints.len(), args.ints, Int)?;
                let s = argument_start(self.strings.len(), args.strings, Text)?;
                let l = argument_start(self.longs.len(), args.longs, Long)?;
                let mut locals = Locals::new(script.locals());
                locals.ints[..usize::from(args.ints)].copy_from_slice(&self.ints[i..]);
                locals.strings[..usize::from(args.strings)].clone_from_slice(&self.strings[s..]);
                locals.longs[..usize::from(args.longs)].copy_from_slice(&self.longs[l..]);
                self.ints.truncate(i);
                self.strings.truncate(s);
                self.longs.truncate(l);
                let caller = std::mem::replace(
                    current,
                    Activation {
                        id,
                        script,
                        next: 0,
                        locals,
                    },
                );
                calls.push(caller);
                budget.locals = total;
            }
            RETURN => {
                if let Some(caller) = calls.pop() {
                    budget.locals -= current.script.locals().total();
                    *current = caller;
                } else {
                    return Ok(true);
                }
            }
            ENUM
            | ENUM_STRING
            | ENUM_HASOUTPUT
            | ENUM_HASOUTPUT_STRING
            | ENUM_GETOUTPUTCOUNT
            | ENUM_GETREVERSECOUNT
            | ENUM_GETREVERSECOUNT_STRING
            | ENUM_GETREVERSEINDEX
            | ENUM_GETREVERSEINDEX_STRING => self.execute_enum(opcode, host)?,
            CC_FIND
            | IF_FIND
            | CC_CREATE
            | CC_DELETE
            | CC_DELETEALL
            | IF_HASSUB
            | IF_HASSUBMODAL
            | IF_HASSUBOVERLAY
            | CC_SENDTOFRONT
            | CC_SENDTOBACK
            | IF_SENDTOFRONT
            | IF_SENDTOBACK
            | CC_GETX
            | IF_GETX
            | CC_GETY
            | IF_GETY
            | CC_GETWIDTH
            | IF_GETWIDTH
            | CC_GETHEIGHT
            | IF_GETHEIGHT
            | CC_GETHIDE
            | IF_GETHIDE
            | CC_GETCOLOUR
            | IF_GETCOLOUR
            | CC_GETGRAPHIC
            | IF_GETGRAPHIC
            | CC_GETTRANS
            | IF_GETTRANS
            | CC_GETSCROLLX
            | IF_GETSCROLLX
            | CC_GETSCROLLY
            | IF_GETSCROLLY
            | CC_GETSCROLLWIDTH
            | IF_GETSCROLLWIDTH
            | CC_GETSCROLLHEIGHT
            | IF_GETSCROLLHEIGHT
            | CC_GETTEXT
            | IF_GETTEXT
            | CC_SETPOSITION
            | IF_SETPOSITION
            | CC_SETSIZE
            | IF_SETSIZE
            | CC_SETHIDE
            | IF_SETHIDE
            | CC_SETCOLOUR
            | IF_SETCOLOUR
            | CC_SETTEXT
            | IF_SETTEXT
            | CC_SETFILL
            | IF_SETFILL
            | CC_SETLINEWID
            | IF_SETLINEWID
            | CC_SETALPHA
            | IF_SETALPHA
            | CC_SETTEXTSHADOW
            | IF_SETTEXTSHADOW
            | CC_SETOUTLINE
            | IF_SETOUTLINE
            | CC_SETGRAPHICSHADOW
            | IF_SETGRAPHICSHADOW
            | CC_SETVFLIP
            | IF_SETVFLIP
            | CC_SETHFLIP
            | IF_SETHFLIP
            | CC_SET2DANGLE
            | IF_SET2DANGLE
            | CC_SETTILING
            | IF_SETTILING
            | CC_SETLINEDIRECTION
            | IF_SETLINEDIRECTION
            | CC_SETMAXLINES
            | IF_SETMAXLINES
            | CC_SETNOCLICKTHROUGH
            | IF_SETNOCLICKTHROUGH
            | CC_SETSCROLLPOS
            | IF_SETSCROLLPOS
            | CC_SETMODELZOOM
            | IF_SETMODELZOOM
            | CC_SETTEXTFONT
            | IF_SETTEXTFONT
            | CC_SETFONTMONO
            | IF_SETFONTMONO
            | CC_SETMODELORTHOG
            | IF_SETMODELORTHOG
            | CC_SETMODELORIGIN
            | IF_SETMODELORIGIN
            | CC_SETTEXTALIGN
            | IF_SETTEXTALIGN
            | CC_SETTARGETVERB
            | IF_SETTARGETVERB
            | CC_SETPAUSETEXT
            | IF_SETPAUSETEXT
            | CC_SETCLICKMASK
            | IF_SETCLICKMASK
            | CC_SETTEXTANTIMACRO
            | IF_SETTEXTANTIMACRO
            | CC_SETDRAGDEADZONE
            | IF_SETDRAGDEADZONE
            | CC_SETGRAPHIC
            | IF_SETGRAPHIC
            | CC_SETTRANS
            | IF_SETTRANS
            | CC_SETMODELANGLE
            | IF_SETMODELANGLE
            | CC_SETMODELLIGHTING
            | IF_SETMODELLIGHTING
            | CC_SETMODELLIGHTING_SUNROTATION
            | IF_SETMODELLIGHTING_SUNROTATION
            | CC_SETMODELTINT
            | IF_SETMODELTINT
            | CC_SETMODEL
            | IF_SETMODEL
            | CC_SETMODEL_ITEMCONTAINER
            | IF_SETMODEL_ITEMCONTAINER
            | CC_SETSCROLLSIZE
            | IF_SETSCROLLSIZE
            | CC_SETASPECT
            | IF_SETASPECT
            | CC_GETMODELZOOM
            | IF_GETMODELZOOM
            | CC_GETMODELANGLE_X
            | IF_GETMODELANGLE_X
            | CC_GETMODELANGLE_Y
            | IF_GETMODELANGLE_Y
            | CC_GETMODELANGLE_Z
            | IF_GETMODELANGLE_Z
            | CC_GETMODELXOF
            | IF_GETMODELXOF
            | CC_GETMODELYOF
            | IF_GETMODELYOF
            | CC_GETMODEL
            | CC_SETOP
            | IF_SETOP
            | CC_SETOPBASE
            | IF_SETOPBASE
            | CC_SETOPCURSOR
            | IF_SETOPCURSOR
            | CC_SETTARGETOPCURSOR
            | IF_SETTARGETOPCURSOR
            | CC_SETOPCHAR
            | IF_SETOPCHAR
            | CC_SETOPTCHAR
            | IF_SETOPTCHAR
            | CC_SETOPKEY
            | IF_SETOPKEY
            | CC_SETOPTKEY
            | IF_SETOPTKEY
            | CC_SETPARAM_INT
            | IF_SETPARAM_INT
            | CC_SETPARAM_STRING
            | IF_SETPARAM_STRING
            | CC_CLEAROPS
            | IF_CLEAROPS
            | CC_SETTARGETCURSORS
            | IF_SETTARGETCURSORS
            | CC_SETMOUSEOVERCURSOR
            | IF_SETMOUSEOVERCURSOR
            | CC_GETOP
            | IF_GETOP
            | CC_GETOPBASE
            | IF_GETOPBASE
            | CC_GETTARGETMASK
            | IF_GETTARGETMASK
            | CC_SETPARAM
            | CC_PARAM
            | CC_SETONCLICK
            | CC_SETONHOLD
            | CC_SETONRELEASE
            | CC_SETONMOUSEOVER
            | CC_SETONMOUSELEAVE
            | CC_SETONDRAG
            | CC_SETONTARGETLEAVE
            | CC_SETONVARTRANSMIT
            | CC_SETONTIMER
            | CC_SETONOP
            | CC_SETONDRAGCOMPLETE
            | CC_SETONCLICKREPEAT
            | CC_SETONMOUSEREPEAT
            | CC_SETONINVTRANSMIT
            | CC_SETONSTATTRANSMIT
            | CC_SETONTARGETENTER
            | CC_SETONSCROLLWHEEL
            | CC_SETONCHATTRANSMIT
            | CC_SETONKEY
            | CC_SETONFRIENDTRANSMIT
            | CC_SETONCLANCHANNELTRANSMIT
            | CC_SETONMISCTRANSMIT
            | CC_SETONDIALOGABORT
            | CC_SETONSUBCHANGE
            | CC_SETONSTOCKTRANSMIT
            | CC_SETONCAMFINISHED
            | CC_SETONRESIZE
            | CC_SETONVARCTRANSMIT
            | CC_SETONVARCSTRTRANSMIT
            | CC_SETONOPT
            | CC_SETONCLANSETTINGSTRANSMIT
            | CC_SETLINKACTIVECLANCHANNEL
            | CC_SETONVARCLANTRANSMIT
            | CC_CLEARSCRIPTHOOKS
            | IF_SETONCLICK
            | IF_SETONHOLD
            | IF_SETONRELEASE
            | IF_SETONMOUSEOVER
            | IF_SETONMOUSELEAVE
            | IF_SETONDRAG
            | IF_SETONTARGETLEAVE
            | IF_SETONVARTRANSMIT
            | IF_SETONTIMER
            | IF_SETONOP
            | IF_SETONDRAGCOMPLETE
            | IF_SETONCLICKREPEAT
            | IF_SETONMOUSEREPEAT
            | IF_SETONINVTRANSMIT
            | IF_SETONSTATTRANSMIT
            | IF_SETONTARGETENTER
            | IF_SETONSCROLLWHEEL
            | IF_SETONCHATTRANSMIT
            | IF_SETONKEY
            | IF_SETONFRIENDTRANSMIT
            | IF_SETONCLANTRANSMIT
            | IF_SETONMISCTRANSMIT
            | IF_SETONDIALOGABORT
            | IF_SETONSUBCHANGE
            | IF_SETONSTOCKTRANSMIT
            | IF_SETONCAMFINISHED
            | IF_SETONRESIZE
            | IF_SETONVARCTRANSMIT
            | IF_SETONVARCSTRTRANSMIT
            | IF_SETONOPT
            | IF_SETONCLANSETTINGSTRANSMIT
            | IF_SETONCLANCHANNELTRANSMIT
            | IF_SETONVARCLANTRANSMIT
            | IF_CLEARSCRIPTHOOKS
            | CC_GETLAYER
            | IF_GETLAYER
            | CC_GET2DANGLE
            | IF_GET2DANGLE
            | CC_GETID
            | IF_GETNEXTSUBID
            | CC_SETDRAGDEADTIME
            | IF_SETDRAGDEADTIME
            | CC_SETDRAGRENDERBEHAVIOUR
            | IF_SETDRAGRENDERBEHAVIOUR
            | CC_DRAGPICKUP
            | IF_DRAGPICKUP
            | CC_SETDRAGGABLE
            | IF_SETDRAGGABLE
            | CC_SETRECOL
            | IF_SETRECOL
            | CC_SETRETEX
            | IF_SETRETEX
            | CC_CALLONRESIZE
            | IF_CALLONRESIZE
            | CC_GETINVOBJECT
            | IF_GETINVOBJECT
            | CC_GETINVCOUNT
            | IF_GETINVCOUNT
            | CC_GETFONTMETRICS
            | IF_GETFONTMETRICS
            | CC_GETFONTGRAPHIC
            | IF_GETFONTGRAPHIC
            | CC_SETOBJECT
            | IF_SETOBJECT
            | CC_SETOBJECT_NONUM
            | IF_SETOBJECT_NONUM
            | CC_SETOBJECT_ALWAYSNUM
            | IF_SETOBJECT_ALWAYSNUM
            | CC_SETOBJECT_WEARCOL
            | IF_SETOBJECT_WEARCOL
            | CC_SETOBJECT_WEARCOL_NONUM
            | IF_SETOBJECT_WEARCOL_NONUM
            | CC_SETOBJECT_WEARCOL_ALWAYSNUM
            | IF_SETOBJECT_WEARCOL_ALWAYSNUM
            | CC_SETNPCHEAD
            | IF_SETNPCHEAD
            | CC_SETNPCMODEL
            | IF_SETNPCMODEL
            | CC_SETPLAYERMODEL
            | IF_SETPLAYERMODEL
            | CC_SETPLAYERHEAD_SELF
            | IF_SETPLAYERHEAD_SELF
            | CC_SETPLAYERMODEL_SELF
            | IF_SETPLAYERMODEL_SELF
            | CC_SETMODELANIM
            | IF_SETMODELANIM
            | CC_SETVIDEO_GRAPHIC
            | IF_SETVIDEO_GRAPHIC
            | CC_SETVIDEO_TEXT
            | IF_SETVIDEO_TEXT
            | CC_NPC_SETCUSTOMBODYMODEL
            | IF_NPC_SETCUSTOMBODYMODEL
            | CC_NPC_SETCUSTOMHEADMODEL
            | IF_NPC_SETCUSTOMHEADMODEL
            | CC_NPC_SETCUSTOMRECOL
            | IF_NPC_SETCUSTOMRECOL
            | CC_NPC_SETCUSTOMRETEX
            | IF_NPC_SETCUSTOMRETEX
            | IF_GETMODEL => {
                let operand = int()?;
                self.execute_component(opcode, operand, host)?
            }
            CC_GETPARENTLAYER
            | IF_GETPARENTLAYER
            | IF_GET_GAMESCREEN
            | IF_DEBUG_GETOPENIFCOUNT
            | IF_DEBUG_GETOPENIFID
            | IF_DEBUG_GETNAME
            | IF_DEBUG_GETCOMCOUNT
            | IF_DEBUG_GETCOMNAME
            | IF_DEBUG_GETSERVERTRIGGERS => {
                let operand = int()?;
                self.execute_query(opcode, operand, host)?
            }
            IF_CREATE | IF_DELETEALL | CC_LOADENTITYMODEL | IF_DEBUG_TARGET | IF_DEBUG_BUTTON1
            | IF_DEBUG_BUTTON2 | IF_DEBUG_BUTTON3 | IF_DEBUG_BUTTON4 | IF_DEBUG_BUTTON5
            | IF_DEBUG_BUTTON6 | IF_DEBUG_BUTTON7 | IF_DEBUG_BUTTON8 | IF_DEBUG_BUTTON9
            | IF_DEBUG_BUTTON10 => {
                let operand = int()?;
                self.execute_interface(opcode, operand, host)?
            }
            NOTIFY_ACCOUNTCREATESTARTED => {}
            INV_GETOBJ | INV_GETNUM | INV_TOTAL | INV_SIZE | INV_STOCKBASE | INVOTHER_GETOBJ
            | INVOTHER_GETNUM | INVOTHER_TOTAL | INV_FREESPACE | INV_TOTALPARAM
            | INV_TOTALPARAM_STACK | INV_GETTOOLTIP | INV_OTHER_GETTOOLTIP | OC_FIND
            | OC_FINDNEXT | OC_FINDRESTART | ITEM_FIND_PARAMINT | ITEM_FIND_PARAMSTR => {
                self.execute_inventory(opcode, host)?
            }
            OC_NAME | OC_OP | OC_IOP | OC_COST | OC_STACKABLE | OC_CERT | OC_UNCERT
            | OC_WEARPOS | OC_WEARPOS2 | OC_WEARPOS3 | OC_MEMBERS | OC_PARAM | OC_ICURSOR
            | OC_MULTISTACKSIZE | OC_MINIMENU_COLOUR_OVERRIDDEN | OC_MINIMENU_COLOUR | NC_PARAM
            | STRUCT_PARAM | SEQ_PARAM => self.execute_config(opcode, host)?,
            op if Self::is_outbound_opcode(op) => self.execute_outbound(op, host)?,
            op if Self::is_social_opcode(op) => self.execute_social(op, host)?,
            op if Self::is_clan_opcode(op) => self.execute_clan(op, host)?,
            op if Self::is_clan_var_opcode(op) => self.execute_clan_var(op, operand, host)?,
            op if Self::is_chat_phrase_opcode(op) => self.execute_chat_phrase(op, host)?,
            op if Self::is_chat_opcode(op) => self.execute_chat(op, host)?,
            op if Self::is_chat_send_opcode(op) => self.execute_chat_send(op, host)?,
            op if Self::is_message_opcode(op) => self.execute_message(op, host)?,
            op if Self::is_dialog_opcode(op) => self.execute_dialog(op, host)?,
            op if Self::is_bas_opcode(op) => self.execute_bas(op, host)?,
            op if Self::is_sub_interface_opcode(op) => self.execute_sub_interface(op, host)?,
            op if Self::is_player_op_opcode(op) => self.execute_player_op(op, host)?,
            op if Self::is_quick_chat_opcode(op) => self.execute_quick_chat(op, host)?,
            op if Self::is_player_opcode(op) => self.execute_player(op, operand, host)?,
            op if Self::is_entity_opcode(op) => self.execute_entity(op, host)?,
            op if Self::is_appearance_opcode(op) => {
                self.execute_appearance(op, matches!(operand, Operand::Int(1)), host)?
            }
            op if Self::is_scene_opcode(op) => self.execute_scene(op, host)?,
            op if Self::is_projection_opcode(op) => self.execute_projection(op, host)?,
            op if Self::is_bounds_opcode(op) => self.execute_bounds(op, host)?,
            op if Self::is_world_map_opcode(op) => self.execute_world_map(op, host)?,
            op if Self::is_mini_menu_opcode(op) => self.execute_mini_menu(op, host)?,
            op if Self::is_platform_opcode(op) => self.execute_platform(op, host)?,
            op if Self::is_quest_opcode(op) => self.execute_quest(op, host)?,
            op if Self::is_userdetail_opcode(op) => self.execute_userdetail(op, host)?,
            op if Self::is_world_map_effect_opcode(op) => {
                self.execute_world_map_effect(op, host)?
            }
            op if Self::is_login_effect_opcode(op) => self.execute_login_effect(op, host)?,
            op if Self::is_world_opcode(op) => self.execute_world(op, host)?,
            op if Self::is_client_opcode(op) => self.execute_client(op, host)?,
            Opcode::HSVTORGB
            | Opcode::RANDOM
            | Opcode::RANDOMINC
            | Opcode::RANDOM_SOUND_PITCH
            | Opcode::DATE_MINUTES
            | Opcode::DATE_RUNEDAY
            | Opcode::DATE_YEAR
            | Opcode::DATE_RUNEDAY_TODATE
            | Opcode::FROMDATE
            | Opcode::FORMAT_DATETIME_FROM_MINUTES => self.execute_clock(opcode, host)?,
            Opcode::STRINGWIDTH
            | Opcode::PARAWIDTH
            | Opcode::PARAHEIGHT
            | Opcode::PARAHEIGHT_EXTRA => self.execute_font(opcode, host)?,
            op if Self::is_pause_opcode(op) => self.execute_pause(op, matches!(operand, Operand::Int(1)), host)?,
            op if Self::is_media_query_opcode(op) => self.execute_media_query(op, matches!(operand, Operand::Int(1)), host)?,
            _ => self.execute_text(opcode, budget)?,
        }
        Ok(false)
    }
}

fn check_string(value: Option<&str>, limit: usize) -> Result<(), VmErrorKind> {
    if value.is_some_and(|value| value.len() > limit) {
        Err(VmErrorKind::StringLimit)
    } else {
        Ok(())
    }
}

fn push<T>(stack: &mut Vec<T>, value: T, kind: StackKind) -> Result<(), VmErrorKind> {
    if stack.len() >= STACK_CAPACITY {
        return Err(VmErrorKind::StackOverflow(kind));
    }
    stack.push(value);
    Ok(())
}

fn pop<T>(stack: &mut Vec<T>, kind: StackKind) -> Result<T, VmErrorKind> {
    stack.pop().ok_or(VmErrorKind::StackUnderflow(kind))
}

fn pair<T>(stack: &mut Vec<T>, kind: StackKind) -> Result<(T, T), VmErrorKind> {
    if stack.len() < 2 {
        return Err(VmErrorKind::StackUnderflow(kind));
    }
    let right = pop(stack, kind)?;
    let left = pop(stack, kind)?;
    Ok((left, right))
}

fn local<T>(values: &[T], index: i32, kind: StackKind) -> Result<&T, VmErrorKind> {
    usize::try_from(index)
        .ok()
        .and_then(|n| values.get(n))
        .ok_or(VmErrorKind::LocalIndex(kind, index))
}

fn local_mut<T>(values: &mut [T], index: i32, kind: StackKind) -> Result<&mut T, VmErrorKind> {
    usize::try_from(index)
        .ok()
        .and_then(|n| values.get_mut(n))
        .ok_or(VmErrorKind::LocalIndex(kind, index))
}

fn argument_start(length: usize, count: u16, kind: StackKind) -> Result<usize, VmErrorKind> {
    length
        .checked_sub(usize::from(count))
        .ok_or(VmErrorKind::StackUnderflow(kind))
}

fn paired_quicksort(keys: &mut [i32], values: &mut [i32], end: usize) {
    let mut ranges = vec![(0, end)];
    while let Some((start, end)) = ranges.pop() {
        if start >= end {
            continue;
        }
        let middle = (start + end) / 2;
        let pivot = keys[middle];
        keys.swap(middle, end);
        values.swap(middle, end);
        let mask = usize::from(pivot != i32::MAX);
        let mut partition = start;
        for index in start..end {
            if keys[index] < pivot + (index & mask) as i32 {
                keys.swap(index, partition);
                values.swap(index, partition);
                partition += 1;
            }
        }
        keys.swap(end, partition);
        values.swap(end, partition);
        if partition + 1 < end {
            ranges.push((partition + 1, end));
        }
        if partition > start + 1 {
            ranges.push((start, partition - 1));
        }
    }
}

fn array_slot(slot: i32) -> Result<usize, VmErrorKind> {
    usize::try_from(slot)
        .ok()
        .filter(|n| *n < 5)
        .ok_or(VmErrorKind::ArraySlot(slot))
}

fn array_index(index: i32, length: usize) -> Result<usize, VmErrorKind> {
    usize::try_from(index)
        .ok()
        .filter(|n| *n < length)
        .ok_or(VmErrorKind::ArrayIndex(index))
}

fn jump(current: &mut Activation<'_>, offset: i32) -> Result<(), VmErrorKind> {
    let target = i64::try_from(current.next)
        .ok()
        .and_then(|n| n.checked_add(i64::from(offset)))
        .and_then(|n| usize::try_from(n).ok())
        .filter(|n| *n < current.script.instructions().len())
        .ok_or(VmErrorKind::InstructionPointer)?;
    current.next = target;
    Ok(())
}

fn comparison<T: Ord>(opcode: Opcode, left: T, right: T) -> bool {
    use Opcode::*;
    match opcode {
        BRANCH_NOT | LONG_BRANCH_NOT => left != right,
        BRANCH_EQUALS | LONG_BRANCH_EQUALS => left == right,
        BRANCH_LESS_THAN | LONG_BRANCH_LESS_THAN => left < right,
        BRANCH_GREATER_THAN | LONG_BRANCH_GREATER_THAN => left > right,
        BRANCH_LESS_THAN_OR_EQUALS | LONG_BRANCH_LESS_THAN_OR_EQUALS => left <= right,
        BRANCH_GREATER_THAN_OR_EQUALS | LONG_BRANCH_GREATER_THAN_OR_EQUALS => left >= right,
        _ => false,
    }
}
