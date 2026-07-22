package com.xinyihl.constructionwandlegacy.coremod;

import org.junit.After;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ChunkLightingTransformerTest {
    private static final String CHUNK_CLASS = "net.minecraft.world.chunk.Chunk";
    private static final String ACCESS_INTERNAL = "com/xinyihl/constructionwandlegacy/lighting/ChunkLightingAccess";
    private static final String HOOKS_INTERNAL = "com/xinyihl/constructionwandlegacy/lighting/DeferredLightingBatch";

    @After
    public void disableTransformer() {
        ChunkLightingTransformer.setEnabled(false);
    }

    @Test
    public void disabledTransformerLeavesChunkBytesUntouched() throws IOException {
        byte[] original = readChunkClass();
        ChunkLightingTransformer.setEnabled(false);

        byte[] transformed = new ChunkLightingTransformer().transform(CHUNK_CLASS, CHUNK_CLASS, original);

        assertSame(original, transformed);
    }

    @Test
    public void enabledTransformerAddsDeferredLightingHooksAndBridges() throws IOException {
        byte[] original = readChunkClass();
        ChunkLightingTransformer.setEnabled(true);

        byte[] transformed = new ChunkLightingTransformer().transform(CHUNK_CLASS, CHUNK_CLASS, original);

        assertNotSame(original, transformed);
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        new ClassReader(transformed).accept(classNode, 0);
        assertTrue(classNode.interfaces.contains(ACCESS_INTERNAL));
        assertEquals(1, countMethods(classNode, "constructionwandlegacy$relightBlock", "(III)V"));
        assertEquals(1, countMethods(classNode, "constructionwandlegacy$propagateSkylightOcclusion", "(II)V"));
        assertEquals(1, countHookCalls(classNode, "deferRelight"));
        assertEquals(1, countHookCalls(classNode, "deferSkylightOcclusion"));
    }

    private static int countMethods(ClassNode classNode, String name, String descriptor) {
        int count = 0;
        for (MethodNode method : classNode.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                count++;
            }
        }
        return count;
    }

    private static int countHookCalls(ClassNode classNode, String hookName) {
        int count = 0;
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode invocation = (MethodInsnNode) instruction;
                    if (HOOKS_INTERNAL.equals(invocation.owner) && hookName.equals(invocation.name)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static byte[] readChunkClass() throws IOException {
        try (InputStream input = ChunkLightingTransformerTest.class.getResourceAsStream("/net/minecraft/world/chunk/Chunk.class")) {
            if (input == null) {
                throw new IOException("Chunk.class was not available on the test classpath");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
