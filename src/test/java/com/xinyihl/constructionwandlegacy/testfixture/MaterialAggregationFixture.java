package com.xinyihl.constructionwandlegacy.testfixture;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Test fixture for the one-pass material aggregation contract planned for Phase 3.
 */
public final class MaterialAggregationFixture {
    private MaterialAggregationFixture() {
    }

    public static List<Aggregate> aggregate(Collection<ItemStack> inventorySlots) {
        List<Aggregate> aggregates = new ArrayList<>();
        for (ItemStack stack : inventorySlots) {
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
                continue;
            }

            Aggregate existing = find(aggregates, stack);
            if (existing == null) {
                ItemStack key = stack.copy();
                key.setCount(1);
                aggregates.add(new Aggregate(key, stack.getCount()));
            } else {
                existing.count = saturatedAdd(existing.count, stack.getCount());
            }
        }
        return Collections.unmodifiableList(aggregates);
    }

    private static Aggregate find(List<Aggregate> aggregates, ItemStack query) {
        for (Aggregate aggregate : aggregates) {
            if (stackEquals(aggregate.key, query)) {
                return aggregate;
            }
        }
        return null;
    }

    private static boolean stackEquals(ItemStack first, ItemStack second) {
        return !first.isEmpty()
                && !second.isEmpty()
                && first.getItem() == second.getItem()
                && first.getMetadata() == second.getMetadata()
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static int saturatedAdd(int first, int second) {
        long sum = (long) first + second;
        return sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    public static final class Aggregate {
        private final ItemStack key;
        private int count;

        private Aggregate(ItemStack key, int count) {
            this.key = key;
            this.count = count;
        }

        public ItemStack getKey() {
            return key.copy();
        }

        public int getCount() {
            return count;
        }
    }
}
