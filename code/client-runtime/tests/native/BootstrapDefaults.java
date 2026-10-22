import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.systeminfo.ProcessorSpecs;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import java.util.Arrays;
import java.util.HexFormat;

public final class BootstrapDefaults {
    private static String quoted(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char value : text.toCharArray()) {
            if (value == '"' || value == '\\') out.append('\\').append(value);
            else if (value < 32) out.append(String.format("\\u%04x", (int) value));
            else out.append(value);
        }
        return out.append('"').toString();
    }

    private static String parameter(String id) {
        String value = Loader.clientParams.getProperty(id);
        if (value == null) throw new IllegalStateException("missing public Loader parameter " + id);
        return value;
    }

    private static String loaderDefaults() {
        Loader.IP_ADDRESS = "offline.invalid";
        Loader.loadParams();
        return "{\"affiliateP\":" + quoted(parameter("33"))
            + ",\"clientKey\":" + quoted(parameter("13"))
            + ",\"JS5token\":" + quoted(parameter("17"))
            + ",\"sessionToken\":" + Integer.parseInt(parameter("27"))
            + ",\"game\":" + Integer.parseInt(parameter("8"))
            + ",\"language\":" + Integer.parseInt(parameter("31")) + "}";
    }

    private static byte[] encode(GamePreferences preferences) {
        Packet packet = preferences.encode();
        if (packet.currentPosition != 35) {
            throw new IllegalStateException("native preferences length " + packet.currentPosition);
        }
        return Arrays.copyOf(packet.buffer, packet.currentPosition);
    }

    private static String preferences(int memory, int processors, String architecture, boolean arm) {
        GameShell.maxMemory = memory;
        GameShell.availableProcessors = processors;
        SystemInfo.OS_ARCH = architecture;
        GamePreferences preferences = new GamePreferences(Game.RUNESCAPE);
        ProcessorSpecs specs = preferences.getProcessorSpecs();
        if (specs.getMaxMemory() != memory || specs.getProcessorCount() != processors || specs.isARM() != arm) {
            throw new IllegalStateException("native hardware assumptions mismatch");
        }
        byte[] fresh = encode(preferences);
        Packet input = new Packet(fresh.clone());
        byte[] roundTrip = encode(new GamePreferences(input, Game.RUNESCAPE));
        if (input.currentPosition != fresh.length) {
            throw new IllegalStateException("native preferences decode did not consume payload");
        }
        if (!Arrays.equals(fresh, roundTrip)) {
            throw new IllegalStateException("native preferences round trip mismatch");
        }
        return "{\"input\":{\"game\":\"RUNESCAPE\",\"gameId\":" + Game.RUNESCAPE.id
            + ",\"maxMemory\":" + memory + ",\"availableProcessors\":" + processors
            + ",\"osArch\":" + quoted(architecture) + "},\"processorSpecs\":{\"maxMemory\":"
            + specs.getMaxMemory() + ",\"processorCount\":" + specs.getProcessorCount()
            + ",\"isArm\":" + specs.isARM() + "},\"length\":" + fresh.length
            + ",\"hex\":" + quoted(HexFormat.of().formatHex(fresh))
            + ",\"roundTripHex\":" + quoted(HexFormat.of().formatHex(roundTrip))
            + ",\"roundTripEqual\":true}";
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments expected");
        String defaults = preferences(245, 2, "amd64", false);
        String memory244 = preferences(244, 2, "amd64", false);
        String processors1 = preferences(245, 1, "amd64", false);
        String arm = preferences(245, 2, "arm", true);
        System.out.println("{\"loaderDefaults\":" + loaderDefaults() + ",\"defaults\":" + defaults
            + ",\"variants\":{\"memory244\":" + memory244 + ",\"processors1\":" + processors1
            + ",\"arm\":" + arm + "}}");
    }
}
