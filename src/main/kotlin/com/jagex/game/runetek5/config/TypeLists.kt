package com.jagex.game.runetek5.config

import java.util.Arrays

import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.bas.BasTypeList
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsTypeList
import com.jagex.game.runetek5.config.cursor.CursorTypeList
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType
import com.jagex.game.runetek5.config.defaults.InteractionDefaultsType
import com.jagex.game.runetek5.config.enumtype.EnumTypeList
import com.jagex.game.runetek5.config.flo.FloTypeList
import com.jagex.game.runetek5.config.flu.FluTypeList
import com.jagex.game.runetek5.config.gametip.GameTipTypeList
import com.jagex.game.runetek5.config.hitbar.HitbarTypeList
import com.jagex.game.runetek5.config.hitsplat.HitSplatTypeList
import com.jagex.game.runetek5.config.identitykit.IdkTypeList
import com.jagex.game.runetek5.config.inputsubscriber.CutsceneDefaultsType
import com.jagex.game.runetek5.config.light.LightTypeList
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.config.mapsprite.MapSpriteTypeList
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.mec.MECTypeList
import com.jagex.game.runetek5.config.npc.NpcTypeList
import com.jagex.game.runetek5.config.item.ItemTypeList
import com.jagex.game.runetek5.config.param.ParamTypeList
import com.jagex.game.runetek5.config.particle.ParticleProducerType
import com.jagex.game.runetek5.config.particle.ParticleType
import com.jagex.game.runetek5.config.quest.QuestTypeList
import com.jagex.game.runetek5.config.quickchat.QuickchatCategoryTypeList
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageTypeList
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerTypeList
import com.jagex.game.runetek5.config.sequence.SeqTypeList
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimTypeList
import com.jagex.game.runetek5.config.skybox.SkyboxTypeList
import com.jagex.game.runetek5.config.struct.StructTypeList
import com.jagex.game.runetek5.config.sun.SunTypeList
import com.jagex.game.runetek5.config.varc.*
import com.jagex.game.runetek5.config.varn.VarnBitTypeList
import com.jagex.game.runetek5.config.varn.VarnTypeList
import com.jagex.game.runetek5.config.varp.VarpTypeList
import com.jagex.game.runetek5.config.varp.VarpbitTypeList
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.graphics.hint.HintArrow
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.staticmethods.StaticMethods

object TypeLists {

    @JvmField var QUEST: QuestTypeList? = null
    @JvmField var SEQ: SeqTypeList? = null
    @JvmField var PARAMS: ParamTypeList? = null
    @JvmField var BAS: BasTypeList? = null
    @JvmField var CURSOR: CursorTypeList? = null
    @JvmField var ENUM: EnumTypeList? = null
    @JvmField var FLO: FloTypeList? = null
    @JvmField var FLU: FluTypeList? = null
    @JvmField var HITSPLAT: HitSplatTypeList? = null
    @JvmField var HITBAR: HitbarTypeList? = null
    @JvmField var IDK: IdkTypeList? = null
    @JvmField var CONTAINER: ItemContainerTypeList? = null
    @JvmField var LIGHT: LightTypeList? = null
    @JvmField var LOC: ObjectTypeList? = null
    @JvmField var MEC: MECTypeList? = null
    @JvmField var MAP_SPRITE: MapSpriteTypeList? = null
    @JvmField var NPC: NpcTypeList? = null
    @JvmField var OBJ: ItemTypeList? = null
    @JvmField var SKYBOX: SkyboxTypeList? = null
    @JvmField var SUN: SunTypeList? = null
    @JvmField var SPOT_ANIM: SpotAnimTypeList? = null
    @JvmField var STRUCT: StructTypeList? = null
    @JvmField var VARC_STRING: VarcStringTypeList? = null
    @JvmField var VARC: VarcTypeList? = null
    @JvmField var VARPBIT: VarpbitTypeList? = null
    @JvmField var VAR: VarpTypeList? = null
    @JvmField var VARN: VarnTypeList? = null
    @JvmField var VARNBIT: VarnBitTypeList? = null
    @JvmField var CLAN_VAR: ClanVarTypeList? = null
    @JvmField var CLAN_VAR_SETTINGS: ClanVarSettingsTypeList? = null
    @JvmField var QUICK_CHAT_CATEGORY: QuickchatCategoryTypeList? = null
    @JvmField var QUICK_CHAT_MESSAGE: QuickchatMessageTypeList? = null
    @JvmField var MATERIALS: MaterialProvider? = null
    @JvmField var GAME_TIPS: GameTipTypeList? = null

    @JvmField var CUTSCENE_DEFAULTS: CutsceneDefaultsType? = null
    @JvmField var EQUIPMENT_DEFAULTS: EquipmentDefaultsType? = null
    @JvmField var INTERACTION_DEFAULTS: InteractionDefaultsType? = null

    @JvmStatic
    fun clean() {
        FLO!!.cycle()
        FLU!!.processCycle()
        IDK!!.cycleCache()
        Resource.mapRegion!!.getLoctypeList().cacheClean()
        NPC!!.sweepCaches()
        OBJ!!.processCycle()
        SEQ!!.processCacheCycle()
        SPOT_ANIM!!.sweepCache()
        VARPBIT!!.processCycle()
        VAR!!.sweepCache()
        VARN!!.sweep()
        VARNBIT!!.sweepCache()
        CLAN_VAR!!.sweep()
        CLAN_VAR_SETTINGS!!.sweepCache()
        BAS!!.cleanCache()
        MEC!!.sweepCache()
        MAP_SPRITE!!.sweepCaches()
        PARAMS!!.sweepCache()
        SKYBOX!!.cacheClean()
        SUN!!.sweepCache()
        LIGHT!!.cacheClean()
        CURSOR!!.processCycle()
        HITSPLAT!!.processCacheCycle()
        HITBAR!!.processCycle()
        PlayerAppearance.processCacheCycle()
        Component.processCacheCycles()
        StaticMethods.initializedFontcombos!!.sweepCache()
        HintArrow.sweepCache()
        PathingEntity.sweepPathingEntityModelcache()
        Client.OVERHEAD_SPRITES.clean(5)
        ChatBox.CHAT_ICONS.clean(5)
        Client.DUMMY.clean(5)
    }

    @JvmStatic
    fun resetCachedData(skipTypelists: Boolean) {
        if (!skipTypelists) {
            FLO!!.reset()
            FLU!!.resetCache()
            IDK!!.reset()
            Resource.mapRegion!!.getLoctypeList().resetAll()
            NPC!!.resetCaches()
            OBJ!!.resetCaches()
            SEQ!!.resetCaches()
            SPOT_ANIM!!.resetCache()
            VARPBIT!!.resetCache()
            VAR!!.resetCache()
            VARN!!.reset()
            VARNBIT!!.resetCache()
            CLAN_VAR!!.reset()
            CLAN_VAR_SETTINGS!!.resetCache()
            BAS!!.resetCache()
            MAP_SPRITE!!.resetCaches()
            MEC!!.resetCache()
            PARAMS!!.resetCache()
            SKYBOX!!.cacheReset()
            SUN!!.resetCache()
            LIGHT!!.cacheReset()
            CURSOR!!.resetCaches()
            HITSPLAT!!.resetCaches()
            HITBAR!!.resetCaches()
            PlayerAppearance.resetCache()
            Component.resetCaches()
            StaticMethods.initializedFontcombos!!.resetCache()
            ItemContainer.resetContainerItemCache()
            if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
                Arrays.fill(Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA, null)
                Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES = 0
            }
            Resource.mapRegion!!.getEnvironmentManager().reset()
            HintArrow.resetCache()
            PathingEntity.resetPathingentityModels()
            ParticleProducerType.resetCache()
            ParticleType.resetCache()
            Client.DUMMY.reset()
            Client.OVERHEAD_SPRITES.reset()
            ChatBox.CHAT_ICONS.reset()
        }
        CS2Executor.resetCs2Cache()
        Renderers.cleanup()
    }

    @JvmStatic
    fun clear() {
        FLO!!.clear()
        FLU!!.clearQueue()
        IDK!!.remove()
        Resource.mapRegion!!.getLoctypeList().clearQueue()
        NPC!!.clearCacheQueues()
        OBJ!!.clearCacheQueues()
        SEQ!!.clearQueues()
        SPOT_ANIM!!.clearCacheQueue()
        VARPBIT!!.clearQueue()
        VAR!!.clearCacheQueue()
        VARN!!.clearQueue()
        VARNBIT!!.clearCacheQueue()
        CLAN_VAR!!.clearQueue()
        CLAN_VAR_SETTINGS!!.clearCacheQueue()
        BAS!!.resizeCache()
        MAP_SPRITE!!.clearCacheQueues()
        MEC!!.clearCacheQueue()
        PARAMS!!.clearCacheQueue()
        SKYBOX!!.cacheResize()
        SUN!!.clearCacheQueue()
        LIGHT!!.cacheResize()
        CURSOR!!.clearQueues()
        HITSPLAT!!.clearQueues()
        HITBAR!!.clearQueues()
        PlayerAppearance.clearQueue()
        Component.clearQueues()
        StaticMethods.initializedFontcombos!!.clearCacheQueue()
        HintArrow.clearHintarrows()
        PathingEntity.clearQueue()
        Client.OVERHEAD_SPRITES.resize()
        ChatBox.CHAT_ICONS.resize()
        Client.DUMMY.resize()
    }
}
