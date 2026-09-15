# Remote debugging on target platform

Eclipse Kura can be started with Java Debug Wire Protocol (JDWP) support, allowing the remote debugging of the developed application using any Java IDE. The procedure for remote debugging is presented in the following.

* Connect to the target platform (i.e. RaspberryPi) and stop the Kura service typing `sudo systemctl stop kura`.

* Start Kura in debug mode typing `sudo /opt/eclipse/kura/bin/start_kura.sh --debug`. This will start Kura in the foreground with the OSGi console on the terminal and will also start listening for JDWP socket connections on port 8000 (and for console connections on port 5002). The debug mode also writes the garbage collection log to `/var/log/kura-gc.log` and a heap dump to `/var/log/kura-heapdump.hprof` in case of out of memory errors.

* Open the TCP port 8000 in the firewall. This can be done through the Firewall section of the Kura web UI or using the [firewall REST APIs](../references/rest-apis/rest-network-configuration-api.md).

* Install your application bundle on the target platform, as a package or from the OSGi console (see [Deploy and Debug Applications](./deploy-and-debug-applications.md)).

* From the IDE, set a breakpoint in the application code at a point that will be reached (i.e. activation method, common logging statement, etc.) and attach a remote debugger to the device. In Eclipse IDE:
    * Go to "Run -> Debug Configurations…"
    * Select "Remote Java Application" and click the "New launch configuration" button
    * For "Project:", select the bundle project to be debugged
    * For "Connection Type:", select the default "Standard (Socket Attach)"
    * For "Connection Properties:", enter the IP address of the target platform and the TCP port 8000
    * Click Debug

    In Visual Studio Code, add an `attach` launch configuration of type `java` with the `hostName` of the device and `port` 8000; in IntelliJ IDEA, create a *Remote JVM Debug* run configuration with the same host and port.

* The IDE will connect to the target platform VM and stop at the breakpoint when it is hit. The sources of the bundle are those of the Maven project generated with the [Kura Addon Archetype](./kura-addon-archetype.md); the Kura sources can be attached from the `-sources` artifacts of the Kura bundles, which Maven downloads on request.

* To stop the remote debugging, disconnect the debugger from the IDE. Then stop Kura with the `exit` command of the OSGi console and start the service again with `sudo systemctl start kura`.
