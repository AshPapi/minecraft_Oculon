package ashpapi.oculon.client.mixin;

import ashpapi.oculon.client.OculonController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void oculon$modifyWorldFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(OculonController.modifyWorldFov(cir.getReturnValueF()));
    }
}
