package com.xinyihl.constructionwandlegacy.material.source;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CapturedEndpointIdentityTest {
    @Test
    public void requiresBothCurrentParentAndOwnerIdentity() {
        Object parent = new Object();
        Object owner = new Object();

        assertTrue(CapturedEndpointIdentity.matches(parent, parent, owner, owner));
        assertFalse(CapturedEndpointIdentity.matches(parent, new Object(), owner, owner));
        assertFalse(CapturedEndpointIdentity.matches(parent, parent, owner, new Object()));
        assertFalse(CapturedEndpointIdentity.matches(null, null, owner, owner));
        assertFalse(CapturedEndpointIdentity.matches(parent, parent, null, null));
    }
}
