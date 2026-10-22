use super::{pair, pop, Vm};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};

impl Vm {
    pub(super) fn is_sub_interface_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(opcode, IF_OPENSUBCLIENT | IF_CLOSESUBCLIENT)
    }

    pub(super) fn execute_sub_interface(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        let unsupported = || VmErrorKind::UnsupportedOpcode(opcode);
        let host = host.as_deref_mut().ok_or_else(unsupported)?;
        let model = host.components().map_err(|error| match error {
            HostError::Unavailable => unsupported(),
            error => VmErrorKind::Host(error),
        })?;
        let outcome = if opcode == Opcode::IF_OPENSUBCLIENT {
            let (parent, interface) = pair(&mut self.ints, StackKind::Int)?;
            model.open_sub_client(parent, interface)
        } else {
            let parent = pop(&mut self.ints, StackKind::Int)?;
            model.close_sub_client(parent)
        };
        outcome.map_err(|()| VmErrorKind::Host(HostError::NoEntity))
    }
}
