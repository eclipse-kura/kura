FROM registry.access.redhat.com/ubi8/ubi:latest as builder

MAINTAINER Eclipse Kura Developers <kura-dev@eclipse.org>
LABEL maintainer="Eclipse Kura Developers <kura-dev@eclipse.org>" \
      io.k8s.description="Containerized version of the Eclipse Kura™ IoT gateway"

ARG GIT_REPO
ARG GIT_BRANCH
ARG KURA_COMMIT
ARG PACKED=false

RUN chmod a+x -R /usr/local/bin
RUN dnf -y install git java-11-openjdk-devel maven zip

ENV \
  GIT_REPO=${GIT_REPO:-https://github.com/eclipse/kura.git} \
  GIT_BRANCH=${GIT_BRANCH:-develop} \
  JAVA_HOME=/usr/lib/jvm/jre-11 \
  LAUNCHER_VERSION="1.5.800.v20200727-1323"

RUN echo "$GIT_REPO / $GIT_BRANCH / $KURA_COMMIT"

COPY . /context

RUN true && \
    if [ -d /context/.git ]; then \
      mv /context /kura; \
      mv /kura/kura/container/kura_ubi /context; \
    else \
      git clone --depth 1 "$GIT_REPO" -b "$GIT_BRANCH"; \
    fi

WORKDIR /kura

RUN if [ -n "$KURA_COMMIT" ]; then git checkout "$KURA_COMMIT"; fi && git log -1

# Replace broken 'nn' script
RUN cp kura/distrib/src/main/sh/extract.sh kura/distrib/src/main/sh/extract_nn.sh

ENV \
  MAVEN_PROPS="-DskipTests -Dmaven.artifact.threads=20"

RUN mvn -B -f target-platform/pom.xml clean install -Pno-mirror $MAVEN_PROPS
RUN mvn -B -f kura/pom.xml clean install $MAVEN_PROPS -Pspeedup
RUN mvn -B -f kura/distrib/pom.xml clean install $MAVEN_PROPS -Pintel-up2-centos-7-nn -nsu

WORKDIR /

RUN ls -la /kura/kura/distrib/target


FROM registry.access.redhat.com/ubi8/ubi-minimal:latest


RUN true && \
    microdnf -y update && \
    microdnf -y install \
        java-1.8.0-openjdk-headless \
        procps \
        zip \
        unzip \
        gzip \
        tar \
        psmisc \
        socat \
        dos2unix \
        openssl \
        net-tools \
        hostname \
        which \
        findutils \
    && \
    microdnf -y clean all && rm -rf /var/cache/yum


COPY --from=builder /kura/kura/distrib/target/kura_*_intel-up2-centos-7-nn_installer.sh /installer.sh

ENV \
  KURA_DIR=/opt/eclipse/kura \
  JAVA_HOME=/usr/lib/jvm/jre-1.8.0 \
  SKIP_JAVA_VERSION_CHECK=true \
  LAUNCHER_VERSION="1.5.800.v20200727-1323"

RUN mkdir -p /usr/local/bin

COPY --from=builder /context/bin/* /usr/local/bin

RUN true && \
    /installer.sh && \
    echo "Kura installer completed" && \
    rm installer.sh && \
    chmod a+rw -R /opt/eclipse && \
    find /opt/eclipse -type d | xargs chmod a+x && \
    chmod a+rwx /var/log && \
    `# Test for the existence of the entry point` \
    test -x "${KURA_DIR}/bin/start_kura.sh" && \
    chmod a+x /usr/local/bin/* && \
    install -m 0777 -d "${KURA_DIR}/data" && \
    sed -i 's/kura.primary.network.interface/#kura.primary.network.interface/g' ${KURA_DIR}/framework/kura.properties && \
    mkdir -p ${KURA_DIR}/packages && \
    if [ "$PACKED" == "true" ]; then touch /kura.packed && pack-kura; fi && \
    \
    unpack-kura && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.utils.fileinstall/0.6.0/de.dentrassi.kura.addons.utils.fileinstall-0.6.0.dp" && \
    add-config-ini "felix.fileinstall.disableNio2=true" && \
    add-config-ini "felix.fileinstall.dir=/load" && \
    pack-kura

EXPOSE 443

VOLUME ["/load"]

ENTRYPOINT ["/usr/local/bin/kura-entry-point"]
