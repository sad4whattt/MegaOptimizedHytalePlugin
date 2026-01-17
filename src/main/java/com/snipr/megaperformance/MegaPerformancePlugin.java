package com.snipr.megaperformance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.snipr.megaperformance.config.MegaPerformanceConfig;
import com.snipr.megaperformance.optimizers.ChatBurstOptimizer;
import com.snipr.megaperformance.optimizers.EntityOptimizer;
import com.snipr.megaperformance.optimizers.ParticleOptimizer;
import com.snipr.megaperformance.commands.MegaperfReloadCommand;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.google.gson.stream.JsonReader;

public class MegaPerformancePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_FOLDER_NAME = "mods/Snipr_MegaOptimized";
    private static final String CONFIG_FILE_NAME = "config.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private MegaPerformanceConfig config;

    private ThreadPoolExecutor asyncExecutor;
    private ScheduledFuture<?> autosaveTask;
    private ScheduledFuture<?> metricsTask;
    private ScheduledFuture<?> entityTask;

    private final EntityOptimizer entityOptimizer = new EntityOptimizer();
    private final ChatBurstOptimizer chatOptimizer = new ChatBurstOptimizer();
    private final ParticleOptimizer particleOptimizer = new ParticleOptimizer();

    public MegaPerformancePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        this.config = loadConfig();
        initAsyncExecutor();
        scheduleAutosave();
        scheduleLightMetrics();
        registerReloadCommand();
        registerChatOptimizer();
        scheduleEntityOptimizer();
        particleOptimizer.applyScalar(config.particles, LOGGER);
        LOGGER.atInfo().log("MegaPerformance initialized with defaults: %s", config);
    }

    @Override
    protected void shutdown() {
        cancelScheduledTasks();
        closeAsyncExecutor();
        super.shutdown();
    }

    private void initAsyncExecutor() {
        int threads = Math.max(1, config.async.workerThreads);
        int maxQueue = Math.max(64, config.async.maxQueue);
        asyncExecutor = new ThreadPoolExecutor(
            threads,
            threads,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(maxQueue),
            runnable -> {
                Thread t = new Thread(runnable);
                t.setName("MegaPerf-Worker");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private void scheduleAutosave() {
        int minutes = Math.max(1, config.world.autosaveMinutes);
        if (autosaveTask != null) {
            autosaveTask.cancel(false);
        }
        autosaveTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                if (Universe.get() == null || Universe.get().getDefaultWorld() == null) {
                    LOGGER.atInfo().log("[MegaPerf] Skipping backup; no default world loaded yet");
                    return;
                }
                Universe.get().runBackup().exceptionally(throwable -> {
                    LOGGER.atWarning().withCause(throwable).log("Autosave/backup failed");
                    return null;
                });
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error triggering autosave/backup");
            }
        }, minutes, minutes, TimeUnit.MINUTES);
    }

    private void scheduleEntityOptimizer() {
        int aiSkipEvery = Math.max(1, config.entities.distantAiSkipEvery);
        int softCap = Math.max(1, config.entities.softCapPerChunk);
        if (entityTask != null) {
            entityTask.cancel(false);
        }
        entityTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                Universe.get().getDefaultWorld().execute(() -> entityOptimizer.tick(
                    Universe.get().getDefaultWorld(),
                    softCap,
                    aiSkipEvery,
                    config.bundled.aggressiveTrimming,
                    LOGGER
                ));
            } catch (Exception ignored) { }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void registerChatOptimizer() {
        this.getEventRegistry().registerGlobal(
            PlayerChatEvent.class,
            event -> chatOptimizer.applyFormatter(event, config.network)
        );
    }

    private void registerReloadCommand() {
        CommandRegistry registry = this.getCommandRegistry();
        registry.registerCommand(new MegaperfReloadCommand(this));
    }

    private void scheduleLightMetrics() {
        metricsTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                int queued = asyncExecutor != null ? asyncExecutor.getQueue().size() : 0;
                long completed = asyncExecutor != null ? asyncExecutor.getCompletedTaskCount() : 0;
                LOGGER.atInfo().log("[MegaPerf] async queued=%d completed=%d", queued, completed);
            } catch (Exception ignored) { }
        }, 1, 30, TimeUnit.SECONDS);
    }

    public MegaPerformanceConfig loadConfig() {
        File folder = new File(CONFIG_FOLDER_NAME);
        File file = new File(folder, CONFIG_FILE_NAME);
        if (!folder.exists()) folder.mkdirs();

        if (!file.exists()) {
            MegaPerformanceConfig defaults = new MegaPerformanceConfig();
            persistConfig(file, defaults);
            return defaults;
        }

        try (FileReader fr = new FileReader(file)) {
            JsonReader reader = new JsonReader(fr);
            reader.setLenient(true);
            MegaPerformanceConfig loaded = gson.fromJson(reader, MegaPerformanceConfig.class);
            return loaded != null ? loaded : new MegaPerformanceConfig();
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to read config, using defaults");
            MegaPerformanceConfig fallback = new MegaPerformanceConfig();
            persistConfig(file, fallback);
            return fallback;
        }
    }

    private void persistConfig(File file, MegaPerformanceConfig cfg) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(buildConfigWithComments(cfg));
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to write default config");
        }
    }

    private String buildConfigWithComments(MegaPerformanceConfig cfg) {
        return "{\n" +
            "  \"tick\": {\n" +
            "    \"maxCatchupMillis\": " + cfg.tick.maxCatchupMillis + ", // Options: 0 (no catchup), 25 (gentler), 50 (current)\n" +
            "    \"jitterClampMs\": " + cfg.tick.jitterClampMs + " // Options: 0 (no clamp), 5 (tight), 10 (current)\n" +
            "  },\n" +
            "  \"async\": {\n" +
            "    \"workerThreads\": " + cfg.async.workerThreads + ", // Options: 1 (minimal), 2 (current), 4 (busier CPUs)\n" +
            "    \"maxQueue\": " + cfg.async.maxQueue + " // Options: 128 (strict), 512 (current), 1024 (burstier)\n" +
            "  },\n" +
            "  \"entities\": {\n" +
            "    \"softCapPerChunk\": " + cfg.entities.softCapPerChunk + ", // Options: 20 (tighter), 35 (current), 50 (looser)\n" +
            "    \"distantAiSkipEvery\": " + cfg.entities.distantAiSkipEvery + " // Options: 2 (more AI), 4 (current), 8 (fewer AI ticks)\n" +
            "  },\n" +
            "  \"network\": {\n" +
            "    \"packetBurstLimit\": " + cfg.network.packetBurstLimit + ", // Options: 128 (strict), 256 (current), 512 (looser)\n" +
            "    \"coalesceChatMs\": " + cfg.network.coalesceChatMs + " // Options: 0 (off), 50 (tighter), 75 (current)\n" +
            "  },\n" +
            "  \"world\": {\n" +
            "    \"autosaveMinutes\": " + cfg.world.autosaveMinutes + ", // Options: 5 (frequent), 10 (current), 30 (light)\n" +
            "    \"staggerSaves\": " + cfg.world.staggerSaves + " // Options: true (current), false (single burst)\n" +
            "  },\n" +
            "  \"particles\": {\n" +
            "    \"densityScalar\": " + cfg.particles.densityScalar + ", // Options: 0.6 (low), 0.85 (current), 1.0 (full)\n" +
            "    \"applyToProjectiles\": " + cfg.particles.applyToProjectiles + " // Options: true (current), false (leave projectiles untouched)\n" +
            "  },\n" +
            "  \"bundled\": {\n" +
            "    \"aggressiveTrimming\": " + cfg.bundled.aggressiveTrimming + ", // Options: false (current), true (heavier culling)\n" +
            "    \"lowAiDepth\": " + cfg.bundled.lowAiDepth + " // Options: false (current), true (lighter AI for perf)\n" +
            "  }\n" +
            "}\n";
    }

    public synchronized boolean reloadConfigFromDisk() {
        try {
            cancelScheduledTasks();
            closeAsyncExecutor();
            this.config = loadConfig();
            initAsyncExecutor();
            scheduleAutosave();
            scheduleLightMetrics();
            scheduleEntityOptimizer();
            particleOptimizer.applyScalar(config.particles, LOGGER);
            LOGGER.atInfo().log("Reloaded MegaPerformance config successfully");
            return true;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to reload MegaPerformance config");
            return false;
        }
    }

    private void cancelScheduledTasks() {
        if (autosaveTask != null) {
            autosaveTask.cancel(false);
            autosaveTask = null;
        }
        if (metricsTask != null) {
            metricsTask.cancel(false);
            metricsTask = null;
        }
        if (entityTask != null) {
            entityTask.cancel(false);
            entityTask = null;
        }
    }

    private void closeAsyncExecutor() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdownNow();
            asyncExecutor = null;
        }
    }
}
