package com.jagex.game.runetek5.interaction.chat;

import com.jagex.game.runetek5.client.client;

/**
 * [J]
 * @author Brainpower - 03/02/2023
 *
 */
public class ChatLine {

    public int identifier = ChatBox.getChatlineIdentifier();
    public int time;
    public int type;
    public int effectFlags;
    public String crownedName;
    public String name;
    public String simpleName;
    public String clan;
    public int quickchatMessageId;
    public String message;
    
	public ChatLine(int type, int effectFlags, String crownedName, String name, String nameSimple, String clan, int quickchatMessageId, String message) {
    	this.time = client.cycles;
        this.type = type;
        this.effectFlags = effectFlags;
        this.crownedName = crownedName;
        this.name = name;
        this.simpleName = nameSimple;
        this.clan = clan;
        this.quickchatMessageId = quickchatMessageId;
        this.message = message;
    }

    public void set(int type, int effectFlags, String crownedName, String name, String simpleName, String clan, int quickchatMessageId, String message) {
    	this.identifier = ChatBox.getChatlineIdentifier();
        this.time = client.cycles;
        this.type = type;
        this.effectFlags = effectFlags;
        this.crownedName = crownedName;
        this.name = name;
        this.simpleName = simpleName;
        this.clan = clan;
        this.quickchatMessageId = quickchatMessageId;
        this.message = message;
    }
}
