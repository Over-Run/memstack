# Memory stack

Memory stack for FFM API.

## Overview

```java
void main() {
    // push a frame of the memory stack stored with thread-local variable
    try (var stack = MemoryStack.pushLocal()) {
        // allocate using methods in SegmentAllocator
        // you should initialize the allocated memory segment at once, either by fill((byte)0) or C functions
        var segment = stack.allocate(ValueLayout.JAVA_INT);
        // pass to C functions
        storeToPointer(segment);
        // access the memory segment
        readData(segment.get(ValueLayout.JAVA_INT, 0L));
    }
    // the memory stack automatically pops with try-with-resources statement
}
```

This is equivalent to C code:

```c
void storeToPointer(int* p) { *p = ...; }
void readData(int i);

int main() {
    int i;
    storeToPointer(&i);
    readData(i);
}
```

## Download

Maven coordinate: `io.github.over-run:memstack:VERSION`

Gradle:

```groovy
dependencies {
    implementation("io.github.over-run:memstack:0.2.0")
}
```
