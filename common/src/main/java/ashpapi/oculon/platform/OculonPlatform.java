package ashpapi.oculon.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface OculonPlatform {
    OculonPlatform INSTANCE = load();

    static OculonPlatform get() {
        return INSTANCE;
    }

    private static OculonPlatform load() {
        return ServiceLoader.load(OculonPlatform.class)
                .findFirst()
                .orElseGet(() -> new OculonPlatform() {
                    @Override
                    public Path getConfigDir() {
                        return Path.of(System.getProperty("java.io.tmpdir"));
                    }

                    @Override
                    public boolean isModLoaded(String modId) {
                        return false;
                    }
                });
    }

    Path getConfigDir();

    boolean isModLoaded(String modId);
}
