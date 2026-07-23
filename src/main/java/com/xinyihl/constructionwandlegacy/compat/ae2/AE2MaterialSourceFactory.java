package com.xinyihl.constructionwandlegacy.compat.ae2;

import ae2.api.config.Actionable;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.tile.networking.TileController;
import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.material.*;
import com.xinyihl.constructionwandlegacy.material.source.InventoryRefunds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;

public final class AE2MaterialSourceFactory implements MaterialSourceFactory {
    @Nullable
    @Optional.Method(modid = "ae2")
    private static MaterialSource createSource(EntityPlayer player, World world, BlockPos pos, TileController controller, IGrid grid, IActionSource actionSource) {
        MEStorage storage = grid.getStorageService().getInventory();
        return storage == null ? null : new AE2MaterialSource(player, world, pos, controller, grid, storage, actionSource);
    }

    @Nullable
    @Override
    @Optional.Method(modid = "ae2")
    public MaterialSource create(EntityPlayer player, ItemStack wand) {
        AE2Compat.Binding binding = AE2Compat.readBinding(wand);
        if (binding == null) {
            return null;
        }
        BlockPos boundPos = binding.getPosition();
        World boundWorld = DimensionManager.getWorld(binding.getDimension());
        if (boundWorld == null || !boundWorld.isBlockLoaded(boundPos)) {
            return null;
        }
        TileEntity tile = boundWorld.getTileEntity(boundPos);
        if (!(tile instanceof TileController)) {
            return null;
        }
        TileController controller = (TileController) tile;
        try {
            IGridNode node = controller.getMainNode().getNode();
            if (node == null || !node.isActive()) {
                return null;
            }
            return createSource(player, boundWorld, boundPos, controller, node.grid(), IActionSource.ofPlayer(player, controller));
        } catch (RuntimeException exception) {
            if (ConstructionWandLegacy.LOGGER != null) {
                ConstructionWandLegacy.LOGGER.debug("Unable to resolve bound AE2 grid", exception);
            }
            return null;
        }
    }

    private static final class AE2MaterialSource implements MaterialSource {
        private static final String ID = "ae2";

        private final EntityPlayer player;
        private final World world;
        private final BlockPos pos;
        private final TileController controller;
        private final IGrid grid;
        private final MEStorage storage;
        private final IActionSource actionSource;

        private AE2MaterialSource(EntityPlayer player, World world, BlockPos pos, TileController controller, IGrid grid, MEStorage storage, IActionSource actionSource) {
            this.player = player;
            this.world = world;
            this.pos = pos;
            this.controller = controller;
            this.grid = grid;
            this.storage = storage;
            this.actionSource = actionSource;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        @Optional.Method(modid = "ae2")
        public void enumerate(MaterialCollector collector) {
            if (!isValidEndpoint()) {
                return;
            }
            KeyCounter availableStacks = storage.getAvailableStacks();
            for (AEKey aeKey : availableStacks.keySet()) {
                if (!(aeKey instanceof AEItemKey)) {
                    continue;
                }
                long amount = availableStacks.get(aeKey);
                if (SaturatedAmounts.fromLong(amount) == 0) {
                    continue;
                }
                ItemStack definition = ((AEItemKey) aeKey).toStack();
                if (!definition.isEmpty()) {
                    collector.accept(MaterialKey.of(definition), amount);
                }
            }
        }

        @Override
        @Optional.Method(modid = "ae2")
        public MaterialReceipt extract(MaterialKey key, int count) {
            if (count <= 0 || !isValidEndpoint()) {
                return MaterialReceipt.empty();
            }
            AEItemKey request = AEItemKey.of(key.createStack(1));
            if (request == null) {
                return MaterialReceipt.empty();
            }
            long result;
            try {
                result = storage.extract(request, count, Actionable.MODULATE, actionSource);
            } catch (RuntimeException exception) {
                return MaterialReceipt.empty();
            }
            int extracted = Math.min(count, SaturatedAmounts.fromLong(result));
            return MaterialReceipt.of(ID, key, extracted, this::refund);
        }

        private int refund(MaterialKey key, int count) {
            if (!isValidEndpoint()) {
                return InventoryRefunds.refund(player, key, count);
            }
            AEItemKey request = AEItemKey.of(key.createStack(1));
            int remaining = count;
            if (request != null) {
                try {
                    long inserted = storage.insert(request, count, Actionable.MODULATE, actionSource);
                    remaining = count - Math.min(count, SaturatedAmounts.fromLong(inserted));
                } catch (RuntimeException ignored) {
                    remaining = count;
                }
            }
            return remaining <= 0 ? 0 : InventoryRefunds.refund(player, key, remaining);
        }

        @Optional.Method(modid = "ae2")
        private boolean isValidEndpoint() {
            if (world == null || world.isRemote || !world.isBlockLoaded(pos) || world.getTileEntity(pos) != controller || controller.isInvalid()) {
                return false;
            }
            try {
                IGridNode node = controller.getMainNode().getNode();
                if (node == null || !node.isActive() || node.grid() != grid) {
                    return false;
                }
                return grid.getStorageService().getInventory() == storage;
            } catch (RuntimeException exception) {
                return false;
            }
        }
    }
}
