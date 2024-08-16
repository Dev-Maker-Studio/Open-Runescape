package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.ITipDetails;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class AnchoredTextMeta implements ITipDetails {

	/**
	 * 
		The northern plains were once lush and green, stippled with farmland and cities. A magic blast - the last act of the God Wars - ruined the north and turned it into the Wilderness that is found there today.
		The vyrewatch are hard beasts to kill. Impervious to normal weapons, they can only be damaged by the Ivandis Flail, or weapons from the Blisterwood Tree.
		A crossbow isn't just for killing. Mithril grappling hooks, made using the Smithing skill, can be used to create a shortcut over obstacles.
		Spears can be tipped with karambwan paste after the Tai Bwo Wannai Trio quest. This poison is one of the strongest weapon poisons.
		Combat spells will often conduct through metal, increasing the damage done to warriors by effectively boiling them in their own armour.
		The God Wars ended centuries ago, but no one has told the generals of Armadyl, Bandos, Saradomin, Zamorak and Zaros. They continue to rage against each other in the thawed God Wars Dungeons.
		The River Salve, blessed by Saradominist priests, is the only barrier between the vampyres of Morytania and the vulnerable 'bloodsacks' of Lumbridge and Varrock.
		Tired of firing volley after volley of arrows? Use alternative ranged weapons like swamp lizards, chinchompas, holy water and javelins.
		Kiteshields are heavier than square shields, but provide a greater resistance, particularly to crush attacks. Some warriors find that a stab attack is the best way past a kiteshield's defences.
		Mages can use a Bake Pie spell from their Lunar Spellbook to create pies. Nine out of ten players prefer the taste!
		Safety can be found in numbers. Whether fighting a boss or taking part in combat minigames, you'll be more successful if you invite others. Add likeminded people to your friends list and send them a private message when you need a hand.
		Vampyre society is based on tiers. The juveniles and juvinates scuttle about on the lower tiers, the vyrewatch form the middle tier and the vyrelords form the top tier. Vampyres dare not talk to those on a higher tier.
		The most powerful bows for rangers include the dark bow, received as a drop from dark beasts, and the Saradomin, Zamorak and Guthix bows, which can be obtained as rewards from elite treasure trails.
		Melee warriors are particularly susceptible to Entangle and other effects that stun or paralyse. Taunting a paralysed warrior is a mage's favourite activity.
		Magic is often the best method to use when fighting demons. For reasons that scholars are yet to discover, demons have a low resistance to magical damage.
		Clan members can use their Clan Battlefield to make combat scenarios and minigames. As long as a clan has permitted you access, you may visit and play these battlefield scenarios.
		Even an inventory full of food will not protect you in Morytania. Ghasts are notorious for rotting anything that smells tasty.
		For additional damage, try applying poison to your arrows, or adding gemstone heads to your crossbow bolts and enchanting them. Enchanted crossbow bolts have a wide variety of effects.
		A melee warrior's blade cuts through ranged and magic armour with ease. For rangers and mages the best defence against a melee warrior is keeping at a distance.
		If you don't have the magic level for a specific teleport spell, consider buying a teleport tablet to that destination. You won't get experience for using one, but it has the same effect.
		Despite appearances, many of the combat minigames within RuneScape are 'safe', meaning you won't lose your items if you die. Get in there and bust some heads!
		Many of the human inhabitants of Morytania are, in fact, werewolves, and will become powerful combatants if attacked. Use a wolfbane dagger to keep them in human form!
		Rangers who have befriended the dwarves of RuneScape will be able to take a hand cannon into battle, or its bigger brother, the dwarf multicannon.
		More aggressive melee warriors will carry a defender, a blade that can be used to parry and block an opponent's attack. These offhand blades can be won in the Warriors' Guild.
		The Telegrab spell allows a mage to gain possession of an item that is otherwise out of reach. Many have tried to Telegrab the ring in Varrock's west bank and failed.
		Safety can be found in numbers. Whether fighting a boss or taking part in combat minigames, you'll be more successful if you invite others. Add likeminded people to your friends list and send them a private message when you need a hand.
		Agile players looking to train their Prayer skill will find a superb, yet slimy, training opportunity at the Ectofuntus temple. Complete the Ghosts Ahoy quest for a handy way to teleport there.
		Rangers rely on animal-skin clothing to protect themselves. They use their Crafting skill to make armour from cow hide, snake skin and dragon hide.
		In a pinch, pickaxes and hatchets can be used as melee weapons. Although they are more suitable for Mining and Woodcutting, their attacks are more potent than using your fists.
		As your Magic level increases, so does your resistance to magic damage. As such, rangers and warriors will find training their Magic skill worthwhile.
	 *
	 */
    public final String tipText;

    public final AnchorX anchorX;

    public final AnchorY anchorY;

    public final int offsetX;

    public final int offsetY;

    public final int textAllignment;

    public final int textVerticalAllignment;

    public final int lineSpacing;

    public final int width;

    public final int height;

    /**
     * The sprite & font id
     */
    public final int fileId;

    public final int textColor;

    public final int textShadowColor;

    public AnchoredTextMeta(String tipText, AnchorX archorX, AnchorY anchorY, int offsetX, int offsetY, int textAllignment,
                            int textVerticalAllignment, int lineSpacing, int width, int height, int fileId, int textColor, int textShadowColor) {
        this.tipText = tipText;
        this.anchorX = archorX;
        this.anchorY = anchorY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.textAllignment = textAllignment;
        this.textVerticalAllignment = textVerticalAllignment;
        this.lineSpacing = lineSpacing;
        this.width = width;
        this.height = height;
        this.fileId = fileId;
        this.textColor = textColor;
        this.textShadowColor = textShadowColor;
    }

    public static AnchoredTextMeta decode(Packet buffer) {
        String tipText = buffer.readString();
        AnchorX anchorX = AnchorX.values()[buffer.readUnsignedByte()];
        AnchorY archorY = AnchorY.values()[buffer.readUnsignedByte()];
        int offsetX = buffer.readShort();
        int offsetY = buffer.readShort();
        int textAllignment = buffer.readUnsignedByte();
        int textVerticalAllignment = buffer.readUnsignedByte();
        int lineSpacing = buffer.readUnsignedByte();
        int width = buffer.readUnsignedShort();
        int height = buffer.readUnsignedShort();
        int fileId = buffer.readBigSmart();
        int textColor = buffer.readInt();
        int textShadowColor = buffer.readInt();
        return new AnchoredTextMeta(tipText, anchorX, archorY, offsetX, offsetY, textAllignment,
                textVerticalAllignment, lineSpacing, width, height, fileId, textColor, textShadowColor);
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.ANCHORED_TEXT;
    }
}
