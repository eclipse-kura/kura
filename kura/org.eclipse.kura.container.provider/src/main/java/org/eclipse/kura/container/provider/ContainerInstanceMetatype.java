/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.container.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.container.provider.ContainerInstance", name = "Container Instance", description = "Allows for the creation of containers.")
public @interface ContainerInstanceMetatype {

    @AttributeDefinition(name = "Enabled", cardinality = 1, description = "Enables this container")
    boolean container_enabled() default false;

    @AttributeDefinition(name = "Image name", cardinality = 1, description = "Specifies the image reference that will be used to create this container instance.              The value has to be expressed in the form registryURL/imagename. If no registryURL is provided, the official Docker Hub registry will be used as default.              When pulling the testing/test-image image from a local registry listening on port 5000 (e.g. myregistry.local:5000), the registryURL/imagename field has             to be specified as follows: myregistry.local:5000/testing/test-image             Default: nginx (the image will be pulled from the Docker Hub registry)")
    String container_image() default "nginx";

    @AttributeDefinition(name = "Image tag", cardinality = 1, description = "Describes which image version should be pulled from the container registry. Default: latest")
    String container_image_tag() default "latest";

    @AttributeDefinition(name = "Authentication Registry URL", required = false, description = "Url for docker registry. Required only for authenticated registries")
    String registry_hostname() default "";

    @AttributeDefinition(name = "Authentication Username", required = false, description = "Username for container registry. Required only for authenticated registries")
    String registry_username() default "";

    @AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false, description = "Password for container registry. Required only for authenticated registries")
    String registry_password() default "";

    @AttributeDefinition(name = "Trust anchor", cardinality = 1, required = false, description = "Trust anchor used to verify the container image signature.|TextArea")
    String container_signature_trust_anchor() default "";

    @AttributeDefinition(name = "Verify in transparency log", cardinality = 1, required = false, description = "Sets the transparency log verification, to be used when a container image signature has been uploaded to the transparency log.")
    boolean container_signature_verify_transparency_log() default true;

    @AttributeDefinition(name = "Container Image Enforcement Digest", cardinality = 1, required = false, description = "Digest of the container image allowed to run on the device, if the Container Enforcement Monitor is enabled. If not provided, it will be computed by the Container Signature Verification service.")
    String container_signature_enforcement_digest() default "";

    @AttributeDefinition(name = "Image Download Retries", cardinality = 1, min = "0", description = "Specifies the number of retries the framework performs when attempting to pull the container image. Set to 0 for unlimited retries. Default: 5")
    int container_image_download_retries() default 5;

    @AttributeDefinition(name = "Image Download Retry Interval", cardinality = 1, min = "0", description = "The interval (in milliseconds) between retries to pull the container image. Default: 30000")
    int container_image_download_interval() default 30000;

    @AttributeDefinition(name = "Image Download Timeout", min = "1", description = "Image download timeout. Value specified in seconds. Default: 500")
    int container_image_download_timeout() default 500;

    @AttributeDefinition(name = "Internal Ports", cardinality = 1, required = false, description = "A comma-separated list of ports. If no protocol is specified tcp will be used. Note, the number of internal ports must be equal to the number of external ports. A port internet protocol can also be specified with a colon and text after the port number. Example: 80, 443:udp, 8080:tcp.")
    String container_ports_internal() default "80:tcp";

    @AttributeDefinition(name = "External Ports", cardinality = 1, required = false, description = "A comma separated list of ports. Note, the number of external ports must be equal to the number of internal ports. Example: 8080, 443.")
    String container_ports_external() default "8080";

    @AttributeDefinition(name = "Privileged Mode", cardinality = 1, description = "Give the container privileged access. (Warning: use this option at your own risk as privileged containers can be dangerous)")
    boolean container_privileged() default false;

    @AttributeDefinition(name = "Environment Variables", cardinality = 1, required = false, description = "Additional container enviroment variables. Example: example_var_1=123, example_var_2=123.")
    String container_env() default "";

    @AttributeDefinition(name = "Entrypoint Override", cardinality = 1, required = false, description = "Comma separated list which is used to override the command used to start a container. Example: ./test.sh,-v,-d,--human-readable")
    String container_entrypoint() default "";

    @AttributeDefinition(name = "Memory", required = false, description = "The maximum amount of memory the container can use in bytes. Set it as a positive integer, optionally followed by a suffix of b, k, m, g, to indicate bytes, kilobytes, megabytes, or gigabytes.              The minimum allowed value is platform dependent (i.e. 6m). If left empty, the memory assigned to the container will be set to a default value by the native container orchestrator.             This parameter only takes effect if the host system’s kernel has the corresponding cgroup v2 features enabled.             Please refer to the Kura documentation for more information.")
    String container_memory() default "";

    @AttributeDefinition(name = "CPUs", required = false, description = "Specify how many CPUs a container can use. Decimal values are allowed, so if set to 1.5, the container will use at most one and a half cpu resource.             This parameter only takes effect if the host system’s kernel has the corresponding cgroup v2 features enabled.             Please refer to the Kura documentation for more information.")
    float container_cpus();

    @AttributeDefinition(name = "GPUs", cardinality = 1, required = false, description = "Specify how many Nvidia GPUs a container can use. Allowed values are 'all' or an integer number. If there's no Nvidia GPU installed, leave the field empty.")
    String container_gpus() default "";

    @AttributeDefinition(name = "Volume Mount", cardinality = 1, required = false, description = "The path on the container at which you would like to mount a file or folder. Example: /path/on/host1:/path/on/container1, /path/on/host2:/path/on/container2.")
    String container_volume() default "";

    @AttributeDefinition(name = "Peripheral Device", cardinality = 1, required = false, description = "Used to pass physical devices to a container. Example: /dev/gpiomem, /dev/ttyUSB0. (Generally Requires privileged mode to be enabled)")
    String container_device() default "";

    @AttributeDefinition(name = "Runtime", cardinality = 1, required = false, description = "Specifies the fully qualified name of an alternate OCI-compatible runtime, which is used to run commands specified by the 'run' instruction. Example: 'nvidia' corresponds to '--runtime=nvidia'.")
    String container_runtime() default "";

    @AttributeDefinition(name = "Networking Mode", required = false, description = "Used to specify what networking mode the container will use. Possible Drivers: bridge, none, container:{container id}, host. Note: This field is case-sensitive.")
    String container_networkMode() default "";

    @AttributeDefinition(name = "Logger Type", options = { @Option(label = "NONE", value = "NONE"), @Option(label = "DEFAULT", value = "DEFAULT"), @Option(label = "LOCAL", value = "LOCAL"), @Option(label = "ETWLOGS", value = "ETWLOGS"), @Option(label = "JSON_FILE", value = "JSON_FILE"), @Option(label = "SYSLOG", value = "SYSLOG"), @Option(label = "JOURNALD", value = "JOURNALD"), @Option(label = "GELF", value = "GELF"), @Option(label = "FLUENTD", value = "FLUENTD"), @Option(label = "AWSLOGS", value = "AWSLOGS"), @Option(label = "DB", value = "DB"), @Option(label = "SPLUNK", value = "SPLUNK"), @Option(label = "GCPLOGS", value = "GCPLOGS"), @Option(label = "LOKI", value = "LOKI") }, description = "Used to specify what logging driver the container will use. By default, containers will log to a JSON-FILE on the gateway.")
    String container_loggingType() default "DEFAULT";

    @AttributeDefinition(name = "Logger Parameters", cardinality = 1, required = false, description = "Used to pass logger parameters to a container's logging driver. Example: max-size=10m, max-file=2. Default: max-size=10m")
    String container_loggerParameters() default "max-size=10m";

    @AttributeDefinition(name = "Restart Container On Failure", cardinality = 1, description = "Automatically restart the container when it has failed. Default: false")
    boolean container_restart_onfailure() default false;

    @AttributeDefinition(name = "Enable Identity Integration", cardinality = 1, description = "Enable integration with Kura Identity Service to provide temporary credentials to the container. Default: false")
    boolean container_identity_enabled() default false;

    @AttributeDefinition(name = "Container Permissions", cardinality = 1, required = false, description = "Comma-separated list of permissions to grant to the container when identity integration is enabled. Example: rest.assets,rest.configuration")
    String container_permissions() default "";

}


