FROM fedora:24

MAINTAINER Jens Reimann <jreimann@redhat.com>

LABEL Description="This is an emulator instance of Eclipse Kura"

ARG GIT_REPO=https://github.com/eclipse/kura.git
ARG GIT_BRANCH=develop

ENV JAVA_HOME=/usr/lib/jvm/jre-1.8.0

RUN dnf -y install git java-1.8.0-openjdk-devel maven && \
    git clone "$GIT_REPO" -b "$GIT_BRANCH"&& \
    cd /kura && ./build-all.sh -Pspeedup -Pnot-existing && \
    cd /kura/karaf && mvn clean install && \
    cd /kura/karaf/deployment/fragments && mvn clean install && \
    cd /kura/karaf/deployment/targets/docker-emulator-fedora && mvn clean install && \
    dnf remove -y git java-1.8.0-openjdk-devel maven && \
    dnf install -y jre-1.8.0-openjdk-headless && \
    dnf install -y /kura/karaf/deployment/targets/docker-emulator-fedora/target/*.rpm && \
    rm -Rf /kura /root/.m2 && dnf -y clean all

EXPOSE 8181

ENTRYPOINT ["/opt/eclipse/kura/bin/karaf"]
CMD ["console"]