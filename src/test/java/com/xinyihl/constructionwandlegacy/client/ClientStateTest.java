package com.xinyihl.constructionwandlegacy.client;

import com.xinyihl.constructionwandlegacy.config.RuleSnapshot;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientStateTest {
    @Test
    public void undoBlocksAreDefensivelyCopiedAndImmutable() {
        ClientState state = new ClientState();
        Set<BlockPos> source = new HashSet<>();
        source.add(BlockPos.ORIGIN);

        state.replaceUndoBlocks(source);
        source.clear();

        assertEquals(Collections.singleton(BlockPos.ORIGIN), state.getUndoBlocks());
        try {
            state.getUndoBlocks().clear();
            org.junit.Assert.fail("undo set must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void staleRuleRevisionsAreIgnoredAndDisconnectResetsState() {
        ClientState state = new ClientState();
        RuleSnapshot revisionTwo = rules(2L);
        RuleSnapshot revisionOne = rules(1L);

        assertTrue(state.applyServerRules(revisionTwo));
        assertFalse(state.applyServerRules(revisionOne));
        assertEquals(2L, state.getServerRulesRevision());

        state.resetConnection();
        assertEquals(Long.MIN_VALUE, state.getServerRulesRevision());
        assertTrue(state.getUndoBlocks().isEmpty());
        assertTrue(state.getPreview().isEmpty());
    }

    private static RuleSnapshot rules(long revision) {
        return RuleSnapshot.create(revision, 9, 27, 81, 256, false,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
