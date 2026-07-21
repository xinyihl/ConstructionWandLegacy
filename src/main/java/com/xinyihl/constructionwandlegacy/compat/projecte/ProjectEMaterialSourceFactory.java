package com.xinyihl.constructionwandlegacy.compat.projecte;

import com.xinyihl.constructionwandlegacy.material.*;
import com.xinyihl.constructionwandlegacy.material.source.InventoryRefunds;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ProjectEMaterialSourceFactory implements MaterialSourceFactory {
    @Override
    @Optional.Method(modid = "projecte")
    public MaterialSource create(EntityPlayer player, ItemStack wand) {
        IKnowledgeProvider knowledge = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getPersistentID());
        IEMCProxy emc = ProjectEAPI.getEMCProxy();
        return knowledge == null || emc == null ? null : new ProjectEMaterialSource(player, knowledge, emc);
    }

    private static final class ProjectEMaterialSource implements MaterialSource {
        private static final String ID = "projecte";

        private final EntityPlayer player;
        private final IKnowledgeProvider knowledge;
        private final IEMCProxy emc;
        private final SharedMaterialBudget budget;
        private final Map<MaterialKey, Long> unitCosts = new HashMap<>();

        private ProjectEMaterialSource(EntityPlayer player, IKnowledgeProvider knowledge, IEMCProxy emc) {
            this.player = player;
            this.knowledge = knowledge;
            this.emc = emc;
            this.budget = new SharedMaterialBudget(Math.max(0L, knowledge.getEmc()));
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        @Optional.Method(modid = "projecte")
        public void enumerate(MaterialCollector collector) {
            if (!isKnowledgeAvailable()) {
                return;
            }
            Set<MaterialKey> seen = new HashSet<>();
            for (ItemStack stack : knowledge.getKnowledge()) {
                if (stack.isEmpty() || !knowledge.hasKnowledge(stack) || !emc.hasValue(stack)) {
                    continue;
                }
                long unitValue = emc.getValue(stack);
                if (unitValue <= 0L) {
                    continue;
                }
                MaterialKey key = MaterialKey.of(stack);
                if (seen.add(key)) {
                    unitCosts.put(key, unitValue);
                    budget.register(key, unitValue);
                    collector.accept(key, Math.max(0L, knowledge.getEmc()) / unitValue);
                }
            }
        }

        @Override
        public int availableCapacity(MaterialKey key, int cachedAvailable) {
            return budget.available(key, cachedAvailable);
        }

        @Override
        public int reserveCapacity(MaterialKey key, int requested, int cachedAvailable) {
            return budget.reserve(key, requested, cachedAvailable);
        }

        @Override
        public void finishReservation(MaterialKey key, int count, boolean committed) {
            budget.finish(key, count, committed);
        }

        @Override
        @Optional.Method(modid = "projecte")
        public MaterialReceipt extract(MaterialKey key, int count) {
            Long unitValue = unitCosts.get(key);
            if (count <= 0 || unitValue == null || unitValue <= 0L || !isServerKnowledgeUsable()) {
                return MaterialReceipt.empty();
            }
            ItemStack definition = key.createStack(1);
            try {
                if (!knowledge.hasKnowledge(definition) || !emc.hasValue(definition)) {
                    return MaterialReceipt.empty();
                }
                long currentEmc = Math.max(0L, knowledge.getEmc());
                int extracted = Math.min(count, SaturatedAmounts.fromLong(currentEmc / unitValue));
                if (extracted <= 0) {
                    return MaterialReceipt.empty();
                }
                long cost = unitValue * extracted;
                knowledge.setEmc(currentEmc - cost);
                sync();
                return MaterialReceipt.of(ID, key, extracted, (refundKey, refundCount) -> refund(refundKey, refundCount, unitValue));
            } catch (RuntimeException exception) {
                return MaterialReceipt.empty();
            }
        }

        private int refund(MaterialKey key, int count, long unitValue) {
            if (!isServerKnowledgeUsable()) {
                return InventoryRefunds.refund(player, key, count);
            }
            try {
                long cost = unitValue * count;
                knowledge.setEmc(SaturatedAmounts.add(Math.max(0L, knowledge.getEmc()), cost));
                sync();
                return 0;
            } catch (RuntimeException exception) {
                return InventoryRefunds.refund(player, key, count);
            }
        }

        private boolean isKnowledgeAvailable() {
            if (player == null || player.isDead || player.world == null) {
                return false;
            }
            try {
                return ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getPersistentID()) == knowledge;
            } catch (RuntimeException exception) {
                return false;
            }
        }

        private boolean isServerKnowledgeUsable() {
            return InventoryRefunds.isUsable(player) && isKnowledgeAvailable();
        }

        private void sync() {
            if (player instanceof EntityPlayerMP) {
                try {
                    knowledge.sync((EntityPlayerMP) player);
                } catch (RuntimeException ignored) {
                    // EMC was already changed; a later sync can repair the client view.
                }
            }
        }
    }
}
