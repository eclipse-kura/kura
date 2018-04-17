# Installation of Kura on Fedora / Raspberry Pi

See [../fedora/README.md](../fedora/README.md).

## Firewall

As this version of Kura does not manage the firewall of the target device it may
be necessary to do this yourself. By default this is done by `firewall-cmd`.

For example opening port 8080, for the Kura Web UI, can be done by:

    sudo firewall-cmd --permanent --add-port=8080/tcp
    sudo firewall-cmd --reload
