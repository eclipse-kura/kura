# Cloud Connection Status

Kura can report the state of the cloud connection outside of the web UI, using an LED on the gateway or the Kura log. This makes it possible to check whether a gateway is connected by looking at the device, without logging in to it.

The output is selected with the `ccs.status.notification.url` property. By default the status is reported in the log, so an LED must be explicitly configured.

For the connection details shown in the web UI, see [Gateway Status](gateway-status.md).

## LED Patterns

When an LED is configured, it shows one of the following patterns:

| Pattern | Timing | Meaning |
|---|---|---|
| Off | - | Kura is not connected and is not trying to connect. |
| Fast blinking | 100 ms on, 100 ms off | A connection attempt is in progress. |
| Slow blinking | 300 ms on, 300 ms off | Kura is not connected and will retry after a delay. |
| On | - | Kura is connected. |
| Heartbeat | 150 ms on, 150 ms off, 150 ms on, 600 ms off | Not used by Kura itself. Available to custom components, see [Custom Components](#custom-components). |

A gateway that cannot reach its broker alternates between the two blinking patterns: it blinks slowly while waiting and quickly during each connection attempt.

The LED turns off when:

- the cloud connection is disconnected on request, for example from the [Cloud Connections](cloud-connections.md) page
- **Connect Auto-on-startup** is disabled in the [Data Service configuration](data-service-configuration.md) and no connection is active
- a [connection schedule](data-service-configuration.md#connection-schedule) is enabled and the gateway is waiting for the next connection time

!!! note
    If **Connect Auto-on-startup** is disabled and an established connection is lost unexpectedly, the LED is not updated and stays on. Use the [Gateway Status](gateway-status.md) page to check the connection in this case.

## Configuration

Set `ccs.status.notification.url` in `/opt/eclipse/kura/user/kura_custom.properties`, then restart Kura. The value is only read when the service starts.

Values set in `kura_custom.properties` override the defaults in `/opt/eclipse/kura/framework/kura.properties`, which contains commented examples of the most common values.

The following values are available:

| Value | Description |
|---|---|
| `ccs:log` | Reports the status in the Kura log. This is the default value in `kura.properties`. |
| `ccs:none` | Disables the status notifications. This also applies if the property is not set. |
| `ccs:led:<terminal>` | Uses the GPIO with the given terminal index. |
| `ccs:led:terminal:<terminal>` | Same as `ccs:led:<terminal>`. |
| `ccs:led:name:<name>` | Uses the GPIO with the given name. |
| `ccs:linux_led:<path>` | Uses a Linux LED class device, where `<path>` is its directory. Kura writes `1` or `0` to the `brightness` file in that directory. |

The GPIO terminal indexes and names are the ones defined by the `GPIOService` of the gateway.

### Examples

Using the GPIO with terminal index 16:

```
ccs.status.notification.url=ccs:led:16
```

Using a GPIO by name:

```
ccs.status.notification.url=ccs:led:name:LED_1
```

Using a Linux LED class device:

```
ccs.status.notification.url=ccs:linux_led:/sys/class/leds/led1_green
```

### Inverted GPIO LEDs

If the LED is on when the GPIO is low, add `:inverted` at the end of a `ccs:led` value:

```
ccs.status.notification.url=ccs:led:16:inverted
ccs.status.notification.url=ccs:led:name:LED_1:inverted
```

This option is not available for `ccs:linux_led`.

### Combining Values

Multiple values can be separated by `;`. This allows the same configuration to work on gateways with different hardware:

```
ccs.status.notification.url=ccs:linux_led:/sys/class/leds/led1_green;ccs:led:16
```

When a configuration includes an LED, Kura selects the output in this order:

1. The Linux LED, if the configured directory exists.
2. The GPIO LED, if a `GPIOService` is available.
3. The Kura log.

!!! warning
    If the values select different output types, the last one wins. For example `ccs:led:16;ccs:log` reports the status only in the log. It is not possible to report the status on an LED and in the log at the same time.

### Invalid Values

If the value starts with `ccs:` but cannot be interpreted, for example `ccs:led:abc`, the notifications are disabled and a warning is written to the log. A value that does not start with `ccs:` also disables the notifications, without a warning.

If an LED cannot be accessed while Kura is running, an error is written to the log and the LED stops being updated until the next status change.

## Log Output

With `ccs:log`, or when no configured LED can be used, Kura writes one of these messages to the log at every status change. The messages refer to the LED pattern even if no LED is present:

| Message | Status |
|---|---|
| `Notification LED on` | On |
| `Notification LED off` | Off |
| `Notification LED slow blinking` | Slow blinking |
| `Notification LED fast blinking` | Fast blinking |
| `Notification LED heartbeating` | Heartbeat |

## Multiple Cloud Connections

The gateway has a single status output, shared by all of its cloud connections. If more than one connection is configured, the LED does not reliably indicate the state of a specific connection. In this case, use the [Cloud Connections](cloud-connections.md) page to check each connection.

## Custom Components

Applications can report their own status through the `CloudConnectionStatusService` API by implementing `CloudConnectionStatusComponent` and registering with the service. Each component has a priority, and the output always shows the status of the registered component with the highest priority.

The service provides the following priority values:

| Constant | Value |
|---|---|
| `PRIORITY_MAX` | `Integer.MAX_VALUE` |
| `PRIORITY_CRITICAL` | 400 |
| `PRIORITY_HIGH` | 300 |
| `PRIORITY_MEDIUM` | 200 |
| `PRIORITY_LOW` | 100 |
| `PRIORITY_MIN` | `Integer.MIN_VALUE` |

Kura uses `PRIORITY_MEDIUM` during a connection attempt and `PRIORITY_LOW` otherwise. A component registered with a higher priority replaces the cloud connection status until it is unregistered.
