FROM debian:stable-slim:latest

LABEL maintainer="Eurotech <docker-maint@nginx.com>" \
      description="Containerized version of the Eclipse Kura™ IoT gateway"

ARG PACKAGE_FILE

COPY bin/ /usr/local/bin/
COPY $PACKAGE_FILE /kura.deb

ENV \
  KURA_DIR=/opt/eclipse/kura \
  LAUNCHER_VERSION="1.6.900"

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y /kura.deb && \
    rm /kura.deb

EXPOSE 443

ENTRYPOINT ["/usr/local/bin/kura-entry-point"]
