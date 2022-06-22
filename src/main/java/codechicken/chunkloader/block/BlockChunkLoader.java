package codechicken.chunkloader.block;

import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.chunkloader.tile.TileChunkLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 11/4/22.
 */
public class BlockChunkLoader extends BlockChunkLoaderBase {

    public static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.75F, 1);

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileChunkLoader(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual) {
        if (level.isClientSide) {
            return createTickerHelper(actual, ChickenChunksModContent.CHUNK_LOADER_TILE.get(), (a, b, c, d) -> d.tickClient());
        }
        return createTickerHelper(actual, ChickenChunksModContent.CHUNK_LOADER_TILE.get(), (a, b, c, d) -> d.tickServer());
    }
}
