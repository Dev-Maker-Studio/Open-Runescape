package com.jagex.game.runetek5.network.packet;

import com.jagex.game.runetek5.buffer.Packet;

public class GrandExchangeSlot {

    public int itemId;
    public int price;
    public int amount;
    public int currentAmount;
    public int totalPrice;
    public byte progress;

    public GrandExchangeSlot() {
    }

    public GrandExchangeSlot(Packet buffer) {
        this.progress = buffer.readByte();
        this.itemId = buffer.readUShort();
        this.price = buffer.readInt();
        this.amount = buffer.readInt();
        this.currentAmount = buffer.readInt();
        this.totalPrice = buffer.readInt();
    }

    public int method5908() {
        return this.progress & 0x7;
    }

    public int method5909() {
        return (this.progress & 0x8) == 8 ? 1 : 0;
    }
}
