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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.rest.deployment.agent.api.DeploymentRequestStatus;
import org.eclipse.kura.rest.deployment.agent.api.InstallRequest;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

@Path("/deploy/v2")
public class DeploymentRestService {

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
     * Provides the list of all the deployment packages installed and tracked by the framework.
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
     * POST method.
     *
     * Installs the deployment package specified in the {@link InstallRequest}. If the request was already issued for
     * the same {@link InstallRequest}, it returns the status of the installation process.
     *
     * @param installRequest
     * @return a {@link DeploymentRequestStatus} object that represents the status of the installation request
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
     * Installs the deployment package specified in the {@link InstallRequest}. If
     * the request was already issued for
     * the same {@link InstallRequest}, it returns the status of the installation
     * process.
     *
     * @param dataInputStream
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

        String uploadedFileLocation = "/tmp/" + fileDetails.getFileName();

        File file = new File(uploadedFileLocation);
        if (file.exists()) {
            logger.warn("File already exists at : {}", file.getAbsolutePath());
        } else {
            try {
                writeToFile(uploadedInputStream, file);
            } catch (IOException e) {
                logger.warn("Error writing file to : {}, caused by", uploadedFileLocation, e);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .type(MediaType.TEXT_PLAIN).entity(ERROR_INSTALLING_PACKAGE + uploadedFileLocation).build());
            }
            logger.info("File uploaded to : {}", file.getAbsolutePath());
        }

        if (this.deploymentAgentService.isInstallingDeploymentPackage(uploadedFileLocation)) {
            return DeploymentRequestStatus.INSTALLING;
        }

        try {
            this.deploymentAgentService.installDeploymentPackageAsync(file.toURI().toURL().toString());
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN).entity(ERROR_INSTALLING_PACKAGE + uploadedFileLocation).build());
        }

        return DeploymentRequestStatus.REQUEST_RECEIVED;
    }

    private void writeToFile(InputStream uploadedInputStream, File uploadedFile) throws IOException {
        try (OutputStream out = new FileOutputStream(uploadedFile)) {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
        }
    }

    /**
     * DELETE method.
     *
     * Uninstalls the deployment package identified by the specified name. If the request was already issued, it reports
     * the status of the uninstallation operation.
     *
     * @param name
     * @return a {@link DeploymentRequestStatus} object that represents the status of the uninstallation request
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
