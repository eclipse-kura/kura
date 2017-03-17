Manual steps to install Kura 3.0 on Fedora 25 / Raspberry Pi
==

See [../fedora25/README.md](../fedora25/README.md).

## Firewall

As this version of Kura does not manage the firewall of the target device it may
be necessary to do this yourself. By default this is done by `firewall-cmd`.

For example opening port 8080, for the Kura Web UI, can be done by:

    sudo firewall-cmd --permanent --add-port=8080/tcp
    sudo firewall-cmd --reload
