package com.snipr.megaperformance.optimizers;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Attempts soft caps and periodic trimming on non-player entities using reflection-friendly access.
 * All operations are best-effort and safely wrapped to avoid crashing the server.
 */
public class EntityOptimizer {

    private long tickCounter = 0;

    public void tick(World world, int softCapPerChunk, int aiSkipEvery, boolean aggressiveTrim, HytaleLogger logger) {
        if (world == null) return;
        tickCounter++;
        if (tickCounter % aiSkipEvery != 0) return;

        EntityStore store = world.getEntityStore();
        if (store == null) return;

        Collection<?> entities = resolveEntities(store);
        if (entities == null) return;

        int total = entities.size();
        if (total <= softCapPerChunk && !aggressiveTrim) return;

        int trimmed = 0;
        int target = Math.max(0, total - softCapPerChunk);
        List<Object> toRemove = new ArrayList<>();

        for (Object e : entities) {
            if (isPlayerEntity(e)) continue;
            toRemove.add(e);
            if (toRemove.size() >= target) break;
        }

        for (Object e : toRemove) {
            if (tryRemove(store, entities, e)) {
                trimmed++;
            }
        }

        if (trimmed > 0) {
            logger.atInfo().log("[MegaPerf] Trimmed %d entities (total before=%d, cap=%d)", trimmed, total, softCapPerChunk);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<?> resolveEntities(EntityStore store) {
        try {
            Method getAll = store.getClass().getMethod("getAllEntities");
            Object result = getAll.invoke(store);
            if (result instanceof Collection) return (Collection<?>) result;
        } catch (Exception ignored) { }

        try {
            Field f = store.getClass().getDeclaredField("entities");
            f.setAccessible(true);
            Object result = f.get(store);
            if (result instanceof Collection) return (Collection<?>) result;
        } catch (Exception ignored) { }

        return null;
    }

    private boolean tryRemove(EntityStore store, Collection<?> entities, Object entity) {
        try {
            Method removeEntity = store.getClass().getMethod("removeEntity", Object.class);
            removeEntity.invoke(store, entity);
            return true;
        } catch (Exception ignored) { }

        try {
            return entities.remove(entity);
        } catch (Exception ignored) { }

        return false;
    }

    private boolean isPlayerEntity(Object entity) {
        if (entity == null) return false;
        String name = entity.getClass().getSimpleName().toLowerCase();
        return name.contains("player");
    }
}
