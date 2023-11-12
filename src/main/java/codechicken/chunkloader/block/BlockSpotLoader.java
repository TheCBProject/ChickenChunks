package codechicken.chunkloader.block;

import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.chunkloader.tile.TileSpotLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 2/11/19.
 */
public class BlockSpotLoader extends BlockChunkLoaderBase {

    public static final VoxelShape SHAPE = Shapes.box(0.25F, 0, 0.25F, 0.75F, 0.4375F, 0.75F);

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return new TileSpotLoader(p_153215_, p_153216_);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual) {
        if (level.isClientSide) {
            return createTickerHelper(actual, ChickenChunksModContent.SPOT_LOADER_TILE.get(), (a, b, c, d) -> d.tickClient());
        }
        return createTickerHelper(actual, ChickenChunksModContent.SPOT_LOADER_TILE.get(), (a, b, c, d) -> d.tickServer());
    }
}
