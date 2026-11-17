package ashpapi.oculon.fabric;

import ashpapi.oculon.client.OculonController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;

public class OculonFabricMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(OculonController.getCategoryId());
        KeyMappingHelper.registerKeyMapping(OculonController.createZoomKey(category));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> OculonController.resetState());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> OculonController.resetState());
    }
}
