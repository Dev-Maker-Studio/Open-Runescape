package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class UpdateClanFlag extends AbstractClanSettingUpdate {

    public int index;
    public int flag;
    public int shift;
    public int bitmaskSize;
    public final ClanSettingsDelta clanSettingsDelta;

    public UpdateClanFlag(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
        this.index = -1;
    }

    @Override
    public void decode(Packet buffer) {
    	this.index = buffer.readUShort();
    	this.flag = buffer.readInt();
    	this.shift = buffer.readUByte();
    	this.bitmaskSize = buffer.readUByte();
    }

    @Override
    public void apply(ClanSettings class611) {
        class611.updateClanFlag(this.index, this.flag, this.shift, this.bitmaskSize);
    }
}
