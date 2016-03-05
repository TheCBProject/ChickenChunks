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
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class BlockChunkLoader extends BlockContainer {

    //@SideOnly(Side.CLIENT)
    //IIcon[][] icons;
    public static final PropertyChunkLoaderType TYPE = PropertyChunkLoaderType.create("type");

    public BlockChunkLoader() {
        super(Material.rock);
        setHardness(20F);
        setDefaultState(getDefaultState().withProperty(TYPE, EnumChunkLoaderType.FULL));
        setResistance(100F);
        setStepSound(soundTypeStone);
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
        setBlockBoundsForItemRender(world.getBlockState(pos).getValue(TYPE));
    }

    @Override
    public void addCollisionBoxesToList(World world, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        setBlockBoundsForItemRender(state.getValue(TYPE));
        super.addCollisionBoxesToList(world, pos, state, mask, list, collidingEntity);
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (world.getBlockState(pos).getValue(TYPE).equals(EnumChunkLoaderType.ONE)) {
            return false;
        }

        return side == EnumFacing.DOWN;
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    public void setBlockBoundsForItemRender(EnumChunkLoaderType type) {
        switch (type) {
        case FULL:
            setBlockBounds(0, 0, 0, 1, 0.75F, 1);
            break;
        case ONE:
            setBlockBounds(0.25F, 0, 0.25F, 0.75F, 0.4375F, 0.75F);
            break;
        }
    }

    //@Override
    //public IIcon getIcon(int side, int meta) {
    //    return icons[meta][side > 2 ? 2 : side];
    //}

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        EnumChunkLoaderType type = state.getValue(TYPE);
        if (type.equals(EnumChunkLoaderType.ONE) || player.isSneaking()) {
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
                player.addChatMessage(new ChatComponentTranslation("chickenchunks.accessdenied"));
            }
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote) {
            return;
        }

        TileChunkLoaderBase ctile = (TileChunkLoaderBase) world.getTileEntity(pos);
        ctile.onBlockPlacedBy(placer);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta == 0) {
            return new TileChunkLoader();
        } else if (meta == 1) {
            return new TileSpotLoader();
        } else {
            return null;
        }
    }

    //@Override
    //public void registerBlockIcons(IIconRegister par1IconRegister) {
    //    icons = new IIcon[2][3];
    //    for (int m = 0; m < icons.length; m++)
    //        for (int i = 0; i < icons[m].length; i++)
    //            icons[m][i] = par1IconRegister.registerIcon("chickenchunks:block_" + m + "_" + i);
    //}

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List list) {
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
    protected BlockState createBlockState() {
        return new BlockState(this, TYPE);
    }

    //@Override
    //@SideOnly(Side.CLIENT)
    //public int getRenderType() {
    //    return ChunkLoaderSBRH.renderID;
    //}
}
