# Eclipse Kura emulator based on Apache Karaf

This is a docker image of the Eclipse Kura emulator running on Apache Karaf.

## Building

Build this docker image by executing:

    sudo docker build -t kura-emulator -f kura/docker/karaf-emulator/Dockerfile github.com/eclipse/kura#develop

## Starting

Start the emulator with:

    sudo docker run -ti -p 8181:8181 kura-emulator

## Using

Navigate your web browser to:

 * http://localhost:8181 – For the Kura Web UI (credentials: admin/admin)
 * http://localhost:8181/system/console – For the Kura Web UI (credentials: karaf/karaf)

**Warning**: Do not use the Karaf Web UI to edit Kura configuration. This will break the Kura configuration. 

