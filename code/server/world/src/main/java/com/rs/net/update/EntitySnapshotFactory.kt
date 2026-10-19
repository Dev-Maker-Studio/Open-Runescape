package com.rs.net.update

import com.rs.Settings
import com.rs.game.World
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.HitBar
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.utils.crawls
import org.darkan.core.Utils
import org.darkan.core.model.MoveType
import org.darkan.core.net.prot.update.NpcUpdateNpc
import org.darkan.core.net.prot.update.NpcUpdateSnapshot
import org.darkan.core.net.prot.update.PlayerUpdatePlayer
import org.darkan.core.net.prot.update.PlayerUpdateSnapshot
import org.darkan.core.update.CommonUpdateBlocks
import org.darkan.core.update.FaceTile
import org.darkan.core.update.ForceMovement
import org.darkan.core.update.HitBarUpdate
import org.darkan.core.update.HitSplat
import org.darkan.core.update.HitsUpdate
import org.darkan.core.update.MovementAnimations
import org.darkan.core.update.NpcCustomisation
import org.darkan.core.update.NpcUpdateBlocks
import org.darkan.core.update.PlayerAppearance
import org.darkan.core.update.PlayerUpdateBlocks
import org.darkan.core.update.Tint
import org.darkan.core.update.SpotAnim as CoreSpotAnim
import org.darkan.core.model.SpotAnim as LegacySpotAnim

/**
 * Converts legacy world-side entities (NPCs and Players) into core-module snapshot data models
 * ([NpcUpdateSnapshot] / [NpcUpdateNpc] / [NpcUpdateBlocks] and [PlayerUpdateSnapshot] /
 * [PlayerUpdatePlayer] / [PlayerUpdateBlocks]).
 *
 * This factory bridges the world module's [NPC] and [Player] entity types to the semantic update
 * types that the core's `Rev727UpdateProtocol` consumes. All wire encoding lives in core; this
 * module only supplies semantic state.
 */
object EntitySnapshotFactory {

    /**
     * Builds a complete [NpcUpdateSnapshot] for the given observer [Player].
     *
     * Queries all nearby NPCs within the observer's view radius (126 for large scene, 14 otherwise)
     * and produces an [NpcUpdateNpc] for each, including their current update blocks.
     */
    fun buildNpcSnapshot(observer: Player): NpcUpdateSnapshot {
        val largeScene = observer.hasLargeSceneView()
        val radius = if (largeScene) 126 else 14

        val npcs = observer.queryNearbyNPCsByTileRange(radius) { npc ->
            npc.withinDistance(observer, radius)
        }

        val npcSnapshots = ArrayList<NpcUpdateNpc>(minOf(npcs.size, Settings.LOCAL_NPCS_LIMIT))
        for (n in npcs) {
            if (npcSnapshots.size >= Settings.LOCAL_NPCS_LIMIT) break
            npcSnapshots.add(buildNpcUpdateNpc(n, observer, added = false))
        }

        return NpcUpdateSnapshot(
            largeSceneView = largeScene,
            localTileHash30 = observer.tileHash,
            nearbyNpcs = npcSnapshots,
        )
    }

    /**
     * Builds a single [NpcUpdateNpc] snapshot for one NPC as seen by the given [observer].
     *
     * @param n The NPC entity.
     * @param observer The observing player (needed for viewer-dependent hit resolution).
     * @param added Whether this NPC is being newly added to the observer's local list this tick.
     */
    fun buildNpcUpdateNpc(n: NPC, observer: Player, added: Boolean): NpcUpdateNpc {
        return NpcUpdateNpc(
            index = n.index,
            id = n.id,
            plane = n.plane,
            x = n.x,
            y = n.y,
            size = n.size,
            finished = n.hasFinished(),
            hasTeleported = n.hasTeleported(),
            nextWalkDirection = n.nextWalkDirection?.id,
            nextRunDirection = n.nextRunDirection?.id,
            movementType = when {
                n.nextRunDirection != null -> MoveType.RUN
                n.definitions.crawls -> MoveType.CRAWL
                else -> MoveType.WALK
            },
            faceAngle = n.faceAngle,
            blocks = buildNpcBlocks(n, observer, added),
        )
    }

    /**
     * Builds the [NpcUpdateBlocks] for an NPC, including all active mask data.
     *
     * Each legacy entity field read is translated into the corresponding semantic model field;
     * the core mask registry decides presence and on-wire ordering.
     *
     * The `added` parameter is no longer needed because the persistent add-only state
     * (face entity, custom name, custom combat level) is captured in dedicated
     * `persistent*` fields and selected by the protocol/encoder at encode time.
     */
    fun buildNpcBlocks(n: NPC, observer: Player, added: Boolean): NpcUpdateBlocks {
        val spotAnims = arrayOfNulls<CoreSpotAnim>(4)
        spotAnims[0] = convertSpotAnim(n.nextSpotAnim1)
        spotAnims[1] = convertSpotAnim(n.nextSpotAnim2)
        spotAnims[2] = convertSpotAnim(n.nextSpotAnim3)
        spotAnims[3] = convertSpotAnim(n.nextSpotAnim4)

        // Face entity: per-tick explicit set (`nextFaceEntity != -2`).
        // Persistent face target (`lastFaceEntity`) is captured separately for the add path.
        val faceEntity: Int? = if (n.nextFaceEntity != -2) n.nextFaceEntity else null
        val persistentFaceEntity: Int? = if (n.lastFaceEntity != -1) n.lastFaceEntity else null

        // Face tile: triggered when fixedFaceTile is set, or nextFaceTile is set while not walking.
        // Store raw tile coordinates — the encoder applies the `(x shl 1) + 1` transform.
        val faceTile: FaceTile? = when {
            n.fixedFaceTile != null -> {
                val tile = n.fixedFaceTile
                FaceTile(tile.x, tile.y)
            }
            n.nextFaceTile != null && n.nextRunDirection == null && n.nextWalkDirection == null -> {
                val tile = n.nextFaceTile
                FaceTile(tile.x, tile.y)
            }
            else -> null
        }

        // Body glow / tint
        val tint: Tint? = n.nextBodyGlow?.let { glow ->
            Tint(
                hue = glow.redAdd,
                saturation = glow.greenAdd,
                lightness = glow.blueAdd,
                opacity = glow.scalar,
                startCycles = 0,
                endCycles = glow.time,
            )
        }

        // Hits (viewer-dependent)
        val hits: HitsUpdate? = resolveNpcHits(observer, n)

        // Model overrides from ModelRotator
        val modelOverrides = n.bodyModelRotator?.toModelOverrides()

        val common = CommonUpdateBlocks(
            tint = tint,
            spotAnims = spotAnims,
            forceMovement = n.nextForceMovement?.let { fm ->
                ForceMovement(
                    diffX1 = fm.diffX1,
                    diffY1 = fm.diffY1,
                    diffX2 = fm.diffX2,
                    diffY2 = fm.diffY2,
                    startClientCycles = fm.startClientCycles,
                    speedClientCycles = fm.speedClientCycles,
                    direction = fm.direction,
                )
            },
            faceEntity = faceEntity,
            persistentFaceEntity = persistentFaceEntity,
            faceTile = faceTile,
            hits = hits,
            modelOverrides = modelOverrides,
        )

        // Movement animations (BAS animation set)
        val movementAnimations: MovementAnimations? = n.nextAnimation?.let { anim ->
            MovementAnimations(
                ids = anim.ids,
                speed = anim.speed,
            )
        }

        // NPC body mesh customisation
        val customisation: NpcCustomisation? = n.bodyMeshModifier?.toCustomisation()

        // BAS id
        val basId: Int? = if (n.bas != -1) {
            if (n.bas == -2) -1 else n.bas
        } else null

        // Transform NPC id
        val transformNpcId: Int? = n.nextTransformation?.toNPCId()

        // Combat level: changed this tick is captured in [combatLevel];
        // a non-default custom level is captured in [persistentCombatLevel] for the add path.
        val combatLevel: Int? = if (n.hasChangedCombatLevel()) n.combatLevel else null
        val persistentCombatLevel: Int? = if (n.customCombatLevel >= 0) n.combatLevel else null

        // Name: changed this tick is captured in [name];
        // a non-default custom name is captured in [persistentName] for the add path.
        val name: String? = if (n.hasChangedName()) n.name else null
        val persistentName: String? = if (n.customName != null) n.name else null

        // Force talk
        val say: String? = n.nextForceTalk?.text()

        return NpcUpdateBlocks(
            common = common,
            movementAnimations = movementAnimations,
            customisation = customisation,
            basId = basId,
            transformNpcId = transformNpcId,
            combatLevel = combatLevel,
            persistentCombatLevel = persistentCombatLevel,
            name = name,
            persistentName = persistentName,
            say = say,
        )
    }

    /**
     * Resolves hit splats and hit bars for an NPC as seen by a specific [observer].
     *
     * Hit rendering is viewer-dependent: hits from/to the observer show detailed marks,
     * while unrelated misses are encoded as 32766 (client miss marker). Soak hits and
     * custom text hitsplats are handled as distinct splat types.
     *
     * @return A [HitsUpdate] if there are any hits or bars to send, null otherwise.
     */
    fun resolveNpcHits(observer: Player, n: NPC): HitsUpdate? {
        if (n.nextHits.isEmpty() && n.nextHitBars.isEmpty()) return null

        val splats = Array<HitSplat>(n.nextHits.size) { i ->
            val hit = n.nextHits[i]
            resolveHitSplat(hit, observer, n)
        }

        val bars = Array<HitBarUpdate>(n.nextHitBars.size) { i ->
            val bar = n.nextHitBars[i]
            resolveHitBar(bar, observer)
        }

        return HitsUpdate(
            hitSplats = splats,
            hitBars = bars,
        )
    }

    /**
     * Resolves a single [Hit] into a [HitSplat] from the perspective of [observer].
     *
     * The visibility/soak rules are:
     * - Non-interacting misses with no text -> [HitSplat.Miss] (encoded as 32766)
     * - Soaking hits with text or large damage -> [HitSplat.CustomText] with soak text
     * - Soaking hits with small damage -> [HitSplat.Soak] with numeric soak
     * - Non-soak hits with text or large damage -> [HitSplat.CustomText]
     * - Standard numeric hits -> [HitSplat.Standard]
     *
     * @param target The entity being hit (NPC or Player). [Hit.interactingWith] and
     *               [Hit.getMark] both accept [Entity], so this is shared for both types.
     */
    private fun resolveHitSplat(hit: Hit, observer: Player, target: Entity): HitSplat {
        val interactingWith = hit.interactingWith(observer, target)

        // Non-interacting miss with no custom text: encode as miss marker
        if (hit.text == null && hit.missed() && !interactingWith) {
            return HitSplat.Miss(
                missValue = 0,
                delay = hit.delay,
            )
        }

        val soaking = hit.soaking

        if (soaking != null) {
            // Has soak component
            val hasText = hit.text != null
            val bigDamage = hit.damage > Short.MAX_VALUE / 2
            val bigSoakDamage = soaking.damage > Short.MAX_VALUE / 2

            if (hasText || bigDamage || bigSoakDamage) {
                // Custom text encoding for large/text hits with soak
                return HitSplat.CustomText(
                    id = hit.getMark(observer, target),
                    text = hit.text ?: Utils.formatNumber(hit.damage),
                    soakId = soaking.getMark(observer, target),
                    soakText = Utils.formatNumber(soaking.damage),
                    delay = hit.delay,
                )
            } else {
                // Numeric soak encoding
                return HitSplat.Soak(
                    id = hit.getMark(observer, target),
                    value = hit.damage,
                    soakId = soaking.getMark(observer, target),
                    soakValue = soaking.damage,
                    delay = hit.delay,
                )
            }
        }

        // No soak
        val hasText = hit.text != null
        val bigDamage = hit.damage > Short.MAX_VALUE / 2

        if (hasText || bigDamage) {
            // Custom text encoding for large/text hits
            return HitSplat.CustomText(
                id = hit.getMark(observer, target),
                text = hit.text ?: Utils.formatNumber(hit.damage),
                delay = hit.delay,
            )
        }

        // Standard numeric hit
        return HitSplat.Standard(
            id = hit.getMark(observer, target),
            value = hit.damage,
            delay = hit.delay,
        )
    }

    /**
     * Resolves a single [HitBar] into a [HitBarUpdate] from the perspective of [observer].
     *
     * If the bar should not be displayed for this observer, it is encoded with timer=32767 (hide).
     * Otherwise, the bar's percentage, optional to-percentage, delay, and timer are captured.
     */
    private fun resolveHitBar(bar: HitBar, observer: Player): HitBarUpdate {
        val perc = bar.percentage
        val toPerc = bar.toPercentage
        val display = bar.display(observer)

        if (!display) {
            return HitBarUpdate(
                type = bar.type,
                timer = 32767,
            )
        }

        val timer = if (perc != toPerc) bar.timer else 0

        return HitBarUpdate(
            type = bar.type,
            timer = timer,
            delay = bar.delay,
            fromPercentage = perc,
            toPercentage = toPerc,
        )
    }

    // =====================================================================
    //  Player snapshot building
    // =====================================================================

    /**
     * Builds a complete [PlayerUpdateSnapshot] for the given [observer].
     *
     * Pre-computes a [PlayerUpdatePlayer] for every potential player index (1..2047),
     * including the observer themselves. The core protocol consumes this array to
     * decide which players are local/outside and which masks to send.
     */
    fun buildPlayerSnapshot(observer: Player): PlayerUpdateSnapshot {
        val playersByIndex = arrayOfNulls<PlayerUpdatePlayer>(2048)
        for (index in 1 until 2048) {
            val p = World.players[index] ?: continue
            if (p.hasStarted() && !p.hasFinished()) {
                playersByIndex[index] = buildPlayerUpdatePlayer(p, observer)
            }
        }
        return PlayerUpdateSnapshot(
            localIndex = observer.index,
            largeSceneView = observer.hasLargeSceneView(),
            localTileHash30 = observer.tileHash,
            playersByIndex = playersByIndex,
        )
    }

    /**
     * Builds a single [PlayerUpdatePlayer] snapshot for one player.
     *
     * Pre-computes the walk/run opcodes from the legacy [Direction]-based movement
     * into the integer opcodes the core protocol expects:
     * - Walking: [nextWalkDirection] = 3-bit walk opcode, [nextRunDirectionOrOpcode] = null
     * - Running: [nextWalkDirection] = 3-bit walk opcode (unused on-wire), [nextRunDirectionOrOpcode] = 4-bit run opcode
     * - Effective zero displacement (walk + run cancel out): treated as no movement
     * - Standing still: both null
     */
    fun buildPlayerUpdatePlayer(p: Player, observer: Player): PlayerUpdatePlayer {
        var walkOpcode: Int? = null
        var runOpcode: Int? = null
        var zeroDisplacementWalk = false

        val walkDir = p.nextWalkDirection
        if (walkDir != null) {
            val runDir = p.nextRunDirection
            if (runDir != null) {
                // Running: combine walk + run deltas
                val totalDx = walkDir.dx + runDir.dx
                val totalDy = walkDir.dy + runDir.dy

                if (totalDx == 0 && totalDy == 0) {
                    // Walk + run cancel out — effective zero displacement.
                    // Legacy treats this as a masks-only update (no movement).
                    walkOpcode = null
                    runOpcode = null
                    zeroDisplacementWalk = true
                } else {
                    val runOp = Utils.getPlayerRunningDirection(totalDx, totalDy)
                    if (runOp != -1) {
                        // Valid run opcode
                        walkOpcode = Utils.getPlayerWalkingDirection(walkDir.dx, walkDir.dy)
                        runOpcode = runOp
                    } else {
                        // Run direction not valid as a 4-bit opcode; fall back to walk
                        walkOpcode = Utils.getPlayerWalkingDirection(totalDx, totalDy)
                        runOpcode = null
                    }
                }
            } else {
                // Walking only
                val dx = walkDir.dx
                val dy = walkDir.dy
                if (dx == 0 && dy == 0) {
                    // Zero-displacement walk — masks-only update
                    walkOpcode = null
                    runOpcode = null
                    zeroDisplacementWalk = true
                } else {
                    walkOpcode = Utils.getPlayerWalkingDirection(dx, dy)
                    runOpcode = null
                }
            }
        }

        return PlayerUpdatePlayer(
            index = p.index,
            tileHash30 = p.tileHash,
            lastTileHash30 = p.lastTile?.getTileHash() ?: p.tileHash,
            regionHash18 = p.regionHash,
            xInRegion = p.xInRegion,
            yInRegion = p.yInRegion,
            finished = p.hasFinished(),
            hidden = p.isTrulyHidden,
            hasTeleported = p.hasTeleported(),
            nextWalkDirection = walkOpcode,
            nextRunDirectionOrOpcode = runOpcode,
            blocks = buildPlayerBlocks(p, observer, added = false),
            zeroDisplacementWalk = zeroDisplacementWalk,
        )
    }

    /**
     * Builds the [PlayerUpdateBlocks] for a player, including all active mask data.
     *
     * Each legacy entity field read is translated into the corresponding semantic model field;
     * the core mask registry decides presence and on-wire ordering.
     *
     * Note: The [added] flag affects face direction and movement type conditions.
     * The snapshot is initially built with `added = false`; the core protocol can
     * rebuild blocks with `added = true` when a player is first added to the
     * observer's local list by calling this method directly.
     */
    fun buildPlayerBlocks(p: Player, observer: Player, added: Boolean): PlayerUpdateBlocks {
        val spotAnims = arrayOfNulls<CoreSpotAnim>(4)
        spotAnims[0] = convertSpotAnim(p.nextSpotAnim1)
        spotAnims[1] = convertSpotAnim(p.nextSpotAnim2)
        spotAnims[2] = convertSpotAnim(p.nextSpotAnim3)
        spotAnims[3] = convertSpotAnim(p.nextSpotAnim4)

        // Face entity: per-tick explicit set (`nextFaceEntity != -2`).
        // Persistent face target (`lastFaceEntity`) is captured separately for the add path.
        val faceEntity: Int? = if (p.nextFaceEntity != -2) p.nextFaceEntity else null
        val persistentFaceEntity: Int? = if (p.lastFaceEntity != -1) p.lastFaceEntity else null

        // Face direction (angle): explicit value when fixedFaceTile is set,
        // or nextFaceTile is set while not walking/running.
        // The persistent angle is captured separately for the add path so newly-visible
        // players are oriented correctly.
        val faceDirection: Int? = when {
            p.fixedFaceTile != null -> p.faceAngle
            p.nextFaceTile != null && p.nextRunDirection == null && p.nextWalkDirection == null -> p.faceAngle
            else -> null
        }
        val persistentFaceDirection: Int? = p.faceAngle

        // Body glow / tint
        val tint: Tint? = p.nextBodyGlow?.let { glow ->
            Tint(
                hue = glow.redAdd,
                saturation = glow.greenAdd,
                lightness = glow.blueAdd,
                opacity = glow.scalar,
                startCycles = 0,
                endCycles = glow.time,
            )
        }

        // Hits (viewer-dependent)
        val hits: HitsUpdate? = resolvePlayerHits(observer, p)

        // Model overrides from ModelRotator
        val modelOverrides = p.bodyModelRotator?.toModelOverrides()

        val common = CommonUpdateBlocks(
            tint = tint,
            spotAnims = spotAnims,
            forceMovement = p.nextForceMovement?.let { fm ->
                ForceMovement(
                    diffX1 = fm.diffX1,
                    diffY1 = fm.diffY1,
                    diffX2 = fm.diffX2,
                    diffY2 = fm.diffY2,
                    startClientCycles = fm.startClientCycles,
                    speedClientCycles = fm.speedClientCycles,
                    direction = fm.direction,
                )
            },
            faceEntity = faceEntity,
            persistentFaceEntity = persistentFaceEntity,
            hits = hits,
            modelOverrides = modelOverrides,
        )

        // Movement animations (anim set)
        val movementAnimations: MovementAnimations? = p.nextAnimation?.let { anim ->
            MovementAnimations(
                ids = anim.ids,
                speed = anim.speed,
            )
        }

        // Appearance: always provide if data exists; the core presence logic
        // handles per-observer caching via PlayerUpdateTrackingState.needAppearanceUpdate().
        val appearance: PlayerAppearance? = p.appearance.appeareanceData?.let { data ->
            val hash = p.appearance.mD5AppeareanceDataHash ?: return@let null
            PlayerAppearance(
                hash = hash,
                data = data,
            )
        }

        // Force talk
        val forceTalk: String? = p.nextForceTalk?.text()

        // Movement type: explicit change this tick is captured in [movementTypeId];
        // the persistent value is always populated for the add path so newly-visible
        // players have the correct walk/run state immediately.
        val movementTypeIdValue = if (p.movementType == MoveType.RUN) 2 else 1
        val movementTypeId: Int? = if (p.isUpdateMovementType) movementTypeIdValue else null
        val persistentMovementTypeId: Int = movementTypeIdValue

        // Temporary move type
        val temporaryMoveTypeId: Int? = p.temporaryMoveType?.id

        return PlayerUpdateBlocks(
            common = common,
            appearance = appearance,
            movementAnimations = movementAnimations,
            faceDirection = faceDirection,
            persistentFaceDirection = persistentFaceDirection,
            movementTypeId = movementTypeId,
            persistentMovementTypeId = persistentMovementTypeId,
            temporaryMoveTypeId = temporaryMoveTypeId,
            forceTalk = forceTalk,
            publicChat = null, // Public chat is not sent via player update masks in the legacy system
        )
    }

    /**
     * Resolves hit splats and hit bars for a [target] player as seen by a specific [observer].
     *
     * Hit rendering is viewer-dependent: hits from/to the observer show detailed marks,
     * while unrelated misses are encoded as 32766 (client miss marker). Soak hits and
     * custom text hitsplats are handled as distinct splat types.
     *
     * The hit resolution logic is identical to [resolveNpcHits] — both entity types
     * use the same [Hit.interactingWith] / [Hit.getMark] APIs.
     *
     * @return A [HitsUpdate] if there are any hits or bars to send, null otherwise.
     */
    fun resolvePlayerHits(observer: Player, target: Player): HitsUpdate? {
        if (target.nextHits.isEmpty() && target.nextHitBars.isEmpty()) return null

        val splats = Array<HitSplat>(target.nextHits.size) { i ->
            val hit = target.nextHits[i]
            resolveHitSplat(hit, observer, target)
        }

        val bars = Array<HitBarUpdate>(target.nextHitBars.size) { i ->
            val bar = target.nextHitBars[i]
            resolveHitBar(bar, observer)
        }

        return HitsUpdate(
            hitSplats = splats,
            hitBars = bars,
        )
    }

    /**
     * Converts a legacy world [LegacySpotAnim] to a core update [CoreSpotAnim].
     *
     * The legacy SpotAnim packs height/speed into `settingsHash` (height << 16 | speed & 0xFFFF)
     * and rotation/adjustReplayMode into `settings2Hash` (rotation & 0x7, adjustReplay = bit 7).
     */
    fun convertSpotAnim(spotAnim: LegacySpotAnim?): CoreSpotAnim? {
        if (spotAnim == null) return null
        return CoreSpotAnim(
            id = spotAnim.id,
            height = spotAnim.settingsHash shr 16,
            speed = spotAnim.settingsHash and 0xFFFF,
            rotation = spotAnim.settings2Hash and 0x7,
            adjustReplayMode = (spotAnim.settings2Hash and 0x80) != 0,
        )
    }
}
