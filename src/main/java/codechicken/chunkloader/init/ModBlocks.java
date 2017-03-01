package codechicken.chunkloader.init;

import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.block.EnumChunkLoaderType;
import codechicken.chunkloader.item.ItemChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ModBlocks {

    public static BlockChunkLoader blockChunkLoader;

    public static void init() {
        blockChunkLoader = new BlockChunkLoader();
        blockChunkLoader.setUnlocalizedName("chickenChunkLoader").setCreativeTab(CreativeTabs.MISC);
        GameRegistry.register(blockChunkLoader.setRegistryName("chickenChunkLoader"));
        GameRegistry.register(new ItemChunkLoader(blockChunkLoader).setRegistryName("chickenChunkLoader"));
        GameRegistry.registerTileEntity(TileChunkLoader.class, "ChickenChunkLoader");
        GameRegistry.registerTileEntity(TileSpotLoader.class, "ChickenSpotLoader");
    }

    @SideOnly(Side.CLIENT)
    public static void initModelVariants() {
        for (int i = 0; i < EnumChunkLoaderType.values().length; i++) {
            EnumChunkLoaderType type = EnumChunkLoaderType.values()[i];
            ModelResourceLocation location = new ModelResourceLocation("chickenchunks:chickenchunkloader", "type=" + type.getName());
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockChunkLoader), i, location);
        }
    }

}
