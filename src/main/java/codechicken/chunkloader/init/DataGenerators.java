package codechicken.chunkloader.init;

import codechicken.lib.datagen.LootTableProvider;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

/**
 * Created by covers1624 on 7/10/20.
 */
public class DataGenerators {

    private static final CrashLock LOCK = new CrashLock("Already initialized.");

    public static void init() {
        LOCK.lock();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(DataGenerators::gatherDataGenerators);
    }

    private static void gatherDataGenerators(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(new LootTables(gen));
    }

    private static class LootTables extends LootTableProvider.BlockLootProvider {

        public LootTables(DataGenerator dataGeneratorIn) {
            super(dataGeneratorIn);
        }

        @Override
        protected void registerTables() {
            register(ChickenChunksModContent.CHUNK_LOADER_BLOCK.get(), singleItem(ChickenChunksModContent.CHUNK_LOADER_BLOCK.get()));
            register(ChickenChunksModContent.SPOT_LOADER_BLOCK.get(), singleItem(ChickenChunksModContent.SPOT_LOADER_BLOCK.get()));
        }

        @Override
        public String getName() {
            return "ChickenChunks Block Loot";
        }
    }
}
