package codechicken.chunkloader.block;

import codechicken.chunkloader.block.property.PropertyChunkLoaderType;
import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.chunkloader.network.ChunkLoaderSPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.chunkloader.tile.TileSpotLoader;
import codechicken.core.ServerUtils;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class BlockChunkLoader extends BlockContainer {

    public static final PropertyChunkLoaderType TYPE = PropertyChunkLoaderType.create("type");

    public BlockChunkLoader() {
        super(Material.rock);
        setHardness(20F);
        setDefaultState(getDefaultState().withProperty(TYPE, EnumChunkLoaderType.FULL));
        setResistance(100F);
        setSoundType(SoundType.STONE);
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        AxisAlignedBB currBox = getBoundingBoxForType(state.getValue(TYPE));
        return currBox != null ? currBox : super.getBoundingBox(state, source, pos);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entityIn) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, getBoundingBox(state, worldIn, pos));
    }

    @Override
    public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (world.getBlockState(pos).getValue(TYPE).equals(EnumChunkLoaderType.SPOT)) {
            return false;
        }

        return state.getValue(TYPE).isDown() ? side == EnumFacing.UP : side == EnumFacing.DOWN;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    public AxisAlignedBB getBoundingBoxForType(EnumChunkLoaderType type) {
        switch (type) {
        case FULL:
            return new AxisAlignedBB(0, 0, 0, 1, 0.75F, 1);
        case SPOT:
            return new AxisAlignedBB(0.25F, 0, 0.25F, 0.75F, 0.4375F, 0.75F);
        case FULL_DOWN:
            return new AxisAlignedBB(0, 0.25F, 0, 1, 1, 1);
        case SPOT_DOWN:
            return new AxisAlignedBB(0.25F, 0.5625F, 0.25F, 0.75F, 1, 0.75F);
        default:
            return null;
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        EnumChunkLoaderType type = state.getValue(TYPE);
        if (type.equals(EnumChunkLoaderType.SPOT) || player.isSneaking()) {
            return false;
        }

        if (!world.isRemote) {
            TileChunkLoader tile = (TileChunkLoader) world.getTileEntity(pos);
            if (tile.owner == null || tile.owner.equals(player.getName()) ||
                    ChunkLoaderManager.opInteract() && ServerUtils.isPlayerOP(player.getName())) {
                PacketCustom packet = new PacketCustom(ChunkLoaderSPH.channel, 12);
                packet.writeCoord(new BlockCoord(pos));
                packet.sendToPlayer(player);
            } else {
                player.addChatMessage(new TextComponentTranslation("chickenchunks.accessdenied"));
            }
        }
        return true;
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(TYPE, EnumChunkLoaderType.values()[facing == EnumFacing.DOWN ? meta + 2 : meta]);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote) {
            return;
        }
        TileChunkLoaderBase loader = (TileChunkLoaderBase) world.getTileEntity(pos);
        loader.onBlockPlacedBy(placer);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta > 1) {
            meta -= 2;
        }
        if (meta == 0) {
            return new TileChunkLoader();
        } else if (meta == 1) {
            return new TileSpotLoader();
        } else {
            return null;
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List<ItemStack> list) {
        list.add(new ItemStack(this, 1, 0));
        list.add(new ItemStack(this, 1, 1));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, EnumChunkLoaderType.values()[meta]);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
}
