package ashpapi.oculon.neoforge;

import ashpapi.oculon.platform.OculonPlatform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatform implements OculonPlatform {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR != null ? FMLPaths.CONFIGDIR.get() : null;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }
}
