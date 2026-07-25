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
import net.minecraft.world.level.block.SoundType;
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
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    //region Blocks
    public static final DeferredHolder<Block, BlockChunkLoader> CHUNK_LOADER_BLOCK = BLOCKS.registerBlock(
            "chunk_loader",
            BlockChunkLoader::new,
            e -> e.strength(20F, 100F)
                    .sound(SoundType.STONE)
    );
    public static final DeferredHolder<Block, BlockSpotLoader> SPOT_LOADER_BLOCK = BLOCKS.registerBlock(
            "spot_loader",
            BlockSpotLoader::new,
            e -> e.strength(20F, 100F)
                    .sound(SoundType.STONE)
    );
    //endregion

    //region Items
    public static final DeferredHolder<Item, BlockItem> CHUNK_LOADER_ITEM = ITEMS.registerItem("chunk_loader", e -> new BlockItem(CHUNK_LOADER_BLOCK.get(), e));
    public static final DeferredHolder<Item, BlockItem> SPOT_LOADER_ITEM = ITEMS.registerItem("spot_loader", e -> new BlockItem(SPOT_LOADER_BLOCK.get(), e));
    //endregion

    //region TileTypes
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChunkLoader>> CHUNK_LOADER_TILE = TILES.register("chunk_loader", () ->
            new BlockEntityType<>(TileChunkLoader::new, CHUNK_LOADER_BLOCK.get())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpotLoader>> SPOT_LOADER_TILE = TILES.register("spot_loader", () ->
            new BlockEntityType<>(TileSpotLoader::new, SPOT_LOADER_BLOCK.get())
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
