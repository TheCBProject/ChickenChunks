package codechicken.chunkloader;

import java.util.HashSet;

import net.minecraft.util.StatCollector;
import net.minecraft.world.ChunkCoordIntPair;

public enum ChunkLoaderShape
{
    Square("square"),
    Circle("circle"),
    LineX("linex"),
    LineZ("linez");

    String name;

    private ChunkLoaderShape(String s) {
        name = s;
    }

    public HashSet<ChunkCoordIntPair> getChunks(int radius, ChunkCoordIntPair center) {
        HashSet<ChunkCoordIntPair> chunkset = new HashSet<ChunkCoordIntPair>();
        radius -= 1;
        switch (this) {
            case Square:
                for (int x = center.chunkXPos - radius; x <= center.chunkXPos + radius; x++) {
                    for (int z = center.chunkZPos - radius; z <= center.chunkZPos + radius; z++) {
                        chunkset.add(new ChunkCoordIntPair(x, z));
                    }
                }
                break;
            case LineX:
                for (int x = center.chunkXPos - radius; x <= center.chunkXPos + radius; x++) {
                    chunkset.add(new ChunkCoordIntPair(x, center.chunkZPos));
                }
                break;
            case LineZ:
                for (int z = center.chunkZPos - radius; z <= center.chunkZPos + radius; z++) {
                    chunkset.add(new ChunkCoordIntPair(center.chunkXPos, z));
                }
                break;
            case Circle:
                for (int x = center.chunkXPos - radius; x <= center.chunkXPos + radius; x++) {
                    for (int z = center.chunkZPos - radius; z <= center.chunkZPos + radius; z++) {
                        int relx = x - center.chunkXPos;
                        int relz = z - center.chunkZPos;
                        double dist = Math.sqrt(relx * relx + relz * relz);
                        if (dist <= radius)
                            chunkset.add(new ChunkCoordIntPair(x, z));
                    }
                }
        }
        return chunkset;
    }

    public ChunkLoaderShape next() {
        int index = ordinal();
        index++;
        if (index == values().length)
            index = 0;
        return values()[index];
    }

    public ChunkLoaderShape prev() {
        int index = ordinal();
        index--;
        if (index == -1)
            index = values().length - 1;
        return values()[index];
    }

    public HashSet<ChunkCoordIntPair> getLoadedChunks(int chunkx, int chunkz, int radius) {
        HashSet<ChunkCoordIntPair> chunkSet = new HashSet<ChunkCoordIntPair>();
        switch (this) {
            case Square:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                        chunkSet.add(new ChunkCoordIntPair(cx, cz));
                    }
                }
                break;
            case LineX:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    chunkSet.add(new ChunkCoordIntPair(cx, chunkz));
                }
                break;
            case LineZ:
                for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                    chunkSet.add(new ChunkCoordIntPair(chunkx, cz));
                }
                break;
            case Circle:
                for (int cx = chunkx - radius; cx <= chunkx + radius; cx++) {
                    for (int cz = chunkz - radius; cz <= chunkz + radius; cz++) {
                        double distSquared = (cx - chunkx) * (cx - chunkx) + (cz - chunkz) * (cz - chunkz);
                        if (distSquared <= radius * radius)
                            chunkSet.add(new ChunkCoordIntPair(cx, cz));
                    }
                }

        }
        return chunkSet;
    }

    public String getName() {
        return StatCollector.translateToLocal("chickenchunks.shape." + name);
    }
}
