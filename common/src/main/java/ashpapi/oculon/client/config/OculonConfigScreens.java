package ashpapi.oculon.client.config;

import ashpapi.oculon.platform.OculonPlatform;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.minecraft.client.gui.screens.Screen;

public final class OculonConfigScreens {
    private OculonConfigScreens() {
    }

    public static Screen createConfigScreen(Screen parent) {
        if (OculonPlatform.get().isModLoaded("sodium")) {
            try {
                Screen sodiumScreen = SodiumBridge.create(parent);
                if (sodiumScreen != null) {
                    return sodiumScreen;
                }
            } catch (Throwable ignored) {
            }
        }
        return new OculonStandaloneConfigScreen(parent);
    }

    private static final class SodiumBridge {
        private static Screen create(Screen parent) {
            var opt = ConfigManager.CONFIG.getModOptions().stream()
                    .filter(a -> a.configId().equals("oculon"))
                    .findFirst()
                    .orElse(null);
            if (opt != null && !opt.pages().isEmpty()) {
                var page = (OptionPage) opt.pages().getFirst();
                return VideoSettingsScreen.createScreen(parent, page);
            }
            return null;
        }
    }
}
