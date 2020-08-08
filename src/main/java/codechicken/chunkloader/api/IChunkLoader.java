package codechicken.chunkloader.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Set;
import java.util.UUID;

public interface IChunkLoader {

    UUID getOwner();

    @Deprecated
    String getMod();

    World world();

    BlockPos pos();

    void deactivate();

    void activate();

    boolean isValid();

    Set<ChunkPos> getChunks();
}
