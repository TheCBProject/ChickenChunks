package codechicken.chunkloader.init;

import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.block.BlockSpotLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

/**
 * Created by covers1624 on 2/11/19.
 */
@ObjectHolder (MOD_ID)
@Mod.EventBusSubscriber (modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContent {

    //region Blocks
    @ObjectHolder ("chunk_loader")
    public static BlockChunkLoader blockChunkLoader;

    @ObjectHolder ("spot_loader")
    public static BlockChunkLoader blockSpotLoader;
    //endregion

    //region Blocks
    @ObjectHolder ("chunk_loader")
    public static BlockItem itemChunkLoader;

    @ObjectHolder ("spot_loader")
    public static BlockItem itemSpotLoader;
    //endregion

    //region TileTypes
    @ObjectHolder ("chunk_loader")
    public static TileEntityType<TileChunkLoader> tileChunkLoaderType;

    @ObjectHolder ("spot_loader")
    public static TileEntityType<TileSpotLoader> tileSpotLoaderType;
    //endregion

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        registry.register(new BlockChunkLoader().setRegistryName("chunk_loader"));
        registry.register(new BlockSpotLoader().setRegistryName("spot_loader"));
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        Item.Properties props = new Item.Properties().group(ItemGroup.MISC);
        registry.register(new BlockItem(blockChunkLoader, props).setRegistryName("chunk_loader"));
        registry.register(new BlockItem(blockSpotLoader, props).setRegistryName("spot_loader"));
    }

    @SubscribeEvent
    public static void onRegisterTiles(RegistryEvent.Register<TileEntityType<?>> event) {
        IForgeRegistry<TileEntityType<?>> registry = event.getRegistry();
        registry.register(TileEntityType.Builder.create(TileChunkLoader::new, blockChunkLoader).build(null).setRegistryName("chunk_loader"));
        registry.register(TileEntityType.Builder.create(TileSpotLoader::new, blockSpotLoader).build(null).setRegistryName("spot_loader"));
    }

}
