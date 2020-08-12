---
layout: page
title:  "Kura Wires MQTT namespace"
categories: [wires]
---

The CloudPublisher is a WireComponent that converts a WireEnvelope in a KuraPayload and publishes it over MQTT. Each WireRecord in a WireEnvelope is trivially converted to a KuraPayload and published on a configurable semantic data or control topic. During this process, the emitter PID of the WireEnvelope is discarded.

The CloudPublisher is agnostic with respect to the contents of the WireEnvelope. It does not know for example if a WireEnvelope contains data readings emitted from a WireAsset. On the other hand, WireAssetS are first-class citizens of a Wire graph and the source of the information which is processed by the downstream Wire components. Eventually, the WireEnvelope representing the output of the processing is connected to a CloudPublisher and published to a Cloud platform.

In the simplest case, a WireAsset is directly connected to a CloudPublisher and it would be useful to publish the telemetry data under a well-known topic namespace, for example the following full topic:

``<accountName>/<deviceID>/W1/A1/<assetName>``

In this case ${assetName} matches the emitterPID of the WireEnvelope emitted by the WireAsset and received by a CloudPublisher.

Interested applications can then subscribe to (or query a message datastore for):

* ``<accountName>/<deviceID>/W1/#``  for all Wires (W) topics
* ``<accountName>/<deviceID>/W1/A1/#`` for all WireAsset (W/A) topics
* ``<accountName>/<deviceID>/W1/A1/<assetName>`` for all topics for a specific WireAsset

In a more complex scenario there might be filters between the WireAsset "source" and the CloudPublisher "sink" and the emitterPID of the WireEnvelope received by the publisher no longer matches the emitterPID of the WireEnvelope emitted by the WireAsset. However the published data still represents "Asset" (filtered) data and should be published under the topic above.

The Kura Wires model haven't the notion of source of a WireEnvelope since a given WireEnvelope instance does not move across the graph but only from one WireEmitter to downstream WireReceiverS that are free to emit something semantically different.

To overcome this issue:

* The CloudPublisher can be configured with a "semantic topic template" like ``W1/A1/$assetName`` where tokens prefixed with '$' will be expanded to the value of a property with the same name in a WireEnvelope's WireRecord.
* Into the WireAsset, it is possible to add an ``assetName`` property to the WireEnvelope's WireRecord.
