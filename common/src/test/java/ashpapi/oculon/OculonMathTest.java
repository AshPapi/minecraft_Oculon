package ashpapi.oculon;

import ashpapi.oculon.client.OculonController;
import ashpapi.oculon.client.OculonMath;
import ashpapi.oculon.client.config.OculonConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OculonMathTest {

    @Test
    void testThirtyScrollTiersMonotonicityUpTo500xAndDownTo2x() {
        double tier = 0.0;
        double prevZoom = 4.0;
        for (int i = 1; i <= 35; i++) {
            tier = OculonMath.stepScrollTier(tier, 1.0, 4.0f);
            double div = OculonMath.computeScrollDivisorForTier(tier, 4.0f, 500.0f);
            double effZoom = 4.0 * div;
            assertTrue(effZoom >= prevZoom - 1e-6, "Scroll up must monotonically increase zoom");
            assertTrue(effZoom <= 500.0 + 1e-4, "Zoom must not exceed 500x");
            prevZoom = effZoom;
        }
        assertEquals(30.0, tier, 1e-6);
        assertEquals(500.0, prevZoom, 1e-4);

        for (int i = 0; i < 40; i++) {
            tier = OculonMath.stepScrollTier(tier, -1.0, 4.0f);
        }
        assertEquals(-6.0, tier, 1e-6);
        double minEff = 4.0 * OculonMath.computeScrollDivisorForTier(tier, 4.0f, 500.0f);
        assertEquals(2.0, minEff, 1e-4);
    }

    @Test
    void testSineTransitionCurveNormalizedAndMonotonic() {
        assertEquals(0.0, OculonMath.evaluateSineCurve(0.0), 1e-9);
        assertEquals(1.0, OculonMath.evaluateSineCurve(1.0), 1e-9);
        double prev = 0.0;
        for (int step = 1; step <= 100; step++) {
            double cur = OculonMath.evaluateSineCurve(step / 100.0);
            assertTrue(cur >= prev - 1e-9 && cur <= 1.0 + 1e-9);
            prev = cur;
        }
    }

    @Test
    void testLogExponentialFrameRateInvariance() {
        double start = 1.0;
        double target = 125.0;
        double speed = 18.0;
        double totalTime = 0.25;

        double val30Fps = start;
        int steps30 = 15;
        for (int i = 0; i < steps30; i++) {
            val30Fps = OculonMath.stepLogExponential(val30Fps, target, speed, totalTime / steps30);
        }

        double val240Fps = start;
        int steps240 = 120;
        for (int i = 0; i < steps240; i++) {
            val240Fps = OculonMath.stepLogExponential(val240Fps, target, speed, totalTime / steps240);
        }

        assertEquals(val30Fps, val240Fps, 1e-5, "Log-space exponential smoothing must be strictly FPS-independent");
    }

    @Test
    void testTangentSensitivityAndFovUpTo500x() {
        assertEquals(0.01f, OculonMath.MIN_FOV_DEGREES, 1e-6f);
        for (float baseFov : new float[]{20.0f, 30.0f, 70.0f, 110.0f}) {
            for (float zoom : new float[]{2.0f, 4.0f, 16.0f, 64.0f, 250.0f, 500.0f}) {
                float zoomedFov = OculonMath.computeZoomedFov(baseFov, zoom);
                assertTrue(zoomedFov < baseFov && zoomedFov >= OculonMath.MIN_FOV_DEGREES);
                double sensMult = OculonMath.computeSensitivityMultiplier(zoomedFov, baseFov);
                assertEquals(1.0 / zoom, sensMult, 1e-4, "Sensitivity multiplier must match 1/zoom up to 500x");
                assertEquals(1.0f / zoom, OculonMath.computeViewBobbingScale(zoom), 1e-4f);
            }
        }
    }

    @Test
    void testInstantTargetZoomContractAndSubpixelMouseAccumulator() {
        OculonController.resetState();
        assertFalse(OculonController.isZoomActive());
        assertEquals(1.0f, OculonController.getCurrentZoomFactor(), 1e-5f);
        assertEquals(1.0f, OculonController.getTargetZoomFactor(), 1e-5f);

        OculonController.updateInputState(true, false, OculonConfig.ZoomMode.HOLD, 4.0f, 500.0f, false);
        assertTrue(OculonController.isZoomActive());
        assertEquals(4.0f, OculonController.getTargetZoomFactor(), 1e-4f);

        for (int i = 0; i < 30; i++) {
            OculonController.applyScrollDelta(1.0);
        }
        assertEquals(500.0f, OculonController.getTargetZoomFactor(), 1e-3f);

        for (int i = 0; i < 60; i++) {
            OculonController.stepZoomFov(70.0f, 1.0 / 60.0);
        }
        assertEquals(500.0f, OculonController.getCurrentZoomFactor(), 0.5f);

        double sumScaled = 0.0;
        for (int i = 0; i < 1000; i++) {
            sumScaled += OculonController.scaleMouseDeltaX(0.01, 0.15);
        }
        assertEquals(10.0 / 500.0, sumScaled, 1e-4, "Sub-pixel mouse accumulator must preserve tiny movements at 500x");

        float rawYaw = 16384.25f;
        float rawYawO = 16384.00f;
        float wrap = OculonController.computeYawWrapDelta(rawYaw);
        float wrappedYaw = rawYaw - wrap;
        float wrappedYawO = rawYawO - wrap;
        assertTrue(wrappedYaw >= -180.0f && wrappedYaw <= 180.0f);
        assertEquals(rawYaw - rawYawO, wrappedYaw - wrappedYawO, 1e-5f);

        OculonController.updateInputState(false, false, OculonConfig.ZoomMode.HOLD, 4.0f, 500.0f, false);
        assertFalse(OculonController.isZoomActive());
        assertEquals(1.0f, OculonController.getTargetZoomFactor(), 1e-5f);
    }

    @Test
    void testContinuousReversalFrom500xWithoutJumpAndRemovedBloatConfigs() {
        OculonController.resetState();
        OculonController.updateInputState(true, false, OculonConfig.ZoomMode.HOLD, 4.0f, 500.0f, false);
        for (int i = 0; i < 30; i++) {
            OculonController.applyScrollDelta(1.0);
        }
        for (int i = 0; i < 60; i++) {
            OculonController.stepZoomFov(70.0f, 1.0 / 60.0);
        }
        assertEquals(500.0, OculonController.getSmoothedZoomFactor(), 0.5);

        OculonController.updateInputState(false, false, OculonConfig.ZoomMode.HOLD, 4.0f, 500.0f, false);
        OculonController.stepZoomFov(70.0f, 0.05);
        double midExitZoom = OculonController.getSmoothedZoomFactor();
        assertTrue(midExitZoom > 2.0 && midExitZoom < 450.0);

        OculonController.updateInputState(true, false, OculonConfig.ZoomMode.HOLD, 4.0f, 500.0f, false);
        OculonController.stepZoomFov(70.0f, 1.0 / 240.0);
        double afterReenterZoom = OculonController.getSmoothedZoomFactor();
        assertTrue(Math.abs(Math.log(afterReenterZoom / midExitZoom)) < 0.35, "Re-entering zoom mid-exit must be C0 continuous");

        OculonController.resetState();
        OculonController.updateInputState(true, false, OculonConfig.ZoomMode.HOLD, 4.0f, 500.0f, false);
        for (int i = 0; i < 6; i++) {
            OculonController.applyScrollDelta(-1.0);
        }
        for (int i = 0; i < 60; i++) {
            OculonController.stepZoomFov(70.0f, 1.0 / 60.0);
        }
        assertEquals(2.0, OculonController.getSmoothedZoomFactor(), 0.05);
        OculonController.updateInputState(false, false, OculonConfig.ZoomMode.HOLD, 4.0f, 500.0f, false);
        double prevExitZoom = OculonController.getSmoothedZoomFactor();
        for (int i = 0; i < 20; i++) {
            OculonController.stepZoomFov(70.0f, 1.0 / 60.0);
            double curExitZoom = OculonController.getSmoothedZoomFactor();
            assertTrue(curExitZoom <= prevExitZoom + 1e-5, "Exiting from negative scroll tier 2x must never jump upward");
            prevExitZoom = curExitZoom;
        }

        assertThrows(NoSuchFieldException.class, () -> OculonConfig.class.getDeclaredField("cinematicInertia"));
        assertThrows(NoSuchFieldException.class, () -> OculonConfig.class.getDeclaredField("scaleSensitivity"));
        assertThrows(NoSuchFieldException.class, () -> OculonConfig.class.getDeclaredField("sensitivityFactorPercent"));
        assertThrows(NoSuchFieldException.class, () -> OculonConfig.class.getDeclaredField("disableViewBobbing"));
        assertThrows(NoSuchFieldException.class, () -> OculonConfig.class.getDeclaredField("scrollZoom"));
    }

    @Test
    void testHoldToggleRememberScrollAndHudVisibilityLifecycle() {
        OculonController.resetState();
        assertFalse(OculonController.shouldHideHud());
        assertEquals(1.0f, OculonController.getViewBobbingScale(), 1e-6f);

        OculonController.updateInputState(true, false, OculonConfig.ZoomMode.TOGGLE, 4.0f, 500.0f, true);
        OculonController.applyScrollDelta(10.0);
        assertTrue(OculonController.isZoomActive());
        assertEquals(10.0, OculonController.getScrollTier(), 1e-6);

        for (int i = 0; i < 30; i++) {
            OculonController.stepZoomFov(70.0f, 1.0 / 60.0);
        }
        assertTrue(OculonController.getSmoothedZoomFactor() > 8.0);
        assertTrue(OculonController.shouldHideHud());
        assertTrue(OculonController.shouldHideHand());
        assertTrue(OculonController.getViewBobbingScale() < 0.15f);

        OculonController.updateInputState(false, false, OculonConfig.ZoomMode.TOGGLE, 4.0f, 500.0f, true);
        assertTrue(OculonController.isZoomActive());
        OculonController.updateInputState(true, false, OculonConfig.ZoomMode.TOGGLE, 4.0f, 500.0f, true);
        assertFalse(OculonController.isZoomActive());
        assertEquals(10.0, OculonController.getScrollTier(), 1e-6);

        int safety = 0;
        while (OculonController.getSmoothedZoomFactor() > OculonController.HUD_RESTORE_ZOOM_THRESHOLD && safety++ < 200) {
            OculonController.stepZoomFov(70.0f, 1.0 / 60.0);
        }
        assertFalse(OculonController.shouldHideHud());
        assertFalse(OculonController.shouldHideHand());
    }
}
