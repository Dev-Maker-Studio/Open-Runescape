import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class LoginEffectBoot {
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

    static ClassTransform replace(String name, String type, String hookType, boolean argument) {
        var hooks = ClassDesc.of("LoginEffectHooks");
        return ClassTransform.transformingMethods(
            method -> method.methodName().equalsString(name) && method.methodType().equalsString(type),
            (builder, element) -> {
                if (element instanceof CodeModel) {
                    builder.withCode(code -> {
                        if (argument) {
                            if (type.startsWith("(Z")) code.iload(1);
                            else code.aload(1);
                        }
                        code.invokestatic(hooks, name, MethodTypeDesc.ofDescriptor(hookType)).return_();
                    });
                } else {
                    builder.with(element);
                }
            });
    }

    static byte[] patch(byte[] bytes) {
        var state = "(Lcom/jagex/game/runetek5/client/GameState;)V";
        var classFile = ClassFile.of();
        var first = classFile.transformClass(classFile.parse(bytes), replace("setGameState", state, state, true));
        return classFile.transformClass(classFile.parse(first), replace("logout", "(Z)V", "(Z)V", true));
    }

    public static void main(String[] args) throws Exception {
        var urls = new URL[args.length - 2];
        for (int i = 2; i < args.length; i++) urls[i - 2] = Path.of(args[i]).toUri().toURL();
        var loader = new PatchLoader(urls);
        var driver = loader.loadClass("LoginEffectFixtures");
        driver.getMethod("main", String[].class).invoke(null, (Object) new String[] {args[0], args[1]});
    }
}
