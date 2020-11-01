package codechicken.chunkloader.world;

import codechicken.chunkloader.api.IChunkLoader;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds a reference to all IChunkLoaders currently loading a given chunk.
 * Automatically allocates and frees a vanilla Ticket.
 * <p>
 * Created by covers1624 on 11/5/20.
 */
public class ChunkTicket {

    private static final TicketType<ChunkPos> TICKET_TYPE = TicketType.create("chicken_chunks", Comparator.comparingLong(ChunkPos::asLong));

    private final TicketManager ticketManager;
    public final ChunkPos pos;

    public final Set<IChunkLoader> loaders = new HashSet<>();
    public Ticket<ChunkPos> vanillaTicket;

    public ChunkTicket(TicketManager ticketManager, ChunkPos pos) {
        this.ticketManager = ticketManager;
        this.pos = pos;
    }

    public boolean tryAlloc() {
        if (vanillaTicket != null) {
            return true;
        }
        if (loaders.isEmpty()) {
            return false;
        }

        vanillaTicket = new Ticket<>(TICKET_TYPE, 31, pos);
        ticketManager.register(pos.asLong(), vanillaTicket);
        return true;
    }

    public boolean tryFree() {
        if (vanillaTicket == null) {
            return true;
        }
        if (!loaders.isEmpty()) {
            return false;
        }

        ticketManager.release(pos.asLong(), vanillaTicket);
        vanillaTicket = null;
        return true;
    }

    public boolean isActive() {
        return vanillaTicket != null;
    }
}
