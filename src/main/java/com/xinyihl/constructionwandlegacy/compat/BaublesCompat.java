package com.xinyihl.constructionwandlegacy.compat;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.containers.ContainerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class BaublesCompat {
    private static final String BAUBLES_MODID = "baubles";
    private static final String BAUBLES_API_CLASS = "baubles.api.BaublesApi";

    private static boolean initialized;
    private static boolean available;
    private static boolean warningLogged;
    private static Method getBaublesHandlerMethod;
    private static Method setChangedMethod;

    private BaublesCompat() {
    }

    public static int countItems(EntityPlayer player, ItemStack requiredStack, ContainerManager containerManager) {
        Object handler = getBaublesHandler(player);
        if (!(handler instanceof IItemHandler)) {
            return 0;
        }

        int total = 0;
        IItemHandler itemHandler = (IItemHandler) handler;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack inventoryStack = itemHandler.getStackInSlot(i);
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                total += Math.max(0, inventoryStack.getCount());
            } else {
                total += containerManager.countItems(player, requiredStack, inventoryStack);
            }
        }
        return total;
    }

    public static int useItems(EntityPlayer player, ItemStack requiredStack, int count, ContainerManager containerManager) {
        Object handler = getBaublesHandler(player);
        if (!(handler instanceof IItemHandler)) {
            return count;
        }

        IItemHandler itemHandler = (IItemHandler) handler;
        for (int i = 0; i < itemHandler.getSlots() && count > 0; i++) {
            ItemStack inventoryStack = itemHandler.getStackInSlot(i);
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                ItemStack extracted = itemHandler.extractItem(i, count, false);
                count -= extracted.getCount();
                continue;
            }

            int remaining = containerManager.useItems(player, requiredStack, inventoryStack, count);
            if (remaining != count) {
                count = remaining;
                markSlotChanged(handler, i);
            }
        }

        return count;
    }

    public static void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer) {
        Object handler = getBaublesHandler(player);
        if (!(handler instanceof IItemHandler)) {
            return;
        }

        IItemHandler itemHandler = (IItemHandler) handler;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack inventoryStack = itemHandler.getStackInSlot(i);
            if (!inventoryStack.isEmpty() && inventoryStack.getItem() == item) {
                consumer.accept(inventoryStack);
            }
        }
    }

    private static Object getBaublesHandler(EntityPlayer player) {
        if (!ensureInitialized()) {
            return null;
        }

        try {
            return getBaublesHandlerMethod.invoke(null, player);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logWarning("Failed to access Baubles handler", e);
            return null;
        }
    }

    private static boolean ensureInitialized() {
        if (initialized) {
            return available;
        }

        initialized = true;
        if (!Loader.isModLoaded(BAUBLES_MODID)) {
            return false;
        }

        try {
            Class<?> apiClass = Class.forName(BAUBLES_API_CLASS);
            getBaublesHandlerMethod = apiClass.getMethod("getBaublesHandler", EntityPlayer.class);
            available = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            logWarning("Failed to initialize Baubles compatibility", e);
        }

        return available;
    }

    private static void markSlotChanged(Object handler, int slot) {
        try {
            if (setChangedMethod == null) {
                setChangedMethod = handler.getClass().getMethod("setChanged", int.class, boolean.class);
            }
            setChangedMethod.invoke(handler, slot, true);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static void logWarning(String message, Throwable error) {
        if (warningLogged || ConstructionWandLegacy.LOGGER == null) {
            return;
        }

        warningLogged = true;
        ConstructionWandLegacy.LOGGER.warn(message, error);
    }
}