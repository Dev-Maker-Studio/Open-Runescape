package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.NodeCollection;

public class ClanSettingsDelta {

    public long aLong4073;

    public int anInt4060 = -1;

    public final NodeCollection pendingUpdates = new NodeCollection();

    public ClanSettingsDelta(Packet buffer) {
        decode(buffer);
    }

    public void decode(Packet buffer) {
        this.aLong4073 = buffer.readLong();
        this.anInt4060 = buffer.readInt();
        for (int opcode = buffer.readUByte(); opcode != 0; opcode = buffer.readUByte()) {
        	AbstractClanSettingUpdate delta;
            if (opcode == 3) {
                delta = new BanUser(this);
            } else if (opcode == 1) {
                delta = new AddNewMember(this);
            } else if (opcode == 13) {
                delta = new AddExistingMember(this);
            } else if (opcode == 4) {
                delta = new UpdatePermission(this);
            } else if (opcode == 6) {
                delta = new UnbanUser(this);
            } else if (opcode == 5) {
                delta = new RemoveMember(this);
            } else if (opcode == 2) {
                delta = new UpdateRank(this);
            } else if (opcode == 7) {
                delta = new UpdateClanFlag(this);
            } else if (opcode == 8) {
                delta = new UpdateIntVariable(this);
            } else if (opcode == 9) {
                delta = new UpdateLongVariable(this);
            } else if (opcode == 10) {
                delta = new UpdateStringVariable(this);
            } else if (opcode == 11) {
                delta = new UpdateBitpackedVariable(this);
            } else if (opcode == 12) {
            	delta = new UpdateClanName(this);
            } else {
            	throw new RuntimeException("");
            }
            delta.decode(buffer);
            this.pendingUpdates.addFirst(delta);
        }
    }

    public void queueUpdates(ClanSettings clanSettings) {
        if (clanSettings.aLong631 == this.aLong4073 && this.anInt4060 == clanSettings.updateCount) {
            for (AbstractClanSettingUpdate update = (AbstractClanSettingUpdate) this.pendingUpdates.getFirst(); update != null; update = (AbstractClanSettingUpdate) this.pendingUpdates.next()) {
                update.apply(clanSettings);
            }
            clanSettings.updateCount++;
        } else {
            throw new RuntimeException("");
        }
    }
}
