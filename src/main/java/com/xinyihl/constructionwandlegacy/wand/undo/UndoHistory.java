package com.xinyihl.constructionwandlegacy.wand.undo;

import com.xinyihl.constructionwandlegacy.network.ModMessages;
import com.xinyihl.constructionwandlegacy.network.PacketUndoBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class UndoHistory {
    private static final int HISTORY_SIZE = 10;
    private final Map<UUID, PlayerEntry> history = new HashMap<>();

    private PlayerEntry getEntry(EntityPlayer player) {
        return history.computeIfAbsent(player.getUniqueID(), key -> new PlayerEntry());
    }

    public void add(EntityPlayer player, World world, List<ISnapshot> snapshots) {
        LinkedList<List<ISnapshot>> entries = getEntry(player).entries;
        entries.add(new ArrayList<>(snapshots));
        while (entries.size() > HISTORY_SIZE) {
            entries.removeFirst();
        }
        refreshClientIfActive(player);
    }

    public boolean undo(EntityPlayer player) {
        LinkedList<List<ISnapshot>> entries = getEntry(player).entries;
        if (entries == null || entries.isEmpty()) {
            refreshClientIfActive(player);
            return false;
        }

        List<ISnapshot> snapshots = entries.removeLast();
        boolean changed = false;
        for (ISnapshot snapshot : snapshots) {
            if (snapshot.canRestore(player.world, player)) {
                changed |= snapshot.restore(player.world, player);
            }
        }
        refreshClientIfActive(player);
        return changed;
    }

    public Set<BlockPos> peekLastPositions(EntityPlayer player) {
        LinkedList<List<ISnapshot>> entries = getEntry(player).entries;
        if (entries == null || entries.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return entries.getLast().stream().map(ISnapshot::getPos).collect(Collectors.toSet());
    }

    public boolean isUndoActive(EntityPlayer player) {
        return getEntry(player).undoActive;
    }

    public void updateClient(EntityPlayerMP player, boolean undoPressed) {
        PlayerEntry entry = getEntry(player);
        entry.undoActive = undoPressed;
        ModMessages.sendToPlayer(new PacketUndoBlocks(peekLastPositions(player)), player);
    }

    public void clearHistory(UUID playerId) {
        history.remove(playerId);
    }

    private void refreshClientIfActive(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        PlayerEntry entry = getEntry(player);
        if (!entry.undoActive) {
            return;
        }

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ModMessages.sendToPlayer(new PacketUndoBlocks(peekLastPositions(playerMP)), playerMP);
    }

    private static final class PlayerEntry {
        private final LinkedList<List<ISnapshot>> entries = new LinkedList<>();
        private boolean undoActive;
    }
}
