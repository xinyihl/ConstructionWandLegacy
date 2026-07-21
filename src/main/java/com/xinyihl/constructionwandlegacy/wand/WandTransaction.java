package com.xinyihl.constructionwandlegacy.wand;

import com.xinyihl.constructionwandlegacy.material.MaterialReceipt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Successful operation records in execution order, with retryable recovery state. */
public final class WandTransaction {
    private final int dimension;
    private final List<Entry> entries;
    private final Set<BlockPos> positions;

    private WandTransaction(int dimension, List<Entry> entries) {
        this.dimension = dimension;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        LinkedHashSet<BlockPos> transactionPositions = new LinkedHashSet<>();
        for (Entry entry : entries) {
            transactionPositions.add(entry.change.getPos());
        }
        this.positions = Collections.unmodifiableSet(transactionPositions);
    }

    public int getDimension() {
        return dimension;
    }

    public Set<BlockPos> getPositions() {
        return positions;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean isComplete() {
        for (Entry entry : entries) {
            if (!entry.isComplete()) {
                return false;
            }
        }
        return true;
    }

    /** Recovers all entries in reverse order without exposing receipts or entry mutators. */
    public RecoveryResult recover(World world, EntityPlayer player) {
        boolean changed = false;
        boolean complete = true;
        WandOperation.RollbackResult firstFailure = null;
        for (int index = entries.size() - 1; index >= 0; index--) {
            RecoveryResult result = entries.get(index).recover(world, player);
            changed |= result.didRestoreWorld();
            if (!result.isComplete()) {
                complete = false;
                if (firstFailure == null) {
                    firstFailure = result.getFailure();
                }
            }
        }
        return RecoveryResult.aggregate(changed, complete, firstFailure);
    }

    public static Builder builder(int dimension) {
        return new Builder(dimension);
    }

    static final class Entry {
        private final WandOperation.AppliedChange change;
        private final MaterialReceipt materialReceipt;
        private boolean worldRestored;
        private boolean materialRefunded;

        private Entry(WandOperation.AppliedChange change, MaterialReceipt materialReceipt) {
            this.change = change;
            this.materialReceipt = materialReceipt;
            this.materialRefunded = materialReceipt.getRemainingCount() == 0;
        }

        public BlockPos getPos() {
            return change.getPos();
        }

        public boolean isComplete() {
            return worldRestored && materialRefunded;
        }

        /**
         * Restores against the supplied current world and only then attempts refund. A retry after
         * a refund failure skips the already restored world and retries the captured receipt.
         */
        private RecoveryResult recover(World world, EntityPlayer player) {
            WandOperation.RollbackResult restoreResult = WandOperation.RollbackResult.restored();
            boolean restoredThisAttempt = false;
            if (!worldRestored) {
                try {
                    restoreResult = change.restore(world, player);
                } catch (RuntimeException exception) {
                    restoreResult = WandOperation.RollbackResult.failed(
                            "exception restoring transaction entry", exception);
                }
                if (!restoreResult.isRestored()) {
                    return RecoveryResult.incomplete(false, false, restoreResult);
                }
                worldRestored = true;
                restoredThisAttempt = true;
            }

            if (!materialRefunded) {
                try {
                    materialReceipt.refund();
                    materialRefunded = materialReceipt.getRemainingCount() == 0;
                } catch (RuntimeException exception) {
                    return RecoveryResult.incomplete(restoredThisAttempt, false,
                            WandOperation.RollbackResult.failed("exception refunding transaction entry", exception));
                }
                if (!materialRefunded) {
                    return RecoveryResult.incomplete(restoredThisAttempt, false,
                            WandOperation.RollbackResult.notRestored("material refund has remaining items"));
                }
            }
            return RecoveryResult.complete(restoredThisAttempt);
        }

    }

    public static final class RecoveryResult {
        private final boolean worldChanged;
        private final boolean materialRefunded;
        @Nullable
        private final WandOperation.RollbackResult failure;

        private RecoveryResult(boolean worldChanged, boolean materialRefunded,
                               @Nullable WandOperation.RollbackResult failure) {
            this.worldChanged = worldChanged;
            this.materialRefunded = materialRefunded;
            this.failure = failure;
        }

        private static RecoveryResult complete(boolean worldChanged) {
            return new RecoveryResult(worldChanged, true, null);
        }

        private static RecoveryResult aggregate(boolean worldChanged, boolean complete,
                                                @Nullable WandOperation.RollbackResult failure) {
            return new RecoveryResult(worldChanged, complete, failure);
        }

        private static RecoveryResult incomplete(boolean worldChanged, boolean materialRefunded,
                                                 WandOperation.RollbackResult failure) {
            return new RecoveryResult(worldChanged, materialRefunded, failure);
        }

        public boolean didRestoreWorld() {
            return worldChanged;
        }

        public boolean isMaterialRefunded() {
            return materialRefunded;
        }

        public boolean isComplete() {
            return materialRefunded && failure == null;
        }

        @Nullable
        public WandOperation.RollbackResult getFailure() {
            return failure;
        }
    }

    public static final class Builder {
        private final int dimension;
        private final List<Entry> entries = new ArrayList<>();

        private Builder(int dimension) {
            this.dimension = dimension;
        }

        public Builder add(WandOperation.AppliedChange change, MaterialReceipt receipt) {
            entries.add(new Entry(change, receipt));
            return this;
        }

        public WandTransaction build() {
            return new WandTransaction(dimension, entries);
        }
    }
}
