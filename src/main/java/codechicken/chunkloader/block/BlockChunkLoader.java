package codechicken.chunkloader.block;

import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import static codechicken.chunkloader.network.ChickenChunksNetwork.C_OPEN_LOADER_GUI;
import static codechicken.chunkloader.network.ChickenChunksNetwork.NET_CHANNEL;

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

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!world.isClientSide) {
            if (!(world.getBlockEntity(pos) instanceof TileChunkLoader tile)) return InteractionResult.PASS;
            if (tile.owner == null) {
                player.sendSystemMessage(Component.translatable("chickenchunks.brokentile"));
            } else if (tile.owner.equals(player.getUUID()) || ChickenChunksConfig.doesBypassLoaderAccess((ServerPlayer) player)) {
                PacketCustom packet = new PacketCustom(NET_CHANNEL, C_OPEN_LOADER_GUI);
                packet.writePos(pos);
                packet.sendToPlayer((ServerPlayer) player);
            } else {
                player.sendSystemMessage(Component.translatable("chickenchunks.accessdenied"));
            }
        }
        return InteractionResult.SUCCESS;
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
