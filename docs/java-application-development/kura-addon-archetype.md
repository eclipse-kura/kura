# Kura Addon Archetype

The Kura Addon Archetype is a [Maven Archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html) that allows to create a development environment with the following features:

- Maven-based build
- Template project for creating DEB/RPM packages
- Tycho-surefire based integration test template
- Uses a remote P2 repository for the target platform

The Kura Archetype JAR (`kura-addon-archetype-<kura-version>.jar`) is available in the released artifacts and can be installed in the local maven repository with the following command:

```shell
mvn install:install-file \
-Dfile=./kura-addon-archetype-<kura-version>.jar \
-DgroupId=org.eclipse.kura \
-DartifactId=kura-addon-archetype \
-Dversion="<kura-version>" \ 
-Dpackaging=jar \
-DgeneratePom=true
```

Valid `<kura-version>` values are `6.0.0-SNAPSHOT` and `6.0.0`. This archetype is available since Kura 6.0.0.

After that, it is possible to generate a project skeleton using the archetype with the following command:

```shell
mvn archetype:generate \
-DarchetypeArtifactId=kura-addon-archetype \
-DarchetypeGroupId=org.eclipse.kura \
-DarchetypeVersion="<kura-version>"
```

The command will start the generation of the archetype in interactive mode. Maven will ask for a few parameters on the command line:

- **groupId** : the Maven group id of the generated pom files, usually `org.eclipse.kura`
- **artifactId**: the Maven artifact id of the generated parent pom file and the name of the generated top level project folder, usually something like `org.eclipse.kura.myartifact`
- **package**: the Java package to be used for the main bundle, usually something like `org.eclipse.kura.myartifact`

Other parameters like **version** and **mainBundleVendor** can be changed by answering `n` after the `Confirm properties configuration` prompt, which appears after editing the properties above.

A `.gitignore` file is automatically added with a default configuration. The `OSGI-INF` folder is omitted because it will be generated during tests at compile-time (this is necessary to make the PDE launcher work).

## Project structure

At the end of the procedure, the archetype will generate a subfolder in the working directory containing the following subfolders:

- **bundles**: the directory where developed bundles can be placed. After the first archetype execution, this directory contains a single project, named as `artifactId.bundle`

- **distrib**: contains a project that builds an RPM and a DEB package that installs the JAR produced in `bundles` in Kura's plugins folder `/opt/eclipse/kura/plugins`. It is recommended to configure this project to customize the target package architecture and other parameters. The project code is commented with hints on the configurable options

- **target-definition**: The `.target` file contained in the project is the way to specify the project dependencies as maven artifacts, Tycho will then wrap them as bundles and make them available in the target platform. Since only released artifacts are published on Maven central, it is recommeded to perform a local Kura build to have the SNAPSHOT versions available

- **tests**: contains OSGi integration tests executed by the `tycho-surefire-plugin`

## Compile and run

The minimum supported Java version for compiling is Java 17. Compile the project with:

```shell
mvn clean install
```

The build will produce the following system packages in `distrib/target`:

- DEB installer (`<artifactId>_<version>_<debian-architecture>.deb`)
- RPM installer (`<artifactId>-<version>.<rpm.architecture>.rpm`)

Installer properties like the architecture, organization name, package dependencies, and others can be configured in the `distrib` project.

Depending on the system, the packages can be installed with:

```shell
# debian
apt install <artifactId>_<version>_<debian-architecture>.deb

# fedora/redhat/centos/others
dnf install <artifactId>-<version>.<rpm.architecture>.rpm
```

On some systems that use RPM package manager only signed packages are allowed (see [`rpm-maven-plugin`](https://www.mojohaus.org/rpm-maven-plugin/adv-params.html#Signatures) documentation on how to sign a package). **During development**, it is possible to bypass this control using:

```shell
rpm --define '_pkgverify_level digest' -i <artifactId>-<version>.<rpm.architecture>.rpm
```

After having installed the package, restart kura with:

```shell
systemctl restart kura
```

During startup Kura will scan the plugins folder to pick up the installed JARs and include them in the framework's runtime.

It is possible to remove the installed plugins with:

```shell
# debian
apt purge <artifactId> # or apt remove <artifactId>

# fedora/redhat/centos/others
dnf remove <artifactId>
```

## IDE setup

### Importing Projects in Eclipse IDE

In Eclipse IDE , create a new workspace (it is not necessary to have the workspace in the root of the project) and import the projects with _File | Import | Maven | Existing Maven Projects_.

![](./images/kura-addon-archetype/eclipse-import-maven-projects-1.png)

![](./images/kura-addon-archetype/eclipse-import-maven-projects-2.png)

Note that if the workspace resides in the root of the project the parent POM file cannot be selected.

#### Load target platform

Open the _.target_ file in the _target-definition_ project and click on _Set as Active Target Platform_. Note that this will download the bundles from the Kura P2 repository and it may take a while to complete.

![](./images/kura-addon-archetype/eclipse-ide-reload-platform.png)

Eclipse IDE should rebuild the workspace automatically and show no errors. If errors appear, see next section.

#### (Optional) IDE errors resolution

In some cases it might be necessary to manually configure the build path. For the bundles and test projects, select _Properties_ and then _Project Natures_ and add the natures as in picture below.

![](./images/kura-addon-archetype/eclipse-project-natures.png)

Then, from the _Java Build Path_ configure the correct source folder as in picture below.

![](./images/kura-addon-archetype/eclipse-java-path.png)

Finally, configure the external **Classpath** dependencies by selecting the plugin dependencies from the _Add Library_.

![](./images/kura-addon-archetype/eclipse-add-library.png)

On old Eclipse IDE installations it might be necessary to uninstall the Tycho configurator 0.1.0 plugin from _Help | Install new software... | What is already installed?_.

## Architecture-specific development

The Addon Archetype standard procedure allows to build generic debian and RPM installers not dependant on the architecture on which the bundle will be installed.

However, it is possible to adjust the files in the `distrib` folder to develop architecture-dependant installers: it could be necessary to build a bundle that contains native code (C/C++ libraries, jars that use JNI, etc.) or to build a bundle that contains architecture-specific files (e.g. a systemd service file). In this case, the `distrib` folder should be modified to include the architecture-specific files in the generated package.

In the following sections we will see how to modify the `distrib` folder to build a package that contains architecture-specific files, for both the RPM and the DEB package. These steps assume that in the source code the architecture-specific jars are build in the form of fragments of the agnostic java code. The architecture-specific jars will then be copied in the `distrib` folder and included in the package.

### Getting the source code

The first modification is the copy and rename of the source code target files generated under the `bundles` project. Supposed that the bundles structure is the following:

```
org.eclipse.kura.myartifact
├── bundles
│   ├── org.eclipse.kura.myartifact.bundle
│   ├── org.eclipse.kura.myartifact.bundle.aarch64
│   ├── org.eclipse.kura.myartifact.bundle.x86_64
....
```
The `org.eclipse.kura.myartifact.bundle` folder contains the agnostic code, while the `org.eclipse.kura.myartifact.bundle.aarch64` and `org.eclipse.kura.myartifact.bundle.x86_64` fragments contain the architecture-specific code. The `distrib` folder will be modified to include the architecture-specific jars in the generated package.

First of all it is possible to define some properties in the `distrib/pom.xml` file:

```xml
<addon.installation.dir>/opt/eclipse/kura/plugins/6s</addon.installation.dir>
<native.core.installation.dir>/opt/eclipse/kura/plugins/5</native.core.installation.dir>
...
<jar.name>org.eclipse.kura.myartifact.bundle</jar.name>
<jar.aarch64.core>org.eclipse.kura.myartifact.bundle.aarch64</jar.aarch64.core>
<jar.x86_64.core>org.eclipse.kura.myartifact.bundle.x86_64</jar.x86_64.core>
...

instead of
<deb.architecture>all</deb.architecture>
<rpm.architecture>noarch</rpm.architecture>
use the following
<deb.amd64.architecture>amd64</deb.amd64.architecture>
<deb.arm64.architecture>arm64</deb.arm64.architecture>
<rpm.x86_64.architecture>x86_64</rpm.x86_64.architecture>
<rpm.aarch64.architecture>aarch64</rpm.aarch64.architecture>
```
!!! tip
    These properties are usefull to define the architecture-specific jars and the architecture-specific debian and rpm architectures. The `addon.installation.dir` property is used to define the installation directory of the bundle in Kura. The `native.core.installation.dir` property is used to define the installation directory of the native code in Kura. It is important that the fragments are installed in a higher level directory than the main bundle, otherwise Kura will not be able to load the native code.

    The `jar.name` property is used to define the name of the main bundle, while the `jar.aarch64.core` and `jar.x86_64.core` properties are used to define the names of the architecture-specific fragments.

    Finally, the `deb.amd64.architecture`, `deb.arm64.architecture`, `rpm.x86_64.architecture` and `rpm.aarch64.architecture` properties are used to define the architecture-specific debian and rpm architectures.

The plugin responsible to copy and rename the jars is the `copy-rename-maven-plugin`. The plugin is configured in the `distrib/pom.xml` file as follows:

```xml
<execution>
    <id>copy-and-rename-jar</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>copy</goal>
    </goals>
    <configuration>
        <sourceFile>${project.basedir}/../bundles/${jar.name}/target/${jar.name}-${project.version}.jar</sourceFile>
        <destinationFile>target/${jar.name}_${project.version}.jar</destinationFile>
    </configuration>
</execution>
```

To load also the architecture-specific jars, the plugin should be configured as follows:

```xml
<execution>
    <id>copy-and-rename-source-jar</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>copy</goal>
    </goals>
    <configuration>
        <sourceFile>${project.basedir}/../bundles/${jar.name}/target/${jar.name}-${project.version}.jar</sourceFile>
        <destinationFile>target/${jar.name}_${project.version}.jar</destinationFile>
    </configuration>
</execution>
<execution>
    <id>copy-and-rename-aarch64-architecture-jar</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>copy</goal>
    </goals>
    <configuration>
        <sourceFile>${project.basedir}/../bundles/${jar.aarch64.core}/target/${jar.aarch64.core}-${project.version}.jar</sourceFile>
        <destinationFile>target/input_files/${jar.aarch64.core}_${project.version}.jar</destinationFile>
    </configuration>
</execution>
<execution>
    <id>copy-and-rename-x86_64-architecture-jar</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>copy</goal>
    </goals>
    <configuration>
        <sourceFile>${project.basedir}/../bundles/${jar.x86_64.core}/target/${jar.x86_64.core}-${project.version}.jar</sourceFile>
        <destinationFile>target/input_files/${jar.x86_64.core}_${project.version}.jar</destinationFile>
    </configuration>
</execution>
```

In this way, in the `input_files` folder all the necessary jars will be copied and renamed to the correct name. The `input_files` folder is used to store the files that will be included in the package.

### Debian architecture dependant packages

The `/distrib/deb/control/control` file contains the DEB package metadata. The standard file is configure like this:

```
Package: [[package.name]]
Version: [[project.version]]
Section: admin
Priority: optional
Depends: kura
Architecture: [[deb.architecture]]
Maintainer: Eclipse Kura Developers <kura-dev@eclipse.org>
Description: [[summary]]
  [[long.description]]
Homepage: https://eclipse-kura.github.io/kura/ (add correct link to documentation here)
```

The Architecture field is the one responsible to specify the architecture of the package. The value is set in the `/distrib/pom.xml` file, by the property `<deb.architecture>all</deb.architecture>`. The value `all` means that the package can be installed on any architecture. This is the recommended value for packages that do not contain architecture-specific files.

To build a package that contains architecture-specific files, it is necessary to separate the control files of the different architectures (namely, `aarch64` and `amd64`). To do so, create two directoris in the `distrib/deb/` folder with this structure:

```
distrib
├── deb
│   ├── amd64
│   ├── arm64
```
Each `control` file must be created in the `distrib/deb/<arch>/` folder. These folders could contain also the `postinst` and `postrm` files, used to execute commands after the installation and before the removal of the package. The content of these files is not relevant for this example, but they can be used to execute commands that are necessary for the correct installation of the package.

An example of `control` file for the `arm64` architecture is:

```
Package: [[package.name]]
Version: [[project.version]]
Section: admin
Priority: optional
Depends: kura
Architecture: [[deb.arm64.architecture]]
Maintainer: Eclipse Kura Developers <kura-dev@eclipse.org>
Description: [[summary]]
  [[long.description]]
Homepage: https://eclipse-kura.github.io/kura/ (add correct link to documentation here)
```

The same file for the `amd64` architecture will just change the `Architecture` field to `[[deb.amd64.architecture]]`.

Finally, the plugin responsible of generating the DEB package is the `jdeb` plugin. The default execution is configured in the `distrib/pom.xml` file as follows:

```xml 
<plugin>
    <groupId>org.vafer</groupId>
    <artifactId>jdeb</artifactId>
    <version>1.12</version>
    <executions>
        <execution>
            <id>generate-deb</id>
            <phase>package</phase>
            <goals>
                <goal>jdeb</goal>
            </goals>
            <configuration>
                <verbose>true</verbose>
                <deb>${basedir}/target/${package.name}_${project.version}_${deb.architecture}.deb</deb>
                <controlDir>${project.basedir}/deb/control</controlDir>
                <skipPOMs>false</skipPOMs>
                <dataSet>
                    <data>
                        <src>${basedir}/target/${jar.name}_${project.version}.jar</src>
                        <dst>${jar.name}_${project.version}.jar</dst>
                        <type>file</type>
                        <mapper>
                            <type>perm</type>
                            <prefix>${addon.installation.dir}</prefix>
                            <user>kurad</user>
                            <group>kurad</group>
                            <filemode>600</filemode>
                        </mapper>
                    </data>
                </dataSet>
            </configuration>
        </execution>
    </executions>
</plugin>
```

To build the architecture-dependant installers, the execution must change to:

```xml
<execution>
    <id>generate-arm64-deb</id>
    <phase>package</phase>
    <goals>
        <goal>jdeb</goal>
    </goals>
    <configuration>
        <verbose>true</verbose>
        <deb>${basedir}/target/deb/${output.installer.name}_${project.version}_${deb.arm64.architecture}.deb</deb>
        <controlDir>${project.basedir}/deb/arm64</controlDir>
        <skipPOMs>false</skipPOMs>
        <dataSet>
            <data>
                <src>${basedir}/target/input_files/${jar.name}_${project.version}.jar</src>
                <dst>${jar.name}_${project.version}.jar</dst>
                <type>file</type>
                <mapper>
                    <type>perm</type>
                    <prefix>${addon.installation.dir}</prefix>
                    <user>kurad</user>
                    <group>kurad</group>
                    <filemode>600</filemode>
                </mapper>
            </data>
            <data>
                <src>${basedir}/target/input_jars/${jar.aarch64.core}_${project.version}.jar</src>
                <dst>${jar.aarch64.core}_${project.version}.jar</dst>
                <type>file</type>
                <mapper>
                    <type>perm</type>
                    <prefix>${native.core.installation.dir}</prefix>
                    <user>kurad</user>
                    <group>kurad</group>
                    <filemode>600</filemode>
                </mapper>
            </data>
        </dataSet>
    </configuration>
</execution>
```

A similar execution can be used for the `amd64` architecture, just changing the `deb` and `controlDir` fields to point to the correct architecture (and change the execution `id` if they're present at the same time). Also in the <dataSet> section, the `src` and `dst` fields must be changed to point to the correct architecture-specific jars.

The final result will consist of two DEB packages, one for each architecture. They will be found in the `distrib/target/deb/` folder with the following names:
```
<artifactId>_<version>_<deb.amd64.architecture>.deb
<artifactId>_<version>_<deb.arm64.architecture>.deb
```

### RPM architecture dependant packages

The RPM packages are generated in a similar way. The configuration of the packages is set in the plugin responsible of the packages generation, the `rpm-maven-plugin`. The plugin is, by default, configured in the `distrib/pom.xml` file as follows:

```xml
<execution>
    <id>generate-rpm</id>
    <phase>package</phase>
    <goals>
        <goal>rpm</goal>
    </goals>
    <configuration>
        <name>${package.name}</name>
        <needarch>${rpm.architecture}</needarch>
        <group>Applications/System</group>
        <targetOS>linux</targetOS>
        <summary>${summary}</summary>
        <description>${long.description}</description>
        <license>EPL 2.0 (https://www.eclipse.org/legal/epl-2.0/)</license>

        <mappings>
            <mapping>
                <directory>${addon.installation.dir}</directory>
                <filemode>600</filemode>
                <username>kurad</username>
                <groupname>kurad</groupname>
                <sources>
                    <source>
                        <location>${basedir}/target/${jar.name}_${project.version}.jar</location>
                    </source>
                </sources>
            </mapping>
        </mappings>

        <requires>
            <require>kura</require>
        </requires>
    </configuration>
</execution>
```

The `needarch` field is the one responsible to specify the architecture of the package. The value is set in the `/distrib/pom.xml` file, by the property `<rpm.architecture>noarch</rpm.architecture>`. The value `noarch` means that the package can be installed on any architecture. This is the recommended value for packages that do not contain architecture-specific files.

To build a package that contains architecture-specific files, it is necessary to separate the executions of the `rpm-maven-plugin` for the different architectures (namely, `aarch64` and `x86_64`). An example for the `x86_64` architecture is:

```xml
<execution>
    <id>generate-x86_64-rpm</id>
    <phase>package</phase>
    <goals>
        <goal>rpm</goal>
    </goals>
    <configuration>
        <workarea>${basedir}/target/rpm/${rpm.x86_64.architecture}</workarea>
        <name>${output.installer.name}</name>
        <needarch>${rpm.x86_64.architecture}</needarch>
        <group>Applications/System</group>
        <targetOS>linux</targetOS>
        <summary>${summary}</summary>
        <description>${long.description}</description>
        <license>EPL 2.0 (https://www.eclipse.org/legal/epl-2.0/)</license>

        <mappings>
            <mapping>
                <directory>${addon.installation.dir}</directory>
                <filemode>600</filemode>
                <username>kurad</username>
                <groupname>kurad</groupname>
                <sources>
                    <source>
                        <location>${basedir}/target/input_files/${jar.name}_${project.version}.jar</location>
                    </source>
                </sources>
            </mapping>
            <mapping>
                <directory>${native.core.installation.dir}</directory>
                <filemode>600</filemode>
                <username>kurad</username>
                <groupname>kurad</groupname>
                <sources>
                    <source>
                        <location>${basedir}/target/input_files/${jar.x86_64.core}_${project.version}.jar</location>
                    </source>
                </sources>
            </mapping>
        </mappings>

        <requires>
            <require>kura</require>
        </requires>
    </configuration>
</execution>
```

A similar execution can be used for the `aarch64` architecture, just changing the `workarea`, `needarch`, `sources` and `location` fields to point to the correct architecture (and change the execution `id` if they're present at the same time). Also in the `<mappings>` section, the `directory` and `location` fields must be changed to point to the correct architecture-specific jars.

With these executions two RPM installers will be generated, one for each architecture. They will be found in the `distrib/target/rpm/` folder with the following names:
```
<artifactId>-<version>.<rpm.x86_64.architecture>.rpm
<artifactId>-<version>.<rpm.aarch64.architecture>.rpm
```