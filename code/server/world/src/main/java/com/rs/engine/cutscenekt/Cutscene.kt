package com.rs.engine.cutscenekt

import org.darkan.core.cache.ObjectDefinitions
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.startConversation
import com.rs.game.World.sendProjectile
import com.rs.game.World.sendSpotAnim
import com.rs.game.World.spawnNPC
import com.rs.game.World.spawnObject
import com.rs.game.map.instance.Instance
import com.rs.game.model.WorldProjectile
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.async.ConditionalWait
import org.darkan.core.model.MoveType
import com.rs.game.model.entity.async.TickWait
import com.rs.game.model.entity.async.WaitCondition
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import world.gregs.voidps.type.Tile
import org.darkan.core.Logger
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Varbit
import com.rs.utils.types
import kotlin.coroutines.*

private val FIXED_GAMEFRAME = intArrayOf(
    Component.interface_548.component_548_4,
    Component.interface_548.component_548_5,
    Component.interface_548.component_548_6,
    Component.interface_548.component_548_7,
    Component.interface_548.component_548_8,
    Component.interface_548.component_548_9,
    Component.interface_548.component_548_10,
    Component.interface_548.component_548_11,
    Component.interface_548.component_548_12,
    Component.interface_548.component_548_13,
    Component.interface_548.component_548_14,
    Component.interface_548.buff_bar,
)

private val RESIZEABLE_GAMEFRAME = intArrayOf(Component.interface_746.buff_bar, Component.interface_751.component_751_3)

class Cutscene(val player: Player) : Continuation<Unit> {
    lateinit var coroutine: Continuation<Unit>
    private var waitCondition: WaitCondition? = null
    var started = false
    var stopped = false

    override val context: CoroutineContext = EmptyCoroutineContext

    val objects = mutableMapOf<Int, Any>()
    var mapHidden = false
    private var hiddenGameframe: IntArray? = null
    var constructingRegion = false
    private var instance: Instance? = null
    var endTile: Tile = player.tile

    init {
        createObjectMap()
    }

    internal fun tick() {
        val next = waitCondition ?: return
        if (next.wait.canContinue()) {
            next.continuation.resume(Unit)
        }
    }

    internal fun start() {
        started = true
        player.setLargeSceneView(true)
        player.lock()
        player.stopAll(true, false)
        player.tempAttribs.setB("CUTSCENE_INTERFACE_CLOSE_DISABLED", true)
    }

    override fun resumeWith(result: Result<Unit>) {
        waitCondition = null
        result.exceptionOrNull()?.let { e -> Logger.handle(Cutscene::class.java, "resumeWith", e) }
    }

    fun stop() {
        if (stopped) return
        waitCondition = null
        if (player.x != endTile.x || player.y != endTile.y || player.plane != endTile.plane) player.tele(endTile)
        if (mapHidden) player.packets.setBlockMinimapState(0)
        restoreGameframe()
        restoreDefaultAspectRatio()
        player.packets.sendResetCamera()
        player.setLargeSceneView(false)
        player.resetReceivedHits()
        player.poison.reset()
        player.unlock()
        deleteObjects()
        instance?.destroy()
        player.tempAttribs.removeB("CUTSCENE_INTERFACE_CLOSE_DISABLED")
        stopped = true
    }

    fun getX(x: Int): Int {
        var instance = this.instance
        if (instance == null) instance = player.instancedArea
        return if (instance != null && instance.isCreated) instance.getLocalX(x) else x
    }

    fun getY(y: Int): Int {
        var instance = this.instance
        if (instance == null) instance = player.instancedArea
        return if (instance != null && instance.isCreated) instance.getLocalY(y) else y
    }

    fun tileFromLocal(localX: Int, localY: Int): Tile {
        return Tile.of(getX(localX), getY(localY), player.plane)
    }

    fun createObjectMap() {
        endTile = player.tile
    }

    fun deleteObjects() {
        for (o in objects.values) deleteObject(o)
    }

    fun deleteObject(o: Any) {
        if (o is NPC && !o.persistsBeyondCutscene()) o.finish()
    }

    fun lowerAspectRatio() {
        player.vars.setVar(1241, 1)
    }

    fun restoreDefaultAspectRatio() {
        player.vars.setVar(1241, 3)
    }

    fun hideMinimap() {
        player.packets.setBlockMinimapState(2)
    }

    fun unhideMinimap() {
        player.packets.setBlockMinimapState(0)
    }

    fun hideGameframe() {
        if (hiddenGameframe != null) return
        val components = if (player.resizeable()) RESIZEABLE_GAMEFRAME else FIXED_GAMEFRAME
        hiddenGameframe = components
        sendGameframeHidden(components, true)
    }

    fun restoreGameframe() {
        val components = hiddenGameframe ?: return
        hiddenGameframe = null
        sendGameframeHidden(components, false)
    }

    private fun sendGameframeHidden(components: IntArray, hidden: Boolean) {
        player.vars.setVarBit(Varbit.varbit_6448, if (hidden) 1 else 0)
        player.packets.sendRunScript(Cs2.cs2_4008, if (hidden) 1 else 0, 0)
        for (component in components)
            player.packets.setIFHidden(Utils.interfaceIdFromHash(component), Utils.componentIdFromHash(component), hidden)
    }

    fun camPos(moveLocalX: Int, moveLocalY: Int, height: Int, speed1: Int = -1, speed2: Int = -1) {
        player.packets.sendCameraPos(Tile.of(getX(moveLocalX), getY(moveLocalY), 0), height, speed1, speed2)
    }

    fun camLook(viewLocalX: Int, viewLocalY: Int, height: Int, speedToExactDestination: Int = -1, speedOnRoutePath: Int = -1) {
        player.packets.sendCameraLook(Tile.of(getX(viewLocalX), getY(viewLocalY), 0), height, speedToExactDestination, speedOnRoutePath)
    }

    fun camShake(slotId: Int, v1: Int, v2: Int, v3: Int, v4: Int) {
        player.packets.sendCameraShake(slotId, v1, v2, v3, v4)
    }

    fun camShakeReset() {
        player.packets.sendStopCameraShake()
    }

    fun camPosResetHard() {
        player.packets.sendResetCamera()
    }

    fun camPosResetSoft() {
        player.packets.sendResetCameraSoft()
    }

    fun dialogue(block: DialogueBuilder.() -> Unit) {
        player.startConversation(block)
    }

    fun fadeIn() {
        player.interfaceManager.fadeIn()
    }

    fun fadeOut() {
        player.interfaceManager.fadeOut()
    }

    suspend fun fadeInAndWait() {
        player.interfaceManager.fadeIn()
        wait(5)
    }

    suspend fun fadeOutAndWait() {
        player.interfaceManager.fadeOut()
        wait(5)
    }

    fun fadeOutQuickly() {
        player.interfaceManager.removeInterface(115)
    }

    fun fadeInBG() {
        player.interfaceManager.fadeInBG()
    }

    fun fadeOutBG() {
        player.interfaceManager.fadeOutBG()
    }

    fun music(id: Int) {
        player.musicTrack(id, 5)
    }

    fun musicEffect(id: Int) {
        player.jingle(id)
    }

    fun npcCreate(npcId: Int, x: Int, y: Int, z: Int, initNpc: (NPC.() -> Unit)? = null): NPC {
        val npc = spawnNPC(npcId, Tile.of(getX(x), getY(y), z), true, true)
        npc.setRandomWalk(false)
        initNpc?.invoke(npc)
        objects[npc.hashCode()] = npc
        return npc
    }

    fun npcCreatePersistent(npcId: Int, x: Int, y: Int, z: Int, initNpc: (NPC.() -> Unit)? = null): NPC {
        val npc = npcCreate(npcId, x, y, z, initNpc)
        npc.persistBeyondCutscene()
        npc.setRandomWalk(false)
        initNpc?.invoke(npc)
        return npc
    }

    fun objCreate(id: Int, rotation: Int, x: Int, y: Int, z: Int) {
        spawnObject(GameObject(id, ObjectDefinitions.getDefs(id).types[0], rotation, Tile.of(getX(x), getY(y), z)))
    }

    fun entityMoveTo(entity: Entity, x: Int, y: Int, moveType: MoveType) {
        entityMoveTo(entity, x, y, entity.plane, moveType)
    }

    fun entityMoveTo(entity: Entity, x: Int, y: Int, z: Int, moveType: MoveType) {
        if (moveType == MoveType.TELEPORT) {
            entity.tele(Tile.of(getX(x), getY(y), z))
            return
        }
        entity.run = moveType == MoveType.RUN
        entity.addWalkSteps(getX(x), getY(y), 25, false)
    }

    fun entityWalkTo(entity: Entity, x: Int, y: Int) {
        entityMoveTo(entity, x, y, MoveType.WALK)
    }

    fun entityRunTo(entity: Entity, x: Int, y: Int) {
        entityMoveTo(entity, x, y, MoveType.RUN)
    }

    fun entityTeleTo(entity: Entity, x: Int, y: Int) {
        entityMoveTo(entity, x, y, MoveType.TELEPORT)
    }

    fun spotAnim(id: Int, x: Int, y: Int, z: Int = player.plane) {
        sendSpotAnim(Tile.of(getX(x), getY(y), z), id)
    }

    fun projectile(from: Tile, to: Tile, spotAnim: Int, startHeight: Int, endHeight: Int, startDelayClientCycles: Int, inAirClientCyclesPerTile: Int, angle: Int, task: ((projectile: WorldProjectile) -> Unit)?) {
        sendProjectile(Tile.of(getX(from.x), getY(from.y), from.plane), Tile.of(getX(to.x), getY(to.y), to.plane), spotAnim, startHeight to endHeight, startDelayClientCycles, inAirClientCyclesPerTile, angle, 0, task)
    }

    fun returnPlayerFromInstance() {
        instance?.let { player.tele(it.returnTo) }
    }

    suspend fun dynamicRegion(returnTile: Tile, baseChunkX: Int, baseChunkY: Int, widthChunks: Int, heightChunks: Int, copyNpcs: Boolean = false) {
        constructingRegion = true
        val old = instance
        val instance = Instance.of(returnTile, widthChunks, heightChunks, copyNpcs)
        instance.copyMapAllPlanes(baseChunkX, baseChunkY).thenAccept {
            instance.teleportTo(player)
            constructingRegion = false
            old?.destroy()
        }
        this.instance = instance
        wait { !constructingRegion }
    }

    suspend fun wait(ticks: Int): Unit = suspendCoroutine {
        waitCondition = WaitCondition(TickWait(ticks), it)
    }

    suspend fun wait(condition: () -> Boolean): Unit = suspendCoroutine {
        waitCondition = WaitCondition(ConditionalWait { condition() }, it)
    }

    suspend fun waitForDialogue(): Unit = suspendCoroutine {
        waitCondition = WaitCondition(ConditionalWait { !player.interfaceManager.containsChatBoxInter() }, it)
    }

    fun isWaiting(): Boolean = waitCondition != null
}