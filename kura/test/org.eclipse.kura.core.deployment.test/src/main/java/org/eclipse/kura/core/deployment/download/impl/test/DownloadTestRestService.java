/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.deployment.download.impl.test;

import java.io.IOException;

import org.osgi.service.component.annotations.Component;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("test/download")
@Component(immediate = true, property = {
        "kura.service.pid=org.eclipse.kura.core.deployment.download.impl.test.DownloadTestRestService",
        "osgi.jakartars.resource=true" }, service = DownloadTestRestService.class)
public class DownloadTestRestService {

    private static final String FILE_NAME = "test";

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile() {
        byte[] content;
        try {
            content = getClass().getClassLoader().getResourceAsStream(FILE_NAME).readAllBytes();
            return Response.ok(content, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(HttpHeaders.CONTENT_LENGTH, content.length).build();
        } catch (IOException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }
}
