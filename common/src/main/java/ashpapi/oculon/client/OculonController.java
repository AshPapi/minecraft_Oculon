package ashpapi.oculon.client;

import ashpapi.oculon.client.config.OculonConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class OculonController {
    public static final double HUD_RESTORE_ZOOM_THRESHOLD = 1.03;

    private static KeyMapping zoomKey;

    private static boolean zoomActive = false;
    private static boolean wasKeyDown = false;
    private static float activeInitialZoom = 4.0f;
    private static float activeMaxZoom = 500.0f;
    private static double baseDivisor = 1.0;
    private static double baseStartDivisor = 1.0;
    private static double baseTargetDivisor = 1.0;
    private static double baseProgress = 1.0;
    private static double scrollTier = 0.0;
    private static double scrollDivisor = 1.0;
    private static double scrollExitStartDivisor = 1.0;
    private static double smoothedZoomFactor = 1.0;
    private static float lastBaseFov = 70.0f;
    private static float lastEffectiveFov = 70.0f;
    private static double mouseRemainderX = 0.0;
    private static double mouseRemainderY = 0.0;
    private static long lastFrameNanos = 0L;

    private OculonController() {
    }

    public static Identifier getCategoryId() {
        return Identifier.fromNamespaceAndPath("oculon", "general");
    }

    public static KeyMapping createZoomKey(KeyMapping.Category category) {
        KeyMapping key = new KeyMapping(
                "key.oculon.zoom",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_C,
                category
        );
        zoomKey = key;
        return key;
    }

    public static boolean isZoomActive() {
        return zoomActive;
    }

    public static boolean isZoomingOrTransitioning() {
        return zoomActive || smoothedZoomFactor > 1.001;
    }

    public static double getSmoothedZoomFactor() {
        return smoothedZoomFactor;
    }

    public static float getCurrentZoomFactor() {
        return (float) smoothedZoomFactor;
    }

    public static double getScrollTier() {
        return scrollTier;
    }

    public static float getTargetZoomFactor() {
        if (!zoomActive) {
            return 1.0f;
        }
        double targetScroll = OculonMath.computeScrollDivisorForTier(scrollTier, activeInitialZoom, activeMaxZoom);
        return (float) Math.clamp(activeInitialZoom * targetScroll, OculonMath.MIN_ZOOM, activeMaxZoom);
    }

    public static void resetState() {
        zoomActive = false;
        wasKeyDown = false;
        OculonConfig cfg = OculonConfig.CONFIG;
        activeInitialZoom = cfg != null ? Math.clamp(cfg.initialZoom, OculonMath.MIN_ZOOM, 16.0f) : 4.0f;
        activeMaxZoom = OculonMath.MAX_ZOOM;
        baseDivisor = 1.0;
        baseStartDivisor = 1.0;
        baseTargetDivisor = 1.0;
        baseProgress = 1.0;
        scrollTier = 0.0;
        scrollDivisor = 1.0;
        scrollExitStartDivisor = 1.0;
        smoothedZoomFactor = 1.0;
        mouseRemainderX = 0.0;
        mouseRemainderY = 0.0;
        lastFrameNanos = 0L;
    }

    public static void updateInputState(boolean keyDown, boolean hasOpenScreen, OculonConfig.ZoomMode mode, float initialZoom) {
        OculonConfig cfg = OculonConfig.CONFIG;
        boolean rememberScroll = cfg != null && cfg.rememberScrollZoom;
        updateInputState(keyDown, hasOpenScreen, mode, initialZoom, OculonMath.MAX_ZOOM, rememberScroll);
    }

    public static void updateInputState(
            boolean keyDown,
            boolean hasOpenScreen,
            OculonConfig.ZoomMode mode,
            float initialZoom,
            float maxZoom,
            boolean rememberScrollZoom
    ) {
        float clampedInitial = Math.clamp(initialZoom, OculonMath.MIN_ZOOM, 16.0f);
        float clampedMax = Math.clamp(maxZoom, clampedInitial, OculonMath.MAX_ZOOM);
        activeInitialZoom = clampedInitial;
        activeMaxZoom = clampedMax;

        if (hasOpenScreen) {
            if (zoomActive) {
                beginZoomExit(rememberScrollZoom);
            }
            wasKeyDown = false;
            return;
        }

        boolean nextActive = zoomActive;
        if (mode == OculonConfig.ZoomMode.TOGGLE) {
            if (keyDown && !wasKeyDown) {
                nextActive = !zoomActive;
            }
        } else {
            nextActive = keyDown;
        }
        wasKeyDown = keyDown;

        if (nextActive && !zoomActive) {
            beginZoomEnter(clampedInitial, rememberScrollZoom);
        } else if (!nextActive && zoomActive) {
            beginZoomExit(rememberScrollZoom);
        } else if (nextActive) {
            baseTargetDivisor = clampedInitial;
        }
    }

    private static void beginZoomEnter(float clampedInitial, boolean rememberScrollZoom) {
        zoomActive = true;
        baseStartDivisor = Math.max(1.0, baseDivisor);
        baseTargetDivisor = clampedInitial;
        baseProgress = 0.0;
        mouseRemainderX = 0.0;
        mouseRemainderY = 0.0;
        if (!rememberScrollZoom) {
            scrollTier = 0.0;
        }
    }

    private static void beginZoomExit(boolean rememberScrollZoom) {
        zoomActive = false;
        baseStartDivisor = Math.max(1.0, baseDivisor);
        baseTargetDivisor = 1.0;
        baseProgress = 0.0;
        scrollExitStartDivisor = Math.max(0.01, scrollDivisor);
        mouseRemainderX = 0.0;
        mouseRemainderY = 0.0;
        if (!rememberScrollZoom) {
            scrollTier = 0.0;
        }
    }

    private static void pollKeyState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            resetState();
            return;
        }
        boolean hasScreen = mc.gui != null && (mc.gui.screen() != null || mc.gui.overlay() != null);
        boolean keyDown = zoomKey != null && zoomKey.isDown();
        OculonConfig cfg = OculonConfig.CONFIG;
        OculonConfig.ZoomMode mode = cfg != null ? cfg.getZoomMode() : OculonConfig.ZoomMode.HOLD;
        float initZoom = cfg != null ? cfg.initialZoom : 4.0f;
        boolean rememberScroll = cfg != null && cfg.rememberScrollZoom;
        updateInputState(keyDown, hasScreen, mode, initZoom, OculonMath.MAX_ZOOM, rememberScroll);
    }

    public static float modifyWorldFov(float baseFov) {
        pollKeyState();

        long now = System.nanoTime();
        double dt;
        if (lastFrameNanos == 0L) {
            dt = 1.0 / 60.0;
        } else {
            dt = Math.clamp((now - lastFrameNanos) * 1e-9, 0.0001, 0.25);
        }
        lastFrameNanos = now;

        return stepZoomFov(baseFov, dt);
    }

    public static float stepZoomFov(float baseFov, double dt) {
        lastBaseFov = baseFov;
        double safeDt = Double.isFinite(dt) && dt > 0.0 ? Math.min(dt, 0.25) : (1.0 / 60.0);

        double duration = zoomActive ? OculonMath.ZOOM_IN_DURATION_SECONDS : OculonMath.ZOOM_OUT_DURATION_SECONDS;
        if (baseProgress < 1.0) {
            baseProgress = Math.min(1.0, baseProgress + safeDt / duration);
        }
        double eased = OculonMath.evaluateSineCurve(baseProgress);
        baseDivisor = OculonMath.logLerp(baseStartDivisor, baseTargetDivisor, eased);

        if (zoomActive) {
            double targetScroll = OculonMath.computeScrollDivisorForTier(scrollTier, activeInitialZoom, activeMaxZoom);
            scrollDivisor = OculonMath.stepLogExponential(scrollDivisor, targetScroll, OculonMath.SCROLL_SMOOTH_SPEED, safeDt);
        } else {
            scrollDivisor = OculonMath.logLerp(scrollExitStartDivisor, 1.0, eased);
        }

        smoothedZoomFactor = Math.clamp(baseDivisor * scrollDivisor, 1.0, OculonMath.MAX_ZOOM);
        if (!zoomActive && (baseProgress >= 1.0 || smoothedZoomFactor <= 1.001)) {
            baseDivisor = 1.0;
            scrollDivisor = 1.0;
            smoothedZoomFactor = 1.0;
            lastEffectiveFov = baseFov;
            return baseFov;
        }

        lastEffectiveFov = OculonMath.computeZoomedFov(baseFov, smoothedZoomFactor);
        return lastEffectiveFov;
    }

    public static boolean onMouseScroll(double scrollDeltaY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || (mc.gui != null && mc.gui.screen() != null)) {
            return false;
        }
        pollKeyState();
        if (!zoomActive) {
            return false;
        }
        applyScrollDelta(scrollDeltaY);
        return true;
    }

    public static void applyScrollDelta(double scrollDeltaY) {
        if (Math.abs(scrollDeltaY) > 1e-5) {
            scrollTier = OculonMath.stepScrollTier(scrollTier, scrollDeltaY, activeInitialZoom);
        }
    }

    public static double getSensitivityMultiplier() {
        if (!isZoomingOrTransitioning()) {
            return 1.0;
        }
        return OculonMath.computeSensitivityMultiplier(lastEffectiveFov, lastBaseFov);
    }

    public static float computeYawWrapDelta(float yRot) {
        if (!Float.isFinite(yRot) || (yRot >= -180.0f && yRot <= 180.0f)) {
            return 0.0f;
        }
        return (float) (Math.floor((yRot + 180.0) / 360.0) * 360.0);
    }

    public static void normalizeActivePlayerYaw() {
        if (!isZoomingOrTransitioning()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        float yRot = mc.player.getYRot();
        float wrap = computeYawWrapDelta(yRot);
        if (wrap != 0.0f) {
            mc.player.setYRot(yRot - wrap);
            mc.player.yRotO -= wrap;
            mc.player.yHeadRot -= wrap;
            mc.player.yHeadRotO -= wrap;
            mc.player.yBodyRot -= wrap;
            mc.player.yBodyRotO -= wrap;
        }
    }

    public static double scaleMouseDeltaX(double rawDx, double baseSens) {
        return scaleMouseDelta(rawDx, baseSens, true);
    }

    public static double scaleMouseDeltaY(double rawDy, double baseSens) {
        return scaleMouseDelta(rawDy, baseSens, false);
    }

    private static double scaleMouseDelta(double rawDelta, double baseSens, boolean isX) {
        double remainder = isX ? mouseRemainderX : mouseRemainderY;
        if (!isZoomingOrTransitioning()) {
            if (isX) {
                mouseRemainderX = 0.0;
            } else {
                mouseRemainderY = 0.0;
            }
            return rawDelta;
        }
        if (rawDelta == 0.0 && remainder == 0.0) {
            return 0.0;
        }
        double effSens = Double.isFinite(baseSens) && baseSens > 1e-6 ? baseSens : 1.0;
        double totalTurn = rawDelta * getSensitivityMultiplier() * effSens + remainder;
        float quantizedTurn = (float) totalTurn;
        double nextRemainder = totalTurn - quantizedTurn;
        if (isX) {
            mouseRemainderX = nextRemainder;
        } else {
            mouseRemainderY = nextRemainder;
        }
        return quantizedTurn / effSens;
    }

    public static float getViewBobbingScale() {
        if (!isZoomingOrTransitioning()) {
            return 1.0f;
        }
        return OculonMath.computeViewBobbingScale(smoothedZoomFactor);
    }

    public static boolean shouldHideHud() {
        return zoomActive || smoothedZoomFactor > HUD_RESTORE_ZOOM_THRESHOLD;
    }

    public static boolean shouldHideHudInFrame(Minecraft mc) {
        return shouldHideHud()
                && mc != null
                && mc.level != null
                && mc.player != null
                && mc.gui != null
                && mc.gui.hud != null
                && mc.gui.screen() == null
                && mc.gui.overlay() == null;
    }

    public static boolean shouldHideHand() {
        return shouldHideHud();
    }
}
