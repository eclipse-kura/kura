FROM eclipse/kura:develop

MAINTAINER Eclipse Kura Developers <kura-dev@eclipse.org>
LABEL maintainer "Eclipse Kura Developers <kura-dev@eclipse.org>"

RUN \
    yum -y install gdb && \
    debuginfo-install -y java-11-openjdk-headless
