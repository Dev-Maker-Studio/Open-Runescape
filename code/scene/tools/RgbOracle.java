import com.jagex.game.runetek5.utils.ColorUtil;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import sun.misc.Unsafe;

public final class RgbOracle {
    static final String COLOR = ColorUtil.class.getName();
    static final String COLOR_PATH = COLOR.replace('.', '/') + ".class";

    static final class BoundaryRandom extends Random {
        final double value;
        int calls;

        BoundaryRandom(double value) {
            super(0);
            this.value = value;
        }

        @Override
        public double nextDouble() {
            calls++;
            return value;
        }
    }

    static String digest(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    static byte[] classBytes() throws Exception {
        try (InputStream input = RgbOracle.class.getClassLoader().getResourceAsStream(COLOR_PATH)) {
            if (input == null) {
                throw new IllegalStateException("missing native ColorUtil class");
            }
            return input.readAllBytes();
        }
    }

    static byte[] canonical(byte[] bytes) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(writer, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    static byte[] tableBytes(int[] rgb, int[] unsignedRgb) {
        if (rgb.length != 65536 || unsignedRgb.length != 65536) {
            throw new IllegalStateException("native HSL table length differs");
        }
        ByteBuffer output = ByteBuffer.allocate(2 * 65536 * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int value : rgb) {
            output.putInt(value);
        }
        for (int value : unsignedRgb) {
            output.putInt(value);
        }
        return output.array();
    }

    static void capture(Class<?> color, StringBuilder metadata, Path output) throws Exception {
        Method calculate = color.getMethod("calculateHslToRgbTable");
        Method rgb = color.getMethod("getHslToRgb");
        Method unsignedRgb = color.getMethod("getUnsignedHslToRgb");
        if (rgb.invoke(null) != null || unsignedRgb.invoke(null) != null) {
            throw new IllegalStateException("fresh JVM native table was already allocated");
        }
        calculate.invoke(null);
        int[] first = (int[]) rgb.invoke(null);
        int[] second = (int[]) unsignedRgb.invoke(null);
        calculate.invoke(null);
        if (first != rgb.invoke(null) || second != unsignedRgb.invoke(null)) {
            throw new IllegalStateException("native cached table was reallocated");
        }
        byte[] tables = tableBytes(first, second);
        metadata.append("table-sha256\t").append(digest(tables)).append('\n');
        metadata.append("table-entries\t").append(first.length + second.length).append('\n');
        Files.write(output, tables);
    }

    static void replaceRandom(Field target, Random random) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        unsafe.putObject(unsafe.staticFieldBase(target), unsafe.staticFieldOffset(target), random);
        if (target.get(null) != random) {
            throw new IllegalStateException("Math random holder seam did not install");
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        String mode = args[1];
        String parameter = args[2];
        Path tables = Path.of(args[3]);
        Path metadataPath = Path.of(args[4]);
        StringBuilder metadata = new StringBuilder();
        byte[] original = classBytes();
        metadata.append("mode\t").append(mode).append('\n');
        metadata.append("parameter\t").append(parameter).append('\n');
        metadata.append("jar-sha256\t").append(digest(Files.readAllBytes(jar))).append('\n');
        metadata.append("class-sha256\t").append(digest(original)).append('\n');
        metadata.append("canonical-class-sha256\t").append(digest(canonical(original))).append('\n');
        metadata.append("java-runtime\t").append(System.getProperty("java.runtime.version")).append('\n');
        metadata.append("native-class-unchanged\ttrue\n");
        Class<?> color = Class.forName(COLOR);
        Class<?> holder = Class.forName("java.lang.Math$RandomNumberGeneratorHolder");
        Field field = holder.getDeclaredField("randomNumberGenerator");
        field.setAccessible(true);
        if (mode.equals("seed")) {
            Random random = (Random) field.get(null);
            long seed = Long.parseLong(parameter);
            random.setSeed(seed);
            double unit = random.nextDouble();
            double nextUnit = random.nextDouble();
            random.setSeed(seed);
            metadata.append("random-unit-bits\t").append(HexFormat.of().toHexDigits(Double.doubleToRawLongBits(unit))).append('\n');
            metadata.append("gamma-bits\t").append(HexFormat.of().toHexDigits(Double.doubleToRawLongBits(0.7 + (unit * 0.03 - 0.015)))).append('\n');
            capture(color, metadata, tables);
            if (Double.doubleToRawLongBits(random.nextDouble()) != Double.doubleToRawLongBits(nextUnit)) {
                throw new IllegalStateException("native allocation did not draw Math.random exactly once");
            }
            metadata.append("allocation-random-calls\t1\n");
        } else if (mode.equals("boundary")) {
            double boundary = Double.longBitsToDouble(Long.parseUnsignedLong(parameter, 16));
            if (!Double.isFinite(boundary) || boundary < 0.0 || boundary >= 1.0) {
                throw new IllegalArgumentException("invalid boundary random unit");
            }
            if (!System.getProperty("java.vm.info").contains("interpreted")) {
                throw new IllegalStateException("boundary RNG holder seam requires a fresh -Xint JVM");
            }
            BoundaryRandom random = new BoundaryRandom(boundary);
            replaceRandom(field, random);
            metadata.append("random-unit-bits\t").append(parameter).append('\n');
            metadata.append("gamma-bits\t").append(HexFormat.of().toHexDigits(Double.doubleToRawLongBits(0.7 + (boundary * 0.03 - 0.015)))).append('\n');
            metadata.append("random-holder-seam\tUnsafe replaces Math$RandomNumberGeneratorHolder.randomNumberGenerator only\n");
            metadata.append("interpreted-jvm\ttrue\n");
            capture(color, metadata, tables);
            if (random.calls != 1) {
                throw new IllegalStateException("boundary table did not draw random seam exactly once");
            }
            metadata.append("allocation-random-calls\t").append(random.calls).append('\n');
        } else {
            throw new IllegalArgumentException("unknown RGB oracle mode " + mode);
        }
        if (!MessageDigest.isEqual(original, classBytes())) {
            throw new IllegalStateException("native ColorUtil class bytes changed");
        }
        Files.writeString(metadataPath, metadata);
        System.out.println("NATIVE_RGB_ORACLE_OK " + mode + " " + parameter + " entries=131072");
    }
}
