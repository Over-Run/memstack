package io.github.overrun.memstack;

/**
 * The configurations of memory stack.
 *
 * @author squid233
 * @since 0.1.0
 */
public final class StackConfigurations {
    /**
     * The default stack size in bytes.
     * Default value: 65536 (64 KiB)
     */
    public static final Entry<Long> STACK_SIZE = new Entry<>(64L * 1024);
    /**
     * The default {@linkplain MemoryStack#frameCount() frame count} for a memory stack.
     * Default value: 8
     */
    public static final Entry<Integer> FRAME_COUNT = new Entry<>(8);

    private StackConfigurations() {
    }

    /**
     * A configuration entry
     *
     * @param <T> the type of the value
     */
    public static final class Entry<T> {
        private T value;

        private Entry(T value) {
            this.value = value;
        }

        /**
         * {@return the value}
         */
        public T get() {
            return value;
        }

        /**
         * Sets the value.
         *
         * @param value the new value
         */
        public void set(T value) {
            this.value = value;
        }
    }
}
