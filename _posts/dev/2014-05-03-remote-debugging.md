---
layout: page
title:  "Remote debugging on target platform"
categories: [dev]
---

Eclipse Kura can be started with Java Debug Wire Protocol (JDWP) support, allowing the remote debugging of the developed application using Eclipse IDE. The procedure for remote debugging is presented in the following.

* Connect to the target platform (i.e. RaspberryPi) and stop the Kura application typing ```sudo systemctl stop kura``` or ```sudo /etc/init.d/kura stop```.

* Start Kura with Java Debug Wire Protocol (JDWP) typing ```sudo /opt/eclipse/kura/bin/start_kura_debug.sh```. This will start Kura and open an OSGi console. It will also start listening for socket connections on port 8000.

{% include alerts.html message="Starting from Java 9, the JDWP socket connector accepts only local connections by default (see [here](https://www.oracle.com/technetwork/java/javase/9-notes-3745703.html#JDK-8041435) for further details). To enable remote debugging on Java 9, the following line in ```/opt/eclipse/kura/bin/start_kura_debug.sh```: <br><br>-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n &#92;<br><br>has to be replaced with the following one: <br><br>-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=<b>*:</b>8000,suspend=n &#92;" %}

* Open the tcp port 8000 in the firewall. This can be done through the firewall tab in Kura web interface or using iptables.

* Install your application bundle on the target platform.

* From Eclipse IDE, set a breakpoint in the application code at a point that will be reached (i.e. activation method, common logging statement, etc.). Then:
  * Go to "Run -> Debug Configurations…"
  * Select “Remote Java Application” and click the “New launch configuration” button
  * For “Project:”, select the bundle project to be debugged
  * For “Connection Type:”, select the default “Standard (Socket Attach)”
  * For “Connection Properties:”, enter the IP address of the target platform and the tcp port 8000
  * Click Debug

* Eclipse will connect to the target platform VM and switch to the Debug Perspective when the breakpoint will have been hit.

* To stop the remote debugging, select the “Disconnect” button from the Debug Perspective.
