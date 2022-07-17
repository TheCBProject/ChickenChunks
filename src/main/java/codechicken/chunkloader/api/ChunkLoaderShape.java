package codechicken.chunkloader.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ChunkLoaderShape {
    SQUARE("square"),
    CIRCLE("circle"),
    LINE_X("linex"),
    LINE_Z("linez");

    private final String name;
    private static final ChunkLoaderShape[] VALUES = values();
    private static final ChunkLoaderShape[] NEXT_LOOKUP = Arrays.stream(VALUES)//
            .map(e -> VALUES[(e.ordinal() + 1) % VALUES.length])//
            .toArray(ChunkLoaderShape[]::new);
    private static final ChunkLoaderShape[] PREV_LOOKUP = Arrays.stream(VALUES)//
            .map(e -> VALUES[Math.floorMod((e.ordinal() - 1), VALUES.length)])//
            .toArray(ChunkLoaderShape[]::new);

    ChunkLoaderShape(String s) {
        name = s;
    }

    public ChunkLoaderShape next() {
        return NEXT_LOOKUP[ordinal()];
    }

    public ChunkLoaderShape prev() {
        return PREV_LOOKUP[ordinal()];
    }

    public Set<ChunkPos> getLoadedChunks(int chunkx, int chunkz, int radius) {
        Set<ChunkPos> chunks = new HashSet<>();
        switch (this) {
            case SQUARE:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                        chunks.add(new ChunkPos(cx, cz));
                    }
                }
                break;
            case LINE_X:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    chunks.add(new ChunkPos(cx, chunkz));
                }
                break;
            case LINE_Z:
                for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                    chunks.add(new ChunkPos(chunkx, cz));
                }
                break;
            case CIRCLE:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                        double distSquared = (cx - chunkx) * (cx - chunkx) + (cz - chunkz) * (cz - chunkz);
                        if (distSquared <= radius * radius) {
                            chunks.add(new ChunkPos(cx, cz));
                        }
                    }
                }

        }
        return chunks;
    }

    public Component getTranslation() {
        return Component.translatable("chickenchunks.shape." + name);
    }
}
