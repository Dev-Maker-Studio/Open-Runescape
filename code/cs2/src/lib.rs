mod appearance;
mod bounds;
mod calendar;
mod client;
mod components;
mod decode;
mod definitions;
mod entity;
mod error;
mod fonts;
mod fdlibm;
mod host;
mod inventory;
mod names;
mod numeric;
mod chat;
mod clan;
mod clan_var;
mod outbox;
mod opcode;
mod player;
mod lobby;
mod login;
mod projection;
mod quest;
mod quick_chat;
mod platform;
mod scene;
mod world_map_defs;
mod social;
mod varc;
mod vm;
mod world;

pub use bounds::{BoundingBox, BoundsState, BoxSet, LocHeight, ModelBounds};
pub use client::{
    AreaSound, AreaSoundKind, ClientState, KeyState, MouseState, Preferences, AREA_SOUND_CAPACITY,
    TOOLKIT_MODERN,
};
pub use components::{
    ComponentError, ComponentEvent, ComponentEventKind, ComponentFailure, ComponentHook,
    ComponentHooks, ComponentId, ComponentModel, ComponentOptions, ComponentSettings,
    ComponentState, EntityInterface, HookScript, HookSlot, HookValue, InterfaceSelection,
    HookRequest, InterfaceRequest, DragState, DragTarget, ItemModelView, KeyTriggers, NpcCustomisation, NpcModelDefinition, OptionInteraction, ParamNode, ParamType, TriggerSlot,
};
pub use appearance::{
    AppearanceState, MeshModifier, PlayerAppearance, CRC64_TABLE, FEMALE_IDKIT_SLOTS,
    IDK_PART_TABLE, MALE_IDKIT_SLOTS,
};
pub use decode::{Counts, DecodeLimits, Instruction, Operand, Script, SwitchCase, SwitchTable};
pub use definitions::{
    BasDefinition,
    ConfigKind, InteractionColours, ItemDefinition, ParamDefinition, ParamTable,
};
pub use entity::{
    CurrentEntity, EntityCommon, EntityState, GroundItemState, MyCoord, NearbyPlayer, NpcEntityState,
    NpcTypeInfo, PlayerEntityState,
};
pub use error::{DecodeError, DecodeErrorKind, EnumFailure, StackKind, VmError, VmErrorKind};
pub use fonts::FontMetrics;
pub use host::{EnumDefinition, EnumValue, HostError, VarHost, Varbit, VarbitLookup};
pub use inventory::{
    ContainerType, ItemContainer, ItemModel, ItemRecord, QueryParam, QueryResults, QUERY_LIMIT,
};
pub use names::{
    decode_friends_chat_name, decode_uid_name, format_displayname, name_to_long, names_match,
};
pub use chat::{ChatLine, ChatState, CHAT_LINE_CAPACITY};
pub use clan_var::{ClanVarDefs, ClanVarType, ClanVarValue};
pub use clan::{
    ClanChannel, ClanChannelPlayer, ClanSettings, ClanState, StockSlot, STOCK_SLOT_COUNT,
};
pub use lobby::LobbyState;
pub use login::{
    CreateStage, LobbyPacket, LoginConnection, LoginEffect, LoginState, Secret,
};
pub use quest::{
    QuestDecodeError, QuestDefinition, QuestDefs, QuestVarRange, QuestVarRequirement,
};
pub use outbox::{ChatSend, ClientPacket, Outbox, RouteRequest, TypedMessage};
pub use opcode::{Opcode, OpcodeMetadata, OPCODES};
pub use player::{
    MyPlayer, NpcState, PlayerState, VarnBit, NPC_STAT_COUNT, SKILL_COUNT,
};
pub use projection::{
    multiply44, HintAnchor, Matrix43, ProjectionState, DIVISION_BY_ZERO,
};
pub use quick_chat::{
    QuickChatCategory, QuickChatDecodeError, QuickChatDefs, QuickChatFault, QuickChatMessage,
    QuickChatSearch, QuickChatValueType, MENU_BASE, SEARCH_LIMIT,
};
pub use scene::{HeightGrid, LightingPlane, SceneState};
pub use world_map_defs::{
    MecType, WorldMapCategory, WorldMapData, WorldMapDecodeError, WorldMapDefs, WorldMapElement,
    WorldMapPlacement,
};
#[doc(hidden)]
pub use fdlibm::atan2 as fdlibm_atan2;
pub use social::{
    equals_ignore_case, strip_crown, Friend, FriendsChatPlayer, Ignore, SocialState,
    FRIEND_CAPACITY, IGNORE_CAPACITY,
};
pub use varc::{
    VarcError, VarcLimits, VarcPulse, VarcPulseKind, VarcState, MAX_VARC_TIME_MS, VARC_DEBOUNCE_MS,
    VARC_SLOT_CAPACITY,
};
pub use vm::{Arguments, Execution, Locals, ScriptRepository, Vm, VmLimits};
pub use platform::{
    mini_menu_click_type, DirectoryPickerState, MecDefinition, MessageBoxState, MiniMenuEntryState, PlatformState,
    GAME_STATE_RENDERING_SCENE,
};
pub use world::{
    CameraState, MiniMenuState, MiniMenuStyle, SplinePath, WorldMapEffect, WorldMapState, WorldState,
    CAMERA_SHAKE_SLOTS,
    NEGATIVE_ARRAY_SIZE,
    RUNTIME_FAILURE, SPLINE_PATH_COUNT,
};
