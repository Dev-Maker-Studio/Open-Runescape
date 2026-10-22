import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

public final class InspectScript {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("script path required");
        var path = Path.of(args[0]);
        if (Files.size(path) > 1024 * 1024) throw new IllegalArgumentException("script byte limit");
        var script = CS2Script.decodeScript(Files.readAllBytes(path));
        var opcodes = new TreeMap<String, Integer>();
        for (var opcode : script.operations) opcodes.merge(opcode.name(), 1, Integer::sum);
        var result = new StringBuilder("{");
        result.append("\"instructions\":").append(script.operations.length);
        result.append(",\"localInts\":").append(script.intLocalsCount);
        result.append(",\"localStrings\":").append(script.stringLocalsCount);
        result.append(",\"localLongs\":").append(script.longLocalsCount);
        result.append(",\"argumentInts\":").append(script.intArgsCount);
        result.append(",\"argumentStrings\":").append(script.stringArgsCount);
        result.append(",\"argumentLongs\":").append(script.longArgsCount);
        result.append(",\"switchTables\":").append(script.switchMaps == null ? 0 : script.switchMaps.length);
        result.append(",\"opcodes\":{");
        boolean first = true;
        for (var entry : opcodes.entrySet()) {
            if (!first) result.append(',');
            first = false;
            result.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        result.append("},\"calls\":[");
        var calls = new java.util.TreeSet<Integer>();
        for (int i = 0; i < script.operations.length; i++) {
            if (script.operations[i].name().equals("GOSUB_WITH_PARAMS")) calls.add(script.intOpValues[i]);
        }
        first = true;
        for (int id : calls) {
            if (!first) result.append(',');
            first = false;
            result.append(id);
        }
        result.append("]}");
        System.out.println(result);
    }
}
