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
 ******************************************************************************/
package org.eclipse.kura.ai.triton.server;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.ai.inference.InferenceEngineModelManagerService;
import org.eclipse.kura.ai.inference.InferenceEngineService;
import org.eclipse.kura.ai.inference.InferenceEngineStatusService;
import org.eclipse.kura.ai.inference.ModelInfo;
import org.eclipse.kura.ai.inference.ModelInfoBuilder;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.eclipse.kura.ai.inference.TensorDescriptorBuilder;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.executor.CommandExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;

import inference.GRPCInferenceServiceGrpc;
import inference.GRPCInferenceServiceGrpc.GRPCInferenceServiceBlockingStub;
import inference.GrpcService.InferParameter;
import inference.GrpcService.InferTensorContents;
import inference.GrpcService.ModelInferRequest;
import inference.GrpcService.ModelInferResponse;
import inference.GrpcService.ModelInferResponse.InferOutputTensor;
import inference.GrpcService.ModelMetadataRequest;
import inference.GrpcService.ModelMetadataResponse;
import inference.GrpcService.ModelReadyRequest;
import inference.GrpcService.ModelReadyResponse;
import inference.GrpcService.RepositoryIndexRequest;
import inference.GrpcService.RepositoryIndexResponse;
import inference.GrpcService.RepositoryIndexResponse.ModelIndex;
import inference.GrpcService.RepositoryModelLoadRequest;
import inference.GrpcService.RepositoryModelUnloadRequest;
import inference.GrpcService.ServerLiveRequest;
import inference.GrpcService.ServerLiveResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class TritonServerServiceImpl implements InferenceEngineService, InferenceEngineStatusService,
        InferenceEngineModelManagerService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerServiceImpl.class);

    private CommandExecutorService commandExecutorService;
    private TritonServerServiceOptions options;
    private TritonServerLocalManager tritonServerLocalManager;

    private ManagedChannel grpcChannel;
    private GRPCInferenceServiceBlockingStub grpcStub;

    public void setCommandExecutorService(CommandExecutorService executorService) {
        if (isNull(this.commandExecutorService)) {
            this.commandExecutorService = executorService;
        }
    }

    public void unsetCommandExecutorService(CommandExecutorService executorService) {
        if (this.commandExecutorService == executorService) {
            this.commandExecutorService = null;
        }
    }

    protected void activate(Map<String, Object> properties) {
        logger.info("Activate TritonServerService...");
        this.options = new TritonServerServiceOptions(properties);
        if (isConfigurationValid()) {
            setGrpcResources();
            this.tritonServerLocalManager = new TritonServerLocalManager(this.options, this.commandExecutorService);
            this.tritonServerLocalManager.start();
            loadModels();
        } else {
            logger.warn("The provided configuration is not valid");
        }
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Update TritonServerService...");
        this.tritonServerLocalManager.stop();
        this.options = new TritonServerServiceOptions(properties);
        if (isConfigurationValid()) {
            setGrpcResources();
            this.tritonServerLocalManager = new TritonServerLocalManager(this.options, this.commandExecutorService);
            this.tritonServerLocalManager.start();
            loadModels();
        } else {
            logger.warn("The provided configuration is not valid");
        }
    }

    protected void deactivate() {
        logger.info("Deactivate TritonServerService...");
        this.tritonServerLocalManager.stop();
        this.grpcChannel.shutdownNow();
    }

    private void setGrpcResources() {
        this.grpcChannel = ManagedChannelBuilder.forAddress(this.options.getAddress(), this.options.getGrpcPort())
                .usePlaintext().build();
        setGrpcStub(GRPCInferenceServiceGrpc.newBlockingStub(this.grpcChannel));
    }

    protected void setGrpcStub(GRPCInferenceServiceBlockingStub grpcStub) {
        this.grpcStub = grpcStub;
    }

    private boolean isConfigurationValid() {
        if (!this.options.isLocalEnabled()) {
            return !isNullOrEmpty(this.options.getAddress());
        }
        return !isNullOrEmpty(this.options.getBackendsPath()) && !isNullOrEmpty(this.options.getModelRepositoryPath());
    }

    private boolean isNullOrEmpty(String property) {
        return isNull(property) || property.isEmpty();
    }

    protected void loadModels() {
        if (!this.options.isLocalEnabled()) {
            int index = 0;
            try {
                while (!isEngineReady()) {
                    if (index++ >= 6) {
                        return;
                    }
                    TritonServerLocalManager.sleepFor(10);
                }
            } catch (KuraIOException e) {
                logger.error("Cannot read engine status", e);
            }
            this.options.getModels().forEach(modelName -> {
                try {
                    loadModel(modelName, Optional.empty());
                } catch (KuraIOException e) {
                    logger.error("Cannot load model " + modelName, e);
                }
            });
        }
    }

    @Override
    public void loadModel(String modelName, Optional<String> model) throws KuraIOException {
        RepositoryModelLoadRequest.Builder builder = RepositoryModelLoadRequest.newBuilder();
        builder.setModelName(modelName);
        try {
            this.grpcStub.repositoryModelLoad(builder.build());
        } catch (StatusRuntimeException e) {
            throw new KuraIOException(e, "Cannot load the model " + modelName);
        }
    }

    @Override
    public void loadModel(String modelName, String modelPath) throws KuraIOException {
        // to do...
    }

    @Override
    public void unloadModel(String modelName) throws KuraIOException {
        RepositoryModelUnloadRequest.Builder builder = RepositoryModelUnloadRequest.newBuilder();
        builder.setModelName(modelName);
        try {
            this.grpcStub.repositoryModelUnload(builder.build());
        } catch (StatusRuntimeException e) {
            throw new KuraIOException(e, "Cannot unload the model " + modelName);
        }

    }

    @Override
    public boolean isModelLoaded(String modelName) throws KuraIOException {
        boolean isLoaded = false;

        ModelReadyRequest.Builder builder = ModelReadyRequest.newBuilder();
        builder.setName(modelName);
        try {
            ModelReadyResponse modelReadyResponse = this.grpcStub.modelReady(builder.build());
            isLoaded = modelReadyResponse.getReady();
        } catch (StatusRuntimeException e) {
            throw new KuraIOException(e, "Cannot unload the model " + modelName);
        }
        return isLoaded;
    }

    @Override
    public List<String> getModelNames() throws KuraIOException {
        List<String> modelNames = new ArrayList<>();

        RepositoryIndexRequest repositoryIndexRequest = RepositoryIndexRequest.getDefaultInstance();
        try {
            RepositoryIndexResponse repositoryIndexResponse = this.grpcStub.repositoryIndex(repositoryIndexRequest);
            List<ModelIndex> models = repositoryIndexResponse.getModelsList();
            models.forEach(model -> modelNames.add(model.getName()));
        } catch (StatusRuntimeException e) {
            throw new KuraIOException(e, "Cannot get the list of model names from server");
        }
        return modelNames;
    }

    @Override
    public Optional<ModelInfo> getModelInfo(String modelName) throws KuraIOException {
        Optional<ModelInfo> modelInfo = Optional.empty();
        ModelMetadataRequest.Builder builder = ModelMetadataRequest.newBuilder();
        builder.setName(modelName);
        try {
            ModelMetadataResponse modelMetadataResponse = this.grpcStub.modelMetadata(builder.build());

            ModelInfoBuilder infoBuilder = ModelInfo.builder(modelMetadataResponse.getName());
            String modelPlatform = modelMetadataResponse.getPlatform();
            if (nonNull(modelPlatform) && !modelPlatform.isEmpty()) {
                infoBuilder.platform(modelPlatform);
            }
            ProtocolStringList versions = modelMetadataResponse.getVersionsList();
            if (nonNull(versions) && !versions.isEmpty()) {
                infoBuilder.version(versions.get(versions.size() - 1));
            }

            modelMetadataResponse.getInputsList().forEach(tensor -> infoBuilder.addInputDescriptor(
                    TensorDescriptor.builder(tensor.getName(), tensor.getDatatype(), tensor.getShapeList()).build()));
            modelMetadataResponse.getOutputsList().forEach(tensor -> infoBuilder.addOutputDescriptor(
                    TensorDescriptor.builder(tensor.getName(), tensor.getDatatype(), tensor.getShapeList()).build()));

            modelInfo = Optional.of(infoBuilder.build());
        } catch (StatusRuntimeException | IllegalArgumentException e) {
            throw new KuraIOException(e, "Cannot get info for " + modelName + " from server");
        }
        return modelInfo;

    }

    @Override
    public boolean isEngineReady() throws KuraIOException {
        boolean isAlive = false;

        ServerLiveRequest serverLiveRequest = ServerLiveRequest.getDefaultInstance();
        try {
            ServerLiveResponse serverLiveResponse = this.grpcStub.serverLive(serverLiveRequest);
            isAlive = serverLiveResponse.getLive();
        } catch (StatusRuntimeException e) {
            throw new KuraIOException(e, "Cannot get the status of the server");
        }
        return isAlive;
    }

    @Override
    public List<Tensor> infer(ModelInfo modelInfo, List<Tensor> inputData) throws KuraIOException {
        List<Tensor> inferenceResults = new ArrayList<>();

        ModelInferRequest.Builder inferRequest = ModelInferRequest.newBuilder();
        inferRequest.setModelName(modelInfo.getName());
        try {
            if (!modelInfo.getParameters().isEmpty()) {
                inferRequest.putAllParameters(getInferParameters(modelInfo.getParameters()));
            }
            inputData.forEach(input -> inferRequest.addInputs(createInputDataBuilder(input)));
            modelInfo.getOutputs().forEach(
                    outputDescriptor -> inferRequest.addOutputs(createRequestedOutputBuilder(outputDescriptor)));

            ModelInferResponse inferResponse = this.grpcStub.modelInfer(inferRequest.build());

            inferenceResults = createOutputInferenceData(inferResponse);
        } catch (StatusRuntimeException | IllegalArgumentException e) {
            logger.warn("Cannot infer outputs for " + modelInfo.getName() + " model", e);
        }
        return inferenceResults;

    }

    private Map<String, InferParameter> getInferParameters(Map<String, Object> parameters) {
        Map<String, InferParameter> inferParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            InferParameter.Builder parameterBuilder = InferParameter.newBuilder();
            if (value instanceof Boolean) {
                parameterBuilder.setBoolParam((Boolean) value);
            } else if (value instanceof Long) {
                parameterBuilder.setInt64Param((long) value);
            } else if (value instanceof String) {
                parameterBuilder.setStringParam((String) value);
            } else {
                throw new IllegalArgumentException("Parameter value type " + value.getClass()
                        + " not valid. Only string, long and boolean are allowed.");
            }
            inferParameters.put(key, parameterBuilder.build());
        });
        return inferParameters;
    }

    private Map<String, Object> getParameters(Map<String, InferParameter> inferParameters) {
        Map<String, Object> parameters = new HashMap<>();
        inferParameters.forEach((key, value) -> {
            switch (value.getParameterChoiceCase()) {
            case BOOL_PARAM:
                parameters.put(key, value.getBoolParam());
                break;
            case INT64_PARAM:
                parameters.put(key, value.getInt64Param());
                break;
            case STRING_PARAM:
                parameters.put(key, value.getStringParam());
                break;
            default:
                throw new IllegalArgumentException("Parameter value type unrecognized.");
            }
        });
        return parameters;
    }

    private ModelInferRequest.InferRequestedOutputTensor.Builder createRequestedOutputBuilder(
            TensorDescriptor outputDescriptor) {
        ModelInferRequest.InferRequestedOutputTensor.Builder output = ModelInferRequest.InferRequestedOutputTensor
                .newBuilder();
        output.setName(outputDescriptor.getName());
        if (!outputDescriptor.getParameters().isEmpty()) {
            output.putAllParameters(getInferParameters(outputDescriptor.getParameters()));
        }
        return output;
    }

    private ModelInferRequest.InferInputTensor.Builder createInputDataBuilder(Tensor input) {
        InferTensorContents.Builder inputDataBuilder = InferTensorContents.newBuilder();
        DataType modelInputType = DataType.valueOf(input.getDescriptor().getType());
        switch (modelInputType) {
        case BOOL:
            addBoolInputData(input, inputDataBuilder);
            break;
        case UINT8:
            addUint8InputData(input, inputDataBuilder);
            break;
        case INT8:
            addInt8InputData(input, inputDataBuilder);
            break;
        case UINT16:
            addUint16InputData(input, inputDataBuilder);
            break;
        case INT16:
            addInt16InputData(input, inputDataBuilder);
            break;
        case UINT32:
            addUint32InputData(input, inputDataBuilder);
            break;
        case INT32:
            addInt32InputData(input, inputDataBuilder);
            break;
        case UINT64:
            addUint64InputData(input, inputDataBuilder);
            break;
        case INT64:
            addInt64InputData(input, inputDataBuilder);
            break;
        case FP32:
            addFp32InputData(input, inputDataBuilder);
            break;
        case FP64:
            addFp64InputData(input, inputDataBuilder);
            break;
        case BYTES:
            addBytesInputData(input, inputDataBuilder);
            break;
        default:
            throw new IllegalArgumentException("Date type " + modelInputType + " not supported");
        }

        ModelInferRequest.InferInputTensor.Builder inputBuilder = ModelInferRequest.InferInputTensor.newBuilder();
        inputBuilder.setName(input.getDescriptor().getName());
        inputBuilder.setDatatype(input.getDescriptor().getType());
        input.getDescriptor().getShape().forEach(inputBuilder::addShape);
        inputBuilder.setContents(inputDataBuilder);
        if (!input.getDescriptor().getParameters().isEmpty()) {
            inputBuilder.putAllParameters(getInferParameters(input.getDescriptor().getParameters()));
        }
        return inputBuilder;
    }

    private void addBytesInputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Byte.class)) {
            Optional<List<Byte>> inputData = input.getData(Byte.class);
            inputData.ifPresent(bytes -> {
                byte[] byteArray = new byte[bytes.size()];
                for (int i = 0; i < bytes.size(); i++) {
                    byteArray[i] = bytes.get(i);
                }
                inputDataBuilder.addBytesContents(ByteString.copyFrom(byteArray));
            });
        } else {
            throw new IllegalArgumentException("Expected a list of bytes but got a list of " + input.getType());
        }
    }

    private void addFp64InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Double.class)) {
            Optional<List<Double>> inputData = input.getData(Double.class);
            inputData.ifPresent(inputDataBuilder::addAllFp64Contents);
        } else {
            throw new IllegalArgumentException("Expected a list of doubles but got a list of " + input.getType());
        }
    }

    private void addFp32InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Float.class)) {
            Optional<List<Float>> inputData = input.getData(Float.class);
            inputData.ifPresent(inputDataBuilder::addAllFp32Contents);
        } else {
            throw new IllegalArgumentException("Expected a list of floats but got a list of " + input.getType());
        }
    }

    private void addInt64InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Long.class)) {
            Optional<List<Long>> inputData = input.getData(Long.class);
            inputData.ifPresent(inputDataBuilder::addAllInt64Contents);
        } else {
            throw new IllegalArgumentException("Expected a list of longs but got a list of " + input.getType());
        }
    }

    private void addUint64InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Long.class)) {
            Optional<List<Long>> inputData = input.getData(Long.class);
            inputData.ifPresent(inputDataBuilder::addAllUint64Contents);
        } else {
            throw new IllegalArgumentException("Expected a list of longs but got a list of " + input.getType());
        }
    }

    private void addInt32InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Integer.class)) {
            Optional<List<Integer>> inputData = input.getData(Integer.class);
            inputData.ifPresent(inputDataBuilder::addAllIntContents);
        } else {
            throw new IllegalArgumentException("Expected a list of integers but got a list of " + input.getType());
        }
    }

    private void addUint32InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Integer.class)) {
            Optional<List<Integer>> inputData = input.getData(Integer.class);
            inputData.ifPresent(inputDataBuilder::addAllUintContents);
        } else {
            throw new IllegalArgumentException("Expected a list of integers but got a list of " + input.getType());
        }
    }

    private void addInt16InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Short.class)) {
            Optional<List<Short>> inputData = input.getData(Short.class);
            inputData.ifPresent(shortList -> {
                List<Integer> integerList = new ArrayList<>();
                shortList.forEach(shortValue -> integerList.add(shortValue.intValue()));
                inputDataBuilder.addAllIntContents(integerList);
            });
        } else if (input.getType().isAssignableFrom(Integer.class)) {
            Optional<List<Integer>> inputData = input.getData(Integer.class);
            inputData.ifPresent(inputDataBuilder::addAllIntContents);
        } else {
            throw new IllegalArgumentException(
                    "Expected a list of shorts or integers but got a list of " + input.getType());
        }
    }

    private void addUint16InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Short.class)) {
            Optional<List<Short>> inputData = input.getData(Short.class);
            inputData.ifPresent(shortList -> {
                List<Integer> integerList = new ArrayList<>();
                shortList.forEach(shortValue -> integerList.add(shortValue.intValue()));
                inputDataBuilder.addAllUintContents(integerList);
            });
        } else if (input.getType().isAssignableFrom(Integer.class)) {
            Optional<List<Integer>> inputData = input.getData(Integer.class);
            inputData.ifPresent(inputDataBuilder::addAllUintContents);
        } else {
            throw new IllegalArgumentException(
                    "Expected a list of shorts or integers but got a list of " + input.getType());
        }
    }

    private void addInt8InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Byte.class)) {
            Optional<List<Byte>> inputData = input.getData(Byte.class);
            inputData.ifPresent(byteList -> {
                List<Integer> integerList = new ArrayList<>();
                byteList.forEach(byteValue -> integerList.add(byteValue.intValue()));
                inputDataBuilder.addAllIntContents(integerList);
            });
        } else if (input.getType().isAssignableFrom(Integer.class)) {
            Optional<List<Integer>> inputData = input.getData(Integer.class);
            inputData.ifPresent(inputDataBuilder::addAllIntContents);
        } else {
            throw new IllegalArgumentException(
                    "Expected a list of bytes or integers but got a list of " + input.getType());
        }
    }

    private void addUint8InputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Byte.class)) {
            Optional<List<Byte>> inputData = input.getData(Byte.class);
            inputData.ifPresent(byteList -> {
                List<Integer> integerList = new ArrayList<>();
                byteList.forEach(byteValue -> integerList.add(byteValue.intValue()));
                inputDataBuilder.addAllUintContents(integerList);
            });
        } else if (input.getType().isAssignableFrom(Integer.class)) {
            Optional<List<Integer>> inputData = input.getData(Integer.class);
            inputData.ifPresent(inputDataBuilder::addAllUintContents);
        } else {
            throw new IllegalArgumentException(
                    "Expected a list of bytes or integers but got a list of " + input.getType());
        }
    }

    private void addBoolInputData(Tensor input, InferTensorContents.Builder inputDataBuilder) {
        if (input.getType().isAssignableFrom(Boolean.class)) {
            Optional<List<Boolean>> inputData = input.getData(Boolean.class);
            inputData.ifPresent(inputDataBuilder::addAllBoolContents);
        } else {
            throw new IllegalArgumentException("Expected a list of booleans but got a list of " + input.getType());
        }
    }

    private List<Tensor> createOutputInferenceData(ModelInferResponse inferResponse) {
        List<Tensor> results = new ArrayList<>();
        for (int index = 0; index < inferResponse.getOutputsCount(); index++) {
            InferOutputTensor inferOutputTensor = inferResponse.getOutputs(index);
            ByteString byteStringResponse = inferResponse.getRawOutputContentsList().get(index);
            if (nonNull(inferOutputTensor)) {
                DataType outputType = DataType.valueOf(inferOutputTensor.getDatatype());
                TensorDescriptorBuilder outputDescriptorBuilder = TensorDescriptor.builder(inferOutputTensor.getName(),
                        outputType.toString(), inferOutputTensor.getShapeList());
                if (!inferOutputTensor.getParametersMap().isEmpty()) {
                    getParameters(inferOutputTensor.getParametersMap()).forEach(outputDescriptorBuilder::addParameter);
                }
                TensorDescriptor outputDescriptor = outputDescriptorBuilder.build();

                switch (outputType) {
                case BOOL:
                    List<Boolean> booleanList = bufferToBooleanList(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN));
                    results.add(new Tensor(Boolean.class, outputDescriptor, booleanList));
                    break;
                case UINT8:
                case INT8:
                    List<Byte> byteList = bufferToByteList(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN));
                    results.add(new Tensor(Byte.class, outputDescriptor, byteList));
                    break;
                case UINT16:
                case INT16:
                    List<Short> shortList = bufferToShortList(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer());
                    results.add(new Tensor(Short.class, outputDescriptor, shortList));
                    break;
                case UINT32:
                case INT32:
                    List<Integer> integerList = bufferToIntList(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer());
                    results.add(new Tensor(Integer.class, outputDescriptor, integerList));
                    break;
                case UINT64:
                case INT64:
                    List<Long> longList = bufferToLongList(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asLongBuffer());
                    results.add(new Tensor(Long.class, outputDescriptor, longList));
                    break;
                case FP32:
                    List<Float> floatList = bufferToFloatList(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer());
                    results.add(new Tensor(Float.class, outputDescriptor, floatList));
                    break;
                case FP64:
                    List<Double> doubleList = bufferToDoubleList(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer());
                    results.add(new Tensor(Double.class, outputDescriptor, doubleList));
                    break;
                case BYTES:
                    List<Byte> bytesList = bufferToBytes(
                            byteStringResponse.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN));
                    results.add(new Tensor(Byte.class, outputDescriptor, bytesList));
                    break;
                default:
                    throw new IllegalArgumentException("Date type " + outputType + " not supported");
                }
            }
        }

        return results;
    }

    private List<Byte> bufferToBytes(ByteBuffer buffer) {
        List<Byte> byteList = new ArrayList<>();
        if (buffer.capacity() < 4) {
            throw new IllegalArgumentException("Too few bytes in buffer; cannot read array length");
        }
        buffer.getInt();
        while (buffer.hasRemaining()) {
            byteList.add(buffer.get());
        }
        return byteList;
    }

    private List<Float> bufferToFloatList(FloatBuffer buffer) {
        List<Float> floatList = new ArrayList<>();
        float[] floatArray;
        if (buffer.hasArray()) {
            if (buffer.arrayOffset() == 0) {
                floatArray = buffer.array();
            } else {
                floatArray = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.array().length);
            }
        } else {
            buffer.rewind();
            floatArray = new float[buffer.remaining()];
            buffer.get(floatArray);
        }
        for (float floatValue : floatArray) {
            floatList.add(floatValue);
        }
        return floatList;
    }

    private List<Double> bufferToDoubleList(DoubleBuffer buffer) {
        List<Double> doubleList = new ArrayList<>();
        double[] doubleArray;
        if (buffer.hasArray()) {
            if (buffer.arrayOffset() == 0) {
                doubleArray = buffer.array();
            } else {
                doubleArray = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.array().length);
            }
        } else {
            buffer.rewind();
            doubleArray = new double[buffer.remaining()];
            buffer.get(doubleArray);
        }
        for (double doubleValue : doubleArray) {
            doubleList.add(doubleValue);
        }
        return doubleList;
    }

    private List<Long> bufferToLongList(LongBuffer buffer) {
        List<Long> longList = new ArrayList<>();
        long[] longArray;
        if (buffer.hasArray()) {
            if (buffer.arrayOffset() == 0) {
                longArray = buffer.array();
            } else {
                longArray = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.array().length);
            }
        } else {
            buffer.rewind();
            longArray = new long[buffer.remaining()];
            buffer.get(longArray);
        }
        for (long longValue : longArray) {
            longList.add(longValue);
        }
        return longList;
    }

    private List<Integer> bufferToIntList(IntBuffer buffer) {
        List<Integer> intList = new ArrayList<>();
        int[] intArray;
        if (buffer.hasArray()) {
            if (buffer.arrayOffset() == 0) {
                intArray = buffer.array();
            } else {
                intArray = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.array().length);
            }
        } else {
            buffer.rewind();
            intArray = new int[buffer.remaining()];
            buffer.get(intArray);
        }
        for (int intValue : intArray) {
            intList.add(intValue);
        }
        return intList;
    }

    private List<Short> bufferToShortList(ShortBuffer buffer) {
        List<Short> shortList = new ArrayList<>();
        short[] shortArray;
        if (buffer.hasArray()) {
            if (buffer.arrayOffset() == 0) {
                shortArray = buffer.array();
            } else {
                shortArray = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.array().length);
            }
        } else {
            buffer.rewind();
            shortArray = new short[buffer.remaining()];
            buffer.get(shortArray);
        }
        for (short shortValue : shortArray) {
            shortList.add(shortValue);
        }
        return shortList;
    }

    private List<Byte> bufferToByteList(ByteBuffer buffer) {
        List<Byte> byteList = new ArrayList<>();
        byte[] byteArray;
        if (buffer.hasArray()) {
            if (buffer.arrayOffset() == 0) {
                byteArray = buffer.array();
            } else {
                byteArray = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.array().length);
            }
        } else {
            buffer.rewind();
            byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);
        }
        for (byte byteValue : byteArray) {
            byteList.add(byteValue);
        }
        return byteList;
    }

    private List<Boolean> bufferToBooleanList(ByteBuffer buffer) {
        List<Boolean> booleanList = new ArrayList<>();
        byte[] byteArray;
        if (buffer.hasArray()) {
            if (buffer.arrayOffset() == 0) {
                byteArray = buffer.array();
            } else {
                byteArray = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.array().length);
            }
        } else {
            buffer.rewind();
            byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);
        }
        for (byte byteValue : byteArray) {
            booleanList.add(byteValue == 0x01);
        }
        return booleanList;
    }

}
