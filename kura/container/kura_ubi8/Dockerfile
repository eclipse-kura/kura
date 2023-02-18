ARG BASE_IMAGE
FROM ${BASE_IMAGE}
ARG JAVA_LINK
ARG ESF_RPM

## to build it
## docker build -t esf .
## to run it
## docker run -p 443:443 -i -t esf

COPY docker_root /

RUN yum -y update && \
    yum -y install epel-release && \
    yum -y update && \
    yum -y install wget openssl tar gzip bzip2 net-tools sudo bluez unzip psmisc telnet iproute dos2unix socat lzo zlib-devel systemd openvpn easy-rsa procps && \
    wget -O jre.tar ${JAVA_LINK} && \
    tar -xzvf jre.tar && \
    mkdir -p /opt/java && \
    mv zulu*/ /opt/java/zulu && \
    update-alternatives --install "/usr/bin/java" "java" "/opt/java/zulu/bin/java" 1 &&\
	update-alternatives --install "/usr/bin/keytool" "keytool" "/opt/java/zulu/bin/keytool" 1 && \
    rm jre.tar && \
    rpm -ivh --force --nodeps /${ESF_RPM}*.rpm && \
    rm /${ESF_RPM}*.rpm && \
    mv /log4j.xml /opt/eurotech/esf/log4j/log4j.xml && \
    mv /log4j-journal.xml /opt/eurotech/esf/log4j/log4j-journal.xml && \
    mv /start_kura_general.sh /opt/eurotech/esf/bin && \
    chmod +x /opt/eurotech/esf/bin/start_kura_general.sh && \
    rm -rf /tmp/* && \
    echo 'root:eurotech' | chpasswd &&\
    sed -i 's/Docker/ESF-Docker-Rocky/' /opt/eurotech/esf/framework/kura.properties &&\
    echo "export VISIBLE=now" >> /etc/profile

EXPOSE 80 443 4443 5002 1883 8883 8000

ENTRYPOINT ["/opt/eurotech/esf/bin/start_kura_general.sh"]