---
layout: page
title:  "FIFO Wire Component"
categories: [wires]
---

This page describes the usage of the FIFO component in Kura Wires.

The current Kura Wires threading model allows any component to perform potentially blocking operations when a wire envelope is received.

The fact that the wire envelopes are delivered synchronously implies
that if a wire component performs blocking operations, other components in the same subgraph might be blocked as well, introducing delays in the processing of the graph.

The FIFO component can be used for decoupling blocking or slow wire components from other parts of the graph that cannot tolerate delays.

![fifo_graph]({{ site.baseurl }}/assets/images/wires/FIFO.png)

In the graph above, the **NODELAY** component cannot tolerate potential delays introduced by the **DB** component, adding a FIFO component allows to decouple the two components.

This component implements a FIFO queue that operates as follows:

* Received envelopes are added to the queue, adding an envelope to the queue is usually (see below) a non blocking operation.
* A dedicated thread pops the envelopes from the queue and delivers them to downstream components.

In this way the threads running the upstream components are not affected by blocking operations performed by the downstream components.

In the example above there will be two threads that manage the processing of the graph:

1. A thread from the **TIMER** Quartz scheduler pool handles the processing for the **NODELAY** component and submits the envelopes produced by it to the queue of the FIFO component.

2. A second thread introduced by the FIFO component pops the received envelopes from the queue and dispatches them to the **DB** component, performing the processing required by it.

In this way the **NODELAY** and **DB** components are decoupled because they are managed by different threads.

## Configuration

The FIFO component configuration is composed by the following properties:

* **queue.capacity**: The size of the queue in terms of number of storable wire envelopes.

* **discard.envelopes** : Configures the behavior of the component in case of full queue.

  * If set to `true`, envelopes received when the queue is full will be dropped. In this mode submitting an envelope to the queue is always a non blocking operation.
  This mode should be used if occasionally losing wire envelopes is acceptable for the application, but introducing delays for upstream components is not.

  * If set to `false`, adding an envelope when the queue is full will block the submitting thread until there is space on the queue.
  In this mode submitting an envelope to the FIFO component can be a blocking operation if the queue is full.
  This mode should be used if dropping wire envelopes is unacceptable for upstream components.

  The probability of dropping envelopes in **discard.envelopes**=`true` mode or the probability of blocking upstream components in **discard.envelopes**=`false` mode can be controlled by setting a proper queue size.
