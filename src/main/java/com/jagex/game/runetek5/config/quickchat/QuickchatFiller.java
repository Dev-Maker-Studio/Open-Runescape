package com.jagex.game.runetek5.config.quickchat;

import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.enumtype.EnumType;
import com.jagex.game.runetek5.config.item.ItemType;
import static com.jagex.game.runetek5.config.quickchat.QuickChatValueType.*;

/**
 * 
 * @author Brainpower - 22/04/2022
 *
 */
public class QuickchatFiller implements QuickchatFillerList {

    @Override
    public String getValueDisplay(QuickChatValueType value, int[] configs, long data) {
        if (value == LIST_DIALOG) {
            EnumType enumType = TypeLists.ENUM.list(configs[0]);
            return enumType.getStringValue((int) data);
        }
        if (value == OBJ_DIALOG || value == OBJ_TRADE_DIALOG) {
            ItemType objType = TypeLists.OBJ.list((int) data);
            return objType.name;
        }
        if (value == ENUM_STRING || value == ENUM_STRING_CLAN || value == ENUM_STRING_STATBASE) {
            return TypeLists.ENUM.list(configs[0]).getStringValue((int) data);
        }
        return null;
    }
}