# Extra container-related tools and resources

## Making use of Apache Felix File Install

This image includes [Apache Felix FileInstall](https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html "Apache Felix File Install"), which monitors a directory and loads all OSGi bundles it detects during runtime. Adding a new bundle is as easy as dropping an OSGi JAR file into a directory. Uninstalling is done by deleting the file and updates are simply done by overwriting the bundle with a newer version.

**Note:** The location of the directory changed from `/opt/eclipse/kura/load` to `/load`. The old path is
          deprecated. It might still work for a while, but it might break at any time.

File Install loads bundles from `/load` which is also defined as a Docker volume,
so that you can link this up with your container host:

    docker run -ti -p 8443:443 -v /home/user/path/to/bundles:/load:z eclipse/kura

Now you can access `/home/user/path/to/bundles` on your host machine and bundles will be loaded
by Kura inside the Docker container.

**Note:** It may be that a bundle, which is first installed, needs to be manually started using the Kura Web UI.

## Running with JMX enabled

Running with JMX or debugging enabled can sometimes be quite helpful. However it is disabled by default. 

### On Linux

If you want to run the image with JMX enabled use the following command on Linux:

    docker run -ti -eJAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Xdebug -Xrunjdwp:transport=dt_socket,address=9011,server=y,suspend=n" -p 8443:443 --expose 9010 --expose 9011 -p 9010:9010 -p 9011:9011 eclipse/kura

### On Windows

If you want to run the image with JMX enabled use the following command on Windows: 

    docker run -ti -eJAVA_OPTS="-Dcom.sun.management.jmxremote -Djava.rmi.server.hostname=<IP> -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Xdebug -Xrunjdwp:transport=dt_socket,address=9011,server=y,suspend=n" -p 8443:443 --expose 9010 --expose 9011 -p 9010:9010 -p 9011:9011 eclipse/kura

Where *<IP>* is the Docker address, you can find it by using *ipconfig* and search for *DockerNAT* address, for instance:

    Ethernet adapter vEthernet (DockerNAT):
    Connection-specific DNS Suffix  . :
    IPv4 Address. . . . . . . . . . . : 10.0.75.1
    Subnet Mask . . . . . . . . . . . : 255.255.255.0
    Default Gateway . . . . . . . . . :

The JMX port defined is 9010 and the Remote debug port is 9011. Both ports are not exposed by default and have to be exposed from the command line using `--expose 9010 --expose 9011`.
 
## Running in OpenShift

There also is an [OpenShift template](openshift/README.md) in both directories, which can be used to deploy this image into [OpenShift](https://www.openshift.org/).

## Building extended images

If you want to add additional content to the Kura installation inside the Docker image,
it is possible to extend the installation.

Also see: [extensions/artemis/README.md](extensions/artemis/README.md)
