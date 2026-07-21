package com.xinyihl.constructionwandlegacy.material;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class SharedMaterialBudgetTest {
    private final Item item = new Item().setRegistryName(new ResourceLocation("test", "shared_budget"));

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void reservationsAcrossDifferentKeysShareOneBudget() {
        MaterialKey cheap = MaterialKey.of(new ItemStack(item, 1, 0));
        MaterialKey expensive = MaterialKey.of(new ItemStack(item, 1, 1));
        BudgetSource source = new BudgetSource(cheap, expensive);
        MaterialSession session = new MaterialSession(Collections.singletonList(source));

        MaterialReservation cheapReservation = session.reserve(cheap, 8);
        assertNotNull(cheapReservation);
        assertEquals(2, session.available(cheap));
        assertEquals(0, session.available(expensive));
        assertNull(session.reserve(expensive, 1));

        cheapReservation.cancel();
        assertEquals(10, session.available(cheap));
        assertEquals(4, session.available(expensive));

        MaterialReservation expensiveReservation = session.reserve(expensive, 4);
        assertNotNull(expensiveReservation);
        assertEquals(0, session.available(cheap));
        assertNotNull(expensiveReservation.commit());
        assertEquals(0, session.available(cheap));
        assertEquals(0, session.available(expensive));
    }

    private static final class BudgetSource implements MaterialSource {
        private final MaterialKey cheap;
        private final MaterialKey expensive;
        private final SharedMaterialBudget budget = new SharedMaterialBudget(100L);

        private BudgetSource(MaterialKey cheap, MaterialKey expensive) {
            this.cheap = cheap;
            this.expensive = expensive;
            budget.register(cheap, 10L);
            budget.register(expensive, 25L);
        }

        @Override
        public String getId() {
            return "budget";
        }

        @Override
        public void enumerate(MaterialCollector collector) {
            collector.accept(cheap, 10);
            collector.accept(expensive, 4);
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
        public MaterialReceipt extract(MaterialKey key, int count) {
            return MaterialReceipt.of(getId(), key, count, (refundKey, refundCount) -> 0);
        }
    }
}
