package codechicken.chunkloader.world;

import codechicken.chunkloader.api.IChunkLoader;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 11/5/20.
 */
public class Organiser {

    //The dormant loaders.
    public final Set<BlockPos> dormantLoaders = new HashSet<>();
    //What loaders are loading any given chunk.
    public final Map<ChunkPos, List<IChunkLoader>> forcedChunksByChunk = new HashMap<>();
    //What chunks each loader is loading.
    public final Map<IChunkLoader, Set<ChunkPos>> forcedChunksByLoader = new HashMap<>();
    //Queue of un-forcing chunks for any given loader.
    public final Map<IChunkLoader, Object2IntMap<ChunkPos>> timedUnloadQueue = new HashMap<>();

    public final List<IChunkLoader> pendingLoaders = new LinkedList<>();

    private final ChunkLoaderHandler handler;
    public final ResourceLocation dim;
    public final UUID player;

    public boolean reviving;
    public boolean dormant = false;

    public Organiser(ChunkLoaderHandler handler, ResourceLocation dim, UUID player) {
        this.handler = handler;
        this.dim = dim;
        this.player = player;
    }

    public boolean isEmpty() {
        return dormantLoaders.isEmpty() && forcedChunksByLoader.isEmpty();
    }

    public CompoundTag write(CompoundTag tag) {
        tag.put("dormantLoaders", dormantLoaders.stream()
                .map(Organiser::writeBlockPos)
                .collect(Collectors.toCollection(ListTag::new))
        );
        tag.put("loaders", forcedChunksByLoader.keySet().stream()
                .map(IChunkLoader::pos)
                .map(Organiser::writeBlockPos)
                .collect(Collectors.toCollection(ListTag::new))
        );
        return tag;
    }

    public Organiser read(CompoundTag tag) {
        tag.getList("dormantLoaders", 10).stream()
                .map(e -> (CompoundTag) e)
                .map(Organiser::readBlockPos)
                .forEach(dormantLoaders::add);
        tag.getList("loaders", 10).stream()
                .map(e -> (CompoundTag) e)
                .map(Organiser::readBlockPos)
                .forEach(dormantLoaders::add);
        dormant = true;
        return this;
    }

    public void addChunkLoader(IChunkLoader loader) {
        if (reviving) {
            return;
        }
        if (dormant) {
            dormantLoaders.add(loader.pos());
        } else {
            pendingLoaders.add(loader);
        }
    }

    public void remChunkLoader(IChunkLoader loader) {
        if (dormant) {
            dormantLoaders.remove(loader.pos());
        } else {
            Set<ChunkPos> chunks = forcedChunksByLoader.remove(loader);
            if (chunks == null) {
                return;
            }
            unforceChunks(loader, chunks, true);
        }
    }

    public void forceChunks(IChunkLoader loader, Set<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            handler.addChunk(loader, dim, chunk);
            List<IChunkLoader> loaders = forcedChunksByChunk.computeIfAbsent(chunk, e -> new LinkedList<>());
            if (!loaders.contains(loader)) {
                loaders.add(loader);
            }
        }
        forcedChunksByLoader.put(loader, chunks);
    }

    public void unforceChunks(IChunkLoader loader, Set<ChunkPos> chunks, boolean remLoader) {
        for (ChunkPos chunk : chunks) {
            Object2IntMap<ChunkPos> unloadQueue = timedUnloadQueue.computeIfAbsent(loader, e -> new Object2IntOpenHashMap<>());
            unloadQueue.put(chunk, 100);
            List<IChunkLoader> loaders = forcedChunksByChunk.get(chunk);
            if (loaders != null) {
                loaders.remove(loader);
                if (loaders.isEmpty()) {
                    forcedChunksByChunk.remove(chunk);
                }
            }
        }
        if (!remLoader) {
            forcedChunksByLoader.get(loader).removeAll(chunks);
        }
    }

    public void devive() {
        if (dormant) {
            return;
        }
        for (IChunkLoader loader : ImmutableSet.copyOf(forcedChunksByLoader.keySet())) {
            dormantLoaders.add(loader.pos());
            handler.removeChunkLoader(loader);
        }
        dormant = true;
    }

    public void revive(ServerLevel world) {
        if (!dormant) {
            return;
        }
        dormant = false;
        if (dormantLoaders.isEmpty()) {
            return;
        }
        Set<BlockPos> dormantLoaders = new HashSet<>(this.dormantLoaders);
        this.dormantLoaders.clear();

        for (BlockPos pos : dormantLoaders) {
            reviving = true;
            BlockEntity tile = world.getBlockEntity(pos);
            reviving = false;
            if (tile instanceof IChunkLoader) {
                handler.addChunkLoader((IChunkLoader) tile);
            }
        }
    }

    public void updateLoader(IChunkLoader loader) {
        Set<ChunkPos> loadedChunks = forcedChunksByLoader.get(loader);
        if (loadedChunks == null) {
            addChunkLoader(loader);
            return;
        }
        HashSet<ChunkPos> oldChunks = new HashSet<>(loadedChunks);
        HashSet<ChunkPos> newChunks = new HashSet<>();
        for (ChunkPos chunk : loader.getChunks()) {
            if (!oldChunks.remove(chunk)) {
                newChunks.add(chunk);
            }
        }

        if (!oldChunks.isEmpty()) {
            unforceChunks(loader, oldChunks, false);
        }
        if (!newChunks.isEmpty()) {
            forceChunks(loader, newChunks);
        }
    }

    public void onTickEnd() {
        tickLoads();
        tickUnloads();
    }

    public void tickLoads() {
        if (!pendingLoaders.isEmpty()) {
            List<IChunkLoader> loaders = new ArrayList<>(pendingLoaders);
            pendingLoaders.clear();
            for (IChunkLoader loader : loaders) {
                forceChunks(loader, loader.getChunks());
            }
        }
    }

    public void tickUnloads() {
        timedUnloadQueue.entrySet().removeIf(loaderEntry -> {
            Object2IntMap<ChunkPos> map = loaderEntry.getValue();
            map.object2IntEntrySet().removeIf(chunkEntry -> {
                int ticks = chunkEntry.getIntValue();
                if (ticks <= 1) {
                    handler.remChunk(loaderEntry.getKey(), dim, chunkEntry.getKey());
                    return true;
                }
                chunkEntry.setValue(ticks - 1);
                return false;
            });
            return map.isEmpty();
        });
    }

    private static BlockPos readBlockPos(CompoundTag p_129240_) {
        return new BlockPos(p_129240_.getInt("X"), p_129240_.getInt("Y"), p_129240_.getInt("Z"));
    }

    private static CompoundTag writeBlockPos(BlockPos p_129225_) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putInt("X", p_129225_.getX());
        compoundtag.putInt("Y", p_129225_.getY());
        compoundtag.putInt("Z", p_129225_.getZ());
        return compoundtag;
    }
}
