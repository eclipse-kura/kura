# Eclipse Kura on Apache Karaf

This is an experimental Karaf setup.

At the moment it is not possible to install Kura as a Karaf feature.
It is necessary to create a new Karaf distribution with all Kura components
provisioned. 

## Building a Karaf distribution

In order to build any Karaf distribution for Kura you will need
to perform a local build of Kura first. The easiest way to make
a full build (including Karaf) is to simply run the `build-all.sh` script
in the root of the repository.

## Running the emulator

The emulator is a ready to run Karaf setup which can be started from the
command line using Maven:

    cd emulator-instance
    mvn exec:java -Prun
    
For more information see the [README of the emulator project](emulator-instance/README.md).
