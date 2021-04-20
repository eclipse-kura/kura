---
layout: page
title:  "Message Publishing Backoff Delay"
categories: [config]
---

In order to have a finer control on the data flow, when a device reconnects to a remote cloud platform, ESF integrates into the Data Service a Backoff delay feature that limits the rate of messages sent.

This feature, enabled by default, integrates the [Token Bucket concept](https://en.wikipedia.org/wiki/Token_bucket) to limit the bursts of messages sent to a remote cloud platform.

In the image below, the parameters that need to be tuned, in the Data Service, to take advantage of this feature:

![kura_cloud_stack]({{ site.baseurl }}/assets/images/config/Kura_rate-limit.png)

- **enable.rate.limit** - Enables the token bucket message rate limiting.

- **rate.limit.average** - The average message publishing rate. It is intended as the number of messages per unit of time.

- **rate.limit.time.unit** - The time unit for the rate.limit.average.

- **rate.limit.burst.size** - The token bucket burst size.

The default setup limits the data flow to **1 message per second with a bucket size of 1 token**.

{% include alerts.html message='This feature needs to be properly tuned by the System Administrator in order to prevent delays in the remote cloud platform due to messages stacked at the edge. **If not sure of the number of messages that your gateways will try to push to the remote platform, disable this feature.**' %}
