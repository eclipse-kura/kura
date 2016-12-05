FROM fedora:24
MAINTAINER Jens Reimann <jreimann@redhat.com>

RUN dnf -y install jre-1.8.0-headless procps

COPY target/*.rpm /

RUN rpm -Uvh *.rpm

EXPOSE 8181
ENV JAVA_HOME=/usr/lib/jvm/jre-1.8.0

CMD /opt/eclipse/kura/bin/karaf console