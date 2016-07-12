Kura
====
An OSGi-based Application Framework for M2M Service Gateways


Background
----------
Until recently, machine-to-machine projects have been approached as embedded systems designed around custom hardware, custom software, and custom network connectivity. The challenge of developing such projects was given by the large customization and integration costs and the small re-usability across similar engagements. The results were often proprietary systems leveraging proprietary protocols.

The emergence of the service gateway model, which operates on the edge of an M2M deployment as an aggregator and controller, has opened up new possibilities. Cost effective service gateways are now capable of running modern software stacks opening the world of M2M to enterprise technologies and programming languages. Advanced software frameworks, which isolate the developer from the complexity of the hardware and the networking sub-systems, can now be offered to complement the service gateway hardware into an integrated hardware and software solution.


Description
-----------
Kura aims at offering a Java/OSGi-based container for M2M applications running in service gateways. Kura provides or, when available, aggregates open source implementations for the most common services needed by M2M applications. Kura components are designed as configurable OSGi Declarative Service exposing service API and raising events. While several Kura components are in pure Java, others are invoked through JNI and have a dependency on the Linux operating system.

For more information, see the [Eclipse project proposal](http://www.eclipse.org/proposals/technology.kura/).


Development Model
-----------------
Development on Kura follows the [gitflow model](http://nvie.com/posts/a-successful-git-branching-model/).  Thus, the working copy is in the develop branch, and the master branch is used for releases.


Getting Started
-----------------
If you are new to Kura, please visit the [getting started page](https://wiki.eclipse.org/Kura/Getting_Started).

To raise an issue, please report a bug on [Eclipse bugzilla](https://bugs.eclipse.org/bugs/buglist.cgi?bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&classification=Technology&list_id=4625660&order=Bug%20Number&product=Kura&query_format=advanced).

Test modification