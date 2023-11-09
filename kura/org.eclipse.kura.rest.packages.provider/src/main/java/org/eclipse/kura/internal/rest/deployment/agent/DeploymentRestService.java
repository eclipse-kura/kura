/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.rest.deployment.agent;

import static org.eclipse.kura.rest.deployment.agent.api.Validable.validate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.deployment.agent.MarketplacePackageDescriptor;
import org.eclipse.kura.rest.deployment.agent.api.DeploymentRequestStatus;
import org.eclipse.kura.rest.deployment.agent.api.DescriptorRequest;
import org.eclipse.kura.rest.deployment.agent.api.InstallRequest;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/deploy/v2")
public class DeploymentRestService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentRestService.class);

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    private static final String KURA_PERMISSION_REST_DEPLOY_ROLE = "kura.permission.rest.deploy";
    private static final String ERROR_INSTALLING_PACKAGE = "Error installing deployment package: ";
    private static final String ERROR_UNINSTALLING_PACKAGE = "Error uninstalling deployment package: ";
    private static final String BAD_REQUEST_MESSAGE = "Bad request";

    private DeploymentAdmin deploymentAdmin;
    private DeploymentAgentService deploymentAgentService;
    private UserAdmin userAdmin;

    public void setUserAdmin(UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
        this.userAdmin.createRole(KURA_PERMISSION_REST_DEPLOY_ROLE, Role.GROUP);
    }

    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    public void setDeploymentAgentService(DeploymentAgentService deploymentAgentService) {
        this.deploymentAgentService = deploymentAgentService;
    }

    /**
     * GET method.
     *
     * Provides the list of all the deployment packages installed and tracked by the
     * framework.
     *
     * @return a list of {@link DeploymentPackageInfo}
     */
    @GET
    @RolesAllowed("deploy")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeploymentPackageInfo> listDeploymentPackages() {

        List<DeploymentPackageInfo> deploymentPackageInfos = new ArrayList<>();
        List<DeploymentPackage> deploymentPackages = Arrays.asList(this.deploymentAdmin.listDeploymentPackages());

        deploymentPackages.forEach(
                dp -> deploymentPackageInfos.add(new DeploymentPackageInfo(dp.getName(), dp.getVersion().toString())));

        return deploymentPackageInfos;
    }

    /**
     * PUT method.
     *
     * Provides the Eclipse Marketplace Package Descriptor information of the
     * deployment package identified by URL passed as query parameter.
     * 
     * @param DescriptorRequest
     *
     * @return a list of {@link MarketplacePackageDescriptor}
     */
    @PUT
    @RolesAllowed("deploy")
    @Path("/_packageDescriptor")
    @Produces(MediaType.APPLICATION_JSON)
    public MarketplacePackageDescriptor getMarketplacePackageDescriptor(DescriptorRequest descriptorRequest) {
        validate(descriptorRequest, BAD_REQUEST_MESSAGE);
        String url = descriptorRequest.getUrl();

        MarketplacePackageDescriptor descriptor;

        if (Objects.isNull(url) || url.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity("Missing URL parameter").build());
        }

        if (!isEclipseMarketplaceUrl(url)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity("The URL passed as argument does not belong to the Eclipse Marketplace").build());
        }

        final String[] urlStrings = url.split("=");
        if (urlStrings.length != 2 || Objects.isNull(urlStrings[1]) || urlStrings[1].isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity("The URL passed as argument does not contain a valid node id").build());
        }

        final String descriptorUrl = String.format("https://marketplace.eclipse.org/node/%s/api/p", urlStrings[1]);
        try {
            descriptor = this.deploymentAgentService.getMarketplacePackageDescriptor(descriptorUrl);
        } catch (Exception e) {
            logger.warn("Error checking package descriptor for {}. Caused by ", url, e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity("Error checking package descriptor for " + url).build());
        }

        return descriptor;
    }

    private boolean isEclipseMarketplaceUrl(String url) {
        final Pattern marketplaceUrlRegexp = Pattern
                .compile("https?:\\/\\/marketplace.eclipse.org/marketplace-client-intro\\?mpc_install=.*");
        return marketplaceUrlRegexp.matcher(url).matches();
    }

    /**
     * POST method.
     *
     * Installs the deployment package specified in the {@link InstallRequest}. If
     * the request was already issued for
     * the same {@link InstallRequest}, it returns the status of the installation
     * process.
     *
     * @param installRequest
     * @return a {@link DeploymentRequestStatus} object that represents the status
     *         of the installation request
     */
    @POST
    @RolesAllowed("deploy")
    @Path("/_install")
    @Produces(MediaType.APPLICATION_JSON)
    public DeploymentRequestStatus installDeploymentPackage(InstallRequest installRequest) {
        validate(installRequest, BAD_REQUEST_MESSAGE);
        String url = installRequest.getUrl();

        if (this.deploymentAgentService.isInstallingDeploymentPackage(url)) {
            return DeploymentRequestStatus.INSTALLING;
        }

        try {
            this.deploymentAgentService.installDeploymentPackageAsync(url);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity(ERROR_INSTALLING_PACKAGE + url).build());
        }

        return DeploymentRequestStatus.REQUEST_RECEIVED;
    }

    /**
     * POST method.
     *
     * Installs the deployment package uploaded through HTTP POST method
     * (multipart/form-data).
     *
     * @param uploadedInputStread
     * @param fileDetails
     * @return a {@link DeploymentRequestStatus} object that represents the status
     *         of the installation request
     */
    @POST
    @RolesAllowed("deploy")
    @Path("/_upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public DeploymentRequestStatus installUploadedDeploymentPackage(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetails) {

        final String uploadedFileName = fileDetails.getFileName();
        final String uploadedFileLocation = System.getProperty(JAVA_IO_TMPDIR) + File.separator + UUID.randomUUID()
                + ".dp";

        try {
            Files.deleteIfExists(Paths.get(uploadedFileLocation));
        } catch (IOException e) {
            logger.warn("Cannot delete file: {}", uploadedFileLocation);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity(ERROR_INSTALLING_PACKAGE + uploadedFileName).build());
        }

        File file = new File(uploadedFileLocation);
        try {
            if (!file.createNewFile()) {
                throw new IOException("File " + uploadedFileLocation + " was not created");
            }
            file.deleteOnExit();
        } catch (IOException e) {
            logger.warn("Cannot create file: {}, caused by", file, e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity(ERROR_INSTALLING_PACKAGE + uploadedFileName).build());
        }

        try {
            FileOutputStream os = new FileOutputStream(file);
            IOUtils.copy(uploadedInputStream, os);
            os.close();
        } catch (IOException e) {
            logger.warn("Error writing file to : {}, caused by", file.getAbsolutePath(), e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity(ERROR_INSTALLING_PACKAGE + uploadedFileName).build());
        }
        logger.info("Deployment package \"{}\" uploaded to: {}", uploadedFileName, file.getAbsolutePath());

        try {
            String fileUrl = file.toURI().toURL().toString();
            this.deploymentAgentService.installDeploymentPackageAsync(fileUrl);
        } catch (Exception e) {
            logger.warn("Cannot install deployment package : {}, caused by", uploadedFileName, e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity(ERROR_INSTALLING_PACKAGE + uploadedFileName).build());
        }

        return DeploymentRequestStatus.REQUEST_RECEIVED;
    }

    /**
     * DELETE method.
     *
     * Uninstalls the deployment package identified by the specified name. If the
     * request was already issued, it reports
     * the status of the uninstallation operation.
     *
     * @param name
     * @return a {@link DeploymentRequestStatus} object that represents the status
     *         of the uninstallation request
     */
    @DELETE
    @RolesAllowed("deploy")
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeploymentRequestStatus uninstallDeploymentPackage(@PathParam("name") String name) {
        if (this.deploymentAgentService.isUninstallingDeploymentPackage(name)) {
            return DeploymentRequestStatus.UNINSTALLING;
        }
        try {
            this.deploymentAgentService.uninstallDeploymentPackageAsync(name);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity(ERROR_UNINSTALLING_PACKAGE + name).build());
        }

        return DeploymentRequestStatus.REQUEST_RECEIVED;
    }
}
