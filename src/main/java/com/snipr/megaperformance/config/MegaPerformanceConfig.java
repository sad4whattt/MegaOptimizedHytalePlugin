package com.snipr.megaperformance.config;

public class MegaPerformanceConfig {
    public Tick tick = new Tick();
    public Async async = new Async();
    public Entities entities = new Entities();
    public Network network = new Network();
    public World world = new World();
    public Particles particles = new Particles();
    public Bundled bundled = new Bundled();

    public static class Tick {
        public int maxCatchupMillis = 50;
        public int jitterClampMs = 10;
    }

    public static class Async {
        public int workerThreads = 2;
        public int maxQueue = 512;
    }

    public static class Entities {
        public int softCapPerChunk = 35;
        public int distantAiSkipEvery = 4;
    }

    public static class Network {
        public int packetBurstLimit = 256;
        public int coalesceChatMs = 75;
    }

    public static class World {
        public int autosaveMinutes = 10;
        public boolean staggerSaves = true;
    }

    public static class Particles {
        public double densityScalar = 0.85;
        public boolean applyToProjectiles = true;
    }

    public static class Bundled {
        public boolean aggressiveTrimming = false;
        public boolean lowAiDepth = false;
    }
}
