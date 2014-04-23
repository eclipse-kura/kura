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
===============

Building
--------

Kura uses Maven to manage the build process.

1. Clone the repo:
    `git clone -b develop https://github.com/eclipse/kura.git`

2. Build the target platform:

        cd kura/target-platform
        mvn clean install

3. Build Kura core:

        cd ../kura
        mvn -Dmaven.test.skip=true -f manifest_pom.xml clean install
        mvn -Dmaven.test.skip=true -f pom_pom.xml clean install


Setting up the development environment
--------------------------------------

### Prerequisites ###
Before installing Kura, you need to have the following programs installed in your OS.  The local emulation mode will only work on the Linux or MAC OS X operating system.

#### Java ####
* Install Java JDK 1.7 from Oracle.  
_Note: You *must* use Java version 1.7.0_25 -- as of 9/24/2013, there was a bug in Eclipse (Juno and Kepler at least) that prevented Maven from working correctly with JDK 1.7.0_40._
* Set the JAVA_HOME  environment variable to the installation directory for the JDK.
* Add JAVA_HOME/bin to your system PATH.
* Verify Java installation by running: `java -version`

#### Maven ####
* Install Maven version 3.0.5 from http://maven.apache.org
_Note: You ''must'' use version 3.0.5 -- as of 9/19/2013, building with Maven 3.1.0 failed due to a plugin incompatibility._
* Set the M2_HOME environment variable to the installation directory for Maven
* Add M2_HOME/bin to your system PATH.
* Verify Maven installation by running: `mvn --version`

#### Eclipse ####
* Create a 'git' directory to hold workspaces from GitHub
* Install latest version of Eclipse 4.2 SR2 for Java EE Developers
* Install maven plugin for eclipse (m2e) using Eclipse's "Install New Software..." dialog
* Install Tycho connector for m2e maven plugin by following:
http://www.sebastianbauer.info/index.php?page=tutorials&which=justify
* NOTE: Maven connector plugin installer breaks on JDK 1.7.0_40. Downgrade the JDK used by Eclipse to a lower version, install the Tycho connector for m2e and then move up the JRE used by Eclipse again.


### Set up the Kura workspace ###

#### Clone the git repository to your local filesystem and build ####

    cd ~/dev/git/              #cd to the location of your git repo
    git clone -b develop https://github.com/eclipse/kura.git
    cd kura/target-platform
    mvn clean install         #should succeeed without errors
    cd ../kura
    mvn -f manifest_pom.xml -Dmaven.test.skip=true clean install && mvn -f pom_pom.xml clean install         #should succeeed without errors
    mvn -f manifest_pom.xml eclipse:clean
    mvn -f manifest_pom.xml eclipse:eclipse

If mvn clean install fails in kura (unexpected element (uri:"", local:"snapshot-store")), try to delete
 /tmp/snapshot-store.xml
If mvn clean install fails in test compilation run it with "-Dmaven.test.skip=true" option


#### Configure Eclipse (Eclipse Juno or newer required) ####
* Set perspective to Java (not Java EE) 
* Install Maven 2 Eclipse (m2e) if it's not already installed
 * Help | Install New Software | Add  
 * Then enter this URL: http://download.eclipse.org/releases/juno  (or the Eclipse release that you have installed - Kepler should have m2e installed by default)
 * You will find m2e under the 'General Purpose Tools' section of the Eclipse Installer
* Install eGit (via the same URL under the Collaboration section, or "Java implementation of Git")
* Restart Eclipse


#### Import into Eclipse and build there ####
* Link your git repository to your Eclipse workspace: go into your workspace directory (cd {workspace_loc}) and then (assuming your local repository is in ~/dev/git/kura) use the following command: `ln -s ~/dev/git/kura .`
* In Eclipse select import and under Maven, select 'Existing Maven Projects' 
 * Browse to kura/kura in your git repository, and select all projects
 * If prompted, go ahead and install Tycho Project Configurators (Eclipse will need to restart)
* Link all projects to the git repository:
 * Select all projects
 * Right-click and select Team > Share Project > Git
* Expand the target-definition project and open the 'kura-equinox_3.8.1.target' file.  Click 'Set as Target Platform' in the upper right area of the center pane.  Alternatively, or if this doesn't work, browse to _Windows | Preferences | Plug-in Development | Target Platform_. Then select _Kura Target Platform Equinox 3.8.1_ and click the _Reload_ button.
** After setting this and letting Eclipse rebuild - there should be no errors in the workspace
** It may happen that you still have errors stating that some Projects are not up to date. In this case right click on the corresponding Projetcs, then Maven > Update Project
* If you have remaining errors,
** for 'distrib': open its pom.xml and choose the quick fix to permanently mark the goal as ignored. Right-click on project and select Maven > Update Project.
** for 'org.eclipse.kura.web': right-click on project and select Properties.  Select Java Build Path and click the Source tab, and click Add Folder.  Browse to target/generated-sources and click the box next to gwt.


#### Emulator ####
* You can run Kura in Eclipse by right clicking the Kura_Emulator_[OS].launch file (in src/main/resources of the org.eclipse.kura.emulator project), and doing a 'Run as OSGi Framework'.
 * This will result in errors, because by default this runs all tooling target platform bundles as well as the 'real' target platform bundles.
* After running once, you can go to 'Run Configurations' and reduce the bundle set that is running.  
 * The easiest way to do this is right-click on the Kura_Emulator_[OS].launch file and select Run As > Run Configurations.
 * Click the 'Deselect All' button to deselect all bundles, and just check the Kura bundles in the workspace, excluding all the *.test bundles.  Then click 'Add required bundles' to add all of the dependencies to the runtime configuration.
 * Click the Apply button, and the Run button.  This should start an error free OSGi/Kura runtime in the console without errors.  You can type 'ss' to see the running components.
* In case the JVM cannot find/load the native libraries try passing the _java.library.path_ to the VM. Right click on the _Kura_Emulator_Linux.launch_, select _Run As | Run Configurations_, select the _Arguments_ tab and add the system property to the _VM Arguments_. For example for a 64 bit Linux add:

        -Djava.library.path=${workspace_loc}/kura/kura/org.eclipse.kura.linux/lib/linux/x86/linux64

* You can execute tests within Eclipse by right clicking any *.test bundle and going to run as 'Junit Plug-in Test'.  This will execute all tests within that bundle.  These are also automatically run during the maven integration-test phase during command line builds.


### Running on a target device ###
* Deploy and Run
 * Make sure a Java VM is installed on the target device
 * After a clean install, archives for supported devices can be found in the kura/kura/distrib/target directory.  The archives are of the format "kura-[target-device]_2.0.0-SNAPSHOT.zip" (e.g. "kura-raspberry-pi_2.0.0-SNAPSHOT.zip").
 * If it does not exist, create an /opt/eclipse directory on the target device. Copy the appropriate archive to the /opt/eclipse directory and extract the contents of the ZIP file.
 * To start Kura, execute the script located in the newly extracted directory: `/opt/eclipse/kura-[device name]_1.0.0-SNAPSHOT/bin/start_kura_background.sh`
 * A log of Kura activity is stored in /var/log/kura.log. Kura activity can be continuously monitored by issuing the command `tail -f /var/log/kura.log`.
* Stopping Kura
 * To stop Kura from the command line, issue the command: `killall java hostapd named dhcpd`
* Reinstalling Kura
 * To completely reinstall Kura, issue the following commands:

            rm -fr /opt/eclipse/kura*
            rm -fr /tmp/.kura/
            rm /etc/init.d/firewall
            rm /etc/dhcpd-*.conf
            rm /etc/named.conf
            rm /etc/wpa_supplicant.conf
            rm /etc/hostapd.conf
            rm /tmp/coninfo-*
            rm /var/log/kura.log
            killall java hostapd named dhcpd

 * Then extract the new ZIP file and start Kura.
* Control Kura through OSGi
 * Connect to the OSGi console by issuing the below command on the target device `telnet 127.0.0.1 5002`
 * While in the console, to exit the OSGi console but leave Kura running, issue the command "disconnect"
 * While in the console, to exit the OSGi console and stop Kura, issue the command "exit"


### GWT ###

#### Google Web Toolkit ####
You need the Google Web Toolkit 2.4.0 which is not available for Eclipse Juno from the update site.
If you are lucky you might have installed the version 2.4.0 for Eclipse Indigo. In this case browse to Window | Preferences | Google | Web Toolkit. Select Use specific SDK and browse to the install location, for example  .eclipse/org.eclipse.platform_3.7.0_1011460474/plugins/com.google.gwt.eclipse.sdkbundle_2.4.0.v201206290132-rel-r37/gwt-2.4.0/ in your home directory.

#### Debugging ####
To get more useful log messages and errors when running GWT on a remote device, you can have it connect to a development code server, which can be launched from Eclipse.  With this setup, it will display source files, line numbers, and stack traces in the GWT log.

* Deploy and launch Kura on the target device.
* In Eclipse on your local machine, right-click on the org.eclipse.kura.web project, select "Run As" > "Run Configurations...". Select "Web Application" and click the "New launch configuration" button.
* Select the "Server" tab and ensure "Run built-in server" is NOT checked.
* Select the "GWT" tab. In the "URL" box, enter the url for the remote device (e.g. "http://192.168.1.123/kura"), then click "Apply". Then click "Run".
* Once it launches, it will give you a url (e.g. "http://192.168.1.123/kura?gwt.codesvr=127.0.0.1:9997").
* Open this url in a browser, and it should connect to the development code server.
* Note that the first time you open the url in a browser, it may prompt you to install the GWT Developer Plugin - go ahead and do this.


### Known Issues ###
* If you don't have an .m2 repository in your folder the Kura build will fail. Do the following

        mvn -pl org.eclipse.kura.api clean install
        mvn -pl org.eclipse.kura.deployment.agent clean install
        mvn -pl org.eclipse.kura.core clean install
        mvn clean install

* If you get a blank page logging in to the web UI at http://localhost:8080/kura, right click on the web project then select Google and GWT Compile. 
