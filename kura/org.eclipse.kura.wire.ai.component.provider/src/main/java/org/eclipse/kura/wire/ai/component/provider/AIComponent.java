/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.ai.component.provider;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.ai.inference.InferenceEngineModelManagerService;
import org.eclipse.kura.ai.inference.InferenceEngineService;
import org.eclipse.kura.ai.inference.ModelInfo;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIComponent implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(AIComponent.class);

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private AIComponentOptions options;
    private InferenceEngineService inferenceEngineService;
    private InferenceEngineModelManagerService inferenceEngineModelManagerService;

    private Optional<ModelInfo> infoPre;
    private Optional<ModelInfo> infoInfer;
    private Optional<ModelInfo> infoPost;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    public void bindInferenceEngineService(final InferenceEngineService inferenceEngineService) {
        if (this.inferenceEngineService == null) {
            this.inferenceEngineService = inferenceEngineService;
        }
    }

    public void unbindInferenceEngineService(final InferenceEngineService inferenceEngineService) {
        if (this.inferenceEngineService == inferenceEngineService) {
            this.inferenceEngineService = inferenceEngineService;
        }
    }

    public void bindInferenceEngineModelManagerService(
            final InferenceEngineModelManagerService inferenceEngineModelManagerService) {
        if (this.inferenceEngineModelManagerService == null) {
            this.inferenceEngineModelManagerService = inferenceEngineModelManagerService;
        }
    }

    public void unbindInferenceEngineModelManagerService(
            final InferenceEngineModelManagerService inferenceEngineModelManagerService) {
        if (this.inferenceEngineModelManagerService == inferenceEngineModelManagerService) {
            this.inferenceEngineModelManagerService = inferenceEngineModelManagerService;
        }
    }

    public void activate(final ComponentContext componentContext, final Map<String, Object> properties)
            throws ComponentException {
        logger.info("Activating AIComponent...");

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        updated(properties);

        logger.info("ActivatingScript AIComponent... Done");
    }

    public void deactivate() {
        logger.info("Deactivating AIComponent...");
        logger.info("Deactivating AIComponent... Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("Updating AIComponent...");

        this.options = new AIComponentOptions(properties);

        try {
            if (this.options.getPreprocessorModelName().isPresent()) {
                this.infoPre = retrieveModelInfo(this.options.getPreprocessorModelName().get());
            }

            this.infoInfer = retrieveModelInfo(this.options.getInferenceModelName());

            if (this.options.getPostprocessorModelName().isPresent()) {
                this.infoPost = retrieveModelInfo(this.options.getPostprocessorModelName().get());
            }

        } catch (KuraIOException e) {
            logger.error("Error opening model.", e);
            this.deactivate();
        }

        logger.info("Updating AIComponent... Done");
    }

    private Optional<ModelInfo> retrieveModelInfo(String modelName) throws KuraIOException {
        Optional<ModelInfo> info = this.inferenceEngineModelManagerService.getModelInfo(modelName);
        if (!info.isPresent()) {
            throw new KuraIOException("Unable to retrieve model info associated to " + modelName);
        }

        return info;
    }

    @Override
    public synchronized void onWireReceive(WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        logger.info("AIComponent: received envelope.");

        List<Tensor> inputs;

        if (this.infoPre.isPresent()) {
            inputs = getTensors(this.infoPre.get(), wireEnvelope.getRecords());
        } else {
            inputs = getTensors(this.infoPre.get(), wireEnvelope.getRecords());
        }

        /*
         * List<WireRecord> records = wireEnvelope.getRecords();
         * 
         * for (WireRecord record : records) {
         * Map<String, TypedValue<?>> properties = record.getProperties();
         * 
         * for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {
         * String channelName = entry.getKey();
         * TypedValue<?> channelValue = entry.getValue();
         * 
         * DataType valueType = channelValue.getType();
         * switch (valueType) {
         * case BOOLEAN:
         * // TODO
         * break;
         * case BYTE_ARRAY:
         * // TODO
         * break;
         * case DOUBLE:
         * // TODO
         * break;
         * case FLOAT:
         * // TODO
         * break;
         * case INTEGER:
         * // TODO
         * break;
         * case LONG:
         * // TODO
         * break;
         * case STRING:
         * // TODO
         * break;
         * default:
         * break;
         * }
         * 
         * }
         * 
         * }
         */

        logger.info("AIComponent: emitting an empty result.");

        List<WireRecord> result = Collections.unmodifiableList(new ArrayList<WireRecord>());
        this.wireSupport.emit(result);
    }

    private List<Tensor> getTensors(ModelInfo info, List<WireRecord> records) {

        List<Tensor> result = new LinkedList<>();

        for (WireRecord record : records) {
            Map<String, TypedValue<?>> properties = record.getProperties();

            for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {
                String channelName = entry.getKey();
                TypedValue<?> channelValue = entry.getValue();

                DataType valueType = channelValue.getType();
                switch (valueType) {
                case BOOLEAN:
                    // TODO
                    break;
                case BYTE_ARRAY:
                    // TODO
                    break;
                case DOUBLE:
                    // TODO
                    break;
                case FLOAT:
                    // TODO
                    break;
                case INTEGER:
                    // TODO
                    break;
                case LONG:
                    // TODO
                    break;
                case STRING:
                    // TODO
                    break;
                default:
                    break;
                }

            }

        }

        return null;
    }

    @Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public void updated(Wire wire, Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }
}
