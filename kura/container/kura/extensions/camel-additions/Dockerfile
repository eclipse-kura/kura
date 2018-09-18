FROM eclipse/kura:develop

MAINTAINER Eclipse Kura Developers <kura-dev@eclipse.org>
LABEL maintainer "Eclipse Kura Developers <kura-dev@eclipse.org>"

RUN \
    unpack-kura && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.amqp/0.6.0/de.dentrassi.kura.addons.amqp-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.camel.groovy/0.6.0/de.dentrassi.kura.addons.camel.groovy-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.camel.gson/0.6.0/de.dentrassi.kura.addons.camel.gson-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.camel.http/0.6.0/de.dentrassi.kura.addons.camel.http-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.camel.iec60870/0.6.0/de.dentrassi.kura.addons.camel.iec60870-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.camel.jetty/0.6.0/de.dentrassi.kura.addons.camel.jetty-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.camel.swagger/0.6.0/de.dentrassi.kura.addons.camel.swagger-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.camel.weather/0.6.0/de.dentrassi.kura.addons.camel.weather-0.6.0.dp" && \
    dp-install "https://repo1.maven.org/maven2/de/dentrassi/kura/addons/de.dentrassi.kura.addons.paho/0.6.0/de.dentrassi.kura.addons.paho-0.6.0.dp" && \
    pack-kura

