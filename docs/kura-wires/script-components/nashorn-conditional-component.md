# Nashorn Conditional Component

!!! warning
    This component is deprecated as of Kura version 5.3 since no more available on Java 8.

The **Conditional** component is a multiport-enabled component that implements the if-then-else logic in the Wire Composer. It is provided by default in every Kura installation.

![Nashorn Conditional Component Example](./images/nashorn-conditional-component-example.png)

In the image above a simple usage example of the Conditional component: a timer ticks and the envelope is received by the Conditional component. The message is then processed and can be sent downstream to the logger component (**then** port) or to a publisher (**else** port).

The choice between the two ports is performed based on a condition expressed in the Conditional component configuration.

![Nashorn Conditional Component Configuration Example](./images/nashorn-conditional-component-conf.png)