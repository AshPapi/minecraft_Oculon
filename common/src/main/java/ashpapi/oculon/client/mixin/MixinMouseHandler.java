package ashpapi.oculon.client.mixin;

import ashpapi.oculon.client.OculonController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void oculon$onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (OculonController.onMouseScroll(yoffset)) {
            ci.cancel();
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void oculon$scaleTurnSensitivity(double deltaTime, CallbackInfo ci) {
        if (!OculonController.isZoomingOrTransitioning()) {
            return;
        }
        OculonController.normalizeActivePlayerYaw();
        this.accumulatedDX = OculonController.scaleMouseDeltaX(this.accumulatedDX, 0.15);
        this.accumulatedDY = OculonController.scaleMouseDeltaY(this.accumulatedDY, 0.15);
    }
}
