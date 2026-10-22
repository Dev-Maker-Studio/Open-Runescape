import com.jagex.game.runetek5.buffer.DataBuffer;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.quickchat.QuickchatCategoryTypeList;
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.QuickChatDisplayOps;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class QuickChatFixtures {
    static final byte[] HOLE = {0};

    static final class W {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        W text(int opcode, String value) {
            out.write(opcode);
            out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
            out.write(0);
            return this;
        }

        W list(int opcode, int[]... entries) {
            out.write(opcode);
            out.write(entries.length);
            for (int[] entry : entries) {
                out.write(entry[0] >> 8);
                out.write(entry[0]);
                out.write(entry[1]);
            }
            return this;
        }

        W flag(int opcode) {
            out.write(opcode);
            return this;
        }

        W shorts(int opcode, int... values) {
            out.write(opcode);
            out.write(values.length);
            for (int value : values) {
                out.write(value >> 8);
                out.write(value);
            }
            return this;
        }

        W types(int[]... entries) {
            out.write(3);
            out.write(entries.length);
            for (int[] entry : entries) {
                for (int value : entry) {
                    out.write(value >> 8);
                    out.write(value);
                }
            }
            return this;
        }

        byte[] done() {
            out.write(0);
            return out.toByteArray();
        }
    }

    static int[] k(int id, int hotkey) { return new int[] {id, hotkey}; }

    static byte[][] messageCategories() {
        return new byte[][] {
            new W().text(1, "Root").list(2, k(1, 'a'), k(2, 'B'), k(3, 0)).list(3, k(0, 'x'), k(1, 0), k(2, 0xE9), k(3, 0x80)).done(),
            new W().text(1, "Café\u0080").list(3, k(4, 'q')).done(),
            new W().list(2).list(3).done(),
            new W().done(),
            new W().flag(4).text(1, "N\u0081x").list(2, k(5, 0x81)).done(),
            new W().list(3, k(6, 0xFF), k(7, 0x8E), k(8, 0x9D)).done(),
            HOLE,
        };
    }

    static byte[][] menuCategories() {
        return new byte[][] {
            new W().text(1, "Menu").list(2, k(1, 'm'), k(2, 'n')).list(3, k(0, 'p'), k(1, 'q'), k(2, 0)).done(),
            new W().list(3, k(3, 'z')).done(),
            new W().done(),
        };
    }

    static byte[][] messages() {
        var list = new ArrayList<byte[]>();
        list.add(new W().text(1, "Hello").done());
        list.add(new W().text(1, "Trade <obj>").done());
        list.add(new W().text(1, "Hidden hello").flag(4).done());
        list.add(new W().text(1, "a<b<c").shorts(2, 5, 6).types(new int[] {0, 7}, new int[] {14, 3}).done());
        list.add(new W().text(1, "Pick <x> <y>").shorts(2).types(new int[] {6, 12, 34}, new int[] {99}, new int[] {1}, new int[] {2}).done());
        list.add(new W().done());
        list.add(new W().text(1, "").done());
        list.add(new W().text(1, "HELLO world é\u0080").done());
        list.add(new W().text(1, "<<").done());
        list.add(HOLE);
        for (int id = 10; id < 70; id++) {
            var writer = new W().text(1, "Common phrase " + ((id * 37) % 60));
            if (id == 10) {
                writer.types(new int[] {4, 1}, new int[] {7, 2}, new int[] {8, 3}, new int[] {9, 4}, new int[] {10}, new int[] {11, 5, 6},
                    new int[] {12}, new int[] {13}, new int[] {14, 7}, new int[] {15}, new int[] {0, 65535});
            }
            list.add(writer.done());
        }
        return list.toArray(new byte[0][]);
    }

    static byte[][] menuMessages() {
        return new byte[][] {
            new W().text(1, "Menu one").shorts(2, 1, 2).done(),
            new W().text(1, "menu two").types(new int[] {0, 9}).done(),
            new W().text(1, "Another").done(),
            new W().text(1, "Zed menu").done(),
            new W().text(1, "menu hidden").flag(4).done(),
            HOLE,
        };
    }

    static sun.misc.Unsafe unsafe() throws Exception {
        var field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    static Js5ResourceProvider provider(byte[][] categories, byte[][] messages) throws Exception {
        var unsafe = unsafe();
        var provider = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        var index = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        index.fileCounts = new int[] {categories.length, messages.length};
        index.archiveCount = 2;
        provider.masterIndex = index;
        provider.archives = new Object[2];
        provider.archiveFiles = new Object[2][];
        byte[][][] archives = {categories, messages};
        for (int archive = 0; archive < 2; archive++) {
            var files = new Object[archives[archive].length];
            for (int file = 0; file < files.length; file++) files[file] = DataBuffer.createDataBuffer(archives[archive][file]);
            provider.archiveFiles[archive] = files;
        }
        return provider;
    }

    static void install(Js5ResourceProvider messages, Js5ResourceProvider menus) {
        TypeLists.QUICK_CHAT_CATEGORY = new QuickchatCategoryTypeList(messages, menus);
        TypeLists.QUICK_CHAT_MESSAGE = new QuickchatMessageTypeList(messages, menus, null);
    }

    static String hex(byte[] bytes) { return HexFormat.of().formatHex(bytes); }

    static void definitions(Path output) throws Exception {
        var out = new StringBuilder();
        out.append("counts\t").append(messages().length).append('\t').append(menuMessages().length).append('\n');
        var groups = new Object[][] {
            {0, 'c', messageCategories()}, {0, 'm', messages()}, {1, 'c', menuCategories()}, {1, 'm', menuMessages()}
        };
        for (var group : groups) {
            var files = (byte[][]) group[2];
            for (int file = 0; file < files.length; file++) {
                out.append("def\t").append(group[0]).append('\t').append(group[1]).append('\t').append(file).append('\t').append(hex(files[file])).append('\n');
            }
        }
        Files.writeString(output.resolve("quick-chat.defs.tsv"), out);
    }

    record Case(String name, ClientScriptOpCode opcode, int[] ints, String[] strings) {}

    static final int[] CATEGORIES = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 1000, 32768, 32769, 32770, 32771, 40000, 65536, 65537};
    static final int[] MESSAGES = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 69, 70, 32768, 32769, 32770, 32771, 32772, 32773, 65537};
    static final int[] INDICES = {-1, 0, 1, 2, 3, 4, 100};
    static final int[] KEYS = {-1, 0, 'a', 'B', 'x', 'q', 'm', 0xE9, 0xFF, 0x20AC, 0x17D, 63, 65536 + 'a', 0x81, 0x8E, 65535, 97 + 0x10000 * 5};
    static final int[] TYPES = {-1, 0, 1, 2, 3, 4, 5, 9, 10, 11, 12};
    static final int[] CONFIGS = {-1, 0, 1, 2, 3};
    static final String[] QUERIES = {"hello", "HELLO", "", "common", "phrase 1", "menu", "e", "é", "€", "...", "xyz", "trade ...obj", "Zed"};

    static void add(List<Case> cases, ClientScriptOpCode opcode, String query, int... ints) {
        int[] full = new int[ints.length + 1];
        full[0] = 727;
        System.arraycopy(ints, 0, full, 1, ints.length);
        String[] strings = query == null ? new String[] {"lower-config-string"} : new String[] {"lower-config-string", query};
        cases.add(new Case(opcode.name().toLowerCase() + "-" + cases.size(), opcode, full, strings));
    }

    static List<Case> cases() {
        var cases = new ArrayList<Case>();
        add(cases, ClientScriptOpCode.CHATPHRASE_FINDNEXT, null);
        add(cases, ClientScriptOpCode.CHATPHRASE_FINDRESTART, null);
        add(cases, ClientScriptOpCode.CHATPHRASE_FINDNEXT, null);
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.CHATCAT_GETDESC, ClientScriptOpCode.CHATCAT_GETSUBCATCOUNT,
            ClientScriptOpCode.CHATCAT_GETPHRASECOUNT}) {
            for (int id : CATEGORIES) add(cases, opcode, null, id);
        }
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.CHATCAT_GETSUBCAT, ClientScriptOpCode.CHATCAT_GETPHRASE,
            ClientScriptOpCode.CHATCAT_GETSUBCATSHORTCUT, ClientScriptOpCode.CHATCAT_GETPHRASESHORTCUT}) {
            for (int id : CATEGORIES) for (int index : INDICES) add(cases, opcode, null, id, index);
        }
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.CHATCAT_FINDSUBCATBYSHORTCUT, ClientScriptOpCode.CHATCAT_FINDPHRASEBYSHORTCUT}) {
            for (int id : CATEGORIES) for (int key : KEYS) add(cases, opcode, null, id, key);
        }
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.CHATPHRASE_GETTEXT, ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSECOUNT,
            ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMANDCOUNT}) {
            for (int id : MESSAGES) add(cases, opcode, null, id);
        }
        for (int id : MESSAGES) for (int index : INDICES) add(cases, ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSE, null, id, index);
        for (int id : MESSAGES) for (int type : TYPES) add(cases, ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMAND, null, id, type);
        for (int id : MESSAGES) for (int type : TYPES) for (int config : CONFIGS) {
            add(cases, ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM, null, id, type, config);
        }
        for (var query : QUERIES) {
            for (int menu : new int[] {0, 1, 2}) {
                add(cases, ClientScriptOpCode.CHATPHRASE_FIND, query, menu);
                for (int next = 0; next < 14; next++) add(cases, ClientScriptOpCode.CHATPHRASE_FINDNEXT, null);
                add(cases, ClientScriptOpCode.CHATPHRASE_FINDRESTART, null);
                for (int next = 0; next < 3; next++) add(cases, ClientScriptOpCode.CHATPHRASE_FINDNEXT, null);
            }
        }
        return cases;
    }

    static void nativeCases(Path output) throws Exception {
        var cases = cases();
        var executor = new CS2Executor();
        executor.longStackPtr = 1;
        executor.longStack[0] = 727727727727L;
        executor.intLocals = new int[] {727};
        executor.objectLocals = new Object[] {"config-local"};
        executor.longLocals = new long[] {Long.MIN_VALUE};
        executor.intOpValues = new int[1];
        executor.instrPtr = 0;
        var expected = new StringBuilder();
        var errors = new TreeMap<String, Integer>();
        var counts = new TreeMap<String, Integer>();
        for (var entry : cases) {
            Arrays.fill(executor.intStack, 0);
            Arrays.fill(executor.stringStack, null);
            executor.intStackPtr = entry.ints.length;
            System.arraycopy(entry.ints, 0, executor.intStack, 0, entry.ints.length);
            executor.stringStackPtr = entry.strings.length;
            System.arraycopy(entry.strings, 0, executor.stringStack, 0, entry.strings.length);
            String result = "ok";
            try {
                QuickChatDisplayOps.dispatch(entry.opcode, executor);
            } catch (RuntimeException error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            counts.merge(entry.opcode.name(), 1, Integer::sum);
            if (!result.equals("ok")) errors.merge(entry.opcode.name() + ":" + result, 1, Integer::sum);
            if (executor.longStackPtr != 1 || executor.longStack[0] != 727727727727L || executor.intLocals[0] != 727
                || executor.instrPtr != 0 || executor.intStackPtr < 0 || executor.stringStackPtr < 0
                || !"lower-config-string".equals(executor.stringStack[0]) || executor.intStack[0] != 727) throw new AssertionError(entry.name);
            var strings = new String[executor.stringStackPtr];
            for (int i = 0; i < strings.length; i++) strings[i] = (String) executor.stringStack[i];
            expected.append("T\t").append(entry.name).append('\t').append(entry.opcode.name()).append('\t')
                .append(NativeFixtures.integers(entry.ints)).append('\t').append(NativeFixtures.texts(entry.strings))
                .append('\t').append(result).append('\t')
                .append(NativeFixtures.integers(Arrays.copyOf(executor.intStack, executor.intStackPtr))).append('\t')
                .append(NativeFixtures.texts(strings)).append('\n');
        }
        expected.append("N\t").append(cases.size()).append('\n');
        Files.writeString(output.resolve("quick-chat.native.tsv"), expected);
        System.out.println("Native quick chat cases " + counts);
        System.out.println("Native quick chat error classes " + errors);
        System.out.println("Generated " + cases.size() + " actual native quick chat dispatch cases");
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        definitions(output);
        install(provider(messageCategories(), messages()), provider(menuCategories(), menuMessages()));
        nativeCases(output);
    }
}
