import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class WorldMapEffectBoot {
    static final String COMPANION = "com.jagex.game.runetek5.client.Client$Companion";
    static final String[] PARENT_FIRST = {"java.", "javax.", "jdk.", "sun.", "com.sun."};

    static final class PatchLoader extends URLClassLoader {
        PatchLoader(URL[] urls) {
            super(urls, ClassLoader.getPlatformClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                var loaded = findLoadedClass(name);
                if (loaded == null) {
                    boolean parentFirst = false;
                    for (var prefix : PARENT_FIRST) if (name.startsWith(prefix)) parentFirst = true;
                    if (parentFirst) {
                        try {
                            loaded = getParent().loadClass(name);
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                    if (loaded == null) loaded = define(name);
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }

        Class<?> define(String name) throws ClassNotFoundException {
            try (InputStream stream = getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (stream == null) return getParent().loadClass(name);
                byte[] bytes = stream.readAllBytes();
                if (name.equals(COMPANION)) bytes = patch(bytes);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (java.io.IOException error) {
                throw new ClassNotFoundException(name, error);
            }
        }
    }

    static byte[] patch(byte[] bytes) {
        var hooks = ClassDesc.of("WorldMapEffectHooks");
        var loadAssets = ClassTransform.transformingMethods(
            method -> method.methodName().equalsString("loadAssets") && method.methodType().equalsString("(Z)V"),
            (builder, element) -> {
                if (element instanceof CodeModel) {
                    builder.withCode(code -> code.iload(1)
                        .invokestatic(hooks, "loadAssets", MethodTypeDesc.ofDescriptor("(Z)V"))
                        .return_());
                } else {
                    builder.with(element);
                }
            });
        var loading = ClassTransform.transformingMethods(
            method -> method.methodName().equalsString("setLoadingWorldMap") && method.methodType().equalsString("()V"),
            (builder, element) -> {
                if (element instanceof CodeModel) {
                    builder.withCode(code -> code
                        .invokestatic(hooks, "setLoadingWorldMap", MethodTypeDesc.ofDescriptor("()V"))
                        .return_());
                } else {
                    builder.with(element);
                }
            });
        var classFile = ClassFile.of();
        var first = classFile.transformClass(classFile.parse(bytes), loadAssets);
        return classFile.transformClass(classFile.parse(first), loading);
    }

    public static void main(String[] args) throws Exception {
        var urls = new URL[args.length - 2];
        for (int i = 2; i < args.length; i++) urls[i - 2] = Path.of(args[i]).toUri().toURL();
        var loader = new PatchLoader(urls);
        var driver = loader.loadClass("WorldMapEffectFixtures");
        driver.getMethod("main", String[].class).invoke(null, (Object) new String[] {args[0], args[1]});
    }
}
