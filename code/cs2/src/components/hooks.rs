use std::sync::Arc;

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum HookValue {
    Int(i32),
    Long(i64),
    Text(Option<Arc<str>>),
}

pub type HookScript = Vec<HookValue>;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum HookSlot {
    OnClick,
    OnClickRepeat,
    OnRelease,
    OnHold,
    OnMouseOver,
    PopupScript,
    OnMouseLeaveScript,
    OnDrag,
    OnDragComplete,
    OnTargetEnter,
    HookParams,
    OnVarpTransmit,
    MouseLeaveScript,
    OnStatTransmit,
    OnKey,
    OnScrollWheel,
    OnTimer,
    Params,
    OnTargetLeave,
    OnMouseMove,
    OnChatTransmit,
    OnKeyType,
    OnFriendTransmit,
    OnFriendsChatTransmit,
    OnClanSettingsTransmit,
    OnMiscTransmit,
    OnStockTransmit,
    OnSubClose,
    OnSubChange,
    OnCamFinished,
    OnResize,
    OnClanVarTransmit,
    OnClanChannelTransmit,
}

impl HookSlot {
    pub const COUNT: usize = 33;

    pub const ALL: [HookSlot; Self::COUNT] = [
        HookSlot::OnClick,
        HookSlot::OnClickRepeat,
        HookSlot::OnRelease,
        HookSlot::OnHold,
        HookSlot::OnMouseOver,
        HookSlot::PopupScript,
        HookSlot::OnMouseLeaveScript,
        HookSlot::OnDrag,
        HookSlot::OnDragComplete,
        HookSlot::OnTargetEnter,
        HookSlot::HookParams,
        HookSlot::OnVarpTransmit,
        HookSlot::MouseLeaveScript,
        HookSlot::OnStatTransmit,
        HookSlot::OnKey,
        HookSlot::OnScrollWheel,
        HookSlot::OnTimer,
        HookSlot::Params,
        HookSlot::OnTargetLeave,
        HookSlot::OnMouseMove,
        HookSlot::OnChatTransmit,
        HookSlot::OnKeyType,
        HookSlot::OnFriendTransmit,
        HookSlot::OnFriendsChatTransmit,
        HookSlot::OnClanSettingsTransmit,
        HookSlot::OnMiscTransmit,
        HookSlot::OnStockTransmit,
        HookSlot::OnSubClose,
        HookSlot::OnSubChange,
        HookSlot::OnCamFinished,
        HookSlot::OnResize,
        HookSlot::OnClanVarTransmit,
        HookSlot::OnClanChannelTransmit,
    ];

    fn survives_reset(self) -> bool {
        matches!(
            self,
            HookSlot::OnClanVarTransmit | HookSlot::OnClanChannelTransmit
        )
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum TriggerSlot {
    Varps,
    MouseLeave,
    StatTransmit,
    KeyPress,
    MouseWheel,
}

impl TriggerSlot {
    pub const COUNT: usize = 5;

    pub const ALL: [TriggerSlot; Self::COUNT] = [
        TriggerSlot::Varps,
        TriggerSlot::MouseLeave,
        TriggerSlot::StatTransmit,
        TriggerSlot::KeyPress,
        TriggerSlot::MouseWheel,
    ];
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ComponentHooks {
    scripts: [Option<HookScript>; HookSlot::COUNT],
    triggers: [Option<Vec<i32>>; TriggerSlot::COUNT],
    pub has_script: bool,
}

impl Default for ComponentHooks {
    fn default() -> Self {
        Self {
            scripts: std::array::from_fn(|_| None),
            triggers: std::array::from_fn(|_| None),
            has_script: false,
        }
    }
}

impl ComponentHooks {
    pub fn script(&self, slot: HookSlot) -> Option<&HookScript> {
        self.scripts[slot as usize].as_ref()
    }

    pub fn triggers(&self, slot: TriggerSlot) -> Option<&Vec<i32>> {
        self.triggers[slot as usize].as_ref()
    }

    pub fn set(
        &mut self,
        slot: HookSlot,
        script: Option<HookScript>,
        triggers: Option<(TriggerSlot, Option<Vec<i32>>)>,
    ) {
        self.scripts[slot as usize] = script;
        if let Some((trigger, values)) = triggers {
            self.triggers[trigger as usize] = values;
        }
        self.has_script = true;
    }

    pub fn clear(&mut self) {
        for slot in HookSlot::ALL {
            if !slot.survives_reset() {
                self.scripts[slot as usize] = None;
            }
        }
        self.triggers = std::array::from_fn(|_| None);
    }
}
