package com.rs.game.map;

import org.darkan.core.model.RegionSize;
import com.rs.game.World;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.net.prot.packet.UpdateZonePartialEnclosed;
import org.darkan.core.net.prot.ZonePacketEncoder;
import org.darkan.core.util.MapUtils;
import org.darkan.core.util.Structure;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Set;

public class UpdateZone {
    private final int baseChunkId;
    private final RegionSize size;
    private final Set<Integer> chunkIds = new IntOpenHashSet();
    private final Set<Integer> regionIds = new IntOpenHashSet();
    protected Set<Integer> playerWatchers = IntSets.synchronize(new IntOpenHashSet());
    protected Set<Integer> npcWatchers = IntSets.synchronize(new IntOpenHashSet());
    private final List<UpdateZonePartialEnclosed> chunkUpdates = new ObjectArrayList<>();

    public UpdateZone(int baseChunkId, RegionSize size) {
        this.baseChunkId = baseChunkId;
        this.size = size;
        // Exactly the scene Entity.loadMapRegions builds: a chunk past it encodes an offset the client has no slot for.
        for (int planeOff = 0;planeOff < 4 * Chunk.PLANE_INC;planeOff += Chunk.PLANE_INC) {
            for (int chunkXOff = 0; chunkXOff < (size.getSize() / 8) * Chunk.X_INC; chunkXOff += Chunk.X_INC) {
                for (int chunkYOff = 0; chunkYOff < (size.getSize() / 8); chunkYOff++) {
                    int chunkId = baseChunkId + chunkXOff + chunkYOff + planeOff;
                    chunkIds.add(chunkId);
                    regionIds.add(MapUtils.chunkToRegionId(chunkId));
                }
            }
        }
    }

    /** A key, not a hash: two scene bases colliding here would hand a player another base's zone. */
    public static long getId(int baseChunkId, RegionSize size) {
        return ((long) baseChunkId << 8) | size.ordinal();
    }

    public void rebuildUpdateZone() {
       int[] base = MapUtils.decode(Structure.CHUNK, baseChunkId);
       for (int chunkId : chunkIds) {
           Chunk chunk = ChunkManager.getChunk(chunkId);
           if (!chunk.getUpdates().isEmpty()) {
               int[] local = MapUtils.decode(Structure.CHUNK, chunk.getId());
               byte[] data = ZonePacketEncoder.INSTANCE.encodeZoneSubPackets(chunk.getUpdates());
               chunkUpdates.add(new UpdateZonePartialEnclosed(
                   local[0] - base[0], local[1] - base[1], local[2], data
               ));
           }
       }
    }

    private void sendUpdates(Player player) {
        if (chunkUpdates.isEmpty() || player == null || player.hasFinished() || !player.hasStarted())
            return;
        for (UpdateZonePartialEnclosed packet : chunkUpdates)
            player.getSession().queuePacket(packet);
    }

    public void update() {
        rebuildUpdateZone();
        for (int pid : playerWatchers)
            sendUpdates(World.getPlayers().get(pid));
        chunkUpdates.clear();
    }

    public void addPlayerWatcher(int pid) {
        playerWatchers.add(pid);
    }

    public void removePlayerWatcher(int pid) {
        playerWatchers.remove(pid);
        if (playerWatchers.isEmpty() && npcWatchers.isEmpty())
            ChunkManager.removeUpdateZone(baseChunkId, size);
    }

    public void addNPCWatcher(int nid) {
        npcWatchers.add(nid);
    }

    public void removeNPCWatcher(int nid) {
        npcWatchers.remove(nid);
        if (playerWatchers.isEmpty() && npcWatchers.isEmpty())
            ChunkManager.removeUpdateZone(baseChunkId, size);
    }

    public Set<Integer> getChunkIds() {
        return chunkIds;
    }

    public Set<Integer> getRegionIds() {
        return regionIds;
    }
}
