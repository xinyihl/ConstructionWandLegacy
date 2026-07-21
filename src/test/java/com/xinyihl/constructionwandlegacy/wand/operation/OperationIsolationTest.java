package com.xinyihl.constructionwandlegacy.wand.operation;

import net.minecraft.world.World;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;

public class OperationIsolationTest {
    private static void assertNestedChangesDoNotHoldWorld(Class<?> operationClass) {
        for (Class<?> nested : operationClass.getDeclaredClasses()) {
            if (!nested.getSimpleName().endsWith("Change")) {
                continue;
            }
            for (Field field : nested.getDeclaredFields()) {
                assertFalse(operationClass.getSimpleName() + " captures World in " + field.getName(), World.class.isAssignableFrom(field.getType()));
            }
        }
    }

    @Test
    public void appliedChangesDoNotCaptureExecutionWorld() {
        assertNestedChangesDoNotHoldWorld(PlaceOperation.class);
        assertNestedChangesDoNotHoldWorld(PlantOperation.class);
        assertNestedChangesDoNotHoldWorld(DestroyOperation.class);
    }
}
