package ashpapi.oculon.fabric;

import ashpapi.oculon.platform.OculonPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricPlatform implements OculonPlatform {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
