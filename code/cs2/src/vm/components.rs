use super::{check_string, push, Vm};
use crate::components::{IntGetter, MediaLookup, MediaOp, Setter};
use crate::{
    ComponentError, ComponentFailure, ComponentId, ComponentModel, HookSlot, HookValue, HostError,
    Opcode, StackKind, TriggerSlot, VarHost, VmErrorKind,
};
use std::sync::Arc;

#[derive(Clone, Copy)]
enum Action {
    Find,
    Create,
    Delete,
    DeleteAll,
    SendToOrder(bool),
    HasSub(bool),
    Get(IntGetter),
    Media(MediaOp),
    GetText,
    GetNextSubId,
    SetDepthChild,
    DragPickup,
    SetPosition,
    SetSize,
    SetHide,
    SetColour,
    SetText,
    SetGraphic,
    SetTrans,
    SetScrollSize,
    SetAspect,
    Set(Setter, usize, usize),
    Option(OptionOp),
    Hook(HookOp),
}

#[derive(Clone, Copy)]
enum HookOp {
    Set(HookSlot, Option<TriggerSlot>),
    Clear,
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum OptionOp {
    SetOp,
    SetOpBase,
    SetOpCursor,
    SetTargetOpCursor,
    SetOpChar,
    SetOpTChar,
    SetOpKey,
    SetOpTKey,
    SetParamInt,
    SetParamString,
    ClearOps,
    SetTargetCursors,
    SetMouseOverCursor,
    GetOp,
    GetOpBase,
    GetTargetMask,
    Param,
}

struct Plan {
    action: Action,
    by_hash: bool,
}

fn plan(opcode: Opcode) -> Plan {
    use Opcode::*;
    let (action, by_hash) = match opcode {
        CC_GETINVOBJECT => (Action::Get(IntGetter::InvObject), false),
        IF_GETINVOBJECT => (Action::Get(IntGetter::InvObject), true),
        CC_GETINVCOUNT => (Action::Get(IntGetter::InvCount), false),
        IF_GETINVCOUNT => (Action::Get(IntGetter::InvCount), true),
        CC_GETFONTMETRICS | CC_GETFONTGRAPHIC => (Action::Get(IntGetter::FontId), false),
        IF_GETFONTMETRICS | IF_GETFONTGRAPHIC => (Action::Get(IntGetter::FontId), true),
        CC_CALLONRESIZE => (Action::Media(MediaOp::CallOnResize), false),
        IF_CALLONRESIZE => (Action::Media(MediaOp::CallOnResize), true),
        CC_SETOBJECT => (Action::Media(MediaOp::Item { wear_col: false, mode: 2 }), false),
        IF_SETOBJECT => (Action::Media(MediaOp::Item { wear_col: false, mode: 2 }), true),
        CC_SETOBJECT_NONUM => (Action::Media(MediaOp::Item { wear_col: false, mode: 0 }), false),
        IF_SETOBJECT_NONUM => (Action::Media(MediaOp::Item { wear_col: false, mode: 0 }), true),
        CC_SETOBJECT_ALWAYSNUM => (Action::Media(MediaOp::Item { wear_col: false, mode: 1 }), false),
        IF_SETOBJECT_ALWAYSNUM => (Action::Media(MediaOp::Item { wear_col: false, mode: 1 }), true),
        CC_SETOBJECT_WEARCOL => (Action::Media(MediaOp::Item { wear_col: true, mode: 2 }), false),
        IF_SETOBJECT_WEARCOL => (Action::Media(MediaOp::Item { wear_col: true, mode: 2 }), true),
        CC_SETOBJECT_WEARCOL_NONUM => (Action::Media(MediaOp::Item { wear_col: true, mode: 0 }), false),
        IF_SETOBJECT_WEARCOL_NONUM => (Action::Media(MediaOp::Item { wear_col: true, mode: 0 }), true),
        CC_SETOBJECT_WEARCOL_ALWAYSNUM => (Action::Media(MediaOp::Item { wear_col: true, mode: 1 }), false),
        IF_SETOBJECT_WEARCOL_ALWAYSNUM => (Action::Media(MediaOp::Item { wear_col: true, mode: 1 }), true),
        CC_SETNPCHEAD => (Action::Media(MediaOp::NpcHead), false),
        IF_SETNPCHEAD => (Action::Media(MediaOp::NpcHead), true),
        CC_SETNPCMODEL => (Action::Media(MediaOp::NpcModel), false),
        IF_SETNPCMODEL => (Action::Media(MediaOp::NpcModel), true),
        CC_SETPLAYERMODEL => (Action::Media(MediaOp::PlayerModel), false),
        IF_SETPLAYERMODEL => (Action::Media(MediaOp::PlayerModel), true),
        CC_SETPLAYERHEAD_SELF => (Action::Media(MediaOp::PlayerHeadSelf), false),
        IF_SETPLAYERHEAD_SELF => (Action::Media(MediaOp::PlayerHeadSelf), true),
        CC_SETPLAYERMODEL_SELF => (Action::Media(MediaOp::PlayerModelSelf), false),
        IF_SETPLAYERMODEL_SELF => (Action::Media(MediaOp::PlayerModelSelf), true),
        CC_SETMODELANIM => (Action::Media(MediaOp::ModelAnim), false),
        IF_SETMODELANIM => (Action::Media(MediaOp::ModelAnim), true),
        CC_SETVIDEO_GRAPHIC => (Action::Media(MediaOp::VideoGraphic), false),
        IF_SETVIDEO_GRAPHIC => (Action::Media(MediaOp::VideoGraphic), true),
        CC_SETVIDEO_TEXT => (Action::Media(MediaOp::VideoText), false),
        IF_SETVIDEO_TEXT => (Action::Media(MediaOp::VideoText), true),
        CC_NPC_SETCUSTOMBODYMODEL => (Action::Media(MediaOp::NpcCustomBody), false),
        IF_NPC_SETCUSTOMBODYMODEL => (Action::Media(MediaOp::NpcCustomBody), true),
        CC_NPC_SETCUSTOMHEADMODEL => (Action::Media(MediaOp::NpcCustomHead), false),
        IF_NPC_SETCUSTOMHEADMODEL => (Action::Media(MediaOp::NpcCustomHead), true),
        CC_NPC_SETCUSTOMRECOL => (Action::Media(MediaOp::NpcCustomRecol), false),
        IF_NPC_SETCUSTOMRECOL => (Action::Media(MediaOp::NpcCustomRecol), true),
        CC_NPC_SETCUSTOMRETEX => (Action::Media(MediaOp::NpcCustomRetex), false),
        IF_NPC_SETCUSTOMRETEX => (Action::Media(MediaOp::NpcCustomRetex), true),
        CC_GETLAYER => (Action::Get(IntGetter::Layer), false),
        IF_GETLAYER => (Action::Get(IntGetter::Layer), true),
        CC_GET2DANGLE => (Action::Get(IntGetter::Angle2d), false),
        IF_GET2DANGLE => (Action::Get(IntGetter::Angle2d), true),
        CC_GETID => (Action::Get(IntGetter::SlotId), false),
        IF_GETNEXTSUBID => (Action::GetNextSubId, true),
        CC_SETDRAGDEADTIME => (Action::Set(Setter::DragDeadTime, 1, 0), false),
        IF_SETDRAGDEADTIME => (Action::Set(Setter::DragDeadTime, 1, 0), true),
        CC_SETDRAGRENDERBEHAVIOUR => (Action::Set(Setter::DragRenderBehaviour, 1, 0), false),
        IF_SETDRAGRENDERBEHAVIOUR => (Action::Set(Setter::DragRenderBehaviour, 1, 0), true),
        CC_DRAGPICKUP => (Action::DragPickup, false),
        IF_DRAGPICKUP => (Action::DragPickup, true),
        CC_SETDRAGGABLE => (Action::SetDepthChild, false),
        IF_SETDRAGGABLE => (Action::SetDepthChild, true),
        CC_SETRECOL => (Action::Set(Setter::Recolor, 3, 0), false),
        IF_SETRECOL => (Action::Set(Setter::Recolor, 3, 0), true),
        CC_SETRETEX => (Action::Set(Setter::Retexture, 3, 0), false),
        IF_SETRETEX => (Action::Set(Setter::Retexture, 3, 0), true),
        CC_FIND => (Action::Find, false),
        IF_FIND => (Action::Find, true),
        CC_CREATE => (Action::Create, false),
        CC_DELETE => (Action::Delete, false),
        CC_DELETEALL => (Action::DeleteAll, false),
        IF_HASSUB => (Action::HasSub(false), true),
        IF_HASSUBMODAL | IF_HASSUBOVERLAY => (Action::HasSub(true), true),
        CC_SENDTOFRONT => (Action::SendToOrder(true), false),
        CC_SENDTOBACK => (Action::SendToOrder(false), false),
        IF_SENDTOFRONT => (Action::SendToOrder(true), true),
        IF_SENDTOBACK => (Action::SendToOrder(false), true),
        CC_GETX => (Action::Get(IntGetter::X), false),
        IF_GETX => (Action::Get(IntGetter::X), true),
        CC_GETY => (Action::Get(IntGetter::Y), false),
        IF_GETY => (Action::Get(IntGetter::Y), true),
        CC_GETWIDTH => (Action::Get(IntGetter::Width), false),
        IF_GETWIDTH => (Action::Get(IntGetter::Width), true),
        CC_GETHEIGHT => (Action::Get(IntGetter::Height), false),
        IF_GETHEIGHT => (Action::Get(IntGetter::Height), true),
        CC_GETHIDE => (Action::Get(IntGetter::Hide), false),
        IF_GETHIDE => (Action::Get(IntGetter::Hide), true),
        CC_GETCOLOUR => (Action::Get(IntGetter::Colour), false),
        IF_GETCOLOUR => (Action::Get(IntGetter::Colour), true),
        CC_GETGRAPHIC => (Action::Get(IntGetter::Graphic), false),
        IF_GETGRAPHIC => (Action::Get(IntGetter::Graphic), true),
        CC_GETTRANS => (Action::Get(IntGetter::Trans), false),
        IF_GETTRANS => (Action::Get(IntGetter::Trans), true),
        CC_GETSCROLLX => (Action::Get(IntGetter::ScrollX), false),
        IF_GETSCROLLX => (Action::Get(IntGetter::ScrollX), true),
        CC_GETSCROLLY => (Action::Get(IntGetter::ScrollY), false),
        IF_GETSCROLLY => (Action::Get(IntGetter::ScrollY), true),
        CC_GETSCROLLWIDTH => (Action::Get(IntGetter::ScrollWidth), false),
        IF_GETSCROLLWIDTH => (Action::Get(IntGetter::ScrollWidth), true),
        CC_GETSCROLLHEIGHT => (Action::Get(IntGetter::ScrollHeight), false),
        IF_GETSCROLLHEIGHT => (Action::Get(IntGetter::ScrollHeight), true),
        CC_GETTEXT => (Action::GetText, false),
        IF_GETTEXT => (Action::GetText, true),
        CC_SETPOSITION => (Action::SetPosition, false),
        IF_SETPOSITION => (Action::SetPosition, true),
        CC_SETSIZE => (Action::SetSize, false),
        IF_SETSIZE => (Action::SetSize, true),
        CC_SETHIDE => (Action::SetHide, false),
        IF_SETHIDE => (Action::SetHide, true),
        CC_SETCOLOUR => (Action::SetColour, false),
        IF_SETCOLOUR => (Action::SetColour, true),
        CC_SETTEXT => (Action::SetText, false),
        IF_SETTEXT => (Action::SetText, true),
        CC_SETGRAPHIC => (Action::SetGraphic, false),
        IF_SETGRAPHIC => (Action::SetGraphic, true),
        CC_SETTRANS => (Action::SetTrans, false),
        IF_SETTRANS => (Action::SetTrans, true),
        CC_SETFILL => (Action::Set(Setter::Fill, 1, 0), false),
        IF_SETFILL => (Action::Set(Setter::Fill, 1, 0), true),
        CC_SETLINEWID => (Action::Set(Setter::LineWidth, 1, 0), false),
        IF_SETLINEWID => (Action::Set(Setter::LineWidth, 1, 0), true),
        CC_SETALPHA => (Action::Set(Setter::Alpha, 1, 0), false),
        IF_SETALPHA => (Action::Set(Setter::Alpha, 1, 0), true),
        CC_SETTEXTSHADOW => (Action::Set(Setter::TextShadow, 1, 0), false),
        IF_SETTEXTSHADOW => (Action::Set(Setter::TextShadow, 1, 0), true),
        CC_SETOUTLINE => (Action::Set(Setter::Outline, 1, 0), false),
        IF_SETOUTLINE => (Action::Set(Setter::Outline, 1, 0), true),
        CC_SETGRAPHICSHADOW => (Action::Set(Setter::GraphicShadow, 1, 0), false),
        IF_SETGRAPHICSHADOW => (Action::Set(Setter::GraphicShadow, 1, 0), true),
        CC_SETVFLIP => (Action::Set(Setter::VerticalFlip, 1, 0), false),
        IF_SETVFLIP => (Action::Set(Setter::VerticalFlip, 1, 0), true),
        CC_SETHFLIP => (Action::Set(Setter::HorizontalFlip, 1, 0), false),
        IF_SETHFLIP => (Action::Set(Setter::HorizontalFlip, 1, 0), true),
        CC_SET2DANGLE => (Action::Set(Setter::Angle2d, 1, 0), false),
        IF_SET2DANGLE => (Action::Set(Setter::Angle2d, 1, 0), true),
        CC_SETTILING => (Action::Set(Setter::Tiling, 1, 0), false),
        IF_SETTILING => (Action::Set(Setter::Tiling, 1, 0), true),
        CC_SETLINEDIRECTION => (Action::Set(Setter::LineDirection, 1, 0), false),
        IF_SETLINEDIRECTION => (Action::Set(Setter::LineDirection, 1, 0), true),
        CC_SETMAXLINES => (Action::Set(Setter::MaxLines, 1, 0), false),
        IF_SETMAXLINES => (Action::Set(Setter::MaxLines, 1, 0), true),
        CC_SETNOCLICKTHROUGH => (Action::Set(Setter::NoClickThrough, 1, 0), false),
        IF_SETNOCLICKTHROUGH => (Action::Set(Setter::NoClickThrough, 1, 0), true),
        CC_SETSCROLLPOS => (Action::Set(Setter::ScrollPosition, 2, 0), false),
        IF_SETSCROLLPOS => (Action::Set(Setter::ScrollPosition, 2, 0), true),
        CC_SETMODELZOOM => (Action::Set(Setter::ModelZoom, 1, 0), false),
        IF_SETMODELZOOM => (Action::Set(Setter::ModelZoom, 1, 0), true),
        CC_SETTEXTFONT => (Action::Set(Setter::TextFont, 1, 0), false),
        IF_SETTEXTFONT => (Action::Set(Setter::TextFont, 1, 0), true),
        CC_SETFONTMONO => (Action::Set(Setter::FontMono, 1, 0), false),
        IF_SETFONTMONO => (Action::Set(Setter::FontMono, 1, 0), true),
        CC_SETMODELORTHOG => (Action::Set(Setter::ModelOrthogonal, 1, 0), false),
        IF_SETMODELORTHOG => (Action::Set(Setter::ModelOrthogonal, 1, 0), true),
        CC_SETMODELORIGIN => (Action::Set(Setter::ModelOrigin, 2, 0), false),
        IF_SETMODELORIGIN => (Action::Set(Setter::ModelOrigin, 2, 0), true),
        CC_SETTEXTALIGN => (Action::Set(Setter::TextAlign, 3, 0), false),
        IF_SETTEXTALIGN => (Action::Set(Setter::TextAlign, 3, 0), true),
        CC_SETTARGETVERB => (Action::Set(Setter::TargetVerb, 0, 1), false),
        IF_SETTARGETVERB => (Action::Set(Setter::TargetVerb, 0, 1), true),
        CC_SETPAUSETEXT => (Action::Set(Setter::PauseText, 0, 1), false),
        IF_SETPAUSETEXT => (Action::Set(Setter::PauseText, 0, 1), true),
        CC_SETCLICKMASK => (Action::Set(Setter::ClickMask, 1, 0), false),
        IF_SETCLICKMASK => (Action::Set(Setter::ClickMask, 1, 0), true),
        CC_SETTEXTANTIMACRO => (Action::Set(Setter::TextAntiMacro, 1, 0), false),
        IF_SETTEXTANTIMACRO => (Action::Set(Setter::TextAntiMacro, 1, 0), true),
        CC_SETDRAGDEADZONE => (Action::Set(Setter::DragDeadzone, 1, 0), false),
        IF_SETDRAGDEADZONE => (Action::Set(Setter::DragDeadzone, 1, 0), true),
        CC_SETMODELANGLE => (Action::Set(Setter::ModelAngle, 6, 0), false),
        IF_SETMODELANGLE => (Action::Set(Setter::ModelAngle, 6, 0), true),
        CC_SETMODELLIGHTING => (Action::Set(Setter::ModelLighting, 4, 0), false),
        IF_SETMODELLIGHTING => (Action::Set(Setter::ModelLighting, 4, 0), true),
        CC_SETMODELLIGHTING_SUNROTATION => (Action::Set(Setter::ModelSunRotation, 2, 0), false),
        IF_SETMODELLIGHTING_SUNROTATION => (Action::Set(Setter::ModelSunRotation, 2, 0), true),
        CC_SETMODELTINT => (Action::Set(Setter::ModelTint, 4, 0), false),
        IF_SETMODELTINT => (Action::Set(Setter::ModelTint, 4, 0), true),
        CC_SETMODEL => (Action::Set(Setter::Model, 1, 0), false),
        IF_SETMODEL => (Action::Set(Setter::Model, 1, 0), true),
        CC_SETMODEL_ITEMCONTAINER => (Action::Set(Setter::ModelItemContainer, 4, 0), false),
        IF_SETMODEL_ITEMCONTAINER => (Action::Set(Setter::ModelItemContainer, 4, 0), true),
        CC_SETSCROLLSIZE => (Action::SetScrollSize, false),
        IF_SETSCROLLSIZE => (Action::SetScrollSize, true),
        CC_SETASPECT => (Action::SetAspect, false),
        IF_SETASPECT => (Action::SetAspect, true),
        CC_GETMODELZOOM => (Action::Get(IntGetter::ModelZoom), false),
        IF_GETMODELZOOM => (Action::Get(IntGetter::ModelZoom), true),
        CC_GETMODELANGLE_X => (Action::Get(IntGetter::ModelAngleX), false),
        IF_GETMODELANGLE_X => (Action::Get(IntGetter::ModelAngleX), true),
        CC_GETMODELANGLE_Y => (Action::Get(IntGetter::ModelAngleY), false),
        IF_GETMODELANGLE_Y => (Action::Get(IntGetter::ModelAngleY), true),
        CC_GETMODELANGLE_Z => (Action::Get(IntGetter::ModelAngleZ), false),
        IF_GETMODELANGLE_Z => (Action::Get(IntGetter::ModelAngleZ), true),
        CC_GETMODELXOF => (Action::Get(IntGetter::ModelOffsetX), false),
        IF_GETMODELXOF => (Action::Get(IntGetter::ModelOffsetX), true),
        CC_GETMODELYOF => (Action::Get(IntGetter::ModelOffsetY), false),
        IF_GETMODELYOF => (Action::Get(IntGetter::ModelOffsetY), true),
        CC_GETMODEL => (Action::Get(IntGetter::Model), false),
        IF_GETMODEL => (Action::Get(IntGetter::Model), true),
        CC_SETOP => (Action::Option(OptionOp::SetOp), false),
        IF_SETOP => (Action::Option(OptionOp::SetOp), true),
        CC_SETOPBASE => (Action::Option(OptionOp::SetOpBase), false),
        IF_SETOPBASE => (Action::Option(OptionOp::SetOpBase), true),
        CC_SETOPCURSOR => (Action::Option(OptionOp::SetOpCursor), false),
        IF_SETOPCURSOR => (Action::Option(OptionOp::SetOpCursor), true),
        CC_SETTARGETOPCURSOR => (Action::Option(OptionOp::SetTargetOpCursor), false),
        IF_SETTARGETOPCURSOR => (Action::Option(OptionOp::SetTargetOpCursor), true),
        CC_SETOPCHAR => (Action::Option(OptionOp::SetOpChar), false),
        IF_SETOPCHAR => (Action::Option(OptionOp::SetOpChar), true),
        CC_SETOPTCHAR => (Action::Option(OptionOp::SetOpTChar), false),
        IF_SETOPTCHAR => (Action::Option(OptionOp::SetOpTChar), true),
        CC_SETOPKEY => (Action::Option(OptionOp::SetOpKey), false),
        IF_SETOPKEY => (Action::Option(OptionOp::SetOpKey), true),
        CC_SETOPTKEY => (Action::Option(OptionOp::SetOpTKey), false),
        IF_SETOPTKEY => (Action::Option(OptionOp::SetOpTKey), true),
        CC_SETPARAM_INT => (Action::Option(OptionOp::SetParamInt), false),
        IF_SETPARAM_INT => (Action::Option(OptionOp::SetParamInt), true),
        CC_SETPARAM_STRING => (Action::Option(OptionOp::SetParamString), false),
        IF_SETPARAM_STRING => (Action::Option(OptionOp::SetParamString), true),
        CC_CLEAROPS => (Action::Option(OptionOp::ClearOps), false),
        IF_CLEAROPS => (Action::Option(OptionOp::ClearOps), true),
        CC_SETTARGETCURSORS => (Action::Option(OptionOp::SetTargetCursors), false),
        IF_SETTARGETCURSORS => (Action::Option(OptionOp::SetTargetCursors), true),
        CC_SETMOUSEOVERCURSOR => (Action::Option(OptionOp::SetMouseOverCursor), false),
        IF_SETMOUSEOVERCURSOR => (Action::Option(OptionOp::SetMouseOverCursor), true),
        CC_GETOP => (Action::Option(OptionOp::GetOp), false),
        IF_GETOP => (Action::Option(OptionOp::GetOp), true),
        CC_GETOPBASE => (Action::Option(OptionOp::GetOpBase), false),
        IF_GETOPBASE => (Action::Option(OptionOp::GetOpBase), true),
        CC_GETTARGETMASK => (Action::Option(OptionOp::GetTargetMask), false),
        IF_GETTARGETMASK => (Action::Option(OptionOp::GetTargetMask), true),
        CC_SETPARAM => (Action::Option(OptionOp::SetParamInt), false),
        CC_PARAM => (Action::Option(OptionOp::Param), false),
        CC_SETONCLICK => (Action::Hook(HookOp::Set(HookSlot::OnClick, None)), false),
        CC_SETONHOLD => (Action::Hook(HookOp::Set(HookSlot::OnHold, None)), false),
        CC_SETONRELEASE => (Action::Hook(HookOp::Set(HookSlot::OnRelease, None)), false),
        CC_SETONMOUSEOVER => (
            Action::Hook(HookOp::Set(HookSlot::OnMouseOver, None)),
            false,
        ),
        CC_SETONMOUSELEAVE => (
            Action::Hook(HookOp::Set(HookSlot::OnMouseLeaveScript, None)),
            false,
        ),
        CC_SETONDRAG => (Action::Hook(HookOp::Set(HookSlot::OnDrag, None)), false),
        CC_SETONTARGETLEAVE => (Action::Hook(HookOp::Set(HookSlot::HookParams, None)), false),
        CC_SETONVARTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::OnVarpTransmit,
                Some(TriggerSlot::Varps),
            )),
            false,
        ),
        CC_SETONTIMER => (Action::Hook(HookOp::Set(HookSlot::OnTimer, None)), false),
        CC_SETONOP => (Action::Hook(HookOp::Set(HookSlot::Params, None)), false),
        CC_SETONDRAGCOMPLETE => (
            Action::Hook(HookOp::Set(HookSlot::OnDragComplete, None)),
            false,
        ),
        CC_SETONCLICKREPEAT => (
            Action::Hook(HookOp::Set(HookSlot::OnClickRepeat, None)),
            false,
        ),
        CC_SETONMOUSEREPEAT => (
            Action::Hook(HookOp::Set(HookSlot::PopupScript, None)),
            false,
        ),
        CC_SETONINVTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::MouseLeaveScript,
                Some(TriggerSlot::MouseLeave),
            )),
            false,
        ),
        CC_SETONSTATTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::OnStatTransmit,
                Some(TriggerSlot::StatTransmit),
            )),
            false,
        ),
        CC_SETONTARGETENTER => (
            Action::Hook(HookOp::Set(HookSlot::OnTargetEnter, None)),
            false,
        ),
        CC_SETONSCROLLWHEEL => (
            Action::Hook(HookOp::Set(HookSlot::OnMouseMove, None)),
            false,
        ),
        CC_SETONCHATTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnChatTransmit, None)),
            false,
        ),
        CC_SETONKEY => (Action::Hook(HookOp::Set(HookSlot::OnKeyType, None)), false),
        CC_SETONFRIENDTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnFriendTransmit, None)),
            false,
        ),
        CC_SETONCLANCHANNELTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnFriendsChatTransmit, None)),
            false,
        ),
        CC_SETONMISCTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnMiscTransmit, None)),
            false,
        ),
        CC_SETONDIALOGABORT => (Action::Hook(HookOp::Set(HookSlot::OnSubClose, None)), false),
        CC_SETONSUBCHANGE => (
            Action::Hook(HookOp::Set(HookSlot::OnSubChange, None)),
            false,
        ),
        CC_SETONSTOCKTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnStockTransmit, None)),
            false,
        ),
        CC_SETONCAMFINISHED => (
            Action::Hook(HookOp::Set(HookSlot::OnCamFinished, None)),
            false,
        ),
        CC_SETONRESIZE => (Action::Hook(HookOp::Set(HookSlot::OnResize, None)), false),
        CC_SETONVARCTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnKey, Some(TriggerSlot::KeyPress))),
            false,
        ),
        CC_SETONVARCSTRTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::OnScrollWheel,
                Some(TriggerSlot::MouseWheel),
            )),
            false,
        ),
        CC_SETONOPT => (
            Action::Hook(HookOp::Set(HookSlot::OnTargetLeave, None)),
            false,
        ),
        CC_SETONCLANSETTINGSTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnClanSettingsTransmit, None)),
            false,
        ),
        CC_SETLINKACTIVECLANCHANNEL => (
            Action::Hook(HookOp::Set(HookSlot::OnClanChannelTransmit, None)),
            false,
        ),
        CC_SETONVARCLANTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnClanVarTransmit, None)),
            false,
        ),
        CC_CLEARSCRIPTHOOKS => (Action::Hook(HookOp::Clear), false),
        IF_SETONCLICK => (Action::Hook(HookOp::Set(HookSlot::OnClick, None)), true),
        IF_SETONHOLD => (Action::Hook(HookOp::Set(HookSlot::OnHold, None)), true),
        IF_SETONRELEASE => (Action::Hook(HookOp::Set(HookSlot::OnRelease, None)), true),
        IF_SETONMOUSEOVER => (Action::Hook(HookOp::Set(HookSlot::OnMouseOver, None)), true),
        IF_SETONMOUSELEAVE => (
            Action::Hook(HookOp::Set(HookSlot::OnMouseLeaveScript, None)),
            true,
        ),
        IF_SETONDRAG => (Action::Hook(HookOp::Set(HookSlot::OnDrag, None)), true),
        IF_SETONTARGETLEAVE => (Action::Hook(HookOp::Set(HookSlot::HookParams, None)), true),
        IF_SETONVARTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::OnVarpTransmit,
                Some(TriggerSlot::Varps),
            )),
            true,
        ),
        IF_SETONTIMER => (Action::Hook(HookOp::Set(HookSlot::OnTimer, None)), true),
        IF_SETONOP => (Action::Hook(HookOp::Set(HookSlot::Params, None)), true),
        IF_SETONDRAGCOMPLETE => (
            Action::Hook(HookOp::Set(HookSlot::OnDragComplete, None)),
            true,
        ),
        IF_SETONCLICKREPEAT => (
            Action::Hook(HookOp::Set(HookSlot::OnClickRepeat, None)),
            true,
        ),
        IF_SETONMOUSEREPEAT => (Action::Hook(HookOp::Set(HookSlot::PopupScript, None)), true),
        IF_SETONINVTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::MouseLeaveScript,
                Some(TriggerSlot::MouseLeave),
            )),
            true,
        ),
        IF_SETONSTATTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::OnStatTransmit,
                Some(TriggerSlot::StatTransmit),
            )),
            true,
        ),
        IF_SETONTARGETENTER => (
            Action::Hook(HookOp::Set(HookSlot::OnTargetEnter, None)),
            true,
        ),
        IF_SETONSCROLLWHEEL => (Action::Hook(HookOp::Set(HookSlot::OnMouseMove, None)), true),
        IF_SETONCHATTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnChatTransmit, None)),
            true,
        ),
        IF_SETONKEY => (Action::Hook(HookOp::Set(HookSlot::OnKeyType, None)), true),
        IF_SETONFRIENDTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnFriendTransmit, None)),
            true,
        ),
        IF_SETONCLANTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnFriendsChatTransmit, None)),
            true,
        ),
        IF_SETONMISCTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnMiscTransmit, None)),
            true,
        ),
        IF_SETONDIALOGABORT => (Action::Hook(HookOp::Set(HookSlot::OnSubClose, None)), true),
        IF_SETONSUBCHANGE => (Action::Hook(HookOp::Set(HookSlot::OnSubChange, None)), true),
        IF_SETONSTOCKTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnStockTransmit, None)),
            true,
        ),
        IF_SETONCAMFINISHED => (
            Action::Hook(HookOp::Set(HookSlot::OnCamFinished, None)),
            true,
        ),
        IF_SETONRESIZE => (Action::Hook(HookOp::Set(HookSlot::OnResize, None)), true),
        IF_SETONVARCTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnKey, Some(TriggerSlot::KeyPress))),
            true,
        ),
        IF_SETONVARCSTRTRANSMIT => (
            Action::Hook(HookOp::Set(
                HookSlot::OnScrollWheel,
                Some(TriggerSlot::MouseWheel),
            )),
            true,
        ),
        IF_SETONOPT => (
            Action::Hook(HookOp::Set(HookSlot::OnTargetLeave, None)),
            true,
        ),
        IF_SETONCLANSETTINGSTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnClanSettingsTransmit, None)),
            true,
        ),
        IF_SETONCLANCHANNELTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnClanChannelTransmit, None)),
            true,
        ),
        IF_SETONVARCLANTRANSMIT => (
            Action::Hook(HookOp::Set(HookSlot::OnClanVarTransmit, None)),
            true,
        ),
        IF_CLEARSCRIPTHOOKS => (Action::Hook(HookOp::Clear), true),
        _ => unreachable!(),
    };
    Plan { action, by_hash }
}

fn failure(error: ComponentError, opcode: Opcode) -> VmErrorKind {
    match error {
        ComponentError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        ComponentError::Failure(failure) => VmErrorKind::Component(failure),
    }
}

fn failed_pops(action: Action) -> usize {
    match action {
        Action::Option(OptionOp::GetOp) => 1,
        Action::Option(OptionOp::SetOpKey) | Action::DragPickup => 2,
        _ => 0,
    }
}

fn hookless_pops(opcode: Opcode, action: Action) -> usize {
    match action {
        Action::Option(OptionOp::GetOp | OptionOp::Param) => 1,
        Action::Option(OptionOp::SetOpKey) => 10,
        Action::DragPickup => 2,
        Action::Option(OptionOp::SetParamInt) if opcode == Opcode::CC_SETPARAM => 2,
        _ => 0,
    }
}

fn reads_interfaces(action: Action) -> bool {
    !matches!(
        action,
        Action::Get(_)
            | Action::Media(
                MediaOp::CallOnResize
                    | MediaOp::NpcCustomBody
                    | MediaOp::NpcCustomHead
                    | MediaOp::NpcCustomRecol
                    | MediaOp::NpcCustomRetex
            )
            | Action::GetText
            | Action::GetNextSubId
            | Action::DragPickup
            | Action::Option(
                OptionOp::SetOpChar
                    | OptionOp::SetOpTChar
                    | OptionOp::SetOpKey
                    | OptionOp::GetOp
                    | OptionOp::GetOpBase
                    | OptionOp::GetTargetMask
                    | OptionOp::Param
            )
    )
}

fn arguments(action: Action, by_hash: bool) -> (usize, usize) {
    match action {
        Action::Option(op) => match op {
            OptionOp::SetOp | OptionOp::SetParamString => (1, 1),
            OptionOp::SetOpBase => (0, 1),
            OptionOp::SetOpCursor
            | OptionOp::SetOpChar
            | OptionOp::SetOpTKey
            | OptionOp::SetParamInt
            | OptionOp::SetTargetCursors => (2, 0),
            OptionOp::SetOpKey => (if by_hash { 3 } else { 11 }, 0),
            OptionOp::SetTargetOpCursor
            | OptionOp::SetOpTChar
            | OptionOp::SetMouseOverCursor
            | OptionOp::GetOp
            | OptionOp::Param => (1, 0),
            OptionOp::ClearOps | OptionOp::GetOpBase | OptionOp::GetTargetMask => (0, 0),
        },
        Action::Hook(_) => (0, 0),
        Action::Find => (0, 0),
        Action::Create => (3, 0),
        Action::Delete => (0, 0),
        Action::DeleteAll => (1, 0),
        Action::SendToOrder(_) | Action::HasSub(_) => (0, 0),
        Action::Get(_) | Action::GetText | Action::GetNextSubId => (0, 0),
        Action::Media(op) => (op.int_arguments(), 0),
        Action::SetDepthChild | Action::DragPickup => (2, 0),
        Action::SetPosition | Action::SetSize => (4, 0),
        Action::SetScrollSize | Action::SetAspect => (2, 0),
        Action::SetHide | Action::SetColour | Action::SetGraphic | Action::SetTrans => (1, 0),
        Action::SetText => (0, 1),
        Action::Set(_, ints, strings) => (ints, strings),
    }
}

impl Vm {
    pub(super) fn execute_component(
        &mut self,
        opcode: Opcode,
        operand: i32,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        let secondary = operand == 1;
        let Plan { action, by_hash } = plan(opcode);
        let lookup = match action {
            Action::Media(op) => self.media_lookup(opcode, op, by_hash, secondary, host)?,
            _ => MediaLookup::default(),
        };
        let model = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?
            .components()
            .map_err(|error| match error {
                HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                error => VmErrorKind::Host(error),
            })?;
        match action {
            Action::Find => self.component_find(opcode, model, secondary, by_hash),
            Action::Create => self.component_create(opcode, model, secondary),
            Action::Delete => model.delete(secondary).map_err(VmErrorKind::Component),
            Action::DeleteAll => {
                let hash = self.top_int(opcode, 1, 0, model)?;
                self.ints.pop();
                let target = model
                    .resolve(hash)
                    .map_err(|error| failure(error, opcode))?
                    .ok_or(VmErrorKind::Component(ComponentFailure::NoComponent))?;
                model.delete_all(target);
                Ok(())
            }
            Action::HasSub(compare) => {
                let needed = if compare { 2 } else { 1 };
                if self.ints.len() < needed {
                    return Err(VmErrorKind::StackUnderflow(StackKind::Int));
                }
                let base = self.ints.len() - needed;
                let open = model.open_interface(self.ints[base]);
                let present = match open {
                    Some(interface) => !compare || interface == self.ints[base + 1],
                    None => false,
                };
                self.ints.truncate(base);
                push(&mut self.ints, i32::from(present), StackKind::Int)
            }
            Action::SendToOrder(front) => {
                if by_hash {
                    let hash = self.top_int(opcode, 1, 0, model)?;
                    self.ints.pop();
                    let target = model
                        .resolve(hash)
                        .map_err(|error| failure(error, opcode))?;
                    if target.is_none() && !model.in_range(hash) {
                        return Err(VmErrorKind::Component(ComponentFailure::Index));
                    }
                    model
                        .send_to_order(hash >> 16, target, front)
                        .map_err(VmErrorKind::Component)
                } else {
                    let hook = model
                        .hook(secondary)
                        .ok_or(VmErrorKind::Component(ComponentFailure::NoComponent))?;
                    model
                        .send_to_order(hook.interface, Some(hook.component), front)
                        .map_err(VmErrorKind::Component)
                }
            }
            _ => self.component_field(opcode, model, secondary, action, by_hash, &lookup),
        }
    }

    fn media_lookup(
        &self,
        opcode: Opcode,
        op: MediaOp,
        by_hash: bool,
        secondary: bool,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<MediaLookup, VmErrorKind> {
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let map = |error: HostError| match error {
            HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
            error => VmErrorKind::Host(error),
        };
        let mut lookup = MediaLookup::default();
        let length = self.ints.len();
        let depth = usize::from(by_hash);
        let argument = |index: usize| {
            length
                .checked_sub(depth + index + 1)
                .map(|position| self.ints[position])
        };
        match op {
            MediaOp::Item { .. } => {
                if let Some(item) = argument(1).filter(|item| *item != -1) {
                    lookup.item = host.item_model_view(item).map_err(map)?;
                }
            }
            MediaOp::PlayerHeadSelf | MediaOp::PlayerModelSelf => {
                lookup.player_index = host.player_state().map_err(map)?.my_player_index;
            }
            MediaOp::ModelAnim => {
                if let Some(sequence) = argument(0).filter(|sequence| *sequence != -1) {
                    let current = {
                        let model = host.components().map_err(map)?;
                        let target = self.peek_target(model, by_hash, secondary);
                        target.map(|target| model.media_animation(target))
                    };
                    if current.is_some_and(|current| current != sequence)
                        && !host.sequence_playable(sequence).map_err(map)?
                    {
                        return Err(VmErrorKind::UnsupportedOpcode(opcode));
                    }
                }
            }
            MediaOp::NpcCustomBody
            | MediaOp::NpcCustomHead
            | MediaOp::NpcCustomRecol
            | MediaOp::NpcCustomRetex => {
                let primary = {
                    let model = host.components().map_err(map)?;
                    self.peek_target(model, by_hash, secondary)
                        .map(|target| model.media_primary(target))
                };
                if let Some(primary) = primary {
                    lookup.npc = host.npc_model_definition(primary).map_err(map)?;
                }
            }
            _ => {}
        }
        Ok(lookup)
    }

    fn peek_target(
        &self,
        model: &ComponentModel,
        by_hash: bool,
        secondary: bool,
    ) -> Option<ComponentId> {
        if by_hash {
            let hash = *self.ints.last()?;
            model.resolve(hash).ok().flatten()
        } else {
            model.hook(secondary).map(|hook| hook.component)
        }
    }

    fn top_int(
        &self,
        opcode: Opcode,
        needed: usize,
        depth: usize,
        model: &ComponentModel,
    ) -> Result<i32, VmErrorKind> {
        if self.ints.len() < needed {
            return Err(VmErrorKind::StackUnderflow(StackKind::Int));
        }
        let hash = self.ints[self.ints.len() - 1 - depth];
        if let Err(ComponentError::Unavailable) = model.resolve(hash) {
            return Err(VmErrorKind::UnsupportedOpcode(opcode));
        }
        Ok(hash)
    }

    fn component_find(
        &mut self,
        opcode: Opcode,
        model: &mut ComponentModel,
        secondary: bool,
        by_hash: bool,
    ) -> Result<(), VmErrorKind> {
        let (needed, slot_depth) = if by_hash { (1, None) } else { (2, Some(0)) };
        if self.ints.len() < needed {
            return Err(VmErrorKind::StackUnderflow(StackKind::Int));
        }
        let slot = match slot_depth {
            Some(depth) => self.ints[self.ints.len() - 1 - depth],
            None => -1,
        };
        let hash = self.ints[self.ints.len() - needed];
        if slot != -1 || by_hash {
            if let Err(ComponentError::Unavailable) = model.resolve(hash) {
                return Err(VmErrorKind::UnsupportedOpcode(opcode));
            }
        }
        let new_len = self.ints.len() - needed;
        self.ints.truncate(new_len);
        let found = if !by_hash && slot == -1 {
            false
        } else {
            match model.find(secondary, hash, slot) {
                Ok(found) => found,
                Err(error) => {
                    let _ = push(&mut self.ints, hash, StackKind::Int);
                    return Err(failure(error, opcode));
                }
            }
        };
        push(&mut self.ints, i32::from(found), StackKind::Int)
    }

    fn component_create(
        &mut self,
        opcode: Opcode,
        model: &mut ComponentModel,
        secondary: bool,
    ) -> Result<(), VmErrorKind> {
        let hash = self.top_int(opcode, 3, 2, model)?;
        let base = self.ints.len() - 3;
        let type_id = self.ints[base + 1];
        let slot = self.ints[base + 2];
        self.ints.truncate(base);
        model
            .create(secondary, hash, type_id, slot)
            .map_err(|error| failure(error, opcode))
    }

    fn component_field(
        &mut self,
        opcode: Opcode,
        model: &mut ComponentModel,
        secondary: bool,
        action: Action,
        by_hash: bool,
        lookup: &MediaLookup,
    ) -> Result<(), VmErrorKind> {
        let (int_arguments, string_arguments) = arguments(action, by_hash);
        let hash_needed = usize::from(by_hash);
        if self.ints.len() < hash_needed + int_arguments {
            return Err(VmErrorKind::StackUnderflow(StackKind::Int));
        }
        if self.strings.len() < string_arguments {
            return Err(VmErrorKind::StackUnderflow(StackKind::String));
        }
        let (interface, target): (i32, ComponentId) = if by_hash {
            let hash = self.top_int(opcode, 1, 0, model)?;
            self.ints.pop();
            let setter = reads_interfaces(action);
            let resolved = match model.resolve(hash) {
                Ok(resolved) => resolved,
                Err(error) => {
                    if matches!(action, Action::Option(OptionOp::SetOpKey) | Action::DragPickup) {
                        self.drop_ints(2);
                    }
                    return Err(failure(error, opcode));
                }
            };
            let target = match resolved {
                Some(target) => target,
                None if setter && !model.in_range(hash) => {
                    self.drop_ints(failed_pops(action));
                    return Err(VmErrorKind::Component(ComponentFailure::Index));
                }
                None => {
                    self.drop_ints(failed_pops(action));
                    if matches!(
                        action,
                        Action::Get(getter) if getter != IntGetter::InvCount
                    ) || matches!(action, Action::Option(OptionOp::GetTargetMask))
                    {
                        let _ = push(&mut self.ints, hash, StackKind::Int);
                    }
                    return Err(VmErrorKind::Component(ComponentFailure::NoComponent));
                }
            };
            (hash >> 16, target)
        } else {
            let Some(hook) = model.hook(secondary) else {
                let mut pops = hookless_pops(opcode, action);
                if matches!(action, Action::Option(OptionOp::Param)) {
                    let id = self.ints.last().copied().unwrap_or(0);
                    let integer = model.param_type(id).is_some_and(|param| !param.is_string());
                    pops = usize::from(!integer);
                }
                self.drop_ints(pops);
                return Err(VmErrorKind::Component(ComponentFailure::NoComponent));
            };
            (hook.interface, hook.component)
        };
        let base = self.ints.len() - int_arguments;
        let values: Vec<i32> = self.ints[base..].to_vec();
        self.ints.truncate(base);
        let component = |error: ComponentFailure| VmErrorKind::Component(error);
        match action {
            Action::Get(getter) => {
                let value = model.get_int(target, getter);
                push(&mut self.ints, value, StackKind::Int)
            }
            Action::Media(op) => {
                let mut values = values;
                if op.pops_late() {
                    if !model.video_matches(target, op) {
                        return Ok(());
                    }
                    let value = self
                        .ints
                        .pop()
                        .ok_or(VmErrorKind::StackUnderflow(StackKind::Int))?;
                    values.push(value);
                }
                model
                    .apply_media(interface, target, op, &values, lookup)
                    .map_err(component)
            }
            Action::GetNextSubId => {
                let value = model.next_sub_id(target);
                push(&mut self.ints, value, StackKind::Int)
            }
            Action::DragPickup => model
                .drag_pickup(target, values[0], values[1])
                .map_err(|error| failure(error, opcode)),
            Action::SetDepthChild => model
                .set_depth_child(target, values[0], values[1])
                .map_err(|error| failure(error, opcode)),
            Action::GetText => {
                let text = model.get_text(target);
                check_string(Some(&text), self.limits.max_string_bytes)?;
                push(&mut self.strings, Some(text), StackKind::String)
            }
            Action::SetPosition => model
                .set_position(
                    interface,
                    target,
                    [values[0], values[1], values[2], values[3]],
                )
                .map_err(component),
            Action::SetSize => model
                .set_size(
                    interface,
                    target,
                    [values[0], values[1], values[2], values[3]],
                )
                .map_err(component),
            Action::SetScrollSize => model
                .set_scroll_size(interface, target, values[0], values[1])
                .map_err(component),
            Action::SetAspect => model
                .set_aspect(interface, target, values[0], values[1])
                .map_err(component),
            Action::SetHide => {
                model.set_hide(interface, target, values[0]);
                Ok(())
            }
            Action::SetColour => {
                model.set_colour(interface, target, values[0]);
                Ok(())
            }
            Action::SetGraphic => {
                model.set_graphic(interface, target, values[0]);
                Ok(())
            }
            Action::SetTrans => {
                model.set_trans(target, values[0]);
                Ok(())
            }
            Action::Set(setter, _, strings) => {
                let text = if strings == 1 {
                    self.strings.pop().flatten()
                } else {
                    None
                };
                model.apply(interface, target, setter, &values, text);
                Ok(())
            }
            Action::SetText => {
                let text: Option<Arc<str>> = self.strings.pop().flatten();
                model.set_text(interface, target, text);
                Ok(())
            }
            Action::Option(op) => self.component_option(op, model, target, by_hash, &values),
            Action::Hook(op) => self.component_hook(op, model, target),
            _ => unreachable!(),
        }
    }

    fn pop_hook_int(&mut self) -> Result<i32, VmErrorKind> {
        self.ints
            .pop()
            .ok_or(VmErrorKind::StackUnderflow(StackKind::Int))
    }

    fn pop_trigger_array(&mut self, types: Option<&str>) -> Result<Option<Vec<i32>>, VmErrorKind> {
        let Some(types) = types.filter(|types| types.ends_with('Y')) else {
            return Ok(None);
        };
        let _ = types;
        let size = self.pop_hook_int()?;
        if size <= 0 {
            return Ok(None);
        }
        let mut triggers = Vec::new();
        for _ in 0..size {
            triggers.push(self.pop_hook_int()?);
        }
        triggers.reverse();
        Ok(Some(triggers))
    }

    fn pop_hook_script(
        &mut self,
        types: Option<&str>,
    ) -> Result<Option<Vec<HookValue>>, VmErrorKind> {
        let Some(types) = types else {
            return Ok(None);
        };
        let kinds: Vec<char> = types.chars().collect();
        let mut script = Vec::with_capacity(kinds.len() + 1);
        for kind in kinds.iter().rev() {
            let value = match kind {
                's' => HookValue::Text(
                    self.strings
                        .pop()
                        .ok_or(VmErrorKind::StackUnderflow(StackKind::String))?,
                ),
                '\u{a7}' => HookValue::Long(
                    self.longs
                        .pop()
                        .ok_or(VmErrorKind::StackUnderflow(StackKind::Long))?,
                ),
                _ => HookValue::Int(self.pop_hook_int()?),
            };
            script.push(value);
        }
        let script_id = self.pop_hook_int()?;
        if script_id == -1 {
            return Ok(None);
        }
        script.push(HookValue::Int(script_id));
        script.reverse();
        Ok(Some(script))
    }

    fn component_hook(
        &mut self,
        op: HookOp,
        model: &mut ComponentModel,
        target: ComponentId,
    ) -> Result<(), VmErrorKind> {
        match op {
            HookOp::Clear => {
                model.component_mut(target).hooks.clear();
                Ok(())
            }
            HookOp::Set(slot, trigger) => {
                let types = self
                    .strings
                    .pop()
                    .ok_or(VmErrorKind::StackUnderflow(StackKind::String))?;
                let mut types = types.as_deref().map(str::to_owned);
                let triggers = self.pop_trigger_array(types.as_deref())?;
                if triggers.is_some() {
                    if let Some(types) = types.as_mut() {
                        types.pop();
                    }
                }
                let script = self.pop_hook_script(types.as_deref())?;
                model.component_mut(target).hooks.set(
                    slot,
                    script,
                    trigger.map(|trigger| (trigger, triggers)),
                );
                Ok(())
            }
        }
    }

    fn drop_ints(&mut self, count: usize) {
        let keep = self.ints.len().saturating_sub(count);
        self.ints.truncate(keep);
    }

    fn component_option(
        &mut self,
        op: OptionOp,
        model: &mut ComponentModel,
        target: ComponentId,
        by_hash: bool,
        values: &[i32],
    ) -> Result<(), VmErrorKind> {
        let component = |error: ComponentFailure| VmErrorKind::Component(error);
        let npe = || VmErrorKind::Component(ComponentFailure::NoComponent);
        match op {
            OptionOp::SetOp => {
                let name = self.strings.pop().flatten();
                let slot = values[0].wrapping_sub(1);
                if (0..=9).contains(&slot) {
                    model
                        .component_mut(target)
                        .options
                        .add_option(slot as usize, name);
                }
                Ok(())
            }
            OptionOp::SetOpBase => {
                model.component_mut(target).options.op_base = self.strings.pop().flatten();
                Ok(())
            }
            OptionOp::SetOpCursor => {
                if (1..=10).contains(&values[0]) {
                    model
                        .component_mut(target)
                        .options
                        .add_cursor(values[0] as usize - 1, values[1]);
                }
                Ok(())
            }
            OptionOp::SetTargetOpCursor => {
                model.component_mut(target).options.target_op_cursor = values[0];
                Ok(())
            }
            OptionOp::SetMouseOverCursor => {
                model.component_mut(target).options.mouse_over_cursor = values[0];
                Ok(())
            }
            OptionOp::SetTargetCursors => {
                let options = &mut model.component_mut(target).options;
                options.target_leave_cursor = values[1];
                options.target_over_cursor = values[0];
                Ok(())
            }
            OptionOp::ClearOps => {
                model.component_mut(target).options.options = None;
                Ok(())
            }
            OptionOp::SetOpChar => {
                let slot = values[0].wrapping_sub(1);
                if (0..=9).contains(&slot) {
                    model
                        .component_mut(target)
                        .options
                        .set_char_slot(slot as usize, values[1]);
                    Ok(())
                } else {
                    Err(component(ComponentFailure::Rejected))
                }
            }
            OptionOp::SetOpTChar => {
                model
                    .component_mut(target)
                    .options
                    .set_char_slot(10, values[0]);
                Ok(())
            }
            OptionOp::SetOpTKey => {
                model.component_mut(target).options.set_trigger_key(
                    10,
                    Some(vec![values[0] as i8]),
                    Some(vec![values[1] as i8]),
                );
                Ok(())
            }
            OptionOp::SetOpKey => {
                let slot = values[0].wrapping_sub(1);
                let (codes, modifiers) = if by_hash {
                    (Some(vec![values[1] as i8]), Some(vec![values[2] as i8]))
                } else {
                    let pairs = &values[1..];
                    let count = (0..5).take_while(|pair| pairs[pair * 2] >= 0).count();
                    if count > 0 {
                        (
                            Some((0..count).map(|pair| pairs[pair * 2] as i8).collect()),
                            Some((0..count).map(|pair| pairs[pair * 2 + 1] as i8).collect()),
                        )
                    } else {
                        (None, None)
                    }
                };
                if (0..=9).contains(&slot) {
                    model.component_mut(target).options.set_trigger_key(
                        slot as usize,
                        codes,
                        modifiers,
                    );
                    Ok(())
                } else {
                    Err(component(ComponentFailure::Rejected))
                }
            }
            OptionOp::SetParamInt => {
                let param = model.param_type(values[0]).cloned().ok_or_else(npe)?;
                let options = &mut model.component_mut(target).options;
                if values[1] != param.default_int {
                    options.set_int_param(values[0], values[1]);
                } else {
                    options.remove_param(values[0]);
                }
                Ok(())
            }
            OptionOp::SetParamString => {
                let value = self.strings.pop().flatten();
                let param = model.param_type(values[0]).cloned().ok_or_else(npe)?;
                let value = value.ok_or_else(npe)?;
                let options = &mut model.component_mut(target).options;
                if param.type_name.as_deref() == Some(&*value) {
                    options.remove_param(values[0]);
                } else {
                    options.set_string_param(values[0], value);
                }
                Ok(())
            }
            OptionOp::GetOp => {
                let text: Arc<str> = model
                    .component(target)
                    .options
                    .option(values[0].wrapping_sub(1))
                    .map_err(component)?
                    .cloned()
                    .unwrap_or_else(|| Arc::from(""));
                check_string(Some(&text), self.limits.max_string_bytes)?;
                push(&mut self.strings, Some(text), StackKind::String)
            }
            OptionOp::GetOpBase => {
                let text: Arc<str> = model
                    .component(target)
                    .options
                    .op_base
                    .clone()
                    .unwrap_or_else(|| Arc::from(""));
                check_string(Some(&text), self.limits.max_string_bytes)?;
                push(&mut self.strings, Some(text), StackKind::String)
            }
            OptionOp::GetTargetMask => {
                let mask = (model.component(target).options.target_settings >> 11) & 0x7f;
                push(&mut self.ints, mask, StackKind::Int)
            }
            OptionOp::Param => {
                let param = model.param_type(values[0]).cloned().ok_or_else(npe)?;
                let options = &model.component(target).options;
                if param.is_string() {
                    let default = param.type_name.ok_or_else(npe)?;
                    let text = options.string_param(values[0], &default);
                    check_string(Some(&text), self.limits.max_string_bytes)?;
                    push(&mut self.strings, Some(text), StackKind::String)
                } else {
                    let value = options.int_param(values[0], param.default_int);
                    push(&mut self.ints, value, StackKind::Int)
                }
            }
        }
    }
}
