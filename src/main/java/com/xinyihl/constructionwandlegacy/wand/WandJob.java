package com.xinyihl.constructionwandlegacy.wand;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.api.IWandAction;
import com.xinyihl.constructionwandlegacy.api.IWandSupplier;
import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.items.ModItems;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWand;
import com.xinyihl.constructionwandlegacy.wand.supplier.SupplierInventory;
import com.xinyihl.constructionwandlegacy.wand.supplier.SupplierRandom;
import com.xinyihl.constructionwandlegacy.wand.undo.ISnapshot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WandJob {
    public final EntityPlayer player;
    public final World world;
    public final RayTraceResult rayTraceResult;
    public final WandOptions options;
    public final ItemStack wand;
    public final ItemWand wandItem;

    private final IWandAction wandAction;
    private final IWandSupplier wandSupplier;

    private List<ISnapshot> snapshots;

    public WandJob(EntityPlayer player, World world, RayTraceResult rayTraceResult, ItemStack wand) {
        this.player = player;
        this.world = world;
        this.rayTraceResult = rayTraceResult;
        this.wand = wand;
        this.wandItem = (ItemWand) wand.getItem();
        this.options = new WandOptions(wand);
        this.snapshots = new ArrayList<>();

        this.wandSupplier = options.random.get() ? new SupplierRandom(player, options) : new SupplierInventory(player, options);
        this.wandAction = options.cores.get().getWandAction();

        wandSupplier.getSupply(getTargetItem(world, rayTraceResult));
    }

    @Nullable
    private static ItemStack getTargetItem(World world, RayTraceResult rayTraceResult) {
        if (rayTraceResult == null) {
            return null;
        }

        IBlockState state = world.getBlockState(rayTraceResult.getBlockPos());
        Block block = state.getBlock();

        Item blockItem = Item.getItemFromBlock(block);
        if (!(blockItem instanceof ItemBlock)) {
            return null;
        }

        ItemBlock item = (ItemBlock) blockItem;
        int metadata = block.damageDropped(state);
        return new ItemStack(item, 1, metadata);
    }

    public void getSnapshots() {
        int limit;
        if (player.isCreative() && wandItem == ModItems.WAND_INFINITY) {
            limit = ConfigServer.LIMIT_CREATIVE;
        } else {
            limit = Math.min(wandItem.remainingDurability(wand), wandAction.getLimit(wand));
        }

        if (rayTraceResult != null && rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            snapshots = wandAction.getSnapshots(world, player, rayTraceResult, wand, options, wandSupplier, limit);
        } else {
            snapshots = wandAction.getSnapshotsFromAir(world, player, rayTraceResult, wand, options, wandSupplier, limit);
        }
    }

    public Set<BlockPos> getBlockPositions() {
        return snapshots.stream().map(ISnapshot::getPos).collect(Collectors.toCollection(HashSet::new));
    }

    public int blockCount() {
        return snapshots.size();
    }

    public boolean doIt() {
        ArrayList<ISnapshot> executed = new ArrayList<>();

        for (ISnapshot snapshot : snapshots) {
            if (wand.isEmpty() || wandItem.remainingDurability(wand) == 0) {
                break;
            }

            if (snapshot.execute(world, player, rayTraceResult)) {
                if (player.isCreative()) {
                    executed.add(snapshot);
                } else {
                    if (wandSupplier.takeItemStack(snapshot.getRequiredItems()) == 0) {
                        executed.add(snapshot);
                        wand.damageItem(1, player);
                    } else {
                        snapshot.forceRestore(world);
                    }
                }
            }
        }

        snapshots = executed;
        if (!snapshots.isEmpty()) {
            ConstructionWandLegacy.instance.undoHistory.add(player, world, snapshots);
            return true;
        }
        return false;
    }
}
