# Development Environment Setup

This document describes how to set up the development environment for Kura, which consists of the following components:

* JVM (Java JDK SE 8)

* Eclipse IDE

* Kura Workspace setup

The Kura development environment may be installed on Windows, Linux, or Mac OS. The setup instructions will be the same across each OS though each system may have unique characteristics.
!!! info
    The local emulation of Kura code is only supported in Linux and Mac, not in Windows.

## JVM Installation

Download and install JDK SE 8 from the following links as appropriate for your OS.

For Windows and Linux users, the JDK can be downloaded from the following link: [Java SE 8 Downloads](https://adoptium.net/temurin/releases/?version=8).  Use the latest version of Java SE Development Kit and download the version appropriate for your system.

For additional information regarding the installation of Java 8 on all supported operating systems, see [JDK 8 and JRE 8 Installation Guide](https://adoptium.net/installation).


## Eclipse IDE

The Eclipse IDE is an open source development tool that consists of an integrated development environment (IDE) and a plug-in system for managing extensions.

### Installing Eclipse

Before installing Eclipse, you should choose directory locations for the Eclipse install and its workspaces.

!!! info
    The following points should be kept in mind regarding Eclipse installs and workspaces:

    - The directory location of the Eclipse workspaces should be chosen carefully. 
      Once Eclipse is installed and workspaces are created, they should never be moved to another location in the file system. <br /> 
    - There may be multiple installs of Eclipse (of different or similar versions), and single instances of each install can be run simultaneously; but there should never be more that one instance of a specific install running at the same time (to avoid corruption to the Eclipse environment). <br /> 
    - Each workspace should be used with only one Eclipse install. You should avoid opening the workspace from more than one installation of Eclipse. <br /> 
    - For the purposes of this guide, only a single Eclipse installation will be covered.

Download the current distribution of Eclipse for your OS from [Eclipse official website](https://www.eclipse.org/downloads/packages/). Choose the **Eclipse IDE for Eclipse Committers**.

The zipped Eclipse file will be downloaded to the local file system and can be saved to a temporary location that can be deleted after Eclipse has been installed. After the file has been downloaded, it should be extracted to the Eclipse installs directory.  The following screen capture shows the installation in Linux using an **eclipse\installs\** directory. The Eclipse executable will then be found in the **eclipse\installs\eclipse\** directory.  
This installation will be different depending on the operating system.

![Eclipse Install Folder](./images/development-environment-setup/image1.png)

Because there may potentially be future Eclipse installs extracted into this location, before doing anything else, rename the directory, such as **eclipse\installs\\*juno1*\\**.
!!! warning
    Once you begin using this Eclipse install, it should NOT be moved or renamed.

![Eclipse Install Folder 2](./images/development-environment-setup/image2.png)

### Installing mToolkit

An additional plugin, mToolkit, is needed to allow remote connectivity to an OSGi framework on a Kura-enabled target device.  To install mToolkit into Eclipse, use the following steps:

- Open the **Help \| Install New Software...** menu.
- Add the following URL as an update site based on your version of Eclipse
    - Eclipse **Mars** and older: [http://mtoolkit-mars.s3-website-us-east-1.amazonaws.com](http://mtoolkit-mars.s3-website-us-east-1.amazonaws.com)
    - Eclipse **Neon** and newer: [http://mtoolkit-neon.s3-website-us-east-1.amazonaws.com](http://mtoolkit-neon.s3-website-us-east-1.amazonaws.com)
- Install the "mToolkit" feature (you need to uncheck the **Group items by category** checkbox in order to see the feature)
- Restart Eclipse. In the menu **Window \| Show View \| Other**, there should be an **mToolkit \| Frameworks** option. If so, the plugin has been installed correctly.

## Workspaces

### Creating an Eclipse Workspace

Run Eclipse by clicking its executable in the install directory.

When Eclipse is run for the first time, a workspace needs to be created.  A single workspace will contain all the Java code/projects/bundles, Eclipse configuration parameters, and other relevant files for a specific business-level product.  If the **Use this as the default** option is selected, the designated workspace becomes the default each time you run Eclipse.

If a workspace has not already been defined, or if you are creating a different workspace for another development project, enter a new workspace name.  The workspace should be named appropriate to the project/product being developed.

!!! warning
    Once you begin using a particular workspace, it should NOT be moved or renamed at any time.

Otherwise, select an existing workspace and click **OK**.  After Eclipse is running, you can select the Eclipse menu **File | Switch Workspace | Other** to create or open a different workspace.

![Workspace Launcher](./images/development-environment-setup/image3.png)

After the new workspace opens, click the Workbench icon to display the development environment.

![Workspace](./images/development-environment-setup/image4.png)

### Importing the Kura User Workspace

To set up your Kura project workspace, you will need to download the Kura User Workspace archive from [Eclipse Kura Download Page](https://websites.eclipseprojects.io/kura/downloads.php).

From the Eclipse File menu, select the **Import** option.  In the Import dialog box, expand the **General** heading, select **Existing Projects into Workspace**, and then click **Next**.

Now click the **Select archive file** option button and browse to the archive file, such as *user_workspace_archive_<version>.zip*.

![Import Project](./images/development-environment-setup/image5.png)

Finally, click **Finish** to import the projects.  At this point, you should have four projects in your workspace.  The four projects are as follows:

* org.eclipse.kura.api – the core Kura API.

* org.eclipse.kura.demo.heater – an example project that you can use as a starting point for creating your own bundle.

* org.eclipse.kura.emulator – the emulator project for running Kura within Eclipse (Linux/Mac only).

* target-definition – a set of required bundles that are dependencies of the APIs and Kura.

    ![Import Project](./images/development-environment-setup/image6.png)

Eclipse will also report some errors at this point.  See the next section to resolve those errors.

### Workspace Setup
This section will guide the users to configure the development workspace environment. 

#### JRE Configuration
The latest Eclipse IDEs require and configure, by default, a Java 11 environment.
In order to be able to leverage and develop using the new workspace for Kura, the user will be required to perform a one-time operation to specify to the IDE a Java 8 JDK.
Opening the Eclipse preferences and selecting the Installed JREs in the Java section, the user has to select an installed Java 8 instance. 

![Set JDK 8](./images/development-environment-setup/image7.png)

After applying the configuration change, the user will be prompted to align also the compiler options. To do so, selecting the Compiler entry in the Java section, the user has to select 1.8 from the list of available Java versions.

![Set Compiler 8](./images/development-environment-setup/image8.png)

After applying the changes, the user will be prompted to recompile the environment.

#### Target Definition Setup
Click the arrow next to the **target-definition** project in the workspace and double-click **kura-equinox_<version>.target** to open it.

![Target Definition Setup](./images/development-environment-setup/image9.png)

In the Target Definition window, click the link **Set as Target Platform**.  Doing so will reset the target platform, rebuild the Kura projects, and clear the errors that were reported.  At this point, you are ready to begin developing Kura-based applications for your target platform.

### Eclipse Oomph installer

The Eclipse Oomph installer is an easy way to install and configure the Eclipse IDE to start developing on Kura.
Download the latest Eclipse Installer appropriate for your platform from [Eclipse Downloads](https://www.eclipse.org/downloads/eclipse-packages/)

* Start the Eclipse Installer
* Switch to advanced mode (in simple mode you cannot add a custom installer)
* Select "Eclipse IDE for Eclipse Committers", select the latest "Product Version" and select a Java 11+ VM. Then click the Next button.

    ![Eclipse Installer](./images/development-environment-setup/image10.png)

* Select "Eclipse Kura" project under the "Eclipse Projects" menu. If it isn't available, add a new installer that you can find [here]( https://raw.githubusercontent.com/eclipse/kura/develop/kura/setups/kura.setup) under the "Github Projects" menu. Then click the Next button.

    ![Kura Oomph](./images/development-environment-setup/image11.png)

* Update Eclipse Kura Git repository's username (prefer the anonymous HTTPS option, link to your fork) and customize further settings if you like (e.g. Root install folder, Installation folder name). Then click the Next button.
* Leave all Bootstrap Tasks selected and press the Finish button.
* Accept the licenses and unsigned content.
* Wait for the installation to finish, a few additional plugins will be installed.
* At first startup Eclipse IDE will checkout the code and perform a full build.

The result will be an Eclipse IDE with all the recommended plug-ins already available, code will be checked out and built, workspace will be set up, a few Working Sets will be prepared with most projects building without errors.

The next step is to get the rest of the projects to build, for which you might need to build them in the console with specific profiles available e.g. the CAN bundle.

#### Run the Eclipse Kura Emulator

To start the Eclipse Kura emulator, select the "Eclipse Kura Emulator.launch" profile from "Other Projects" -> "setups" -> "launchers" and open it with "Run as" -> "Run Configurations...". Then click on the "Arguments" tab and update the "VM arguments" as follows to adapt the paths to the folder structure created by the Oomph installer:

``` shell
-Dkura.have.net.admin=false -Dorg.osgi.framework.storage=/tmp/osgi/framework_storage -Dosgi.clean=true -Dosgi.noShutdown=true -Declipse.ignoreApp=true -Dorg.eclipse.kura.mode=emulator -Dkura.configuration=file:${workspace_loc}/../git/kura/kura/emulator/org.eclipse.kura.emulator/src/main/resources/kura.properties -Ddpa.configuration=/tmp/kura/dpa.properties -Dlog4j.configurationFile=file:${workspace_loc}/../git/kura/kura/emulator/org.eclipse.kura.emulator/src/main/resources/log4j.xml -Dkura.data=${workspace_loc}/kura/data -Dkura.snapshots=${workspace_loc}/kura/user/snapshots -Dorg.eclipse.equinox.http.jetty.customizer.class=org.eclipse.kura.jetty.customizer.KuraJettyCustomizer
```

![Vm arguments](./images/development-environment-setup/image12.png)

The Eclipse Kura Web UI will be available at the following URL: http://127.0.0.1:8080 with username and password **admin**.