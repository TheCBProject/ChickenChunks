package codechicken.chunkloader.block;

import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

import static codechicken.chunkloader.network.ChickenChunksNetwork.*;

public class BlockChunkLoader extends Block {

    public static final VoxelShape SHAPE = VoxelShapes.create(0, 0, 0, 1, 0.75F, 1);

    public BlockChunkLoader() {
        super(Block.Properties.create(Material.ROCK)//
                .hardnessAndResistance(20F, 100F)//
                .sound(SoundType.STONE)//
        );
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileChunkLoader();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!world.isRemote) {
            TileChunkLoader tile = (TileChunkLoader) world.getTileEntity(pos);
            if (tile.owner == null) {
                player.sendMessage(new TranslationTextComponent("chickenchunks.brokentile"), Util.DUMMY_UUID);
            } else if (tile.owner.equals(player.getUniqueID()) || ChickenChunksConfig.doesBypassLoaderAccess((ServerPlayerEntity) player)) {
                PacketCustom packet = new PacketCustom(NET_CHANNEL, C_OPEN_LOADER_GUI);
                packet.writePos(pos);
                packet.sendToPlayer((ServerPlayerEntity) player);
            } else {
                player.sendMessage(new TranslationTextComponent("chickenchunks.accessdenied"), Util.DUMMY_UUID);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (world.isRemote) {
            return;
        }
        TileChunkLoaderBase loader = (TileChunkLoaderBase) world.getTileEntity(pos);
        loader.onBlockPlacedBy(placer);
    }
}
