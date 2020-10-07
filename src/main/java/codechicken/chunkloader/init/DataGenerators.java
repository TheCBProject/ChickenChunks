package codechicken.chunkloader.init;

import codechicken.lib.datagen.LootTableProvider;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

/**
 * Created by covers1624 on 7/10/20.
 */
@Mod.EventBusSubscriber (modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataGenerators(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(new LootTables(gen));
    }

    private static class LootTables extends LootTableProvider.BlockLootProvider {

        public LootTables(DataGenerator dataGeneratorIn) {
            super(dataGeneratorIn);
        }

        @Override
        protected void registerTables() {
            register(ModContent.blockChunkLoader, singleItem(ModContent.blockChunkLoader));
            register(ModContent.blockSpotLoader, singleItem(ModContent.blockSpotLoader));
        }

        @Override
        public String getName() {
            return "ChickenChunks Block Loot";
        }
    }

}
