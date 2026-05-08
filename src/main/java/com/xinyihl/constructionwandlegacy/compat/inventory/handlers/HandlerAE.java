package com.xinyihl.constructionwandlegacy.compat.inventory.handlers;

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
import com.xinyihl.constructionwandlegacy.api.IInventoryHandler;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class HandlerAE implements IInventoryHandler {
    private static final String TAG_BOUND_POS = "ae_bound_pos";
    private static final String TAG_BOUND_DIM = "ae_bound_dim";

    private IGrid grid = null;
    private IActionSource source = null;

    public HandlerAE(EntityPlayer player) {
        ItemStack wand = WandUtil.holdingWand(player);
        if (!wand.isEmpty()) {
            WandOptions opts = new WandOptions(wand);
            int[] posArr = opts.tag.getIntArray(TAG_BOUND_POS);
            int dimId = opts.tag.getInteger(TAG_BOUND_DIM);
            if (posArr.length >= 3) {
                BlockPos boundPos = new BlockPos(posArr[0], posArr[1], posArr[2]);
                World world = DimensionManager.getWorld(dimId);
                if (world != null && world.isBlockLoaded(boundPos)) {
                    TileEntity tile = world.getTileEntity(boundPos);
                    if (tile instanceof TileController) {
                        TileController controller = (TileController) tile;
                        IGridNode node = controller.getGridNode(null);
                        try {
                            ISecurityGrid s = controller.getProxy().getSecurity();
                            if (s.hasPermission(player, SecurityPermissions.EXTRACT)) {
                                if (node != null) {
                                    grid = node.getGrid();
                                    source = new MachineSource(controller);
                                }
                            }
                        } catch (GridAccessException ignore) {

                        }
                    }
                }
            }
        }
    }

    public static void storeBinding(WandOptions opts, BlockPos pos, int dimId) {
        opts.tag.setIntArray(TAG_BOUND_POS, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        opts.tag.setInteger(TAG_BOUND_DIM, dimId);
    }

    public static boolean hasBinding(WandOptions opts) {
        return opts.tag.hasKey(TAG_BOUND_POS) && opts.tag.hasKey(TAG_BOUND_DIM);
    }

    @Nullable
    @Optional.Method(modid = "appliedenergistics2")
    private IMEMonitor<IAEItemStack> getItemStorage() {
        if (grid == null) return null;
        IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
        return storageGrid.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public int countItems(EntityPlayer player, ItemStack requiredStack, ContainerManager containerManager) {
        IMEMonitor<IAEItemStack> storage = getItemStorage();
        if (storage == null) return 0;
        IAEItemStack request = AEItemStack.fromItemStack(requiredStack);
        if (request == null) return 0;
        request.setStackSize(Long.MAX_VALUE);
        IAEItemStack canExtract = storage.extractItems(request, Actionable.SIMULATE, this.source);
        if (canExtract == null) return 0;
        return (int) canExtract.getStackSize();
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public int useItems(EntityPlayer player, ItemStack requiredStack, int count, ContainerManager containerManager) {
        IMEMonitor<IAEItemStack> storage = getItemStorage();
        if (storage == null) return count;
        IAEItemStack request = AEItemStack.fromItemStack(requiredStack);
        if (request == null) return count;
        request.setStackSize(count);
        IAEItemStack canExtract = storage.extractItems(request, Actionable.MODULATE, this.source);
        if (canExtract == null) return count;
        return count - (int) canExtract.getStackSize();
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer) {
        IMEMonitor<IAEItemStack> storage = getItemStorage();
        if (storage == null) return;
        IItemList<IAEItemStack> itemList = storage.getStorageList();
        if (itemList == null) return;
        for (IAEItemStack aeStack : itemList) {
            if (aeStack == null) continue;
            ItemStack defStack = aeStack.getDefinition();
            if (!defStack.isEmpty() && defStack.getItem() == item && aeStack.getStackSize() > 0) {
                consumer.accept(defStack);
            }
        }
    }
}
