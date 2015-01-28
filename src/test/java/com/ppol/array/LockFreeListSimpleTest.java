package com.ppol.array;


import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class LockFreeListSimpleTest {

    /**
     * Simple sequential test
     */
    @Test
    public void test() {
        final int count = 100;
        final List<String> array = new LockFreeList<>();
        for (int index = 0; index < count; index++) {
            array.add("foo" + index);
        }
        MatcherAssert.assertThat(array, Matchers.hasSize(count));
        System.out.println(array.get(500));
        array.set(0, "55 element");
        System.out.println(array.get(0));
    }
}
