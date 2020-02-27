# Using with OpenShift

It is possible to run the Kura emulator instance in an OpenShift instance.

Assuming you already have set up OpenShift it is possible to deploy the Kura Emulator
inside of OpenShift using either method described in the following sections.

The template will create a new build and deployment configuration. This will trigger
OpenShift to download the base docker images, pull the source code of Kura from Git and
completely rebuild it. There is also a Kura instance configured to instantiate that image once
as soon as the build is complete. Every re-build will automatically trigger a re-deployment.

## Installing

All following steps will assume that you already have create a new project in OpenShift named `kura`
for this. You will also need a working internet connection on the machine running OpenShift
and on the machine you will be using.

**Note:** If you are using the templates form a different branch than the master branch,
          then you need to specify the branch name as the template parameter `GIT_BRANCH`.

### Web console

* Click on "Add to project"
* Switch to "Import YAML / JSON"
* Either
  * Copy and paste the content of either of the template files into the text area
  * Use the "Browse…" button and load one of the template files
* Press "Create"
* In the following confirmation dialog:
  * Keep the defaults
  * Press "Continue"

### Command line

Issue the following commands from your command line of choice. Please not that need to have the
[OpenShift CLI installed](https://docs.openshift.org/latest/cli_reference/get_started_cli.html) installed.
You also need to be logged in to your OpenShift cluster with `oc login`.

    oc new-app --file=kura-<mode>.yml

If you are using a template from a branch other than `master`, you must add the following
template parameter, e.g.:

    oc new-app --file=kura-<mode>.yml -p GIT_BRANCH=develop

## Persistence modes

There are three templates for deploying Kura. Emphermal, data persistence, instance persistence.

### Ephermal

With ephermal you will loose all changes once the pod running Kura is destroyed. Each new pod will start
with a fresh installation of Kura. This may be useful for quickly testing Kura.

### Data persistence

Data persistence will keep all configuration changes made inside of Kura, it will also keep additional
installed packages (DPs). But the Kura installation itself is considered "read-only" and cannot be
modified, e.g. by using a Kura updater. Data and installed packaged will be stored a persistent volume.

On the other hand allows to drop in a new container image, which will then bring a new Kura version, but
keep the configuration settings and DPs as they were.

### Instance persistence

Using the instance persistent template, a new volume will be allocated and the Kura instance will be copied
over to this volume on the first start. Subsequent starts will keep the Kura instance and all its data directories.

The main difference between data and instance persistence is, that a new build with the persistent template will
restart the Kura pod, including a fresh container image (updated Java, OS, …) but it will keep the
same Kura version. In order to update Kura, you have to use the Kura update mechanisms. On the other hand,
updating Kura using its update mechanism will modify the Kura installation as expected.

## Stateful sets

The Kura instance is run as a "stateful set". This allows to keep a stable network name, and auto-allocate volumes as needed.

**Note:** Although image triggers have been added to the stateful set, sometimes those triggers don't properly fire.
          This results in a new builds not being activated.

## Configuration through ConfigMaps

The Kura instance will use the config map `kura-instance-config` to store the following Kura specific files:

<dl>

<dt><code>log4j.xml</code><dt>
<dd>
The log4j 2.x config file. By default only "warn" is enabled. But the configuration will be automatically
re-loaded once the config map has been changed.
</dd>

<dt><code>kura_custom.properties</code></dt>
<dd>
</dd>

</dl>

## Loading bundles with ConfigMaps

The Kura docker container allows to map the directory `/load` and drop in OSGi bundles, which
will automatically be picked up by Apache FileInstall.

The OpenShift deployment will leverage that and map the configmap `kura-instance-load-config`
to `/load` (which is also monitored by the Kura instance). This allows one to update the configmap with
a new JAR and the Kura instance will pick up this OSGi bundle and correctly register it.

**Note:** For this to work ConfigMaps with binary support are required. This requires OpenShift 3.10, Kubernetes 1.10.
