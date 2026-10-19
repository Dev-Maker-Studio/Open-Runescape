package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.NodeCollection

/**
 * Represents a delta update for clan settings, containing a queue of pending updates.
 */
class ClanSettingsDelta(buffer: Packet) {

    @JvmField var deltaVersion: Long = 0
    @JvmField var updateCount: Int = -1
    @JvmField val pendingUpdates = NodeCollection()

    init {
        decode(buffer)
    }

    /**
     * Decodes clan settings delta from the packet buffer.
     *
     * @param buffer The packet buffer to decode from
     */
    fun decode(buffer: Packet) {
        deltaVersion = buffer.readLong()
        updateCount = buffer.readInt()
        var opcode = buffer.readUByte()
        while (opcode != 0) {
            val delta: AbstractClanSettingUpdate = when (opcode) {
                3 -> BanUser(this)
                1 -> AddNewMember(this)
                13 -> AddExistingMember(this)
                4 -> UpdatePermission(this)
                6 -> UnbanUser(this)
                5 -> RemoveMember(this)
                2 -> UpdateRank(this)
                7 -> UpdateClanFlag(this)
                8 -> UpdateIntVariable(this)
                9 -> UpdateLongVariable(this)
                10 -> UpdateStringVariable(this)
                11 -> UpdateBitpackedVariable(this)
                12 -> UpdateClanName(this)
                else -> throw RuntimeException("")
            }
            delta.decode(buffer)
            pendingUpdates.addFirst(delta)
            opcode = buffer.readUByte()
        }
    }

    /**
     * Queues and applies all pending updates to the clan settings.
     *
     * @param clanSettings The clan settings to update
     * @throws RuntimeException if the delta version doesn't match the clan settings
     */
    fun queueUpdates(clanSettings: ClanSettings) {
        if (clanSettings.deltaVersion == deltaVersion && updateCount == clanSettings.updateCount) {
            var update = pendingUpdates.getFirst() as AbstractClanSettingUpdate?
            while (update != null) {
                update.apply(clanSettings)
                update = pendingUpdates.next() as AbstractClanSettingUpdate?
            }
            clanSettings.updateCount++
        } else {
            throw RuntimeException("")
        }
    }
}
