package com.xinyihl.constructionwandlegacy.material;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MaterialSessionTest {
    private final Item item = new Item().setRegistryName(new ResourceLocation("test", "session_material"));

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void aggregatesDuplicateStacksAndEnumeratesEachSourceOnce() {
        MaterialKey key = MaterialKey.of(new ItemStack(item, 1, 0));
        FakeSource first = new FakeSource("first")
                .entry(key, 32)
                .entry(key, 16);
        FakeSource second = new FakeSource("second").entry(key, 7);

        MaterialSession session = new MaterialSession(Arrays.asList(first, second));

        assertEquals(55, session.available(key));
        assertEquals(1, first.enumerations);
        assertEquals(1, second.enumerations);
        session.available(key);
        session.simulate(key, 4);
        session.keysForItem(item);
        assertEquals(1, first.enumerations);
        assertEquals(1, second.enumerations);
    }

    @Test
    public void keepsNbtVariantsIndependentAndSaturatesCounts() {
        NBTTagCompound red = new NBTTagCompound();
        red.setString("variant", "red");
        NBTTagCompound blue = new NBTTagCompound();
        blue.setString("variant", "blue");
        MaterialKey redKey = MaterialKey.of(stack(1, red));
        MaterialKey blueKey = MaterialKey.of(stack(1, blue));
        FakeSource source = new FakeSource("source")
                .entry(redKey, Long.MAX_VALUE)
                .entry(redKey, 10)
                .entry(blueKey, 4);

        MaterialSession session = new MaterialSession(Arrays.asList(source));

        assertEquals(Integer.MAX_VALUE, session.available(redKey));
        assertEquals(4, session.available(blueKey));
        assertEquals(2, session.keysForItem(item).size());
    }

    @Test
    public void reserveIsSimulationUntilCommitAndReceiptRefundsExactSourcesOnce() {
        MaterialKey key = MaterialKey.of(new ItemStack(item, 1, 0));
        FakeSource first = new FakeSource("first").entry(key, 1);
        FakeSource second = new FakeSource("second").entry(key, 2);
        MaterialSession session = new MaterialSession(Arrays.asList(first, second));

        MaterialReservation reservation = session.reserve(key, 3);

        assertNotNull(reservation);
        assertEquals(0, session.available(key));
        assertEquals(1, first.actual(key));
        assertEquals(2, second.actual(key));

        MaterialReceipt receipt = reservation.commit();

        assertNotNull(receipt);
        assertEquals(3, receipt.getCount());
        assertEquals("first", receipt.getEntries().get(0).getSourceId());
        assertEquals("second", receipt.getEntries().get(1).getSourceId());
        assertEquals(0, first.actual(key));
        assertEquals(0, second.actual(key));

        assertEquals(true, receipt.refund());
        assertEquals(1, first.actual(key));
        assertEquals(2, second.actual(key));
        assertFalse(receipt.refund());
        assertEquals(1, first.actual(key));
        assertEquals(2, second.actual(key));
    }

    @Test
    public void partialRealExtractionRollsBackAndFailsCommit() {
        MaterialKey key = MaterialKey.of(new ItemStack(item, 1, 0));
        FakeSource source = new FakeSource("changing").entry(key, 2);
        MaterialSession session = new MaterialSession(Arrays.asList(source));
        MaterialReservation reservation = session.reserve(key, 2);
        source.setActual(key, 1);

        assertNull(reservation.commit());
        assertEquals(1, source.actual(key));
        assertEquals(2, session.available(key));
    }

    @Test
    public void insufficientRequestDoesNotFreezeAnyAmount() {
        MaterialKey key = MaterialKey.of(new ItemStack(item, 1, 0));
        MaterialSession session = new MaterialSession(Arrays.asList(new FakeSource("source").entry(key, 2)));

        assertNull(session.reserve(key, 3));
        assertEquals(2, session.available(key));
    }

    @Test
    public void creativeSessionsPreserveCatalogWithoutExtracting() {
        MaterialKey catalogKey = MaterialKey.of(new ItemStack(item, 1, 0));
        MaterialKey absentKey = MaterialKey.of(new ItemStack(item, 1, 1));
        FakeSource unlimitedSource = new FakeSource("unlimited").entry(catalogKey, 2);
        MaterialSession unlimited = MaterialSession.creativeUnlimited(Arrays.asList(unlimitedSource));

        assertEquals(Integer.MAX_VALUE, unlimited.available(absentKey));
        assertEquals(1, unlimited.keysForItem(item).size());
        assertNotNull(unlimited.reserve(absentKey, 100).commit());
        assertEquals(2, unlimitedSource.actual(catalogKey));

        FakeSource catalogSource = new FakeSource("catalog").entry(catalogKey, 2);
        MaterialSession catalog = MaterialSession.creativeCatalog(Arrays.asList(catalogSource));
        MaterialReservation reservation = catalog.reserve(catalogKey, 2);
        assertNotNull(reservation);
        assertNotNull(reservation.commit());
        assertEquals(2, catalogSource.actual(catalogKey));
        assertEquals(0, catalog.available(catalogKey));
    }

    private ItemStack stack(int count, NBTTagCompound tag) {
        ItemStack stack = new ItemStack(item, count, 0);
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class FakeSource implements MaterialSource {
        private final String id;
        private final List<Amount> enumerated = new ArrayList<>();
        private final Map<MaterialKey, Integer> actual = new LinkedHashMap<>();
        private int enumerations;

        private FakeSource(String id) {
            this.id = id;
        }

        private FakeSource entry(MaterialKey key, long count) {
            enumerated.add(new Amount(key, count));
            int amount = SaturatedAmounts.fromLong(count);
            actual.put(key, SaturatedAmounts.add(actual.getOrDefault(key, 0), amount));
            return this;
        }

        private int actual(MaterialKey key) {
            return actual.getOrDefault(key, 0);
        }

        private void setActual(MaterialKey key, int count) {
            actual.put(key, count);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void enumerate(MaterialCollector collector) {
            enumerations++;
            for (Amount amount : enumerated) {
                collector.accept(amount.key, amount.count);
            }
        }

        @Override
        public MaterialReceipt extract(MaterialKey key, int count) {
            int available = actual(key);
            int extracted = Math.min(available, count);
            actual.put(key, available - extracted);
            return MaterialReceipt.of(id, key, extracted,
                    (refundKey, refundCount) -> {
                        actual.put(refundKey, SaturatedAmounts.add(actual(refundKey), refundCount));
                        return 0;
                    });
        }
    }

    private static final class Amount {
        private final MaterialKey key;
        private final long count;

        private Amount(MaterialKey key, long count) {
            this.key = key;
            this.count = count;
        }
    }
}
