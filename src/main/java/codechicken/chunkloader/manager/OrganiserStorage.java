package codechicken.chunkloader.manager;

import codechicken.chunkloader.manager.ChunkLoaderManager.ModOrganiser;
import codechicken.chunkloader.manager.ChunkLoaderManager.PlayerOrganiser;
import codechicken.chunkloader.manager.ChunkLoaderManager.ReviveChange;
import codechicken.chunkloader.manager.ChunkLoaderManager.TicketManager;
import codechicken.lib.util.CommonUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.commons.io.IOUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static codechicken.chunkloader.ChickenChunks.logger;

/**
 * Created by covers1624 on 29/06/2017.
 */
@EventBusSubscriber
public class OrganiserStorage {

    private static final String SAVED_DATA_NAME = "ChickenChunksOrganisers";

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        OrganiserStorage.getStorage(event.player.world).onPlayerLogin(event.player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedOutEvent event) {
        OrganiserStorage.getStorage(event.player.world).onPlayerLogout(event.player);
    }

    public static IOrganiserStorage getStorage(World world) {
        WorldSavedData data = world.getMapStorage().getOrLoadData(SavedData.class, SAVED_DATA_NAME);
        if (data == null) {
            data = new SavedData(SAVED_DATA_NAME);
            world.getMapStorage().setData(SAVED_DATA_NAME, data);
        }
        return ((IOrganiserStorage) data);
    }

    public interface IOrganiserStorage {

        PlayerOrganiser getPlayerOrganiser(String player);

        ModOrganiser getModOrganiser(Object object);

        void load(World world);

        void queDormantUnloads(World world);

        void tickUnloads();

        void onDimensionRevive(World world);

        void onPlayerLogin(EntityPlayer player);

        void onPlayerLogout(EntityPlayer player);

        void onDimensionUnload(World world);

        void forceSave(World world);

        @Deprecated
        void loadLegacyData(File folder);//TODO 1.12 Remove this, as no more LegacyConversion

    }

    public static class SavedData extends WorldSavedData implements IOrganiserStorage {

        private Map<String, PlayerOrganiser> playerOrganisers = new HashMap<>();
        private Map<Object, ModOrganiser> modOrganisers = new HashMap<>();

        public SavedData(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            NBTTagList playerOrganisers = nbt.getTagList("playerOrganisers", 10);
            for (int i = 0; i < playerOrganisers.tagCount(); i++) {
                NBTTagCompound tagCompound = playerOrganisers.getCompoundTagAt(i);
                String username = tagCompound.getString("player");
                PlayerOrganiser organiser = getPlayerOrganiser(username);
                organiser.setDormant();
                organiser.readFromNBT(tagCompound.getCompoundTag("organiser"));
            }
            NBTTagList modOrganisers = nbt.getTagList("modOrganisers", 10);
            for (int i = 0; i < modOrganisers.tagCount(); i++) {
                NBTTagCompound tagCompound = modOrganisers.getCompoundTagAt(i);
                String modId = tagCompound.getString("modId");
                Object modObject = lookupMod(modId);
                if (modObject != null) {
                    ModOrganiser organiser = getModOrganiser(modObject);
                    organiser.readFromNBT(tagCompound.getCompoundTag("organiser"));
                } else {
                    logger.warn("Unable to get ModObject for modId: \"{}\" Assuming mod removed, Dropping data..", modId);
                }
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound compound) {

            NBTTagList playerOrganisers = new NBTTagList();
            for (PlayerOrganiser organiser : this.playerOrganisers.values()) {
                NBTTagCompound organiserTag = new NBTTagCompound();
                organiserTag.setString("player", organiser.username);
                organiserTag.setTag("organiser", organiser.saveToNBT(new NBTTagCompound()));
                playerOrganisers.appendTag(organiserTag);
            }
            compound.setTag("playerOrganisers", playerOrganisers);

            NBTTagList modOrganisers = new NBTTagList();
            for (ModOrganiser organiser : this.modOrganisers.values()) {
                NBTTagCompound organiserTag = new NBTTagCompound();
                organiserTag.setString("modId", organiser.container.getModId());
                organiserTag.setTag("organiser", organiser.saveToNBT(new NBTTagCompound()));
                modOrganisers.appendTag(organiserTag);
            }
            compound.setTag("modOrganisers", modOrganisers);

            return compound;
        }

        @Override
        public boolean isDirty() {
            boolean superDirty = super.isDirty();
            boolean localDirty = false;

            localDirty |= PlayerOrganiser.dirty;

            for (ModOrganiser organiser : modOrganisers.values()) {
                localDirty |= organiser.dirty;
            }

            return superDirty || localDirty;
        }

        @Override
        public void setDirty(boolean isDirty) {

            if (!isDirty) {
                PlayerOrganiser.dirty = false;
                for (ModOrganiser organiser : modOrganisers.values()) {
                    organiser.dirty = false;
                }
            }

            super.setDirty(isDirty);
        }

        @Override
        public PlayerOrganiser getPlayerOrganiser(String player) {
            return playerOrganisers.computeIfAbsent(player, PlayerOrganiser::new);
        }

        @Override
        public ModOrganiser getModOrganiser(Object object) {
            ModOrganiser organiser = modOrganisers.get(object);
            if (organiser == null) {
                ModContainer container = ChunkLoaderManager.getHandledMods().get(object);
                if (container == null) {
                    throw new NullPointerException("Mod not registered with chickenchunks: " + object);
                }
                modOrganisers.put(object, organiser = new ModOrganiser(object, container));
            }
            return organiser;
        }

        @Override
        public void load(World world) {
            for (Entry<String, PlayerOrganiser> entry : playerOrganisers.entrySet()) {
                if (ChunkLoaderManager.allowOffline(entry.getKey()) && ChunkLoaderManager.loggedInRecently(world, entry.getKey())) {
                    ChunkLoaderManager.ReviveChange.PlayerRevive.list.add(entry.getValue());
                }
            }
            ReviveChange.ModRevive.list.addAll(modOrganisers.values());
        }

        @Override
        public void queDormantUnloads(World world) {
            for (PlayerOrganiser organiser : playerOrganisers.values()) {
                if (!organiser.isDormant() && !ChunkLoaderManager.loggedInRecently(world, organiser.username)) {
                    ReviveChange.PlayerDevive.list.add(organiser);
                }
            }
        }

        @Override
        public void tickUnloads() {
            for (Entry<String, PlayerOrganiser> entry : playerOrganisers.entrySet()) {
                entry.getValue().tickDownUnloads();
            }

            for (Entry<Object, ModOrganiser> entry : modOrganisers.entrySet()) {
                entry.getValue().tickDownUnloads();
            }
        }

        @Override
        public void onDimensionRevive(World world) {
            for (PlayerOrganiser organiser : playerOrganisers.values()) {
                organiser.revive(world);
            }
        }

        @Override
        public void onPlayerLogin(EntityPlayer player) {
            ReviveChange.PlayerRevive.list.add(getPlayerOrganiser(player.getName()));
        }

        @Override
        public void onPlayerLogout(EntityPlayer player) {
            if (!ChunkLoaderManager.allowOffline(player.getName())) {
                ReviveChange.PlayerDevive.list.add(getPlayerOrganiser(player.getName()));
            }
        }

        @Override
        public void onDimensionUnload(World world) {
            int dim = CommonUtils.getDimension(world);
            for (TicketManager mgr : playerOrganisers.values()) {
                mgr.unloadDimension(dim);
            }
            for (TicketManager mgr : modOrganisers.values()) {
                mgr.unloadDimension(dim);
            }
        }

        @Override
        public void forceSave(World world) {
            if (isDirty()) {
                world.getMapStorage().saveData(this);
                setDirty(false);
            }
        }

        private static Object lookupMod(String modId) {
            for (Entry<Object, ModContainer> entry : ChunkLoaderManager.getHandledMods().entrySet()) {
                if (Objects.equals(entry.getValue().getModId(), modId)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        @Override
        @Deprecated//TODO 1.12 Remove this, as no more LegacyConversion
        public void loadLegacyData(File folder) {
            File playerFile = new File(folder, "players.dat");
            if (playerFile.exists()) {
                logger.info("Found old players.dat file. Attempting conversion...");
                try {
                    DataInputStream dataIn = new DataInputStream(new FileInputStream(playerFile));
                    int organisers = dataIn.readInt();
                    for (int i = 0; i < organisers; i++) {
                        String username = dataIn.readUTF();
                        PlayerOrganiser organiser = getPlayerOrganiser(username);
                        organiser.setDormant();
                        organiser.loadLegacyData(dataIn);
                    }
                    IOUtils.closeQuietly(dataIn);
                } catch (Throwable e) {
                    logger.warn("Exception thrown whilst converting old players.dat file! This is probably a bad thing.. At most some players loaders wont work until manually chunk loaded.");
                }
                logger.info("Conversion Finished!");
                playerFile.delete();
            }

            for (Entry<Object, ModContainer> entry : ChunkLoaderManager.getHandledMods().entrySet()) {
                File saveFile = new File(folder, entry.getValue().getModId() + ".dat");
                if (!saveFile.exists()) {
                    continue;
                }
                logger.info("Found old {}.dat file. Attempting conversion...", entry.getValue().getModId());
                try {
                    DataInputStream dataIn = new DataInputStream(new FileInputStream(saveFile));
                    ModOrganiser organiser = getModOrganiser(entry.getKey());

                    organiser.loadLegacyData(dataIn);
                    IOUtils.closeQuietly(dataIn);
                } catch (Throwable e) {
                    logger.warn("Exception thrown whilst converting old {}.dat file! This is probably a bad thing.. At most, some loaders wont work until manually chunk loaded.", entry.getValue().getModId());
                }
                logger.info("Conversion Finished!");
                saveFile.delete();
            }
        }
    }
}
