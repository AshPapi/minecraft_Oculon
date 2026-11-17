package ashpapi.oculon.neoforge;

import ashpapi.oculon.client.OculonController;
import ashpapi.oculon.client.config.OculonConfigScreens;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "oculon", dist = Dist.CLIENT)
public class OculonMod {
    public OculonMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            KeyMapping.Category category = new KeyMapping.Category(OculonController.getCategoryId());
            event.registerCategory(category);
            event.register(OculonController.createZoomKey(category));
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> OculonController.resetState());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> OculonController.resetState());
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> OculonConfigScreens.createConfigScreen(parent));
    }
}
