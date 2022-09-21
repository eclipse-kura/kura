# RAM Usage Considerations

During application development and before moving to production, it is advisable to understand if the amount of free RAM available on the device is enough for correct device operation.

Since RAM usage is application dependent, it is important to perform some stress tests to bring the device in the worst case conditions and verify system behavior.

Some of the aspects that should be taken into account are the following:

## Java Heap memory usage"

Java heap is used to store the Java objects and classes at runtime.

The heap should be:

  1. Large enough to satisfy the requirements of applications running inside Kura.
  2. Small enough so that the requirements of the system and applications running outside Kura are satisfied.

The size of the heap is controlled by the `-Xms` and `-Xmx` Java command line arguments. These parameters are defined in the `/opt/eclipse/kura/bin/start_kura_debug.sh` (for development mode) and `/opt/eclipse/kura/bin/start_kura_background.sh` (for production mode).


The `-Xms` parameter defines the initial size of Java heap and `-Xmx` defines the maximum size. The JVM will start using `Xms` as the size of the heap, and then it will grow the heap at runtime up to `Xmx` if needed, depending on application memory demand. 

Resizing the heap has a cost in terms of performance, for this reason `Xms` and `Xmx` are set to the same size by default on most platforms.

In order to understand if the heap is large enough, it is advisable to perform a stress test simulating the conditions of maximum memory demand by the applications running inside Kura.
For example, if a in-memory database instance is used by a DataService instance, during the test the database can be filled up to the maximum capacity to verify if this causes any issue.

Regarding point 2., it should be noted that heap memory is not necessarily backed by physical memory immediately after JVM startup. Even if the JVM performs an allocation of size `Xmx` immediately, physical memory will be assigned to the Java process by the kernel only when the memory pages are actually accessed by the JVM.

For this reason the amount of physical memory used by the JVM might appear small right after system boot and grow with time, up to the maximum size. This can happen even if the applications running inside Kura do not have high memory requirements, and can lead to potential issues that show up only after some time.

In order to recreate such issues, the `-XX:+AlwaysPreTouch` JVM command line option can be used during development to force the JVM to access all heap memory after start, causing the JVM process to use the maximum amount of physical memory immediately.

## Logging

Another aspect that can lead to RAM related issues is logging. As a general rule, it is recommended to reduce the amount of log messages produced by Kura during normal operation.

Kura default logging configuration (`/opt/eclipse/kura/log4j/log4j.xml`) depends on the platform.

The size of the files in the `/var/log` directory will be checked periodically and the files will be rotated to the persisted `/var/old_logs` directory if needed.

### External application RAM usage

If external applications are installed on the system (e.g. Docker containers), their RAM usage should be analyzed as well.

Stress tests related to Java heap size, log size and external applications can be run simultaneously to simulate a worst case scenario.