package codechicken.chunkloader.init;

import codechicken.chunkloader.ChickenChunks;
import codechicken.lib.datagen.LootTableProvider;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.CompletableFuture;

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
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        
        gen.addProvider(event.includeServer(), new LootTables(output));
        gen.addProvider(event.includeServer(), new BlockTags(output, lookupProvider, files));
    }

    private static class LootTables extends LootTableProvider.BlockLootProvider {

    	protected LootTables(PackOutput output) {
            super(output.createPathProvider(null, null)); //TODO: Very much incorrect.
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
        public BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        	super(output, lookupProvider, ChickenChunks.MOD_ID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(ChickenChunksModContent.CHUNK_LOADER_BLOCK.get())
                    .add(ChickenChunksModContent.SPOT_LOADER_BLOCK.get());
        }
    }
}
