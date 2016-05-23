package io.rhiot.quickstarts.kura.camel;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.eclipse.kura.camel.router.CamelRouter;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Example of the Kura Camel application.
 */
public class GatewayRouter extends CamelRouter {

    @Override
    public void configure() throws Exception {
        from("timer://temperature").
                setBody().simple("${random(17,26)}").
                aggregate(simple("temperature"), new AggregationStrategy() {
                    @Override
                    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                        if(oldExchange == null) {
                            return newExchange;
                        } else {
                            double incomingValue = newExchange.getIn().getBody(double.class);
                            double existingValue = oldExchange.getIn().getBody(double.class);
                            newExchange.getIn().setBody((incomingValue + existingValue) / 2d);
                            return newExchange;
                        }
                    }
                }).completionInterval(SECONDS.toMillis(10)).
                to("log:averageTemperatureFromLast10Seconds");
    }

}
