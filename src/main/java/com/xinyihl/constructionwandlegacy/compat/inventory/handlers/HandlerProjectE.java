package com.xinyihl.constructionwandlegacy.compat.inventory.handlers;

import com.xinyihl.constructionwandlegacy.api.IInventoryHandler;
import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

import java.util.function.Consumer;

public class HandlerProjectE implements IInventoryHandler {

    @Override
    @Optional.Method(modid = "projecte")
    public int countItems(EntityPlayer player, ItemStack requiredStack, ContainerManager containerManager) {
        IKnowledgeProvider knowledge = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getPersistentID());
        IEMCProxy emcProxy = ProjectEAPI.getEMCProxy();
        return emcProxy.hasValue(requiredStack) && knowledge.hasKnowledge(requiredStack) ? (int) (knowledge.getEmc() / emcProxy.getValue(requiredStack)) : 0;
    }

    @Override
    @Optional.Method(modid = "projecte")
    public int useItems(EntityPlayer player, ItemStack requiredStack, int count, ContainerManager containerManager) {
        IKnowledgeProvider knowledge = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getPersistentID());
        IEMCProxy emcProxy = ProjectEAPI.getEMCProxy();
        if (knowledge.hasKnowledge(requiredStack) && knowledge.getEmc() >= emcProxy.getValue(requiredStack)) {

            int maxCount = (int) (knowledge.getEmc() / emcProxy.getValue(requiredStack));
            int useCount = Math.min(count, maxCount);
            long cost = useCount * emcProxy.getValue(requiredStack);
            knowledge.setEmc(knowledge.getEmc() - cost);

            if (player instanceof EntityPlayerMP) {
                knowledge.sync((EntityPlayerMP) player);
            }

            count -= useCount;
        }
        return count;
    }

    @Override
    @Optional.Method(modid = "projecte")
    public void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer) {
        IKnowledgeProvider knowledge = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getPersistentID());
        IEMCProxy emcProxy = ProjectEAPI.getEMCProxy();
        for (ItemStack stack : knowledge.getKnowledge()) {
            if (!stack.isEmpty() && stack.getItem() == item && emcProxy.hasValue(stack) && knowledge.getEmc() >= emcProxy.getValue(stack)) {
                consumer.accept(stack);
            }
        }
    }
}
