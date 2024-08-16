package com.jagex.game.runetek5.interaction.chat.clanchat.channel;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.utils.ArrayUtils;

public class ClanChannel extends Node<ClanChannel> {

    public int numPlayers;
    public String clanName;
    public ClanChannelPlayer[] players;
    public byte minRankToKick;
    public byte guestsTalk;
    public boolean namesAsString = true;
    public int[] sortedNameIndices;
    public boolean namesAsLong;
    public long nextUpdateNumber;

    public ClanChannel(Packet buffer) {
        decode(buffer);
    }
    
    public void decode(Packet buffer) {
        int nameTypeFlag = buffer.readUnsignedByte();
        if ((nameTypeFlag & 0x1) != 0) {
            this.namesAsLong = true;
        }
        if ((nameTypeFlag & 0x2) != 0) {
            this.namesAsString = true;
        }
        super.pointer = buffer.readLong();
        this.nextUpdateNumber = buffer.readLong();
        this.clanName = buffer.readString();
        buffer.readUnsignedByte();
        this.minRankToKick = buffer.readByte();
        this.guestsTalk = buffer.readByte();
        this.numPlayers = buffer.readUnsignedShort();
        if (this.numPlayers > 0) {
            this.players = new ClanChannelPlayer[this.numPlayers];
            for (int index = 0; index < this.numPlayers; index++) {
                ClanChannelPlayer player = new ClanChannelPlayer();
                if (this.namesAsLong) {
                    buffer.readLong();
                }
                if (this.namesAsString) {
                    player.name = buffer.readString();
                }
                player.rank = buffer.readByte();
                player.world = buffer.readUnsignedShort();
                this.players[index] = player;
            }
        }
    }

    public int[] getPlayersSortedByName() {
        if (this.sortedNameIndices == null) {
            String[] playerNames = new String[this.numPlayers];
            this.sortedNameIndices = new int[this.numPlayers];
            for (int i = 0; i < this.numPlayers; this.sortedNameIndices[i] = i++) {
                playerNames[i] = this.players[i].name;
            }
            ArrayUtils.quicksortt(playerNames, this.sortedNameIndices);
        }
        return this.sortedNameIndices;
    }

    public void removePlayer(int playerIndex) {
        if (--this.numPlayers == 0) {
            this.players = null;
        } else {
            ArrayUtils.arrayCopy(this.players, playerIndex + 1, this.players, playerIndex, this.numPlayers - playerIndex);
        }
        this.sortedNameIndices = null;
    }

    public int getUserSlot(String playerName) {
        for (int index = 0; index < this.numPlayers; index++) {
            if (this.players[index].name.equalsIgnoreCase(playerName)) {
                return index;
            }
        }
        return -1;
    }

    public void addPlayer(ClanChannelPlayer player) {
        if (this.players == null || this.numPlayers >= this.players.length) {
            expand(this.numPlayers + 5);
        }
        this.players[++this.numPlayers - 1] = player;
        this.sortedNameIndices = null;
    }

    public void expand(int newSize) {
        if (this.players != null) {
            ArrayUtils.arrayCopy(this.players, 0, this.players = new ClanChannelPlayer[newSize], 0, this.numPlayers);
        } else {
            this.players = new ClanChannelPlayer[newSize];
        }
    }
}
