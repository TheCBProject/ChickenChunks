package codechicken.chunkloader.init;

import codechicken.chunkloader.client.ChunkLoaderPearlItemRenderer;
import codechicken.lib.datagen.NoValidationBLockLootSubProvider;
import codechicken.lib.datagen.recipe.RecipeProvider;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;
import static codechicken.chunkloader.init.ChickenChunksModContent.*;

/**
 * Created by covers1624 on 7/10/20.
 */
public class DataGenerators {

    private static final CrashLock LOCK = new CrashLock("Already initialized.");

    public static void init(IEventBus modBus) {
        LOCK.lock();

        if (FMLEnvironment.getDist().isClient()) {
            modBus.addListener(DataGenerators::gatherDataGenerators);
        }
    }

    private static void gatherDataGenerators(GatherDataEvent.Client event) {
        event.createProvider(LootTables::new);
        event.createProvider(BlockTags::new);
        event.createProvider(Recipes::new);
        event.createProvider(Models::new);
    }

    private static class LootTables extends LootTableProvider {

        public LootTables(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(
                    output,
                    Set.of(),
                    List.of(
                            new SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)
                    ),
                    lookupProvider
            );
        }
    }

    private static class BlockLoot extends NoValidationBLockLootSubProvider {

        public BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(
                    CHUNK_LOADER_ITEM.get(),
                    SPOT_LOADER_ITEM.get()
            ), registries);
        }

        @Override
        protected void generate() {
            dropSelf(CHUNK_LOADER_BLOCK.get());
            dropSelf(SPOT_LOADER_BLOCK.get());
        }
    }

    private static class BlockTags extends BlockTagsProvider {

        public BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(CHUNK_LOADER_BLOCK.get())
                    .add(SPOT_LOADER_BLOCK.get());
        }
    }

    private static class Recipes extends RecipeProvider {

        public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> recipes) {
            super(output, recipes, MOD_ID);
        }

        @Override
        protected void registerRecipes() {
            shapedRecipe(CHUNK_LOADER_ITEM.get())
                    .key('P', Tags.Items.ENDER_PEARLS)
                    .key('G', Tags.Items.INGOTS_GOLD)
                    .key('E', Items.ENCHANTING_TABLE)
                    .patternLine(" P ")
                    .patternLine("GGG")
                    .patternLine("GEG");

            shapedRecipe(SPOT_LOADER_ITEM.get())
                    .key('P', Tags.Items.ENDER_PEARLS)
                    .key('C', CHUNK_LOADER_ITEM)
                    .patternLine("PPP")
                    .patternLine("PCP")
                    .patternLine("PPP");
        }
    }

    private static class Models extends ModelProvider {

        public Models(PackOutput output) {
            super(output, MOD_ID);

        }

        @Override
        protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
            blockModels.createNonTemplateModelBlock(CHUNK_LOADER_BLOCK.get());
            blockModels.createNonTemplateModelBlock(SPOT_LOADER_BLOCK.get());
            itemModels.itemModelOutput.accept(
                    CHUNK_LOADER_ITEM.get(),
                    ItemModelUtils.composite(
                            ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(CHUNK_LOADER_BLOCK.get())),
                            ItemModelUtils.specialModel(
                                    ModelLocationUtils.getModelLocation(CHUNK_LOADER_BLOCK.get()),
                                    new ChunkLoaderPearlItemRenderer.Unbaked(0.9, 0.08)
                            )
                    )
            );
            itemModels.itemModelOutput.accept(
                    SPOT_LOADER_ITEM.get(),
                    ItemModelUtils.composite(
                            ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(SPOT_LOADER_BLOCK.get())),
                            ItemModelUtils.specialModel(
                                    ModelLocationUtils.getModelLocation(SPOT_LOADER_BLOCK.get()),
                                    new ChunkLoaderPearlItemRenderer.Unbaked(0.55, 0.05)
                            )
                    )
            );
        }
    }
}
