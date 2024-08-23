package io.github.overrun.memstack.test;

import io.github.overrun.memstack.MemoryStack;
import io.github.overrun.memstack.StackConfigurations;
import org.junit.jupiter.api.Test;

import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * @author squid233
 * @since 0.1.0
 */
public class MemoryStackTest {
    @Test
    void testPushAndPop() {
        MemoryStack local = MemoryStack.ofLocal();

        assertEquals(0L, local.stackPointer());

        local.setPointer(4L);
        assertEquals(4L, local.stackPointer());

        try {
            local.push();
            local.setPointer(8L);
            assertEquals(8L, local.stackPointer());
        } finally {
            local.pop();
        }
        assertEquals(4L, local.stackPointer());

        // try-with-resources
        try (var stack = local.push()) {
            stack.setPointer(8L);
            assertEquals(8L, stack.stackPointer());
        }
        assertEquals(4L, local.stackPointer());

        // static methods
        try {
            MemoryStack stack = MemoryStack.pushLocal();
            stack.setPointer(8L);
            assertEquals(8L, stack.stackPointer());
        } finally {
            MemoryStack.popLocal();
        }
        assertEquals(4L, local.stackPointer());

        try (MemoryStack stack = MemoryStack.pushLocal()) {
            stack.setPointer(8L);
            assertEquals(8L, stack.stackPointer());
        }
        assertEquals(4L, local.stackPointer());
    }

    @Test
    void testAllocate() {
        assertEquals(0L, MemoryStack.ofLocal().stackPointer());
        try (MemoryStack stack = MemoryStack.pushLocal()) {
            stack.allocate(ValueLayout.JAVA_INT);
            assertEquals(4L, stack.stackPointer());

            stack.allocate(ValueLayout.JAVA_INT);
            assertEquals(8L, stack.stackPointer());

            stack.allocate(ValueLayout.JAVA_BYTE);
            assertEquals(9L, stack.stackPointer());

            try (MemoryStack stack1 = MemoryStack.pushLocal()) {
                stack1.allocate(ValueLayout.JAVA_INT);
                assertEquals(16L, stack1.stackPointer());
            }
            assertEquals(9L, stack.stackPointer());

            stack.allocate(ValueLayout.JAVA_INT);
            assertEquals(16L, stack.stackPointer());

            stack.allocate(ValueLayout.JAVA_LONG);
            assertEquals(24L, stack.stackPointer());

            stack.allocate(ValueLayout.JAVA_INT);
            assertEquals(28L, stack.stackPointer());
        }
        assertEquals(0L, MemoryStack.ofLocal().stackPointer());
    }

    @Test
    void testOverflow() {
        MemoryStack stack = MemoryStack.of();
        for (int i = 0; i < stack.frameCount(); i++) {
            stack.push();
        }
        assertThrowsExactly(IndexOutOfBoundsException.class, stack::push);
    }

    @Test
    void testUnderflow() {
        assertThrowsExactly(IndexOutOfBoundsException.class, MemoryStack.of()::pop);
    }

    @Test
    void testOutOfMemory() {
        assertThrowsExactly(IndexOutOfBoundsException.class, () ->
            MemoryStack.of().allocate(StackConfigurations.STACK_SIZE.get() + 1));
    }
}
