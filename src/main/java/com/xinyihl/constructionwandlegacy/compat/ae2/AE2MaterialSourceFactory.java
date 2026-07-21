package com.xinyihl.constructionwandlegacy.compat.ae2;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.networking.TileController;
import appeng.util.item.AEItemStack;
import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.material.MaterialCollector;
import com.xinyihl.constructionwandlegacy.material.MaterialKey;
import com.xinyihl.constructionwandlegacy.material.MaterialReceipt;
import com.xinyihl.constructionwandlegacy.material.MaterialSource;
import com.xinyihl.constructionwandlegacy.material.MaterialSourceFactory;
import com.xinyihl.constructionwandlegacy.material.SaturatedAmounts;
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
    @Override
    @Optional.Method(modid = "appliedenergistics2")
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
            ISecurityGrid security = controller.getProxy().getSecurity();
            IGridNode node = controller.getGridNode(null);
            if (node == null || !security.hasPermission(player, SecurityPermissions.EXTRACT)) {
                return null;
            }
            return createSource(player, boundWorld, boundPos, controller,
                    node.getGrid(), new MachineSource(controller));
        } catch (GridAccessException exception) {
            if (ConstructionWandLegacy.LOGGER != null) {
                ConstructionWandLegacy.LOGGER.debug("Unable to resolve bound AE2 grid", exception);
            }
            return null;
        }
    }

    @Nullable
    @Optional.Method(modid = "appliedenergistics2")
    private static MaterialSource createSource(EntityPlayer player, World world, BlockPos pos,
                                               TileController controller, IGrid grid,
                                               IActionSource actionSource) {
        IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
        IMEMonitor<IAEItemStack> storage = storageGrid.getInventory(
                AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
        return storage == null ? null
                : new AE2MaterialSource(player, world, pos, controller, grid, storage, actionSource);
    }

    private static final class AE2MaterialSource implements MaterialSource {
        private static final String ID = "ae2";

        private final EntityPlayer player;
        private final World world;
        private final BlockPos pos;
        private final TileController controller;
        private final IGrid grid;
        private final IMEMonitor<IAEItemStack> storage;
        private final IActionSource actionSource;

        private AE2MaterialSource(EntityPlayer player, World world, BlockPos pos,
                                  TileController controller, IGrid grid,
                                  IMEMonitor<IAEItemStack> storage, IActionSource actionSource) {
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
        @Optional.Method(modid = "appliedenergistics2")
        public void enumerate(MaterialCollector collector) {
            if (!isValidEndpoint(SecurityPermissions.EXTRACT)) {
                return;
            }
            IItemList<IAEItemStack> itemList = storage.getStorageList();
            if (itemList == null) {
                return;
            }
            for (IAEItemStack aeStack : itemList) {
                if (aeStack == null || SaturatedAmounts.fromLong(aeStack.getStackSize()) == 0) {
                    continue;
                }
                ItemStack definition = aeStack.getDefinition();
                if (!definition.isEmpty()) {
                    collector.accept(MaterialKey.of(definition), aeStack.getStackSize());
                }
            }
        }

        @Override
        @Optional.Method(modid = "appliedenergistics2")
        public MaterialReceipt extract(MaterialKey key, int count) {
            if (count <= 0 || !isValidEndpoint(SecurityPermissions.EXTRACT)) {
                return MaterialReceipt.empty();
            }
            IAEItemStack request = AEItemStack.fromItemStack(key.createStack(1));
            if (request == null) {
                return MaterialReceipt.empty();
            }
            request.setStackSize(count);
            IAEItemStack result;
            try {
                result = storage.extractItems(request, Actionable.MODULATE, actionSource);
            } catch (RuntimeException exception) {
                return MaterialReceipt.empty();
            }
            int extracted = result == null ? 0 : Math.min(count, SaturatedAmounts.fromLong(result.getStackSize()));
            return MaterialReceipt.of(ID, key, extracted, this::refund);
        }

        private int refund(MaterialKey key, int count) {
            if (!isValidEndpoint(SecurityPermissions.INJECT)) {
                return InventoryRefunds.refund(player, key, count);
            }
            IAEItemStack request = AEItemStack.fromItemStack(key.createStack(1));
            int remaining = count;
            if (request != null) {
                request.setStackSize(count);
                try {
                    IAEItemStack rejected = storage.injectItems(request, Actionable.MODULATE, actionSource);
                    remaining = rejected == null ? 0
                            : Math.min(count, SaturatedAmounts.fromLong(rejected.getStackSize()));
                } catch (RuntimeException ignored) {
                    remaining = count;
                }
            }
            return remaining <= 0 ? 0 : InventoryRefunds.refund(player, key, remaining);
        }

        @Optional.Method(modid = "appliedenergistics2")
        private boolean isValidEndpoint(SecurityPermissions permission) {
            if (world == null || world.isRemote || !world.isBlockLoaded(pos)
                    || world.getTileEntity(pos) != controller || controller.isInvalid()) {
                return false;
            }
            try {
                IGridNode node = controller.getGridNode(null);
                if (node == null || node.getGrid() != grid
                        || !controller.getProxy().getSecurity().hasPermission(player, permission)) {
                    return false;
                }
                IStorageGrid currentStorage = grid.getCache(IStorageGrid.class);
                return currentStorage.getInventory(AEApi.instance().storage()
                        .getStorageChannel(IItemStorageChannel.class)) == storage;
            } catch (GridAccessException | RuntimeException exception) {
                return false;
            }
        }
    }
}
