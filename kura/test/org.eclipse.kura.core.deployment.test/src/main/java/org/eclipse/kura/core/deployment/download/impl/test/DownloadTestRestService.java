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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("test/download")
public class DownloadTestRestService {

    private static final String FILE_NAME = "test";

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(FILE_NAME)) {
            int numberOfBytes;
            byte[] data = new byte[4096];

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            while ((numberOfBytes = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, numberOfBytes);
            }
            buffer.flush();
            byte[] content = buffer.toByteArray();
            return Response.ok(buffer.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(HttpHeaders.CONTENT_LENGTH, content.length).build();
        } catch (IOException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }
}
