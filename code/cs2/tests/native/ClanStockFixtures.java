import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannelPlayer;
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.GrandExchangeSlot;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ClanStockFixtures {
    static final long SEED = 0x50c1a15L;
    static final String[] NAMES = {"Zezima", "zezima", "ZEZIMA", "Mod Mark", "mod_mark", "Bob", "bob", "", "x", "A", "a", "Straße", "abc", "abd", "Abc", "b", "B", "a b", "9", "10", "_"};
    static final int[] SMALL = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 50, Integer.MIN_VALUE, Integer.MAX_VALUE};
    static final int[] BIT = {-1, 0, 1, 5, 7, 8, 15, 16, 30, 31, 32, 33, 63};
    static final int[] WIDE = {0, 1, -1, 127, -128, 100, 255, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x12345678, -559038737};
    static final String[] SETTINGS_FIND = {"ACTIVECLANSETTINGS_FIND_LISTENED", "ACTIVECLANSETTINGS_FIND_AFFINED"};
    static final String[] CHANNEL_FIND = {"ACTIVECLANCHANNEL_FIND_LISTENED", "ACTIVECLANCHANNEL_FIND_AFFINED"};
    static final String[] NO_ARG = {
        "ACTIVECLANSETTINGS_FIND_LISTENED", "ACTIVECLANSETTINGS_FIND_AFFINED", "ACTIVECLANCHANNEL_FIND_LISTENED", "ACTIVECLANCHANNEL_FIND_AFFINED", "CLANPROFILE_FIND",
        "ACTIVECLANSETTINGS_GETCLANNAME", "ACTIVECLANSETTINGS_GETALLOWUNAFFINED", "ACTIVECLANSETTINGS_GETRANKTALK", "ACTIVECLANSETTINGS_GETRANKKICK",
        "ACTIVECLANSETTINGS_GETRANKLOOTSHARE", "ACTIVECLANSETTINGS_GETCOINSHARE", "ACTIVECLANSETTINGS_GETAFFINEDCOUNT", "ACTIVECLANSETTINGS_GETBANNEDCOUNT",
        "ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT", "ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT",
        "ACTIVECLANCHANNEL_GETCLANNAME", "ACTIVECLANCHANNEL_GETRANKKICK", "ACTIVECLANCHANNEL_GETRANKTALK", "ACTIVECLANCHANNEL_GETUSERCOUNT"
    };
    static final String[] ONE_ARG = {
        "ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME", "ACTIVECLANSETTINGS_GETAFFINEDRANK", "ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME",
        "ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY", "ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT",
        "ACTIVECLANCHANNEL_GETUSERDISPLAYNAME", "ACTIVECLANCHANNEL_GETUSERRANK", "ACTIVECLANCHANNEL_GETUSERWORLD", "ACTIVECLANCHANNEL_KICKUSER",
        "ACTIVECLANCHANNEL_GETSORTEDUSERSLOT", "UNUSED_CLAN_OP",
        "STOCKMARKET_GETOFFERTYPE", "STOCKMARKET_GETOFFERITEM", "STOCKMARKET_GETOFFERPRICE", "STOCKMARKET_GETOFFERCOUNT", "STOCKMARKET_GETOFFERCOMPLETEDCOUNT",
        "STOCKMARKET_GETOFFERCOMPLETEDGOLD", "STOCKMARKET_ISOFFEREMPTY", "STOCKMARKET_ISOFFERSTABLE", "STOCKMARKET_ISOFFERFINISHED", "STOCKMARKET_ISOFFERADDING"
    };
    static final String[] STRING_ARG = {"ACTIVECLANSETTINGS_GETAFFINEDSLOT", "ACTIVECLANCHANNEL_GETUSERSLOT"};
    static final String EXTRA = "ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO";

    static Field field(Class<?> type, String name) throws Exception {
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    static void set(Object target, String name, Object value) throws Exception {
        var f = field(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    static Object staticGet(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void staticSet(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        f.set(null, value);
    }

    static String nullable(String value) {
        return value == null ? "~" : value;
    }

    static String ints(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String strings(Object[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append('|');
            sb.append(values[i] == null ? "~" : "\"" + values[i] + "\"");
        }
        return sb.toString();
    }

    static String hex(TcpPacket packet) {
        var sb = new StringBuilder();
        for (int i = 0; i < packet.capacity; i++) sb.append(String.format("%02x", packet.buffer.buffer[i] & 0xff));
        return sb.toString();
    }

    static String name(Random random) {
        return NAMES[random.nextInt(NAMES.length)];
    }

    static int pick(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static byte rank(Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> (byte) -1;
            case 1 -> (byte) random.nextInt(128);
            case 2 -> (byte) (random.nextInt(256) - 128);
            default -> (byte) random.nextInt(8);
        };
    }

    static ClanChannel channel(StringBuilder out, int state, int id, Random random) throws Exception {
        var channel = (ClanChannel) unsafe.allocateInstance(ClanChannel.class);
        boolean playersNull = random.nextInt(8) == 0;
        int length = random.nextInt(8);
        int count = switch (random.nextInt(6)) {
            case 0 -> Math.max(0, length - 1 - random.nextInt(2));
            case 1 -> length + 1 + random.nextInt(2);
            default -> length;
        };
        String clanName = random.nextInt(5) == 0 ? null : name(random);
        byte minKick = rank(random);
        byte talk = rank(random);
        set(channel, "numPlayers", count);
        set(channel, "clanName", clanName);
        set(channel, "minRankToKick", minKick);
        set(channel, "guestsTalk", talk);
        out.append("K\t").append(state).append('\t').append(id).append('\t').append(nullable(clanName)).append('\t').append(playersNull ? 1 : 0).append('\t')
            .append(length).append('\t').append(count).append('\t').append(minKick).append('\t').append(talk).append('\n');
        if (playersNull) {
            set(channel, "players", null);
            return channel;
        }
        var players = new ClanChannelPlayer[length];
        for (int i = 0; i < length; i++) {
            if (random.nextInt(10) == 0) {
                out.append("n\t").append(state).append('\t').append(id).append('\t').append(i).append('\n');
                continue;
            }
            var player = new ClanChannelPlayer();
            player.name = random.nextInt(8) == 0 ? null : name(random);
            player.rank = random.nextInt(3) == 0 ? (byte) -1 : rank(random);
            player.world = pick(random, WIDE);
            players[i] = player;
            out.append("k\t").append(state).append('\t').append(id).append('\t').append(i).append('\t').append(nullable(player.name)).append('\t').append(player.rank).append('\t').append(player.world).append('\n');
        }
        set(channel, "players", players);
        return channel;
    }

    static ClanSettings settings(StringBuilder out, int state, int id, Random random) throws Exception {
        var settings = (ClanSettings) unsafe.allocateInstance(ClanSettings.class);
        String clanName = random.nextInt(5) == 0 ? null : name(random);
        boolean allow = random.nextBoolean();
        byte talk = rank(random), kick = rank(random), loot = rank(random), coin = rank(random);
        int namesLen = random.nextInt(8);
        int otherLen = random.nextInt(8);
        int bansLen = random.nextInt(5);
        int count = switch (random.nextInt(6)) {
            case 0 -> namesLen + 1 + random.nextInt(2);
            case 1 -> Math.max(0, namesLen - 1);
            default -> namesLen;
        };
        int banCount = random.nextInt(3) == 0 ? bansLen + 1 : bansLen;
        int owner = random.nextInt(10) - 2;
        int replacement = random.nextInt(10) - 2;
        boolean namesNull = random.nextInt(10) == 0, ranksNull = random.nextInt(10) == 0, daysNull = random.nextInt(10) == 0;
        boolean flagsNull = random.nextInt(10) == 0, bansNull = random.nextInt(10) == 0;
        set(settings, "clanName", clanName);
        set(settings, "allowGuests", allow);
        set(settings, "talkRank", talk);
        set(settings, "kickRank", kick);
        set(settings, "lootshareRank", loot);
        set(settings, "coinShare", coin);
        set(settings, "memberCount", count);
        set(settings, "banCount", banCount);
        set(settings, "currentOwner", owner);
        set(settings, "replacementOwner", replacement);
        out.append("L\t").append(state).append('\t').append(id).append('\t').append(nullable(clanName)).append('\t').append(allow ? 1 : 0).append('\t')
            .append(talk).append('\t').append(kick).append('\t').append(loot).append('\t').append(coin).append('\t').append(count).append('\t').append(banCount).append('\t')
            .append(owner).append('\t').append(replacement).append('\t').append(namesNull ? -1 : namesLen).append('\t').append(ranksNull ? -1 : otherLen).append('\t')
            .append(daysNull ? -1 : otherLen).append('\t').append(flagsNull ? -1 : otherLen).append('\t').append(bansNull ? -1 : bansLen).append('\n');
        if (!namesNull) {
            var names = new String[namesLen];
            for (int i = 0; i < namesLen; i++) {
                names[i] = name(random);
                out.append("m\t").append(state).append('\t').append(id).append('\t').append(i).append('\t').append(names[i]).append('\n');
            }
            set(settings, "memberNames", names);
        }
        var ranks = new byte[otherLen];
        var days = new int[otherLen];
        var flags = new int[otherLen];
        for (int i = 0; i < otherLen; i++) {
            ranks[i] = rank(random);
            days[i] = pick(random, WIDE);
            flags[i] = pick(random, WIDE);
            out.append("r\t").append(state).append('\t').append(id).append('\t').append(i).append('\t').append(ranks[i]).append('\t').append(days[i]).append('\t').append(flags[i]).append('\n');
        }
        if (!ranksNull) set(settings, "memberRanks", ranks);
        if (!daysNull) set(settings, "memberJoinDays", days);
        if (!flagsNull) set(settings, "clanFlags", flags);
        if (!bansNull) {
            var bans = new String[bansLen];
            for (int i = 0; i < bansLen; i++) {
                bans[i] = name(random);
                out.append("b\t").append(state).append('\t').append(id).append('\t').append(i).append('\t').append(bans[i]).append('\n');
            }
            set(settings, "bannedUserNames", bans);
        }
        return settings;
    }

    static sun.misc.Unsafe unsafe;

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        staticSet(Client.class, "CURRENT_LANGUAGE", Language.ENGLISH);
        var context = (BufferedConnectionContext) staticGet(Client.class, "GAME_CONNECTION_CONTEXT");
        var out = new StringBuilder();
        var random = new Random(SEED);
        int stateCount = 24;
        var applies = new ArrayList<Runnable>();
        for (int s = 0; s < stateCount; s++) {
            boolean vars = random.nextBoolean();
            var channels = new ClanChannel[2];
            var settingsPool = new ClanSettings[2];
            for (int i = 0; i < 2; i++) {
                channels[i] = channel(out, s, i, random);
                settingsPool[i] = settings(out, s, i, random);
            }
            int mainChannel = s % 6 == 5 ? -1 : random.nextInt(2);
            int listenedChannel = s % 5 == 4 ? -1 : (random.nextInt(3) == 0 && mainChannel >= 0 ? mainChannel : random.nextInt(2));
            int mainSettings = s % 7 == 6 ? -1 : random.nextInt(2);
            int guestSettings = s % 4 == 3 ? -1 : random.nextInt(2);
            out.append("H\t").append(s).append('\t').append(vars ? 1 : 0).append('\t').append(mainChannel).append('\t').append(listenedChannel).append('\t')
                .append(mainSettings).append('\t').append(guestSettings).append('\n');
            var slots = new GrandExchangeSlot[6];
            for (int i = 0; i < 6; i++) {
                if (random.nextInt(5) == 0) {
                    out.append("g\t").append(s).append('\t').append(i).append('\t').append('~').append('\n');
                    continue;
                }
                var slot = new GrandExchangeSlot();
                slot.progress = (byte) (random.nextInt(3) == 0 ? random.nextInt(256) - 128 : random.nextInt(16));
                slot.itemId = pick(random, WIDE);
                slot.price = pick(random, WIDE);
                slot.amount = pick(random, WIDE);
                slot.currentAmount = pick(random, WIDE);
                slot.totalPrice = pick(random, WIDE);
                slots[i] = slot;
                out.append("g\t").append(s).append('\t').append(i).append('\t').append(slot.progress).append('\t').append(slot.itemId).append('\t').append(slot.price).append('\t')
                    .append(slot.amount).append('\t').append(slot.currentAmount).append('\t').append(slot.totalPrice).append('\n');
            }
            final int fmc = mainChannel, flc = listenedChannel, fms = mainSettings, fgs = guestSettings;
            applies.add(() -> {
                try {
                    staticSet(Client.class, "CLAN_CHANNEL", fmc < 0 ? null : fresh(channels[fmc]));
                    ClanChannel main = (ClanChannel) staticGet(Client.class, "CLAN_CHANNEL");
                    staticSet(Client.class, "LISTENED_CLAN_CHANNEL", flc < 0 ? null : (flc == fmc ? main : fresh(channels[flc])));
                    staticSet(Client.class, "CLAN_SETTINGS", fms < 0 ? null : fresh(settingsPool[fms]));
                    staticSet(Client.class, "GUEST_CLAN_SETTINGS", fgs < 0 ? null : fresh(settingsPool[fgs]));
                    staticSet(ClanVarTypeList.class, "CLAN_VARS", vars ? new Object[0] : null);
                    var target = (GrandExchangeSlot[]) staticGet(Client.class, "GRAND_EXCHANGE_SLOTS");
                    System.arraycopy(slots, 0, target, 0, 6);
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
        }
        var cases = new ArrayList<Object[]>();
        String[][] settingsPreludes = {{}, {"SA"}, {"SL"}, {"SL", "SA"}};
        String[][] channelPreludes = {{}, {"CA"}, {"CL"}, {"CL", "CA"}};
        for (int s = 0; s < stateCount; s++) {
            for (var op : NO_ARG) {
                var preludes = op.contains("CHANNEL") ? channelPreludes : settingsPreludes;
                for (var prelude : preludes) cases.add(new Object[] {op, s, prelude, new int[0], null, false});
            }
            for (var op : ONE_ARG) {
                var preludes = op.startsWith("ACTIVECLANCHANNEL") || op.startsWith("UNUSED") ? channelPreludes : op.startsWith("STOCK") ? new String[][] {{}} : settingsPreludes;
                for (var prelude : preludes) {
                    for (int value : new int[] {-1, 0, 1, 2, 4, pick(random, SMALL)}) cases.add(new Object[] {op, s, prelude, new int[] {value}, null, false});
                }
            }
            for (var op : STRING_ARG) {
                var preludes = op.startsWith("ACTIVECLANCHANNEL") ? channelPreludes : settingsPreludes;
                for (var prelude : preludes) {
                    for (int i = 0; i < 8; i++) cases.add(new Object[] {op, s, prelude, new int[0], name(random), true});
                    cases.add(new Object[] {op, s, prelude, new int[0], null, true});
                }
            }
            for (var prelude : settingsPreludes) {
                for (int i = 0; i < 12; i++) {
                    cases.add(new Object[] {EXTRA, s, prelude, new int[] {pick(random, SMALL), pick(random, BIT), pick(random, BIT)}, null, false});
                }
            }
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        staticSet(Client.class, "gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
        for (var item : cases) {
            String op = (String) item[0];
            int state = (Integer) item[1];
            String[] prelude = (String[]) item[2];
            int[] intArgs = (int[]) item[3];
            String stringArg = (String) item[4];
            boolean hasString = (Boolean) item[5];
            applies.get(state).run();
            context.clearAllQueuedPackets();
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            String result = "ok";
            try {
                for (var p : prelude) {
                    String name = switch (p) {
                        case "SA" -> SETTINGS_FIND[1];
                        case "SL" -> SETTINGS_FIND[0];
                        case "CA" -> CHANNEL_FIND[1];
                        default -> CHANNEL_FIND[0];
                    };
                    ClanStockOps.INSTANCE.dispatch(ClientScriptOpCode.valueOf(name), e);
                }
                for (int value : intArgs) e.intStack[e.intStackPtr++] = value;
                if (hasString) e.stringStack[e.stringStackPtr++] = stringArg;
                ClanStockOps.INSTANCE.dispatch(ClientScriptOpCode.valueOf(op), e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            var packets = new ArrayList<String>();
            var node = context.queuedPackets.getFirst();
            while (node != null) {
                packets.add(hex((TcpPacket) node));
                node = context.queuedPackets.getNext();
            }
            out.append("T\t").append(op).append('\t').append(state).append('\t').append(prelude.length == 0 ? "-" : String.join(",", prelude)).append('\t')
                .append(intArgs.length == 0 ? "-" : ints(intArgs, intArgs.length)).append('\t').append(hasString ? "s" + nullable(stringArg) : "-").append('\t')
                .append(result).append('\t').append(ints(e.intStack, e.intStackPtr)).append('\t').append(strings(e.stringStack, e.stringStackPtr)).append('\t')
                .append(packets.isEmpty() ? "-" : String.join("|", packets)).append('\n');
            stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("clan-stock.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("clan-stock.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }

    static ClanChannel fresh(ClanChannel source) throws Exception {
        var copy = (ClanChannel) unsafe.allocateInstance(ClanChannel.class);
        for (var name : new String[] {"numPlayers", "clanName", "players", "minRankToKick", "guestsTalk"}) {
            var f = field(ClanChannel.class, name);
            f.setAccessible(true);
            f.set(copy, f.get(source));
        }
        return copy;
    }

    static ClanSettings fresh(ClanSettings source) throws Exception {
        var copy = (ClanSettings) unsafe.allocateInstance(ClanSettings.class);
        for (var name : new String[] {"clanName", "allowGuests", "talkRank", "kickRank", "lootshareRank", "coinShare", "memberCount", "memberNames", "memberRanks", "memberJoinDays", "banCount", "bannedUserNames", "currentOwner", "replacementOwner", "clanFlags"}) {
            var f = field(ClanSettings.class, name);
            f.setAccessible(true);
            f.set(copy, f.get(source));
        }
        return copy;
    }
}
