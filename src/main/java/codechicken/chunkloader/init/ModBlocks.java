package codechicken.chunkloader.init;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.block.ItemChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ModBlocks {

    public static BlockChunkLoader blockChunkLoader;
    public static ItemChunkLoader itemChunkLoader;

    public static void init() {
        blockChunkLoader = new BlockChunkLoader();
        blockChunkLoader.setRegistryName("chunk_loader");
        itemChunkLoader = new ItemChunkLoader(blockChunkLoader);
        itemChunkLoader.setRegistryName("chunk_loader");
        ForgeRegistries.BLOCKS.register(blockChunkLoader);
        ForgeRegistries.ITEMS.register(itemChunkLoader);
        GameRegistry.registerTileEntity(TileChunkLoader.class, ChickenChunks.MOD_ID + ":ChunkLoader");
        GameRegistry.registerTileEntity(TileSpotLoader.class, ChickenChunks.MOD_ID + ":SpotLoader");
    }

    @SideOnly (Side.CLIENT)
    public static void registerModels() {
        blockChunkLoader.registerModels();
    }

}
