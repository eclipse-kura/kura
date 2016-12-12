# Eclipse Kura emulator based on Apache Karaf

This is a docker image of the Eclipse Kura emulator running on Apache Karaf.

## Building

Build this docker image by executing:

    sudo docker build -t kura-emulator -f kura/docker/karaf-emulator/Dockerfile github.com/eclipse/kura#develop
    
## Starting

Start the emulator with:

    sudo docker run -ti kura-emulator
