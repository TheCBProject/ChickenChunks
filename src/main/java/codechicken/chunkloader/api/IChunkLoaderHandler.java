package codechicken.chunkloader.api;

import codechicken.chunkloader.world.ChunkLoaderHandler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.util.thread.EffectiveSide;

import java.util.Objects;
import java.util.Set;

/**
 * Handler for interacting with the ChickenChunks loading backend.
 * IChunkLoader implementations are expected to be TileEntities at this moment.
 * This Capability only exists on the {@link Level#OVERWORLD} and handles all dimensions.
 * Due to complexity limitations, it's not viable to move this to a per-world capability at the current moment.
 * <p>
 * Created by covers1624 on 5/4/20.
 */
public interface IChunkLoaderHandler {

    void addChunkLoader(IChunkLoader loader);

    void removeChunkLoader(IChunkLoader loader);

    boolean canLoadChunks(IChunkLoader loader, Set<ChunkPos> newChunks);

    void updateLoader(IChunkLoader loader);

    static IChunkLoaderHandler instance() {
        if (!EffectiveSide.get().isServer()) throw new IllegalStateException("ChunkLoaderHandler on exists on the client.");

        return Objects.requireNonNull(ChunkLoaderHandler.instance(), "ChunkLoaderHandler has not been created.");
    }
}
