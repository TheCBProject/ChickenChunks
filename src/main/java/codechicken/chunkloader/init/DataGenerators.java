package codechicken.chunkloader.init;

import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.block.BlockSpotLoader;
import codechicken.lib.datagen.ItemModelProvider;
import codechicken.lib.datagen.LootTableProvider;
import com.google.gson.JsonObject;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

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

        gen.addProvider(event.includeClient(), new BlockStates(output, files));
        gen.addProvider(event.includeClient(), new ItemModels(output, files));
    }

    private static class LootTables extends LootTableProvider.BlockLootProvider {

        protected LootTables(PackOutput output) {
            super(output, MOD_ID);
        }

        @Override
        protected void registerTables() {
            register(ChickenChunksModContent.CHUNK_LOADER_BLOCK.get(), singleItem(ChickenChunksModContent.CHUNK_LOADER_BLOCK.get()));
            register(ChickenChunksModContent.SPOT_LOADER_BLOCK.get(), singleItem(ChickenChunksModContent.SPOT_LOADER_BLOCK.get()));
        }
    }

    private static class BlockTags extends BlockTagsProvider {

        public BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, MOD_ID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(ChickenChunksModContent.CHUNK_LOADER_BLOCK.get())
                    .add(ChickenChunksModContent.SPOT_LOADER_BLOCK.get());
        }
    }

    private static class BlockStates extends BlockStateProvider {

        public BlockStates(PackOutput output, ExistingFileHelper exFileHelper) {
            super(output, MOD_ID, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            simpleBlock(
                    ChickenChunksModContent.CHUNK_LOADER_BLOCK.get(),
                    chunkLoader("chunk_loader", "full", BlockChunkLoader.SHAPE)
            );
            simpleBlock(
                    ChickenChunksModContent.SPOT_LOADER_BLOCK.get(),
                    chunkLoader("spot_loader", "spot", BlockSpotLoader.SHAPE)
            );
        }

        private ModelFile chunkLoader(String name, String texturePrefix, VoxelShape shape) {
            AABB box = shape.bounds();
            return models().getBuilder(name)
                    .texture("particle", modLoc("block/" + texturePrefix + "/side"))
                    .texture("top", modLoc("block/" + texturePrefix + "/top"))
                    .texture("side", modLoc("block/" + texturePrefix + "/side"))
                    .texture("bottom", modLoc("block/" + texturePrefix + "/bottom"))
                    .element()
                    .from((float) box.minX * 16F, (float) box.minY * 16F, (float) box.minZ * 16F)
                    .to((float) box.maxX * 16F, (float) box.maxY * 16F, (float) box.maxZ * 16F)
                    .allFaces((dir, builder) -> {
                        switch (dir) {
                            case NORTH, EAST, SOUTH, WEST -> builder.texture("#side");
                            case UP -> builder.texture("#top");
                            case DOWN -> builder.texture("#bottom");
                        }
                    })
                    .end();
        }
    }

    private static class ItemModels extends ItemModelProvider {

        public ItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            generated(ChickenChunksModContent.CHUNK_LOADER_ITEM.get())
                    .noTexture()
                    .customLoader(ChunkLoaderItemModelLoaderBuilder::new)
                    .childModel("chickenchunks:block/chunk_loader")
                    .spotLoader(false);

            generated(ChickenChunksModContent.SPOT_LOADER_ITEM.get())
                    .noTexture()
                    .customLoader(ChunkLoaderItemModelLoaderBuilder::new)
                    .childModel("chickenchunks:block/spot_loader")
                    .spotLoader(true);
        }
    }

    private static class ChunkLoaderItemModelLoaderBuilder extends ItemModelProvider.CustomLoaderBuilder {

        private @Nullable String childModel;
        private boolean isSpotLoader;

        public ChunkLoaderItemModelLoaderBuilder(ItemModelProvider.SimpleItemModelBuilder parent) {
            super(new ResourceLocation(MOD_ID, "chunk_loader"), parent);
        }

        public ChunkLoaderItemModelLoaderBuilder childModel(String childModel) {
            this.childModel = childModel;
            return this;
        }

        public ChunkLoaderItemModelLoaderBuilder spotLoader(boolean isSpotLoader) {
            this.isSpotLoader = isSpotLoader;
            return this;
        }

        @Override
        protected JsonObject toJson(JsonObject json) {
            super.toJson(json);

            json.addProperty("childModel", Objects.requireNonNull(childModel));
            json.addProperty("isSpotLoader", isSpotLoader);

            return json;
        }
    }
}
