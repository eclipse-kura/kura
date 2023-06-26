ARG base_image_version=latest
FROM base-kura-docker-x86_64:${base_image_version} AS kura_base
FROM registry.access.redhat.com/ubi8/ubi-minimal:latest

MAINTAINER Eclipse Kura Developers <kura-dev@eclipse.org>
LABEL maintainer="Eclipse Kura Developers <kura-dev@eclipse.org>" \
      io.k8s.description="Containerized version of the Eclipse Kura™ IoT gateway" \
      io.openshift.non-scalable=true


COPY bin/ /usr/local/bin/

COPY --from=kura_base /kura-install/kura_*_docker-x86_64-nn_installer.sh /kura-install/

ENV \
  KURA_DIR=/opt/eclipse/kura \
  JAVA_HOME=/usr/lib/jvm/jre-1.8.0 \
  SKIP_JAVA_VERSION_CHECK=true \
  LAUNCHER_VERSION="1.5.800.v20200727-1323"

RUN true && \
    rpm -ivh https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm && \
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
    microdnf -y clean all && rm -rf /var/cache/yum && \
    /kura-install/kura_*_docker-x86_64-nn_installer.sh && rm -rf /kura-install/ && \
    chmod -R go-rwx /opt/eclipse  && \
    chmod a+rx /opt/eclipse  && \
    find /opt/eclipse -type d | xargs chmod a+x && \
    chmod a+rwx /var/log && \
    `# Test for the existence of the entry point` \
    test -x "${KURA_DIR}/bin/start_kura.sh" && \
    chmod a+x -R /usr/local/bin/ && \
    install -m 0777 -d "${KURA_DIR}/data" && \
    mkdir -p ${KURA_DIR}/packages && \
    if [ "$PACKED" == "true" ]; then touch /kura.packed && sh pack-kura; fi  && \
    chmod +x /usr/local/bin/*  && \
    unpack-kura  && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.utils.fileinstall/0.6.0/de.dentrassi.kura.addons.utils.fileinstall-0.6.0.dp"  && \
    add-config-ini "felix.fileinstall.disableNio2=true"  && \
    add-config-ini "felix.fileinstall.dir=/load"  && \
    pack-kura

EXPOSE 443

VOLUME ["/load"]

ENTRYPOINT ["/usr/local/bin/kura-entry-point"]
