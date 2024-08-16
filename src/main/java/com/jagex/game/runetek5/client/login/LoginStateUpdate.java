package com.jagex.game.runetek5.client.login;

import com.jagex.game.runetek5.entity.EntityNode;
import com.jagex.game.runetek5.utils.Utils;

public class LoginStateUpdate extends EntityNode {

	public final int time = (int) (Utils.time() / 1000L);
    public final String username;
    
    /**
     * logout =  0
     * login = > 0
     */
    public final short actionType;

    public LoginStateUpdate(String username, int actionType) {
        this.username = username;
        this.actionType = (short) actionType;
    }
}
