package com.snipr.megaperformance.optimizers;

import com.hypixel.hytale.logger.HytaleLogger;
import com.snipr.megaperformance.config.MegaPerformanceConfig;

/**
 * Best effort particle density scaler. Without a stable public API, this records the chosen scalar
 * and sets JVM properties that can be read by other systems/patches if present. It is intentionally
 * conservative and side-effect free if unsupported/breaks.
 */
public class ParticleOptimizer {

    private double currentScalar = 1.0;
    private boolean applyProjectiles = true;

    public void applyScalar(MegaPerformanceConfig.Particles cfg, HytaleLogger logger) {
        currentScalar = Math.max(0.1, Math.min(1.0, cfg.densityScalar));
        applyProjectiles = cfg.applyToProjectiles;

        System.setProperty("megaperf.particle.scalar", Double.toString(currentScalar));
        System.setProperty("megaperf.particle.applyProjectiles", Boolean.toString(applyProjectiles));

        logger.atInfo().log("[MegaPerf] Particle density scalar set to %.2f (projectiles=%s)", currentScalar, applyProjectiles);
    }

    public double getCurrentScalar() {
        return currentScalar;
    }

    public boolean isApplyProjectiles() {
        return applyProjectiles;
    }
}
