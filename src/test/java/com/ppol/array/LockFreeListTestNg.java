package com.ppol.array;

import java.util.Collection;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * Concurrent calls
 */
public final class LockFreeListTestNg {

    private final Collection<String> collection = new LockFreeList<>();

    @Test(invocationCount = 2000, threadPoolSize = 8)
    public void testAddConcurrently() {
        collection.add("foo");
    }

    @AfterClass
    public void afterTest() {
        MatcherAssert.assertThat(this.collection, Matchers.hasSize(2000));
    }
}