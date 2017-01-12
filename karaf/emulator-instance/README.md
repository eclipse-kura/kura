# Running Eclipse Kura on Apache Karaf

The emulator can be started either from the command line using:

    cd emulator-instance
    mvn exec:java -Prun

Or it can also be started by launching the launch profile "Apache Karaf Emulator"
from the Eclipse launch menu.

## Rebuilding the emulator 

When you made changes to Kura you will need to perform `mvn install` for
the modified Kura modules and afterwards need to re-build the emulator
distribution and maybe the Karaf features before that as well:

    # Rebuild features (if necessary)
    cd features
    mvn install
    cd ..
    
    # Rebuild emulator
    cd emulator-instance
    mvn package
    mvn exec:java -Prun
    
You can also launch the Maven launch configuration "karaf-emulator" to start
build and start the emulator.

## Using other IDEs

While developing Kura itself with Eclipse is the best way,
using Karaf as emulator it is easily possible to use a different IDE
for developing Kura addons and testing them. Simply follow the
standard Karaf ways like using "Apache File Install" or Karaf features.
