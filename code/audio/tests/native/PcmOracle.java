import com.jagex.game.runetek5.config.midi.MixedAudioBuffer;
import com.jagex.game.runetek5.sound.jagtheora.MixedAudioStream;
import com.jagex.game.runetek5.sound.jagtheora.PcmStreamMixer;
import com.jagex.game.runetek5.sound.player.PcmPlayer;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class PcmOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 16);
    }

    private static int clip(int value) {
        int data = value;
        if ((data + 8388608 & ~0xffffff) != 0) {
            data = 0x7fffff ^ (data >> 31);
        }
        return data >> 8;
    }

    private static MixedAudioStream voice(String text) {
        String[] p = text.split(",", -1);
        MixedAudioBuffer buffer = new MixedAudioBuffer(HexFormat.of().parseHex(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        buffer.enableLoop = p[3].equals("1");
        MixedAudioStream stream = new MixedAudioStream(buffer, Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]));
        stream.setNumLoops(Integer.parseInt(p[7]));
        return stream;
    }

    private static String flags(List<MixedAudioStream> all) {
        StringBuilder out = new StringBuilder();
        for (MixedAudioStream stream : all) {
            out.append(stream.linked() ? (stream.fill ? '1' : '0') : '-');
        }
        return out.toString();
    }

    private static String run(String[] f) throws Exception {
        String id = f[1];
        boolean stereo = f[2].equals("1");
        PcmPlayerManager.sampleRate = 22050;
        PcmPlayerManager.stereo = stereo;
        PcmStreamMixer mixer = new PcmStreamMixer();
        PcmPlayer player = new PcmPlayer();
        player.setCurrentPcmStream(mixer);
        List<MixedAudioStream> all = new ArrayList<>();
        int[] block = new int[stereo ? 512 : 256];
        StringBuilder log = new StringBuilder();
        String[] voices = f[3].isEmpty() ? new String[0] : f[3].split("/");
        String[] ops = f[4].isEmpty() ? new String[0] : f[4].split(";");
        int index = 0;
        try {
            for (String v : voices) {
                MixedAudioStream stream = voice(v);
                all.add(stream);
                mixer.addSubStream(stream);
            }
            for (; index < ops.length; index++) {
                String op = ops[index];
                String arg = op.substring(1);
                switch (op.charAt(0)) {
                    case 'R': {
                        int blocks = Integer.parseInt(arg);
                        for (int b = 0; b < blocks; b++) {
                            player.fillSoundBuffer(block);
                            byte[] pcm = new byte[block.length * 2];
                            for (int i = 0; i < block.length; i++) {
                                int c = clip(block[i]);
                                pcm[i * 2] = (byte) c;
                                pcm[i * 2 + 1] = (byte) (c >> 8);
                            }
                            log.append(digest(pcm)).append(':').append(flags(all)).append('|');
                        }
                        break;
                    }
                    case 'K':
                        player.skipAudioSamples();
                        log.append("K:").append(flags(all)).append('|');
                        break;
                    case 'V': {
                        String[] a = arg.split(",");
                        all.get(Integer.parseInt(a[0])).setVolumeWithShift(Integer.parseInt(a[1]));
                        break;
                    }
                    case 'N': {
                        String[] a = arg.split(",");
                        all.get(Integer.parseInt(a[0])).setPanPosition(Integer.parseInt(a[1]));
                        break;
                    }
                    case 'O': {
                        String[] a = arg.split(",");
                        all.get(Integer.parseInt(a[0])).fadeOutAndStop(Integer.parseInt(a[1]));
                        break;
                    }
                    case 'X': {
                        mixer.removeSubStream(all.get(Integer.parseInt(arg)));
                        break;
                    }
                    case 'A': {
                        MixedAudioStream stream = voice(arg);
                        all.add(stream);
                        mixer.addSubStream(stream);
                        break;
                    }
                    default:
                        throw new IllegalStateException("op");
                }
            }
        } catch (Throwable error) {
            return "P\t" + id + "\terror\t" + index;
        }
        return "P\t" + id + "\t" + digest(log.toString().getBytes(StandardCharsets.US_ASCII)) + "\t" + flags(all);
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        if (!hash(jar).equals(PINNED)) {
            throw new IllegalStateException("jar does not match the pinned oracle sha256");
        }
        List<String> cases = Files.readAllLines(Path.of(args[1]), StandardCharsets.US_ASCII);
        int errors = 0;
        int total = 0;
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[2]), StandardCharsets.US_ASCII)) {
            for (String line : cases) {
                if (line.isEmpty()) {
                    continue;
                }
                String result = run(line.split("\t", -1));
                if (result.contains("\terror")) {
                    errors++;
                }
                total++;
                out.write(result);
                out.write("\n");
            }
        }
        System.out.println("rows=" + total + " errors=" + errors);
    }
}
