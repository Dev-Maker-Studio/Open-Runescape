import java.util.ArrayList;
import java.util.List;

public final class WorldMapEffectHooks {
    static final List<String> LOG = new ArrayList<>();

    public static void loadAssets(boolean full) {
        LOG.add(full ? "reload" : "reload-partial");
    }

    public static void setLoadingWorldMap() {
        LOG.add("loading");
    }
}
