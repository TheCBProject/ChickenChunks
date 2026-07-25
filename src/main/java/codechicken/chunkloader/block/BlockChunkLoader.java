package codechicken.chunkloader.block;

import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.tile.TileChunkLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * Created by covers1624 on 11/4/22.
 */
public class BlockChunkLoader extends BlockChunkLoaderBase {

    public static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.75F, 1);

    public BlockChunkLoader(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileChunkLoader(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (world.isClientSide() || !(player instanceof ServerPlayer sPlayer)) return InteractionResult.SUCCESS;

        if (!(world.getBlockEntity(pos) instanceof TileChunkLoader tile)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (tile.owner == null) {
            sPlayer.sendSystemMessage(Component.translatable("chickenchunks.brokentile"));
        } else if (tile.owner.equals(player.getUUID()) || ChickenChunksConfig.doesBypassLoaderAccess(sPlayer)) {
            var packet = ChickenChunksNetwork.OPEN_LOADER_GUI.toClient(sPlayer);
            packet.writeBlockPos(pos);
            packet.sendToPlayer(sPlayer);
        } else {
            sPlayer.sendSystemMessage(Component.translatable("chickenchunks.accessdenied"));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual) {
        if (level.isClientSide()) {
            return createTickerHelper(actual, ChickenChunksModContent.CHUNK_LOADER_TILE.get(), (a, b, c, d) -> d.tickClient());
        }
        return createTickerHelper(actual, ChickenChunksModContent.CHUNK_LOADER_TILE.get(), (a, b, c, d) -> d.tickServer());
    }
}
