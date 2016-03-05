package codechicken.chunkloader.init;

import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.item.ItemChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ModBlocks {

    public static BlockChunkLoader blockChunkLoader;

    public static void init() {
        blockChunkLoader = new BlockChunkLoader();
        blockChunkLoader.setUnlocalizedName("chickenChunkLoader").setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerBlock(blockChunkLoader, ItemChunkLoader.class, "chickenChunkLoader");
        GameRegistry.registerTileEntity(TileChunkLoader.class, "ChickenChunkLoader");
        GameRegistry.registerTileEntity(TileSpotLoader.class, "ChickenSpotLoader");
    }

}
