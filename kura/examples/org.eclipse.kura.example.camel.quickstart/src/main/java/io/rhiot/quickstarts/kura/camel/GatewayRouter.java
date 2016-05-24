package io.rhiot.quickstarts.kura.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.kura.camel.camelcloud.DefaultCamelCloudService;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.camel.router.CamelRouter;
import org.eclipse.kura.message.KuraPayload;

import java.util.Random;

/**
 * Example of the Kura Camel application.
 */
public class GatewayRouter extends CamelRouter {

    @Override
    public void configure() throws Exception {
        KuraCloudComponent cloudComponent = new KuraCloudComponent();
        cloudComponent.setCloudService(new DefaultCamelCloudService(camelContext));
        camelContext.addComponent("kura-cloud", cloudComponent);

        from("timer://heartbeat").
                process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        KuraPayload payload = new KuraPayload();
                        payload.addMetric("temperature", new Random().nextInt(20));
                        exchange.getIn().setBody(payload);
                    }
                }).to("kura-cloud:myapp/topic");

        from("kura-cloud:myapp/topic").
                choice().
                  when(simple("${body.metrics()[temperature]} < 10"))
                .to("log:lessThanTen")
                  .when(simple("${body.metrics()[temperature]} == 10"))
                  .to("log:equalToTen")
                .otherwise()
                .to("log:greaterThanTen");

        from("timer://xmltopic").
                process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        KuraPayload payload = new KuraPayload();
                        payload.addMetric("temperature", new Random().nextInt(20));
                        exchange.getIn().setBody(payload);
                    }
                }).to("kura-cloud:myapp/xmltopic");
    }

}