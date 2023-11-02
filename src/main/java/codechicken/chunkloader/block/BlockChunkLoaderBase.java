package codechicken.chunkloader.block;

import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import static codechicken.chunkloader.network.ChickenChunksNetwork.C_OPEN_LOADER_GUI;
import static codechicken.chunkloader.network.ChickenChunksNetwork.NET_CHANNEL;

public abstract class BlockChunkLoaderBase extends BaseEntityBlock {

    public BlockChunkLoaderBase() {
        super(Block.Properties.of()
                .strength(20F, 100F)
                .sound(SoundType.STONE)
        );
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!world.isClientSide) {
            TileChunkLoader tile = (TileChunkLoader) world.getBlockEntity(pos);
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

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (world.isClientSide) {
            return;
        }
        TileChunkLoaderBase loader = (TileChunkLoaderBase) world.getBlockEntity(pos);
        loader.onBlockPlacedBy(placer);
    }

    @Override
    public RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.MODEL;
    }
}
