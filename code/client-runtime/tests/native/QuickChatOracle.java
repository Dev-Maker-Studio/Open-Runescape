import com.jagex.game.runetek5.buffer.DataBuffer;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.quickchat.QuickchatCategoryTypeList;
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.QuickChatDisplayOps;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;

public final class QuickChatOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final byte[] HOLE = {0};
    private static final String[] QUERIES = {
        "a", "e", "hello", "please", "the", "you", "i'm", "...", "ing", "?", "", "trade", "<", "zzzz", "quest", "dungeon", "thank", "A", "Hello", "é"
    };
    private static final CS2Executor EXECUTOR = new CS2Executor();

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        var field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    private static byte[][] read(DataInputStream in, int count, TreeSet<Integer> ids) throws Exception {
        byte[][] files = new byte[count][];
        int present = in.readInt();
        for (int i = 0; i < present; i++) {
            int id = in.readInt();
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            files[id] = data;
            if (ids != null) ids.add(id);
        }
        for (int i = 0; i < count; i++) if (files[i] == null) files[i] = HOLE;
        return files;
    }

    private static Js5ResourceProvider provider(byte[][] categories, byte[][] messages) throws Exception {
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

    private static String text(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("u");
        for (int i = 0; i < value.length(); i++) out.append(String.format("%04x", (int) value.charAt(i)));
        return out.toString();
    }

    private static boolean call(ClientScriptOpCode opcode, int[] arguments, String... texts) {
        EXECUTOR.intStackPtr = arguments.length;
        System.arraycopy(arguments, 0, EXECUTOR.intStack, 0, arguments.length);
        EXECUTOR.stringStackPtr = texts.length;
        System.arraycopy(texts, 0, EXECUTOR.stringStack, 0, texts.length);
        try {
            QuickChatDisplayOps.dispatch(opcode, EXECUTOR);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static String integer(ClientScriptOpCode opcode, int... arguments) {
        return call(opcode, arguments) ? Integer.toString(EXECUTOR.intStack[EXECUTOR.intStackPtr - 1]) : "E";
    }

    private static String list(List<String> values) {
        return values.isEmpty() ? "-" : String.join(",", values);
    }

    private static String category(int id) {
        String name = call(ClientScriptOpCode.CHATCAT_GETDESC, new int[] {id}) ? text((String) EXECUTOR.stringStack[EXECUTOR.stringStackPtr - 1]) : "E";
        var line = new StringBuilder("C\t" + id + "\t" + name);
        for (var kind : new String[] {"sub", "phrase"}) {
            boolean sub = kind.equals("sub");
            int count = Integer.parseInt(integer(sub ? ClientScriptOpCode.CHATCAT_GETSUBCATCOUNT : ClientScriptOpCode.CHATCAT_GETPHRASECOUNT, id));
            var entries = new ArrayList<String>();
            var keys = new ArrayList<String>();
            for (int i = 0; i < count; i++) {
                entries.add(integer(sub ? ClientScriptOpCode.CHATCAT_GETSUBCAT : ClientScriptOpCode.CHATCAT_GETPHRASE, id, i));
                keys.add(integer(sub ? ClientScriptOpCode.CHATCAT_GETSUBCATSHORTCUT : ClientScriptOpCode.CHATCAT_GETPHRASESHORTCUT, id, i));
            }
            line.append('\t').append(list(entries)).append('\t').append(list(keys));
        }
        return line.toString();
    }

    private static String message(int id) {
        String text = call(ClientScriptOpCode.CHATPHRASE_GETTEXT, new int[] {id}) ? text((String) EXECUTOR.stringStack[EXECUTOR.stringStackPtr - 1]) : "E";
        var responses = new ArrayList<String>();
        int responseCount = Integer.parseInt(integer(ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSECOUNT, id));
        for (int i = 0; i < responseCount; i++) responses.add(integer(ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSE, id, i));
        var commands = new ArrayList<String>();
        int commandCount = Integer.parseInt(integer(ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMANDCOUNT, id));
        for (int i = 0; i < commandCount; i++) {
            String type = integer(ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMAND, id, i);
            var entry = new StringBuilder(i + ":" + type);
            if (type.equals("0")) {
                for (int config = -1; config <= 4; config++) {
                    entry.append(':').append(integer(ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM, id, i, config));
                }
            }
            commands.add(entry.toString());
        }
        return "M\t" + id + "\t" + text + "\t" + list(responses) + "\t" + list(commands);
    }

    private static String search(String query, int menu) {
        boolean ok = call(ClientScriptOpCode.CHATPHRASE_FIND, new int[] {menu}, query);
        if (!ok) return "S\t" + text(query) + "\t" + menu + "\tE\t-";
        int count = EXECUTOR.intStack[EXECUTOR.intStackPtr - 1];
        var found = new ArrayList<String>();
        for (int i = 0; i < Math.max(count, 0); i++) found.add(integer(ClientScriptOpCode.CHATPHRASE_FINDNEXT));
        return "S\t" + text(query) + "\t" + menu + "\t" + count + "\t" + list(found);
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path bundle = Path.of(args[1]);
        Path output = Path.of(args[2]);
        if (!hash(jar).equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        Path loaded = Path.of(QuickChatDisplayOps.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException("code source mismatch");
        var categoryIds = new TreeSet<Integer>();
        var messageIds = new TreeSet<Integer>();
        Js5ResourceProvider[] providers = new Js5ResourceProvider[2];
        int[] categoryCounts = new int[2];
        int[] messageCounts = new int[2];
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(bundle), 1 << 20))) {
            for (int source = 0; source < 2; source++) {
                categoryCounts[source] = in.readInt();
                messageCounts[source] = in.readInt();
                var categoryTarget = new TreeSet<Integer>();
                var messageTarget = new TreeSet<Integer>();
                byte[][] categories = read(in, categoryCounts[source], categoryTarget);
                byte[][] messages = read(in, messageCounts[source], messageTarget);
                providers[source] = provider(categories, messages);
                int offset = source == 0 ? 0 : 32768;
                for (int id : categoryTarget) categoryIds.add(id + offset);
                for (int id : messageTarget) messageIds.add(id + offset);
            }
        }
        TypeLists.QUICK_CHAT_CATEGORY = new QuickchatCategoryTypeList(providers[0], providers[1]);
        TypeLists.QUICK_CHAT_MESSAGE = new QuickchatMessageTypeList(providers[0], providers[1], null);
        categoryIds.add(-1);
        messageIds.add(-1);
        categoryIds.add(categoryCounts[0]);
        messageIds.add(messageCounts[0]);
        categoryIds.add(32768 + categoryCounts[1]);
        messageIds.add(32768 + messageCounts[1]);
        int lines = 0;
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output), 1 << 20), false, StandardCharsets.US_ASCII)) {
            for (int id : categoryIds) {
                out.println(category(id));
                lines++;
            }
            for (int id : messageIds) {
                out.println(message(id));
                lines++;
            }
            for (var query : QUERIES) {
                for (int menu = 0; menu < 3; menu++) {
                    out.println(search(query, menu));
                    lines++;
                }
            }
        }
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("QUICK_CHAT_ORACLE ok lines=" + lines + " categories=" + categoryIds.size() + " messages=" + messageIds.size());
    }
}
