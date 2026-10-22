import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.param.ParamTypeList;
import com.jagex.game.runetek5.config.quest.QuestTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.QuestOps;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.vardomain.PlayerVarDomain;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Drives the real QuestOps.dispatch over real QuestTypeList/QuestType objects decoded from
 * synthetic quest files served by a fake Js5ResourceProvider. The player variables, varbit
 * definitions, skill levels and param types are injected; every case records the full int and
 * string stack, or the exception class.
 */
public final class QuestFixtures {
    static final int QUEST_ARCHIVE = 35;
    static final int[] VARPS = {0, 1, 2, 3, 10, 11, 12, 40};
    static final int[][] VARBITS = {{200, 3, 0, 2}, {201, 3, 3, 5}, {202, 10, 0, 7}, {203, 11, 16, 31}, {204, 12, 0, 31}, {205, 2, 5, 5}};
    static final int[] PARAM_IDS = {1, 2, 3, 4, 5, 6};
    static final int[] PARAM_QUERY = {1, 2, 3, 4, 5, 6};
    static final StringBuilder out = new StringBuilder();
    static final TreeMap<String, Integer> counts = new TreeMap<>();
    static final TreeMap<String, Integer> errors = new TreeMap<>();
    static int total;

    static final ClientScriptOpCode[] OPS = {
        ClientScriptOpCode.QUEST_GETNAME, ClientScriptOpCode.QUEST_GETSORTNAME, ClientScriptOpCode.QUEST_TYPE,
        ClientScriptOpCode.QUEST_GETDIFFICULTY, ClientScriptOpCode.QUEST_GETMEMBERS, ClientScriptOpCode.QUEST_POINTS,
        ClientScriptOpCode.QUEST_QUESTREQ_COUNT, ClientScriptOpCode.QUEST_QUESTREQ, ClientScriptOpCode.QUEST_QUESTREQ_MET,
        ClientScriptOpCode.QUEST_POINTSREQ, ClientScriptOpCode.QUEST_POINTSREQ_MET, ClientScriptOpCode.QUEST_STATREQ_COUNT,
        ClientScriptOpCode.QUEST_STATREQ_STAT, ClientScriptOpCode.QUEST_STATREQ_LEVEL, ClientScriptOpCode.QUEST_STATREQ_MET,
        ClientScriptOpCode.QUEST_VARPREQ_COUNT, ClientScriptOpCode.QUEST_VARPREQ_DESC, ClientScriptOpCode.QUEST_VARPREQ_MET,
        ClientScriptOpCode.QUEST_VARBITREQ_COUNT, ClientScriptOpCode.QUEST_VARBITREQ_DESC, ClientScriptOpCode.QUEST_VARBITREQ_MET,
        ClientScriptOpCode.QUEST_ALLREQMET, ClientScriptOpCode.QUEST_STARTED, ClientScriptOpCode.QUEST_FINISHED,
        ClientScriptOpCode.QUEST_PARAM,
    };

    static boolean indexed(ClientScriptOpCode op) {
        return switch (op) {
            case QUEST_QUESTREQ, QUEST_QUESTREQ_MET, QUEST_STATREQ_STAT, QUEST_STATREQ_LEVEL, QUEST_STATREQ_MET,
                QUEST_VARPREQ_DESC, QUEST_VARPREQ_MET, QUEST_VARBITREQ_DESC, QUEST_VARBITREQ_MET -> true;
            default -> false;
        };
    }

    static boolean stateful(ClientScriptOpCode op) {
        return switch (op) {
            case QUEST_QUESTREQ_MET, QUEST_POINTSREQ_MET, QUEST_STATREQ_MET, QUEST_VARPREQ_MET, QUEST_VARBITREQ_MET,
                QUEST_ALLREQMET, QUEST_STARTED, QUEST_FINISHED -> true;
            default -> false;
        };
    }

    static final class Bytes {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Bytes b(int value) {
            stream.write(value);
            return this;
        }

        Bytes s(int value) {
            return b(value >> 8).b(value);
        }

        Bytes m(int value) {
            return b(value >> 16).b(value >> 8).b(value);
        }

        Bytes i(int value) {
            return b(value >> 24).b(value >> 16).b(value >> 8).b(value);
        }

        Bytes text(String value) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                b(c == '€' ? 0x80 : c == 'Œ' ? 0x8c : c == '™' ? 0x99 : c);
            }
            return b(0);
        }

        Bytes jag(String value) {
            return b(0).text(value);
        }

        Bytes bigSmart(int value) {
            if (value < 0 || value >= 32767) return i(value | 0x80000000);
            return s(value);
        }

        byte[] done() {
            return stream.toByteArray();
        }
    }

    static final String[] NAMES = {"Cook's Assistant", "Dragon Slayer", "Café € Quest", "", "Rune Mysteries", "X", "Œuvre ™", "Zed", "alpha", "Beta"};

    static byte[] quest(Random r, int index, int count, int mode) {
        var o = new Bytes();
        var order = new ArrayList<Integer>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15, 17, 18, 19, 249, 11, 16, 20));
        Collections.shuffle(order, r);
        for (int op : order) {
            if (mode == 3 && op != 1) continue;
            if (r.nextInt(100) < 25 && op != 1) continue;
            switch (op) {
                case 1 -> {
                    if (r.nextInt(8) != 0) o.b(1).jag(NAMES[(index + r.nextInt(3)) % NAMES.length]);
                }
                case 2 -> o.b(2).jag(NAMES[r.nextInt(NAMES.length)]);
                case 3, 4 -> {
                    int n = r.nextInt(4);
                    o.b(op).b(n);
                    for (int i = 0; i < n; i++) {
                        int id = op == 3 ? VARPS[r.nextInt(VARPS.length)] : VARBITS[r.nextInt(VARBITS.length)][0];
                        int start = r.nextInt(6) - 1;
                        o.s(id).i(start).i(start + 1 + r.nextInt(4));
                    }
                }
                case 5 -> o.b(5).s(r.nextInt(65536));
                case 6 -> o.b(6).b(r.nextInt(256));
                case 7 -> o.b(7).b(r.nextInt(256));
                case 8 -> o.b(8);
                case 9 -> o.b(9).b(r.nextInt(256));
                case 10 -> {
                    int n = r.nextInt(3);
                    o.b(10).b(n);
                    for (int i = 0; i < n; i++) o.i(r.nextInt());
                }
                case 12 -> o.b(12).i(r.nextInt());
                case 13 -> {
                    int n = r.nextInt(4);
                    o.b(13).b(n);
                    for (int i = 0; i < n; i++) o.s(mode == 2 && r.nextInt(3) == 0 ? count + r.nextInt(4) : r.nextInt(count));
                }
                case 14 -> {
                    int n = r.nextInt(4);
                    o.b(14).b(n);
                    for (int i = 0; i < n; i++) o.b(mode == 2 && r.nextInt(4) == 0 ? 25 + r.nextInt(3) : r.nextInt(25)).b(1 + r.nextInt(99));
                }
                case 15 -> o.b(15).s(r.nextInt(5) == 0 ? r.nextInt(65536) : r.nextInt(12));
                case 17 -> o.b(17).bigSmart(r.nextInt(5) == 0 ? 40000 + r.nextInt(5) : r.nextInt(4));
                case 18, 19 -> {
                    int n = r.nextInt(4);
                    o.b(op).b(n);
                    for (int i = 0; i < n; i++) {
                        int id;
                        if (op == 18) id = mode == 2 && r.nextInt(5) == 0 ? (r.nextBoolean() ? 70000 : -3) : VARPS[r.nextInt(VARPS.length)];
                        else id = VARBITS[r.nextInt(VARBITS.length)][0];
                        int min = r.nextInt(7) - 2;
                        int max = r.nextInt(4) == 0 ? min - 1 - r.nextInt(3) : min + r.nextInt(5);
                        o.i(id).i(min).i(max).text(NAMES[r.nextInt(NAMES.length)]);
                    }
                }
                case 249 -> {
                    int n = r.nextInt(5);
                    o.b(249).b(n);
                    var used = new HashSet<Integer>();
                    for (int i = 0; i < n; i++) {
                        int key = PARAM_QUERY[r.nextInt(PARAM_QUERY.length)];
                        if (!used.add(key)) key = 100 + i;
                        boolean text = r.nextBoolean();
                        o.b(text ? 1 : 0).m(key);
                        if (text) o.text(NAMES[r.nextInt(NAMES.length)]);
                        else o.i(r.nextInt(5) == 0 ? r.nextInt() : r.nextInt(100) - 20);
                    }
                }
                case 11, 16, 20 -> o.b(op);
                default -> throw new AssertionError(op);
            }
        }
        return o.b(0).done();
    }

    static byte[][] files(long seed, int count, int mode) {
        var r = new Random(seed);
        var files = new byte[count][];
        for (int i = 0; i < count; i++) files[i] = mode == 4 ? new byte[] {0} : quest(r, i, count, mode);
        return files;
    }

    static Js5ResourceProvider provider(byte[][] files) throws Exception {
        var unsafe = FontFixtures.unsafe();
        var provider = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        var index = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        var counts = new int[10000];
        var archiveFiles = new Object[10000][];
        counts[QUEST_ARCHIVE] = files.length;
        archiveFiles[QUEST_ARCHIVE] = new Object[files.length];
        for (int i = 0; i < files.length; i++) archiveFiles[QUEST_ARCHIVE][i] = files[i];
        index.fileCounts = counts;
        provider.masterIndex = index;
        provider.archiveFiles = archiveFiles;
        return provider;
    }

    static final String[][] PARAMS = {
        {"1", "i", "7", null}, {"2", "s", "0", "dflt2"}, {"3", "i", "-5", null},
        {"4", "s", "0", "déf€4"}, {"5", "s", "0", ""}, {"6", "", "0", null},
    };

    static PlayerVarDomain install() throws Exception {
        var domain = NativeFixtures.variableDomain();
        for (int[] v : VARBITS) NativeFixtures.variableDefinition(v[0], v[1], v[2], v[3]);
        var params = new ParamTypeList(null);
        for (var spec : PARAMS) {
            var type = new ParamType();
            type.typeChar = spec[1].isEmpty() ? (char) 0 : spec[1].charAt(0);
            type.defaultInt = Integer.parseInt(spec[2]);
            type.typeName = spec[3];
            params.cache.put(type, Integer.parseInt(spec[0]));
        }
        TypeLists.PARAMS = params;
        var unsafe = FontFixtures.unsafe();
        var skillField = Client.class.getDeclaredField("SKILL_LEVEL_ACTUAL");
        unsafe.putObject(unsafe.staticFieldBase(skillField), unsafe.staticFieldOffset(skillField), new int[25]);
        return domain;
    }

    static String params() {
        var joiner = new StringJoiner(";");
        for (var spec : PARAMS) {
            joiner.add(spec[0] + ":" + (spec[1].equals("s") ? 1 : 0) + ":" + spec[2] + ":" + NativeFixtures.utf16(spec[3] == null ? "-" : spec[3]));
        }
        return joiner.toString();
    }

    static String varbits() {
        var joiner = new StringJoiner(";");
        for (int[] v : VARBITS) joiner.add(v[0] + ":" + v[1] + ":" + v[2] + ":" + v[3]);
        return joiner.toString();
    }

    static int[] randomSkills(Random r) {
        var skills = new int[25];
        for (int i = 0; i < 25; i++) skills[i] = r.nextInt(5) == 0 ? r.nextInt(130) - 10 : 1 + r.nextInt(99);
        return skills;
    }

    static String run(PlayerVarDomain domain, ClientScriptOpCode op, int... ints) {
        var exec = new CS2Executor();
        Arrays.fill(exec.intStack, 0);
        Arrays.fill(exec.stringStack, null);
        System.arraycopy(ints, 0, exec.intStack, 0, ints.length);
        exec.intStackPtr = ints.length;
        exec.stringStackPtr = 0;
        exec.longStackPtr = 0;
        String result = "ok";
        try {
            QuestOps.dispatch(op, exec);
        } catch (RuntimeException error) {
            result = "error:" + error.getClass().getSimpleName();
        }
        counts.merge(op.name(), 1, Integer::sum);
        if (!result.equals("ok")) errors.merge(op.name() + ":" + result, 1, Integer::sum);
        total++;
        if (!result.equals("ok")) return op.name() + "\t" + NativeFixtures.integers(ints) + "\t" + result + "\t-\t-\n";
        var strings = new String[exec.stringStackPtr];
        for (int i = 0; i < strings.length; i++) strings[i] = (String) exec.stringStack[i];
        return op.name() + "\t" + NativeFixtures.integers(ints) + "\tok\t"
            + NativeFixtures.integers(Arrays.copyOf(exec.intStack, exec.intStackPtr)) + "\t" + NativeFixtures.texts(strings) + "\n";
    }

    static void profile(PlayerVarDomain domain, String name, long seed, int count, int mode, int states) throws Exception {
        var files = files(seed, count, mode);
        TypeLists.QUEST = new QuestTypeList(provider(files));
        var hex = new StringJoiner(",");
        for (byte[] file : files) hex.add("x" + HexFormat.of().formatHex(file));
        out.append("P\t").append(name).append('\t').append(hex).append('\t').append(varbits()).append('\t').append(params()).append('\n');
        var r = new Random(seed * 31 + 7);
        var ids = new ArrayList<Integer>();
        ids.add(-1);
        for (int i = 0; i < count; i++) ids.add(i);
        ids.add(count);
        ids.add(count + 5);
        ids.add(Integer.MIN_VALUE);
        int[] indexes = {-1, 0, 1, 2, 4, 100, Integer.MAX_VALUE};
        for (int state = 0; state < states; state++) {
            var varps = new StringJoiner(",");
            Arrays.fill(domain.activeVars, 0);
            for (int varp : VARPS) {
                int value = r.nextInt(4) == 0 ? r.nextInt() : r.nextInt(8);
                if (state == 0 && r.nextBoolean()) value = 0;
                domain.activeVars[varp] = value;
                varps.add(varp + "=" + value);
            }
            var skills = randomSkills(r);
            System.arraycopy(skills, 0, Client.SKILL_LEVEL_ACTUAL, 0, 25);
            out.append("S\t").append(varps).append('\t').append(NativeFixtures.integers(skills)).append('\n');
            for (var op : OPS) {
                if (state > 0 && !stateful(op)) continue;
                if (op == ClientScriptOpCode.QUEST_PARAM) {
                    if (state > 0) continue;
                    for (int id : ids) for (int param : PARAM_QUERY) out.append("C\t").append(run(domain, op, id, param));
                } else if (indexed(op)) {
                    for (int id : ids) for (int index : indexes) {
                        if (state > 0 && id != -1 && r.nextInt(3) != 0) continue;
                        out.append("C\t").append(run(domain, op, id, index));
                    }
                } else {
                    for (int id : ids) out.append("C\t").append(run(domain, op, id));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var domain = install();
        profile(domain, "plain-a", 1, 10, 0, 4);
        profile(domain, "plain-b", 2, 16, 1, 4);
        profile(domain, "faulty", 3, 8, 2, 4);
        profile(domain, "names-only", 4, 6, 3, 2);
        profile(domain, "empty", 5, 3, 4, 2);
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("quest.native.tsv"), out);
        System.out.println("Native quest cases " + counts);
        System.out.println("Native quest error classes " + errors);
        System.out.println("Generated " + total + " actual native quest dispatch cases");
        System.out.flush();
        Runtime.getRuntime().halt(0);
    }
}
