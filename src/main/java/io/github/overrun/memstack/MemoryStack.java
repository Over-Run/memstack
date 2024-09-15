package io.github.overrun.memstack;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.function.Consumer;

/**
 * <h2>Memory stack</h2>
 * Memory stack is backed with a {@linkplain MemorySegment memory segment}.
 * Each allocation returns a slice of the segment starts at the current offset
 * (modulo additional padding to satisfy alignment constraint),
 * with the given size.
 * <p>
 * It extends {@link SegmentAllocator}, which allows allocating from the given data.
 * <p>
 * It does not extend {@link Arena} since the memory stack is not supposed to be a long-alive arena allocator.
 * The stack itself does not bind to any segment scope;
 * it just slices the backing segment.
 * <p>
 * To re-associate a memory segment with the memory stack, use {@link #asArena()}.
 * <p>
 * Memory stack is not thread-safe;
 * consider using the {@linkplain #ofLocal() local stacks} to manage with threads.
 * <h3>Push and pop</h3>
 * It remembers the current offset when {@link #push()} is called,
 * then it resets to the previous offset when {@link #pop()} is called.
 * <p>
 * It extends {@link AutoCloseable} to allow using try-with-resources statement to call {@code pop} automatically.
 * <p>
 * The push and pop operations must be symmetric.
 * <p>
 * Using memory stack without push and pop operations
 * has the same effect as {@linkplain SegmentAllocator#slicingAllocator(MemorySegment) slicing allocator}.
 *
 * @author squid233
 * @since 0.1.0
 */
public interface MemoryStack extends SegmentAllocator, AutoCloseable {
    /**
     * Creates a default memory stack backed with the given memory segment and {@linkplain #frameCount() frame count}.
     *
     * @param segment    the memory segment to be sliced
     * @param frameCount the frame count of the memory stack
     * @return a new memory stack
     * @throws IllegalArgumentException if {@code segment} is {@linkplain MemorySegment#isReadOnly() read-only}
     *                                  or {@code frameCount <= 0}
     */
    static MemoryStack of(MemorySegment segment, int frameCount) {
        assertWritable(segment);
        checkSize(frameCount, "invalid frame count");
        return new DefaultMemoryStack(segment, frameCount);
    }

    /**
     * Creates a memory stack,
     * backed with a memory segment allocated with an {@linkplain Arena#ofAuto() auto arena} and the given size,
     * with the given {@linkplain #frameCount() frame count}.
     *
     * @param byteSize   the size of the memory segment
     * @param frameCount the frame count of the memory stack
     * @return a new memory stack
     * @throws IllegalArgumentException if {@code segment} is {@linkplain MemorySegment#isReadOnly() read-only},
     *                                  {@code byteSize <= 0} or {@code frameCount <= 0}
     * @see #of(MemorySegment, int)
     */
    static MemoryStack of(long byteSize, int frameCount) {
        checkSize(byteSize, "invalid stack size");
        return of(Arena.ofAuto().allocate(byteSize), frameCount);
    }

    /**
     * Creates a memory stack with the default size and {@linkplain #frameCount() frame count}.
     *
     * @return a new memory stack
     * @see #of(long, int)
     * @see StackConfigurations#STACK_SIZE
     * @see StackConfigurations#FRAME_COUNT
     */
    static MemoryStack of() {
        return of(StackConfigurations.STACK_SIZE.get(), StackConfigurations.FRAME_COUNT.get());
    }

    /**
     * {@return the memory stack for the current thread}
     *
     * @see #of()
     */
    static MemoryStack ofLocal() {
        class Holder {
            static final ThreadLocal<MemoryStack> TLS = ThreadLocal.withInitial(MemoryStack::of);
        }
        return Holder.TLS.get();
    }

    /**
     * Calls {@link #push()} of the {@linkplain #ofLocal() local memory stack}.
     *
     * @return the local memory stack
     */
    static MemoryStack pushLocal() {
        return ofLocal().push();
    }

    /**
     * Calls {@link #pop()} of the {@linkplain #ofLocal() local memory stack}.
     */
    static void popLocal() {
        ofLocal().pop();
    }

    private static void assertWritable(MemorySegment segment) {
        if (segment.isReadOnly()) {
            throw new IllegalArgumentException("read-only segment");
        }
    }

    private static void checkSize(long size, String message) {
        if (size <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * {@inheritDoc}
     * The returned memory segment is a slice of the {@linkplain #segment() backing segment}
     * and is not initialized with zero.
     * <p>
     * Use {@link MemorySegment#fill(byte) fill((byte)0)} to initialize with zero.
     *
     * @throws IndexOutOfBoundsException if there is not enough space to allocate
     * @throws IllegalArgumentException  if {@code byteSize < 0}, {@code byteAlignment <= 0},
     *                                   or if {@code byteAlignment} is not a power of 2
     */
    @Override
    MemorySegment allocate(long byteSize, long byteAlignment);

    /**
     * Remembers the current offset and pushes a frame for next allocations.
     * <p>
     * The memory stack expands the internal frames array by 1.5x if there is not enough space to push.
     *
     * @return {@code this}
     */
    MemoryStack push();

    /**
     * Pops to the previous frame and sets the current offset.
     *
     * @throws IndexOutOfBoundsException if the frame index reached the bottom of the stack
     */
    void pop();

    /**
     * Calls {@link #pop()}.
     */
    @Override
    default void close() {
        pop();
    }

    /**
     * {@return the count of the offsets this stack can store}
     */
    int frameCount();

    /**
     * {@return the current frame index}
     */
    int frameIndex();

    /**
     * {@return the current offset of this stack}
     */
    long stackPointer();

    /**
     * Sets the offset of this stack.
     *
     * @param pointer the new offset
     */
    void setPointer(long pointer);

    /**
     * {@return the backing memory segment}
     */
    MemorySegment segment();

    /**
     * Wraps this memory stack into an arena for re-associating a memory segment with
     * {@link MemorySegment#reinterpret(Arena, Consumer) MemorySegment::reinterpret}.
     * <p>
     * The obtained arena closes when this stack is popped.
     *
     * @return the arena that wraps this memory stack
     */
    Arena asArena();
}
