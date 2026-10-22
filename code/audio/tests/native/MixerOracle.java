import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.midi.MixedAudioBuffer;
import com.jagex.game.runetek5.sound.Resampler;
import com.jagex.game.runetek5.sound.jagtheora.MixedAudioStream;
import com.jagex.game.runetek5.sound.jagtheora.PcmStream;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.io.BufferedWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public final class MixerOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final int MIX_SIZE = 128;

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 16);
    }

    private static byte[] ints(int[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4);
        for (int value : values) {
            buffer.putInt(value);
        }
        return buffer.array();
    }

    private static byte[] shorts(short[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 2);
        for (short value : values) {
            buffer.putShort(value);
        }
        return buffer.array();
    }

    private static String state(MixedAudioStream s, int[] mix, boolean filled) throws Exception {
        String priority;
        try {
            priority = Integer.toString(s.getPriority());
        } catch (Throwable error) {
            priority = "E";
        }
        return s.playbackPosition + "," + s.sampleRate + "," + s.currentVolume + "," + s.leftChannelVolume + ","
            + s.rightChannelVolume + "," + s.fadeSamplesRemaining + "," + s.loopsRemaining + "," + s.targetVolume + ","
            + s.panPosition + "," + s.volumeFadeDelta + "," + s.leftVolumeFadeDelta + "," + s.rightVolumeFadeDelta + ","
            + s.start + "," + s.end + "," + (s.linked() ? 1 : 0) + "," + priority + "," + (s.isFinished() ? 1 : 0) + ","
            + (s.isFading() ? 1 : 0) + "," + s.getActiveVoiceCount() + "," + s.getAbsolutePlaybackRate() + ","
            + s.getTargetVolume() + "," + s.getPanPosition() + (filled ? "," + digest(ints(mix)) : "");
    }

    private static int[] parse(String text) {
        if (text.isEmpty()) {
            return new int[0];
        }
        String[] parts = text.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i]);
        }
        return out;
    }

    private static String mixer(String[] f) throws Exception {
        String id = f[1];
        PcmPlayerManager.stereo = f[2].equals("1");
        byte[] samples = HexFormat.of().parseHex(f[6]);
        MixedAudioBuffer buffer = new MixedAudioBuffer(samples, Integer.parseInt(f[4]), Integer.parseInt(f[5]));
        buffer.enableLoop = f[3].equals("1");
        MixedAudioStream stream = new MixedAudioStream(buffer, Integer.parseInt(f[7]), Integer.parseInt(f[8]), Integer.parseInt(f[9]));
        Node<PcmStream> head = new Node<>();
        Node<PcmStream> tail = new Node<>();
        stream.previous = head;
        stream.next = tail;
        head.next = stream;
        tail.previous = stream;
        int[] mix = new int[MIX_SIZE];
        StringBuilder all = new StringBuilder();
        String last = state(stream, mix, false);
        all.append(last).append('|');
        String[] ops = f[10].isEmpty() ? new String[0] : f[10].split(";");
        for (int index = 0; index < ops.length; index++) {
            String op = ops[index];
            char code = op.charAt(0);
            int[] a = parse(op.substring(1));
            boolean filled = false;
            try {
                switch (code) {
                    case 'F':
                        java.util.Arrays.fill(mix, 0);
                        stream.fill(mix, a[0], a[1]);
                        filled = true;
                        break;
                    case 'K':
                        stream.skip(a[0]);
                        break;
                    case 'D':
                        stream.fadeToVolume(a[0], a[1]);
                        break;
                    case 'P':
                        stream.fadeToVolumeAndPan(a[0], a[1], a[2]);
                        break;
                    case 'O':
                        stream.fadeOutAndStop(a[0]);
                        break;
                    case 'V':
                        stream.setVolumeWithShift(a[0]);
                        break;
                    case 'N':
                        stream.setPanPosition(a[0]);
                        break;
                    case 'L':
                        stream.setNumLoops(a[0]);
                        break;
                    case 'T':
                        stream.setPlaybackRate(a[0]);
                        break;
                    case 'B':
                        stream.reversePlaybackDirection();
                        break;
                    case 'S':
                        stream.setPlaybackPosition(a[0]);
                        break;
                    case 'U':
                        stream.muteVolume();
                        break;
                    case 'C':
                        stream.cancelFade();
                        break;
                    case 'W':
                        stream.setVolumeAndPan(a[0], a[1]);
                        break;
                    case 'X':
                        stream.updateFadeState();
                        break;
                    case 'Y':
                        stream.recalculateChannelVolumes();
                        break;
                    default:
                        throw new IllegalStateException("op");
                }
            } catch (Throwable error) {
                return "M\t" + id + "\terror\t" + index;
            }
            last = state(stream, mix, filled);
            all.append(last).append('|');
        }
        return "M\t" + id + "\t" + digest(all.toString().getBytes(StandardCharsets.US_ASCII)) + "\t" + last;
    }

    private static String resampler(String[] f) throws Exception {
        String id = f[1];
        int in = Integer.parseInt(f[2]);
        int out = Integer.parseInt(f[3]);
        byte[] samples = HexFormat.of().parseHex(f[6]);
        try {
            Resampler resampler = new Resampler(in, out);
            MixedAudioBuffer source = new MixedAudioBuffer(samples, Integer.parseInt(f[4]), Integer.parseInt(f[5]));
            source.sampleRate = Integer.parseInt(f[7]);
            short[] wide = new short[samples.length];
            for (int i = 0; i < samples.length; i++) {
                wide[i] = (short) (samples[i] * 257);
            }
            short[] narrow = resampler.resample(wide);
            int rate = resampler.scaleRate(12345);
            int position = resampler.scalePosition(777);
            MixedAudioBuffer result = source.resample(resampler);
            return "R\t" + id + "\t" + digest(result.samples) + "\t" + result.samples.length + "\t" + result.start + "\t"
                + result.end + "\t" + result.sampleRate + "\t" + source.sampleRate + "\t" + source.start + "\t" + source.end
                + "\t" + digest(shorts(narrow)) + "\t" + narrow.length + "\t" + rate + "\t" + position;
        } catch (Throwable error) {
            return "R\t" + id + "\terror";
        }
    }

    private static String attenuation(String[] f) {
        int volume = Integer.parseInt(f[2]);
        int pan = Integer.parseInt(f[3]);
        return "A\t" + f[1] + "\t" + StaticMethods.calculateSoundAttenuation(volume, pan) + "\t"
            + StaticMethods.calculateSoundPan(volume, pan);
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
                String[] f = line.split("\t", -1);
                String result;
                if (f[0].equals("M")) {
                    result = mixer(f);
                } else if (f[0].equals("R")) {
                    result = resampler(f);
                } else {
                    result = attenuation(f);
                }
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
