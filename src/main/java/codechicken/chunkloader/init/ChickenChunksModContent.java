package codechicken.chunkloader.init;

import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.block.BlockSpotLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

/**
 * Created by covers1624 on 2/11/19.
 */
public class ChickenChunksModContent {

    private static final CrashLock LOCK = new CrashLock("Already Initialized");
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MOD_ID);

    //region Blocks
    public static final RegistryObject<BlockChunkLoader> CHUNK_LOADER_BLOCK = BLOCKS.register("chunk_loader", BlockChunkLoader::new);
    public static final RegistryObject<BlockSpotLoader> SPOT_LOADER_BLOCK = BLOCKS.register("spot_loader", BlockSpotLoader::new);
    //endregion

    //region Items
    private static final Item.Properties itemProps = new Item.Properties().tab(CreativeModeTab.TAB_MISC);
    public static final RegistryObject<BlockItem> CHUNK_LOADER_ITEM = ITEMS.register("chunk_loader", () -> new BlockItem(CHUNK_LOADER_BLOCK.get(), itemProps));
    public static final RegistryObject<BlockItem> SPOT_LOADER_ITEM = ITEMS.register("spot_loader", () -> new BlockItem(SPOT_LOADER_BLOCK.get(), itemProps));
    //endregion

    //region TileTypes
    public static final RegistryObject<BlockEntityType<TileChunkLoader>> CHUNK_LOADER_TILE = TILES.register("chunk_loader", () ->
            BlockEntityType.Builder.of(TileChunkLoader::new, CHUNK_LOADER_BLOCK.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<TileSpotLoader>> SPOT_LOADER_TILE = TILES.register("spot_loader", () ->
            BlockEntityType.Builder.of(TileSpotLoader::new, SPOT_LOADER_BLOCK.get()).build(null)
    );
    //endregion

    public static void init() {
        LOCK.lock();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILES.register(bus);
    }
}
