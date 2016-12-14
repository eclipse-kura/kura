# Eclipse Kura emulator based on Apache Karaf

This is a docker image of the Eclipse Kura emulator running on Apache Karaf.

## Building

Build this docker image by executing:

    sudo docker build -t kura-emulator github.com/eclipse/kura#develop:kura/docker/karaf-emulator

If you want to build a different repository or branch:

    sudo docker build -t kura-emulator --build-arg GIT_REPO=https://github.com/ctron/kura --build-arg GIT_BRANCH=feature/karaf_docker_1 github.com/ctron/kura#feature/karaf_docker_1:kura/docker/karaf-emulator

## Starting

Start the emulator with:

    sudo docker run -ti -p 8181:8181 kura-emulator

## Using

Navigate your web browser to:

 * http://localhost:8181 – For the Kura Web UI (credentials: admin/admin)
 * http://localhost:8181/system/console – For the Kura Web UI (credentials: karaf/karaf)

**Warning**: Do not use the Karaf Web UI to edit Kura configuration. This will break the Kura configuration. 

