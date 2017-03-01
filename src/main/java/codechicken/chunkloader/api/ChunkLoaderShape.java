package codechicken.chunkloader.api;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.translation.I18n;

import java.util.HashSet;

public enum ChunkLoaderShape {
    Square("square"),
    Circle("circle"),
    LineX("linex"),
    LineZ("linez");

    String name;

    ChunkLoaderShape(String s) {
        name = s;
    }

    public HashSet<ChunkPos> getChunks(int radius, ChunkPos center) {
        HashSet<ChunkPos> chunkset = new HashSet<>();
        radius -= 1;
        switch (this) {
            case Square:
                for (int x = center.chunkXPos - radius; x <= center.chunkXPos + radius; x++) {
                    for (int z = center.chunkZPos - radius; z <= center.chunkZPos + radius; z++) {
                        chunkset.add(new ChunkPos(x, z));
                    }
                }
                break;
            case LineX:
                for (int x = center.chunkXPos - radius; x <= center.chunkXPos + radius; x++) {
                    chunkset.add(new ChunkPos(x, center.chunkZPos));
                }
                break;
            case LineZ:
                for (int z = center.chunkZPos - radius; z <= center.chunkZPos + radius; z++) {
                    chunkset.add(new ChunkPos(center.chunkXPos, z));
                }
                break;
            case Circle:
                for (int x = center.chunkXPos - radius; x <= center.chunkXPos + radius; x++) {
                    for (int z = center.chunkZPos - radius; z <= center.chunkZPos + radius; z++) {
                        int relx = x - center.chunkXPos;
                        int relz = z - center.chunkZPos;
                        double dist = Math.sqrt(relx * relx + relz * relz);
                        if (dist <= radius) {
                            chunkset.add(new ChunkPos(x, z));
                        }
                    }
                }
        }
        return chunkset;
    }

    public ChunkLoaderShape next() {
        int index = ordinal();
        index++;
        if (index == values().length) {
            index = 0;
        }
        return values()[index];
    }

    public ChunkLoaderShape prev() {
        int index = ordinal();
        index--;
        if (index == -1) {
            index = values().length - 1;
        }
        return values()[index];
    }

    public HashSet<ChunkPos> getLoadedChunks(int chunkx, int chunkz, int radius) {
        HashSet<ChunkPos> chunkSet = new HashSet<>();
        switch (this) {
            case Square:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                        chunkSet.add(new ChunkPos(cx, cz));
                    }
                }
                break;
            case LineX:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    chunkSet.add(new ChunkPos(cx, chunkz));
                }
                break;
            case LineZ:
                for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                    chunkSet.add(new ChunkPos(chunkx, cz));
                }
                break;
            case Circle:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                        double distSquared = (cx - chunkx) * (cx - chunkx) + (cz - chunkz) * (cz - chunkz);
                        if (distSquared <= radius * radius) {
                            chunkSet.add(new ChunkPos(cx, cz));
                        }
                    }
                }

        }
        return chunkSet;
    }

    public String getName() {
        return I18n.translateToLocal("chickenchunks.shape." + name);
    }
}
