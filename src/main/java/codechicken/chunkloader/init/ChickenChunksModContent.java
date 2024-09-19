package codechicken.chunkloader.init;

import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.block.BlockSpotLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

/**
 * Created by covers1624 on 2/11/19.
 */
public class ChickenChunksModContent {

    private static final CrashLock LOCK = new CrashLock("Already Initialized");
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    //region Blocks
    public static final DeferredHolder<Block, BlockChunkLoader> CHUNK_LOADER_BLOCK = BLOCKS.register("chunk_loader", BlockChunkLoader::new);
    public static final DeferredHolder<Block, BlockSpotLoader> SPOT_LOADER_BLOCK = BLOCKS.register("spot_loader", BlockSpotLoader::new);
    //endregion

    //region Items
    private static final Item.Properties itemProps = new Item.Properties();
    public static final DeferredHolder<Item, BlockItem> CHUNK_LOADER_ITEM = ITEMS.register("chunk_loader", () -> new BlockItem(CHUNK_LOADER_BLOCK.get(), itemProps));
    public static final DeferredHolder<Item, BlockItem> SPOT_LOADER_ITEM = ITEMS.register("spot_loader", () -> new BlockItem(SPOT_LOADER_BLOCK.get(), itemProps));
    //endregion

    //region TileTypes
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChunkLoader>> CHUNK_LOADER_TILE = TILES.register("chunk_loader", () ->
            BlockEntityType.Builder.of(TileChunkLoader::new, CHUNK_LOADER_BLOCK.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpotLoader>> SPOT_LOADER_TILE = TILES.register("spot_loader", () ->
            BlockEntityType.Builder.of(TileSpotLoader::new, SPOT_LOADER_BLOCK.get()).build(null)
    );
    //endregion

    public static void init(IEventBus modBus) {
        LOCK.lock();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TILES.register(modBus);
        modBus.addListener(ChickenChunksModContent::onCreativeTabBuild);
    }

    private static void onCreativeTabBuild(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CHUNK_LOADER_BLOCK.get());
            event.accept(SPOT_LOADER_BLOCK.get());
        }
    }
}
