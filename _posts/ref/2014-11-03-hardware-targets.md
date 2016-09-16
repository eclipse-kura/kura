---
layout: page
title:  "Hardware Targets"
categories: [ref]
---

[Overview](#overview)

[Code Modifications](#code-modifications)

[Code Additions](#code-additions)

[Native Libraries](#native-libraries)

[Artifacts](#artifacts)


## Overview
This reference provides instructions on adding a new hardware target to Kura. Before beginning,
make sure you have <a href="http://wiki.eclipse.org/Kura/Getting_Started" target="_blank">setup a full Kura development environment</a>.
The only two hard
requirements at this time for running Kura are:

* Linux Operating System
* Java SE 7

Memory requirements will vary greatly depending on the types of applications running within the
framework and the amount/frequency of messages being sent and received. Also note that some
functionality relies on native libraries. If the libraries supplied in Kura do not match your
target architecture, you will need to compile your own native libraries.

## Code Modifications
The code changes required to add a new hardware target to Kura are minimal. For the remainder
of this document, "hello-hw" will refer to the new example target hardare to be setup. First, modify
kura/distrib/pom.xml and add the below Maven profile to the profiles section:

```xml
<profile>
    <id>hello-hw</id>
    <activation>
        <activeByDefault>true</activeByDefault>
    </activation>
    <build>
      <resources>
          <resource>
              <directory>src/main/resources</directory>
              <filtering>true</filtering>
          </resource>
      </resources>
      <plugins>
          <plugin>
              <groupId>org.codehaus.mojo</groupId>
              <artifactId>properties-maven-plugin</artifactId>
              <version>1.0-alpha-1</version>
              <executions>
                  <execution>
                      <phase>initialize</phase>
                      <goals>
                          <goal>read-project-properties</goal>
                      </goals>
                      <configuration>
                          <files>
                              <file>${basedir}/build.properties</file>
                              <file>${basedir}/config/kura.build.properties</file>
                          </files>
                      </configuration>
                  </execution>
              </executions>
          </plugin>
          <plugin>
              <groupId>org.codehaus.mojo</groupId>
              <artifactId>buildnumber-maven-plugin</artifactId>
              <version>1.0</version>
              <executions>
                  <execution>
                      <phase>validate</phase>
                      <goals>
                          <goal>create-timestamp</goal>
                      </goals>
                  </execution>
              </executions>
          </plugin>
          <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-antrun-plugin</artifactId>
              <version>1.7</version>
              <executions>
                <execution>
                    <id>hello-hw-jars</id>
                    <phase>install</phase>
                    <goals>
                        <goal>run</goal>
                    </goals>
                    <configuration>
                      <target>
                          <property name="buildNumber" value="buildNumber" />
                          <property name="project.version" value="${project.version}" />
                          <property name="project.build.profile" value="${project.build.profile}" />
                          <property name="project.build.directory" value="${project.build.directory}" />
                          <property name="build.name" value="hello-hw" />
                          <property name="target.device" value="hello-hw" />
                          <property name="kura.os.version" value="debian" />
                          <property name="kura.arch" value="armv6_hf" />
                          <property name="kura.mem.size" value="256m" />
                          <property name="kura.install.dir" value="${kura.install.dir}" />
                          <ant antfile="${basedir}/src/main/ant/build_equinox_distrib.xml"
                              target="dist-linux" />
                      </target>
                    </configuration>
                </execution>
          </plugin>
      </plugins>
    </build>
</profile>
```
The profile will assist in creating a shell script installer for the targeted hardware. If the hardware is running a Debian based OS, the "raspberry-pi" and "beaglebone"
profiles can be used as references for creating a Debian package. There is also information on the use of jdeb in Kura [here](http://wiki.eclipse.org/Kura/Use_of_jdeb_in_Kura).

The majority of the profile can be used as is, the attributes that must be edited are:

* \<id> - This is the profile ID and must be unique
* \<property name="buid.name"> - This is used to name the output artifacts
* \<property naeme="target.device"> - Kura uses this to occasionally run specific code for a specific target
* \<property name="kura.os.version"> - Similar to target.device, this is sometimes used for executing specific code
* \<property name="kura.arch"> - This is used to identify the correct native libraries to load
* \<property name="kura.mem.size"> - This sets the Xms, Xmx, and MaxPermSize Java VM memory properties

## Code Additions
Each hardware target has a directory in kura/distrib/src/main/resources (ex: kura/distrib/src/main/resources/raspberry-pi). For simplicity, use the below code to
copy an existing target platform:

```
mkdir kura/src/main/resources/hello-hw
cp -R kura/src/main/resources/raspberry-pi/* kura/src/main/resources/hello-hw/.
```

An optional step would be to create a copy of the *-nn directory for the new target. This would create a version of the build without networking support. This is useful
if you do not want Kura to assist in managing network settings.

Each file in the new directory should be reviewed and edited to match the new target. Below is a brief overview of each file:

* firewall.init - This file is used to setup the linux firewall during installation.  The default file opens ports for HTTP (management UI), DNS, DHCP, and 1450 for remote bundle deployment (mToolKit).
* kura.properties - This file contains various system properties. The values should be examined and updated to match the new target.
* kura_install.sh - This is the base installer script. For most situations it should not be edited.
* kura_upgrade.sh - This is the base upgrade script. For most situations it should not be edited.
* kuranet.conf - This file contains basic information about the available network interfaces. Interfaces should be added or removed as needed using the original file as a template.
* log4j.properties - This file contains settings for the logger utility. The provided settings should work fine in most cases.
* recover_dflt_kura_config.sh - Helper utility to assist in recovering configurations. This file should not be edited.
* snapshot_0.xml - This is the main configuration file for Kura. A good practice is to use the copied configuration file to start Kura for the first time. After Kura has been configured to the desired
starting conditions, the updated snapshot file can be obtained through the management UI (Settings -> Snapshots) or from the /opt/eclipse/kura/data/snapshots directory. The latest snapshot will have the largest numeric value. This updated file can then be renamed to snapshot_0.xml and copied over to the hardware directory.

## Native Libraries
<b>Coming Soon</b>

## Artifacts
After a successful Maven build, the new target hardware should have artifacts in kura/distrib/target. Most important of which is an installer shell script that should be used to install Kura on the new target hardware.
