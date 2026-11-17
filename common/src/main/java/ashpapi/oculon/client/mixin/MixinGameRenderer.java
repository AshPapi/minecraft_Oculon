package ashpapi.oculon.client.mixin;

import ashpapi.oculon.client.OculonController;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapMethod(method = "extract")
    private void oculon$wrapExtractWithF1Mode(DeltaTracker deltaTracker, boolean advanceGameTime, Operation<Void> original) {
        if (!OculonController.shouldHideHudInFrame(this.minecraft)) {
            original.call(deltaTracker, advanceGameTime);
            return;
        }
        MixinHudAccessor hud = (MixinHudAccessor) this.minecraft.gui.hud;
        boolean prevHidden = hud.oculon$isHidden();
        hud.oculon$setHidden(true);
        try {
            original.call(deltaTracker, advanceGameTime);
        } finally {
            hud.oculon$setHidden(prevHidden);
        }
    }

    @WrapMethod(method = "render")
    private void oculon$wrapRenderWithF1Mode(Operation<Void> original) {
        if (!OculonController.shouldHideHudInFrame(this.minecraft)) {
            original.call();
            return;
        }
        MixinHudAccessor hud = (MixinHudAccessor) this.minecraft.gui.hud;
        boolean prevHidden = hud.oculon$isHidden();
        hud.oculon$setHidden(true);
        try {
            original.call();
        } finally {
            hud.oculon$setHidden(prevHidden);
        }
    }

    @WrapMethod(method = "bobView")
    private void oculon$scaleViewBobbing(CameraRenderState cameraState, PoseStack poseStack, Operation<Void> original) {
        float scale = OculonController.getViewBobbingScale();
        if (scale >= 0.9999f || cameraState == null || cameraState.entityRenderState == null) {
            original.call(cameraState, poseStack);
            return;
        }
        if (scale <= 0.0001f) {
            return;
        }
        float prevBob = cameraState.entityRenderState.bob;
        cameraState.entityRenderState.bob = prevBob * scale;
        try {
            original.call(cameraState, poseStack);
        } finally {
            cameraState.entityRenderState.bob = prevBob;
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void oculon$hideHandInZoom(CallbackInfo ci) {
        if (OculonController.shouldHideHand()) {
            ci.cancel();
        }
    }
}
