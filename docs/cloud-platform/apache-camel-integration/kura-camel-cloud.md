# Apache Camel&trade; as a Kura cloud service

The default way to create a new cloud service instance backed by Camel is to use the new Web UI for cloud services. A new cloud service instance of the type `org.eclipse.kura.camel.cloud.factory.CamelFactory` has to be created. In addition to that a set of Camel routes have to be provided.

The interface with the Kura application is the Camel `vm` component. Information set "upstream" from the Kura application can be received by the Camel cloud service instance of the following endpoint `vm:camel:example`. Where `camel` is the application id and `example` is the topic.

The following code snippet writes out all of the Kura payload structure received on this topic to the logger system:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<routes xmlns="http://camel.apache.org/schema/spring">

  <route id="camel-example-topic">
    <from uri="vm:camel:example"/>
    <split>
      <simple>${body.metrics().entrySet()}</simple>
      <setHeader headerName="item">
        <simple>${body.key()}</simple>
      </setHeader>
      <setBody>
        <simple>${body.value()}</simple>
      </setBody>
      <toD uri="log:kura.data.${header.item}"/>
    </split>
  </route>

</routes>
```

The snippet splits up the incoming KuraPayload structure and creates a logger called `kura.data.<metric>` for each metric and writes out the actual value to it. The output in the log file should look like:

```
2016-11-14 16:14:34,539 [Camel (camel-10) thread #18 - vm://camel:example] INFO  k.d.intValue - Exchange[ExchangePattern: InOnly, BodyType: Long, Body: 19]
2016-11-14 16:14:34,566 [Camel (camel-10) thread #18 - vm://camel:example] INFO  k.d.doubleValue - Exchange[ExchangePattern: InOnly, BodyType: Double, Body: 10.226808617581144]
2016-11-14 16:14:35,575 [Camel (camel-10) thread #18 - vm://camel:example] INFO  k.d.intValue - Exchange[ExchangePattern: InOnly, BodyType: Long, Body: 19]
2016-11-14 16:14:35,602 [Camel (camel-10) thread #18 - vm://camel:example] INFO  k.d.doubleValue - Exchange[ExchangePattern: InOnly, BodyType: Double, Body: 10.27218775669447]
2016-11-14 16:14:36,539 [Camel (camel-10) thread #18 - vm://camel:example] INFO  k.d.intValue - Exchange[ExchangePattern: InOnly, BodyType: Long, Body: 19]
2016-11-14 16:14:36,567 [Camel (camel-10) thread #18 - vm://camel:example] INFO  k.d.doubleValue - Exchange[ExchangePattern: InOnly, BodyType: Double, Body: 10.314456684208022]
```
