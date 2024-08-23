package io.github.overrun.memstack;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;

/**
 * The default implementation of {@link MemoryStack}.
 *
 * @author squid233
 * @since 0.1.0
 */
public class DefaultMemoryStack implements MemoryStack {
    private final MemorySegment segment;
    private long[] frames;
    private long offset = 0L;
    private int frameIndex = 0;

    /**
     * Creates the default memory stack with the given segment and frame count.
     *
     * @param segment    the memory segment
     * @param frameCount the frame count
     */
    public DefaultMemoryStack(MemorySegment segment, int frameCount) {
        this.segment = segment;
        this.frames = new long[frameCount];
    }

    private MemorySegment trySlice(long byteSize, long byteAlignment) {
        long min = segment.address();
        long start = ((min + offset + byteAlignment - 1) & -byteAlignment) - min;
        MemorySegment slice = segment.asSlice(start, byteSize, byteAlignment);
        offset = start + byteSize;
        return slice;
    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0) {
            throw new IllegalArgumentException("The provided allocation size is negative: " + byteSize);
        }
        if (byteAlignment <= 0 || ((byteAlignment & (byteAlignment - 1)) != 0L)) {
            throw new IllegalArgumentException("Invalid alignment constraint: " + byteAlignment);
        }
        return trySlice(byteSize, byteAlignment);
    }

    @Override
    public MemoryStack push() {
        if (frameIndex >= frames.length) {
            frames = Arrays.copyOf(frames, frames.length * 3 / 2);
        }
        frames[frameIndex] = offset;
        frameIndex++;
        return this;
    }

    @Override
    public void pop() {
        if (frameIndex <= 0) {
            throw new IndexOutOfBoundsException("stack frame underflow");
        }
        frameIndex--;
        offset = frames[frameIndex];
    }

    @Override
    public int frameCount() {
        return frames.length;
    }

    @Override
    public int frameIndex() {
        return frameIndex;
    }

    @Override
    public long stackPointer() {
        return offset;
    }

    @Override
    public void setPointer(long pointer) {
        offset = pointer;
    }

    @Override
    public MemorySegment segment() {
        return segment;
    }
}
