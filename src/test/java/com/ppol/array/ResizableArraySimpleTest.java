package com.ppol.array;


import java.util.Collection;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class ResizableArraySimpleTest {

    /**
     * Simple sequential test
     */
    @Test
    public void test() {
        final int count = 100;
        final Collection<String> array = new ResizableArray<>();
        for (int index = 0; index < count; index++) {
            array.add("foo" + index);
        }
        MatcherAssert.assertThat(array, Matchers.hasSize(count));
    }
}
