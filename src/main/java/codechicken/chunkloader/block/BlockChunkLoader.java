package codechicken.chunkloader.block;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.client.ChunkLoaderItemModel;
import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.chunkloader.network.ChunkLoaderSPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.chunkloader.tile.TileSpotLoader;
import codechicken.lib.model.ModelRegistryHelper;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.util.ServerUtils;
import codechicken.lib.vec.Cuboid6;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockChunkLoader extends Block implements ITileEntityProvider {

    public static final PropertyEnum<Type> TYPE = PropertyEnum.create("type", Type.class);

    public BlockChunkLoader() {
        super(Material.ROCK);
        setHardness(20F);
        setDefaultState(getDefaultState().withProperty(TYPE, Type.BLOCK));
        setResistance(100F);
        setSoundType(SoundType.STONE);
        setUnlocalizedName(ChickenChunks.MOD_ID + ":chunk_loader");
        setCreativeTab(CreativeTabs.MISC);
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
        Type type = state.getValue(TYPE);
        return type.getBounds().aabb();
    }

    @Override
    public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return !world.getBlockState(pos).getValue(TYPE).equals(Type.SPOT) && side == EnumFacing.DOWN;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        Type type = state.getValue(TYPE);
        if (type == Type.SPOT || player.isSneaking()) {
            return false;
        }

        if (!world.isRemote) {
            TileChunkLoader tile = (TileChunkLoader) world.getTileEntity(pos);
            if (tile.owner == null || tile.owner.equals(player.getName()) || ChunkLoaderManager.canOpInteract() && ServerUtils.isPlayerOP(player.getName())) {
                PacketCustom packet = new PacketCustom(ChunkLoaderSPH.channel, 12);
                packet.writePos(pos);
                packet.sendToPlayer(player);
            } else {
                player.sendMessage(new TextComponentTranslation("chickenchunks.accessdenied"));
            }
        }
        return true;
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
    public void getSubBlocks(CreativeTabs creativeTab, NonNullList<ItemStack> list) {
        for (Type type : Type.VALUES) {
            list.add(new ItemStack(this, 1, type.getMetadata()));
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE).getMetadata();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, Type.byMetadata(meta));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE);
    }

    @SideOnly (Side.CLIENT)
    public void registerModels() {
        for (int i = 0; i < Type.values().length; i++) {
            Type type = Type.values()[i];
            ModelResourceLocation location = new ModelResourceLocation("chickenchunks:chunk_loader", "item-type=" + type.getName());
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), i, location);
            ModelRegistryHelper.register(location, new ChunkLoaderItemModel(() -> new ModelResourceLocation("chickenchunks:chunk_loader", "type=" + type.getName())));
        }
    }

    public static enum Type implements IStringSerializable {

        BLOCK(0, "block_loader", new Cuboid6(0, 0, 0, 1, 0.75F, 1)),
        SPOT(1, "spot_loader", new Cuboid6(0.25F, 0, 0.25F, 0.75F, 0.4375F, 0.75F));

        public static final Type[] VALUES = new Type[values().length];
        private final int metadata;
        private final String name;
        private final Cuboid6 bounds;

        Type(int meta, String name, Cuboid6 bounds) {

            this.metadata = meta;
            this.name = name;
            this.bounds = bounds;
        }

        public int getMetadata() {
            return metadata;
        }

        @Override
        public String getName() {
            return name;
        }

        public Cuboid6 getBounds() {
            return bounds;
        }

        public static Type byMetadata(int metadata) {
            if (metadata < 0 || metadata >= VALUES.length) {
                metadata = 0;
            }
            return VALUES[metadata];
        }

        static {
            for (Type type : values()) {
                VALUES[type.getMetadata()] = type;
            }
        }

    }

}
