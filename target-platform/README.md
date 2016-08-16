# Target platform

This project assembles the target platform for Eclipse Kura.

The target platform consists of the OSGi container (Eclipse Equinox) and the additional
dependencies required in order to run Kura.

The project builds as follows:

 * Build local projects
 * Assemble P2 repositories
   * Downloading external dependencies
   * Adding previously built, local projects
   * Generating P2 meta data
   * Build target platform manifest: `bundleVersions.properties`
 * Copy output (P2 and manifest) to Kura `target-definition` directory
    
The build process in the `distrib` project will pick up the platform manifest file and
take versions from there. The download will take the versions from the file `config/download.version.properties`. This duplication is required since there are download versions and
OSGi versions, which don't always match (e.g. a Maven version may be "2.0" where the OSGi version
would be "2.0.0"). Also does the download file contain maven artifact IDs, where the manifest file
contains the OSGi "Bundle Symbolic Name".

In a future step the download file should be merged into the actual download script and the bundle manifest
file should become obsolete since the build should take only artifacts from the build. Currently there already
is an issue with having two GWT version that the build system can't properly handle.

## Assumptions by the process

The process makes the following assumptions:

 * All downloads/dependencies are OSGi bundles
 * The bundles are stored in the file system as <BSN>_<OSGI-VERSION>.jar, the download process will rename
   them accordingly
 * The key in the manifest file is: <BSN>.version=<OSGI-VERSION>

## Adding new dependencies

The following section describes how to technically add a new dependency to Kura, still the Eclipse IP
process has to be followed to get additional or updated dependencies approved.

Adding a new dependency requires the following steps:

 * Get approval from the Eclipse IP process
 * Add the file to the downloads on either of the `build-p2.ant` files
 * Add the file to the startup list in the `config.ini`, see below
 
### Registering for startup

The `config.ini` file for Equinox is currently being created manually in the ant script at `kura/distrib/src/main/ant/build_equinox_distrib.xml`. In order to install the new dependency it has to be added to the
`osgi.bundles` system property.

For example (assuming the BSN is `org.eclipse.equinox.common`):
```
<entry key="osgi.bundles" operation="+"
  value=", reference:file:${kura.install.dir}/kura/plugins/org.eclipse.equinox.common_${org.eclipse.equinox.common.version}.jar@1:start" />

```
