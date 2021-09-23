---
layout: page
title:  "Getting Started"
categories: [dev]
---

## Setting up the Kura Development Environment

This document describes how to set up the development environment for Eclipse Kura, which consists of the following components:

* JVM (Java JDK SE 8 or Open JDK 8)
* Eclipse IDE
* Kura Workspace setup

The Kura development environment may be installed on a Windows, Linux, or Mac OS.  The setup instructions will be the same across OSs though each system may have unique characteristics.

{% include alerts.html message="The local emulation of Kura code is only supported in Linux and Mac, not in Windows. " %}

## JVM Installation

Download and install Java SE from [Java SE Downloads](http://www.oracle.com/technetwork/java/javase/downloads/index.html). Use the latest version of Java SE Development Kit and download the version appropriate for your system.

For additional information regarding the installation of Java 8 on all supported operating systems, see [JDK 8 and JRE 8 Installation Guide](http://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html).


## Eclipse IDE

The Eclipse IDE is an open source development tool that consists of an integrated development environment (IDE) and a plug-in system for managing extensions.

For automated installation see [Oomph installer](#oomph-installer) below.

### Installing Eclipse

Before installing Eclipse, you should choose directory locations for the Eclipse install and its workspaces.

{% include alerts.html message="{::nomarkdown}The following points should be kept in mind regarding Eclipse installs and workspaces:<br/><ul><li>The directory location of the Eclipse workspaces should be chosen carefully. Once Eclipse is installed and workspaces are created, they should never be moved to another location in the file system.</li><li>There may be multiple installs of Eclipse (of different or similar versions), and single instances of each install can be run simultaneously; but there should never be more that one instance of a specific install running at the same time (to avoid corruption to the Eclipse environment).</li><li>Each workspace should be used with only one Eclipse install. You should avoid opening the workspace from more than one installation of Eclipse.</li><li>For the purposes of this guide, only a single Eclipse installation will be covered.</li></ul>{:/}" %}

Download the current distribution of Eclipse for your OS from the [Eclipse download site](http://www.eclipse.org/downloads/). Choose the Eclipse IDE for Java EE Developers.

The zipped Eclipse file will be downloaded to the local file system and can be saved to a temporary location that can be deleted after Eclipse has been installed. After the file has been downloaded, it should be extracted to the Eclipse installs directory. The following screen capture shows the installation in Linux using an eclipse/installs/ directory. The Eclipse executable will then be found in the "eclipse\installs\eclipse\" directory.  This installation will be different depending on the operating system.

![Image 1]({{ site.baseurl }}/assets/images/kura_setup/image001.png)

Because there may potentially be future Eclipse installs extracted into this location, before doing anything else rename the directory, such as to “eclipse/installs/_juno1_/”. Once you begin using this Eclipse install, **it should not be moved or renamed later**.
![Image 2]({{ site.baseurl }}/assets/images/kura_setup/image002.png)

### Installing mToolkit

An additional plugin, mToolkit, is needed to allow remote connectivity to an OSGi framework on a Kura-enabled target device.  To install mToolkit into Eclipse, use the following steps:

* Open the **Help \| Install New Software...** menu.
* Add the following URL as an update site based on your version of Eclipse
    * Eclipse Mars and older: **http://mtoolkit-mars.s3-website-us-east-1.amazonaws.com**
    * Eclipse Neon: **http://mtoolkit-neon.s3-website-us-east-1.amazonaws.com**
* Install the "mToolkit" feature (you need to uncheck the **Group items by category** checkbox in order to see the feature)
* Restart Eclipse. In the menu **Window \| Show View \| Other**, there should be an **mToolkit \| Frameworks** option. If so, the plugin has been installed correctly.

## Workspaces

For automated installation see [Oomph installer](#oomph-installer) below.

### Creating an Eclipse Workspace

Run Eclipse by clicking its executable in the install directory.

When Eclipse is run for the first time, a workspace needs to be created.  A single workspace will contain all the Java code/projects/bundles, Eclipse configuration parameters, and other relevant files for a specific business-level product.  If the *Use this as the default* option is selected, the designated workspace becomes the default each time you run Eclipse.

If a workspace has not already been defined, or if you are creating a different workspace for another development project, enter a new workspace name.  The workspace should be named appropriate to the project/product being developed.

{% include alerts.html message="Once you begin using a particular workspace, _it should not be moved or renamed at any time_." %}

![Image 3]({{ site.baseurl }}/assets/images/kura_setup/image003.png)

Otherwise, select an existing workspace and click **OK**. After Eclipse is running, you can select the Eclipse menu **File \| Switch Workspace \| Other** to create or open a different workspace.

After the new workspace opens, click the Workbench icon to display the development environment.

![Image 4]({{ site.baseurl }}/assets/images/kura_setup/image004.png)

### Importing Kura User Workspace

To set up your Kura project workspace, you will need to download the [Kura User Workspace archive](https://www.eclipse.org/kura/downloads.php).

From the Eclipse File menu, select the **Import** option. In the Import dialog box, expand the **General** heading, select **Existing Projects into Workspace**, and then click **Next**.

Now click the **Select archive file** option button and browse to the archive file, such as user_workspace_archive_2.0.1.zip.

![Image 5]({{ site.baseurl }}/assets/images/kura_setup/image005.png)

Finally, click *Finish* to import the projects. At this point, you should have four projects in your workspace. The four projects are as follows:
* org.eclipse.kura.api – This is the core Kura API.
* org.eclipse.kura.demo.heater – This is an example project that you can use as a starting point for creating your own bundle.
* org.eclipse.kura.emulator – This is the emulator project for running Kura within Eclipse (Linux/Mac only).
* target-definition – This is a set of required bundles that are dependencies of the APIs and Kura.

![Image 6]({{ site.baseurl }}/assets/images/kura_setup/image006.png)

Eclipse will also report some errors at this point. See the next section to resolve those errors.

### Workspace Setup

Click the arrow next to the *target-definition* project in the workspace and double-click **kura-equinox_3.11.1.target** to open it.

![Image 7]({{ site.baseurl }}/assets/images/kura_setup/image007.png)

In the Target Definition window, click the link **Set as Target Platform**. Doing so will reset the target platform, rebuild the Kura projects, and clear the errors that were reported. At this point, you are ready to begin developing Kura-based applications for your target platform.

### Run the Eclipse Kura Emulator

To start the Eclipse Kura emulator, select the "Kura_Emulator_Linux.launch" or "Kura_Emulator_OSX.launch" profile from "org.eclipse.kura.emulator" project -> "launch_configs" -> "Kura_Emulator_[OSX | Linux].launch" and run it with "Run as".

The Eclipse Kura Web UI will be available at the following URL: `http://127.0.0.1:8080` with username and password **admin**.

## Eclipse Oomph installer

The Eclipse Oomph installer is an easy way to install and configure the Eclipse IDE to start developing on Kura.
Download the latest Eclipse Installer appropriate for your platform from [Eclipse Downloads](https://www.eclipse.org/downloads/eclipse-packages/)

* Start the Eclipse Installer
* Switch to advanced mode (in simple mode you cannot add a custom installer)
* Select "Eclipse IDE for Eclipse Committers", select the latest "Product Version" and select a Java 11+ VM. Then click the Next button.

![Image 8]({{ site.baseurl }}/assets/images/kura_setup/image008.png)

* Select "Eclipse Kura" project under the "Eclipse Projects" menu. If it isn't available, add a new installer by URL: https://raw.githubusercontent.com/eclipse/kura/develop/kura/setups/kura.setup under the "Github Projects" menu. Then click the Next button.

![Image 9]({{ site.baseurl }}/assets/images/kura_setup/image009.png)

* Update Eclipse Kura Git repository's username (prefer the anonymous HTTPS option, link to your fork) and customize further settings if you like (e.g. Root install folder, Installation folder name). Then click the Next button.
* Leave all Bootstrap Tasks selected and press the Finish button.
* Accept the licenses and unsigned content.
* Wait for the installation to finish, a few additional plugins will be installed.
* At first startup Eclipse IDE will checkout the code and perform a full build.

The result will be an Eclipse IDE with all the recommended plug-ins already available, code will be checked out and built, workspace will be set up, a few Working Sets will be prepared with most projects building without errors.

The next step is to get the rest of the projects to build, for which you might need to build them in the console with specific profiles available e.g. the CAN bundle.

### Run the Eclipse Kura Emulator

To start the Eclipse Kura emulator, select the "Eclipse Kura Emulator.launch" profile from "Other Projects" -> "setups" -> "launchers" and open it with "Run as" -> "Run Configurations...". Then click on the "Arguments" tab and update the "VM arguments" as follows to adapt the paths to the folder structure created by the Oomph installer:

```
-Dkura.have.net.admin=false -Dorg.osgi.framework.storage=/tmp/osgi/framework_storage -Dosgi.clean=true -Dosgi.noShutdown=true -Declipse.ignoreApp=true  -Dorg.eclipse.kura.mode=emulator -Dkura.configuration=file:${workspace_loc}/../git/kura/kura/emulator/org.eclipse.kura.emulator/src/main/resources/kura.properties -Ddpa.configuration=/tmp/kura/dpa.properties -Dlog4j.configurationFile=file:${workspace_loc}/../git/kura/kura/emulator/org.eclipse.kura.emulator/src/main/resources/log4j.xml -Dkura.data=${workspace_loc}/kura/data -Dkura.snapshots=${workspace_loc}/kura/user/snapshots -Dorg.eclipse.equinox.http.jetty.customizer.class=org.eclipse.kura.jetty.customizer.KuraJettyCustomizer
```

![Image 10]({{ site.baseurl }}/assets/images/kura_setup/image010.png)

The Eclipse Kura Web UI will be available at the following URL: `http://127.0.0.1:8080` with username and password **admin**.