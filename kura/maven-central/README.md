This project converts the existing Kura P2 repository into a Maven 2 repository.
It also creates an upload script which can upload the resulting M2 artifacts
into the OSSRH repository for deploying to Maven Central.

In order to convert this repository the POM files are re-generated to be valid
Maven Central POM files. This includes the addition of proper project meta data
and the generation of source and javadoc attachemnts. The latter are just empty
dummy archived but are required for the upload process. The OSSRH guide suggests
to use empty files of those cannot be provided.

## Building

In order to build the Maven repository you will need to perfom a full build
of Kura in the same directory structure first.

Then run the `build.ant` script, either through some IDE or by executing:

    ant -f build.ant

## Upload

The upload process takes care of signing and uploading the artifacts to the OSSRH
staging repository. For this to work a proper GPG setup is required.

The upload script must be executed on Linux by using the following command:

    ./maven2/upload.sh

The following environment variables can be used to customize the upload process:

<dl>
<dt>REPO</dt><dd>The repository to upload to. Defaults to `https://oss.sonatype.org/service/local/staging/deploy/maven2/`.</dd>
<dt>ID</dt><dd>The ID of the repository. Used to locate the repository credential from the global Maven settings. Defaults to `ossrh`.</dd>
<dt>MVN</dt><dd>The name of the Maven executable. May be an absolute location. Defaults to `mvn`.</dd>
</dl>