package codechicken.chunkloader.init;

import codechicken.chunkloader.ChickenChunks;
import codechicken.lib.datagen.LootTableProvider;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;

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
        gen.addProvider(event.includeServer(), new LootTables(gen));
        gen.addProvider(event.includeServer(), new BlockTags(gen, event.getExistingFileHelper()));
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

    private static class BlockTags extends BlockTagsProvider {
        public BlockTags(DataGenerator dataGeneratorIn, @Nullable ExistingFileHelper existingFileHelper) {
            super(dataGeneratorIn, ChickenChunks.MOD_ID, existingFileHelper);
        }

        @Override
        protected void addTags() {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(ChickenChunksModContent.CHUNK_LOADER_BLOCK.get())
                    .add(ChickenChunksModContent.SPOT_LOADER_BLOCK.get());
        }
    }
}
