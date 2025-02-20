/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.KuraErrorCode;
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

import com.eclipsesource.json.JsonObject;

public class JsonMarshallUnmarshallImpl implements Marshaller, Unmarshaller {

    @Override
    public String marshal(Object object) throws KuraException {
        if (object instanceof WireGraphConfiguration) {
            JsonObject result = WireGraphJsonMarshallUnmarshallImpl
                    .marshalWireGraphConfiguration((WireGraphConfiguration) object);
            return result.toString();
        } else if (object instanceof KuraPayload) {
            return CloudPayloadJsonEncoder.marshal((KuraPayload) object);
        } else if (object instanceof SystemDeploymentPackages) {
            return JsonJavaSystemDeploymentPackagesMapper.marshal((SystemDeploymentPackages) object);
        } else if (object instanceof SystemBundles) {
            return JsonJavaSystemBundlesMapper.marshal((SystemBundles) object);
        } else if (object instanceof SystemPackages) {
            return JsonJavaSystemPackagesMapper.marshal((SystemPackages) object);
        } else if (object instanceof DockerContainers) {
            return JsonJavaDockerContainersMapper.marshal((DockerContainers) object);
        } else if (object instanceof ContainerImages) {
            return JsonJavaContainerImagesMapper.marshal((ContainerImages) object);
        } else if (object instanceof SystemResourcesInfo) {
            return JsonJavaSystemResourcesMapper.marshal((SystemResourcesInfo) object);
        }
        throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
    }

    @Override
    public void marshal(OutputStream out, Object object) throws KuraException {
        try {
            if (object instanceof WireGraphConfiguration) {
                JsonObject result = WireGraphJsonMarshallUnmarshallImpl
                        .marshalWireGraphConfiguration((WireGraphConfiguration) object);
                out.write(result.toString().getBytes());
            } else if (object instanceof KuraPayload) {
                out.write(CloudPayloadJsonEncoder.marshal((KuraPayload) object).getBytes());
            } else if (object instanceof SystemDeploymentPackages) {
                out.write(JsonJavaSystemDeploymentPackagesMapper.marshal((SystemDeploymentPackages) object).getBytes());
            } else if (object instanceof SystemBundles) {
                out.write(JsonJavaSystemBundlesMapper.marshal((SystemBundles) object).getBytes());
            } else if (object instanceof SystemPackages) {
                out.write(JsonJavaSystemPackagesMapper.marshal((SystemPackages) object).getBytes());
            } else if (object instanceof DockerContainers) {
                out.write(JsonJavaDockerContainersMapper.marshal((DockerContainers) object).getBytes());
            } else if (object instanceof ContainerImages) {
                out.write(JsonJavaContainerImagesMapper.marshal((ContainerImages) object).getBytes());
            } else if (object instanceof SystemResourcesInfo) {
                out.write(JsonJavaSystemResourcesMapper.marshal((SystemResourcesInfo) object).getBytes());
            }
        } catch (IOException ex) {
            throw new KuraIOException(ex);
        }

        throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(String s, Class<T> clazz) throws KuraException {
        if (clazz.equals(WireGraphConfiguration.class)) {
            return (T) WireGraphJsonMarshallUnmarshallImpl.unmarshalToWireGraphConfiguration(s);
        } else if (clazz.equals(KuraPayload.class)) {
            return (T) CloudPayloadJsonDecoder.buildFromString(s);
        } else if (EntryInfo.class.isAssignableFrom(clazz)) {
            return (T) KeystoreEntryInfoMapper.unmarshal(s, clazz);
        } else if (clazz.equals(SystemBundleRef.class)) {
            return (T) JsonJavaSystemBundleRefMapper.unmarshal(s);
        } else if (clazz.equals(DockerContainer.class)) {
            return (T) JsonJavaDockerContainersMapper.unmarshal(s);
        } else if (clazz.equals(ContainerImage.class)) {
            return (T) JsonJavaContainerImagesMapper.unmarshal(s);
        }
        throw new IllegalArgumentException("Invalid parameter!");
    }

    @Override
    public <T> T unmarshal(InputStream in, Class<T> clazz) throws KuraException {
        try {
            String inputString = IOUtils.toString(in, StandardCharsets.UTF_8);

            if (clazz.equals(WireGraphConfiguration.class)) {
                return (T) WireGraphJsonMarshallUnmarshallImpl.unmarshalToWireGraphConfiguration(inputString);
            } else if (clazz.equals(KuraPayload.class)) {
                return (T) CloudPayloadJsonDecoder.buildFromString(inputString);
            } else if (EntryInfo.class.isAssignableFrom(clazz)) {
                return (T) KeystoreEntryInfoMapper.unmarshal(inputString, clazz);
            } else if (clazz.equals(SystemBundleRef.class)) {
                return (T) JsonJavaSystemBundleRefMapper.unmarshal(inputString);
            } else if (clazz.equals(DockerContainer.class)) {
                return (T) JsonJavaDockerContainersMapper.unmarshal(inputString);
            } else if (clazz.equals(ContainerImage.class)) {
                return (T) JsonJavaContainerImagesMapper.unmarshal(inputString);
            }
        } catch (IOException ex) {
            throw new KuraIOException(ex);
        }

        throw new IllegalArgumentException("Invalid parameter!");
    }

}
