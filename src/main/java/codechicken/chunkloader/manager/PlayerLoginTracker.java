package codechicken.chunkloader.manager;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.commons.io.IOUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import static codechicken.chunkloader.ChickenChunks.logger;

/**
 * Created by covers1624 on 29/06/2017.
 */
@EventBusSubscriber
public class PlayerLoginTracker {

    private static final String SAVED_DATA_NAME = "ChickenChunksLoginTimes";

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ILoginTracker tracker = getTracker(event.player.world);
        tracker.updateLoginTime(event.player.getName());
    }

    public static ILoginTracker getTracker(World world) {
        WorldSavedData data = world.getMapStorage().getOrLoadData(SavedData.class, SAVED_DATA_NAME);
        if (data == null) {
            data = new SavedData(SAVED_DATA_NAME);
            world.getMapStorage().setData(SAVED_DATA_NAME, data);
        }
        return ((ILoginTracker) data);
    }

    public interface ILoginTracker {

        long getLoginTime(String player);

        void updateLoginTime(String player);

        void forceSave(World world);

        @Deprecated
        void loadLegacyData(File folder);//TODO 1.12 Remove this, as no more LegacyConversion
    }

    public static class SavedData extends WorldSavedData implements ILoginTracker {

        private Map<String, Long> loginTimes = new HashMap<>();

        public SavedData(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            NBTTagList list = nbt.getTagList("loginTimes", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound compound = list.getCompoundTagAt(i);
                String name = compound.getString("player");
                long time = compound.getLong("time");
                loginTimes.put(name, time);
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            NBTTagList list = new NBTTagList();
            for (Map.Entry<String, Long> entry : loginTimes.entrySet()) {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setString("player", entry.getKey());
                compound.setLong("time", entry.getValue());
                list.appendTag(compound);
            }
            tag.setTag("loginTimes", list);
            return tag;
        }

        @Override
        public long getLoginTime(String player) {
            return loginTimes.computeIfAbsent(player, (p) -> {
                markDirty();
                return System.currentTimeMillis();
            });
        }

        @Override
        public void updateLoginTime(String player) {
            loginTimes.put(player, System.currentTimeMillis());
            markDirty();
        }

        @Override
        public void forceSave(World world) {
            if (isDirty()) {
                world.getMapStorage().saveData(this);
                setDirty(false);
            }
        }

        @Override
        @Deprecated//TODO 1.12 Remove this, as no more LegacyConversion
        public void loadLegacyData(File folder) {
            File saveFile = new File(folder, "loginTimes.dat");
            if (!saveFile.exists()) {
                return;
            }

            logger.info("Found old loginTimes.data file. Attempting conversion...");
            try {
                DataInputStream dataIn = new DataInputStream(new FileInputStream(saveFile));
                int entries = dataIn.readInt();
                for (int i = 0; i < entries; i++) {
                    markDirty();
                    loginTimes.put(dataIn.readUTF(), dataIn.readLong());
                }
                IOUtils.closeQuietly(dataIn);
            } catch (Throwable e) {
                logger.warn("Exception thrown whilst converting old loginTimes.data! Some login data may be lost..", e);
            }

            logger.info("Conversion Finished!");
            saveFile.delete();
        }
    }

}
