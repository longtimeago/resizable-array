package com.ppol.array;

import com.google.testing.threadtester.AnnotatedTestRunner;
import com.google.testing.threadtester.ThreadedAfter;
import com.google.testing.threadtester.ThreadedBefore;
import com.google.testing.threadtester.ThreadedMain;
import com.google.testing.threadtester.ThreadedSecondary;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class LockFreeListCorrectnessTest {
    private LockFreeList<String> collection = new LockFreeList<>();

    @ThreadedBefore
    public void before() {
        System.out.println("ThreadedBefore");
        this.collection = new LockFreeList<>();
    }

    @ThreadedMain
    public void mainThread() {
        System.out.println("ThreadedMain");
        this.collection.add("foo");
    }

    @ThreadedSecondary
    public void secondThread() {
        System.out.println("ThreadedSecondary");
        this.collection.add("bar");
    }

    @ThreadedAfter
    public void after() {
        System.out.println("ThreadedAfter");
        MatcherAssert.assertThat(this.collection.size(), Matchers.is(2));
        MatcherAssert.assertThat(this.collection, Matchers.hasItem("foo"));
        MatcherAssert.assertThat(this.collection, Matchers.hasItem("bar"));
    }

    @Test
    public void testResizableArray() {
        final AnnotatedTestRunner runner = new AnnotatedTestRunner();
        runner.runTests(this.getClass(), LockFreeList.class);
    }
}