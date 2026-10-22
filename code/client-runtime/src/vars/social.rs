use super::WorldVarHost;
use openrs_cs2::{ChatState, ClanState, ClanVarDefs, HostError, SocialState};

#[derive(Default)]
pub(super) struct SocialBinding<'a> {
    social: Option<&'a mut SocialState>,
    clan: Option<&'a ClanState>,
    clan_var_defs: Option<&'a ClanVarDefs>,
    chat: Option<&'a mut ChatState>,
}

impl<'a> SocialBinding<'a> {
    pub(super) fn social_state(&self) -> Result<&SocialState, HostError> {
        self.social.as_deref().ok_or(HostError::Unavailable)
    }

    pub(super) fn social_state_mut(&mut self) -> Result<&mut SocialState, HostError> {
        self.social.as_deref_mut().ok_or(HostError::Unavailable)
    }

    pub(super) fn clan_state(&self) -> Result<&ClanState, HostError> {
        self.clan.ok_or(HostError::Unavailable)
    }

    pub(super) fn clan_var_defs(&self) -> Result<&ClanVarDefs, HostError> {
        self.clan_var_defs.ok_or(HostError::Unavailable)
    }

    pub(super) fn chat_state(&self) -> Result<&ChatState, HostError> {
        self.chat.as_deref().ok_or(HostError::Unavailable)
    }

    pub(super) fn chat_state_mut(&mut self) -> Result<&mut ChatState, HostError> {
        self.chat.as_deref_mut().ok_or(HostError::Unavailable)
    }
}

impl<'a> WorldVarHost<'a> {
    pub fn with_social_state(mut self, state: &'a mut SocialState) -> Self {
        self.social.social = Some(state);
        self
    }

    pub fn with_clan_state(mut self, state: &'a ClanState) -> Self {
        self.social.clan = Some(state);
        self
    }

    pub fn with_clan_var_defs(mut self, defs: &'a ClanVarDefs) -> Self {
        self.social.clan_var_defs = Some(defs);
        self
    }

    pub fn with_chat_state(mut self, state: &'a mut ChatState) -> Self {
        self.social.chat = Some(state);
        self
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::vars::WorldVariables;
    use openrs_cs2::{
        Arguments, ChatState, ClanSettings, ClanVarDefs, ClanVarValue, Friend, Opcode, Outbox,
        Script, Vm, VmErrorKind,
    };
    use std::collections::BTreeMap;

    fn script(code: &[(Opcode, i32)]) -> BTreeMap<i32, Script> {
        let mut bytes = vec![0];
        for (opcode, operand) in code {
            bytes.extend_from_slice(&opcode.wire().to_be_bytes());
            if opcode.metadata().has_int_constant {
                bytes.extend_from_slice(&operand.to_be_bytes());
            } else {
                bytes.push(*operand as u8);
            }
        }
        bytes.extend_from_slice(&(code.len() as i32).to_be_bytes());
        bytes.extend_from_slice(&[0; 12]);
        bytes.extend_from_slice(&[0, 0, 1]);
        BTreeMap::from([(0, Script::decode(&bytes).unwrap())])
    }

    fn run(code: &[(Opcode, i32)], host: &mut WorldVarHost<'_>) -> (Vm, Result<(), VmErrorKind>) {
        let mut vm = Vm::default();
        let result = vm
            .run_with_host(0, &script(code), Arguments::default(), host)
            .map(|_| ())
            .map_err(|error| error.kind);
        (vm, result)
    }

    fn unsupported(code: &[(Opcode, i32)], opcode: Opcode) {
        let mut variables = WorldVariables::new(1).unwrap();
        let (_, result) = run(code, &mut WorldVarHost::new(&mut variables, 0));
        assert_eq!(result, Err(VmErrorKind::UnsupportedOpcode(opcode)));
    }

    #[test]
    fn social_state_answers_friend_ops_and_unbound_is_unsupported() {
        let code = [(Opcode::FRIEND_COUNT, 0), (Opcode::RETURN, 0)];
        unsupported(&code, Opcode::FRIEND_COUNT);
        let mut variables = WorldVariables::new(1).unwrap();
        let mut social = SocialState {
            friends_loaded: 2,
            friends: vec![Friend::default(), Friend::default()],
            ..SocialState::default()
        };
        let mut host = WorldVarHost::new(&mut variables, 0).with_social_state(&mut social);
        let (vm, result) = run(&code, &mut host);
        assert_eq!(result, Ok(()));
        assert_eq!(vm.int_stack(), [2]);
    }

    #[test]
    fn friends_chat_display_name_reads_the_bound_social_state() {
        let code = [(Opcode::CLAN_GETCHATDISPLAYNAME, 0), (Opcode::RETURN, 0)];
        unsupported(&code, Opcode::CLAN_GETCHATDISPLAYNAME);
        let mut variables = WorldVariables::new(1).unwrap();
        let mut social = SocialState::default();
        let mut host = WorldVarHost::new(&mut variables, 0).with_social_state(&mut social);
        let (vm, result) = run(&code, &mut host);
        assert_eq!(result, Ok(()));
        assert_eq!(vm.string_stack().len(), 1);
    }

    #[test]
    fn clan_state_answers_the_clan_variable_ops() {
        let global = [(Opcode::PUSH_VARCLAN, 0), (Opcode::RETURN, 0)];
        unsupported(&global, Opcode::PUSH_VARCLAN);
        let clan = ClanState {
            clan_vars: Some(vec![Some(ClanVarValue::Int(9))]),
            settings: Some(ClanSettings {
                variables: Some(BTreeMap::from([(0, ClanVarValue::Int(42))])),
                ..ClanSettings::default()
            }),
            ..ClanState::default()
        };
        let defs = ClanVarDefs {
            vars: Vec::new(),
            settings: vec![Some(vec![1, 105, 0])],
        };
        let mut variables = WorldVariables::new(1).unwrap();
        let mut host = WorldVarHost::new(&mut variables, 0).with_clan_state(&clan);
        let (vm, result) = run(&global, &mut host);
        assert_eq!(result, Ok(()));
        assert_eq!(vm.int_stack(), [9]);
        let setting = [
            (Opcode::ACTIVECLANSETTINGS_FIND_AFFINED, 0),
            (Opcode::PUSH_VARCLANSETTING, 0),
            (Opcode::RETURN, 0),
        ];
        let (_, result) = run(&setting, &mut host);
        assert_eq!(
            result,
            Err(VmErrorKind::UnsupportedOpcode(Opcode::PUSH_VARCLANSETTING))
        );
        let mut host = WorldVarHost::new(&mut variables, 0)
            .with_clan_state(&clan)
            .with_clan_var_defs(&defs);
        let (vm, result) = run(&setting, &mut host);
        assert_eq!(result, Ok(()));
        assert_eq!(vm.int_stack(), [1, 42]);
    }

    #[test]
    fn chat_state_answers_filters_history_and_set_filter_updates_it() {
        let read = [(Opcode::CHAT_GETFILTER_PUBLIC, 0), (Opcode::RETURN, 0)];
        unsupported(&read, Opcode::CHAT_GETFILTER_PUBLIC);
        let mut variables = WorldVariables::new(1).unwrap();
        let mut chat = ChatState {
            public_filter: 2,
            line_count: 5,
            ..ChatState::default()
        };
        let mut outbox = Outbox::default();
        let mut host = WorldVarHost::new(&mut variables, 0)
            .with_chat_state(&mut chat)
            .with_outbox(&mut outbox);
        let (vm, result) = run(&read, &mut host);
        assert_eq!(result, Ok(()));
        assert_eq!(vm.int_stack(), [2]);
        let (vm, result) = run(
            &[(Opcode::CHAT_GETHISTORYLENGTH, 0), (Opcode::RETURN, 0)],
            &mut host,
        );
        assert_eq!(result, Ok(()));
        assert_eq!(vm.int_stack(), [5]);
        let (_, result) = run(
            &[
                (Opcode::PUSH_CONSTANT_INT, 1),
                (Opcode::PUSH_CONSTANT_INT, 2),
                (Opcode::PUSH_CONSTANT_INT, 0),
                (Opcode::CHAT_SETFILTER, 0),
                (Opcode::RETURN, 0),
            ],
            &mut host,
        );
        assert_eq!(result, Ok(()));
        assert_eq!(outbox.packets.len(), 1);
        assert_eq!(chat.public_filter, 1);
        assert_eq!(chat.private_filter, Some(2));
        assert_eq!(chat.trade_filter, 0);
    }

    #[test]
    fn dialog_ops_write_to_the_bound_outbox_only() {
        let code = [
            (Opcode::PUSH_CONSTANT_INT, 77),
            (Opcode::RESUME_HSLDIALOG, 0),
            (Opcode::RETURN, 0),
        ];
        unsupported(&code, Opcode::RESUME_HSLDIALOG);
        let mut variables = WorldVariables::new(1).unwrap();
        let mut outbox = Outbox::default();
        let mut host = WorldVarHost::new(&mut variables, 0).with_outbox(&mut outbox);
        let (_, result) = run(&code, &mut host);
        assert_eq!(result, Ok(()));
        assert_eq!(outbox.packets.len(), 1);
    }
}
