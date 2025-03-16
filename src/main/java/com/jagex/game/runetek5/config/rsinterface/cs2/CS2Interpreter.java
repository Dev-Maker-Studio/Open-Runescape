package com.jagex.game.runetek5.config.rsinterface.cs2;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.cache.FileOnDisk;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.cache.node.QueueIterator;
import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.console.CommandExecutor;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.client.login.AccountCreationResponseOpcodes;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.CreateAccountRequest;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuEntry;
import com.jagex.game.runetek5.client.periodic.PulseEventQueue;
import com.jagex.game.runetek5.client.window.DisplayModeDetails;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.clanvar.ClanVarType;
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList;
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsType;
import com.jagex.game.runetek5.config.enumtype.EnumType;
import com.jagex.game.runetek5.config.material.texture.ColorImageCache;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.quest.QuestType;
import com.jagex.game.runetek5.config.quickchat.QuickChatCategoryType;
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage;
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageType;
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentType;
import com.jagex.game.runetek5.config.rsinterface.HookRequest;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerType;
import com.jagex.game.runetek5.config.sequence.InterfaceSequence;
import com.jagex.game.runetek5.config.staticelements.StaticElements;
import com.jagex.game.runetek5.config.varc.VarcTypeList;
import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.hint.HintDetails;
import com.jagex.game.runetek5.graphics.hint.HintRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapCategory;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1;
import com.jagex.game.runetek5.graphics.model.MediaType;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;
import com.jagex.game.runetek5.interaction.Friend;
import com.jagex.game.runetek5.interaction.FriendList;
import com.jagex.game.runetek5.interaction.Ignore;
import com.jagex.game.runetek5.interaction.IgnoreList;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.ChatLine;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.network.world.WorldMeta;
import com.jagex.game.runetek5.network.world.WorldDescriptor;
import com.jagex.game.runetek5.rendering.MessageBox;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.*;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.VorbisSoundManager;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.ProfileCpu;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.Browser;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.*;
import com.jagex.game.runetek5.vardomain.VarProviders;

public class CS2Interpreter {

    public static final int[] anIntArray428 = {0, 1, 2, 3, 4, 5, 6, 14};
    public static final int[] anIntArray422 = {7, 8, 9, 10, 11, 12, 13, 15};
    public static int lastEnteredGraphicsMode;
    public static int CS2_QUERY_RESULTS_LEN;

    public static void executeOperation(ClientScriptOpCode operation, CS2Executor exec) {
        switch (operation) {
            case PUSH_INT:
                pushInt(exec);
                break;
            case LOAD_VARP:
                loadVarp(exec);
                break;
            case STORE_VARP:
                storeVarp(exec);
                break;
            case PUSH_STRING:
                pushString(exec);
                break;
            case GOTO:
                gotoInstr(exec);
                break;
            case INT_NE:
                intNotEqual(exec);
                break;
            case INT_EQ:
                intEqual(exec);
                break;
            case INT_LT:
                intLessThan(exec);
                break;
            case INT_GT:
                intGreaterThan(exec);
                break;
            case GET_VARP_OLD:
                getVarpOld(exec);
                break;
            case GET_VARPBIT_OLD:
                getVarpbitOld(exec);
                break;
            case GET_VARN_OLD:
                getVarnOld(exec);
                break;
            case GET_VARNBIT_OLD:
                getVarnbitOld(exec);
                break;
            case RETURN:
                returnInstr(exec);
                break;
            case LOAD_VARPBIT:
                loadVarpbit(exec);
                break;
            case STORE_VARPBIT:
                storeVarpbit(exec);
                break;
            case INT_LE:
                intLessOrEqual(exec);
                break;
            case INT_GE:
                intGreaterOrEqual(exec);
                break;
            case LOAD_INT:
                loadInt(exec);
                break;
            case STORE_INT:
                storeInt(exec);
                break;
            case LOAD_STRING:
                loadString(exec);
                break;
            case STORE_STRING:
                storeString(exec);
                break;
            case MERGE_STRINGS:
                mergeStrings(exec);
                break;
            case POP_INT:
                popInt(exec);
                break;
            case POP_STRING:
                popString(exec);
                break;
            case CALL_CS2:
                callCS2Script(exec);
                break;
            case LOAD_VARC:
                loadVarc(exec);
                break;
            case STORE_VARC:
                storeVarc(exec);
                break;
            case ARRAY_NEW:
                arrayNew(exec);
                break;
            case ARRAY_LOAD:
                arrayLoad(exec);
                break;
            case ARRAY_STORE:
                arrayStore(exec);
                break;
            case LOAD_VARC_STRING:
                loadVarcString(exec);
                break;
            case STORE_VARC_STRING:
                storeVarcString(exec);
                break;
            case SWITCH:
                switchInstr(exec);
                break;
            case PUSH_LONG:
                pushLong(exec);
                break;
            case POP_LONG:
                popLong(exec);
                break;
            case LOAD_LONG:
                loadLong(exec);
                break;
            case STORE_LONG:
                storeLong(exec);
                break;
            case LONG_NE:
                longNotEqual(exec);
                break;
            case LONG_EQ:
                longEqual(exec);
                break;
            case LONG_LT:
                longLessThan(exec);
                break;
            case LONG_GT:
                longGreaterThan(exec);
                break;
            case LONG_LE:
                longLessOrEqual(exec);
                break;
            case LONG_GE: //also branch
                longGreaterOrEqual(exec);
                break;
            case BRANCH_EQ1:
                branchEq1(exec);
                break;
            case BRANCH_EQ0:
                branchEq0(exec);
                break;
            case LOAD_CLAN_VAR:
                loadClanVar(exec);
                break;
            case LOAD_CLAN_VARBIT:
                loadClanVarbit(exec);
                break;
            case LOAD_CLAN_VAR_LONG:
                loadClanVarLong(exec);
                break;
            case LOAD_CLAN_VAR_STRING:
                loadClanVarString(exec);
                break;
            case LOAD_CLAN_SETTING_VAR:
                loadClanSettingVar(exec);
                break;
            case LOAD_CLAN_SETTING_VARBIT:
                loadClanSettingVarbit(exec);
                break;
            case LOAD_CLAN_SETTING_VAR_LONG:
                loadClanSettingVarLong(exec);
                break;
            case LOAD_CLAN_SETTING_VAR_STRING:
                loadClanSettingVarString(exec);
                break;
            case CC_CREATE:
                ccCreate(exec);
                break;
            case CC_DELETE:
                ccDelete(exec);
                break;
            case CC_DELETEALL:
                clearCompChildren(exec);
                break;
            case CC_SETCHILD_SLOT:
                method6668(exec);
                break;
            case CC_SETCHILD:
                method2620(exec);
                break;
            case IF_SENDTOFRONT:
                ifSetFront(true, exec);
                break;
            case IF_SENDTOBACK:
                ifSetFront(false, exec);
                break;
            case CC_SENDTOFRONT:
                ccSetFront(true, exec);
                break;
            case CC_SENDTOBACK:
                ccSetFront(false, exec);
                break;
            case IF_RESUME_PAUSEBUTTON:
                ifResumePauseButton(exec);
                break;
            case CC_RESUME_PAUSEBUTTON:
                ccResumePauseButton(exec);
                break;
            case BASE_IDKIT:
                setBaseIdentiKit(exec);
                break;
            case BASE_COLOR:
                setBaseColor(exec);
                break;
            case SET_GENDER:
                setGender(exec);
                break;
            case SET_ITEM:
                setItem(exec);
                break;
            case CC_SETPOSITION:
                ccSetPosition(exec);
                break;
            case CC_SETSIZE:
                ccSetSize(exec);
                break;
            case CC_SETHIDE:
                ccSetHide(exec);
                break;
            case CC_SETASPECT:
                ccSetAspect(exec);
                break;
            case CC_SETNOCLICKTHROUGH:
                ccSetNoClickThrough(exec);
                break;
            case CC_SETSCROLLPOS:
                ccSetScrollPos(exec);
                break;
            case CC_SETCOLOR:
                method5314(exec);
                break;
            case CC_SETFILL:
                method3918(exec);
                break;
            case CC_SETTRANS:
                method5487(exec);
                break;
            case CC_SETLINEWID:
                method6687(exec);
                break;
            case CC_SETGRAPHIC:
                method6071(exec);
                break;
            case CC_SET2DANGLE:
                method5332(exec);
                break;
            case CC_SETTILING:
                method5774(exec);
                break;
            case CC_SETMODEL:
                method8327(exec);
                break;
            case CC_SETMODELANGLE:
                method6364(exec);
                break;
            case CC_SETMODELANIM:
                method8726(exec);
                break;
            case CC_SETMODELORTHOG:
                method15523(exec);
                break;
            case CC_SETMODELTINT:
                method5099(exec);
                break;
            case CC_SETTEXT:
                setCCText(exec);
                break;
            case CC_SETTEXTFONT:
                setCCFont(exec);
                break;
            case CC_SETTEXTALIGN:
                setCCTextAlign(exec);
                break;
            case CC_SETTEXTSHADOW:
                method12659(exec);
                break;
            case CC_SETTEXTANTIMACRO:
                method8714(exec);
                break;
            case CC_SETOUTLINE:
                method4965(exec);
                break;
            case CC_SETGRAPHICSHADOW:
                method1497(exec);
                break;
            case CC_SETVFLIP:
                method1569(exec);
                break;
            case CC_SETHFLIP:
                method15448(exec);
                break;
            case CC_SETSCROLLSIZE:
                method7929(exec);
                break;
            case CC_SETALPHA:
                method7933(exec);
                break;
            case CC_SETMODELZOOM:
                method12116(exec);
                break;
            case CC_SETLINEDIRECTION:
                method1259(exec);
                break;
            case CC_SETMODELORIGIN:
                method893(exec);
                break;
            case CC_SETMAXLINES:
                method1949(exec);
                break;
            case CC_SETPARAM_INT:
                method3031(exec);
                break;
            case CC_SETPARAM_STRING:
                method4558(exec);
                break;
            case instr6050: //something sprite index 36
                method6210(exec);
                break;
            case instr6051: //something text index 36
                method8344(exec);
                break;
            case CC_SETRECOL:
                method11380(exec);
                break;
            case CC_SETRETEX:
                method6688(exec);
                break;
            case CC_SETFONTMONO:
                method12846(exec);
                break;
            case CC_SETPARAM:
                method8753(exec);
                break;
            case CC_SETCLICKMASK:
                method5833(exec);
                break;
            case CC_SETITEM:
                ccSetItem(exec);
                break;
            case CC_SETNPCHEAD:
                method3356(exec);
                break;
            case CC_SETPLAYERHEAD_SELF:
                method6802(exec);
                break;
            case CC_SETNPCMODEL:
                method8212(exec);
                break;
            case CC_SETPLAYERMODEL:
                method6283(exec);
                break;
            case CC_SETITEM_NONUM:
                ccSetItemNoNum(exec);
                break;
            case CC_SETMODELLIGHTING:
                method4970(exec);
                break;
            case instr6064:
                method2567(exec);
                break;
            case CC_SETITEM_WEARCOL:
                ccSetItemWearCol(exec);
                break;
            case CC_SETITEM_WEARCOL_NONUM:
                ccSetItemWearColNoNum(exec);
                break;
            case instr6895:
                method6510(exec);
                break;
            case CC_SETPLAYERMODEL_SELF:
                method4381(exec);
                break;
            case CC_SETITEM_ALWAYSNUM:
                ccSetItemAlwaysNum(exec);
                break;
            case CC_SETITEM_WEARCOL_ALWAYSNUM:
                ccSetItemWearColAlwaysNum(exec);
                break;
            case CC_SETOP:
                method6452(exec);
                break;
            case instr6676:
                ccSetDepthChild(exec);
                break;
            case instr6073: //CC_SETDRAGRENDERBEHAVIOUR?
                method2169(exec);
                break;
            case instr6443: //CC_SETDRAGDEADZONE?
                method2965(exec);
                break;
            case instr6075: //CC_SETDRAGDEADTIME?
                method4801(exec);
                break;
            case CC_SETOPBASE:
                ccSetOpBase(exec);
                break;
            case CC_SETTARGETVERB:
                ccSetTargetVerb(exec);
                break;
            case CC_CLEAROPS:
                method8298(exec);
                break;
            case CC_SETTARGETCURSORS:
                method6150(exec);
                break;
            case CC_SETOPCURSOR:
                method5203(exec);
                break;
            case CC_SETRESUMETEXT: //(pause text is official name but that's absolutely cooked)
                method541(exec);
                break;
            case CC_SETTARGETOPCURSOR:
                ccSetTargetOpCursor(exec);
                break;
            case CC_SETOPCHAR:
                method6305(exec);
                break;
            case CC_SETOPTCHAR:
                method5512(exec);
                break;
            case CC_SETOPKEY:
                method4200(exec);
                break;
            case CC_SETOPTKEY:
                method15416(exec);
                break;
            case CC_SETMOUSEOVERCURSOR:
                method6776(exec);
                break;
            case instr6088:
                method1606(exec);
                break;
            case instr5957:
                method3892(exec);
                break;
            case instr6450:
                method4030(exec);
                break;
            case instr6091:
                method8285(exec);
                break;
            case instr6092:
                method5008(exec);
                break;
            case instr6224:
                method6120(exec);
                break;
            case instr6094:
                method1631(exec);
                break;
            case instr6095:
                method5454(exec);
                break;
            case instr6556:
                method14521(exec);
                break;
            case instr6687:
                method4971(exec);
                break;
            case instr6499:
                method14746(exec);
                break;
            case instr6084:
                method4797(exec);
                break;
            case CC_SETONMOUSEOVER:
                method260(exec);
                break;
            case CC_SETONMOUSELEAVE:
                method6043(exec);
                break;
            case instr6899:
                method8723(exec);
                break;
            case instr6103:
                method485(exec);
                break;
            case instr6765:
                method12499(exec);
                break;
            case instr6370:
                method6395(exec);
                break;
            case instr6106:
                method12802(exec);
                break;
            case instr6580:
                method1084(exec);
                break;
            case instr6437:
                method5296(exec);
                break;
            case instr6903:
                method16095(exec);
                break;
            case instr6099:
                method3561(exec);
                break;
            case instr6111:
                method3676(exec);
                break;
            case instr6112:
                method5004(exec);
                break;
            case instr6113:
                method14340(exec);
                break;
            case instr6492:
                method3041(exec);
                break;
            case instr6096:
                method7270(exec);
                break;
            case instr6116:
                method5775(exec);
                break;
            case instr6054:
                method4283(exec);
                break;
            case instr6782:
                method3546(exec);
                break;
            case CC_SETLINKACTIVECLANCHANNEL:
                method741(exec);
                break;
            case instr6120:
                method3370(exec);
                break;
            case instr6121:
                method6800(exec);
                break;
            case instr6552:
                method8863(exec);
                break;
            case instr6772:
                method6796(exec);
                break;
            case instr6124:
                method7032(exec);
                break;
            case instr6125:
                method5598(exec);
                break;
            case CC_ISHIDDEN:
                method892(exec);
                break;
            case instr6462:
                method13491(exec);
                break;
            case instr6128:
                method3168(exec);
                break;
            case instr6762:
                method7219(exec);
                break;
            case instr6130:
                method11125(exec);
                break;
            case instr6131:
                method5123(exec);
                break;
            case instr6132:
                method7502(exec);
                break;
            case instr6133:
                method4901(exec);
                break;
            case instr6134:
                method14578(exec);
                break;
            case CC_GETSPRITESCALE:
                method11249(exec);
                break;
            case CC_GETSPRITEPITCH:
                method12925(exec);
                break;
            case CC_GETSPRITEYAW:
                method8211(exec);
                break;
            case instr6670:
                method6236(exec);
                break;
            case CC_GETTRANSPARENCY:
                method8735(exec);
                break;
            case instr6005:
                method11350(exec);
                break;
            case instr6262:
                method1498(exec);
                break;
            case instr6928:
                method6314(exec);
                break;
            case instr6771:
                method1134(exec);
                break;
            case instr6144:
                method12723(exec);
                break;
            case instr6230:
                method6449(exec);
                break;
            case instr6566:
                method15559(exec);
                break;
            case instr6842:
                method7676(exec);
                break;
            case instr6148:
                method8154(exec);
                break;
            case CC_CONTAINERITEMID:
                method4277(exec);
                break;
            case instr6150:
                method4565(exec);
                break;
            case instr6151:
                method4802(exec);
                break;
            case CC_GETOPTIONFLAGS:
                method1562(exec);
                break;
            case instr6153:
                method779(exec);
                break;
            case instr6154:
                method1864(exec);
                break;
            case instr6155:
                method401(exec);
                break;
            case instr6662:
                method8859(exec);
                break;
            case instr6157:
                method6138(exec);
                break;
            case IF_SETPOSITION:
                ifSetPosition(exec);
                break;
            case IF_SETSIZE:
                method2826(exec);
                break;
            case IF_SETHIDE:
                hideComp(exec);
                break;
            case IF_SETASPECT:
                method283(exec);
                break;
            case IF_SETNOCLICKTHROUGH:
                method1167(exec);
                break;
            case IF_SETSCROLLPOS:
                method5302(exec);
                break;
            case IF_SETCOLOR:
                method15514(exec);
                break;
            case IF_SETFILL:
                method3554(exec);
                break;
            case IF_SETTRANS:
                setCompTransparency(exec);
                break;
            case IF_SETLINEWID:
                method6016(exec);
                break;
            case IF_SETGRAPHIC:
                setCompSprite(exec);
                break;
            case IF_SET2DANGLE:
                method460(exec);
                break;
            case IF_SETTILING:
                method3250(exec);
                break;
            case IF_SETMODEL:
                method4311(exec);
                break;
            case IF_SETMODELANGLE:
                method3020(exec);
                break;
            case IF_SETMODELANIM:
                animateComp(exec);
                break;
            case instr6174:
                method11492(exec);
                break;
            case IF_SETMODELTINT:
                method1509(exec);
                break;
            case IF_SETTEXT:
                setCompText(exec);
                break;
            case IF_SETTEXTFONT:
                ifSetfont(exec);
                break;
            case IF_SETTEXTALIGN:
                setIfTextAlign(exec);
                break;
            case instr6170:
                method8018(exec);
                break;
            case instr6180:
                method3064(exec);
                break;
            case instr6181:
                method484(exec);
                break;
            case IF_SETSPRITESHADOW:
                method4960(exec);
                break;
            case instr6183:
                method6669(exec);
                break;
            case instr6184:
                method1492(exec);
                break;
            case IF_SETSCROLLSIZE:
                method15510(exec);
                break;
            case IF_SETALPHA:
                method11252(exec);
                break;
            case instr6187:
                method7208(exec);
                break;
            case instr6188:
                method6738(exec);
                break;
            case instr6377:
                method563(exec);
                break;
            case instr6190:
                method1624(exec);
                break;
            case instr6191:
                method5756(exec);
                break;
            case instr6192:
                method12839(exec);
                break;
            case instr6193:
                method14342(exec);
                break;
            case instr6194:
                method305(exec);
                break;
            case instr6195:
                method4174(exec);
                break;
            case instr6196:
                method7560(exec);
                break;
            case instr6197:
                method3949(exec);
                break;
            case IF_SETCLICKMASK:
                method1069(exec);
                break;
            case IF_SETITEM:
                ifSetItem(exec);
                break;
            case IF_SETNPCHEAD:
                method4863(exec);
                break;
            case IF_SETPLAYERHEAD_SELF:
                method11403(exec);
                break;
            case instr6715:
                method2865(exec);
                break;
            case instr6864:
                method1510(exec);
                break;
            case instr6879:
                ifSetItemNoNum(exec);
                break;
            case instr6205:
                method8750(exec);
                break;
            case instr6919:
                method12492(exec);
                break;
            case instr6207:
                ifSetItemWearCol(exec);
                break;
            case instr6208:
                ifSetItemWearColNoNum(exec);
                break;
            case instr6209:
                method11126(exec);
                break;
            case instr6210:
                method3117(exec);
                break;
            case instr6129:
                ifSetItemAlwaysNum(exec);
                break;
            case instr6793:
                ifSetItemWearColAlwaysNum(exec);
                break;
            case instr6213:
                method15475(exec);
                break;
            case instr6214:
                ifSetDepthChild(exec);
                break;
            case instr6215:
                method2150(exec);
                break;
            case instr5969:
                method8208(exec);
                break;
            case instr6217:
                method5488(exec);
                break;
            case instr6569:
                method6310(exec);
                break;
            case instr6219:
                method4633(exec);
                break;
            case instr6220:
                method4864(exec);
                break;
            case instr6221:
                method11373(exec);
                break;
            case instr6222:
                method8152(exec);
                break;
            case instr6223:
                method6141(exec);
                break;
            case instr6888:
                method1385(exec);
                break;
            case instr6136:
                method5758(exec);
                break;
            case instr6226:
                method6851(exec);
                break;
            case instr6477:
                method6938(exec);
                break;
            case instr6228:
                method7520(exec);
                break;
            case instr6229:
                method1349(exec);
                break;
            case HOOK_MOUSE_PRESS:
                hookMousePress(exec);
                break;
            case instr6231:
                method11438(exec);
                break;
            case instr6232:
                method4904(exec);
                break;
            case HOOK_MOUSE_ENTER:
                hookMouseEnter(exec);
                break;
            case HOOK_MOUSE_EXIT:
                hookMouseExit(exec);
                break;
            case instr6235:
                method2071(exec);
                break;
            case instr6527:
                method3357(exec);
                break;
            case instr6237:
                method3940(exec);
                break;
            case instr6342:
                method11246(exec);
                break;
            case instr6239:
                method2564(exec);
                break;
            case instr6240:
                method3596(exec);
                break;
            case instr5973:
                method5511(exec);
                break;
            case IF_SETONMOUSEOVER:
                ifSetOnMouseOver(exec);
                break;
            case IF_SETONMOUSELEAVE:
                ifSetOnMouseLeave(exec);
                break;
            case instr6393:
                method6762(exec);
                break;
            case instr6376:
                method13408(exec);
                break;
            case instr6246:
                method14640(exec);
                break;
            case instr6247:
                method5000(exec);
                break;
            case instr6248:
                method564(exec);
                break;
            case instr6708:
                method11338(exec);
                break;
            case instr6250:
                method7961(exec);
                break;
            case instr6251:
                method11593(exec);
                break;
            case instr6252:
                method4175(exec);
                break;
            case instr6253:
                method11372(exec);
                break;
            case instr6254:
                method8694(exec);
                break;
            case instr6255:
                method4033(exec);
                break;
            case instr6256:
                method5595(exec);
                break;
            case instr6257:
                method6685(exec);
                break;
            case instr6507:
                method8843(exec);
                break;
            case instr6259:
                method1813(exec);
                break;
            case instr6260:
                method2855(exec);
                break;
            case IF_SETLINKACTIVECLANCHANNEL:
                method4629(exec);
                break;
            case instr6898:
                method6372(exec);
                break;
            case IF_CLEARSCRIPTHOOKS:
                method6040(exec);
                break;
            case IF_GETX:
                method4146(exec);
                break;
            case IF_GETY:
                method6151(exec);
                break;
            case IF_GETWIDTH:
                getCompWidth(exec);
                break;
            case IF_GETHEIGHT:
                getCompHeight(exec);
                break;
            case IF_GETHIDE:
                getCompHidden(exec);
                break;
            case IF_GETLAYER:
                method5045(exec);
                break;
            case IF_GETPARENTLAYER:
                method8220(exec);
                break;
            case IF_GETCOLOR:
                method454(exec);
                break;
            case IF_GETSCROLLX:
                method7422(exec);
                break;
            case IF_GETSCROLLY:
                method11473(exec);
                break;
            case IF_GETTEXT:
                getCompText(exec);
                break;
            case IF_GETSCROLLWIDTH:
                method858(exec);
                break;
            case instr6276:
                method14659(exec);
                break;
            case instr6277:
                method289(exec);
                break;
            case instr6292:
                method2945(exec);
                break;
            case instr6279:
                method6174(exec);
                break;
            case instr6280:
                method1571(exec);
                break;
            case instr6281:
                method11247(exec);
                break;
            case IF_GETOFFSETX:
                method5926(exec);
                break;
            case IF_GETOFFSETY:
                method6696(exec);
                break;
            case IF_GETSPRITEID:
                method6212(exec);
                break;
            case instr6072:
                method3065(exec);
                break;
            case instr6159:
                method1512(exec);
                break;
            case instr6287:
                method14602(exec);
                break;
            case instr6288:
                method7643(exec);
                break;
            case instr6289:
                method6142(exec);
                break;
            case instr6290:
                method15392(exec);
                break;
            case instr6875:
                method4201(exec);
                break;
            case instr6805:
                method2914(exec);
                break;
            case instr6293:
                method4898(exec);
                break;
            case IF_ISOPEN:
                method5002(exec);
                break;
            case instr6295:
                method14679(exec);
                break;
            case instr6296:
                method6041(exec);
                break;
            case instr6297:
                method8045(exec);
                break;
            case instr6298:
                method8215(exec);
                break;
            case instr6299:
                method8715(exec);
                break;
            case instr6300:
                method855(exec); //IF_GETCHARINDEXATPOS?
                break;
            case instr6391:
                method6941(exec); //IF_GETCHARPOSATINDEX?
                break;
            case instr6302:
                method6670(exec);
                break;
            case instr6303:
                method1967(exec);
                break;
            case instr6304:
                method14660(exec);
                break;
            case instr6646:
                method4865(exec);
                break;
            case instr6186:
                method3201(exec);
                break;
            case instr6307:
                method5029(exec);
                break;
            case instr6002:
                method3171(exec);
                break;
            case instr6309:
                method12583(exec);
                break;
            case MES:
                sendMessage(exec);
                break;
            case RESET_MYPLAYER_ANIMS:
                method1836(exec);
                break;
            case IF_CLOSE:
                method3939();
                break;
            case RESUME_COUNTDIALOG:
                resumeCountDialog(exec);
                break;
            case RESUME_NAMEDIALOG:
                resumeNamedDialog(exec);
                break;
            case RESUME_STRINGDIALOG:
                resumeStringDialog(exec);
                break;
            case OPPLAYER:
                method6194(exec);
                break;
            case IF_DRAGPICKUP:
                method870(exec);
                break;
            case CC_DRAGPICKUP:
                method5082(exec);
                break;
            case RESUME_ITEMDIALOG:
                method12841(exec);
                break;
            case IF_OPENSUBCLIENT:
                method1355(exec);
                break;
            case IF_CLOSESUBCLIENT:
                method4839(exec);
                break;
            case OPPLAYERT:
                method15399(exec);
                break;
            case MES_TYPED:
                appendTypedMessage(exec);
                break;
            case SETUP_MESSAGEBOX:
                loadMessagebox(exec);
                break;
            case RESUME_HSLDIALOG:
                resumeHSLDialog(exec);
                break;
            case RESUME_CLANFORUMQFCDIALOG:
                resumeClanForumQFCDialog(exec);
                break;
            case SOUND_SYNTH:
                playSoundSynth(exec);
                break;
            case SOUND_SONG:
                method3555(exec);
                break;
            case SOUND_JINGLE:
                method5027(exec);
                break;
            case SOUND_SYNTH_VOLUME:
                method14520(exec);
                break;
            case SOUND_SONG_VOLUME:
                method4557(exec);
                break;
            case SOUND_JINGLE_VOLUME:
                method11612(exec);
                break;
            case SOUND_VORBIS_VOLUME:
                method2823(exec);
                break;
            case SOUND_SPEECH_VOLUME:
                method8153(exec);
                break;
            case SOUND_SYNTH_RATE:
                method1888(exec);
                break;
            case SOUND_VORBIS_RATE:
                method5335(exec);
                break;
            case CLIENTCLOCK:
                method456(exec);
                break;
            case INV_GETITEM:
                method7675(exec);
                break;
            case INV_GETNUM:
                method5197(exec);
                break;
            case INV_TOTAL:
                method15389(exec);
                break;
            case INV_SIZE:
                method2729(exec);
                break;
            case INV_TOTALCAT:
                method16089(exec);
                break;
            case STAT:
                method3913(exec);
                break;
            case STAT_BASE:
                method11486(exec);
                break;
            case STAT_VISIBLE_XP:
                method7444(exec);
                break;
            case COORD:
                getMyPlayerPos(exec);
                break;
            case COORDX:
                method13057(exec);
                break;
            case COORDY:
                method7427(exec);
                break;
            case COORDZ:
                method5003(exec);
                break;
            case WORLD_MEMBERS:
                method5733(exec);
                break;
            case INVOTHER_GETITEM:
                method4825(exec);
                break;
            case INVOTHER_GETNUM:
                method6230(exec);
                break;
            case INVOTHER_TOTAL:
                method5490(exec);
                break;
            case STAFFMODLEVEL:
                staffModLevel(exec);
                break;
            case GET_SYSTEM_UPDATE_TIMER:
                getSystemUpdateTimer(exec);
                break;
            case WORLD_ID:
                getWorldId(exec);
                break;
            case RUNENERGY_VISIBLE:
                getRunEnergy(exec);
                break;
            case RUNWEIGHT_VISIBLE:
                getRunWeight(exec);
                break;
            case PLAYERMOD:
                method1467(exec);
                break;
            case PLAYERMODLEVEL:
                method3033(exec);
                break;
            case PLAYERMEMBER:
                getPlayerIsMember(exec);
                break;
            case COMLEVEL_ACTIVE:
                getPlayerCombatLevel(exec);
                break;
            case GENDER:
                getPlayerGender(exec);
                break;
            case WORLD_QUICKCHAT:
                getWorldQuickchatOnly(exec);
                break;
            case CONTAINER_FREE_SPACE:
                getContainerFreeSpace(exec);
                break;
            case CONTAINER_TOTAL_PARAM:
                containerTotalParam(exec);
                break;
            case CONTAINER_TOTAL_PARAM_STACK:
                containerTotalParamStack(exec);
                break;
            case WORLD_LANGUAGE:
                getWorldLanguage(exec);
                break;
            case MOVECOORD:
                method1613(exec);
                break;
            case AFFILIATE:
                method6799(exec);
                break;
            case PROFILE_CPU:
                getCPUProfile(exec);
                break;
            case PLAYERDEMO:
                method6399(exec);
                break;
            case APPLET_HASFOCUS:
                getAppletFocus(exec);
                break;
            case FROMBILLING:
                method7699(exec);
                break;
            case GET_MOUSE_X:
                getMouseX(exec);
                break;
            case GET_MOUSE_Y:
                getMouseY(exec);
                break;
            case GET_ACTIVE_MINIMENU_ENTRY:
                method1801(exec);
                break;
            case GET_SECOND_MINIMENU_ENTRY:
                method7322(exec);
                break;
            case GET_MINIMENU_LENGTH:
                method6121(exec);
                break;
            case GET_CURRENTCURSOR:
                method2552(exec);
                break;
            case GET_SELFYANGLE:
                method3340(exec);
                break;
            case MAP_ISOWNER:
                method13494(exec);
                break;
            case GET_MOUSEBUTTONS:
                method4031(exec);
                break;
            case SELF_PLAYER_UID:
                method7186(exec);
                break;
            case GET_MINIMENU_TARGET:
                method6275(exec);
                break;
            case ENUM_STRING:
                method4966(exec);
                break;
            case ENUM:
                method5901(exec);
                break;
            case ENUM_HASOUTPUT:
                method14503(exec);
                break;
            case ENUM_HASOUTPUT_STRING:
                method5776(exec);
                break;
            case ENUM_GETOUTPUTCOUNT:
                getEnumSize(exec);
                break;
            case ENUM_GETREVERSECOUNT:
                method1853(exec);
                break;
            case ENUM_GETREVERSECOUNT_STRING:
                method8757(exec);
                break;
            case ENUM_GETREVERSEINDEX:
                method6242(exec);
                break;
            case ENUM_GETREVERSEINDEX_STRING:
                method536(exec);
                break;
            case EMAIL_VALIDATION_SUBMIT_CODE:
                method11594(exec);
                break;
            case EMAIL_VALIDATION_CHANGE_ADDRESS:
                method7501(exec);
                break;
            case EMAIL_VALIDATION_ADD_NEW_ADDRESS:
                emailValidationAddNewAddress(exec);
                break;
            case FRIEND_COUNT:
                method4773(exec);
                break;
            case FRIEND_GETNAME:
                method2096(exec);
                break;
            case FRIEND_GETWORLD:
                method5695(exec);
                break;
            case FRIEND_GETRANK:
                method3597(exec);
                break;
            case FRIEND_GETWORLDFLAGS:
                method11370(exec);
                break;
            case FRIEND_SETRANK:
                method1641(exec);
                break;
            case FRIEND_ADD:
                method1500(exec);
                break;
            case FRIEND_DEL:
                method4278(exec);
                break;
            case IGNORE_ADD:
                method1452(exec);
                break;
            case IGNORE_DEL:
                method4181(exec);
                break;
            case FRIEND_TEST:
                method7932(exec);
                break;
            case FRIEND_GETWORLDNAME:
                method2871(exec);
                break;
            case FC_GETCHATDISPLAYNAME:
                loadFriendchatName(exec);
                break;
            case FC_GETCHATCOUNT:
                method6680(exec);
                break;
            case FC_GETCHATUSERNAME:
                method8870(exec);
                break;
            case FC_GETCHATUSERWORLD:
                method4905(exec);
                break;
            case FC_GETCHATUSERRANK:
                method8756(exec);
                break;
            case FC_GETCHATMINKICK:
                method3829(exec);
                break;
            case FC_KICKUSER:
                method6152(exec);
                break;
            case FC_GETCHATRANK:
                method14491(exec);
                break;
            case FC_JOINCHAT:
                method4902(exec);
                break;
            case FC_LEAVECHAT:
                sendJoinFriendchatPacket();
                break;
            case IGNORE_COUNT:
                method4829(exec);
                break;
            case IGNORE_GETNAME:
                method5489(exec);
                break;
            case IGNORE_TEST:
                method16124(exec);
                break;
            case FC_ISSELF:
                method1608(exec);
                break;
            case FC_GETCHATOWNERNAME:
                method13044(exec);
                break;
            case FC_GETCHATUSERWORLDNAME:
                method11355(exec);
                break;
            case FRIEND_PLATFORM:
                method1625(exec);
                break;
            case FRIEND_GETSLOTFROMNAME:
                method1775(exec);
                break;
            case PLAYERCOUNTRY:
                method14833(exec);
                break;
            case IGNORE_ADD_TEMP:
                method1462(exec);
                break;
            case IGNORE_IS_TEMP:
                method3164(exec);
                break;
            case FC_GETCHATUSERNAME_UNFILTERED:
                method4566(exec);
                break;
            case IGNORE_GETNAME_UNFILTERED:
                method8019(exec);
                break;
            case FRIEND_IS_REFERRER:
                method3739(exec);
                break;
            case ACTIVECLANSETTINGS_FIND_LISTENED:
                method8698(exec);
                break;
            case ACTIVECLANSETTINGS_FIND_AFFINED:
                method12881(exec);
                break;
            case ACTIVECLANSETTINGS_GETCLANNAME:
                method5922(exec);
                break;
            case ACTIVECLANSETTINGS_GETALLOWUNAFFINED:
                method8701(exec);
                break;
            case ACTIVECLANSETTINGS_GETRANKTALK:
                method5902(exec);
                break;
            case ACTIVECLANSETTINGS_GETRANKKICK:
                method1889(exec);
                break;
            case ACTIVECLANSETTINGS_GETRANKLOOTSHARE:
                method7274(exec);
                break;
            case ACTIVECLANSETTINGS_GETCOINSHARE:
                method1135(exec);
                break;
            case ACTIVECLANSETTINGS_GETAFFINEDCOUNT:
                method1802(exec);
                break;
            case ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME:
                method8037(exec);
                break;
            case ACTIVECLANSETTINGS_GETAFFINEDRANK:
                method2102(exec);
                break;
            case ACTIVECLANSETTINGS_GETBANNEDCOUNT:
                method1171(exec);
                break;
            case ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME:
                method3779(exec);
                break;
            case ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO:
                method5362(exec);
                break;
            case ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT:
                method933(exec);
                break;
            case ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT:
                method4295(exec);
                break;
            case ACTIVECLANSETTINGS_GETAFFINEDSLOT:
                method3361(exec);
                break;
            case ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT:
                method6205(exec);
                break;
            case UNUSED_CLAN_OP:
                method15075(exec);
                break;
            case ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY:
                method6741(exec);
                break;
            case ACTIVECLANCHANNEL_FIND_LISTENED:
                activeClanchannelFindListened(exec);
                break;
            case ACTIVECLANCHANNEL_FIND_AFFINED:
                activeClanchannelFindAffined(exec);
                break;
            case ACTIVECLANCHANNEL_GETCLANNAME:
                method811(exec);
                break;
            case ACTIVECLANCHANNEL_GETRANKKICK:
                method14644(exec);
                break;
            case ACTIVECLANCHANNEL_GETRANKTALK:
                method3021(exec);
                break;
            case ACTIVECLANCHANNEL_GETUSERCOUNT:
                method13410(exec);
                break;
            case ACTIVECLANCHANNEL_GETUSERDISPLAYNAME:
                method1979(exec);
                break;
            case ACTIVECLANCHANNEL_GETUSERRANK:
                method6677(exec);
                break;
            case ACTIVECLANCHANNEL_GETUSERWORLD:
                method3169(exec);
                break;
            case ACTIVECLANCHANNEL_KICKUSER:
                method4968(exec);
                break;
            case ACTIVECLANCHANNEL_GETUSERSLOT:
                method3043(exec);
                break;
            case ACTIVECLANCHANNEL_GETSORTEDUSERSLOT:
                method471(exec);
                break;
            case CLAN_VARS_ENABLED:
                clanVarsEnabled(exec);
                break;
            case STOCKMARKET_GETOFFERTYPE:
                method6231(exec);
                break;
            case STOCKMARKET_GETOFFERITEM:
                getGEOfferItem(exec);
                break;
            case STOCKMARKET_GETOFFERPRICE:
                getGEOfferPrice(exec);
                break;
            case STOCKMARKET_GETOFFERCOUNT:
                method3802(exec);
                break;
            case STOCKMARKET_GETOFFERCOMPLETEDCOUNT:
                method7715(exec);
                break;
            case STOCKMARKET_GETOFFERCOMPLETEDGOLD:
                method6206(exec);
                break;
            case STOCKMARKET_ISOFFEREMPTY:
                method6797(exec);
                break;
            case STOCKMARKET_ISOFFERSTABLE:
                method8716(exec);
                break;
            case STOCKMARKET_ISOFFERFINISHED:
                method1951(exec);
                break;
            case STOCKMARKET_ISOFFERADDING:
                method5297(exec);
                break;
            case ADD:
                add(exec);
                break;
            case SUBTRACT:
                subtract(exec);
                break;
            case MULTIPLY:
                multiply(exec);
                break;
            case DIVIDE:
                divide(exec);
                break;
            case RANDOM:
                random(exec);
                break;
            case RANDOM_INCLUSIVE:
                randomInclusive(exec);
                break;
            case INTERPOLATE:
                interpolate(exec);
                break;
            case ADD_PERCENT:
                addPercent(exec);
                break;
            case SET_BIT:
                setBit(exec);
                break;
            case CLEAR_BIT:
                clearBit(exec);
                break;
            case BIT_FLAGGED:
                bitFlagged(exec);
                break;
            case MODULO:
                modulo(exec);
                break;
            case POW:
                pow(exec);
                break;
            case POW_INVERSE:
                powInverse(exec);
                break;
            case BIT_AND:
                bitAnd(exec);
                break;
            case BIT_OR:
                bitOr(exec);
                break;
            case MIN:
                min(exec);
                break;
            case MAX:
                max(exec);
                break;
            case SCALE:
                scale(exec);
                break;
            case RANDOM_SOUND_PITCH:
                randSoundPitch(exec);
                break;
            case HSVTORGB:
                hsvToRGB(exec);
                break;
            case BIT_NOT:
                bitNot(exec);
                break;
            case APPEND_NUM:
                strAppendNum(exec);
                break;
            case APPEND:
                strAppend(exec);
                break;
            case APPEND_SIGNNUM:
                strAppendSigNum(exec);
                break;
            case GET_COL_TAG:
                getColTag(exec);
                break;
            case LOWERCASE:
                lowerString(exec);
                break;
            case FROMDATE:
                fromDate(exec);
                break;
            case TEXT_GENDER:
                textGender(exec);
                break;
            case TOSTRING:
                toString(exec);
                break;
            case COMPARE:
                stringEqual(exec);
                break;
            case PARAHEIGHT:
                method3346(exec);
                break;
            case PARAWIDTH:
                method5735(exec);
                break;
            case TEXT_SWITCH:
                method7556(exec);
                break;
            case ESCAPE:
                method5456(exec);
                break;
            case APPEND_CHAR:
                method6117(exec);
                break;
            case CHAR_ISPRINTABLE:
                method4252(exec);
                break;
            case CHAR_ISALPHANUMERIC:
                method8218(exec);
                break;
            case CHAR_ISALPHA:
                charIsalpha(exec);
                break;
            case CHAR_ISNUMERIC:
                charIsnumeric(exec);
                break;
            case STRING_LENGTH:
                stringLength(exec);
                break;
            case SUBSTRING:
                substring(exec);
                break;
            case REMOVETAGS:
                removetags(exec);
                break;
            case STRING_INDEXOF_CHAR:
                method5915(exec);
                break;
            case STRING_INDEXOF_STRING:
                method3547(exec);
                break;
            case CHAR_TOLOWERCASE:
                method3951(exec);
                break;
            case CHAR_TOUPPERCASE:
                method6373(exec);
                break;
            case TOSTRING_LOCALISED:
                method6691(exec);
                break;
            case STRINGWIDTH:
                method1854(exec);
                break;
            case FORMAT_DATETIME_FROM_MINUTES:
                method3830(exec);
                break;
            case CLANFORUMQFC_TOSTRING:
                method6139(exec);
                break;
            case ITEM_NAME:
                method4247(exec);
                break;
            case ITEM_OP:
                method3042(exec);
                break;
            case ITEM_IOP:
                method4202(exec);
                break;
            case ITEM_COST:
                method6284(exec);
                break;
            case ITEM_STACKABLE:
                method11406(exec);
                break;
            case ITEM_CERT:
                method12215(exec);
                break;
            case ITEM_UNCERT:
                method12724(exec);
                break;
            case ITEM_WEARPOS:
                method7445(exec);
                break;
            case ITEM_WEARPOS2:
                method5765(exec);
                break;
            case ITEM_WEARPOS3:
                method6781(exec);
                break;
            case ITEM_MEMBERS:
                method5198(exec);
                break;
            case ITEM_PARAM:
                getItemParam(exec);
                break;
            case OC_ICURSOR:
                method4630(exec);
                break;
            case ITEM_FIND:
                method5691(exec);
                break;
            case ITEM_FINDNEXT:
                method2868(exec);
                break;
            case OC_FINDRESTART:
                method4315();
                break;
            case ITEM_MULTISTACKSIZE:
                method3229(exec);
                break;
            case ITEM_FIND_PARAMINT:
                method4089(exec);
                break;
            case ITEM_FIND_PARAMSTR:
                method6400(exec);
                break;
            case ITEM_MINIMENU_COLOUR_OVERRIDDEN:
                method3030(exec);
                break;
            case ITEM_MINIMENU_COLOUR:
                method911(exec);
                break;
            case NPC_PARAM:
                getNPCParam(exec);
                break;
            case OBJECT_PARAM:
                getObjectParam(exec);
                break;
            case STRUCT_PARAM:
                getStructParam(exec);
                break;
            case ANIMATION_PARAM:
                getAnimationParam(exec);
                break;
            case BAS_GETANIM_READY:
                method15408(exec);
                break;
            case UNUSED_LOGIN_GLOBAL_BOOL: //related
                method5755(exec);
                break;
            case UNUSED_PACKET_SEND_STRING: //related
                sendAClass3794624(exec);
                break;
            case UNUSED_PACKET_SET_GLOBAL_BYTE: //related
                method546(exec);
                break;
            case CHAT_GETFILTER_PUBLIC:
                setPublicFilter(exec);
                break;
            case CHAT_SETFILTER:
                ChatBox.setFilter(exec);
                break;
            case CHAT_SENDABUSEREPORT:
                sendReportAbusePacket(exec);
                break;
            case CHAT_GETBYLINE:
                method2824(exec);
                break;
            case CHAT_GETTYPEBYLINE:
                method15512(exec);
                break;
            case CHAT_GETFILTER_PRIVATE:
                setPrivateFilter(exec);
                break;
            case instr6563:
                method6005(exec);
                break;
            case SEND_PUBLIC_CHAT:
                method1860(exec);
                break;
            case SEND_PRIVATE_MESSAGE:
                method6913(exec);
                break;
            case instr6189:
                method6237(exec);
                break;
            case CHAT_GETCLAN:
                method6733(exec);
                break;
            case instr6565:
                method3783(exec);
                break;
            case CHAT_PLAYERNAME:
                method8717(exec);
                break;
            case CHAT_GETFILTER_TRADE:
                setTradeFilter(exec);
                break;
            case instr6225:
                method5100(exec);
                break;
            case instr6572:
                chatlineGeteffectflags(exec);
                break;
            case instr6573:
                method14835(exec);
                break;
            case instr6574:
                method7771(exec);
                break;
            case instr6682:
                method3803(exec);
                break;
            case CHATLINE_GETCYCLES20MS:
                method894(exec);
                break;
            case instr6577:
                method565(exec);
                break;
            case QC_GETNAME:
                method12689(exec);
                break;
            case instr6202:
                method742(exec);
                break;
            case instr6030:
                method3780(exec);
                break;
            case instr6581:
                method814(exec);
                break;
            case QC_GETMESSAGE:
                method14492(exec);
                break;
            case instr6583:
                method5697(exec);
                break;
            case instr6584:
                method8865(exec);
                break;
            case instr6069:
                method1362(exec);
                break;
            case QC_SETMESSAGE:
                method11154(exec);
                break;
            case QC_SENDPUBLICMESSAGE:
                method6315(exec);
                break;
            case QC_SENDPRIVATEMESSAGE:
                method8296(exec);
                break;
            case QC_SENDFRIENDSCHATMESSAGE:
                method3921(exec);
                break;
            case QC_GETSUBHOTKEY:
                method291(exec);
                break;
            case instr6591:
                method7826(exec);
                break;
            case instr6592:
                method4148(exec);
                break;
            case instr6449:
                method1783(exec);
                break;
            case instr6594:
                method6304(exec);
                break;
            case instr6595:
                method6439(exec);
                break;
            case instr6596:
                method4631(exec);
                break;
            case instr6597:
                method1085(exec);
                break;
            case instr6598:
                method2091(exec);
                break;
            case instr6599:
                method1070(exec);
                break;
            case instr6613:
                method3986(exec);
                break;
            case instr6601:
                method3950();
                break;
            case QC_SENDCLANCHATMESSAGE:
                method468(exec);
                break;
            case QC_SENDGUESTCLANCHATMESSAGE:
                method1603(exec);
                break;
            case instr6233:
                method3988(exec);
                break;
            case instr6716:
                method928(exec);
                break;
            case instr6606:
                method8297(exec);
                break;
            case instr6607:
                zoomWorldmap(exec);
                break;
            case instr6608:
                method15417(exec);
                break;
            case instr6308:
                method7668(exec);
                break;
            case instr6610:
                method13756(exec);
                break;
            case instr6611:
                method6073(exec);
                break;
            case instr6612:
                method8219(exec);
                break;
            case instr6849:
                method1391(exec);
                break;
            case instr6614:
                method7276(exec);
                break;
            case instr6615:
                method4988(exec);
                break;
            case instr6616:
                method5788(exec);
                break;
            case instr6617:
                method787(exec);
                break;
            case instr6090:
                method3556(exec);
                break;
            case instr6074:
                method2856(exec);
                break;
            case instr6620:
                method856(exec);
                break;
            case instr6621:
                method2737(exec);
                break;
            case instr6439:
                method3768(exec);
                break;
            case instr6685:
                method7043(exec);
                break;
            case instr6638:
                method2584(exec);
                break;
            case instr6212:
                method2916(exec);
                break;
            case instr5966:
                method12572(exec);
                break;
            case instr6206:
                method810(exec);
                break;
            case instr6628:
                method7774(exec);
                break;
            case instr6238:
                method3919(exec);
                break;
            case instr6630:
                method11339(exec);
                break;
            case instr6631:
                method5829(exec);
                break;
            case instr6632:
                method12506(exec);
                break;
            case instr6633:
                method2963(exec);
                break;
            case instr6634:
                method14836(exec);
                break;
            case instr6582:
                method3557(exec);
                break;
            case instr6671:
                method4091(exec);
                break;
            case instr6637:
                method12660(exec);
                break;
            case instr6818:
                method15403();
                break;
            case FULLSCREEN:
                method3804(exec);
                break;
            case instr6488:
                method3613();
                break;
            case FULLSCREEN_MODECOUNT:
                method3066(exec);
                break;
            case FULLSCREEN_GETMODE:
                method5769(exec);
                break;
            case FULLSCREEN_LASTMODE:
                method14244(exec);
                break;
            case instr6644:
                method8199(exec);
                break;
            case CHOOSE_RENDER_TYPE:
                method12933(exec);
                break;
            case instr6406:
                method3358(exec);
                break;
            case instr6647:
                method8738(exec);
                break;
            case instr6648:
                method14434(exec);
                break;
            case instr6649:
                method1861(exec);
                break;
            case instr6650:
                method296(exec);
                break;
            case instr6651:
                method2112(exec);
                break;
            case instr6652:
                method4176(exec);
                break;
            case instr6653:
                WindowMode.notifyError();
                break;
            case instr6654:
                method4774(exec);
                break;
            case instr6655:
                method8263(exec);
                break;
            case instr6656:
                method1604(exec);
                break;
            case instr6657:
                method8206(exec);
                break;
            case instr6658:
                method7277();
                break;
            case instr6666:
                method11405(exec);
                break;
            case POP_2_INT:
                method3938(exec);
                break;
            case instr6661:
                method254(exec);
                break;
            case instr6866:
                method584(exec);
                break;
            case instr6416:
                method2171();
                break;
            case instr6410:
                method587();
                break;
            case GETCLIPBOARD:
                method6440(exec);
                break;
            case instr6780:
                method7209(exec);
                break;
            case instr6667:
                method12210(exec);
                break;
            case instr6668:
                method6044(exec);
                break;
            case CHECK_JAVA_VERSION:
                checkJavaVersion(exec);
                break;
            case IS_GAMESCREEN_STATE:
                method6317(exec);
                break;
            case instr6923:
                method15513(exec);
                break;
            case instr6672:
                method8718(exec);
                break;
            case instr6673:
                method7021(exec);
                break;
            case instr6451:
                method13359(exec);
                break;
            case instr6675:
                method4177(exec);
                break;
            case instr6162:
                method1162(exec);
                break;
            case instr6677:
                method1366(exec);
                break;
            case instr6678:
                method1837();
                break;
            case instr6679:
                method16079(exec);
                break;
            case instr6456:
                method1456(exec);
                break;
            case instr6681:
                method1370(exec);
                break;
            case instr6663:
                method4775();
                break;
            case instr6108:
                method1627();
                break;
            case instr6684:
                method1071();
                break;
            case instr6823:
                method7518();
                break;
            case instr6686:
                method1367(exec);
                break;
            case instr6331:
                method8205(exec);
                break;
            case instr6627:
                method11439(exec);
                break;
            case instr6236:
                method8742();
                break;
            case instr6013:
                method3038(exec);
                break;
            case CAM_MODEISFOLLOWPLAYER:
                method12115(exec);
                break;
            case instr6692:
                method6674(exec);
                break;
            case instr6204:
                method6695();
                break;
            case instr6694:
                method3368();
                break;
            case SEND_VERIFY_EMAIL_PACKET:
                sendVerifyEmailPacket(exec);
                break;
            case SEND_SIGNUP_FORM_PACKET:
                sendSignupFormPacket(exec);
                break;
            case instr6697:
                method3172();
                break;
            case instr6698:
                method7704(exec);
                break;
            case instr6699:
                method8303(exec);
                break;
            case instr6700:
                method4417(exec);
                break;
            case instr6323:
                method4161(exec);
                break;
            case instr6702:
                method5119(exec);
                break;
            case instr6703:
                method5205(exec);
                break;
            case instr6774:
                method4826(exec);
                break;
            case instr6641:
                method2107();
                break;
            case instr6305:
                method301(exec);
                break;
            case instr6707:
                method5925(exec);
                break;
            case instr6623:
                method775();
                break;
            case instr6709:
                method6285(exec);
                break;
            case instr6710:
                method2825(exec);
                break;
            case instr6711:
                method12597(exec);
                break;
            case instr6712:
                method453(exec);
                break;
            case instr6642:
                method6280();
                break;
            case instr6714:
                method3119(exec);
                break;
            case instr6576:
                method14669(exec);
                break;
            case instr6101:
                method2153(exec);
                break;
            case instr6717:
                method3120();
                break;
            case instr6718:
                method6169(exec);
                break;
            case instr6719:
                method6396(exec);
                break;
            case instr6720:
                method7744(exec);
                break;
            case instr6721:
                method3748(exec);
                break;
            case instr6722:
                method5047(exec);
                break;
            case instr6609:
                method284(exec);
                break;
            case instr6724:
                method457(exec);
                break;
            case instr6725:
                method7500(exec);
                break;
            case instr6726:
                method2568(exec);
                break;
            case instr6727:
                method14857(exec);
                break;
            case instr6860:
                method3611(exec);
                break;
            case DETAIL_LIGHTDETAIL_HIGH:
                method451(exec);
                break;
            case DETAIL_WATERDETAIL_HIGH:
                method3693(exec);
                break;
            case instr6731:
                method12803(exec);
                break;
            case instr6674:
                setAntiAliasingPreview(exec);
                break;
            case DETAIL_STEREO:
                setMonoStero(exec);
                break;
            case DETAIL_SOUNDVOL:
                setEffectVolume(exec);
                break;
            case DETAIL_MUSICVOL:
                method2554(exec);
                break;
            case DETAIL_BGSOUNDVOL:
                method12718(exec);
                break;
            case DETAIL_REMOVEROOFS_OPTION_OVERRIDE:
                method6397(exec);
                break;
            case DETAIL_PARTICLES:
                method11218(exec);
                break;
            case DETAIL_ANTIALIASING_DEFAULT:
                saveAntiAliasingPreference(exec);
                break;
            case DETAIL_BUILDAREA:
                method5766(exec);
                break;
            case DETAIL_BLOOM:
                setBloom(exec);
                break;
            case DETAIL_CUSTOMCURSOR:
                method1857(exec);
                break;
            case instr6743:
                method3750(exec);
                break;
            case instr6744:
                method1803(exec);
                break;
            case CHANGE_RENDER:
                method6678(exec);
                break;
            case instr6922:
                method6729(exec);
                break;
            case instr6747:
                method2586(exec);
                break;
            case instr6748:
                method3079(exec);
                break;
            case instr6570:
                method4035(exec);
                break;
            case instr6750:
                method3200(exec);
                break;
            case instr6629:
                method4899(exec);
                break;
            case instr6752:
                method1586(exec);
                break;
            case instr6948:
                method452(exec);
                break;
            case instr6754:
                method8156(exec);
                break;
            case instr6285:
                method292(exec);
                break;
            case instr6756:
                method15555(exec);
                break;
            case instr6688:
                method6773(exec);
                break;
            case instr6489:
                method12560(exec);
                break;
            case instr6759:
                method1350(exec);
                break;
            case instr6785:
                method6675(exec);
                break;
            case instr6761:
                method585(exec);
                break;
            case DETAILGET_WATERDETAIL_HIGH:
                getWaterPreference(exec);
                break;
            case instr6060:
                pushFogPreference(exec);
                break;
            case instr6938:
                method6195(exec);
                break;
            case instr6755:
                method7082(exec);
                break;
            case instr6766:
                method4881(exec);
                break;
            case instr6767:
                method3692(exec);
                break;
            case instr6768:
                method13404(exec);
                break;
            case instr5975:
                method4279(exec);
                break;
            case instr6770:
                method2978(exec);
                break;
            case instr6605:
                method3893(exec);
                break;
            case instr6640:
                method15449(exec);
                break;
            case instr6773:
                method7334(exec);
                break;
            case instr6925:
                method13301(exec);
                break;
            case instr5954:
                method1140(exec);
                break;
            case instr6600:
                method3667(exec);
                break;
            case instr6777:
                method6908(exec);
                break;
            case instr6778:
                method7448(exec);
                break;
            case instr6286:
                method4900(exec);
                break;
            case instr6400:
                method2549(exec);
                break;
            case instr6781:
                method1463(exec);
                break;
            case instr6301:
                method5734(exec);
                break;
            case instr6783:
                method7143(exec);
                break;
            case instr6784:
                method6238(exec);
                break;
            case instr6941:
                method4324(exec);
                break;
            case instr6786:
                method6153(exec);
                break;
            case instr6775:
                method934(exec);
                break;
            case instr6097:
                method745(exec);
                break;
            case instr6789:
                method2873(exec);
                break;
            case instr6790:
                method1643(exec);
                break;
            case instr6764:
                method1815(exec);
                break;
            case instr6792:
                method15404(exec);
                break;
            case instr6141:
                method4882(exec);
                break;
            case instr6486:
                method4973(exec);
                break;
            case instr5988:
                method2587(exec);
                break;
            case instr6365:
                method7551(exec);
                break;
            case instr6797:
                method287(exec);
                break;
            case instr6798:
                method4178(exec);
                break;
            case instr6799:
                method5299(exec);
                break;
            case instr6800:
                method1479(exec);
                break;
            case instr6821:
                method6272(exec);
                break;
            case instr6802:
                method4194(exec);
                break;
            case instr6803:
                method15437(exec);
                break;
            case instr6227:
                method6374(exec);
                break;
            case instr6829:
                method11244(exec);
                break;
            case instr6806:
                method5010(exec); //setgamehost
                break;
            case instr6807:
                method1387(exec);
                break;
            case instr6808:
                method7726(exec);
                break;
            case instr6809:
                method5816();
                break;
            case WORLDLIST_PINGWORLDS:
                setPingWorlds(exec);
                break;
            case instr6589:
                method3039(exec);
                break;
            case IF_DEBUG_GETOPENIFCOUNT:
                method927(exec);
                break;
            case IF_DEBUG_GETOPENIFID:
                method11613(exec);
                break;
            case IF_DEBUG_GETNAME:
                method7006(exec);
                break;
            case IF_DEBUG_GETCOMCOUNT:
                method5904(exec);
                break;
            case IF_DEBUG_GETCOMNAME:
                method3341(exec);
                break;
            case IF_DEBUG_GETSERVERTRIGGERS:
                method8724(exec);
                break;
            case instr6386:
                method7187();
                break;
            case instr6659:
                method11440(exec);
                break;
            case instr6794:
                method11128(exec);
                break;
            case instr6278:
                method3562(exec);
                break;
            case instr6890:
                method15387(exec);
                break;
            case instr6350:
                method8021(exec);
                break;
            case instr6824:
                method6940(exec);
                break;
            case instr6825:
                method11340(exec);
                break;
            case instr6826:
                method3714(exec);
                break;
            case instr6827:
                method548(exec);
                break;
            case instr6828:
                method3911(exec);
                break;
            case instr6920:
                method1138(exec);
                break;
            case instr5955:
                method504(exec);
                break;
            case MEC_TEXT:
                method15616(exec);
                break;
            case MEC_SPRITE:
                method1255(exec);
                break;
            case MEC_TEXTSIZE:
                method3755(exec);
                break;
            case MEC_CATEGORY:
                method290(exec);
                break;
            case MEC_PARAM:
                getWorldMapParam(exec);
                break;
            case USERDETAIL_QUICKCHAT:
                method5832(exec);
                break;
            case USERDETAIL_LOBBY_MEMBERSHIP:
                method1499(exec);
                break;
            case USERDETAIL_LOBBY_RECOVERYDAY:
                method14655(exec);
                break;
            case USERDETAIL_LOBBY_UNREADMESSAGES:
                method7915(exec);
                break;
            case USERDETAIL_LOBBY_LASTLOGINDAY:
                method12859(exec);
                break;
            case USERDETAIL_LOBBY_LASTLOGINADDRESS:
                method7462(exec);
                break;
            case USERDETAIL_LOBBY_EMAILSTATUS:
                method3894(exec);
                break;
            case USERDETAIL_LOBBY_CCEXPIRY:
                method1357(exec);
                break;
            case USERDETAIL_LOBBY_GRACEEXPIRY:
                method1855(exec);
                break;
            case USERDETAIL_LOBBY_DOBREQUESTED:
                method12352(exec);
                break;
            case USERDETAIL_DOB:
                method586(exec);
                break;
            case USERDETAIL_LOBBY_MEMBERSSTATS:
                method13467(exec);
                break;
            case USERDETAIL_LOBBY_PLAYAGE:
                method8869(exec);
                break;
            case USERDETAIL_LOBBY_JCOINS_BALANCE:
                getJcoins(exec);
                break;
            case USERDETAIL_LOBBY_LOYALTY_ENABLED:
                method1351(exec);
                break;
            case USERDETAIL_LOBBY_LOYALTY_BALANCE:
                method6046(exec);
                break;
            case instr6852:
                enterDisplaymode(exec);
                break;
            case AUTOSETUP_SETHIGH:
                method3233();
                break;
            case AUTOSETUP_SETMEDIUM:
                method7773();
                break;
            case AUTOSETUP_SETLOW:
                method15306();
                break;
            case AUTOSETUP_SETMIN:
                method6239();
                break;
            case instr6857:
                method4776();
                break;
            case instr6858:
                computeGraphicsMode();
                break;
            case instr6859:
                method481(exec);
                break;
            case instr6046:
                method6290(exec);
                break;
            case instr6861:
                method12561(exec);
                break;
            case instr6862:
                method5199(exec);
                break;
            case instr6863:
                method14818(exec);
                break;
            case instr6838:
                method3170(exec);
                break;
            case instr6680:
                method7962();
                break;
            case instr6476:
                method14839();
                break;
            case instr6867:
                method15406(exec);
                break;
            case instr6868:
                method572(exec);
                break;
            case instr6869:
                method6350(exec);
                break;
            case instr6058:
                method2597(exec);
                break;
            case DETAILCANMOD_GROUNDDECOR:
                method1862(exec);
                break;
            case DETAILCANMOD_CHARSHADOWS:
                method15371(exec);
                break;
            case DETAILCANMOD_SPOTSHADOWS:
                method4989(exec);
                break;
            case DETAILCANMOD_WATERDETAIL:
                method2588(exec);
                break;
            case DETAILCANMOD_ANTIALIASING:
                method8343(exec);
                break;
            case DETAILCANMOD_PARTICLES:
                method5767(exec);
                break;
            case DETAILCANMOD_BUILDAREA:
                loadMemoryMode(exec);
                break;
            case DETAILCANMOD_BLOOM:
                method6072(exec);
                break;
            case DETAILCANMOD_GROUNDBLENDING:
                method8844(exec);
                break;
            case DETAILCANMOD_TEXTURING:
                method743(exec);
                break;
            case DETAILCANMOD_MAXSCREENSIZE:
                method7552(exec);
                break;
            case DETAILCANMOD_FOG:
                method5065(exec);
                break;
            case DETAILCANMOD_TOOLKIT_DEFAULT:
                method7420(exec);
                break;
            case DETAILCANMOD_TOOLKIT:
                method3781(exec);
                break;
            case DETAILCANMOD_SKYBOXES:
                method1574(exec);
                break;
            case DETAILCANSET_GROUNDDECOR:
                method11341(exec);
                break;
            case DETAILCANSET_CHARSHADOWS:
                method15551(exec);
                break;
            case DETAILCANSET_SPOTSHADOWS:
                method11351(exec);
                break;
            case DETAILCANSET_WATERDETAIL:
                method3713(exec);
                break;
            case DETAILCANSET_ANTIALIASING:
                method7218(exec);
                break;
            case DETAILCANSET_PARTICLES:
                method6279(exec);
                break;
            case DETAILCANSET_BUILDAREA:
                method14649(exec);
                break;
            case DETAILCANSET_BLOOM:
                method4413(exec);
                break;
            case DETAILCANSET_GROUNDBLENDING:
                method1352(exec);
                break;
            case DETAILCANSET_TEXTURING:
                method1572(exec);
                break;
            case DETAILCANSET_MAXSCREENSIZE:
                method12596(exec);
                break;
            case DETAILCANSET_FOG:
                method15407(exec);
                break;
            case DETAILCANSET_TOOLKIT_DEFAULT:
                method1968(exec);
                break;
            case DETAILCANSET_RENDERER:
                method2108(exec);
                break;
            case DETAILCANSET_SKYBOXES:
                method4867(exec);
                break;
            case instr6388:
                method543(exec);
                break;
            case instr6902: //get entity interface id?
                method261(exec);
                break;
            case GET_ENTITY_SAY:
                method6281(exec);
                break;
            case GET_DISPLAYNAME_WITHEXTRAS:
                method6730(exec);
                break;
            case instr6905:
                method5814(exec);
                break;
            case instr6906:
                method12586(exec);
                break;
            case GET_NPC_NAME:
                method12915(exec);
                break;
            case instr6908:
                method1784(exec);
                break;
            case GET_ENTITY_SCREEN_POSITION:
                pushEntityHintPosition(exec);
                break;
            case IF_GET_GAMESCREEN:
                method14606(exec);
                break;
            case instr6911:
                method2867(exec);
                break;
            case GET_NPC_STAT:
                method2639(exec);
                break;
            case IS_NPC_ACTIVE:
                method300(exec);
                break;
            case IS_NPC_VISIBLE:
                method3740(exec);
                break;
            case IS_TARGETED_ENTITY:
                method3344(exec);
                break;
            case IF_DELETEALL:
                method4830(exec);
                break;
            case NPC_TYPE:
                method2103(exec);
                break;
            case GET_OBJECT_SCREEN_POSITION:
                pushGameobjectHintLocation(exec);
                break;
            case GET_ITEM_SCREEN_POSITION:
                pushGroundItemHintPosition(exec);
                break;
            case GET_OBJECT_OVERLAY_HEIGHT:
                method6042(exec);
                break;
            case GET_ITEM_OVERLAY_HEIGHT:
                method3769(exec);
                break;
            case GET_OBJECT_BOUNDING_BOX:
                method7332(exec);
                break;
            case GET_ITEM_BOUNDING_BOX:
                method798(exec);
                break;
            case GET_ENTITY_BOUNDING_BOX:
                method3067(exec);
                break;
            case BUG_REPORT:
                method5298(exec);
                break;
            case ARRAY_SORT:
                method8700(exec);
                break;
            case QUEST_GETNAME:
                method2101(exec);
                break;
            case QUEST_GETSORTNAME:
                method5736(exec);
                break;
            case QUEST_TYPE:
                method356(exec);
                break;
            case QUEST_GETDIFFICULTY:
                method7434(exec);
                break;
            case QUEST_GETMEMBERS:
                method7461(exec);
                break;
            case QUEST_POINTS:
                method1799(exec);
                break;
            case QUEST_QUESTREQ_COUNT:
                method5016(exec);
                break;
            case QUEST_QUESTREQ:
                method777(exec);
                break;
            case QUEST_QUESTREQ_MET:
                method4314(exec);
                break;
            case QUEST_POINTSREQ:
                method3753(exec);
                break;
            case QUEST_POINTSREQ_MET:
                method466(exec);
                break;
            case QUEST_STATREQ_COUNT:
                method2953(exec);
                break;
            case QUEST_STATREQ_STAT:
                method2092(exec);
                break;
            case QUEST_STATREQ_LEVEL:
                method2841(exec);
                break;
            case QUEST_STATREQ_MET:
                method14841(exec);
                break;
            case QUEST_VARPREQ_COUNT:
                method868(exec);
                break;
            case QUEST_VARPREQ_DESC:
                method3553(exec);
                break;
            case QUEST_VARPREQ_MET:
                method1067(exec);
                break;
            case QUEST_VARBITREQ_COUNT:
                method7083(exec);
                break;
            case QUEST_VARBITREQ_DESC:
                method11592(exec);
                break;
            case QUEST_VARBITREQ_MET:
                method12587(exec);
                break;
            case QUEST_ALLREQMET:
                method4556(exec);
                break;
            case QUEST_STARTED:
                method8216(exec);
                break;
            case QUEST_FINISHED:
                method5290(exec);
                break;
            case QUEST_PARAM:
                method4249(exec);
                break;
            default:
                throw new RuntimeException();
        }

    }

    public static void ccSetFront(boolean bool, CS2Executor executor) {
        CS2Interface underlaydefinition3 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions4 = underlaydefinition3.comp;
        Interface interface5 = underlaydefinition3.inter;
        if (bool) {
            Interface.method7554(interface5, icomponentdefinitions4);
        } else {
            Interface.method3710(interface5, icomponentdefinitions4);
        }
    }

    public static void ifSetFront(boolean bool, CS2Executor executor) {
        int i_3 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions4 = Component.fromHash(i_3);
        Interface interface5 = Interface.INTERFACES[i_3 >> 16];
        if (bool) {
            Interface.method7554(interface5, icomponentdefinitions4);
        } else {
            Interface.method3710(interface5, icomponentdefinitions4);
        }

    }

    public static void method5755(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.aBool7323 ? 1 : 0;
    }

    public static void method5756(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method1454(icomponentdefinitions3, executor);
    }

    public static void method5758(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        method7927(icomponentdefinitions3, executor);
    }

    public static void method3546(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method5017(icomponentdefinitions3, executor);
    }

    public static void method3547(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        int i_4 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = string_2.indexOf(string_3, i_4);
    }

    //Deprecated jsobject applet BS
    public static void method2597(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
    }

    public static void loadClanVarbit(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        ClanVarType class4053 = TypeLists.CLAN_VAR.list(i_2);
        if (class4053.aChar4832 != 1) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
        Integer integer4 = (Integer) ClanVarTypeList.CLAN_VARS[class4053.baseVar];
        if (integer4 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            int i_5 = class4053.endBit == 31 ? -1 : (1 << class4053.endBit + 1) - 1;
            executor.intStack[++executor.intStackPtr - 1] = (integer4.intValue() & i_5) >>> class4053.startBit;
        }
    }

    public static void loadClanSettingVarLong(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        Long long_3 = executor.currentClanSettings.getLongVar(client.CURRENT_GAME.id << 16 | i_2);
        long long_4;
        if (long_3 == null) {
            long_4 = -1L;
        } else {
            long_4 = long_3.longValue();
        }
        executor.longStack[++executor.longStackPtr - 1] = long_4;
    }

    public static void loadClanSettingVarString(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        String string_3 = executor.currentClanSettings.getStringVar(client.CURRENT_GAME.id << 16 | i_2);
        String string_4;
        if (string_3 == null) {
            string_4 = "";
        } else {
            string_4 = string_3;
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_4;
    }

    public static void method11218(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.particles, i_2);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void gotoInstr(CS2Executor executor) {
        executor.instrPtr += executor.intOpValues[executor.instrPtr];
    }

    public static void method1255(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        MECType worldmapareadefs3 = TypeLists.MEC.list(i_2);
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3.defaultIconArchive;
    }

    public static void method7518() {
        StaticMethods.method8146();
    }

    public static void method7520(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method12209(icomponentdefinitions3, executor);
    }

    public static void getWaterPreference(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.water.getValue() == 2 ? 1 : 0;
    }

    public static void ccSetOpBase(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setOpBase(icomponentdefinitions3, executor);
    }

    public static void getMyPlayerPos(CS2Executor executor) {
        byte b_2 = PlayerUpdate.MY_PLAYER.plane;
        Vector3 vector33 = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
        CoordGrid coordgrid4 = Resource.mapRegion.getBaseCoordinates();
        int i_5 = ((int) vector33.x >> 9) + coordgrid4.x;
        int i_6 = ((int) vector33.z >> 9) + coordgrid4.y;
        executor.intStack[++executor.intStackPtr - 1] = (i_5 << 14) + i_6 + (b_2 << 28);
    }

    public static void method15510(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method506(icomponentdefinitions3, interface4, executor);
    }

    public static void method15512(CS2Executor executor) {
        int index = executor.intStack[--executor.intStackPtr];
        ChatLine chatLine = ChatBox.getLine(index);
        int type = -1;
        if (chatLine != null) {
            type = chatLine.type.getValue();
        }
        executor.intStack[++executor.intStackPtr - 1] = type;
    }

    public static void method15513(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = GameShell.getPreferenceFile().toString();
    }

    public static void method15514(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        ifSetColor(icomponentdefinitions3, interface4, executor);
    }

    public static void method2865(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method11337(icomponentdefinitions3, interface4, executor);
    }

    public static void method2867(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentEntity.getIconHeight();
    }

    public static void method2868(CS2Executor executor) {
        if (ColorImageCache.CS2_QUERY_RESULTS != null && StaticElements.CS2_QUERY_RESULT_IDX < CS2_QUERY_RESULTS_LEN) {
            executor.intStack[++executor.intStackPtr - 1] = ColorImageCache.CS2_QUERY_RESULTS[++StaticElements.CS2_QUERY_RESULT_IDX - 1] & 0xffff;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        }
    }

    public static void method1366(CS2Executor executor) {
        executor.intStackPtr -= 6;
        int i_2 = executor.intStack[executor.intStackPtr];
        if (i_2 >= 2) {
            throw new RuntimeException();
        } else {
            client.anInt7277 = i_2;
            int i_3 = executor.intStack[executor.intStackPtr + 1];
            if (i_3 + 1 >= client.anIntArrayArrayArray7302[client.anInt7277].length >> 1) {
                throw new RuntimeException();
            } else {
                client.anInt7279 = i_3;
                client.anInt7256 = 0;
                client.anInt7327 = executor.intStack[executor.intStackPtr + 2];
                client.anInt7182 = executor.intStack[executor.intStackPtr + 3];
                int i_4 = executor.intStack[executor.intStackPtr + 4];
                if (i_4 >= 2) {
                    throw new RuntimeException();
                } else {
                    client.anInt7448 = i_4;
                    int i_5 = executor.intStack[executor.intStackPtr + 5];
                    if (i_5 + 1 >= client.anIntArrayArrayArray7302[client.anInt7448].length >> 1) {
                        throw new RuntimeException();
                    } else {
                        client.anInt7280 = i_5;
                        Camera.cameraCalculationType = 3;
                        StaticMethods.anInt833 = -1;
                        PingRequester.anInt5864 = -1;
                    }
                }
            }
        }
    }

    public static void method1367(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 < 0) {
            i_2 = 0;
        }
        client.anInt7271 = i_2 + client.anInt7270;
    }

    public static void method1370(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = (int) Camera.camAngleY >> 3;
    }

    public static void method12572(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapElement class282Sub50Sub63 = WorldMapSub1.getStaticElement();
        if (class282Sub50Sub63 != null) {
            boolean bool4 = class282Sub50Sub63.method14775(i_2 >> 14 & 0x3fff, i_2 & 0x3fff, CS2Executor.anIntArray5900);
            if (bool4) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900[1];
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900[2];
            } else {
                executor.intStack[++executor.intStackPtr - 1] = -1;
                executor.intStack[++executor.intStackPtr - 1] = -1;
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.intStack[++executor.intStackPtr - 1] = -1;
        }
    }

    public static void setPingWorlds(CS2Executor executor) {
        if (client.gameState == GameState.UNK_0) {
            WorldConnection.PING_WORLDS = executor.intStack[--executor.intStackPtr] == 1;
        }
    }

    public static void method8714(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method2960(icomponentdefinitions3, interface4, executor);
    }

    public static void method8715(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        method3251(icomponentdefinitions3, executor);
    }

    public static void method8716(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int i_3 = client.GRAND_EXCHANGE_SLOTS[i_2].method5908();
        executor.intStack[++executor.intStackPtr - 1] = i_3 == 2 ? 1 : 0;
    }

    public static void method8717(CS2Executor executor) {
        String string_2;
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER.username != null) {
            string_2 = PlayerUpdate.MY_PLAYER.getUsernameWithTitle();
        } else {
            string_2 = "";
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2;
    }

    public static void method8718(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.filePicker = new DirectoryPicker(string_2);
    }

    public static void scale(CS2Executor executor) {
        executor.intStackPtr -= 3;
        long long_2 = executor.intStack[executor.intStackPtr];
        long long_4 = executor.intStack[executor.intStackPtr + 1];
        long long_6 = executor.intStack[executor.intStackPtr + 2];
        executor.intStack[++executor.intStackPtr - 1] = (int) (long_6 * long_2 / long_4);
    }

    public static void arrayLoad(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        int i_3 = executor.intStack[--executor.intStackPtr];
        if (i_3 >= 0 && i_3 < executor.globalArrayLengths[i_2]) {
            executor.intStack[++executor.intStackPtr - 1] = executor.globalArrays[i_2][i_3];
        } else {
            throw new RuntimeException();
        }
    }

    public static void method14679(CS2Executor executor) {
        method1573(executor);
    }

    public static void method3779(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings.bannedUserNames[i_2];
    }

    public static void method3780(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        QuickChatCategoryType class282Sub50Sub144 = TypeLists.QUICK_CHAT_CATEGORY.list(i_2);
        int i_5 = class282Sub50Sub144.subCategories[i_3];
        executor.intStack[++executor.intStackPtr - 1] = i_5;
    }

    public static void method3781(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.toolkit.visible() ? 1 : 0;
    }

    public static void method3783(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        int i_4 = -1;
        if (chatline3 != null) {
            i_4 = chatline3.quickchatMessageId;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
    }

    public static void method3553(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).varpRequirementNames[executor.intStack[executor.intStackPtr - 1]];
        executor.intStackPtr -= 2;
    }

    public static void method3554(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method8754(icomponentdefinitions3, executor);
    }

    public static void method3555(CS2Executor executor) {
        SongPlayer.playSong(executor.intStack[--executor.intStackPtr], 255, 50);
    }

    public static void method3556(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapElement class282Sub50Sub63 = WorldMapSub1.getStaticElement();
        if (class282Sub50Sub63 != null) {
            boolean bool4 = class282Sub50Sub63.method14778(i_2 >> 28 & 0x3, i_2 >> 14 & 0x3fff, i_2 & 0x3fff, CS2Executor.anIntArray5900);
            if (bool4) {
                StaticMethods.method14263(CS2Executor.anIntArray5900[1], CS2Executor.anIntArray5900[2]);
            }
        }
    }

    public static void method3557(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Node node3 = WorldMapSub1.aClass4658025.get(i_2);
        executor.intStack[++executor.intStackPtr - 1] = node3 != null ? 1 : 0;
    }

    public static void method466(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).hasQuestpointRequirement(VarProviders.PLAYER_VAR_PROVIDER) ? 1 : 0;
    }

    public static void longLessThan(CS2Executor executor) {
        executor.longStackPtr -= 2;
        if (executor.longStack[executor.longStackPtr] < executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method468(CS2Executor executor) {
        BufferedConnectionContext class1842 = client.getConnectionContext();
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842.isaac);
        tcpmessage3.buffer.writeByte(0);
        int i_4 = tcpmessage3.buffer.currentPosition;
        tcpmessage3.buffer.writeByte(2);
        tcpmessage3.buffer.writeShort(executor.currentQuickChatMessage.id);
        executor.currentQuickChatMessage.message.method14896(tcpmessage3.buffer, executor.currentQuickChatMessage.anIntArray4046);
        tcpmessage3.buffer.writeIndex(tcpmessage3.buffer.currentPosition - i_4);
        class1842.queuePacket(tcpmessage3);
    }

    public static void method471(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = executor.clanChannel.getPlayersSortedByName()[executor.intStack[executor.intStackPtr - 1]];
    }

    public static void multiply(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_3 * i_2;
    }

    public static void method15387(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(4, i_2 << 16 | i_3, i_4, "");
    }

    public static void branchEq1(CS2Executor executor) {
        if (executor.intStack[--executor.intStackPtr] == 1) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void branchEq0(CS2Executor executor) {
        if (executor.intStack[--executor.intStackPtr] == 0) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method1586(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 > 255 || i_2 < 0) {
            i_2 = 0;
        }
        if (i_2 != PreferenceManager.preferences.gametipStartStagePreference.getValue()) {
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.gametipStartStagePreference, i_2);
            PreferenceManager.savePreferences();
            client.preferencesSavedOnServer = false;
        }
    }

    public static void enterDisplaymode(CS2Executor executor) {
        int hraphicsPreference = GameShell.calculateGraphics();
        executor.intStack[++executor.intStackPtr - 1] = lastEnteredGraphicsMode = PreferenceManager.preferences.activeToolkit.getValue();
        executor.intStack[++executor.intStackPtr - 1] = hraphicsPreference;
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    //Deprecated jsobject applet BS
    public static void method3170(CS2Executor executor) {
        boolean bool2 = false;
        int param = Integer.valueOf(executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = bool2 ? 1 : 0;
    }

    public static void method3171(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        method3601(icomponentdefinitions3, executor);
    }

    public static void method3172() {
        GameState.method12426();
    }

    public static void method6279(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.particles.canModify(i_2);
    }

    //Deprecated jsobject applet BS
    public static void method6280() {
        client.aBool7189 = true;
    }

    public static void method6281(CS2Executor executor) {
        HintDetails class1632 = executor.currentEntity.getHintDetails();
        if (class1632 == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = class1632.getText();
        }
    }

    public static void method15616(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        MECType worldmapareadefs3 = TypeLists.MEC.list(i_2);
        if (worldmapareadefs3.areaName == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = worldmapareadefs3.areaName;
        }
    }

    public static void method6016(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method14892(icomponentdefinitions3, executor);
    }

    public static void setCCFont(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setIfFont(icomponentdefinitions3, interface4, executor);
    }

    public static void method8263(CS2Executor executor) {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false);
        }
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        boolean bool3 = executor.intStack[--executor.intStackPtr] == 1;
        String string_4 = ServerEnvironment.getUrl() + string_2;
        Browser.openOpenjs(string_4, bool3, client.aBool7158);
    }

    public static void getNPCParam(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault4 = TypeLists.PARAMS.list(i_3);
        if (attributedefault4.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.NPC.list(i_2).getCS2Param(i_3, attributedefault4.typeName);
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.NPC.list(i_2).getCs2Value(i_3, attributedefault4.defaultInt);
        }
    }

    public static void addPercent(CS2Executor executor) {
        executor.intStackPtr -= 2;
        long long_2 = executor.intStack[executor.intStackPtr];
        long long_4 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = (int) (long_2 + long_2 * long_4 / 100L);
    }

    public static void method3892(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method3078(icomponentdefinitions3, executor);
    }

    public static void method3893(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.mapSize.getValue();
    }

    public static void method3894(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.EMAIL_STATUS;
    }

    public static void getPlayerGender(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.MY_PLAYER.playerAppearance != null && PlayerUpdate.MY_PLAYER.playerAppearance.female ? 1 : 0;
    }

    public static void method4881(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.soundEffectVolume.getValue();
    }

    public static void method4882(CS2Executor executor) {
        AbstractToolkit.calculateGameArea(0, 0, client.GAME_SCREEN_INTERFACE.width, client.GAME_SCREEN_INTERFACE.height, false);
        executor.intStack[++executor.intStackPtr - 1] = client.gameAreaWidth;
        executor.intStack[++executor.intStackPtr - 1] = client.gameAreaHeight;
    }

    public static void storeVarc(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        VarcTypeList.VARC_INT[i_2] = executor.intStack[--executor.intStackPtr];
        PulseEventQueue.delayedClientvarcUpdate(i_2);
        client.clientIsActive |= VarcTypeList.VARC_BOOL[i_2];
    }

    public static void method5099(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method4159(icomponentdefinitions3, executor);
    }

    public static void method5100(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.getLineCount();
    }

    public static void method13408(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method1642(icomponentdefinitions3, executor);
    }

    public static void method12859(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.LASTLOGINLDAY;
    }

    public static void method2978(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.antiAliasing.getValue();
    }

    public static void method1853(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        if (i_3 == -1) {
            throw new RuntimeException();
        } else {
            EnumType enumdefinitions5 = TypeLists.ENUM.list(i_3);
            if (i_2 != enumdefinitions5.valueType) {
                throw new RuntimeException();
            } else {
                int[] ints_6 = enumdefinitions5.getValueReferences(Integer.valueOf(i_4));
                int i_7 = 0;
                if (ints_6 != null) {
                    i_7 = ints_6.length;
                }
                executor.intStack[++executor.intStackPtr - 1] = i_7;
            }
        }
    }

    public static void method1854(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        int i_3 = executor.intStack[--executor.intStackPtr];
        FontMetrics fontmetrics4 = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_3);
        assert fontmetrics4 != null;
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics4.getTextWidth(string_2, StaticMedia.nameIconSprites);
    }

    public static void method1855(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = FileOnDisk.anInt5357;
    }

    public static void stringLength(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        if (string_2 != null) {
            executor.intStack[++executor.intStackPtr - 1] = string_2.length();
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method1857(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.customCursor, executor.intStack[--executor.intStackPtr] != 0 ? 1 : 0);
        PreferenceManager.savePreferences();
    }

    public static void method7643(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        int i_4 = -1;
        int i_5 = -1;
        assert icomponentdefinitions3 != null;
        InteractiveComponentSprite class1196 = icomponentdefinitions3.addInteractiveSprite(Renderers.activeToolkit);
        if (class1196 != null) {
            i_4 = class1196.width;
            i_5 = class1196.height;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
        executor.intStack[++executor.intStackPtr - 1] = i_5;
    }

    public static void method6290(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (i_2 != -1) {
            if (i_3 > 255) {
                i_3 = 255;
            } else if (i_3 < 0) {
                i_3 = 0;
            }
            VorbisSoundManager.method1575(i_2, i_3, false);
        }
    }

    public static void setCCText(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setCompText(icomponentdefinitions3, interface4, executor);
    }

    public static void method13494(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        if (client.aString3643 != null && client.aString3643.equalsIgnoreCase(string_2)) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method14857(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.characterShadows, executor.intStack[--executor.intStackPtr] == 1 ? 1 : 0);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void emailValidationAddNewAddress(CS2Executor executor) {
        --executor.stringStackPtr;
        executor.intStackPtr -= 3;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        boolean bool3 = executor.intStack[executor.intStackPtr] == 1;
        boolean bool4 = executor.intStack[executor.intStackPtr + 1] == 1;
        boolean bool5 = executor.intStack[executor.intStackPtr + 2] == 1;
        TcpPacket tcpmessage6 = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_ADD_NEW_ADDRESS, client.LOBBY_CONNECTION_CONTEXT.isaac);
        tcpmessage6.buffer.writeShort(TextUtils.getLength(string_2) + 1);
        tcpmessage6.buffer.writeString(string_2);
        int i_7 = 0;
        if (bool3) {
            i_7 |= 0x1;
        }
        if (bool4) {
            i_7 |= 0x2;
        }
        if (bool5) {
            i_7 |= 0x4;
        }
        tcpmessage6.buffer.writeByte(i_7);
        client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage6);
    }

    public static void method5829(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapSub1.method2094(i_2);
    }

    public static void randomInclusive(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = (int) (Math.random() * (i_2 + 1));
    }

    public static void method5832(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.USERDETAIL_QUICKCHAT && !client.VERIFIED_EMAIL_ADDRESS ? 1 : 0;
    }

    public static void method5833(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method13468(icomponentdefinitions3, interface4, executor);
    }

    public static void method12659(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method7142(icomponentdefinitions4, executor);
    }

    public static void method12660(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = i_3 >> 14 & 0x3fff;
        int i_5 = i_3 & 0x3fff;
        int i_6 = WorldMapCategory.method12702(i_2, i_4, i_5);
        if (i_6 < 0) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = i_6;
        }
    }

    public static void method13410(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel.numPlayers;
    }

    public static void method12925(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spritePitch;
    }

    public static void method1864(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        if (icomponentdefinitions3.opBase == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3.opBase;
        }
    }

    public static void sendJoinFriendchatPacket() {
        PacketEncoder.sendJoinFriendchatPacket();
    }

    public static void method741(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method5455(icomponentdefinitions3, executor);
    }

    public static void method742(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        QuickChatCategoryType class282Sub50Sub143 = TypeLists.QUICK_CHAT_CATEGORY.list(i_2);
        if (class282Sub50Sub143.subCategories == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub143.subCategories.length;
        }
    }

    public static void method743(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.textures.playingRunescape() ? 1 : 0;
    }

    public static void method745(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.safemodeForcedDuringStartup ? 1 : 0;
    }

    public static void substring(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.intStackPtr -= 2;
        int i_3 = executor.intStack[executor.intStackPtr];
        int i_4 = executor.intStack[executor.intStackPtr + 1];
        executor.stringStack[++executor.stringStackPtr - 1] = string_2.substring(i_3, i_4);
    }

    public static void method8843(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method6288(icomponentdefinitions3, executor);
    }

    public static void method8844(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.goundBlending.playingRunescape() ? 1 : 0;
    }

    public static void getAnimationParam(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault4 = TypeLists.PARAMS.list(i_3);
        if (attributedefault4.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.SEQ.list(i_2).getCs2Object(i_3, attributedefault4.typeName);
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.SEQ.list(i_2).getCs2Integer(i_3, attributedefault4.defaultInt);
        }
    }

    public static void method3676(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method7889(icomponentdefinitions3, executor);
    }

    public static void longGreaterThan(CS2Executor executor) {
        executor.longStackPtr -= 2;
        if (executor.longStack[executor.longStackPtr] > executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    //Deprecated jsobject applet BS
    public static void method6350(CS2Executor executor) {
        boolean bool2 = false;
        String string_3 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = bool2 ? 1 : 0;
    }

    public static void method6799(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.AFFILIATE;
    }

    public static void method6800(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method12630(icomponentdefinitions3);
    }

    public static void method15475(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method5291(icomponentdefinitions3, executor);
    }

    public static void method3079(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.textures, executor.intStack[--executor.intStackPtr] == 1 ? 1 : 0);
        PreferenceManager.savePreferences();
        GraphicsCache.resetGraphicCaches();
        client.preferencesSavedOnServer = false;
    }

    public static void ccSetItem(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setItemIFComp(icomponentdefinitions3, interface4, false, Component.StackMode.WHEN_MULTIPLE, executor);
    }

    public static void method15399(CS2Executor executor) {
        PacketEncoder.method4167((String) executor.stringStack[--executor.stringStackPtr]);
    }

    public static void method6796(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.y;
    }

    public static void method6797(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int i_3 = client.GRAND_EXCHANGE_SLOTS[i_2].method5908();
        executor.intStack[++executor.intStackPtr - 1] = i_3 == 0 ? 1 : 0;
    }

    public static void method8199(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = WindowMode.getWindowMode();
    }

    public static void method7006(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (Interface.INTERFACES[i_2] == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        } else {
            String string_3 = Interface.INTERFACES[i_2].components[0].name;
            if (string_3 == null) {
                executor.stringStack[++executor.stringStackPtr - 1] = "";
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = string_3.substring(0, string_3.indexOf(58));
            }
        }
    }

    public static void storeString(CS2Executor executor) {
        executor.objectLocals[executor.intOpValues[executor.instrPtr]] = executor.stringStack[--executor.stringStackPtr];
    }

    public static void method4863(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        ifSetNPCHead(icomponentdefinitions3, interface4, executor);
    }

    public static void method4864(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method15474(icomponentdefinitions3);
    }

    public static void method4865(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method1494(icomponentdefinitions3, executor);
    }

    public static void method4867(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.skyboxes.canModify(i_2);
    }

    public static void storeVarcString(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        VarcTypeList.VARC_STRING[i_2] = (String) executor.stringStack[--executor.stringStackPtr];
        PulseEventQueue.delayedVarcstringUpdate(i_2);
    }

    public static void hideComp(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method786(icomponentdefinitions3, interface4, executor);
    }

    public static void method5302(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method1852(icomponentdefinitions3, interface4, executor);
    }

    public static void method11125(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.scrollX;
    }

    public static void method11126(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method1455(icomponentdefinitions3, interface4, executor);
    }

    public static void method11128(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(2, i_2 << 16 | i_3, i_4, "");
    }

    public static void method5901(CS2Executor executor) {
        executor.intStackPtr -= 4;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        int i_5 = executor.intStack[executor.intStackPtr + 3];
        EnumType enumDefinitions = TypeLists.ENUM.list(i_4);
        if (i_2 == enumDefinitions.keyType && i_3 == enumDefinitions.valueType) {
            if (i_3 == 's') {
                executor.stringStack[++executor.stringStackPtr - 1] = enumDefinitions.getStringValue(i_5);
            } else {
                executor.intStack[++executor.intStackPtr - 1] = enumDefinitions.getIntValue(i_5);
            }
        } else {
            throw new RuntimeException(i_4 + " " + i_5);
        }
    }

    public static void method5902(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.talkRank;
    }

    public static void method5904(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (Interface.INTERFACES[i_2] == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Interface.INTERFACES[i_2].components.length;
        }
    }

    public static void method6668(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int ifComp = executor.intStack[executor.intStackPtr];
        int slotId = executor.intStack[executor.intStackPtr + 1];
        CS2Interface inter;
        if (executor.aBool7022) {
            inter = executor.hookedInterface2;
        } else {
            inter = executor.hookedInterface1;
        }
        executor.intStack[++executor.intStackPtr - 1] = slotId != -1 && inter.setChild(ifComp, slotId) ? 1 : 0;
    }

    public static void method6669(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method4280(icomponentdefinitions3, executor);
    }

    public static void method6670(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        method14680(icomponentdefinitions3, executor);
    }

    public static void method6674(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        LoginManager.directLogin(string_2, string_3);
    }

    public static void method6675(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.sceneryShadows.getValue();
    }

    public static void method6449(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.mediaType == MediaType.RAW_MODEL ? icomponentdefinitions3.primaryMediaId : -1;
    }

    public static void method6452(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method5291(icomponentdefinitions3, executor);
    }

    public static void method4030(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method3365(icomponentdefinitions3, executor);
    }

    public static void method4031(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder.leftButtonDown() ? 1 : 0;
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder.rightButtonDown() ? 1 : 0;
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder.middleButtonDown() ? 1 : 0;
    }

    public static void method4033(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method14653(icomponentdefinitions3, executor);
    }

    public static void method4035(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.maxScreenSize, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
        client.needsResize = true;
    }

    public static void min(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = Math.min(i_2, i_3);
    }

    public static void longGreaterOrEqual(CS2Executor executor) {
        executor.longStackPtr -= 2;
        if (executor.longStack[executor.longStackPtr] >= executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method7021(CS2Executor executor) {
        boolean bool2 = false;
        String path = "";
        if (executor.filePicker != null && executor.filePicker.fileChoserOpen()) {
            File directory = executor.filePicker.getPickedDirectory();
            if (directory != null) {
                path = directory.getPath();
            }
            bool2 = true;
            executor.filePicker = null;
        }
        executor.intStack[++executor.intStackPtr - 1] = bool2 ? 1 : 0;
        executor.stringStack[++executor.stringStackPtr - 1] = path;
    }

    public static void method13301(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.idleAnimations.getValue();
    }

    public static void method6773(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.idleAnimations.getValue();
    }

    public static void method15551(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.characterShadows.canModify(i_2);
    }

    public static void method4277(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.containerItemId;
    }

    public static void method4278(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        FriendList.remove(string_2);
    }

    public static void method4279(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ParticleSystemManager.method5206();
    }

    public static void method4283(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method11221(icomponentdefinitions3, executor);
    }

    public static void setItem(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (PlayerUpdate.MY_PLAYER.playerAppearance != null) {
            PlayerUpdate.MY_PLAYER.playerAppearance.addAppearanceItem(i_2, i_3, TypeLists.OBJ);
        }
    }

    public static void method775() {
        client.logout(false);
    }

    public static void method777(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).questPrerequisiteIds[executor.intStack[executor.intStackPtr - 1]];
        --executor.intStackPtr;
    }

    public static void method779(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        int i_4 = executor.intStack[--executor.intStackPtr];
        --i_4;
        if (icomponentdefinitions3.options != null && i_4 < icomponentdefinitions3.options.length && icomponentdefinitions3.options[i_4] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3.options[i_4];
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method4295(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.replacementOwner;
    }

    public static void intEqual(CS2Executor executor) {
        executor.intStackPtr -= 2;
        if (executor.intStack[executor.intStackPtr + 1] == executor.intStack[executor.intStackPtr]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method3064(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method2960(icomponentdefinitions3, interface4, executor);
    }

    public static void method3065(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.angle2d;
    }

    public static void method3066(CS2Executor executor) {
        if (WindowMode.fullScreenSupported) {
            DisplayModeDetails[] arr_2 = WindowMode.getActiveDisplayModeDetails();
            executor.intStack[++executor.intStackPtr - 1] = arr_2.length;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method3067(CS2Executor executor) {
        getBoundingBox(executor.currentEntity, executor);
    }

    public static void charIsalpha(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.method1169((char) i_2) ? 1 : 0;
    }

    public static void method5008(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method11223(icomponentdefinitions3, executor);
    }

    public static void method5010(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        String string_3 = (String) executor.stringStack[--executor.stringStackPtr];
        if (client.gameState == GameState.UNK_0 && !LoginManager.method3360()) {
            executor.intStack[++executor.intStackPtr - 1] = LoginManager.setGameHost(i_2, string_3) ? 1 : 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method12724(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ItemType itemdefinitions3 = TypeLists.OBJ.list(i_2);
        if (itemdefinitions3.certTemplateId >= 0 && itemdefinitions3.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions3.certId;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = i_2;
        }
    }

    public static void method12215(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ItemType itemdefinitions3 = TypeLists.OBJ.list(i_2);
        if (itemdefinitions3.certTemplateId == -1 && itemdefinitions3.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions3.certId;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = i_2;
        }
    }

    public static void method7143(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.soundLevel.getValue();
    }

    public static void ccSetAspect(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        ifSetAspect(icomponentdefinitions3, interface4, executor);
    }

    public static void method6283(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method5066(icomponentdefinitions3, interface4, executor);
    }

    public static void method6284(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).value;
    }

    public static void method6285(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt72;
    }

    public static void method1775(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        if (string_2.startsWith(TextUtils.getCrown(0)) || string_2.startsWith(TextUtils.getCrown(1))) {
            string_2 = string_2.substring(7);
        }
        executor.intStack[++executor.intStackPtr - 1] = FriendList.getIndex(string_2);
    }

    public static void hookMousePress(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method15409(icomponentdefinitions3, executor);
    }

    public static void loadClanVarString(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        String string_3 = (String) ClanVarTypeList.CLAN_VARS[i_2];
        if (string_3 == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3;
        }
    }

    public static void method3356(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        ifSetNPCHead(icomponentdefinitions3, interface4, executor);
    }

    public static void method3357(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method14586(icomponentdefinitions3, executor);
    }

    public static void method3358(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.fixedScreen.getValue();
    }

    public static void method3361(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.getMemberIndex((String) executor.stringStack[--executor.stringStackPtr]);
    }

    public static void method1385(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setTargetOpCursor(icomponentdefinitions3, executor);
    }

    public static void method1387(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldDescriptor class217Sub13 = WorldDescriptor.getWorld(i_2);
        if (class217Sub13 != null) {
            executor.intStack[++executor.intStackPtr - 1] = class217Sub13.flags;
            executor.stringStack[++executor.stringStackPtr - 1] = class217Sub13.unknown;
            WorldMeta class2134 = class217Sub13.getWorld();
            executor.intStack[++executor.intStackPtr - 1] = class2134.countryId;
            executor.stringStack[++executor.stringStackPtr - 1] = class2134.activity;
            executor.intStack[++executor.intStackPtr - 1] = class217Sub13.playerCount;
            executor.intStack[++executor.intStackPtr - 1] = class217Sub13.ping;
            executor.stringStack[++executor.stringStackPtr - 1] = class217Sub13.ipAddress;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void loadMemoryMode(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.mapSize.allowSelection() ? 1 : 0;
    }

    public static void method6205(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = executor.currentClanSettings.getSortedAffinedSlots()[executor.intStack[executor.intStackPtr - 1]];
    }

    public static void method6206(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.GRAND_EXCHANGE_SLOTS[i_2].totalPrice;
    }

    public static void random(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = (int) (Math.random() * i_2);
    }

    public static void method6210(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method789(icomponentdefinitions3, interface4, executor);
    }

    public static void strAppend(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        executor.stringStack[++executor.stringStackPtr - 1] = string_2 + string_3;
    }

    public static void method7929(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method506(icomponentdefinitions3, interface4, executor);
    }

    public static void getCompWidth(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.width;
    }

    public static void method7932(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        if (string_2.startsWith(TextUtils.getCrown(0)) || string_2.startsWith(TextUtils.getCrown(1))) {
            string_2 = string_2.substring(7);
        }
        executor.intStack[++executor.intStackPtr - 1] = FriendList.contains(string_2) ? 1 : 0;
    }

    public static void method7933(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method2741(icomponentdefinitions3, executor);
    }

    public static void method893(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        ifSetModelOrigin(icomponentdefinitions3, executor);
    }

    public static void method894(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        int i_4 = -1;
        if (chatline3 != null) {
            i_4 = chatline3.time;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
    }

    public static void loadInt(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.intLocals[executor.intOpValues[executor.instrPtr]];
    }

    public static void method12506(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        boolean bool3 = executor.intStack[executor.intStackPtr + 1] == 1;
        Node node4 = WorldMapSub1.aClass4658029.get(i_2);
        if (node4 != null && !bool3) {
            node4.unlink();
        } else if (node4 == null && bool3) {
            node4 = new Node();
            WorldMapSub1.aClass4658029.put(node4, i_2);
        }
    }

    public static void method12560(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.flickeringEffects.getValue() == 1 ? 1 : 0;
    }

    public static void method12561(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 != -1) {
            StaticMethods.method1563(i_2);
        }
    }

    public static void method4089(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.intStackPtr -= 3;
        int i_3 = executor.intStack[executor.intStackPtr];
        int i_4 = executor.intStack[executor.intStackPtr + 1];
        int i_5 = executor.intStack[executor.intStackPtr + 2];
        StaticMethods.queryItemParamInt(string_2, i_3 == 1, i_4, i_5);
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN;
    }

    public static void method4091(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = WorldMap.staticElement != null ? WorldMap.staticElement.anInt9536 : -1;
    }

    public static void method15306() {
        PreferenceManager.loadLowDetailPreferences();
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method3667(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.activeToolkit.getValue();
    }

    public static void method14340(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method14653(icomponentdefinitions4, executor);
    }

    public static void method14342(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method789(icomponentdefinitions3, interface4, executor);
    }

    public static void method12933(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 >= 1 && i_2 <= 2) {
            WindowMode.requestWindowMode(i_2, -1, -1, false);
        }
    }

    public static void intLessThan(CS2Executor executor) {
        executor.intStackPtr -= 2;
        if (executor.intStack[executor.intStackPtr] < executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method8750(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method6313(icomponentdefinitions3, executor);
    }

    public static void method8753(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStackPtr -= 2;
        int i_4 = executor.intStack[executor.intStackPtr];
        int i_5 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault6 = TypeLists.PARAMS.list(i_4);
        if (i_5 != attributedefault6.defaultInt) {
            icomponentdefinitions3.method1999(i_4, i_5);
        } else {
            icomponentdefinitions3.method1995(i_4);
        }
    }

    public static void method8756(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYERS[i_2].rank;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method8757(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        String string_3 = (String) executor.stringStack[--executor.stringStackPtr];
        if (i_2 == -1) {
            throw new RuntimeException();
        } else {
            EnumType enumdefinitions4 = TypeLists.ENUM.list(i_2);
            if (enumdefinitions4.valueType != 115) {
                throw new RuntimeException();
            } else {
                int[] ints_5 = enumdefinitions4.getValueReferences(string_3);
                int i_6 = 0;
                if (ints_5 != null) {
                    i_6 = ints_5.length;
                }
                executor.intStack[++executor.intStackPtr - 1] = i_6;
            }
        }
    }

    public static void getWorldMapParam(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault4 = TypeLists.PARAMS.list(i_3);
        if (attributedefault4.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.MEC.list(i_2).getStringParameter(i_3, attributedefault4.typeName);
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.MEC.list(i_2).getIntParameter(i_3, attributedefault4.defaultInt);
        }
    }

    public static void method5119(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.method14668().getValue();
    }

    public static void pushFogPreference(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.fog.getValue() == 1 ? 1 : 0;
    }

    public static void method5123(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions4.scrollY;
    }

    public static void method14578(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.scrollHeight;
    }

    public static void ifSetPosition(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setComponentPosition(icomponentdefinitions3, interface4, executor);
    }

    public static void method6687(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method14892(icomponentdefinitions3, executor);
    }

    public static void method6688(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method6010(icomponentdefinitions4, interface5, executor);
    }

    public static void method6691(CS2Executor executor) {
        boolean bool2 = executor.intStack[--executor.intStackPtr] != 0;
        int i_3 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = SystemInfo.getCacheCompletionPercent(i_3, 0, bool2, client.CURRENT_LANGUAGE);
    }

    public static void method6695() {
        LoginManager.method1774();
    }

    public static void method5691(CS2Executor executor) {
        String itemName = (String) executor.stringStack[--executor.stringStackPtr];
        int tradeableOnly = executor.intStack[--executor.intStackPtr];
        StaticMethods.queryItemNoParams(itemName, tradeableOnly == 1);
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN;
    }

    public static void pushGameobjectHintLocation(CS2Executor executor) {
        HintRenderer.calculateSceneobjectHintProjection((SceneGraphEntity) executor.currentLocation, executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[0];
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[1];
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[2];
    }

    public static void method5695(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2].worldId;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void playSoundSynth(CS2Executor executor) {
        executor.intStackPtr -= 3;
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], 255, 256);
    }

    public static void method3169(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel.players[i_2].world;
    }

    public static void method4161(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = CS2Interpreter.method2598().getValue();
    }

    public static void saveAntiAliasingPreference(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.antiAliasing, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
    }

    public static void method14644(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel.minRankToKick;
    }

    public static void method12586(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        method2738(executor.currentEntity.inter, i_2 & 0xffff, i_3, i_4, executor.aBool7022, executor);
    }

    public static void method12587(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).hasVarbitRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1]) ? 1 : 0;
        --executor.intStackPtr;
    }

    //Deprecated jsobject applet BS
    public static void method572(CS2Executor executor) {
        boolean bool2 = true;
        String string_3 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = bool2 ? 1 : 0;
    }

    public static void method7704(CS2Executor executor) {
        PacketEncoder.method16088(executor.intStack[--executor.intStackPtr]);
    }

    public static void method14503(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        if (i_3 == -1) {
            throw new RuntimeException();
        } else {
            EnumType enumdefinitions5 = TypeLists.ENUM.list(i_3);
            if (i_2 != enumdefinitions5.valueType) {
                throw new RuntimeException();
            } else {
                executor.intStack[++executor.intStackPtr - 1] = enumdefinitions5.containsKey(Integer.valueOf(i_4)) ? 1 : 0;
            }
        }
    }

    public static void resumeCountDialog(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        int i_3 = 0;
        if (Utils.isWrongJava(string_2)) {
            i_3 = Utils.parseInt(string_2);
        }
        TcpPacket tcpmessage4 = PacketEncoder.createPacket(ClientProt.RESUME_COUNTDIALOG, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage4.buffer.writeInt(i_3);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage4);
    }

    public static void pushLong(CS2Executor executor) {
        executor.longStack[++executor.longStackPtr - 1] = executor.current.longOpValues[executor.instrPtr];
    }

    public static void method5915(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.intStackPtr -= 2;
        int i_3 = executor.intStack[executor.intStackPtr];
        int i_4 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = string_2.indexOf(i_3, i_4);
    }

    public static void getObjectParam(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault4 = TypeLists.PARAMS.list(i_3);
        if (attributedefault4.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = Resource.mapRegion.getLoctypeList().list(i_2).getParamsString(i_3, attributedefault4.typeName);
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Resource.mapRegion.getLoctypeList().list(i_2).getParamsInt(i_3, attributedefault4.defaultInt);
        }
    }

    public static void setBaseIdentiKit(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (PlayerUpdate.MY_PLAYER.playerAppearance != null) {
            int i_4;
            for (i_4 = 0; i_4 < anIntArray428.length; i_4++) {
                if (anIntArray428[i_4] == i_2) {
                    PlayerUpdate.MY_PLAYER.playerAppearance.addAppearanceIdentity(i_4, i_3, TypeLists.IDK);
                    return;
                }
            }
            for (i_4 = 0; i_4 < anIntArray422.length; i_4++) {
                if (anIntArray422[i_4] == i_2) {
                    PlayerUpdate.MY_PLAYER.playerAppearance.addAppearanceIdentity(i_4, i_3, TypeLists.IDK);
                    break;
                }
            }
        }
    }

    public static void resumeClanForumQFCDialog(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.RESUME_CLANFORUMQFCDIALOG, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage3.buffer.writeByte(string_2.length() + 1);
        tcpmessage3.buffer.writeString(string_2);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
    }

    public static void method2871(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendList.FRIENDS[i_2].worldName;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void setPublicFilter(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.PUBLIC_FILTER;
    }

    public static void method2873(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.skyboxes.getValue();
    }

    public static void method4247(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ.list(i_2).name;
    }

    public static void method4249(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault4 = TypeLists.PARAMS.list(i_3);
        if (attributedefault4.isString()) {
            String string_5 = attributedefault4.typeName;
            if (i_2 == -1) {
                executor.stringStack[++executor.stringStackPtr - 1] = string_5;
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST.list(i_2).getParamstring(i_3, string_5);
            }
        } else {
            int i_6 = attributedefault4.defaultInt;
            if (i_2 == -1) {
                executor.intStack[++executor.intStackPtr - 1] = i_6;
            } else {
                executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUEST.list(i_2).getParamint(i_3, i_6);
            }
        }
    }

    public static void method4252(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.method12402((char) i_2) ? 1 : 0;
    }

    public static void method1479(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        boolean bool3 = true;
        if (i_2 < 0) {
            bool3 = (i_2 + 1) % 4 == 0;
        } else if (i_2 < 1582) {
            bool3 = i_2 % 4 == 0;
        } else if (i_2 % 4 != 0) {
            bool3 = false;
        } else if (i_2 % 100 != 0) {
        } else if (i_2 % 400 != 0) {
            bool3 = false;
        }
        executor.intStack[++executor.intStackPtr - 1] = bool3 ? 1 : 0;
    }

    public static void method3918(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method8754(icomponentdefinitions3, executor);
    }

    public static void method3919(CS2Executor executor) {
        WorldMapSub1.aBool8027 = executor.intStack[--executor.intStackPtr] == 1;
    }

    public static void method3921(CS2Executor executor) {
        BufferedConnectionContext class1842 = client.getConnectionContext();
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842.isaac);
        tcpmessage3.buffer.writeByte(0);
        int i_4 = tcpmessage3.buffer.currentPosition;
        tcpmessage3.buffer.writeByte(1);
        tcpmessage3.buffer.writeShort(executor.currentQuickChatMessage.id);
        executor.currentQuickChatMessage.message.method14896(tcpmessage3.buffer, executor.currentQuickChatMessage.anIntArray4046);
        tcpmessage3.buffer.writeIndex(tcpmessage3.buffer.currentPosition - i_4);
        class1842.queuePacket(tcpmessage3);
    }

    public static void method6138(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method5001(icomponentdefinitions3, executor);
    }

    public static void method6139(CS2Executor executor) {
        long long_2 = executor.longStack[--executor.longStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = long_2 == -1L ? "" : Long.toString(long_2, 36).toUpperCase();
    }

    public static void method6141(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setResumeText(icomponentdefinitions3, executor);
    }

    public static void method6142(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.fontId;
    }

    public static void method289(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteScale;
    }

    public static void method290(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        MECType worldmapareadefs3 = TypeLists.MEC.list(i_2);
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3.anInt2718;
    }

    public static void method291(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY.list(i_2).subCategoryHotkeys[i_3];
    }

    public static void method292(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.removeRoofs.getValue() == 2 ? 1 : 0;
    }

    public static void method5733(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.memberWorld ? 1 : 0;
    }

    public static void method5734(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.voiceOverVolume.getValue();
    }

    public static void method5735(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.intStackPtr -= 2;
        int i_3 = executor.intStack[executor.intStackPtr];
        int i_4 = executor.intStack[executor.intStackPtr + 1];
        FontMetrics fontmetrics5 = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_4);
        assert fontmetrics5 != null;
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics5.getTextWidthMultiline(string_2, i_3, StaticMedia.nameIconSprites);
    }

    public static void method5736(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[--executor.intStackPtr]).sortName;
    }

    public static void method504(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anInt5904;
    }

    public static void method1067(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).hasVarpRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1]) ? 1 : 0;
        --executor.intStackPtr;
    }

    public static void method1069(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method13468(icomponentdefinitions3, interface4, executor);
    }

    public static void method1070(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        boolean bool3 = executor.intStack[--executor.intStackPtr] == 1;
        QuickchatMessageType.method1360(string_2, bool3);
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN;
    }

    public static void method1071() {
        client.method4195();
    }

    public static void storeVarp(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        VarProviders.PLAYER_VAR_PROVIDER.setVarValue(i_2, executor.intStack[--executor.intStackPtr]);
    }

    public static void method2737(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ComponentMinimap.mapAssetLoadingPercentage == 100 ? 1 : 0;
    }

    public static void method1357(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt8307;
    }

    public static void method1362(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_2).responses[i_3];
    }

    public static void method11252(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method2741(icomponentdefinitions3, executor);
    }

    public static void method3753(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).questpointRequirement;
    }

    public static void method3755(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        MECType worldmapareadefs3 = TypeLists.MEC.list(i_2);
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3.baseTextzoom;
    }

    public static void method15389(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getContainerTotal(i_2, i_3, false);
    }

    public static void method1562(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = client.getComponentSettings(icomponentdefinitions3).getUseOptionFlags();
    }

    public static void method2101(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[--executor.intStackPtr]).name;
    }

    public static void method2102(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.memberRanks[i_2];
    }

    public static void method2103(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ((NpcEntity) executor.currentEntity).definitions.type;
    }

    public static void loadVarc(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = VarcTypeList.VARC_INT[executor.intOpValues[executor.instrPtr]];
    }

    public static void method6776(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method2955(icomponentdefinitions3, executor);
    }

    public static void method5697(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_2).method14898();
    }

    public static void pow(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (i_2 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = (int) Math.pow(i_2, i_3);
        }
    }

    public static void ccSetItemWearColNoNum(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setItemIFComp(icomponentdefinitions3, interface4, true, Component.StackMode.NONUM, executor);
    }

    public static void method2564(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method910(icomponentdefinitions3, executor);
    }

    public static void sendAClass3794624(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        if (client.gameState == GameState.UNK_0 && !LoginManager.method3360()) {
            if (string_2.length() > 20) {
                client.aByte7458 = -4;
            } else {
                client.aByte7458 = -1;
                TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.DEPRECATED_97_SERVERPACKET_52, client.LOBBY_CONNECTION_CONTEXT.isaac);
                tcpmessage3.buffer.writeByte(0);
                int i_4 = tcpmessage3.buffer.currentPosition;
                tcpmessage3.buffer.writeString(string_2);
                tcpmessage3.buffer.writeIndex(tcpmessage3.buffer.currentPosition - i_4);
                client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
            }
        } else {
            client.aByte7458 = -5;
        }
    }

    public static void method2567(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method7729(icomponentdefinitions3, executor);
    }

    public static void method2568(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.flickeringEffects, executor.intStack[--executor.intStackPtr] == 1 ? 1 : 0);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method1349(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method2955(icomponentdefinitions3, executor);
    }

    public static void method1350(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.characterShadows.getValue() == 1 ? 1 : 0;
    }

    public static void method1351(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = QueueIterator.LOYALTY_ENABLED ? 1 : 0;
    }

    public static void method1352(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.goundBlending.canModify(i_2);
    }

    public static void method1355(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        Interface.openSub(i_2, new SubInterface(i_3, 3), null, true);
    }

    public static void ccCreate(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Component.fromHash(i_2);
        method2738(Interface.INTERFACES[i_2 >>> 16], i_2 & 0xffff, i_3, i_4, executor.aBool7022, executor);
    }

    public static void method5296(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method3366(icomponentdefinitions4, executor);
    }

    public static void method5297(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int i_3 = client.GRAND_EXCHANGE_SLOTS[i_2].method5908();
        int[] ints_4 = executor.intStack;
        int i_5 = ++executor.intStackPtr - 1;
        byte b_6;
        if (i_3 == 1) {
            b_6 = 1;
        } else {
            b_6 = 0;
        }
        ints_4[i_5] = b_6;
    }

    public static void method5298(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStackPtr -= 2;
        String string_3 = (String) executor.stringStack[executor.stringStackPtr];
        String string_4 = (String) executor.stringStack[executor.stringStackPtr + 1];
        if (string_3.length() <= 500 && string_4.length() <= 500) {
            PacketEncoder.reportBug(i_2, string_3, string_4);
        }
    }

    public static void method5299(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = CalenderUtil.getLocalYear(Utils.time());
    }

    public static void getGEOfferPrice(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.GRAND_EXCHANGE_SLOTS[i_2].price;
    }

    public static void getCompText(CS2Executor executor) {
        int component = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(component);
        assert icomponentdefinitions3 != null;
        executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3.text;
    }

    public static void method5487(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setTransparency(icomponentdefinitions3, executor);
    }

    public static void method5488(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method15256(icomponentdefinitions3, executor);
    }

    public static void method5489(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED != 0 && i_2 < IgnoreList.IGNORE_COUNT) {
            Ignore class103 = IgnoreList.IGNORED[i_2];
            executor.stringStack[++executor.stringStackPtr - 1] = class103.unfilteredUsername;
            if (class103.displayName != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = class103.displayName;
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = "";
            }
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method5490(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getContainerTotal(i_2, i_3, true);
    }

    public static void method6314(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteId;
    }

    public static void method6315(CS2Executor executor) {
        BufferedConnectionContext class1842 = client.getConnectionContext();
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842.isaac);
        tcpmessage3.buffer.writeByte(0);
        int i_4 = tcpmessage3.buffer.currentPosition;
        tcpmessage3.buffer.writeByte(0);
        tcpmessage3.buffer.writeShort(executor.currentQuickChatMessage.id);
        executor.currentQuickChatMessage.message.method14896(tcpmessage3.buffer, executor.currentQuickChatMessage.anIntArray4046);
        tcpmessage3.buffer.writeIndex(tcpmessage3.buffer.currentPosition - i_4);
        class1842.queuePacket(tcpmessage3);
    }

    public static void getGEOfferItem(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.GRAND_EXCHANGE_SLOTS[i_2].itemId;
    }

    public static void method6317(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = GameState.isRenderingScene(client.gameState) ? 1 : 0;
    }

    public static void ccSetItemWearCol(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setItemIFComp(icomponentdefinitions3, interface4, true, Component.StackMode.WHEN_MULTIPLE, executor);
    }

    public static void setCompSprite(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method8390(icomponentdefinitions3, interface4, executor);
    }

    public static void getPlayerIsMember(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.IS_MEMBER ? 1 : 0;
    }

    public static void method1138(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Component icomponentdefinitions5 = Component.listIcomponent(i_2 << 16 | i_3, i_4);
        InterfaceComponentRenderer.cancelInterfaceSelection();
        assert icomponentdefinitions5 != null;
        IFEvents class282Sub106 = client.getComponentSettings(icomponentdefinitions5);
        InterfaceRenderer.setUseOptionFlags(icomponentdefinitions5, class282Sub106.getUseOptionFlags(), class282Sub106.interfaceId);
    }

    public static void method1140(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.goundBlending.getValue() == 1 ? 1 : 0;
    }

    public static void method6310(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setOpBase(icomponentdefinitions3, executor);
    }

    public static void method787(CS2Executor executor) {
        WorldMapCategory class282Sub362 = WorldMap.method11595();
        if (class282Sub362 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub362.mecId;
            int i_3 = class282Sub362.anInt7988 << 28 | class282Sub362.absX + WorldMap.mapSquareAbsX << 14 | class282Sub362.absY + WorldMap.mapSquareAbsY;
            executor.intStack[++executor.intStackPtr - 1] = i_3;
        }
    }

    public static void method11403(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method3987(icomponentdefinitions3, interface4);
    }

    public static void method11405(CS2Executor executor) {
        executor.intStackPtr -= 2;
        CursorManager.defaultCursorId = executor.intStack[executor.intStackPtr];
        CursorManager.defaultAttackCursor = executor.intStack[executor.intStackPtr + 1];
    }

    public static void method11406(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).stackable == 1 ? 1 : 0;
    }

    public static void ifResumePauseButton(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        method6942(icomponentdefinitions3);
    }

    public static void method4174(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method7667(icomponentdefinitions3, interface4, executor);
    }

    public static void method4175(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method2170(icomponentdefinitions3, executor);
    }

    public static void method4176(CS2Executor executor) {
        int i_2 = client.anIntArrayArrayArray7302[executor.intStack[--executor.intStackPtr]].length >> 1;
        executor.intStack[++executor.intStackPtr - 1] = i_2;
    }

    public static void method4177(CS2Executor executor) {
        executor.intStackPtr -= 4;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        int i_5 = executor.intStack[executor.intStackPtr + 3];
        CoordGrid coordgrid6 = Resource.mapRegion.getBaseCoordinates();
        Camera.moveCamera((i_2 >> 14 & 0x3fff) - coordgrid6.x, (i_2 & 0x3fff) - coordgrid6.y, i_3 << 2, i_4, i_5, false);
    }

    public static void method4178(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        long long_5 = CalenderUtil.getGmtNoon(i_2, i_3, i_4);
        int i_7 = StaticMethods.method7914(long_5);
        if (i_4 < 1970) {
            --i_7;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_7;
    }

    public static void method4181(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        IgnoreList.remove(string_2);
    }

    public static void method6071(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method8390(icomponentdefinitions3, interface4, executor);
    }

    public static void method6072(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.bloom.visible() && Renderers.activeToolkit.hasBloomSupport() ? 1 : 0;
    }

    public static void method6169(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        LoginManager.snLogin(i_2);
    }

    public static void intGreaterOrEqual(CS2Executor executor) {
        executor.intStackPtr -= 2;
        if (executor.intStack[executor.intStackPtr] >= executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void getMouseY(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder.getMouseY();
    }

    public static void method300(CS2Executor executor) {
        NpcEntity npc2 = (NpcEntity) executor.currentEntity;
        boolean bool3 = false;
        NPCType npcdefinitions4 = npc2.definitions;
        if (npcdefinitions4.alternativeIds != null) {
            npcdefinitions4 = npcdefinitions4.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
        }
        if (npcdefinitions4 != null) {
            bool3 = npcdefinitions4.visible;
        }
        executor.intStack[++executor.intStackPtr - 1] = bool3 ? 1 : 0;
    }

    public static void method301(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt106;
    }

    public static void method305(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method6186(icomponentdefinitions3, interface4, executor);
    }

    public static void computeGraphicsMode() {
        if (lastEnteredGraphicsMode == 2) {
            GameShell.softwareMode = true;
        } else if (lastEnteredGraphicsMode == 1) {
            GameShell.openglMode = true;
        } else if (lastEnteredGraphicsMode == 3) {
            GameShell.directxMode = true;
        }
    }

    public static void method4200(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStackPtr -= 10;
        byte[] bytes_4 = null;
        byte[] bytes_5 = null;
        int i_6;
        for (i_6 = 0; i_6 < 10 && executor.intStack[i_6 + executor.intStackPtr] >= 0; i_6 += 2) {
        }
        if (i_6 > 0) {
            bytes_4 = new byte[i_6 / 2];
            bytes_5 = new byte[i_6 / 2];
            for (i_6 -= 2; i_6 >= 0; i_6 -= 2) {
                bytes_4[i_6 / 2] = (byte) executor.intStack[i_6 + executor.intStackPtr];
                bytes_5[i_6 / 2] = (byte) executor.intStack[i_6 + executor.intStackPtr + 1];
            }
        }
        method1838(icomponentdefinitions3, bytes_4, bytes_5, executor);
    }

    public static void method4201(CS2Executor executor) {
        Component icomponentdefinitions2 = Component.fromHash(executor.intStack[--executor.intStackPtr]);
        assert icomponentdefinitions2 != null;
        if (icomponentdefinitions2.containerItemId != -1) {
            executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions2.itemStackSize;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method4202(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ItemType itemdefinitions4 = TypeLists.OBJ.list(i_2);
        if (i_3 >= 1 && i_3 <= 5 && itemdefinitions4.inventoryActions[i_3 - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = itemdefinitions4.inventoryActions[i_3 - 1];
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void setIfTextAlign(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setIfTextAlign(icomponentdefinitions3, executor);
    }

    public static void getAppletFocus(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = GameShell.appletHasFocus ? 1 : 0;
    }

    public static void method6802(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method3987(icomponentdefinitions3, interface4);
    }

    public static void method8327(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method7555(icomponentdefinitions3, interface4, executor);
    }

    public static void storeLong(CS2Executor executor) {
        executor.longLocals[executor.intOpValues[executor.instrPtr]] = executor.longStack[--executor.longStackPtr];
    }

    public static void method15523(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method7267(icomponentdefinitions3, executor);
    }

    public static void method5016(CS2Executor executor) {
        QuestType questdefinitions2 = TypeLists.QUEST.list(executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = questdefinitions2.questPrerequisiteIds == null ? 0 : questdefinitions2.questPrerequisiteIds.length;
    }

    public static void longLessOrEqual(CS2Executor executor) {
        executor.longStackPtr -= 2;
        if (executor.longStack[executor.longStackPtr] <= executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method5922(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings.clanName;
    }

    public static void method5925(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        LoginManager.method2635(string_2, string_3);
    }

    public static void method5926(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.offsetX;
    }

    public static void method7699(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.aBool7151 ? 1 : 0;
    }

    public static void loadClanSettingVarbit(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        ClanVarSettingsType class5373 = TypeLists.CLAN_VAR_SETTINGS.list(i_2);
        if (class5373 == null) {
            throw new RuntimeException();
        } else {
            Integer integer4 = executor.currentClanSettings.method1225(client.CURRENT_GAME.id << 16 | class5373.baseVar, class5373.startBit, class5373.endBit);
            int i_5;
            if (integer4 == null) {
                i_5 = 0;
            } else {
                i_5 = integer4.intValue();
            }
            executor.intStack[++executor.intStackPtr - 1] = i_5;
        }
    }

    public static void method8859(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        method3965(icomponentdefinitions4, executor);
    }

    public static void method8863(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.x;
    }

    public static void method933(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.currentOwner;
    }

    public static void method934(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = SystemInfo.SYSTEM_INFO.ram >= 512 && !client.isForcedSafemode && !client.renderWithCpu ? 0 : 1;
    }

    public static void bitOr(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 | i_3;
    }

    public static void method3692(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.musicVolume.getValue();
    }

    public static void method3693(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.water, executor.intStack[--executor.intStackPtr] == 1 ? 2 : 0);
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void clearCompChildren(CS2Executor executor) {
        Component icomponentdefinitions2 = Component.fromHash(executor.intStack[--executor.intStackPtr]);
        assert icomponentdefinitions2 != null;
        icomponentdefinitions2.slotChildren = null;
        icomponentdefinitions2.itemSlots = null;
        Component.redrawComponent(icomponentdefinitions2);
    }

    public static void method3911(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(10, i_2 << 16 | i_3, i_4, "");
    }

    public static void method1783(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (i_3 == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY.list(i_2).messageForHotkey((char) i_3);
        }
    }

    public static void method1784(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        CS2Interface underlaydefinition3;
        if (executor.aBool7022) {
            underlaydefinition3 = executor.hookedInterface2;
        } else {
            underlaydefinition3 = executor.hookedInterface1;
        }
        executor.intStack[++executor.intStackPtr - 1] = underlaydefinition3.load(executor.currentEntity.inter, i_2) ? 1 : 0;
    }

    public static void method2953(CS2Executor executor) {
        QuestType questdefinitions2 = TypeLists.QUEST.list(executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = questdefinitions2.levelRequirements == null ? 0 : questdefinitions2.levelRequirements.length;
    }

    public static void method4773(CS2Executor executor) {
        if (FriendList.LOADED == 0) {
            executor.intStack[++executor.intStackPtr - 1] = -2;
        } else if (FriendList.LOADED == 1) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIEND_COUNT;
        }
    }

    public static void method4774(CS2Executor executor) {
        String string_2 = null;
        if (PacketDecoder.HOSTNAME_IDENTIFIER != null) {
            string_2 = PacketDecoder.HOSTNAME_IDENTIFIER.getHostName();
        }
        if (string_2 == null) {
            string_2 = "";
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2;
    }

    public static void method4775() {
        StaticMethods.method4166();
    }

    public static void method4776() {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.graphics, 0);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void containerTotalParamStack(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getParamBasedContainerTotal(i_2, i_3, true);
    }

    public static void loadVarcString(CS2Executor executor) {
        String string_2 = VarcTypeList.VARC_STRING[executor.intOpValues[executor.instrPtr]];
        if (string_2 == null) {
            string_2 = "";
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2;
    }

    public static void popString(CS2Executor executor) {
        --executor.stringStackPtr;
    }

    public static void method7771(CS2Executor executor) {
        String string_2;
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER.username != null) {
            string_2 = PlayerUpdate.MY_PLAYER.getDisplayName();
        } else {
            string_2 = "";
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2;
    }

    public static void method7773() {
    	PreferenceManager.loadMidDetailPreferences();
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method7774(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        WorldMapSub1.reset(i_2, i_3 >> 14 & 0x3fff, i_3 & 0x3fff, true);
    }

    public static void getVarpOld(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.intStack[++executor.intStackPtr - 1] = ((PlayerEntity) executor.currentEntity).playerConfigs.getVarp(i_2);
    }

    public static void method8343(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.antiAliasing.visible() && Renderers.activeToolkit.hasAntiAliasing() ? 1 : 0;
    }

    public static void method8344(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6186(icomponentdefinitions3, interface4, executor);
    }

    public static void method3802(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.GRAND_EXCHANGE_SLOTS[i_2].amount;
    }

    public static void method3803(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        int i_4 = -1;
        if (chatline3 != null) {
            i_4 = chatline3.identifier;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
    }

    public static void method3804(CS2Executor executor) {
        executor.intStackPtr -= 2;
        if (WindowMode.fullScreenSupported) {
            executor.intStack[++executor.intStackPtr - 1] = WindowMode.fullScreenFrame != null ? 1 : 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void ifSetItemWearColAlwaysNum(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setItemIFComp(icomponentdefinitions3, interface4, true, Component.StackMode.ALWAYS_NUM, executor);
    }

    public static void method4556(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).hasAllRequirements(VarProviders.PLAYER_VAR_PROVIDER, client.SKILL_LEVEL_ACTUAL) ? 1 : 0;
    }

    public static void method4557(CS2Executor executor) {
        executor.intStackPtr -= 3;
        SongPlayer.playSong(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2]);
    }

    public static void method4558(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method505(icomponentdefinitions3, executor);
    }

    public static void stringEqual(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        executor.intStack[++executor.intStackPtr - 1] = TextUtils.method7557((String) executor.stringStack[executor.stringStackPtr], (String) executor.stringStack[executor.stringStackPtr + 1], client.CURRENT_LANGUAGE);
    }

    public static void toString(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = Integer.toString(i_2);
    }

    public static void method8215(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        if (icomponentdefinitions3.opBase == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3.opBase;
        }
    }

    public static void method8216(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).isStarted(VarProviders.PLAYER_VAR_PROVIDER) ? 1 : 0;
    }

    public static void method8218(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TextUtils.isAlphabeticalOrNumeric((char) i_2) ? 1 : 0;
    }

    public static void method8219(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt5760;
        executor.intStack[++executor.intStackPtr - 1] = ComponentMinimap.anInt4097;
    }

    public static void method8220(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >>> 16];
        assert icomponentdefinitions3 != null;
        Component icomponentdefinitions5 = Interface.getSub(interface4, icomponentdefinitions3);
        int[] ints_6 = executor.intStack;
        int i_7 = ++executor.intStackPtr - 1;
        int i_8;
        if (icomponentdefinitions5 == null) {
            i_8 = -1;
        } else {
            i_8 = icomponentdefinitions5.idHash;
        }
        ints_6[i_7] = i_8;
    }

    public static void method1631(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method14586(icomponentdefinitions4, executor);
    }

    public static void ifSetDepthChild(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        setDepthChild(icomponentdefinitions3, executor);
    }

    public static void method4839(CS2Executor executor) {
        --executor.intStackPtr;
        int i_2 = executor.intStack[executor.intStackPtr];
        SubInterface class282Sub443 = client.OPEN_INTERFACES.get(i_2);
        if (class282Sub443 != null && class282Sub443.overlay == 3) {
            Interface.closeChildInterface(class282Sub443, true, true);
        }
    }

    public static void method6395(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method5009(icomponentdefinitions4, executor);
    }

    public static void method6396(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        StaticMethods.method15430(i_2);
    }

    public static void method6397(CS2Executor executor) {
        int i_2 = PreferenceManager.preferences.removeRoofs.getValue();
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.removeRoofsOptionOverride, executor.intStack[--executor.intStackPtr] == 1 ? 0 : i_2);
        Scene.setRoofOverride();
    }

    public static void method6399(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = 0;
    }

    public static void method6400(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        executor.intStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr];
        int i_4 = executor.intStack[executor.intStackPtr + 1];
        String string_5 = (String) executor.stringStack[executor.stringStackPtr + 1];
        CS2Executor.queryItemParamString(string_2, i_3 == 1, i_4, string_5);
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN;
    }

    public static void method13044(CS2Executor executor) {
        if (FriendsChat.FC_OWNER_NAME != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_OWNER_NAME;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void activeClanchannelFindAffined(CS2Executor executor) {
        if (client.CLAN_CHANNEL != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
            executor.clanChannel = client.CLAN_CHANNEL;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method3611(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.sceneryShadows, executor.intStack[--executor.intStackPtr]);
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method3613() {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false);
        }
    }

    public static void divide(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 / i_3;
    }

    public static void method5765(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).wearPos2;
    }

    public static void method5766(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.mapSize, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method5767(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.particles.supported() ? 1 : 0;
    }

    public static void method5769(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (WindowMode.fullScreenSupported) {
            DisplayModeDetails[] arr_3 = WindowMode.getActiveDisplayModeDetails();
            executor.intStack[++executor.intStackPtr - 1] = arr_3[i_2].width;
            executor.intStack[++executor.intStackPtr - 1] = arr_3[i_2].height;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method6174(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteYaw;
    }

    public static void method15448(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method774(icomponentdefinitions3, executor);
    }

    public static void method11438(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method3078(icomponentdefinitions3, executor);
    }

    public static void method11439(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int i_3 = i_2 >> 14 & 0x3fff;
        int i_4 = i_2 & 0x3fff;
        CoordGrid coordgrid5 = Resource.mapRegion.getBaseCoordinates();
        i_3 -= coordgrid5.x;
        if (i_3 < 0) {
            i_3 = 0;
        } else if (i_3 >= Resource.mapRegion.getSceneSizeX()) {
            i_3 = Resource.mapRegion.getSceneSizeX();
        }
        i_4 -= coordgrid5.y;
        if (i_4 < 0) {
            i_4 = 0;
        } else if (i_4 >= Resource.mapRegion.getSceneSizeY()) {
            i_4 = Resource.mapRegion.getSceneSizeY();
        }
        client.anInt7262 = (i_3 << 9) + 256;
        client.anInt7376 = (i_4 << 9) + 256;
        Camera.cameraCalculationType = 4;
        StaticMethods.anInt833 = -1;
        PingRequester.anInt5864 = -1;
    }

    public static void method11440(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(1, i_2 << 16 | i_3, i_4, "");
    }

    public static void method4965(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method4160(icomponentdefinitions3, executor);
    }

    public static void method4966(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        EnumType enumdefinitions4 = TypeLists.ENUM.list(i_2);
        executor.stringStack[++executor.stringStackPtr - 1] = enumdefinitions4.getStringValue(i_3);
    }

    public static void fromDate(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.method13045(CalenderUtil.method5363(executor.intStack[--executor.intStackPtr]), client.CURRENT_LANGUAGE.getValue());
    }

    public static void method4968(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        PacketEncoder.kickPlayer(executor.clanChannel == client.CLAN_CHANNEL, i_2);
    }

    public static void method4970(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6313(icomponentdefinitions3, executor);
    }

    public static void method4971(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method910(icomponentdefinitions3, executor);
    }

    public static void method5197(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getAmountAtSlot(i_2, i_3, false);
    }

    public static void method5198(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).membersOnly ? 1 : 0;
    }

    public static void method5199(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 != -1) {
            StaticMethods.setVorbisSound(i_2);
        }
    }

    public static void method563(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        ifSetModelOrigin(icomponentdefinitions3, executor);
    }

    public static void method564(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method4651(icomponentdefinitions3, executor);
    }

    public static void method565(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        String str4 = "";
        if (chatline3 != null && chatline3.simpleName != null) {
            str4 = chatline3.simpleName;
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4;
    }

    public static void method6851(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        method14605(icomponentdefinitions3, executor);
    }

    public static void method1391(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = WorldMapSub1.anInt5123 + WorldMap.mapSquareAbsX;
        executor.intStack[++executor.intStackPtr - 1] = WorldMapSub1.anInt5624 + WorldMap.mapSquareAbsY;
    }

    public static void setCCTextAlign(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setIfTextAlign(icomponentdefinitions3, executor);
    }

    public static void method6272(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int[] ints_3 = CalenderUtil.getLocalDdmmyyyy(i_2);
        ArrayUtils.arrayCopy(ints_3, 0, executor.intStack, executor.intStackPtr, 3);
        executor.intStackPtr += 3;
    }

    public static void method6275(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.interfaceSelectionActive ? 1 : 0;
        executor.stringStack[++executor.stringStackPtr - 1] = client.useOnTarget == null ? "" : client.useOnTarget;
        executor.stringStack[++executor.stringStackPtr - 1] = client.useOnType == null ? "" : client.useOnType;
    }

    public static void method16079(CS2Executor executor) {
        executor.intStackPtr -= 2;
        Camera.setAngle(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], 0);
    }

    public static void method1624(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method569(icomponentdefinitions3, executor);
    }

    public static void method1625(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED == 2 && i_2 >= 0 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2].platform;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method1627() {
        StaticMethods.method16114();
    }

    public static void arrayNew(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr] >> 16;
        int i_3 = executor.intOpValues[executor.instrPtr] & 0xffff;
        int i_4 = executor.intStack[--executor.intStackPtr];
        if (i_4 >= 0 && i_4 <= 5000) {
            executor.globalArrayLengths[i_2] = i_4;
            byte b_5 = -1;
            if (i_3 == 105) {
                b_5 = 0;
            }
            for (int i_6 = 0; i_6 < i_4; i_6++) {
                executor.globalArrays[i_2][i_6] = b_5;
            }
        } else {
            throw new RuntimeException();
        }
    }

    public static void subtract(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 - i_3;
    }

    public static void method2963(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Node node3 = WorldMapSub1.aClass4658029.get(i_2);
        executor.intStack[++executor.intStackPtr - 1] = node3 != null ? 1 : 0;
    }

    public static void method2965(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method8387(icomponentdefinitions4, executor);
    }

    public static void method1802(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.memberCount;
    }

    public static void method1803(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.goundBlending, executor.intStack[--executor.intStackPtr] != 0 ? 1 : 0);
        PreferenceManager.savePreferences();
        Resource.mapRegion.reload();
    }

    public static void strAppendSigNum(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        int i_3 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = string_2 + TextUtils.createNumberCenteredWithinText(i_3, true);
    }

    public static void method927(CS2Executor executor) {
        int i_2 = client.OPEN_INTERFACES.size();
        if (client.BASE_WINDOW_ID != -1) {
            ++i_2;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_2;
    }

    public static void method928(CS2Executor executor) {
        if (KeyRecorder.keyRecorder.held(82)) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method2586(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.cpu, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
    }

    public static void method2587(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.aShort7436;
        executor.intStack[++executor.intStackPtr - 1] = client.aShort7437;
    }

    public static void method2588(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.water.display() ? 1 : 0;
    }

    public static void ifSetOnMouseLeave(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        setOnMouseLeave(icomponentdefinitions3, executor);
    }

    public static void method6194(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        String string_3 = (String) executor.stringStack[--executor.stringStackPtr];
        PacketEncoder.method2075(i_2, string_3);
    }

    public static void method6195(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.antiAliasingPreview.getValue();
    }

    public static void longNotEqual(CS2Executor executor) {
        executor.longStackPtr -= 2;
        if (executor.longStack[executor.longStackPtr] != executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method2855(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method5017(icomponentdefinitions3, executor);
    }

    public static void method2856(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int position = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        Queue class4774 = WorldMap.getRenderedElements(position >> 14 & 0x3fff, position & 0x3fff);
        boolean bool5 = false;
        for (WorldMapElement class282Sub50Sub66 = (WorldMapElement) class4774.getNext(); class282Sub50Sub66 != null; class282Sub50Sub66 = (WorldMapElement) class4774.getDirectNext()) {
            if (i_3 == class282Sub50Sub66.anInt9536) {
                bool5 = true;
                break;
            }
        }
        if (bool5) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method3768(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        StaticMethods.method14263(i_2 >> 14 & 0x3fff, i_2 & 0x3fff);
    }

    public static void method3769(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentGroundItem.getIconHeight();
    }

    public static void method14491(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = FriendsChat.MY_PLAYER_FC_RANK;
    }

    public static void method14492(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY.list(i_2).messages[i_3];
    }

    public static void method283(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        ifSetAspect(icomponentdefinitions3, interface4, executor);
    }

    public static void method284(CS2Executor executor) {
        boolean bool2 = executor.intStack[--executor.intStackPtr] == 1;
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.removeRoofs, bool2 ? 2 : 1);
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.removeRoofsOptionOverride, bool2 ? 2 : 1);
        Scene.setRoofOverride();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method287(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = (int) (Utils.time() / 86400000L) - 11745;
    }

    public static void method481(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.graphics.getValue();
    }

    public static void method484(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method4160(icomponentdefinitions3, executor);
    }

    public static void method485(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method1642(icomponentdefinitions4, executor);
    }

    public static void method14606(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.GAME_SCREEN_INTERFACE == null ? -1 : client.GAME_SCREEN_INTERFACE.idHash;
    }

    public static void method7322(CS2Executor executor) {
        method11160(MiniMenu.method8727(), executor);
    }

    public static void method6762(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method3338(icomponentdefinitions3, executor);
    }

    public static void method8723(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method3338(icomponentdefinitions3, executor);
    }

    public static void method8724(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (Interface.INTERFACES[i_2] == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Interface.INTERFACES[i_2].components[i_3].serverTriggers;
        }
    }

    public static void method8726(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method797(icomponentdefinitions3, interface4, executor);
    }

    public static void method7270(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method502(icomponentdefinitions3, executor);
    }

    public static void method7274(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.lootshareRank;
    }

    public static void method7276(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapElement class282Sub50Sub63 = WorldMap.poll(i_2);
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.bitpackedPlacement >> 14 & 0x3fff;
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.bitpackedPlacement & 0x3fff;
        }
    }

    public static void method7277() {
        StaticMedia.resetChatMediaAndIcons();
        FontMetrics.useBoldFont = false;
    }

    public static void method2091(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        QuickchatMessageType quickchatdefinitions5 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_2);
        if (quickchatdefinitions5.getValueType(i_3).id != 0) {
            throw new RuntimeException("");
        } else {
            executor.intStack[++executor.intStackPtr - 1] = quickchatdefinitions5.method14901(i_3, i_4);
        }
    }

    public static void method2092(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).levelRequirements[executor.intStack[executor.intStackPtr - 1]][0];
        --executor.intStackPtr;
    }

    public static void getVarnbitOld(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.intStack[++executor.intStackPtr - 1] = ((NpcEntity) executor.currentEntity).varns.getVarnBit(i_2);
    }

    public static void method2096(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            Friend class63 = FriendList.FRIENDS[i_2];
            executor.stringStack[++executor.stringStackPtr - 1] = class63.displayName;
            if (class63.username != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = class63.username;
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = "";
            }
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void ccSetHide(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method786(icomponentdefinitions4, interface5, executor);
    }

    public static void method2639(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        NpcEntity npc3 = (NpcEntity) executor.currentEntity;
        int i_4 = npc3.getStat(i_2);
        int i_5 = npc3.getCurrentStat(i_2);
        executor.intStack[++executor.intStackPtr - 1] = i_4;
        executor.intStack[++executor.intStackPtr - 1] = i_5;
    }

    public static void animateComp(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method797(icomponentdefinitions3, interface4, executor);
    }

    public static void method8742() {
        Camera.method12606();
    }

    public static void returnInstr(CS2Executor executor) {
        if (executor.returnValuesPtr != 0) {
            CS2ReturnValue class5092 = executor.returnValues[--executor.returnValuesPtr];
            executor.current = class5092.aCacheableNodeSub55869;
            executor.operations = executor.current.operations;
            executor.intOpValues = executor.current.intOpValues;
            executor.instrPtr = class5092.anInt5866;
            executor.intLocals = class5092.anIntArray5867;
            executor.objectLocals = class5092.anObjectArray5865;
            executor.longLocals = class5092.aLongArray5868;
        }
    }

    public static void pushGroundItemHintPosition(CS2Executor executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentGroundItem, executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[0];
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[1];
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[2];
    }

    public static void switchInstr(CS2Executor executor) {
        IterableNodeMap iterablenodemap2 = executor.current.switchMaps[executor.intOpValues[executor.instrPtr]];
        IntNode class282Sub383 = (IntNode) iterablenodemap2.get(executor.intStack[--executor.intStackPtr]);
        if (class282Sub383 != null) {
            executor.instrPtr += class282Sub383.value;
        }
    }

    public static void method5314(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        ifSetColor(icomponentdefinitions4, interface5, executor);
    }

    public static void callCS2Script(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        CS2Script cs2script3 = CS2Script.getCs2Script(i_2);
        if (cs2script3 == null) {
            throw new RuntimeException();
        } else {
            int[] ints_4 = new int[cs2script3.intLocalsCount];
            Object[] arr_5 = new Object[cs2script3.stringLocalsCount];
            long[] longs_6 = new long[cs2script3.longLocalsCount];
            int i_7;
            for (i_7 = 0; i_7 < cs2script3.intArgsCount; i_7++) {
                ints_4[i_7] = executor.intStack[i_7 + (executor.intStackPtr - cs2script3.intArgsCount)];
            }
            for (i_7 = 0; i_7 < cs2script3.stringArgsCount; i_7++) {
                arr_5[i_7] = executor.stringStack[i_7 + (executor.stringStackPtr - cs2script3.stringArgsCount)];
            }
            for (i_7 = 0; i_7 < cs2script3.longArgsCount; i_7++) {
                longs_6[i_7] = executor.longStack[i_7 + (executor.longStackPtr - cs2script3.longArgsCount)];
            }
            executor.intStackPtr -= cs2script3.intArgsCount;
            executor.stringStackPtr -= cs2script3.stringArgsCount;
            executor.longStackPtr -= cs2script3.longArgsCount;
            CS2ReturnValue class5098 = new CS2ReturnValue();
            class5098.aCacheableNodeSub55869 = executor.current;
            class5098.anInt5866 = executor.instrPtr;
            class5098.anIntArray5867 = executor.intLocals;
            class5098.anObjectArray5865 = executor.objectLocals;
            class5098.aLongArray5868 = executor.longLocals;
            if (executor.returnValuesPtr >= executor.returnValues.length) {
                throw new RuntimeException();
            } else {
                executor.returnValues[++executor.returnValuesPtr - 1] = class5098;
                executor.current = cs2script3;
                executor.operations = executor.current.operations;
                executor.intOpValues = executor.current.intOpValues;
                executor.instrPtr = -1;
                executor.intLocals = ints_4;
                executor.objectLocals = arr_5;
                executor.longLocals = longs_6;
            }
        }
    }

    public static void method15449(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.bloom.getValue() == 1 ? 1 : 0;
    }

    public static void method2549(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = DisplayModeDetails.getProfilingPerformance(PreferenceManager.preferences.activeToolkit.getValue(), 200);
    }

    public static void method2552(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.CURRENT_CURSOR;
    }

    public static void method2554(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int i_3 = PreferenceManager.preferences.musicVolume.getValue();
        if (i_2 != i_3) {
            if (GameState.isRenderingScene(client.gameState)) {
                if (i_3 == 0 && AreaSoundPlayer.currentMusicArchive != -1) {
                    AreaSoundPlayer.method13400(Resource.MUSIC, AreaSoundPlayer.currentMusicArchive, i_2);
                    SongPlayer.method12658();
                    SoundEffectPlayer.soundJinglePlaying = false;
                } else if (i_2 == 0) {
                	SoundResourceManager.discardSongData();
                    SoundEffectPlayer.soundJinglePlaying = false;
                } else {
                    SoundEffectPlayer.method1491(i_2);
                }
            }
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.musicVolume, i_2);
            PreferenceManager.savePreferences();
            client.preferencesSavedOnServer = false;
        }
    }

    public static void method7032(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.width;
    }

    public static void method14659(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.scrollHeight;
    }

    public static void method14660(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        method1494(icomponentdefinitions3, executor);
    }

    public static void setCompTransparency(CS2Executor executor) {
        int interfaceId = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(interfaceId);
        Interface interface4 = Interface.INTERFACES[interfaceId >> 16];
        assert icomponentdefinitions3 != null;
        setTransparency(icomponentdefinitions3, executor);
    }

    public static void method1259(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method13450(icomponentdefinitions3, executor);
    }

    public static void removetags(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        StringBuilder stringbuilder3 = new StringBuilder(string_2.length());
        boolean bool4 = false;
        for (int i_5 = 0; i_5 < string_2.length(); i_5++) {
            char var_6 = string_2.charAt(i_5);
            if (var_6 == 60) {
                bool4 = true;
            } else if (var_6 == 62) {
                bool4 = false;
            } else if (!bool4) {
                stringbuilder3.append(var_6);
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = stringbuilder3.toString();
    }

    public static void method3938(CS2Executor executor) {
        executor.intStackPtr -= 2;
    }

    public static void method3939() {
        PacketEncoder.closeInterface();
    }

    public static void method3940(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method6689(icomponentdefinitions3, executor);
    }

    public static void ccSetDepthChild(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setDepthChild(icomponentdefinitions3, executor);
    }

    public static void setMonoStero(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.monoStereo, executor.intStack[--executor.intStackPtr] == 1 ? 1 : 0);
        SoundEffectPlayer.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method2823(CS2Executor executor) {
        executor.intStackPtr -= 4;
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], false, 256);
    }

    public static void method2824(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        String str4 = "";
        if (chatline3 != null && chatline3.message != null) {
            str4 = chatline3.message;
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4;
    }

    public static void method2825(CS2Executor executor) {
        if (client.base64Url != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method2826(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method12260(icomponentdefinitions3, interface4, executor);
    }

    public static void getItemParam(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault4 = TypeLists.PARAMS.list(i_3);
        if (attributedefault4.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ.list(i_2).getCs2String(i_3, attributedefault4.typeName);
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).getCs2Integer(i_3, attributedefault4.defaultInt);
        }
    }

    public static void method2729(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.CONTAINER.list(i_2).maxItemSize;
    }

    public static void ccSetNoClickThrough(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setNoClickThrough(icomponentdefinitions3, executor);
    }

    public static void method3164(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = IgnoreList.IGNORED[i_2].temporary ? 1 : 0;
    }

    public static void method3168(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        Component icomponentdefinitions5 = Interface.getSub(interface4, icomponentdefinitions3);
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions5 == null ? -1 : icomponentdefinitions5.idHash;
    }

    public static void ccSetSize(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method12260(icomponentdefinitions4, interface5, executor);
    }

    public static void method6117(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        int i_3 = executor.intStack[--executor.intStackPtr];
        if (i_3 == -1) {
            throw new RuntimeException("");
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2 + (char) i_3;
        }
    }

    public static void method6120(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method1083(icomponentdefinitions3, executor);
    }

    public static void method6121(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = MiniMenu.entryCount;
        executor.intStack[++executor.intStackPtr - 1] = MiniMenu.anInt170;
    }

    public static void method1084(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6158(icomponentdefinitions3, executor);
    }

    public static void method1085(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.currentQuickChatMessage.anIntArray4046[i_2] = i_3;
    }

    public static void storeInt(CS2Executor executor) {
        executor.intLocals[executor.intOpValues[executor.instrPtr]] = executor.intStack[--executor.intStackPtr];
    }

    public static void method13057(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = i_2 >> 14 & 0x3fff;
    }

    public static void method1467(CS2Executor executor) {
        if (client.PLAYER_MOD_LEVEL >= 5 && client.PLAYER_MOD_LEVEL <= 9) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method2169(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method869(icomponentdefinitions3, executor);
    }

    //Deprecated jsobject applet BS
    public static void method2171() {
    }

    public static void method6439(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_2).getValueType(i_3).id;
        executor.intStack[++executor.intStackPtr - 1] = i_4;
    }

    public static void method6440(CS2Executor executor) {
        String str2 = "";
        if (Console.clipboard != null) {
            Transferable transferable3 = Console.clipboard.getContents(null);
            if (transferable3 != null) {
                try {
                    str2 = (String) transferable3.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception ignored) {
                }
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str2;
    }

    public static void method5045(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.parentId;
    }

    public static void setPrivateFilter(CS2Executor executor) {
        if (ChatBox.PRIVATE_FILTER == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = ChatBox.PRIVATE_FILTER.id;
        }
    }

    public static void method5047(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.brightness, i_2);
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method4413(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (!Renderers.activeToolkit.hasBloomSupport()) {
            executor.intStack[++executor.intStackPtr - 1] = 3;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.bloom.canModify(i_2);
        }
    }

    public static void method4417(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt103;
    }

    public static void method12881(CS2Executor executor) {
        if (client.CLAN_SETTINGS != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
            executor.currentClanSettings = client.CLAN_SETTINGS;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method11350(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.offsetX;
    }

    public static void method11351(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.sceneryShadows.canModify(i_2);
    }

    public static void method11355(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS[i_2].worldName;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method7676(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        int i_4 = -1;
        int i_5 = -1;
        InteractiveComponentSprite class1196 = icomponentdefinitions3.addInteractiveSprite(Renderers.activeToolkit);
        if (class1196 != null) {
            i_4 = class1196.width;
            i_5 = class1196.height;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
        executor.intStack[++executor.intStackPtr - 1] = i_5;
    }

    public static void method892(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.hidden ? 1 : 0;
    }

    public static void method6212(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteId;
    }

    public static void bitFlagged(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = (i_2 & 1 << i_3) != 0 ? 1 : 0;
    }

    public static void method11592(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).varbitRequirementNames[executor.intStack[executor.intStackPtr - 1]];
        executor.intStackPtr -= 2;
    }

    public static void method11593(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method4772(icomponentdefinitions3, executor);
    }

    public static void method11594(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_SUBMIT_CODE, client.LOBBY_CONNECTION_CONTEXT.isaac);
        tcpmessage3.buffer.writeByte(TextUtils.getLength(string_2));
        tcpmessage3.buffer.writeString(string_2);
        client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
    }

    public static void method7715(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.GRAND_EXCHANGE_SLOTS[i_2].currentAmount;
    }

    public static void method5332(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method8310(icomponentdefinitions3, executor);
    }

    public static void method5335(CS2Executor executor) {
        executor.intStackPtr -= 5;
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], false, executor.intStack[executor.intStackPtr + 4]);
    }

    public static void method7082(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.monoStereo.getValue() == 1 ? 1 : 0;
    }

    public static void method7083(CS2Executor executor) {
        QuestType questdefinitions2 = TypeLists.QUEST.list(executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = questdefinitions2.varbitRequirements == null ? 0 : questdefinitions2.varbitRequirements.length;
    }

    public static void method584(CS2Executor executor) {
        CommandExecutor.executeCommand((String) executor.stringStack[--executor.stringStackPtr], false, false);
    }

    public static void method585(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.lightDetail.getValue() == 1 ? 1 : 0;
    }

    public static void method586(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.PLAYER_DOB;
    }

    //Deprecated jsobject applet BS
    public static void method587() {
    }

    public static void method1949(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method569(icomponentdefinitions3, executor);
    }

    public static void method1951(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int i_3 = client.GRAND_EXCHANGE_SLOTS[i_2].method5908();
        executor.intStack[++executor.intStackPtr - 1] = i_3 == 5 ? 1 : 0;
    }

    public static void method4898(CS2Executor executor) {
        Component icomponentdefinitions2 = Component.fromHash(executor.intStack[--executor.intStackPtr]);
        assert icomponentdefinitions2 != null;
        if (icomponentdefinitions2.slotChildren == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            int i_3 = icomponentdefinitions2.slotChildren.length;
            for (int i_4 = 0; i_4 < icomponentdefinitions2.slotChildren.length; i_4++) {
                if (icomponentdefinitions2.slotChildren[i_4] == null) {
                    i_3 = i_4;
                    break;
                }
            }
            executor.intStack[++executor.intStackPtr - 1] = i_3;
        }
    }

    public static void method4899(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        int i_3 = PreferenceManager.preferences.soundLevel.getValue();
        if (i_3 != i_2 && AreaSoundPlayer.titlescreenMusicArchive == AreaSoundPlayer.currentMusicArchive) {
            if (!GameState.isRenderingScene(client.gameState)) {
                if (i_3 == 0) {
                    AreaSoundPlayer.method13400(Resource.MUSIC, AreaSoundPlayer.currentMusicArchive, i_2);
                    SongPlayer.method12658();
                    SoundEffectPlayer.soundJinglePlaying = false;
                } else if (i_2 == 0) {
                	SoundResourceManager.discardSongData();
                    SoundEffectPlayer.soundJinglePlaying = false;
                } else {
                    SoundEffectPlayer.method1491(i_2);
                }
            }
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.soundLevel, i_2);
            PreferenceManager.savePreferences();
            client.preferencesSavedOnServer = false;
        }
    }

    public static void method4900(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.textures.getValue() == 1 ? 1 : 0;
    }

    public static void method4901(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.scrollWidth;
    }

    public static void method4902(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        PacketEncoder.method3345(string_2);
    }

    public static void method15437(CS2Executor executor) {
        if (client.gameState == GameState.UNK_0 && !LoginManager.method3360()) {
            if (WorldConnection.aBool5422) {
                executor.intStack[++executor.intStackPtr - 1] = 0;
            } else if (WorldConnection.aLong5425 > Utils.time() - 1000L) {
                executor.intStack[++executor.intStackPtr - 1] = 1;
            } else {
                WorldConnection.aBool5422 = true;
                TcpPacket tcpmessage2 = PacketEncoder.createPacket(ClientProt.REQUEST_WORLD_LIST, client.LOBBY_CONNECTION_CONTEXT.isaac);
                tcpmessage2.buffer.writeInt(StaticMethods.WORLD_LIST_IDK);
                client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage2);
                executor.intStack[++executor.intStackPtr - 1] = 0;
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        }
    }

    public static void containerTotalParam(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getParamBasedContainerTotal(i_2, i_3, false);
    }

    public static void method3229(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).multiStackSize;
    }

    public static void ifSetfont(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setIfFont(icomponentdefinitions3, interface4, executor);
    }

    public static void method3233() {
        PreferenceManager.loadHighDetailPreferences();
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method11492(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method7267(icomponentdefinitions3, executor);
    }

    public static void method6304(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_2).method14916();
    }

    public static void method6305(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method7927(icomponentdefinitions3, executor);
    }

    public static void method11473(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.scrollY;
    }

    public static void method3200(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.voiceOverVolume, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method3201(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        method15207(icomponentdefinitions3, executor);
    }

    public static void loadMessagebox(CS2Executor executor) {
        executor.intStackPtr -= 11;
        AnchorX[] anchorX = AnchorX.values();
        AnchorY[] anchorY = AnchorY.values();
        MessageBox.initialize(
        		anchorX[executor.intStack[executor.intStackPtr]],
        		anchorY[executor.intStack[executor.intStackPtr + 1]],
        		executor.intStack[executor.intStackPtr + 2], 
        		executor.intStack[executor.intStackPtr + 3], 
        		executor.intStack[executor.intStackPtr + 4], 
        		executor.intStack[executor.intStackPtr + 5], 
        		executor.intStack[executor.intStackPtr + 6], 
        		executor.intStack[executor.intStackPtr + 7], 
        		executor.intStack[executor.intStackPtr + 8], 
        		executor.intStack[executor.intStackPtr + 9], 
        		executor.intStack[executor.intStackPtr + 10]);
    }

    public static void method6908(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.toolkit.getValue();
    }

    public static void method1497(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method2915(icomponentdefinitions3, executor);
    }

    public static void method1498(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.offsetY;
    }

    public static void method1499(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = (int) (LoginManager.MEMBERSHIP_END / 60000L);
        executor.intStack[++executor.intStackPtr - 1] = (int) ((LoginManager.MEMBERSHIP_END - Utils.time() - LoginManager.aLong420) / 60000L);
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.IS_MEMBER_SUBSCRIPTION ? 1 : 0;
    }

    public static void method1500(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        FriendList.add(string_2);
    }

    public static void activeClanchannelFindListened(CS2Executor executor) {
        if (client.LISTENED_CLAN_CHANNEL != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
            executor.clanChannel = client.LISTENED_CLAN_CHANNEL;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method14602(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.fontId;
    }

    public static void bitAnd(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 & i_3;
    }

    public static void method8303(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt106;
    }

    public static void ifSetOnMouseOver(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        setOnMouseOver(icomponentdefinitions3, executor);
    }

    public static void method6738(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method13450(icomponentdefinitions3, executor);
    }

    public static void method6741(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.memberJoinDays[i_2];
    }

    public static void method541(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setResumeText(icomponentdefinitions3, executor);
    }

    public static void method543(CS2Executor executor) {
        executor.currentEntity.method15813(executor.intStack[--executor.intStackPtr]);
    }

    public static void setBit(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 | 1 << i_3;
    }

    public static void method546(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.aByte7458;
        if (client.aByte7458 != -1) {
            client.aByte7458 = -6;
        }
    }

    public static void method548(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(9, i_2 << 16 | i_3, i_4, "");
    }

    public static void charIsnumeric(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = Utils.isNumber((char) i_2) ? 1 : 0;
    }

    public static void method8018(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method7142(icomponentdefinitions3, executor);
    }

    public static void method8019(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED != 0 && i_2 < IgnoreList.IGNORE_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = IgnoreList.IGNORED[i_2].unfilteredUsername;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method8021(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(5, i_2 << 16 | i_3, i_4, "");
    }

    public static void method8045(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        int i_4 = executor.intStack[--executor.intStackPtr];
        --i_4;
        assert icomponentdefinitions3 != null;
        if (icomponentdefinitions3.options != null && i_4 < icomponentdefinitions3.options.length && icomponentdefinitions3.options[i_4] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3.options[i_4];
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method1813(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method11221(icomponentdefinitions3, executor);
    }

    public static void chatlineGeteffectflags(CS2Executor executor) {
        int line = executor.intStack[--executor.intStackPtr];
        ChatLine chatline = ChatBox.getLine(line);
        int effectFlags = 0;
        if (chatline != null) {
            effectFlags = chatline.effectFlags;
        }
        executor.intStack[++executor.intStackPtr - 1] = effectFlags;
    }

    public static void method1815(CS2Executor executor) {
        executor.intStackPtr -= 2;
        client.aShort7394 = (short) executor.intStack[executor.intStackPtr];
        if (client.aShort7394 <= 0) {
            client.aShort7394 = 256;
        }
        client.aShort7324 = (short) executor.intStack[executor.intStackPtr + 1];
        if (client.aShort7324 <= 0) {
            client.aShort7324 = 320;
        }
    }

    public static void getJcoins(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.JCOINS;
    }

    public static void getColTag(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = TextUtils.setTextColor(i_2);
    }

    public static void method7332(CS2Executor executor) {
        getBoundingBox((SceneGraphEntity) executor.currentLocation, executor);
    }

    public static void method7334(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.customCursor.getValue() == 1 ? 1 : 0;
    }

    public static void method12846(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6697(icomponentdefinitions3, interface4, executor);
    }

    public static void method7726(CS2Executor executor) {
        executor.intStackPtr -= 4;
        int i_2 = executor.intStack[executor.intStackPtr];
        boolean bool3 = executor.intStack[executor.intStackPtr + 1] == 1;
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        boolean bool5 = executor.intStack[executor.intStackPtr + 3] == 1;
        WorldConnection.method6299(i_2, bool3, i_4, bool5);
    }

    public static void setBloom(CS2Executor executor) {
        int enabled = executor.intStack[--executor.intStackPtr];
        if (enabled < 0 || enabled > 1) {
            enabled = 0;
        }
        PreferenceManager.setBloom(enabled == 1);
    }

    public static void method1509(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method4159(icomponentdefinitions3, executor);
    }

    public static void method1510(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method5066(icomponentdefinitions3, interface4, executor);
    }

    public static void ccSetTargetVerb(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setTargetVerb(icomponentdefinitions3, executor);
    }

    public static void method1512(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.mediaType == MediaType.RAW_MODEL ? icomponentdefinitions3.primaryMediaId : -1;
    }

    public static void arrayStore(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.intStackPtr -= 2;
        int i_3 = executor.intStack[executor.intStackPtr];
        if (i_3 >= 0 && i_3 < executor.globalArrayLengths[i_2]) {
            executor.globalArrays[i_2][i_3] = executor.intStack[executor.intStackPtr + 1];
        } else {
            throw new RuntimeException();
        }
    }

    public static void method4904(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method3365(icomponentdefinitions3, executor);
    }

    public static void method4905(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYERS[i_2].worldId;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void pushEntityHintPosition(CS2Executor executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentEntity, executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[0];
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[1];
        executor.intStack[++executor.intStackPtr - 1] = (int) client.HINT_POSITION[2];
    }

    public static void method5814(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        CS2Interface underlaydefinition4;
        if (executor.aBool7022) {
            underlaydefinition4 = executor.hookedInterface2;
        } else {
            underlaydefinition4 = executor.hookedInterface1;
        }
        executor.intStack[++executor.intStackPtr - 1] = underlaydefinition4.load(executor.currentEntity.inter, i_2) ? 1 : 0;
    }

    public static void method5816() {
        LoginManager.prepareHost();
    }

    public static void method2150(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method869(icomponentdefinitions3, executor);
    }

    public static void getStructParam(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ParamType attributedefault4 = TypeLists.PARAMS.list(i_3);
        if (attributedefault4.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.STRUCT.getStruct(i_2).getString(i_3, attributedefault4.typeName);
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.STRUCT.getStruct(i_2).getInteger(i_3, attributedefault4.defaultInt);
        }
    }

    public static void method2153(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt113;
    }

    public static void getContainerFreeSpace(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getFreeSlots(i_2);
    }

    public static void method3986(CS2Executor executor) {
        if (ColorImageCache.CS2_QUERY_RESULTS != null && StaticElements.CS2_QUERY_RESULT_IDX < CS2_QUERY_RESULTS_LEN) {
            executor.intStack[++executor.intStackPtr - 1] = ColorImageCache.CS2_QUERY_RESULTS[++StaticElements.CS2_QUERY_RESULT_IDX - 1] & 0xffff;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        }
    }

    public static void method15416(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method12209(icomponentdefinitions3, executor);
    }

    public static void method6733(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        String str4 = "";
        if (chatline3 != null && chatline3.clan != null) {
            str4 = chatline3.clan;
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4;
    }

    public static void getVarnOld(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.intStack[++executor.intStackPtr - 1] = ((NpcEntity) executor.currentEntity).varns.getVarn(i_2);
    }

    public static void loadLong(CS2Executor executor) {
        executor.longStack[++executor.longStackPtr - 1] = executor.longLocals[executor.intOpValues[executor.instrPtr]];
    }

    public static void method13491(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.parentId;
    }

    public static void getWorldLanguage(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.CURRENT_LANGUAGE.getValue();
    }

    public static void method4194(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = (int) (CalenderUtil.method5363(i_2) / 60000L);
    }

    public static void method6236(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteRoll;
    }

    public static void method6237(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        String str4 = "";
        if (chatline3 != null && chatline3.crownedName != null) {
            str4 = chatline3.crownedName;
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4;
    }

    public static void method6238(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.isForcedSafemode ? 1 : 0;
    }

    public static void method6239() {
        PreferenceManager.loadMinDetailPreferences();
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void appendTypedMessage(CS2Executor executor) {
        executor.intStackPtr -= 2;
        ChatMessageType type = ChatMessageType.forId(executor.intStack[executor.intStackPtr]);
        int effectFlags = executor.intStack[executor.intStackPtr + 1];
        String message = (String) executor.stringStack[--executor.stringStackPtr];
        if (type == ChatMessageType.DEV_CONSOLE) {
            Console.writeLine(message);
        } else if (type == ChatMessageType.DEV_CONSOLE_CLEAR) {
            Console.setConsoleText(message);
        } else {
            ChatBox.writeMessage(type, effectFlags, "", "", "", message);
        }
    }

    public static void method6230(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getAmountAtSlot(i_2, i_3, true);
    }

    public static void method6231(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.GRAND_EXCHANGE_SLOTS[i_2].method5909();
    }

    public static void getCompHidden(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.hidden ? 1 : 0;
    }

    public static void method1613(CS2Executor executor) {
        executor.intStackPtr -= 4;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        int i_5 = executor.intStack[executor.intStackPtr + 3];
        i_2 += i_3 << 14;
        i_2 += i_4 << 28;
        i_2 += i_5;
        executor.intStack[++executor.intStackPtr - 1] = i_2;
    }

    public static void method6729(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        boolean bool3 = executor.intStack[executor.intStackPtr + 1] == 1;
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, i_2);
        if (!bool3) {
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.graphics, 0);
        }
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method6730(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = ((PlayerEntity) executor.currentEntity).getUsernameWithTitle();
    }

    public static void getEnumSize(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        EnumType enumdefinitions3 = TypeLists.ENUM.list(i_2);
        executor.intStack[++executor.intStackPtr - 1] = enumdefinitions3.getSize();
    }

    public static void method12596(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.maxScreenSize.canModify(i_2);
    }

    public static void method12597(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = (int) (client.aLong7153 >> 32);
        executor.intStack[++executor.intStackPtr - 1] = (int) (client.aLong7153);
    }

    public static void ccDelete(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        if (underlaydefinition2.comp.slotId == -1) {
            throw new RuntimeException("");
        } else {
            Component icomponentdefinitions3 = underlaydefinition2.method8772();
            icomponentdefinitions3.slotChildren[underlaydefinition2.comp.slotId] = null;
            Component.redrawComponent(icomponentdefinitions3);
        }
    }

    public static void hookMouseExit(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method11223(icomponentdefinitions3, executor);
    }

    public static void getMouseX(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder.getMouseX();
    }

    public static void lowerString(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = string_2.toLowerCase();
    }

    public static void resumeStringDialog(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.RESUME_TEXTDIALOG, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage3.buffer.writeByte(string_2.length() + 1);
        tcpmessage3.buffer.writeString(string_2);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
    }

    public static void method1456(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = (int) Camera.camAngleX >> 3;
    }

    public static void method14655(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.RECOVERYQUESTIONSSETDATE;
    }

    public static void method6073(CS2Executor executor) {
        WorldMapElement class282Sub50Sub62 = WorldMap.poll(executor.intStack[--executor.intStackPtr]);
        if (class282Sub50Sub62 != null) {
            if (class282Sub50Sub62.aString9534 != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = class282Sub50Sub62.aString9534;
                return;
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = "";
    }

    public static void loadClanSettingVar(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        ClanVarSettingsType class5373 = TypeLists.CLAN_VAR_SETTINGS.list(i_2);
        if (class5373 == null) {
            throw new RuntimeException();
        } else {
            Integer integer4 = executor.currentClanSettings.getIntVar(client.CURRENT_GAME.id << 16 | i_2);
            int i_5;
            if (integer4 == null) {
                if (class5373.aChar7096 != 105 && class5373.aChar7096 != 49) {
                    i_5 = -1;
                } else {
                    i_5 = 0;
                }
            } else {
                i_5 = integer4.intValue();
            }
            executor.intStack[++executor.intStackPtr - 1] = i_5;
        }
    }

    public static void method7556(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        if (executor.intStack[--executor.intStackPtr] == 1) {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3;
        }
    }

    public static void method7560(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method6010(icomponentdefinitions3, interface4, executor);
    }

    public static void ccSetPosition(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setComponentPosition(icomponentdefinitions3, interface4, executor);
    }

    public static void method7434(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).difficulty;
    }

    public static void method7444(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.SKILL_XP[i_2];
    }

    public static void method7445(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).wearPos;
    }

    public static void method7448(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.cpu.getValue();
    }

    public static void method11246(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method4136(icomponentdefinitions3, executor);
    }

    public static void method11247(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.transparency;
    }

    public static void method11249(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteScale;
    }

    public static void method254(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int[] ints_4 = executor.intStack;
        int i_5 = ++executor.intStackPtr - 1;
        byte b_6;
        if (Interface.method4248(i_2, i_3)) {
            b_6 = 1;
        } else {
            b_6 = 0;
        }
        ints_4[i_5] = b_6;
    }

    public static void method15404(CS2Executor executor) {
        executor.intStackPtr -= 4;
        client.aShort7276 = (short) executor.intStack[executor.intStackPtr];
        if (client.aShort7276 <= 0) {
            client.aShort7276 = 1;
        }
        client.aShort7441 = (short) executor.intStack[executor.intStackPtr + 1];
        if (client.aShort7441 <= 0) {
            client.aShort7441 = 32767;
        } else if (client.aShort7441 < client.aShort7276) {
            client.aShort7441 = client.aShort7276;
        }
        client.aShort7214 = (short) executor.intStack[executor.intStackPtr + 2];
        if (client.aShort7214 <= 0) {
            client.aShort7214 = 1;
        }
        client.aShort7474 = (short) executor.intStack[executor.intStackPtr + 3];
        if (client.aShort7474 <= 0) {
            client.aShort7474 = 32767;
        } else if (client.aShort7474 < client.aShort7214) {
            client.aShort7474 = client.aShort7214;
        }
    }

    public static void method356(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).type;
    }

    public static void method7218(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (!Renderers.activeToolkit.hasAntiAliasing()) {
            executor.intStack[++executor.intStackPtr - 1] = 3;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.antiAliasing.canModify(i_2);
        }
    }

    public static void method7219(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.color;
    }

    public static void method6005(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        BufferedConnectionContext class1843 = client.getConnectionContext();
        TcpPacket tcpmessage4 = PacketEncoder.createPacket(ClientProt.CHAT_TYPE, class1843.isaac);
        tcpmessage4.buffer.writeByte(i_2);
        class1843.queuePacket(tcpmessage4);
    }

    public static void sendVerifyEmailPacket(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        PacketEncoder.sendCheckEmailPacket(string_2);
    }

    public static void method11244(CS2Executor executor) {
        WorldDescriptor descriptor = WorldConnection.getNextWorldDescriptor();
        if (descriptor != null) {
            executor.intStack[++executor.intStackPtr - 1] = descriptor.worldNumber;
            executor.intStack[++executor.intStackPtr - 1] = descriptor.flags;
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.unknown;
            WorldMeta world = descriptor.getWorld();
            executor.intStack[++executor.intStackPtr - 1] = world.countryId;
            executor.stringStack[++executor.stringStackPtr - 1] = world.activity;
            executor.intStack[++executor.intStackPtr - 1] = descriptor.playerCount;
            executor.intStack[++executor.intStackPtr - 1] = descriptor.ping;
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.ipAddress;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void sendReportAbusePacket(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        executor.intStackPtr -= 2;
        int i_4 = executor.intStack[executor.intStackPtr];
        int i_5 = executor.intStack[executor.intStackPtr + 1];
        if (string_3 == null) {
            string_3 = "";
        }
        if (string_3.length() > 80) {
            string_3 = string_3.substring(0, 80);
        }
        BufferedConnectionContext class1846 = client.getConnectionContext();
        TcpPacket tcpmessage7 = PacketEncoder.createPacket(ClientProt.REPORT_ABUSE, class1846.isaac);
        tcpmessage7.buffer.writeByte(TextUtils.getLength(string_2) + 2 + TextUtils.getLength(string_3));
        tcpmessage7.buffer.writeString(string_2);
        tcpmessage7.buffer.writeByte(i_4 - 1);
        tcpmessage7.buffer.writeByte(i_5);
        tcpmessage7.buffer.writeString(string_3);
        class1846.queuePacket(tcpmessage7);
    }

    public static void method8037(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings.memberNames[i_2];
    }

    public static void method12492(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method7729(icomponentdefinitions3, executor);
    }

    public static void loadClanVar(CS2Executor executor) {
        int param = executor.intOpValues[executor.instrPtr];
        Integer integer3 = (Integer) ClanVarTypeList.CLAN_VARS[param];
        if (integer3 == null) {
            ClanVarType clanvarType = TypeLists.CLAN_VAR.list(param);
            if (clanvarType.aChar4832 != 105 && clanvarType.aChar4832 != 49) {
                executor.intStack[++executor.intStackPtr - 1] = -1;
            } else {
                executor.intStack[++executor.intStackPtr - 1] = 0;
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = integer3.intValue();
        }
    }

    public static void method4324(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.gametipStartStagePreference.getValue();
    }

    public static void method3038(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 == -1) {
            int i_3 = 16383;
            int i_4 = i_2 & 0x3fff;
            CoordGrid coordgrid5 = Resource.mapRegion.getBaseCoordinates();
            i_3 -= coordgrid5.x;
            if (i_3 < 0) {
                i_3 = 0;
            } else if (i_3 >= Resource.mapRegion.getSceneSizeX()) {
                i_3 = Resource.mapRegion.getSceneSizeX();
            }
            i_4 -= coordgrid5.y;
            if (i_4 < 0) {
                i_4 = 0;
            } else if (i_4 >= Resource.mapRegion.getSceneSizeY()) {
                i_4 = Resource.mapRegion.getSceneSizeY();
            }
            PingRequester.anInt5864 = (i_3 << 9) + 256;
            StaticMethods.anInt833 = (i_4 << 9) + 256;
        } else {
            PingRequester.anInt5864 = -1;
            StaticMethods.anInt833 = -1;
        }
    }

    public static void method3039(CS2Executor executor) {
        WorldDescriptor class217Sub12 = WorldDescriptor.get_world();
        executor.intStack[++executor.intStackPtr - 1] = class217Sub12 == null ? 0 : class217Sub12.flags;
    }

    public static void method3041(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method1845(icomponentdefinitions3, executor);
    }

    public static void method3042(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        ItemType itemdefinitions4 = TypeLists.OBJ.list(i_2);
        if (i_3 >= 1 && i_3 <= 5 && itemdefinitions4.groundActions[i_3 - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = itemdefinitions4.groundActions[i_3 - 1];
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method3043(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel.getUserSlot((String) executor.stringStack[--executor.stringStackPtr]);
    }

    public static void method3713(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.water.canModify(i_2);
    }

    public static void method3714(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(8, i_2 << 16 | i_3, i_4, "");
    }

    public static void method15392(CS2Executor executor) {
        Component icomponentdefinitions2 = Component.fromHash(executor.intStack[--executor.intStackPtr]);
        assert icomponentdefinitions2 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions2.containerItemId;
    }

    public static void loadString(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.objectLocals[executor.intOpValues[executor.instrPtr]];
    }

    public static void method3368() {
        if (!LoginManager.method3360()) {
            LoginManager.method5558();
        }
    }

    public static void method3370(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method542(icomponentdefinitions3, executor);
    }

    public static void loadFriendchatName(CS2Executor executor) {
        if (FriendsChat.FC_NAME != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = StaticMethods.decodeFriendchatName(FriendsChat.FC_NAME);
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method6372(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method542(icomponentdefinitions3, executor);
    }

    public static void method6373(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = Character.toUpperCase((char) i_2);
    }

    public static void method6374(CS2Executor executor) {
        WorldDescriptor descriptor = WorldDescriptor.getFirstWorldDescriptor();
        if (descriptor != null) {
            executor.intStack[++executor.intStackPtr - 1] = descriptor.worldNumber;
            executor.intStack[++executor.intStackPtr - 1] = descriptor.flags;
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.unknown;
            WorldMeta world = descriptor.getWorld();
            executor.intStack[++executor.intStackPtr - 1] = world.countryId;
            executor.stringStack[++executor.stringStackPtr - 1] = world.activity;
            executor.intStack[++executor.intStackPtr - 1] = descriptor.playerCount;
            executor.intStack[++executor.intStackPtr - 1] = descriptor.ping;
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.ipAddress;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void ifSetItemNoNum(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setItemIFComp(icomponentdefinitions3, interface4, false, Component.StackMode.NONUM, executor);
    }

    public static void method7915(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.MESSAGES;
    }

    public static void method3988(CS2Executor executor) {
        if (KeyRecorder.keyRecorder.held(86)) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method6781(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ.list(i_2).wearPos3;
    }

    public static void getCPUProfile(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ProfileCpu.getProfile();
    }

    public static void method3020(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method4276(icomponentdefinitions3, interface4, executor);
    }

    public static void method3021(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel.guestsTalk;
    }

    public static void method7675(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getItemIdAtSlot(i_2, i_3, false);
    }

    public static void method2107() {
        LoginManager.method7777();
    }

    public static void method2108(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.toolkit.canModify(i_2);
    }

    public static void method2112(CS2Executor executor) {
        executor.intStackPtr -= 7;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1] << 1;
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        int i_5 = executor.intStack[executor.intStackPtr + 3];
        int i_6 = executor.intStack[executor.intStackPtr + 4];
        int i_7 = executor.intStack[executor.intStackPtr + 5];
        int i_8 = executor.intStack[executor.intStackPtr + 6];
        if (i_2 >= 0 && i_2 < 2 && client.anIntArrayArrayArray7302[i_2] != null && i_3 >= 0 && i_3 < client.anIntArrayArrayArray7302[i_2].length) {
            client.anIntArrayArrayArray7302[i_2][i_3] = new int[]{(i_4 >> 14 & 0x3fff) << 9, i_5 << 2, (i_4 & 0x3fff) << 9, i_8};
            client.anIntArrayArrayArray7302[i_2][i_3 + 1] = new int[]{(i_6 >> 14 & 0x3fff) << 9, i_7 << 2, (i_6 & 0x3fff) << 9};
        }
    }

    public static void method11486(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.SKILL_LEVEL_ACTUAL[i_2];
    }

    public static void mergeStrings(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.stringStackPtr -= i_2;
        String string_3 = Utils.method6398(executor.stringStack, executor.stringStackPtr, i_2);
        executor.stringStack[++executor.stringStackPtr - 1] = string_3;
    }

    public static void method12499(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method3672(icomponentdefinitions3, executor);
    }

    public static void method810(CS2Executor executor) {
        StaticMethods.method458(executor.intStack[--executor.intStackPtr]);
    }

    public static void method811(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.clanChannel.clanName;
    }

    public static void method814(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        QuickChatCategoryType class282Sub50Sub143 = TypeLists.QUICK_CHAT_CATEGORY.list(i_2);
        if (class282Sub50Sub143.messages == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub143.messages.length;
        }
    }

    public static void ccResumePauseButton(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method6942(icomponentdefinitions3);
    }

    public static void method12689(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY.list(i_2).name;
    }

    public static void method1452(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        IgnoreList.add(string_2, false);
    }

    public static void sendMessage(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        ChatBox.writeUnfilterableMessage(string_2);
    }

    public static void method3340(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.MY_PLAYER.yaw.getRotation() >> 3;
    }

    public static void method3341(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (Interface.INTERFACES[i_2] == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        } else {
            String string_4 = Interface.INTERFACES[i_2].components[i_3].name;
            if (string_4 == null) {
                executor.stringStack[++executor.stringStackPtr - 1] = "";
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = string_4;
            }
        }
    }

    public static void method3344(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.ENTITY_TARGET == executor.currentEntity.getCS2Index() ? 1 : 0;
    }

    public static void method3346(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.intStackPtr -= 2;
        int i_3 = executor.intStack[executor.intStackPtr];
        int i_4 = executor.intStack[executor.intStackPtr + 1];
        FontMetrics fontmetrics5 = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_4);
        assert fontmetrics5 != null;
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics5.method6949(string_2, i_3, StaticMedia.nameIconSprites);
    }

    public static void loadVarpbit(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.intStack[++executor.intStackPtr - 1] = VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(i_2);
    }

    public static void method5203(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method14482(icomponentdefinitions3, executor);
    }

    public static void intLessOrEqual(CS2Executor executor) {
        executor.intStackPtr -= 2;
        if (executor.intStack[executor.intStackPtr] <= executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method5205(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = AccountCreationResponseOpcodes.method1489().getValue();
    }

    public static void modulo(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 % i_3;
    }

    public static void method3829(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_MIN_RANK_CAN_KICK;
    }

    public static void method3830(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.getLanguageTimestring(executor.intStack[--executor.intStackPtr] * 60000L, client.CURRENT_LANGUAGE.getValue()) + " UTC";
    }

    public static void method5065(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.fog.visible() ? 1 : 0;
    }

    public static void method7208(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method15511(icomponentdefinitions3, interface4, executor);
    }

    public static void method7209(CS2Executor executor) {
        MiniMenu.anInt198 = executor.intStack[--executor.intStackPtr];
    }

    public static void longEqual(CS2Executor executor) {
        executor.longStackPtr -= 2;
        if (executor.longStack[executor.longStackPtr + 1] == executor.longStack[executor.longStackPtr]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method15371(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.characterShadows.visible() ? 1 : 0;
    }

    public static void method3030(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ItemType itemdefinitions3 = TypeLists.OBJ.list(i_2);
        executor.intStack[++executor.intStackPtr - 1] = itemdefinitions3.hasTooltipColor ? 1 : 0;
    }

    public static void method3031(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method1454(icomponentdefinitions3, executor);
    }

    public static void method3033(CS2Executor executor) {
        if (client.PLAYER_MOD_LEVEL >= 5 && client.PLAYER_MOD_LEVEL <= 9) {
            executor.intStack[++executor.intStackPtr - 1] = client.PLAYER_MOD_LEVEL;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method15559(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.fontId;
    }

    public static void method4629(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method5455(icomponentdefinitions3, executor);
    }

    public static void method4630(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1] - 1;
        ItemType itemdefinitions4 = TypeLists.OBJ.list(i_2);
        if (i_3 == itemdefinitions4.customCursorOp1) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions4.customCursorId1;
        } else if (i_3 == itemdefinitions4.customCursorOp2) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions4.customCursorId2;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        }
    }

    public static void method4631(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.currentQuickChatMessage.anIntArray4046[i_2] = i_3;
    }

    public static void method4633(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setTargetVerb(icomponentdefinitions3, executor);
    }

    public static void method6938(CS2Executor executor) {
        executor.intStackPtr -= 3;
        byte[] bytes_2 = {(byte) executor.intStack[executor.intStackPtr]};
        byte[] bytes_3 = {(byte) executor.intStack[executor.intStackPtr + 1]};
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Component icomponentdefinitions5 = Component.fromHash(i_4);
        method1838(icomponentdefinitions5, bytes_2, bytes_3, executor);
    }

    public static void method6940(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(6, i_2 << 16 | i_3, i_4, "");
    }

    public static void method6941(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        method5001(icomponentdefinitions3, executor);
    }

    public static void method451(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.lightDetail, executor.intStack[--executor.intStackPtr] == 1 ? 1 : 0);
        GraphicsCache.resetGraphicCaches();
        Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method452(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 != PreferenceManager.preferences.skyboxes.getValue()) {
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.skyboxes, i_2);
            PreferenceManager.savePreferences();
            client.preferencesSavedOnServer = false;
        }
    }

    public static void method453(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.aBool7189 ? 1 : 0;
    }

    public static void method454(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.color;
    }

    public static void method8211(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteYaw;
    }

    public static void method8212(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method11337(icomponentdefinitions3, interface4, executor);
    }

    public static void loadVarp(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.intStack[++executor.intStackPtr - 1] = VarProviders.PLAYER_VAR_PROVIDER.activeVars[i_2];
    }

    public static void method6364(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method4276(icomponentdefinitions3, interface4, executor);
    }

    public static void method3117(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method12401(icomponentdefinitions3, interface4);
    }

    public static void method3119(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt107;
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt109;
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt108;
        StaticMethods.anInt107 = -2;
        StaticMethods.anInt109 = -1;
        StaticMethods.anInt108 = -1;
    }

    public static void method3120() {
        LoginManager.method1478();
    }

    public static void method1603(CS2Executor executor) {
        BufferedConnectionContext class1842 = client.getConnectionContext();
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842.isaac);
        tcpmessage3.buffer.writeByte(0);
        int i_4 = tcpmessage3.buffer.currentPosition;
        tcpmessage3.buffer.writeByte(3);
        tcpmessage3.buffer.writeShort(executor.currentQuickChatMessage.id);
        executor.currentQuickChatMessage.message.method14896(tcpmessage3.buffer, executor.currentQuickChatMessage.anIntArray4046);
        tcpmessage3.buffer.writeIndex(tcpmessage3.buffer.currentPosition - i_4);
        class1842.queuePacket(tcpmessage3);
    }

    public static void method1604(CS2Executor executor) {
        System.out.println(executor.stringStack[--executor.stringStackPtr]);
    }

    public static void method1606(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method15409(icomponentdefinitions3, executor);
    }

    public static void method1608(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendsChat.FC_PLAYERS != null && i_2 < FriendsChat.FC_PLAYER_COUNT && FriendsChat.FC_PLAYERS[i_2].username.equalsIgnoreCase(PlayerUpdate.MY_PLAYER.displayName)) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void resumeNamedDialog(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.RESUME_NAMEDIALOG, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage3.buffer.writeByte(string_2.length() + 1);
        tcpmessage3.buffer.writeString(string_2);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
    }

    public static void method5788(CS2Executor executor) {
        WorldMapCategory class282Sub362 = WorldMapSub1.method12538();
        if (class282Sub362 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub362.mecId;
            int i_3 = class282Sub362.anInt7988 << 28 | class282Sub362.absX + WorldMap.mapSquareAbsX << 14 | class282Sub362.absY + WorldMap.mapSquareAbsY;
            executor.intStack[++executor.intStackPtr - 1] = i_3;
        }
    }

    public static void method12583(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method3601(icomponentdefinitions3, executor);
    }

    public static void getWorldId(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.GAME_CONNECTION_INFO.worldId;
    }

    public static void method1888(CS2Executor executor) {
        executor.intStackPtr -= 5;
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], executor.intStack[executor.intStackPtr + 4]);
    }

    public static void method1889(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.kickRank;
    }

    public static void powInverse(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (i_2 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else if (i_3 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = Integer.MAX_VALUE;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = (int) Math.pow(i_2, 1.0D / i_3);
        }
    }

    public static void checkJavaVersion(CS2Executor executor) {
        if (SystemInfo.SYSTEM_INFO.javaBuild < 6) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else if (SystemInfo.SYSTEM_INFO.javaBuild == 6 && SystemInfo.SYSTEM_INFO.javaUpdate < 10) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        }
    }

    public static void ifSetItem(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setItemIFComp(icomponentdefinitions3, interface4, false, Component.StackMode.WHEN_MULTIPLE, executor);
    }

    public static void method5027(CS2Executor executor) {
        executor.intStackPtr -= 2;
        SoundEffectPlayer.playSoundJingle(executor.intStack[executor.intStackPtr], 255);
    }

    public static void method5029(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method15207(icomponentdefinitions3, executor);
    }

    public static void method14746(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method2593(icomponentdefinitions3, executor);
    }

    public static void method8735(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions4.transparency;
    }

    public static void getRunEnergy(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.RUN_ENERGY;
    }

    public static void method3596(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method2593(icomponentdefinitions3, executor);
    }

    public static void method3597(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2].friendsChatRank;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void setEffectVolume(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.soundEffectVolume, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method855(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        method3965(icomponentdefinitions3, executor);
    }

    public static void method856(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapElement class282Sub50Sub63 = WorldMap.poll(i_2);
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9542;
        }
    }

    public static void method858(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.scrollWidth;
    }

    public static void method5290(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).isComplete(VarProviders.PLAYER_VAR_PROVIDER) ? 1 : 0;
    }

    public static void hsvToRGB(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(i_2) & 0xffff];
    }

    public static void method12802(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method4651(icomponentdefinitions3, executor);
    }

    public static void method12803(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.fog, executor.intStack[--executor.intStackPtr] == 1 ? 1 : 0);
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void pushInt(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.intOpValues[executor.instrPtr];
    }

    public static void method6510(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method1455(icomponentdefinitions3, interface4, executor);
    }

    public static void method2914(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        SubInterface class282Sub443 = client.OPEN_INTERFACES.get(i_2);
        if (class282Sub443 != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method2916(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapElement class282Sub50Sub63 = WorldMapSub1.getStaticElement();
        if (class282Sub50Sub63 != null) {
            boolean bool4 = class282Sub50Sub63.method14778(i_2 >> 28 & 0x3, i_2 >> 14 & 0x3fff, i_2 & 0x3fff, CS2Executor.anIntArray5900);
            if (bool4) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900[1];
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900[2];
            } else {
                executor.intStack[++executor.intStackPtr - 1] = -1;
                executor.intStack[++executor.intStackPtr - 1] = -1;
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.intStack[++executor.intStackPtr - 1] = -1;
        }
    }

    public static void method13756(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapElement class282Sub50Sub63 = WorldMap.getMapDef(i_2 >> 14 & 0x3fff, i_2 & 0x3fff);
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9536;
        }
    }

    public static void method15403() {
        client.setLoading();
    }

    public static void method798(CS2Executor executor) {
        getBoundingBox(executor.currentGroundItem, executor);
    }

    public static void method7420(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.defaultToolkit.visible() ? 1 : 0;
    }

    public static void method7422(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.scrollX;
    }

    public static void method1799(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).questPointReward;
    }

    public static void pushString(CS2Executor executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.current.stringOpValues[executor.instrPtr];
    }

    public static void method1801(CS2Executor executor) {
        method11160(method8697(), executor);
    }
    
    public static MiniMenuEntry method8697() {
        return MiniMenu.aCacheableNodeSub79445;
    }

    public static void method260(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setOnMouseOver(icomponentdefinitions3, executor);
    }

    public static void method261(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentEntity.loadedInterfaceId;
    }

    public static void method11612(CS2Executor executor) {
        executor.intStackPtr -= 3;
        SoundEffectPlayer.playSoundJingle(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 2]);
    }

    public static void method11613(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (client.BASE_WINDOW_ID != -1) {
            if (i_2 == 0) {
                executor.intStack[++executor.intStackPtr - 1] = client.BASE_WINDOW_ID;
                return;
            }
            --i_2;
        }
        SubInterface class282Sub443;
        for (class282Sub443 = client.OPEN_INTERFACES.first(); i_2-- > 0; class282Sub443 = client.OPEN_INTERFACES.next()) {
        }
        executor.intStack[++executor.intStackPtr - 1] = class282Sub443.subInterfaceId;
    }

    public static void setGender(CS2Executor executor) {
        boolean bool2 = executor.intStack[--executor.intStackPtr] != 0;
        if (PlayerUpdate.MY_PLAYER.playerAppearance != null) {
            PlayerUpdate.MY_PLAYER.playerAppearance.changeGender(bool2);
        }
    }

    public static void method4311(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method7555(icomponentdefinitions3, interface4, executor);
    }

    public static void staffModLevel(CS2Executor executor) {
        if (client.PLAYER_RIGHTS >= 2) {
            executor.intStack[++executor.intStackPtr - 1] = client.PLAYER_RIGHTS;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method4314(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).hasPrerequisiteRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1]) ? 1 : 0;
        --executor.intStackPtr;
    }

    public static void method4315() {
        StaticElements.CS2_QUERY_RESULT_IDX = 0;
    }

    public static void method13359(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        boolean bool3 = ClientStartup.activeClient.method4658(new File(string_2));
        executor.intStack[++executor.intStackPtr - 1] = bool3 ? 1 : 0;
    }

    public static void method4973(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.aShort7394;
        executor.intStack[++executor.intStackPtr - 1] = client.aShort7324;
    }

    public static void method5454(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6689(icomponentdefinitions3, executor);
    }

    public static void method5456(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = TextUtils.atLtGtTags(string_2);
    }

    public static void hookMouseEnter(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method6289(icomponentdefinitions3, executor);
    }

    public static void method1492(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method774(icomponentdefinitions3, executor);
    }

    public static void method1967(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method14680(icomponentdefinitions3, executor);
    }

    public static void method1968(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.defaultToolkit.canModify(i_2);
    }

    public static void method5362(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.method1198(i_2, i_3, i_4);
    }

    public static void method8205(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.anInt7271 - client.anInt7270;
    }

    public static void method8206(CS2Executor executor) {
        executor.intStackPtr -= 12;
        MiniMenu.close();
        StaticMedia.resetChatMedia();
        TextureOpIrregularBricks.anInt9792 = executor.intStack[executor.intStackPtr];
        NativeSprite.anInt3556 = executor.intStack[executor.intStackPtr + 1];
        MiniMenu.anInt188 = executor.intStack[executor.intStackPtr + 2];
        StaticMethods.anInt2262 = executor.intStack[executor.intStackPtr + 3];
        StaticMethods.anInt5438 = executor.intStack[executor.intStackPtr + 4];
        NativeSprite.anInt4350 = executor.intStack[executor.intStackPtr + 5];
        StaticMethods.anInt3145 = executor.intStack[executor.intStackPtr + 6];
        StaticMethods.anInt5396 = executor.intStack[executor.intStackPtr + 7];
        StaticMethods.anInt148 = executor.intStack[executor.intStackPtr + 8];
        MiniMenu.anInt4722 = executor.intStack[executor.intStackPtr + 9];
        StaticMethods.anInt1005 = executor.intStack[executor.intStackPtr + 10];
        StaticMethods.anInt2982 = executor.intStack[executor.intStackPtr + 11];
        Resource.SPRITES.isFileCached(StaticMethods.anInt5438);
        Resource.SPRITES.isFileCached(NativeSprite.anInt4350);
        Resource.SPRITES.isFileCached(StaticMethods.anInt3145);
        Resource.SPRITES.isFileCached(StaticMethods.anInt5396);
        Resource.SPRITES.isFileCached(StaticMethods.anInt148);
        Resource.SPRITES.isFileCached(StaticMethods.anInt2982);
        Resource.FONT_METRICS.isFileCached(StaticMethods.anInt2982);
        StaticMethods.aNativeSprite4110 = null;
        StaticMethods.aNativeSprite9315 = null;
        StaticMethods.aNativeSprite1571 = null;
        StaticMethods.aNativeSprite612 = null;
        StaticMedia.aNativeSprite647 = null;
        StaticMedia.aNativeSprite5576 = null;
        StaticMedia.aNativeSprite3452 = null;
        StaticMedia.aNativeSprite5339 = null;
        FontMetrics.useBoldFont = true;
    }

    public static void method3250(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method501(icomponentdefinitions3, executor);
    }

    public static void method8865(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        QuickchatMessageType quickchatdefinitions3 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_2);
        if (quickchatdefinitions3.responses == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = quickchatdefinitions3.responses.length;
        }
    }

    public static void method8869(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt1526;
    }

    public static void method8870(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS[i_2].displayName;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void method12915(CS2Executor executor) {
        NpcEntity npc2 = (NpcEntity) executor.currentEntity;
        String string_3 = npc2.modifiedName;
        NPCType npcdefinitions4 = npc2.definitions;
        if (npcdefinitions4.alternativeIds != null) {
            npcdefinitions4 = npcdefinitions4.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
            if (npcdefinitions4 == null) {
                string_3 = "";
            } else {
                string_3 = npcdefinitions4.name;
            }
        }
        if (string_3 == null) {
            string_3 = "";
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_3;
    }

    public static void method7826(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY.list(i_2).messageHotkeys[i_3];
    }

    public static void method12352(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.aBool5556 ? 1 : 0;
    }

    public static void bitNot(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = ~i_2;
    }

    public static void method5000(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method5009(icomponentdefinitions3, executor);
    }

    public static void method5002(CS2Executor executor) {
        method1573(executor);
    }

    public static void method5003(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = i_2 & 0x3fff;
    }

    public static void method5004(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6690(icomponentdefinitions3, executor);
    }

    public static void method15075(CS2Executor executor) {
        PacketEncoder.method6811(executor.intStack[--executor.intStackPtr]);
    }

    public static void method16095(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method4772(icomponentdefinitions3, executor);
    }

    public static void method14434(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        PacketEncoder.method11404((String) executor.stringStack[executor.stringStackPtr], (String) executor.stringStack[executor.stringStackPtr + 1], "", executor.intStack[--executor.intStackPtr] == 1, false);
    }

    public static void setCompText(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setCompText(icomponentdefinitions3, interface4, executor);
    }

    public static void randSoundPitch(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (i_2 > 700 || i_3 > 700) {
            executor.intStack[++executor.intStackPtr - 1] = 256;
        }
        double d_4 = (Math.random() * (i_3 + i_2) - i_2 + 800.0D) / 100.0D;
        executor.intStack[++executor.intStackPtr - 1] = (int) (Math.pow(2.0D, d_4) + 0.5D);
    }

    public static void method7961(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method3366(icomponentdefinitions3, executor);
    }

    //Deprecated jsobject applet BS
    public static void method7962() {
    }

    public static void method6242(CS2Executor executor) {
        executor.intStackPtr -= 5;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        int i_5 = executor.intStack[executor.intStackPtr + 3];
        int i_6 = executor.intStack[executor.intStackPtr + 4];
        if (i_4 == -1) {
            throw new RuntimeException();
        } else {
            EnumType enumdefinitions7 = TypeLists.ENUM.list(i_4);
            if (i_3 != enumdefinitions7.keyType) {
                throw new RuntimeException();
            } else if (i_2 != enumdefinitions7.valueType) {
                throw new RuntimeException();
            } else {
                int[] ints_8 = enumdefinitions7.getValueReferences(Integer.valueOf(i_5));
                if (i_6 >= 0 && ints_8 != null && ints_8.length > i_6) {
                    executor.intStack[++executor.intStackPtr - 1] = ints_8[i_6];
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }

    public static void textGender(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        if (PlayerUpdate.MY_PLAYER.playerAppearance != null && PlayerUpdate.MY_PLAYER.playerAppearance.female) {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2;
        }
    }

    public static void clearBit(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 & -1 - (1 << i_3);
    }

    public static void method1462(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        IgnoreList.add(string_2, true);
    }

    public static void method1463(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.maxScreenSize.getValue();
    }

    public static void setAntiAliasingPreview(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.antiAliasingPreview, executor.intStack[--executor.intStackPtr]);
        client.loadAssets(PreferenceManager.preferences.activeToolkit.getValue(), false);
        PreferenceManager.savePreferences();
    }

    public static void method14649(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.mapSize.canModify(i_2);
    }

    public static void method5082(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        CS2Interface underlaydefinition4 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        InterfaceRenderer.method12221(underlaydefinition4.comp, i_2, i_3);
    }

    public static void method7186(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.myPlayerIndex;
    }

    public static void method7187() {
    }

    public static void method3739(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2].referrer ? 1 : 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method3740(CS2Executor executor) {
        NpcEntity npc2 = (NpcEntity) executor.currentEntity;
        NPCType npcdefinitions3 = npc2.definitions;
        if (npcdefinitions3.alternativeIds != null) {
            npcdefinitions3 = npcdefinitions3.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
        }
        executor.intStack[++executor.intStackPtr - 1] = npcdefinitions3 != null ? 1 : 0;
    }

    public static void getWorldQuickchatOnly(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.IS_QUICKCHAT_ONLY ? 1 : 0;
    }

    public static void intGreaterThan(CS2Executor executor) {
        executor.intStackPtr -= 2;
        if (executor.intStack[executor.intStackPtr] > executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method16089(CS2Executor executor) {
        executor.intStackPtr -= 2;
        ItemContainerType inventorydef2 = TypeLists.CONTAINER.list(executor.intStack[executor.intStackPtr]);
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = -1;
        for (int i_5 = 0; i_5 < inventorydef2.defaultItemSize; i_5++) {
            if (i_3 == inventorydef2.itemIds[i_5]) {
                i_4 = inventorydef2.amounts[i_5];
                break;
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
    }

    public static void method6040(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method12630(icomponentdefinitions3);
    }

    public static void method6041(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = client.getComponentSettings(icomponentdefinitions3).getUseOptionFlags();
    }

    public static void method6042(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ((SceneGraphEntity) executor.currentLocation).getIconHeight();
    }

    public static void method6043(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setOnMouseLeave(icomponentdefinitions3, executor);
    }

    public static void method6044(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.isApplet ? 1 : 0;
    }

    public static void method6046(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.LOYALTY_BALANCE;
    }

    public static void method12718(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.ambientSoundVolume, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void ccSetItemNoNum(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        setItemIFComp(icomponentdefinitions4, interface5, false, Component.StackMode.NONUM, executor);
    }

    public static void method8285(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6289(icomponentdefinitions3, executor);
    }

    public static void ifSetItemWearColNoNum(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setItemIFComp(icomponentdefinitions3, interface4, true, Component.StackMode.NONUM, executor);
    }

    public static void method8296(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        BufferedConnectionContext class1843 = client.getConnectionContext();
        TcpPacket tcpmessage4 = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PRIVATE, class1843.isaac);
        tcpmessage4.buffer.writeByte(0);
        int i_5 = tcpmessage4.buffer.currentPosition;
        tcpmessage4.buffer.writeString(string_2);
        tcpmessage4.buffer.writeShort(executor.currentQuickChatMessage.id);
        executor.currentQuickChatMessage.message.method14896(tcpmessage4.buffer, executor.currentQuickChatMessage.anIntArray4046);
        tcpmessage4.buffer.writeIndex(tcpmessage4.buffer.currentPosition - i_5);
        class1843.queuePacket(tcpmessage4);
    }

    public static void method8297(CS2Executor executor) {
        if (KeyRecorder.keyRecorder.held(81)) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method8298(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method15474(icomponentdefinitions4);
    }

    public static void method868(CS2Executor executor) {
        QuestType questdefinitions2 = TypeLists.QUEST.list(executor.intStack[--executor.intStackPtr]);
        executor.intStack[++executor.intStackPtr - 1] = questdefinitions2.varpRequirements == null ? 0 : questdefinitions2.varpRequirements.length;
    }

    public static void method870(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Component icomponentdefinitions5 = Component.fromHash(i_4);
        InterfaceRenderer.method12221(icomponentdefinitions5, i_2, i_3);
    }

    public static void add(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = i_2 + i_3;
    }

    public static void method15417(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.method304();
    }

    public static void method4381(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method12401(icomponentdefinitions4, interface5);
    }

    public static void method14818(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = Resource.NATIVE_LIBRARY_LOADER.isJagtheoraLoaded() ? 1 : 0;
    }

    public static void method14833(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.COUNTRY;
    }

    public static void method14835(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ChatLine chatline3 = ChatBox.getLine(i_2);
        String str4 = "";
        if (chatline3 != null && chatline3.name != null) {
            str4 = chatline3.name;
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4;
    }

    public static void method14836(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        boolean bool3 = executor.intStack[executor.intStackPtr + 1] == 1;
        Node node4 = WorldMapSub1.aClass4658025.get(i_2);
        if (node4 != null && !bool3) {
            node4.unlink();
        } else if (node4 == null && bool3) {
            node4 = new Node();
            WorldMapSub1.aClass4658025.put(node4, i_2);
        }
    }

    //Deprecated jsobject applet BS
    public static void method14839() {
    }

    public static void method14841(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).hasLevelRequirement(client.SKILL_LEVEL_ACTUAL, executor.intStack[executor.intStackPtr - 1]) ? 1 : 0;
        --executor.intStackPtr;
    }

    public static void method6677(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel.players[i_2].rank;
    }

    public static void method6678(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 < 0 || i_2 > 5) {
            i_2 = 2;
        }
        client.loadAssets(i_2, false);
    }

    public static void method6680(CS2Executor executor) {
        if (FriendsChat.FC_NAME != null) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYER_COUNT;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method6685(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method502(icomponentdefinitions3, executor);
    }

    public static void method12839(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method505(icomponentdefinitions3, executor);
    }

    public static void method12841(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.GRAND_EXCHANGE_ITEM_SELECT, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage3.buffer.writeShort(i_2);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
    }

    public static void method456(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.cycles;
    }

    public static void method457(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.groundDecoration, executor.intStack[--executor.intStackPtr] == 1 ? 1 : 0);
        Resource.mapRegion.reload();
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void ccSetScrollPos(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method1852(icomponentdefinitions3, interface4, executor);
    }

    public static void method460(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method8310(icomponentdefinitions3, executor);
    }

    public static void method5774(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method501(icomponentdefinitions3, executor);
    }

    public static void method5775(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method6288(icomponentdefinitions3, executor);
    }

    public static void method5776(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        String string_3 = (String) executor.stringStack[--executor.stringStackPtr];
        if (i_2 == -1) {
            throw new RuntimeException();
        } else {
            EnumType enumdefinitions4 = TypeLists.ENUM.list(i_2);
            if (enumdefinitions4.valueType != 's') {
                throw new RuntimeException();
            } else {
                executor.intStack[++executor.intStackPtr - 1] = enumdefinitions4.containsKey(string_3) ? 1 : 0;
            }
        }
    }

    public static void method14669(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.method3360() ? 1 : 0;
    }

    public static void method2584(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        WorldMapSub1.reset(i_2, i_3 >> 14 & 0x3fff, i_3 & 0x3fff, false);
    }

    public static void method6913(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        String string_2 = (String) executor.stringStack[executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[executor.stringStackPtr + 1];
        if (client.PLAYER_RIGHTS != 0 || (!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
            BufferedConnectionContext class1844 = client.getConnectionContext();
            TcpPacket tcpmessage5 = PacketEncoder.createPacket(ClientProt.SEND_PRIVATE_MESSAGE, class1844.isaac);
            tcpmessage5.buffer.writeShort(0);
            int i_6 = tcpmessage5.buffer.currentPosition;
            tcpmessage5.buffer.writeString(string_2);
            PacketEncoder.chatMessageToBuffer(tcpmessage5.buffer, string_3);
            tcpmessage5.buffer.writeShortLength(tcpmessage5.buffer.currentPosition - i_6);
            class1844.queuePacket(tcpmessage5);
        }
    }

    public static void getPlayerCombatLevel(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.MY_PLAYER.combatLevel;
    }

    public static void loadClanVarLong(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        Long long_3 = (Long) ClanVarTypeList.CLAN_VARS[i_2];
        if (long_3 == null) {
            executor.longStack[++executor.longStackPtr - 1] = -1L;
        } else {
            executor.longStack[++executor.longStackPtr - 1] = long_3.longValue();
        }
    }

    public static void method3748(CS2Executor executor) {
        if (client.aString7165 != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = client.aString7165;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void popInt(CS2Executor executor) {
        --executor.intStackPtr;
    }

    public static void method3750(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.idleAnimations, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
    }

    public static void getSystemUpdateTimer(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.REBOOT_TIMER;
    }

    public static void method911(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        ItemType itemdefinitions3 = TypeLists.OBJ.list(i_2);
        int i_4;
        if (itemdefinitions3.hasTooltipColor) {
            i_4 = itemdefinitions3.tooltipColor;
        } else if (itemdefinitions3.membersOnly) {
            i_4 = TypeLists.INTERACTION_DEFAULTS.tooltipColorP2p;
        } else {
            i_4 = TypeLists.INTERACTION_DEFAULTS.tooltipColorF2p;
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
    }

    public static void method536(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        String string_5 = (String) executor.stringStack[--executor.stringStackPtr];
        if (i_3 == -1) {
            throw new RuntimeException();
        } else {
            EnumType enumdefinitions6 = TypeLists.ENUM.list(i_3);
            if (i_2 != enumdefinitions6.keyType) {
                throw new RuntimeException();
            } else if (enumdefinitions6.valueType != 115) {
                throw new RuntimeException();
            } else {
                int[] ints_7 = enumdefinitions6.getValueReferences(string_5);
                if (i_4 >= 0 && ints_7 != null && ints_7.length > i_4) {
                    executor.intStack[++executor.intStackPtr - 1] = ints_7[i_4];
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }

    public static void method7427(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = i_2 >> 28;
    }

    public static void method1167(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setNoClickThrough(icomponentdefinitions3, executor);
    }

    public static void method1171(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.banCount;
    }

    public static void setTradeFilter(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.TRADE_FILTER;
    }

    public static void method401(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method3251(icomponentdefinitions3, executor);
    }

    public static void method12210(CS2Executor executor) {
        executor.stringStackPtr -= 3;
        PacketEncoder.method11404((String) executor.stringStack[executor.stringStackPtr], (String) executor.stringStack[executor.stringStackPtr + 1], (String) executor.stringStack[executor.stringStackPtr + 2], executor.intStack[--executor.intStackPtr] == 1, true);
    }

    public static void method7500(CS2Executor executor) {
        PreferenceManager.preferences.setValue(PreferenceManager.preferences.idleAnimations, executor.intStack[--executor.intStackPtr]);
        PreferenceManager.savePreferences();
        client.preferencesSavedOnServer = false;
    }

    public static void method7501(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        String string_3 = (String) executor.stringStack[--executor.stringStackPtr];
        TcpPacket tcpmessage4 = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_CHANGE_ADDRESS, client.LOBBY_CONNECTION_CONTEXT.isaac);
        tcpmessage4.buffer.writeShort(TextUtils.getLength(string_2) + TextUtils.getLength(string_3));
        tcpmessage4.buffer.writeString(string_2);
        tcpmessage4.buffer.writeString(string_3);
        client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage4);
    }

    public static void method7502(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions4.text;
    }

    public static void method3949(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method6697(icomponentdefinitions3, interface4, executor);
    }

    public static void method3950() {
        StaticElements.CS2_QUERY_RESULT_IDX = 0;
    }

    public static void method3951(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = Character.toLowerCase((char) i_2);
    }

    public static void method5511(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method2874(icomponentdefinitions3, executor);
    }

    public static void method5512(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        method14605(icomponentdefinitions3, executor);
    }

    public static void method4960(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method2915(icomponentdefinitions3, executor);
    }

    public static void ifSetItemWearCol(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setItemIFComp(icomponentdefinitions3, interface4, true, Component.StackMode.WHEN_MULTIPLE, executor);
    }

    public static void ifSetItemAlwaysNum(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        setItemIFComp(icomponentdefinitions3, interface4, false, Component.StackMode.ALWAYS_NUM, executor);
    }

    public static void method1569(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method4280(icomponentdefinitions3, executor);
    }

    public static void ccSetItemWearColAlwaysNum(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setItemIFComp(icomponentdefinitions3, interface4, true, Component.StackMode.ALWAYS_NUM, executor);
    }

    public static void method1571(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spriteRoll;
    }

    public static void method1572(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.textures.canModify(i_2);
    }

    public static void method1573(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        SubInterface class282Sub444 = client.OPEN_INTERFACES.get(i_2);
        if (class282Sub444 != null && i_3 == class282Sub444.subInterfaceId) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method1574(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.skyboxes.display() ? 1 : 0;
    }

    public static void method12723(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.angle2d;
    }

    public static void method1162(CS2Executor executor) {
        executor.intStackPtr -= 4;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        int i_5 = executor.intStack[executor.intStackPtr + 3];
        CoordGrid coordgrid6 = Resource.mapRegion.getBaseCoordinates();
        Camera.lookat((i_2 >> 14 & 0x3fff) - coordgrid6.x, (i_2 & 0x3fff) - coordgrid6.y, i_3 << 2, i_4, i_5);
    }

    public static void method11154(CS2Executor executor) {
        executor.currentQuickChatMessage = new QuickChatMessage();
        executor.currentQuickChatMessage.id = executor.intStack[--executor.intStackPtr];
        executor.currentQuickChatMessage.message = TypeLists.QUICK_CHAT_MESSAGE.listMessage(executor.currentQuickChatMessage.id);
        executor.currentQuickChatMessage.anIntArray4046 = new int[executor.currentQuickChatMessage.message.method14916()];
    }

    public static void method1979(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = executor.clanChannel.players[i_2].name;
    }

    public static void method7744(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt94;
    }

    public static void method14244(CS2Executor executor) {
        int i_2 = WindowMode.fullScreenWidth;
        int i_3 = WindowMode.fullScreenHeight;
        int i_4 = -1;
        if (WindowMode.fullScreenSupported) {
            DisplayModeDetails[] arr_5 = WindowMode.getActiveDisplayModeDetails();
            for (int i_6 = 0; i_6 < arr_5.length; i_6++) {
                DisplayModeDetails class4677 = arr_5[i_6];
                if (i_2 == class4677.width && i_3 == class4677.height) {
                    i_4 = i_6;
                    break;
                }
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4;
    }

    public static void method11380(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method7667(icomponentdefinitions3, interface4, executor);
    }

    public static void method4825(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getItemIdAtSlot(i_2, i_3, true);
    }

    public static void method4826(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt112;
    }

    public static void method4829(CS2Executor executor) {
        if (FriendList.LOADED == 0) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = IgnoreList.IGNORE_COUNT;
        }
    }

    public static void method4830(CS2Executor executor) {
        Component icomponentdefinitions2 = executor.currentEntity.inter.getComponent(executor.intStack[--executor.intStackPtr]);
        icomponentdefinitions2.slotChildren = null;
        icomponentdefinitions2.itemSlots = null;
        Component.redrawComponent(icomponentdefinitions2);
    }

    public static void method7461(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 1]).members ? 1 : 0;
    }

    public static void method7462(CS2Executor executor) {
        String string_2 = null;
        if (PacketDecoder.HOSTNAME_IDENTIFIER != null) {
            string_2 = PacketDecoder.HOSTNAME_IDENTIFIER.getHostName();
        }
        if (string_2 == null) {
            string_2 = "";
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2;
    }

    public static void interpolate(CS2Executor executor) {
        executor.intStackPtr -= 5;
        int a = executor.intStack[executor.intStackPtr];
        int b = executor.intStack[executor.intStackPtr + 1];
        int c = executor.intStack[executor.intStackPtr + 2];
        int d = executor.intStack[executor.intStackPtr + 3];
        int e = executor.intStack[executor.intStackPtr + 4];
        executor.intStack[++executor.intStackPtr - 1] = a + (e - c) * (b - a) / (d - c);
    }

    public static void method1860(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        if (client.PLAYER_RIGHTS != 0 || (!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
            String string_3 = string_2.toLowerCase();
            byte b_4 = 0;
            if (string_3.startsWith(LocalizedText.YELLOW.translate(Language.ENGLISH))) {
                string_2 = string_2.substring(LocalizedText.YELLOW.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.RED.translate(Language.ENGLISH))) {
                b_4 = 1;
                string_2 = string_2.substring(LocalizedText.RED.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.GREEN.translate(Language.ENGLISH))) {
                b_4 = 2;
                string_2 = string_2.substring(LocalizedText.GREEN.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.CYAN.translate(Language.ENGLISH))) {
                b_4 = 3;
                string_2 = string_2.substring(LocalizedText.CYAN.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.PURPLE.translate(Language.ENGLISH))) {
                b_4 = 4;
                string_2 = string_2.substring(LocalizedText.PURPLE.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.WHITE.translate(Language.ENGLISH))) {
                b_4 = 5;
                string_2 = string_2.substring(LocalizedText.WHITE.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.FLASH1.translate(Language.ENGLISH))) {
                b_4 = 6;
                string_2 = string_2.substring(LocalizedText.FLASH1.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.FLASH2.translate(Language.ENGLISH))) {
                b_4 = 7;
                string_2 = string_2.substring(LocalizedText.FLASH2.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.FLASH3.translate(Language.ENGLISH))) {
                b_4 = 8;
                string_2 = string_2.substring(LocalizedText.FLASH3.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.GLOW1.translate(Language.ENGLISH))) {
                b_4 = 9;
                string_2 = string_2.substring(LocalizedText.GLOW1.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.GLOW2.translate(Language.ENGLISH))) {
                b_4 = 10;
                string_2 = string_2.substring(LocalizedText.GLOW2.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.GLOW3.translate(Language.ENGLISH))) {
                b_4 = 11;
                string_2 = string_2.substring(LocalizedText.GLOW3.translate(Language.ENGLISH).length());
            } else if (client.CURRENT_LANGUAGE != Language.ENGLISH) {
                if (string_3.startsWith(LocalizedText.YELLOW.translate(client.CURRENT_LANGUAGE))) {
                    string_2 = string_2.substring(LocalizedText.YELLOW.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.RED.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 1;
                    string_2 = string_2.substring(LocalizedText.RED.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.GREEN.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 2;
                    string_2 = string_2.substring(LocalizedText.GREEN.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.CYAN.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 3;
                    string_2 = string_2.substring(LocalizedText.CYAN.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.PURPLE.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 4;
                    string_2 = string_2.substring(LocalizedText.PURPLE.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.WHITE.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 5;
                    string_2 = string_2.substring(LocalizedText.WHITE.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.FLASH1.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 6;
                    string_2 = string_2.substring(LocalizedText.FLASH1.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.FLASH2.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 7;
                    string_2 = string_2.substring(LocalizedText.FLASH2.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.FLASH3.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 8;
                    string_2 = string_2.substring(LocalizedText.FLASH3.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.GLOW1.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 9;
                    string_2 = string_2.substring(LocalizedText.GLOW1.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.GLOW2.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 10;
                    string_2 = string_2.substring(LocalizedText.GLOW2.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.GLOW3.translate(client.CURRENT_LANGUAGE))) {
                    b_4 = 11;
                    string_2 = string_2.substring(LocalizedText.GLOW3.translate(client.CURRENT_LANGUAGE).length());
                }
            }
            string_3 = string_2.toLowerCase();
            byte b_5 = 0;
            if (string_3.startsWith(LocalizedText.WAVE.translate(Language.ENGLISH))) {
                b_5 = 1;
                string_2 = string_2.substring(LocalizedText.WAVE.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.WAVE2.translate(Language.ENGLISH))) {
                b_5 = 2;
                string_2 = string_2.substring(LocalizedText.WAVE2.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.SHAKE.translate(Language.ENGLISH))) {
                b_5 = 3;
                string_2 = string_2.substring(LocalizedText.SHAKE.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.SCROLL.translate(Language.ENGLISH))) {
                b_5 = 4;
                string_2 = string_2.substring(LocalizedText.SCROLL.translate(Language.ENGLISH).length());
            } else if (string_3.startsWith(LocalizedText.SLIDE.translate(Language.ENGLISH))) {
                b_5 = 5;
                string_2 = string_2.substring(LocalizedText.SLIDE.translate(Language.ENGLISH).length());
            } else if (client.CURRENT_LANGUAGE != Language.ENGLISH) {
                if (string_3.startsWith(LocalizedText.WAVE.translate(client.CURRENT_LANGUAGE))) {
                    b_5 = 1;
                    string_2 = string_2.substring(LocalizedText.WAVE.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.WAVE2.translate(client.CURRENT_LANGUAGE))) {
                    b_5 = 2;
                    string_2 = string_2.substring(LocalizedText.WAVE2.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.SHAKE.translate(client.CURRENT_LANGUAGE))) {
                    b_5 = 3;
                    string_2 = string_2.substring(LocalizedText.SHAKE.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.SCROLL.translate(client.CURRENT_LANGUAGE))) {
                    b_5 = 4;
                    string_2 = string_2.substring(LocalizedText.SCROLL.translate(client.CURRENT_LANGUAGE).length());
                } else if (string_3.startsWith(LocalizedText.SLIDE.translate(client.CURRENT_LANGUAGE))) {
                    b_5 = 5;
                    string_2 = string_2.substring(LocalizedText.SLIDE.translate(client.CURRENT_LANGUAGE).length());
                }
            }
            BufferedConnectionContext class1846 = client.getConnectionContext();
            TcpPacket tcpmessage7 = PacketEncoder.createPacket(ClientProt.CHAT, class1846.isaac);
            tcpmessage7.buffer.writeByte(0);
            int i_8 = tcpmessage7.buffer.currentPosition;
            tcpmessage7.buffer.writeByte(b_4);
            tcpmessage7.buffer.writeByte(b_5);
            PacketEncoder.chatMessageToBuffer(tcpmessage7.buffer, string_2);
            tcpmessage7.buffer.writeIndex(tcpmessage7.buffer.currentPosition - i_8);
            class1846.queuePacket(tcpmessage7);
        }
    }

    public static void method1861(CS2Executor executor) {
        executor.intStackPtr -= 2;
        client.recolorPalette[executor.intStack[executor.intStackPtr]] = (short) ColorUtil.rgbToHsl24(executor.intStack[executor.intStackPtr + 1]);
        TypeLists.OBJ.resetModelCache();
        TypeLists.OBJ.resetRecolorCache();
        TypeLists.NPC.resetBodyCaches();
        GameShell.setComponentsOnTop();
    }

    public static void method1862(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.groundDecoration.playingRunescape() ? 1 : 0;
    }

    public static void setBaseColor(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (PlayerUpdate.MY_PLAYER.playerAppearance != null) {
            PlayerUpdate.MY_PLAYER.playerAppearance.recolor(i_2, i_3);
        }
    }

    public static void method1836(CS2Executor executor) {
        executor.intStackPtr -= 2;
        PlayerUpdate.animateZero(PlayerUpdate.MY_PLAYER, executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1]);
    }

    public static void method1837() {
        Camera.smoothReset();
    }

    public static void method7551(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = (int) (Utils.time() / 60000L);
    }

    public static void method7552(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.maxScreenSize.visible() ? 1 : 0;
    }

    public static void method16124(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        if (string_2.startsWith(TextUtils.getCrown(0)) || string_2.startsWith(TextUtils.getCrown(1))) {
            string_2 = string_2.substring(7);
        }
        executor.intStack[++executor.intStackPtr - 1] = IgnoreList.contains(string_2) ? 1 : 0;
    }

    public static void method3561(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method2170(icomponentdefinitions3, executor);
    }

    public static void method3562(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(3, i_2 << 16 | i_3, i_4, "");
    }

    //Deprecated jsobject applet BS
    public static void method15406(CS2Executor executor) {
        boolean bool2 = true;
        executor.intStack[++executor.intStackPtr - 1] = bool2 ? 1 : 0;
    }

    public static void method15407(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.fog.canModify(i_2);
    }

    public static void method15408(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        BasType bas = TypeLists.BAS.list(i_2);
        if (bas.randomStandSequences != null && bas.randomStandSequences.length > 0) {
            int i_4 = 0;
            int i_5 = bas.randomStandSequenceChances[0];
            for (int i_6 = 1; i_6 < bas.randomStandSequences.length; i_6++) {
                if (bas.randomStandSequenceChances[i_6] > i_5) {
                    i_4 = i_6;
                    i_5 = bas.randomStandSequenceChances[i_6];
                }
            }
            executor.intStack[++executor.intStackPtr - 1] = bas.randomStandSequences[i_4];
        } else {
            executor.intStack[++executor.intStackPtr - 1] = bas.standSequenceId;
        }
    }

    public static void method12115(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = Camera.cameraCalculationType == 2 ? 1 : 0;
    }

    public static void method12116(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method15511(icomponentdefinitions3, interface4, executor);
    }

    public static void ccSetTargetOpCursor(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setTargetOpCursor(icomponentdefinitions3, executor);
    }

    public static void method296(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (i_2 >= 0 && i_2 < 2) {
            client.anIntArrayArrayArray7302[i_2] = new int[i_3 << 1][4];
        }
    }

    public static void popLong(CS2Executor executor) {
        --executor.longStackPtr;
    }

    public static void method6150(CS2Executor executor) {
        CS2Interface underlaydefinition2;
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2;
        } else {
            underlaydefinition2 = executor.hookedInterface1;
        }
        Component icomponentdefinitions4 = underlaydefinition2.comp;
        Interface interface5 = underlaydefinition2.inter;
        method6045(icomponentdefinitions4, executor);
    }

    public static void method6151(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.y;
    }

    public static void method6152(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        FriendsChat.kickPlayer(string_2);
    }

    public static void method6153(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.defaultToolkit.getValue();
    }

    public static void method13467(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.anInt3644;
    }

    public static void getVarpbitOld(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        executor.intStack[++executor.intStackPtr - 1] = ((PlayerEntity) executor.currentEntity).playerConfigs.getVarbit(i_2);
    }

    public static void strAppendNum(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        int i_3 = executor.intStack[--executor.intStackPtr];
        executor.stringStack[++executor.stringStackPtr - 1] = string_2 + i_3;
    }

    public static void method4988(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        WorldMapElement class282Sub50Sub63 = WorldMap.poll(i_2);
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0;
            executor.intStack[++executor.intStackPtr - 1] = 0;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9540 - class282Sub50Sub63.anInt9539;
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9535 - class282Sub50Sub63.anInt9541;
        }
    }

    public static void method4989(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.sceneryShadows.enabled() ? 1 : 0;
    }

    public static void method7043(CS2Executor executor) {
        WorldMapElement class282Sub50Sub62 = WorldMapSub1.getStaticElement();
        if (class282Sub50Sub62 != null) {
            boolean bool3 = class282Sub50Sub62.method14775(WorldMap.mapSquareAbsX + WorldMapSub1.anInt5123, WorldMap.mapSquareAbsY + WorldMapSub1.anInt5624, CS2Executor.anIntArray5900);
            if (bool3) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900[1];
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900[2];
            } else {
                executor.intStack[++executor.intStackPtr - 1] = -1;
                executor.intStack[++executor.intStackPtr - 1] = -1;
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1;
            executor.intStack[++executor.intStackPtr - 1] = -1;
        }
    }

    public static void method4797(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method2874(icomponentdefinitions3, executor);
    }

    public static void zoomWorldmap(CS2Executor executor) {
        WorldMapInteraction.requestZoomChange(executor.intStack[--executor.intStackPtr]);
    }

    public static void method4801(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method15256(icomponentdefinitions3, executor);
    }

    public static void method4802(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.slotId;
    }

    public static void method11338(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method6158(icomponentdefinitions3, executor);
    }

    public static void method11339(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = WorldMapSub1.aBool8027 ? 1 : 0;
    }

    public static void method11340(CS2Executor executor) {
        executor.intStackPtr -= 3;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        Interface.ifOptionInteract(7, i_2 << 16 | i_3, i_4, "");
    }

    public static void method11341(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.groundDecoration.canModify(i_2);
    }

    public static void method11370(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2].worldFlags;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method11372(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method7889(icomponentdefinitions3, executor);
    }

    public static void method11373(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method6045(icomponentdefinitions3, executor);
    }

    public static void method4565(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        if (icomponentdefinitions3.containerItemId != -1) {
            executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.itemStackSize;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method4566(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS[i_2].username;
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = "";
        }
    }

    public static void max(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        executor.intStack[++executor.intStackPtr - 1] = Math.max(i_2, i_3);
    }

    public static void sendSignupFormPacket(CS2Executor executor) {
        executor.stringStackPtr -= 2;
        executor.intStackPtr -= 2;
        PacketEncoder.method2074((String) executor.stringStack[executor.stringStackPtr], (String) executor.stringStack[executor.stringStackPtr + 1], executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1] == 1);
    }

    public static void method2071(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method1083(icomponentdefinitions3, executor);
    }

    public static void resumeHSLDialog(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        TcpPacket tcpmessage3 = PacketEncoder.createPacket(ClientProt.RESUME_HSLDIALOG, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage3.buffer.writeShort(i_2);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
    }

    public static void storeVarpbit(CS2Executor executor) {
        int i_2 = executor.intOpValues[executor.instrPtr];
        VarProviders.PLAYER_VAR_PROVIDER.setVarBitValue(i_2, executor.intStack[--executor.intStackPtr]);
    }

    public static void method2945(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.spritePitch;
    }

    public static void method3913(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        executor.intStack[++executor.intStackPtr - 1] = client.SKILL_LEVEL_VISIBLE[i_2];
    }

    public static void method1641(CS2Executor executor) {
        String string_2 = (String) executor.stringStack[--executor.stringStackPtr];
        int i_3 = executor.intStack[--executor.intStackPtr];
        FriendsChat.setRank(string_2, i_3);
    }

    public static void method1643(CS2Executor executor) {
        executor.intStackPtr -= 2;
        client.aShort7436 = (short) executor.intStack[executor.intStackPtr];
        if (client.aShort7436 <= 0) {
            client.aShort7436 = 256;
        }
        client.aShort7437 = (short) executor.intStack[executor.intStackPtr + 1];
        if (client.aShort7437 <= 0) {
            client.aShort7437 = 205;
        }
    }

    public static void getRunWeight(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = client.RUN_WEIGHT;
    }

    public static void method13404(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.ambientSoundVolume.getValue();
    }

    public static void method8152(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method14482(icomponentdefinitions3, executor);
    }

    public static void method8153(CS2Executor executor) {
        executor.intStackPtr -= 4;
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], true, 256);
    }

    public static void method8154(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.fontId;
    }

    public static void method8156(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences.brightness.getValue();
    }

    public static void method8694(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method6690(icomponentdefinitions3, executor);
    }

    public static void method8698(CS2Executor executor) {
        if (client.GUEST_CLAN_SETTINGS != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1;
            executor.currentClanSettings = client.GUEST_CLAN_SETTINGS;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0;
        }
    }

    public static void method8700(CS2Executor executor) {
        int i_2 = executor.intStack[executor.intStackPtr - 3];
        int i_3 = executor.intStack[executor.intStackPtr - 2];
        int i_4 = executor.intStack[executor.intStackPtr - 1];
        executor.intStackPtr -= 3;
        if (i_2 > executor.globalArrayLengths[i_3]) {
            throw new RuntimeException();
        } else if (i_2 > executor.globalArrayLengths[i_4]) {
            throw new RuntimeException();
        } else if (i_3 == i_4) {
            throw new RuntimeException();
        } else {
            ArrayUtils.quicksort(executor.globalArrays[i_3], executor.globalArrays[i_4], 0, i_2 - 1);
        }
    }

    public static void method8701(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.allowGuests ? 1 : 0;
    }

    public static void method8738(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        if (i_2 >= 1 && i_2 <= 2) {
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.fixedScreen, i_2);
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.fullScreen, i_2);
            PreferenceManager.savePreferences();
        }
    }

    public static void method14520(CS2Executor executor) {
        executor.intStackPtr -= 4;
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], 256);
    }

    public static void method14521(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        method4136(icomponentdefinitions3, executor);
    }

    public static void method4146(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.x;
    }

    public static void getCompHeight(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.height;
    }

    public static void method4148(CS2Executor executor) {
        executor.intStackPtr -= 2;
        int i_2 = executor.intStack[executor.intStackPtr];
        int i_3 = executor.intStack[executor.intStackPtr + 1];
        if (i_3 == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1;
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY.list(i_2).subCategoryForHotkey((char) i_3);
        }
    }

    public static void method5595(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method1845(icomponentdefinitions3, executor);
    }

    public static void intNotEqual(CS2Executor executor) {
        executor.intStackPtr -= 2;
        if (executor.intStack[executor.intStackPtr] != executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues[executor.instrPtr];
        }
    }

    public static void method5598(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.height;
    }

    public static void method2841(CS2Executor executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST.list(executor.intStack[executor.intStackPtr - 2]).levelRequirements[executor.intStack[executor.intStackPtr - 1]][1];
        --executor.intStackPtr;
    }

    public static void clanVarsEnabled(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = ClanVarTypeList.CLAN_VARS != null ? 1 : 0;
    }

    public static void method14640(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        method3672(icomponentdefinitions3, executor);
    }

    public static void ccSetItemAlwaysNum(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        Interface interface4 = underlaydefinition2.inter;
        setItemIFComp(icomponentdefinitions3, interface4, false, Component.StackMode.ALWAYS_NUM, executor);
    }

    public static void method15555(CS2Executor executor) {
        int[] ints_2 = executor.intStack;
        int i_3 = ++executor.intStackPtr - 1;
        byte b_4;
        if (PreferenceManager.preferences.groundDecoration.getValue() == 1) {
            b_4 = 1;
        } else {
            b_4 = 0;
        }
        ints_2[i_3] = b_4;
    }

    public static void method8208(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        Interface interface4 = Interface.INTERFACES[i_2 >> 16];
        assert icomponentdefinitions3 != null;
        method8387(icomponentdefinitions3, executor);
    }

    public static void method7668(CS2Executor executor) {
        WorldMapSub1.reset(executor.intStack[--executor.intStackPtr], -1, -1, false);
    }

    public static void method6696(CS2Executor executor) {
        int i_2 = executor.intStack[--executor.intStackPtr];
        Component icomponentdefinitions3 = Component.fromHash(i_2);
        assert icomponentdefinitions3 != null;
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.offsetY;
    }

    public static void method2620(CS2Executor executor) {
        int ifComp = executor.intStack[--executor.intStackPtr];
        CS2Interface underlaydefinition3;
        if (executor.aBool7022) {
            underlaydefinition3 = executor.hookedInterface2;
        } else {
            underlaydefinition3 = executor.hookedInterface1;
        }
        executor.intStack[++executor.intStackPtr - 1] = underlaydefinition3.setChild(ifComp, -1) ? 1 : 0;
    }

    public static void method1134(CS2Executor executor) {
        CS2Interface underlaydefinition2 = executor.aBool7022 ? executor.hookedInterface2 : executor.hookedInterface1;
        Component icomponentdefinitions3 = underlaydefinition2.comp;
        int i_4 = executor.intStack[--executor.intStackPtr];
        ParamType attributedefault5 = TypeLists.PARAMS.list(i_4);
        if (attributedefault5.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3.method1998(i_4, attributedefault5.typeName);
        } else {
            executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3.method1997(i_4, attributedefault5.defaultInt);
        }
    }

    public static void method1135(CS2Executor executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings.coinShare;
    }

    /**
     * START MULTI PARAMETER UTILITY METHODS
     */

    public static void method15511(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.spriteScale = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method14680(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        int i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        int i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1;
        if (icomponentdefinitions0.mediaType != MediaType.NPC_MODEL) {
            throw new RuntimeException("");
        } else {
            NPCType npcdefinitions5 = TypeLists.NPC.list(icomponentdefinitions0.primaryMediaId);
            if (icomponentdefinitions0.npcCustomisation == null) {
                icomponentdefinitions0.npcCustomisation = new NPCTypeCustomisation(npcdefinitions5, true);
            }
            icomponentdefinitions0.npcCustomisation.uid = CS2Executor.method1480();
            if (i_4 >= 0 && i_4 < npcdefinitions5.modelIds.length) {
                icomponentdefinitions0.npcCustomisation.modelIds[i_4] = i_3;
                Component.redrawComponent(icomponentdefinitions0);
            } else {
                throw new RuntimeException("" + i_4);
            }
        }
    }

    public static void getBoundingBox(SceneGraphEntity transform, CS2Executor cs2executor) {
        boolean bool3 = false;
        int minX = 0;
        int minY = 0;
        int maxX = 0;
        int maxY = 0;
        if (transform.boundingBoxPolygons != null) {
            for (int index = 0; index < transform.boundingBoxPolygons.length; index++) {
                BoundingBoxPolygon box = transform.boundingBoxPolygons[index];
                if (box.positionCalculated) {
                    int left;
                    int right;
                    if (box.right < box.left) {
                        left = box.right - box.size;
                        right = box.size + box.left;
                    } else {
                        left = box.left - box.size;
                        right = box.right + box.size;
                    }
                    int bottom;
                    int top;
                    if (box.top < box.bottom) {
                        bottom = box.top - box.size;
                        top = box.size + box.bottom;
                    } else {
                        bottom = box.bottom - box.size;
                        top = box.top + box.size;
                    }
                    if (!bool3 || left < minX) {
                        minX = left;
                    }
                    if (!bool3 || bottom < minY) {
                        minY = bottom;
                    }
                    if (!bool3 || right > maxX) {
                        maxX = right;
                    }
                    if (!bool3 || top > maxY) {
                        maxY = top;
                    }
                    bool3 = true;
                }
            }
        }
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = bool3 ? 1 : 0;
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = minX;
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = minY;
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = maxX;
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = maxY;
    }

    public static void method4883(Component icomponentdefinitions0, int i_1, int i_2) {
        if (icomponentdefinitions0.anIntArray1267 == null) {
            if (i_2 <= 0) {
                return;
            }
            icomponentdefinitions0.keyTriggeringAction = new byte[11][];
            icomponentdefinitions0.keyTriggerParams = new byte[11][];
            icomponentdefinitions0.cycleSteps = new int[11];
            icomponentdefinitions0.anIntArray1267 = new int[11];
        }
        icomponentdefinitions0.anIntArray1267[i_1] = i_2;
        if (i_2 > 0) {
            icomponentdefinitions0.hasInteraction = true;
        } else {
            icomponentdefinitions0.hasInteraction = false;
            for (int i_5 = 0; i_5 < icomponentdefinitions0.keyTriggeringAction.length; i_5++) {
                if (icomponentdefinitions0.keyTriggeringAction[i_5] != null || icomponentdefinitions0.anIntArray1267[i_5] > 0) {
                    icomponentdefinitions0.hasInteraction = true;
                    break;
                }
            }
        }
    }

    public static void method15207(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        int i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        int i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1;
        if (icomponentdefinitions0.mediaType != MediaType.NPC_MODEL && icomponentdefinitions0.mediaType != MediaType.NPC_HEAD) {
            throw new RuntimeException("");
        } else {
            NPCType npcdefinitions5 = TypeLists.NPC.list(icomponentdefinitions0.primaryMediaId);
            if (icomponentdefinitions0.npcCustomisation == null) {
                icomponentdefinitions0.npcCustomisation = new NPCTypeCustomisation(npcdefinitions5, icomponentdefinitions0.mediaType == MediaType.NPC_MODEL);
            }
            icomponentdefinitions0.npcCustomisation.uid = CS2Executor.method1480();
            if (npcdefinitions5.recolorDst != null && i_4 >= 0 && i_4 < npcdefinitions5.recolorDst.length) {
                icomponentdefinitions0.npcCustomisation.recolorDst[i_4] = (short) i_3;
                Component.redrawComponent(icomponentdefinitions0);
            } else {
                throw new RuntimeException("");
            }
        }
    }

    public static void method1852(Component component, Interface intr, CS2Executor cs2) {
        cs2.intStackPtr -= 2;
        component.scrollX = cs2.intStack[cs2.intStackPtr];
        if (component.scrollX > component.scrollWidth - component.width) {
            component.scrollX = component.scrollWidth - component.width;
        }
        if (component.scrollX < 0) {
            component.scrollX = 0;
        }
        component.scrollY = cs2.intStack[cs2.intStackPtr + 1];
        if (component.scrollY > component.scrollHeight - component.height) {
            component.scrollY = component.scrollHeight - component.height;
        }
        if (component.scrollY < 0) {
            component.scrollY = 0;
        }
        Component.redrawComponent(component);
        if (component.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIfscrollposUpdate(component.idHash);
        }
    }

    public static void method6289(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onMouseOver = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method7927(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        cs2executor1.intStackPtr -= 2;
        int i_3 = cs2executor1.intStack[cs2executor1.intStackPtr] - 1;
        int i_4 = cs2executor1.intStack[cs2executor1.intStackPtr + 1];
        if (i_3 >= 0 && i_3 <= 9) {
            method4883(icomponentdefinitions0, i_3, i_4);
        } else {
            throw new RuntimeException();
        }
    }

    public static void method3078(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onHold = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method3672(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onMouseMove = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setOnMouseLeave(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        int[] ints_5 = popIntArrayParam(string_4, cs2executor2);
        if (ints_5 != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.mouseLeaveScript = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.mouseLeaveArrayParams = ints_5;
        icomponentdefinitions0.hasScript = true;
    }

    public static void method7889(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.hookrequestType1Params = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method774(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.flipHorizontal = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void ifSetModelOrigin(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 2;
        icomponentdefinitions0.originX = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.originY = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method2874(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1319 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method7267(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.usesOrthogonal = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method5009(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1413 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method7142(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.shadow = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method6288(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        int[] ints_5 = popIntArrayParam(string_4, cs2executor2);
        if (ints_5 != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onScrollWheel = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.mouseWheelArray = ints_5;
        icomponentdefinitions0.hasScript = true;
    }

    public static void method2915(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.spriteShadow = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method8754(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.filled = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void ifSetColor(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.color = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfcolorUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method6689(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        int[] ints_5 = popIntArrayParam(string_4, cs2executor2);
        if (ints_5 != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1397 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.varps = ints_5;
        icomponentdefinitions0.hasScript = true;
    }

    public static void method6690(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1361 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method4159(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 4;
        icomponentdefinitions0.modelTintRed = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.modelTintGreen = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        icomponentdefinitions0.modelTintBlue = cs2executor2.intStack[cs2executor2.intStackPtr + 2];
        icomponentdefinitions0.modelTintScalar = cs2executor2.intStack[cs2executor2.intStackPtr + 3];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method4160(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.borderThickness = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static int[] popIntArrayParam(String paramTypes, CS2Executor cs2executor1) {
        int[] intArr = null;
        if (!paramTypes.isEmpty() && paramTypes.charAt(paramTypes.length() - 1) == 'Y') {
            int size = cs2executor1.intStack[--cs2executor1.intStackPtr];
            if (size > 0) {
                intArr = new int[size];
                while (size-- > 0) {
                    intArr[size] = cs2executor1.intStack[--cs2executor1.intStackPtr];
                }
//				for (intArr = new int[size]; size-- > 0; intArr[size] = cs2executor_1.intStack[--cs2executor_1.intStackPtr]) {
//					;
//				}
            }
        }
        return intArr;
    }

    public static void method11221(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1393 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method11223(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onMouseLeaveScript = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method501(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.tiling = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method502(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        int[] ints_5 = popIntArrayParam(string_4, cs2executor2);
        if (ints_5 != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onKey = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.keyPressArray = ints_5;
        icomponentdefinitions0.hasScript = true;
    }

    public static void method505(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        int i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr];
        String string_5 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        ParamType attributedefault6 = TypeLists.PARAMS.list(i_4);
        if (!attributedefault6.typeName.equals(string_5)) {
            icomponentdefinitions0.method2000(i_4, string_5);
        } else {
            icomponentdefinitions0.method1995(i_4);
        }
    }

    public static void method506(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 2;
        icomponentdefinitions0.scrollWidth = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.scrollHeight = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icomponentdefinitions0, false);
        }
    }

    public static void method2738(Interface interface0, int i_1, int i_2, int i_3, boolean bool4, CS2Executor cs2executor5) {
        if (i_2 == 0) {
            throw new RuntimeException();
        } else {
            Component icomponentdefinitions7 = interface0.components[i_1];
            if (icomponentdefinitions7.slotChildren == null) {
                icomponentdefinitions7.slotChildren = new Component[i_3 + 1];
                icomponentdefinitions7.itemSlots = icomponentdefinitions7.slotChildren;
            }
            if (icomponentdefinitions7.slotChildren.length <= i_3) {
                Component[] arr_8;
                if (icomponentdefinitions7.slotChildren == icomponentdefinitions7.itemSlots) {
                    arr_8 = new Component[i_3 + 1];
                    System.arraycopy(icomponentdefinitions7.slotChildren, 0, arr_8, 0, icomponentdefinitions7.slotChildren.length);
                    icomponentdefinitions7.slotChildren = icomponentdefinitions7.itemSlots = arr_8;
                } else {
                    arr_8 = new Component[i_3 + 1];
                    Component[] arr_11 = new Component[i_3 + 1];
                    for (int i_10 = 0; i_10 < icomponentdefinitions7.slotChildren.length; i_10++) {
                        arr_8[i_10] = icomponentdefinitions7.slotChildren[i_10];
                        arr_11[i_10] = icomponentdefinitions7.itemSlots[i_10];
                    }
                    icomponentdefinitions7.slotChildren = arr_8;
                    icomponentdefinitions7.itemSlots = arr_11;
                }
            }
            if (i_3 > 0 && icomponentdefinitions7.slotChildren[i_3 - 1] == null) {
                throw new RuntimeException("" + (i_3 - 1));
            } else {
                Component icomponentdefinitions12 = new Component();
                icomponentdefinitions12.type = ComponentType.forId(i_2);
                icomponentdefinitions12.parentId = icomponentdefinitions12.idHash = icomponentdefinitions7.idHash;
                icomponentdefinitions12.slotId = i_3;
                icomponentdefinitions7.slotChildren[i_3] = icomponentdefinitions12;
                if (icomponentdefinitions7.slotChildren != icomponentdefinitions7.itemSlots) {
                    icomponentdefinitions7.itemSlots[i_3] = icomponentdefinitions12;
                }
                CS2Interface underlaydefinition13;
                if (bool4) {
                    underlaydefinition13 = cs2executor5.hookedInterface2;
                } else {
                    underlaydefinition13 = cs2executor5.hookedInterface1;
                }
                underlaydefinition13.inter = interface0;
                underlaydefinition13.comp = icomponentdefinitions12;
                Component.redrawComponent(icomponentdefinitions7);
            }
        }
    }

    public static void method2741(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        boolean bool5;
        bool5 = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        icomponentdefinitions0.alpha = bool5;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method8387(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.anInt1380 = cs2executor2.intStack[--cs2executor2.intStackPtr];
    }

    public static void method8390(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        int i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr];
        if (i_4 != icomponentdefinitions0.spriteId) {
            icomponentdefinitions0.spriteId = i_4;
            Component.redrawComponent(icomponentdefinitions0);
        }
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfgraphicUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method15256(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.anInt1381 = cs2executor2.intStack[--cs2executor2.intStackPtr];
    }

    public static void method4651(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1292 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method6313(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 4;
        icomponentdefinitions0.skyboxId = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.lightPositionX = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        icomponentdefinitions0.lightPositionY = cs2executor2.intStack[cs2executor2.intStackPtr + 2];
        icomponentdefinitions0.lightPositionZ = cs2executor2.intStack[cs2executor2.intStackPtr + 3];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method786(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        boolean bool4 = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        if (bool4 != icomponentdefinitions0.hidden) {
            icomponentdefinitions0.hidden = bool4;
            Component.redrawComponent(icomponentdefinitions0);
        }
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfhide(icomponentdefinitions0.idHash);
        }
    }

    public static void method789(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        if (icomponentdefinitions0.type == ComponentType.SPRITE) {
            method5459(icomponentdefinitions0, interface1, cs2executor2);
        }
    }

    public static void method5017(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1383 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method2955(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.moveOverCursor = cs2executor2.intStack[--cs2executor2.intStackPtr];
    }

    public static void method4772(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1419 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setTriggerKey(Component icomponentdefinitions0, int i_1, byte[] bytes_2, byte[] bytes_3) {
        if (icomponentdefinitions0.keyTriggeringAction == null) {
            if (bytes_2 == null) {
                return;
            }
            icomponentdefinitions0.keyTriggeringAction = new byte[11][];
            icomponentdefinitions0.keyTriggerParams = new byte[11][];
            icomponentdefinitions0.cycleSteps = new int[11];
            icomponentdefinitions0.anIntArray1267 = new int[11];
        }
        icomponentdefinitions0.keyTriggeringAction[i_1] = bytes_2;
        if (bytes_2 != null) {
            icomponentdefinitions0.hasInteraction = true;
        } else {
            icomponentdefinitions0.hasInteraction = false;
            for (int i_6 = 0; i_6 < icomponentdefinitions0.keyTriggeringAction.length; i_6++) {
                if (icomponentdefinitions0.keyTriggeringAction[i_6] != null || icomponentdefinitions0.anIntArray1267[i_6] > 0) {
                    icomponentdefinitions0.hasInteraction = true;
                    break;
                }
            }
        }
        icomponentdefinitions0.keyTriggerParams[i_1] = bytes_3;
    }

    public static void ifSetNPCHead(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.mediaType = MediaType.NPC_HEAD;
        icomponentdefinitions0.npcCustomisation = null;
        icomponentdefinitions0.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr];
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method2960(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.textAntiMacro = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedClearIftextantimacro(icomponentdefinitions0.idHash);
        }
    }

    public static void method11160(MiniMenuEntry class282Sub50Sub70, CS2Executor cs2executor1) {
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = MiniMenu.getClickType(class282Sub50Sub70);
        cs2executor1.stringStack[++cs2executor1.stringStackPtr - 1] = MiniMenu.method13768(class282Sub50Sub70);
        cs2executor1.stringStack[++cs2executor1.stringStackPtr - 1] = MiniMenu.method7303(class282Sub50Sub70);
        cs2executor1.stringStack[++cs2executor1.stringStackPtr - 1] = MiniMenu.addQuestChaticons(class282Sub50Sub70);
    }

    public static void method2593(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1394 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static Object[] popParamsDynamic(String paramTypes, CS2Executor executor) {
        Object[] params = new Object[paramTypes.length() + 1];
        for (int i = params.length - 1; i >= 1; --i) {
            if (paramTypes.charAt(i - 1) == 's') {
                params[i] = executor.stringStack[--executor.stringStackPtr];
            } else if (paramTypes.charAt(i - 1) == '\u00a7') {
                params[i] = Long.valueOf(executor.longStack[--executor.longStackPtr]);
            } else {
                params[i] = Integer.valueOf(executor.intStack[--executor.intStackPtr]);
            }
        }
        int scriptId = executor.intStack[--executor.intStackPtr];
        if (scriptId != -1) {
            params[0] = Integer.valueOf(scriptId);
        } else {
            params = null;
        }
        return params;
    }

    public static void method13468(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.clickMask = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfclickmaskUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method4136(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1409 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setItemIFComp(Component ifComp, Interface interface1, boolean wearCol, Component.StackMode renderStack, CS2Executor cs2executor4) {
        cs2executor4.intStackPtr -= 2;
        int itemId = cs2executor4.intStack[cs2executor4.intStackPtr];
        int i_7 = cs2executor4.intStack[cs2executor4.intStackPtr + 1];
        if (ifComp.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfitemUpdate(ifComp.idHash);
            PulseEventQueue.delayedIfangleUpdate(ifComp.idHash);
            PulseEventQueue.delayedIfoffsetUpdate(ifComp.idHash);
        }
        if (itemId == -1) {
            ifComp.mediaType = MediaType.RAW_MODEL;
            ifComp.primaryMediaId = -1;
            ifComp.containerItemId = -1;
        } else {
            ifComp.containerItemId = itemId;
            ifComp.itemStackSize = i_7;
            ifComp.wearCol = wearCol;
            ItemType itemdefinitions8 = TypeLists.OBJ.list(itemId);
            ifComp.spritePitch = itemdefinitions8.modelRotationX;
            ifComp.spriteRoll = itemdefinitions8.modelRotationY;
            ifComp.spriteYaw = itemdefinitions8.modelRotationZ;
            ifComp.offsetX = itemdefinitions8.modelOffsetX;
            ifComp.offsetY = itemdefinitions8.modelOffsetY;
            ifComp.spriteScale = itemdefinitions8.modelZoom;
            ifComp.renderStack = renderStack;
            if (ifComp.aspectWidth > 0) {
                ifComp.spriteScale = ifComp.spriteScale * 32 / ifComp.aspectWidth;
            } else if (ifComp.baseWidth > 0) {
                ifComp.spriteScale = ifComp.spriteScale * 32 / ifComp.baseWidth;
            }
        }
    }

    public static void setCompText(Component component, Interface intr, CS2Executor cs2) {
        String text = (String) cs2.stringStack[--cs2.stringStackPtr];
        if (!text.equals(component.text)) {
            component.text = text;
            Component.redrawComponent(component);
        }
        if (component.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIftextUpdate(component.idHash);
        }
    }

    public static void setOpBase(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.opBase = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (Loader.DEBUG_IF_OPS)
            icomponentdefinitions0.opBase += "(opBase)";
    }

    public static void method14482(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        int i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr];
        int i_5 = cs2executor2.intStack[--cs2executor2.intStackPtr];
        if (i_5 >= 1 && i_5 <= 10) {
            icomponentdefinitions0.addCursor(i_5 - 1, i_4);
        }
    }

    public static void method1083(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1451 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setIfTextAlign(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 3;
        icomponentdefinitions0.textHorizontalAli = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.textVerticalAli = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        icomponentdefinitions0.anInt1358 = cs2executor2.intStack[cs2executor2.intStackPtr + 2];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method2170(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.hookrequestType0Params = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method14605(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        byte b_3 = 10;
        int i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        method4883(icomponentdefinitions0, b_3, i_4);
    }

    public static void method6186(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        if (icomponentdefinitions0.type == ComponentType.TEXT) {
            method5459(icomponentdefinitions0, interface1, cs2executor2);
        }
    }

    public static void method8310(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.angle2d = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method542(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1407 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setIfFont(Component component, Interface intr, CS2Executor cs2) {
        component.fontId = cs2.intStack[--cs2.intStackPtr];
        Component.redrawComponent(component);
        if (component.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIffontUpdate(component.idHash);
        }
    }

    public static void method7729(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 2;
        icomponentdefinitions0.sunRotationX = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.sunRotationY = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void setComponentPosition(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 4;
        icomponentdefinitions0.basePositionX = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.basePositionY = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        int i_4 = cs2executor2.intStack[cs2executor2.intStackPtr + 2];
        if (i_4 < 0) {
            i_4 = 0;
        } else if (i_4 > 5) {
            i_4 = 5;
        }
        int i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 3];
        if (i_5 < 0) {
            i_5 = 0;
        } else if (i_5 > 5) {
            i_5 = 5;
        }
        icomponentdefinitions0.aspectXType = (byte) i_4;
        icomponentdefinitions0.aspectYType = (byte) i_5;
        Component.redrawComponent(icomponentdefinitions0);
        method913(interface1, icomponentdefinitions0);
        if (icomponentdefinitions0.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icomponentdefinitions0, false);
        }
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfpositionUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method1454(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 2;
        int i_4 = cs2executor2.intStack[cs2executor2.intStackPtr];
        int i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        ParamType attributedefault6 = TypeLists.PARAMS.list(i_4);
        if (i_5 != attributedefault6.defaultInt) {
            icomponentdefinitions0.method1999(i_4, i_5);
        } else {
            icomponentdefinitions0.method1995(i_4);
        }
    }

    public static void method1455(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 4;
        icomponentdefinitions0.primaryMediaId = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.secondaryMediaId = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        if (cs2executor2.intStack[cs2executor2.intStackPtr + 2] == 1) {
            icomponentdefinitions0.mediaType = MediaType.ITEM_CONTAINER_FEMALE;
        } else {
            icomponentdefinitions0.mediaType = MediaType.ITEM_CONTAINER_MALE;
        }
        icomponentdefinitions0.wearCol = cs2executor2.intStack[cs2executor2.intStackPtr + 3] == 1;
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method14653(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1353 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method12260(Component icDefs, Interface interface1, CS2Executor executor) {
        executor.intStackPtr -= 4;
        icDefs.baseWidth = executor.intStack[executor.intStackPtr];
        icDefs.baseHeight = executor.intStack[executor.intStackPtr + 1];
        icDefs.aspectWidth = 0;
        icDefs.aspectHeight = 0;
        int i_4 = executor.intStack[executor.intStackPtr + 2];
        if (i_4 < 0) {
            i_4 = 0;
        } else if (i_4 > 4) {
            i_4 = 4;
        }
        int i_5 = executor.intStack[executor.intStackPtr + 3];
        if (i_5 < 0) {
            i_5 = 0;
        } else if (i_5 > 4) {
            i_5 = 4;
        }
        icDefs.aspectWidthType = (byte) i_4;
        icDefs.aspectHeightType = (byte) i_5;
        Component.redrawComponent(icDefs);
        method913(interface1, icDefs);
        if (icDefs.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icDefs, false);
        }
    }

    public static void method7555(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.mediaType = MediaType.RAW_MODEL;
        icomponentdefinitions0.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method6010(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 3;
        int i_4 = cs2executor2.intStack[cs2executor2.intStackPtr];
        short s_5 = (short) cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        short s_6 = (short) cs2executor2.intStack[cs2executor2.intStackPtr + 2];
        if (i_4 >= 0 && i_4 < 5) {
            icomponentdefinitions0.retexture(i_4, s_5, s_6);
            Component.redrawComponent(icomponentdefinitions0);
            if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
                PulseEventQueue.delayedIfretex(icomponentdefinitions0.idHash, i_4);
            }
        }
    }

    public static void method1845(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1271 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method3365(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onRelease = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method3366(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1416 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setTargetVerb(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.targetVerb = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (Loader.DEBUG_IF_OPS)
            icomponentdefinitions0.targetVerb += "(targetVerb)";
    }

    public static void method3965(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        FontMetrics fontmetrics3 = icomponentdefinitions0.getFontMetrics(StaticMethods.initializedFontcombos, client.FONT_PROVIDER);
        int i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        int i_5 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        int i_6 = fontmetrics3.method6956(icomponentdefinitions0.text, icomponentdefinitions0.width, icomponentdefinitions0.anInt1358, i_5, i_4, StaticMedia.nameIconSprites);
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = i_6;
    }

    public static void method3987(Component icomponentdefinitions0, Interface interface1) {
        icomponentdefinitions0.mediaType = MediaType.PLAYER_HEAD;
        icomponentdefinitions0.primaryMediaId = PlayerUpdate.myPlayerIndex;
        icomponentdefinitions0.secondaryMediaId = 0;
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method1494(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        int i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        int i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1;
        if (icomponentdefinitions0.mediaType != MediaType.NPC_HEAD) {
            throw new RuntimeException("");
        } else {
            NPCType npcdefinitions5 = TypeLists.NPC.list(icomponentdefinitions0.primaryMediaId);
            if (icomponentdefinitions0.npcCustomisation == null) {
                icomponentdefinitions0.npcCustomisation = new NPCTypeCustomisation(npcdefinitions5, false);
            }
            icomponentdefinitions0.npcCustomisation.uid = CS2Executor.method1480();
            if (i_4 >= 0 && i_4 < npcdefinitions5.headModels.length) {
                icomponentdefinitions0.npcCustomisation.modelIds[i_4] = i_3;
                Component.redrawComponent(icomponentdefinitions0);
            } else {
                throw new RuntimeException("");
            }
        }
    }

    public static void setResumeText(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.resumeText = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (Loader.DEBUG_IF_OPS)
            icomponentdefinitions0.resumeText += "(resumeText)";
    }

    public static void method12401(Component icomponentdefinitions0, Interface interface1) {
        icomponentdefinitions0.mediaType = MediaType.PLAYER_MODEL;
        icomponentdefinitions0.primaryMediaId = PlayerUpdate.myPlayerIndex;
        icomponentdefinitions0.secondaryMediaId = 0;
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method14892(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.lineWidth = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method4276(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 6;
        icomponentdefinitions0.offsetX = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.offsetY = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        icomponentdefinitions0.spritePitch = cs2executor2.intStack[cs2executor2.intStackPtr + 2];
        icomponentdefinitions0.spriteRoll = cs2executor2.intStack[cs2executor2.intStackPtr + 3];
        icomponentdefinitions0.spriteYaw = cs2executor2.intStack[cs2executor2.intStackPtr + 4];
        icomponentdefinitions0.spriteScale = cs2executor2.intStack[cs2executor2.intStackPtr + 5];
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(icomponentdefinitions0.idHash);
            PulseEventQueue.delayedIfoffsetUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method4280(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        boolean bool5;
        bool5 = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        icomponentdefinitions0.flipVertical = bool5;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method12630(Component icomponentdefinitions0) {
        icomponentdefinitions0.reset();
    }

    public static void method5066(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.mediaType = MediaType.PLAYER_MODEL;
        icomponentdefinitions0.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr];
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method13450(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        int i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr];
        icomponentdefinitions0.lineDirection = i_4 == 1;
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method6942(Component component) {
        if (client.getComponentSettings(component).continueOptionEnabled() && client.aClass1187352 == null) {
            PacketEncoder.continueInterfacePressed(component.idHash, component.slotId);
            client.aClass1187352 = Component.listIcomponent(component.idHash, component.slotId);
            assert client.aClass1187352 != null;
            Component.redrawComponent(client.aClass1187352);
        }
    }

    public static void method3601(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        int i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        int i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1;
        if (icomponentdefinitions0.mediaType != MediaType.NPC_MODEL && icomponentdefinitions0.mediaType != MediaType.NPC_HEAD) {
            throw new RuntimeException("");
        } else {
            NPCType npcdefinitions5 = TypeLists.NPC.list(icomponentdefinitions0.primaryMediaId);
            if (icomponentdefinitions0.npcCustomisation == null) {
                icomponentdefinitions0.npcCustomisation = new NPCTypeCustomisation(npcdefinitions5, icomponentdefinitions0.mediaType == MediaType.NPC_MODEL);
            }
            icomponentdefinitions0.npcCustomisation.uid = CS2Executor.method1480();
            if (npcdefinitions5.retextureDst != null && i_4 >= 0 && i_4 < npcdefinitions5.retextureDst.length) {
                icomponentdefinitions0.npcCustomisation.retextureDst[i_4] = (short) i_3;
                Component.redrawComponent(icomponentdefinitions0);
            } else {
                throw new RuntimeException("");
            }
        }
    }

    public static void method5291(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        int i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr] - 1;
        if (i_4 >= 0 && i_4 <= 9) {
            icomponentdefinitions0.addOptionName(i_4, (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr]);
        } else {
            --cs2executor2.stringStackPtr;
        }
    }

    public static void method797(Component component, Interface intr, CS2Executor cs2) {
        int sequenceId = cs2.intStack[--cs2.intStackPtr];
        if (sequenceId != component.animation) {
            if (sequenceId != -1) {
                if (component.animatingSequence == null) {
                    component.animatingSequence = new InterfaceSequence();
                }
                component.animatingSequence.update(sequenceId);
            } else {
                component.animatingSequence = null;
            }
            component.animation = sequenceId;
            Component.redrawComponent(component);
        }
        if (component.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIfanimUpdate(component.idHash);
        }
    }

    public static void setTransparency(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.transparency = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method5455(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1418 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method5459(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        int i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr];
        if (i_4 != icomponentdefinitions0.anInt1435) {
            icomponentdefinitions0.anInt1435 = i_4;
            Component.redrawComponent(icomponentdefinitions0);
        }
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.method5581(icomponentdefinitions0.idHash);
        }
    }

    public static void method569(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.maxTextLines = cs2executor2.intStack[--cs2executor2.intStackPtr];
        Component.redrawComponent(icomponentdefinitions0);
    }

    public static void method3251(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        if (cs2executor1.hookRequestCount >= 10) {
            throw new RuntimeException();
        } else {
            if (icomponentdefinitions0.anObjectArray1271 != null) {
                HookRequest hookrequest3 = new HookRequest();
                hookrequest3.source = icomponentdefinitions0;
                hookrequest3.params = icomponentdefinitions0.anObjectArray1271;
                hookrequest3.requestId = cs2executor1.hookRequestCount + 1;
                client.PENDING_HOOK_REQUESTS.addFirst(hookrequest3);
            }
        }
    }

    public static void method5001(Component icomponentdefinitions0, CS2Executor cs2executor1) {
        FontMetrics fontmetrics3 = icomponentdefinitions0.getFontMetrics(StaticMethods.initializedFontcombos, client.FONT_PROVIDER);
        int i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr];
        Point point5 = fontmetrics3.method6954(icomponentdefinitions0.text, icomponentdefinitions0.width, icomponentdefinitions0.anInt1358, i_4, StaticMedia.nameIconSprites);
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = point5.x;
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = point5.y;
    }

    public static void method3338(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        int[] ints_5 = popIntArrayParam(string_4, cs2executor2);
        if (ints_5 != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1387 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.anIntArray1402 = ints_5;
        icomponentdefinitions0.hasScript = true;
    }

    public static void ifSetAspect(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 2;
        icomponentdefinitions0.aspectX = cs2executor2.intStack[cs2executor2.intStackPtr];
        icomponentdefinitions0.aspectY = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        Component.redrawComponent(icomponentdefinitions0);
        method913(interface1, icomponentdefinitions0);
        if (icomponentdefinitions0.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icomponentdefinitions0, false);
        }
    }
    
    public static void method913(Interface interface0, Component icomponentdefinitions1) {
        Component icomponentdefinitions3 = Interface.getSub(interface0, icomponentdefinitions1);
        int i_4;
        int i_5;
        if (icomponentdefinitions3 == null) {
            i_4 = GameShell.screenCanvasWidth;
            i_5 = client.screenCanvasHeight * -969250379;
        } else {
            i_4 = icomponentdefinitions3.width;
            i_5 = icomponentdefinitions3.height;
        }
        InterfaceRenderer.initSizes(icomponentdefinitions1, i_4, i_5, false);
        InterfaceComponentRenderer.componentPositionAdjustment(icomponentdefinitions1, i_4, i_5);
    }

    public static void setDepthChild(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 2;
        int i_4 = cs2executor2.intStack[cs2executor2.intStackPtr];
        int i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        if (i_4 == -1 && i_5 == -1) {
            icomponentdefinitions0.depthChildOverride = null;
        } else {
            icomponentdefinitions0.depthChildOverride = Component.listIcomponent(i_4, i_5);
        }
    }

    public static void method14586(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.hookParams = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method6045(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.targetLeaveCursor = cs2executor2.intStack[--cs2executor2.intStackPtr];
        icomponentdefinitions0.targetOverCursor = cs2executor2.intStack[--cs2executor2.intStackPtr];
    }

    public static void setOnMouseOver(Component iCompDefs, CS2Executor cs2executor2) {
        String paramTypes = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(paramTypes, cs2executor2) != null) {
            paramTypes = paramTypes.substring(0, paramTypes.length() - 1);
        }
        iCompDefs.popupScript = popParamsDynamic(paramTypes, cs2executor2);
        iCompDefs.hasScript = true;
    }

    public static void method869(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        Component.UnkSetting1 i_4 = Component.UnkSetting1.forId(cs2executor2.intStack[--cs2executor2.intStackPtr]);
        if (i_4 != null)
            icomponentdefinitions0.anUnkSetting11 = i_4;
    }

    public static void method910(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.params = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setNoClickThrough(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.preventClickThrough = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
    }

    public static void method12209(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 2;
        byte b_4 = 10;
        byte[] bytes_5 = {(byte) cs2executor2.intStack[cs2executor2.intStackPtr]};
        byte[] bytes_6 = {(byte) cs2executor2.intStack[cs2executor2.intStackPtr + 1]};
        setTriggerKey(icomponentdefinitions0, b_4, bytes_5, bytes_6);
    }

    public static void method1838(Component icomponentdefinitions0, byte[] bytes_1, byte[] bytes_2, CS2Executor cs2executor3) {
        int i_5 = cs2executor3.intStack[--cs2executor3.intStackPtr] - 1;
        if (i_5 >= 0 && i_5 <= 9) {
            setTriggerKey(icomponentdefinitions0, i_5, bytes_1, bytes_2);
        } else {
            throw new RuntimeException();
        }
    }

    public static void method15409(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.onClick = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method6158(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1415 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void method11337(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.mediaType = MediaType.NPC_MODEL;
        icomponentdefinitions0.npcCustomisation = null;
        icomponentdefinitions0.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr];
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0.idHash);
        }
    }

    public static void method15474(Component icomponentdefinitions0) {
        icomponentdefinitions0.options = null;
    }

    public static void method1642(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        String string_4 = (String) cs2executor2.stringStack[--cs2executor2.stringStackPtr];
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4.substring(0, string_4.length() - 1);
        }
        icomponentdefinitions0.anObjectArray1400 = popParamsDynamic(string_4, cs2executor2);
        icomponentdefinitions0.hasScript = true;
    }

    public static void setTargetOpCursor(Component icomponentdefinitions0, CS2Executor cs2executor2) {
        icomponentdefinitions0.targetOpCursor = cs2executor2.intStack[--cs2executor2.intStackPtr];
    }

    public static void method7667(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr -= 3;
        int i_4 = cs2executor2.intStack[cs2executor2.intStackPtr];
        short s_5 = (short) cs2executor2.intStack[cs2executor2.intStackPtr + 1];
        short s_6 = (short) cs2executor2.intStack[cs2executor2.intStackPtr + 2];
        if (i_4 >= 0 && i_4 < 5) {
            icomponentdefinitions0.recolor(i_4, s_5, s_6);
            Component.redrawComponent(icomponentdefinitions0);
            if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
                PulseEventQueue.delayedIfrecol(icomponentdefinitions0.idHash, i_4);
            }
        }
    }

    public static void method6697(Component icomponentdefinitions0, Interface interface1, CS2Executor cs2executor2) {
        icomponentdefinitions0.monospaced = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1;
        Component.redrawComponent(icomponentdefinitions0);
        if (icomponentdefinitions0.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfmonospacedUpdate(icomponentdefinitions0.idHash);
        }
    }

	public static CreateAccountRequest method2598() {
	    return AccountCreationStage.aClass4942916 == null ? CreateAccountRequest.aClass4945786 : AccountCreationStage.aClass4942916;
	}

	public static void queryItem(String query, boolean tradeableOnly, int paramId, int paramIntQuery, String paramStringQuery, boolean paramIsString) {
	    query = query.toLowerCase();
	    short[] results = new short[16];
	    int paramIntDefault = -1;
	    String paramStrDefault = null;
	    if (paramId != -1) {
	        ParamType paramDef = TypeLists.PARAMS.list(paramId);
	        if (paramDef == null || paramDef.isString() != paramIsString) {
	            return;
	        }
	        if (paramDef.isString()) {
	            paramStrDefault = paramDef.typeName;
	        } else {
	            paramIntDefault = paramDef.defaultInt;
	        }
	    }
	    int numResults = 0;
	    for (int i = 0; i < TypeLists.OBJ.totalItemCount; i++) {
	        ItemType itemDef = TypeLists.OBJ.list(i);
	        if ((!tradeableOnly || itemDef.tradeable) && itemDef.certTemplateId == -1 && itemDef.lendTemplateId == -1 && itemDef.bindTemplateId == -1 && itemDef.unknownInt6 == 0 && itemDef.name.toLowerCase().indexOf(query) != -1) {
	            if (paramId != -1) {
	                if (paramIsString) {
	                    if (!paramStringQuery.equals(itemDef.getCs2String(paramId, paramStrDefault))) {
	                        continue;
	                    }
	                } else if (paramIntQuery != itemDef.getCs2Integer(paramId, paramIntDefault)) {
	                    continue;
	                }
	            }
	            if (numResults >= 250) {
	                CS2_QUERY_RESULTS_LEN = -1;
	                ColorImageCache.CS2_QUERY_RESULTS = null;
	                return;
	            }
	            if (numResults >= results.length) {
	                short[] newResults = new short[results.length * 2];
	                System.arraycopy(results, 0, newResults, 0, numResults);
	                results = newResults;
	            }
	            results[numResults++] = (short) i;
	        }
	    }
	    ColorImageCache.CS2_QUERY_RESULTS = results;
	    StaticElements.CS2_QUERY_RESULT_IDX = 0;
	    CS2_QUERY_RESULTS_LEN = numResults;
	    String[] resultNames = new String[CS2_QUERY_RESULTS_LEN];
	    for (int i_12 = 0; i_12 < CS2_QUERY_RESULTS_LEN; i_12++) {
	        resultNames[i_12] = TypeLists.OBJ.list(results[i_12]).name;
	    }
	    Utils.sortAlphabetically(resultNames, ColorImageCache.CS2_QUERY_RESULTS);
	}
}