FROM maven:3-jdk-8

MAINTAINER Jens Reimann <jreimann@redhat.com>

RUN git clone https://github.com/ctron/kura -b feature/fix_quickstart_2 && cd kura/kura/examples/org.eclipse.kura.example.camel.quickstart && mvn package