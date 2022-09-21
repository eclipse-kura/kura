# How to Use CAN bus

The Kura CAN bus protocol implementation is based on the SocketCAN interface, which provides a socket interface to userspace applications. The sockets are designed as can0 and can1. The SocketCAN package is an implementation of Controller Area Network (CAN) protocols. For more information, refer to the following link: <https://www.kernel.org/doc/Documentation/networking/can.txt>.

# Configure the CAN bus Driver

The CAN network must be initialized prior to communications. Verify that the CAN driver module has been enabled in the kernel by issuing the following command:

```shell
ifconfig -a
```
The connections “can0” and “can1” should be displayed.

Next, the sockets must be enabled and configured using the following commands (the bitrate value must be set according to the bitrate of the device that will be connected):
```shell
ip link set can0 type can bitrate 50000 triple-sampling on
ip link set can0 up
ip link set can1 type can bitrate 50000 triple-sampling on
ip link set can1 up
```
# Use the CAN bus Driver in Kura

To use the Can bus Driver in Kura, the bundle **org.eclipse.kura.protocol.can** must be installed. Refer to the section [Application Management](/administration/application-management/) for more information.

Once this bundle is installed and verified, the CanConnectionService provides access to basic functionalities of the CAN network, including:

- **sendCanMessage** – sends an array of bytes in RAW mode.

- **receiveCanMessage** – reads frames in RAW mode waiting on socket CAN.

Refer to the following Kura javadocs for more information: <http://download.eclipse.org/kura/docs/api/5.2.0/apidocs/>.

Also, for information about the wrapper that this service utilizes, refer to the following link: <https://github.com/entropia/libsocket-can-java>.