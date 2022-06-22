package codechicken.chunkloader.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public interface IChunkLoader {

    @Nullable
    UUID getOwner();

    Level world();

    BlockPos pos();

    void deactivate();

    void activate();

    boolean isValid();

    Set<ChunkPos> getChunks();
}
