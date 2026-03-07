package kr.co.voxeliver.server;

import com.badlogic.gdx.math.Vector3;

public class ServerConfig {
    public int port = 25565;
    public int tickRate = 20;
    public String worldPath = "saves/server-world";
    public Vector3 spawnPosition = new Vector3(0f, 0f, 0f);
    public int initialChunkRadius = 4;
    public int chunkPreloadRadius = 2;
    public int visibleChunkRadius = 4;
    public int keepLoadedChunkRadius = 6;
    public int maxLoadedChunks = 256;
    public int defaultGroundBlockType = 0;
    public int flatWorldFloorY = -3;
    public int flatWorldSurfaceY = 0;
    public long keepAliveTimeoutMillis = 15_000L;
    public float maxMoveDistancePerRequest = 2.5f;
    public float interactionRange = 6f;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ServerConfig config = new ServerConfig();

        public Builder port(int port) {
            config.port = port;
            return this;
        }

        public Builder tickRate(int tickRate) {
            config.tickRate = tickRate;
            return this;
        }

        public Builder worldPath(String worldPath) {
            config.worldPath = worldPath;
            return this;
        }

        public Builder spawnPosition(float x, float y, float z) {
            config.spawnPosition = new Vector3(x, y, z);
            return this;
        }

        public Builder initialChunkRadius(int initialChunkRadius) {
            config.initialChunkRadius = initialChunkRadius;
            return this;
        }

        public Builder chunkPreloadRadius(int chunkPreloadRadius) {
            config.chunkPreloadRadius = chunkPreloadRadius;
            return this;
        }

        public Builder visibleChunkRadius(int visibleChunkRadius) {
            config.visibleChunkRadius = visibleChunkRadius;
            return this;
        }

        public Builder keepLoadedChunkRadius(int keepLoadedChunkRadius) {
            config.keepLoadedChunkRadius = keepLoadedChunkRadius;
            return this;
        }

        public Builder maxLoadedChunks(int maxLoadedChunks) {
            config.maxLoadedChunks = maxLoadedChunks;
            return this;
        }

        public Builder defaultGroundBlockType(int defaultGroundBlockType) {
            config.defaultGroundBlockType = defaultGroundBlockType;
            return this;
        }

        public Builder flatWorldFloorY(int flatWorldFloorY) {
            config.flatWorldFloorY = flatWorldFloorY;
            return this;
        }

        public Builder flatWorldSurfaceY(int flatWorldSurfaceY) {
            config.flatWorldSurfaceY = flatWorldSurfaceY;
            return this;
        }

        public Builder keepAliveTimeoutMillis(long keepAliveTimeoutMillis) {
            config.keepAliveTimeoutMillis = keepAliveTimeoutMillis;
            return this;
        }

        public Builder maxMoveDistancePerRequest(float maxMoveDistancePerRequest) {
            config.maxMoveDistancePerRequest = maxMoveDistancePerRequest;
            return this;
        }

        public Builder interactionRange(float interactionRange) {
            config.interactionRange = interactionRange;
            return this;
        }

        public ServerConfig build() {
            if (config.tickRate <= 0) {
                throw new IllegalArgumentException("tickRate must be positive");
            }
            if (config.initialChunkRadius < 0 || config.chunkPreloadRadius < 0) {
                throw new IllegalArgumentException("chunk radii must be non-negative");
            }
            if (config.keepLoadedChunkRadius < config.visibleChunkRadius) {
                throw new IllegalArgumentException("keepLoadedChunkRadius must be >= visibleChunkRadius");
            }
            if (config.flatWorldFloorY > config.flatWorldSurfaceY) {
                throw new IllegalArgumentException("flatWorldFloorY must be <= flatWorldSurfaceY");
            }
            if (config.maxMoveDistancePerRequest <= 0f) {
                throw new IllegalArgumentException("maxMoveDistancePerRequest must be positive");
            }
            if (config.interactionRange <= 0f) {
                throw new IllegalArgumentException("interactionRange must be positive");
            }
            return config;
        }
    }
}
