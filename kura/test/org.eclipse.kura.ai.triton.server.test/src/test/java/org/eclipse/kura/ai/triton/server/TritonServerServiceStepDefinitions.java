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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.ai.inference.ModelInfo;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.junit.Before;
import org.junit.Rule;

import com.google.protobuf.ByteString;

import inference.GRPCInferenceServiceGrpc;
import inference.GrpcService.InferTensorContents;
import inference.GrpcService.ModelInferRequest;
import inference.GrpcService.ModelInferResponse;
import inference.GrpcService.ModelInferResponse.InferOutputTensor;
import inference.GrpcService.ModelMetadataRequest;
import inference.GrpcService.ModelMetadataResponse;
import inference.GrpcService.ModelMetadataResponse.TensorMetadata;
import inference.GrpcService.RepositoryIndexRequest;
import inference.GrpcService.RepositoryIndexResponse;
import inference.GrpcService.RepositoryIndexResponse.ModelIndex;
import inference.GrpcService.RepositoryModelUnloadRequest;
import inference.GrpcService.RepositoryModelUnloadResponse;
import inference.GrpcService.ServerLiveRequest;
import inference.GrpcService.ServerLiveResponse;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

public abstract class TritonServerServiceStepDefinitions {

    protected TritonServerServiceImpl tritonServerService;
    protected boolean methodCalled;
    protected boolean exceptionCaught;
    protected Optional<ModelInfo> modelInfo = Optional.empty();

    private List<String> tritonModelRepoStub = Arrays.asList("myModel");

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    protected List<String> modelsFound = new ArrayList<>();
    private List<Tensor> tensorList = new ArrayList<>();
    private boolean isEngineReady;

    protected void givenTritonServerServiceImpl(Map<String, Object> properties) throws IOException {
        this.tritonServerService = createTritonServerServiceImpl(properties, tritonModelRepoStub);
    }

    protected void whenLoadModel(String modelName) throws KuraIOException {
        try {
            this.tritonServerService.loadModel(modelName, Optional.empty());
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    protected void whenGetModelLoadState(String modelName) throws KuraIOException {
        try {
            this.tritonServerService.isModelLoaded(modelName);
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    protected void whenUnloadModel(String modelName) throws KuraIOException {
        try {
            this.tritonServerService.unloadModel(modelName);
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    protected void whenGetModelNames() {
        try {
            this.modelsFound = this.tritonServerService.getModelNames();
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    protected void whenGetModelInfo(String modelName) {
        try {
            this.modelInfo = this.tritonServerService.getModelInfo(modelName);
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    protected void whenInferData(ModelInfo modelInfo, List<Tensor> inputData) {
        try {
            this.tensorList = this.tritonServerService.infer(modelInfo, inputData);
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    protected void whenAskingIfEngineIsReady() {
        try {
            this.isEngineReady = this.tritonServerService.isEngineReady();
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    protected void thenExceptionIsCaught() {
        assertTrue(this.exceptionCaught);
    }

    protected void thenModelIsLoaded() {
        assertFalse(this.exceptionCaught);
        assertTrue(this.methodCalled);
    }

    protected void thenModelIsUnLoaded() {
        assertFalse(this.exceptionCaught);
        assertTrue(this.methodCalled);
    }

    protected void thenListIsNotEmpty() {
        assertFalse(this.exceptionCaught);
        assertTrue(this.methodCalled);
        assertFalse(this.modelsFound.isEmpty());
    }

    protected void thenModelInfoExists() {
        assertFalse(this.exceptionCaught);
        assertTrue(this.methodCalled);
        assertTrue(this.modelInfo.isPresent());
    }

    protected void thenTensorsAreReturned() {
        assertFalse(this.exceptionCaught);
        assertTrue(this.methodCalled);
        assertFalse(this.tensorList.isEmpty());
    }

    protected Map<String, Object> defaultProperties() {

        Map<String, Object> properties = new HashMap<>();

        properties.put("server.address", "localhost");
        properties.put("server.ports", "4000,4001,4002");
        properties.put("enable.local", "false");

        return properties;
    }

    protected void thenEngineIsReady() {
        assertTrue(this.isEngineReady);
    }

    protected ModelInfo exampleModel() {

        List<Long> inputShape = new ArrayList<>();
        List<Long> outputShape = new ArrayList<>();
        inputShape.add(1l);
        outputShape.add(1l);

        List<TensorDescriptor> inDescs = new ArrayList<>();
        inDescs.add(new TensorDescriptor("IN_INT", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_BOOL", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_STR", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(
                new TensorDescriptor("IN_BYTEARR", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_DOUBLE", "", Optional.empty(), inputShape, new HashMap<String, Object>()));
        inDescs.add(new TensorDescriptor("IN_LONG", "", Optional.empty(), inputShape, new HashMap<String, Object>()));

        List<TensorDescriptor> outDescs = new ArrayList<>();
        outDescs.add(new TensorDescriptor("IN_INT", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(new TensorDescriptor("IN_BOOL", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(new TensorDescriptor("IN_STR", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(
                new TensorDescriptor("IN_BYTEARR", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(
                new TensorDescriptor("IN_DOUBLE", "", Optional.empty(), outputShape, new HashMap<String, Object>()));
        outDescs.add(new TensorDescriptor("IN_LONG", "", Optional.empty(), outputShape, new HashMap<String, Object>()));

        return ModelInfo.builder("name").platform("platform").version("version").addAllInputDescriptor(inDescs)
                .addAllOutputDescriptor(outDescs).build();
    }

    protected List<Tensor> exampleInputData() {
        List<Tensor> tensors = new ArrayList<>();

        List<Long> shape = new ArrayList<>();
        shape.add(1l);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param1", 1l);

        List<Double> data = Arrays.asList(3.45, 7.34, 88.887);

        tensors.add(
                new Tensor(Double.class, new TensorDescriptor("name", "FP64", Optional.empty(), shape, params), data));

        return tensors;
    }

    private TritonServerServiceImpl createTritonServerServiceImpl(Map<String, Object> properties,
            List<String> tritonModelRepoStub) throws IOException {

        TritonServerServiceImpl tritonServerServiceImpl = new TritonServerServiceImpl();

        tritonServerServiceImpl.activate(properties);

        GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase serviceImpl = mock(
                GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase.class,
                delegatesTo(new GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase() {

                    @Override
                    public void repositoryModelLoad(inference.GrpcService.RepositoryModelLoadRequest request,
                            io.grpc.stub.StreamObserver<inference.GrpcService.RepositoryModelLoadResponse> responseObserver) {
                        TritonServerServiceStepDefinitions.this.methodCalled = true;
                        if (!tritonModelRepoStub.contains(request.getModelName())) {
                            responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT));
                        } else {
                            responseObserver
                                    .onNext(inference.GrpcService.RepositoryModelLoadResponse.getDefaultInstance());
                            responseObserver.onCompleted();
                        }
                    }

                    @Override
                    public void repositoryModelUnload(RepositoryModelUnloadRequest request,
                            StreamObserver<RepositoryModelUnloadResponse> responseObserver) {
                        TritonServerServiceStepDefinitions.this.methodCalled = true;
                        if (!tritonModelRepoStub.contains(request.getModelName())) {
                            responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT));
                        } else {
                            responseObserver
                                    .onNext(inference.GrpcService.RepositoryModelUnloadResponse.getDefaultInstance());
                            responseObserver.onCompleted();
                        }
                    }

                    @Override
                    public void repositoryIndex(RepositoryIndexRequest request,
                            StreamObserver<RepositoryIndexResponse> responseObserver) {
                        TritonServerServiceStepDefinitions.this.methodCalled = true;

                        ModelIndex modelIndex = ModelIndex.newBuilder().setName("myModel").build();
                        RepositoryIndexResponse response = RepositoryIndexResponse.newBuilder().addModels(modelIndex)
                                .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void modelMetadata(ModelMetadataRequest request,
                            StreamObserver<ModelMetadataResponse> responseObserver) {

                        TritonServerServiceStepDefinitions.this.methodCalled = true;

                        ModelMetadataResponse response = ModelMetadataResponse.newBuilder().setPlatform("platform")
                                .setName("name")
                                .addInputs(TensorMetadata.newBuilder().setName("tensorName").setDatatype("UINT64")
                                        .addShape(1l).build())
                                .addOutputs(TensorMetadata.newBuilder().setName("tensorName").setDatatype("UINT64")
                                        .addShape(1l).build())
                                .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void modelInfer(ModelInferRequest request,
                            StreamObserver<ModelInferResponse> responseObserver) {

                        TritonServerServiceStepDefinitions.this.methodCalled = true;

                        List<InferOutputTensor> outputTensor = new ArrayList<>();
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("FP64").setName("name")
                                .setContents(InferTensorContents.newBuilder().addFp64Contents(34.76).build())
                                .addShape(1).setShape(0, 1).build());

                        List<ByteString> rawOutputTensor = new ArrayList<>();
                        rawOutputTensor.add(ByteString.copyFrom(convertDoubleToByteArray(34.76)));

                        ModelInferResponse response = ModelInferResponse.newBuilder().addAllOutputs(outputTensor)
                                .addAllRawOutputContents(rawOutputTensor).build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void serverLive(ServerLiveRequest request,
                            StreamObserver<ServerLiveResponse> responseObserver) {
                        TritonServerServiceStepDefinitions.this.methodCalled = true;

                        ServerLiveResponse response = ServerLiveResponse.newBuilder().setLive(true).build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }

                }));

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
        tritonServerServiceImpl.setGrpcStub(GRPCInferenceServiceGrpc.newBlockingStub(channel));

        return tritonServerServiceImpl;
    }

    @Before
    public void resetStatus() {
        this.exceptionCaught = false;
        this.methodCalled = false;
        this.modelsFound.clear();
        this.modelInfo = Optional.empty();
        this.tensorList.clear();
        this.isEngineReady = false;
    }

    private byte[] convertDoubleToByteArray(Double number) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES);

        byteBuffer.putDouble(number);

        return byteBuffer.array();

    }
}
