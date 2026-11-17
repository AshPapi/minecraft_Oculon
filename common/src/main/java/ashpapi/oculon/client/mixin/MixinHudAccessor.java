package ashpapi.oculon.client.mixin;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface MixinHudAccessor {
    @Accessor("isHidden")
    boolean oculon$isHidden();

    @Accessor("isHidden")
    void oculon$setHidden(boolean hidden);
}
