/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.RequiredPermissions.Mode;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceSnapshotsServlet extends AuditServlet {

    private static final long serialVersionUID = -2533869595709953567L;

    private static Logger logger = LoggerFactory.getLogger(DeviceSnapshotsServlet.class);

    private DiskFileItemFactory diskFileItemFactory;
    private FileCleaningTracker fileCleaningTracker;

    public DeviceSnapshotsServlet() {
        super("UI Snapshots", "Return device snapshot");
    }

    // USED TO RETRIEVE WIREGRAPH SNAPSHOT, DEVICE LOG AND ENTIRE SYSTEM SNAPSHOT
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        KuraRemoteServiceServlet.requirePermissions(request, Mode.ALL, new String[] { KuraPermission.ADMIN });

        // BEGIN XSRF - Servlet dependent code

        try {
            GwtXSRFToken token = new GwtXSRFToken(request.getParameter("xsrfToken"));
            KuraRemoteServiceServlet.checkXSRFToken(request, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        try {

            String snapshotId = request.getParameter("snapshotId");
            ServiceLocator locator = ServiceLocator.getInstance();
            ConfigurationService cs = locator.getService(ConfigurationService.class);
            if (snapshotId != null) {

                long sid = Long.parseLong(snapshotId);

                GwtServerUtil.writeSnapshot(response, cs.getSnapshot(sid), "snapshot_" + sid,
                        request.getParameter("format"));

            }
        } catch (Exception e) {
            logger.error("Error exporting snapshot");
            throw new ServletException(e);
        }
    }

    // USED TO RETRIEVE PARTIAL SYSTEM SNAPSHOT
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html");

        DownloadSnapshotRequest upload = new DownloadSnapshotRequest(this.diskFileItemFactory);
        List<String> errors = new ArrayList<>();

        try {
            upload.parse(request);
        } catch (FileUploadException e) {
            errors.add("Error parsing the file upload request");
            response.getWriter().write("Error parsing the file upload request.");
            throw new IOException(e);
        }

        Map<String, String> formFields = upload.getFormFields();

        KuraRemoteServiceServlet.requirePermissions(request, Mode.ALL, new String[] { KuraPermission.ADMIN });

        try {
            GwtXSRFToken token = new GwtXSRFToken(formFields.get("xsrfToken"));
            KuraRemoteServiceServlet.checkXSRFToken(request, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        try {

            ServiceLocator locator = ServiceLocator.getInstance();
            ConfigurationService cs = locator.getService(ConfigurationService.class);

            String format = formFields.get("downloadFormat");
            Long snapshotId = Long.parseLong(formFields.get("snapshotId"));
            List<String> selectedPids = Arrays.asList(formFields.get("pidsList").split(","));

            List<ComponentConfiguration> configs = cs.getSnapshot(snapshotId).stream().filter(config -> {
                return selectedPids.contains(config.getPid());
            }).collect(Collectors.toList());

            GwtServerUtil.writeSnapshot(response, configs, "snapshot_" + snapshotId, format);

        } catch (Exception e) {
            logger.error("Error exporting snapshot");
            throw new ServletException(e);
        }

    }

    /*
     * Utils
     */

    @Override
    public void destroy() {
        super.destroy();

        logger.info("Servlet {} destroyed", getServletName());

        if (this.fileCleaningTracker != null) {
            logger.info("Number of temporary files tracked: {}", this.fileCleaningTracker.getTrackCount());
        }
    }

    private int getFileUploadInMemorySizeThreshold() {
        ServiceLocator locator = ServiceLocator.getInstance();

        int sizeThreshold = DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD;
        try {
            SystemService systemService = locator.getService(SystemService.class);
            sizeThreshold = Integer
                    .parseInt(systemService.getProperties().getProperty("file.upload.in.memory.size.threshold",
                            String.valueOf(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD)));
        } catch (GwtKuraException e) {
            logger.error("Error locating SystemService", e);
        }

        return sizeThreshold;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        logger.info("Servlet {} initialized", getServletName());

        ServletContext ctx = getServletContext();
        this.fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(ctx);

        int sizeThreshold = getFileUploadInMemorySizeThreshold();
        File repository = new File(System.getProperty("java.io.tmpdir"));

        logger.debug("DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD: {}", DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        logger.debug("DiskFileItemFactory: using size threshold of: {}", sizeThreshold);

        this.diskFileItemFactory = new DiskFileItemFactory(sizeThreshold, repository);
        this.diskFileItemFactory.setFileCleaningTracker(this.fileCleaningTracker);
    }
}

class DownloadSnapshotRequest extends ServletFileUpload {

    private static Logger logger = LoggerFactory.getLogger(UploadRequest.class);

    Map<String, String> formFields;

    public DownloadSnapshotRequest(DiskFileItemFactory diskFileItemFactory) {
        super(diskFileItemFactory);
        setSizeMax(FileServlet.getFileUploadSizeMax());
        // contrary to what the name says, this method does not set the number of
        // allowed files but the number of parts
        // (files and fields)
        setFileCountMax(10L);
        this.formFields = new HashMap<>();
    }

    public void parse(HttpServletRequest req) throws FileUploadException {
        List<FileItem> items = null;
        items = parseRequest(req);
        Iterator<FileItem> iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = iter.next();

            if (item.isFormField()) {
                String name = item.getFieldName();
                String value = item.getString();

                logger.debug("Form field item name: {}, value: {}", name, value);

                this.formFields.put(name, value);
            }
        }
    }

    public Map<String, String> getFormFields() {
        return this.formFields;
    }

}
