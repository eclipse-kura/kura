/*******************************************************************************
 * Copyright (c) 2017, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.json.marshaller.unmarshaller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.CharSequenceInputStream;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.core.inventory.resources.ContainerImage;
import org.eclipse.kura.core.inventory.resources.ContainerImages;
import org.eclipse.kura.core.inventory.resources.DockerContainer;
import org.eclipse.kura.core.inventory.resources.DockerContainers;
import org.eclipse.kura.core.inventory.resources.SystemBundleRef;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.keystore.KeystoreEntryInfoMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonDecoder;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonEncoder;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaContainerImagesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaDockerContainersMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemBundleRefMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemBundlesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemDeploymentPackagesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemPackagesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemResourcesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.wiregraph.WireGraphJsonMarshallUnmarshallImpl;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;

public class JsonMarshallUnmarshallImpl implements Marshaller, Unmarshaller {

    @Override
    public String marshal(Object object) throws KuraException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        marshal(outStream, object);
        return new String(outStream.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public void marshal(OutputStream outStream, Object object) throws KuraIOException {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);

            if (object instanceof WireGraphConfiguration) {
                WireGraphJsonMarshallUnmarshallImpl.marshalWireGraphConfiguration(writer,
                        (WireGraphConfiguration) object);
            } else if (object instanceof KuraPayload) {
                CloudPayloadJsonEncoder.marshal(writer, (KuraPayload) object);
            } else if (object instanceof SystemDeploymentPackages) {
                JsonJavaSystemDeploymentPackagesMapper.marshal(writer, (SystemDeploymentPackages) object);
            } else if (object instanceof SystemBundles) {
                JsonJavaSystemBundlesMapper.marshal(writer, (SystemBundles) object);
            } else if (object instanceof SystemPackages) {
                JsonJavaSystemPackagesMapper.marshal(writer, (SystemPackages) object);
            } else if (object instanceof DockerContainers) {
                JsonJavaDockerContainersMapper.marshal(writer, (DockerContainers) object);
            } else if (object instanceof ContainerImages) {
                JsonJavaContainerImagesMapper.marshal(writer, (ContainerImages) object);
            } else if (object instanceof SystemResourcesInfo) {
                JsonJavaSystemResourcesMapper.marshal(writer, (SystemResourcesInfo) object);
            }

            writer.flush();

        } catch (IOException ex) {
            throw new KuraIOException(ex);
        }
    }

    @Override
    public <T> T unmarshal(String inputString, Class<T> clazz) throws KuraException {

        CharSequenceInputStream stream = CharSequenceInputStream.builder().setCharSequence(inputString)
                .setBufferSize(8192).setCharset(StandardCharsets.UTF_8).get();

        return unmarshal(stream, clazz);
    }

    @Override
    public <T> T unmarshal(InputStream inputStream, Class<T> clazz) throws KuraException {
        try {

            final Reader jsonReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            if (clazz.equals(WireGraphConfiguration.class)) {
                return (T) WireGraphJsonMarshallUnmarshallImpl.unmarshalToWireGraphConfiguration(jsonReader);
            } else if (clazz.equals(KuraPayload.class)) {
                return (T) CloudPayloadJsonDecoder.buildFromReader(jsonReader);
            } else if (EntryInfo.class.isAssignableFrom(clazz)) {
                return (T) KeystoreEntryInfoMapper.unmarshal(jsonReader, clazz);
            } else if (clazz.equals(SystemBundleRef.class)) {
                return (T) JsonJavaSystemBundleRefMapper.unmarshal(jsonReader);
            } else if (clazz.equals(DockerContainer.class)) {
                return (T) JsonJavaDockerContainersMapper.unmarshal(jsonReader);
            } else if (clazz.equals(ContainerImage.class)) {
                return (T) JsonJavaContainerImagesMapper.unmarshal(jsonReader);
            }
        } catch (IOException ex) {
            throw new KuraIOException(ex);
        }

        throw new IllegalArgumentException("Invalid parameter!");
    }

}
