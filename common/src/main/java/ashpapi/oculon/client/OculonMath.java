package ashpapi.oculon.client;

public final class OculonMath {
    public static final float MIN_ZOOM = 2.0f;
    public static final float MAX_ZOOM = 500.0f;
    public static final float MIN_FOV_DEGREES = 0.01f;
    public static final double MAX_SCROLL_TIERS = 30.0;
    public static final double MIN_NEGATIVE_SCROLL_TIERS = -6.0;
    public static final double ZOOM_IN_DURATION_SECONDS = 0.30;
    public static final double ZOOM_OUT_DURATION_SECONDS = 0.18;
    public static final double SCROLL_SMOOTH_SPEED = 18.0;

    private OculonMath() {
    }

    public static double evaluateSineCurve(double progress) {
        if (!Double.isFinite(progress)) {
            return 1.0;
        }
        double t = Math.clamp(progress, 0.0, 1.0);
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        return 0.5 * (1.0 - Math.cos(Math.PI * t));
    }

    public static double logLerp(double startDivisor, double targetDivisor, double easedProgress) {
        double s = Double.isFinite(startDivisor) ? Math.clamp(startDivisor, 0.01, MAX_ZOOM) : 1.0;
        double e = Double.isFinite(targetDivisor) ? Math.clamp(targetDivisor, 0.01, MAX_ZOOM) : 1.0;
        double p = Double.isFinite(easedProgress) ? Math.clamp(easedProgress, 0.0, 1.0) : 1.0;
        if (p <= 0.0) {
            return s;
        }
        if (p >= 1.0 || Math.abs(s - e) < 1e-9) {
            return e;
        }
        return Math.exp(Math.log(s) + (Math.log(e) - Math.log(s)) * p);
    }

    public static double stepScrollTier(double currentTier, double scrollDeltaY, float initialZoom) {
        double minTier = initialZoom > MIN_ZOOM + 1e-4f ? MIN_NEGATIVE_SCROLL_TIERS : 0.0;
        double clamped = Double.isFinite(currentTier) ? Math.clamp(currentTier, minTier, MAX_SCROLL_TIERS) : 0.0;
        if (!Double.isFinite(scrollDeltaY) || Math.abs(scrollDeltaY) < 1e-6) {
            return clamped;
        }
        return Math.clamp(clamped + scrollDeltaY, minTier, MAX_SCROLL_TIERS);
    }

    public static double computeScrollDivisorForTier(double scrollTier, float initialZoom, float maxZoom) {
        double init = Math.clamp(initialZoom, MIN_ZOOM, MAX_ZOOM);
        double max = Math.clamp(maxZoom, init, MAX_ZOOM);
        if (!Double.isFinite(scrollTier) || Math.abs(scrollTier) < 1e-9) {
            return 1.0;
        }
        if (scrollTier > 0.0) {
            if (max <= init + 1e-6) {
                return 1.0;
            }
            double s = Math.clamp(scrollTier / MAX_SCROLL_TIERS, 0.0, 1.0);
            double u = 0.40 * s + 0.60 * Math.pow(s, 1.5);
            return Math.pow(max / init, u);
        }
        if (init <= MIN_ZOOM + 1e-6) {
            return 1.0;
        }
        double sNeg = Math.clamp(-scrollTier / (-MIN_NEGATIVE_SCROLL_TIERS), 0.0, 1.0);
        return Math.pow(MIN_ZOOM / init, sNeg);
    }

    public static double stepLogExponential(double currentDivisor, double targetDivisor, double speed, double deltaTimeSeconds) {
        double cur = Double.isFinite(currentDivisor) ? Math.clamp(currentDivisor, 0.01, MAX_ZOOM) : 1.0;
        double tgt = Double.isFinite(targetDivisor) ? Math.clamp(targetDivisor, 0.01, MAX_ZOOM) : 1.0;
        if (!Double.isFinite(deltaTimeSeconds) || deltaTimeSeconds <= 0.0) {
            return cur;
        }
        if (!Double.isFinite(speed) || speed <= 0.0) {
            return tgt;
        }
        double decay = Math.exp(-speed * Math.min(deltaTimeSeconds, 1.0));
        double lnCur = Math.log(cur);
        double lnTgt = Math.log(tgt);
        double lnNext = lnTgt + (lnCur - lnTgt) * decay;
        double next = Math.exp(lnNext);
        return Math.abs(next - tgt) < 1e-6 * tgt ? tgt : next;
    }

    public static float computeZoomedFov(float baseFovDegrees, double effectiveZoomFactor) {
        float clampedBase = Math.clamp(baseFovDegrees, 10.0f, 170.0f);
        double zoom = Double.isFinite(effectiveZoomFactor) ? Math.clamp(effectiveZoomFactor, 1.0, MAX_ZOOM) : 1.0;
        if (zoom <= 1.0001) {
            return clampedBase;
        }
        double halfBaseRad = Math.toRadians(clampedBase) * 0.5;
        double zoomedHalfRad = Math.atan(Math.tan(halfBaseRad) / zoom);
        float zoomedFov = (float) Math.toDegrees(zoomedHalfRad * 2.0);
        return Math.clamp(zoomedFov, MIN_FOV_DEGREES, clampedBase);
    }

    public static double computeSensitivityMultiplier(float currentFovDegrees, float baseFovDegrees) {
        float clampedBase = Math.clamp(baseFovDegrees, 10.0f, 170.0f);
        float clampedCurrent = Math.clamp(currentFovDegrees, MIN_FOV_DEGREES, clampedBase);
        double tanCurrent = Math.tan(Math.toRadians(clampedCurrent) * 0.5);
        double tanBase = Math.tan(Math.toRadians(clampedBase) * 0.5);
        if (tanBase <= 1e-6) {
            return 1.0;
        }
        double ratio = tanCurrent / tanBase;
        return Math.clamp(ratio, 0.0001, 1.0);
    }

    public static float computeViewBobbingScale(double effectiveZoomFactor) {
        if (!Double.isFinite(effectiveZoomFactor) || effectiveZoomFactor <= 1.001) {
            return 1.0f;
        }
        return (float) Math.clamp(1.0 / effectiveZoomFactor, 0.0, 1.0);
    }
}
