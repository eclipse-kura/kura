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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.ai.inference.ModelInfo;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
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

    protected static final String TRITON_IMAGE_NAME = "tritonserver";
    protected static final String TRITON_IMAGE_TAG = "latest";

    protected TritonServerServiceAbs tritonServerService;
    protected boolean methodCalled;
    protected boolean exceptionCaught;
    protected Optional<ModelInfo> modelInfo;

    private List<String> tritonModelRepoStub;

    private Command startTritonServerCmd = new Command(new String[] { "tritonserver",
            "--model-repository=/fake-repository-path", "--backend-directory=/fake-backends-path", "--http-port=4001",
            "--grpc-port=4002", "--metrics-port=4003", "--model-control-mode=explicit", "2>&1", "|", "systemd-cat",
            "-t tritonserver", "-p info" });

    public TritonServerServiceStepDefinitions() {
        this.startTritonServerCmd.setExecuteInAShell(true);
        this.tritonModelRepoStub = Arrays.asList("myModel");
        this.modelInfo = Optional.empty();
    }

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    protected List<String> modelsFound = new ArrayList<>();
    private List<Tensor> tensorList = new ArrayList<>();
    private boolean isEngineReady;
    private CommandExecutorService ces;
    private CryptoService cry;
    private ContainerOrchestrationService orc;

    protected void givenTritonServerServiceImpl(Map<String, Object> properties) throws IOException {
        this.tritonServerService = createTritonServerServiceImpl(properties, tritonModelRepoStub, true);
    }

    protected void givenTritonServerServiceRemoteImpl(Map<String, Object> properties) throws IOException {
        this.tritonServerService = createTritonServerServiceRemoteImpl(properties, tritonModelRepoStub, true);
    }

    protected void givenTritonServerServiceNativeImpl(Map<String, Object> properties) throws IOException {
        this.tritonServerService = createTritonServerServiceNativeImpl(properties, tritonModelRepoStub, true);
    }

    protected void givenTritonServerServiceContainerImpl(Map<String, Object> properties) throws IOException {
        this.tritonServerService = createTritonServerServiceContainerImpl(properties, tritonModelRepoStub, true);
    }

    protected void givenTritonServerServiceImplNotActive() throws IOException {
        this.tritonServerService = createTritonServerServiceImpl(null, tritonModelRepoStub, false);
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
        this.isEngineReady = this.tritonServerService.isEngineReady();
    }

    protected void whenTritonServerIsActivated(Map<String, Object> properties) {
        try {
            this.tritonServerService.activate(properties);
        } catch (KuraRuntimeException kre) {
            this.exceptionCaught = true;
        }
    }

    protected void whenDeactivateIsInvokedOnTritonServer() {
        try {
            this.tritonServerService.deactivate();
        } catch (KuraRuntimeException kre) {
            this.exceptionCaught = true;
        }
    }

    protected void whenUpdatedIsInvokedOnTritonServer(Map<String, Object> properties) {
        try {
            this.tritonServerService.updated(properties);
        } catch (KuraRuntimeException kre) {
            this.exceptionCaught = true;
        }
    }

    protected void thenExceptionIsCaught() {
        assertTrue(this.exceptionCaught);
    }

    protected void thenNoExceptionIsCaught() {
        assertFalse(this.exceptionCaught);
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

    protected void thenAfterWaiting(long millisecondsToWait) throws InterruptedException {
        Thread.sleep(millisecondsToWait);
    }

    @SuppressWarnings("unchecked")
    protected void thenTritonStartServerCommandIsExecuted() {
        verify(this.ces).execute(eq(this.startTritonServerCmd), any(Consumer.class));
    }

    protected Map<String, Object> defaultProperties() {

        Map<String, Object> properties = new HashMap<>();

        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", Boolean.FALSE);

        return properties;
    }

    protected Map<String, Object> updatedProperties() {

        Map<String, Object> properties = new HashMap<>();

        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4001, 4002, 4003 });
        properties.put("enable.local", Boolean.FALSE);

        return properties;
    }

    protected Map<String, Object> invalidProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("server.ports", new Integer[] { 4000, 4001 });
        properties.put("enable.local", Boolean.FALSE);

        return properties;
    }

    protected Map<String, Object> enableLocalServerProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("server.ports", new Integer[] { 4001, 4002, 4003 });
        properties.put("enable.local", Boolean.TRUE);
        properties.put("local.backends.path", "/fake-backends-path");
        properties.put("local.model.repository.path", "/fake-repository-path");

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

    private TritonServerServiceAbs createTritonServerServiceImpl(Map<String, Object> properties,
            List<String> tritonModelRepoStub, boolean activate) throws IOException {

        TritonServerServiceAbs tritonServerServiceImpl = new TritonServerServiceOrigImpl();

        this.ces = mock(CommandExecutorService.class);
        when(ces.isRunning(new String[] { "tritonserver" })).thenReturn(false);

        tritonServerServiceImpl.setCommandExecutorService(ces);

        this.cry = mock(CryptoService.class);
        tritonServerServiceImpl.setCryptoService(cry);

        if (activate) {
            tritonServerServiceImpl.activate(properties);
        }

        GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase serviceImpl = createGRPCMock(tritonModelRepoStub);

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
        tritonServerServiceImpl.setGrpcStub(GRPCInferenceServiceGrpc.newBlockingStub(channel));

        return tritonServerServiceImpl;
    }

    private TritonServerServiceAbs createTritonServerServiceNativeImpl(Map<String, Object> properties,
            List<String> tritonModelRepoStub, boolean activate) throws IOException {

        TritonServerServiceAbs tritonServerServiceImpl = new TritonServerServiceNativeImpl();

        this.ces = mock(CommandExecutorService.class);
        when(ces.isRunning(new String[] { "tritonserver" })).thenReturn(false);

        tritonServerServiceImpl.setCommandExecutorService(ces);

        this.cry = mock(CryptoService.class);
        tritonServerServiceImpl.setCryptoService(cry);

        if (activate) {
            tritonServerServiceImpl.activate(properties);
        }

        GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase serviceImpl = createGRPCMock(tritonModelRepoStub);

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
        tritonServerServiceImpl.setGrpcStub(GRPCInferenceServiceGrpc.newBlockingStub(channel));

        return tritonServerServiceImpl;
    }

    private TritonServerServiceAbs createTritonServerServiceContainerImpl(Map<String, Object> properties,
            List<String> tritonModelRepoStub, boolean activate) throws IOException {

        TritonServerServiceAbs tritonServerServiceImpl = new TritonServerServiceContainerImpl();

        this.orc = mock(ContainerOrchestrationService.class);
        setTritonDockerImageAsAvailable();
        setTritonDockerContainerAsNotRunning();
        tritonServerServiceImpl.setContainerOrchestrationService(orc);

        this.cry = mock(CryptoService.class);
        tritonServerServiceImpl.setCryptoService(cry);

        if (activate) {
            tritonServerServiceImpl.activate(properties);
        }

        GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase serviceImpl = createGRPCMock(tritonModelRepoStub);

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
        tritonServerServiceImpl.setGrpcStub(GRPCInferenceServiceGrpc.newBlockingStub(channel));

        return tritonServerServiceImpl;
    }

    private void setTritonDockerImageAsAvailable() {
        ImageInstanceDescriptor imageDescriptor = mock(ImageInstanceDescriptor.class);
        when(imageDescriptor.getImageName()).thenReturn(TRITON_IMAGE_NAME);
        when(imageDescriptor.getImageTag()).thenReturn(TRITON_IMAGE_TAG);
        when(this.orc.listImageInstanceDescriptors()).thenReturn(Arrays.asList(imageDescriptor));
    }

    private void setTritonDockerContainerAsNotRunning() {
        when(this.orc.listImageInstanceDescriptors()).thenReturn(Arrays.asList());
    }

    private TritonServerServiceAbs createTritonServerServiceRemoteImpl(Map<String, Object> properties,
            List<String> tritonModelRepoStub, boolean activate) throws IOException {

        TritonServerServiceAbs tritonServerServiceImpl = new TritonServerServiceRemoteImpl();

        this.ces = mock(CommandExecutorService.class);
        tritonServerServiceImpl.setCommandExecutorService(ces);

        this.cry = mock(CryptoService.class);
        tritonServerServiceImpl.setCryptoService(cry);

        if (activate) {
            tritonServerServiceImpl.activate(properties);
        }

        GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase serviceImpl = createGRPCMock(tritonModelRepoStub);

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
        tritonServerServiceImpl.setGrpcStub(GRPCInferenceServiceGrpc.newBlockingStub(channel));

        return tritonServerServiceImpl;
    }

    private GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase createGRPCMock(List<String> tritonModelRepoStub) {
        return mock(GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase.class,
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
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("FP64").setName("name1")
                                .setContents(InferTensorContents.newBuilder().addFp64Contents(34.76d).build())
                                .addShape(0).setShape(0, 0).build());
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("BOOL").setName("name2")
                                .setContents(InferTensorContents.newBuilder().addBoolContents(true).build()).addShape(1)
                                .setShape(0, 0).build());
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("BYTES").setName("name3")
                                .setContents(InferTensorContents.newBuilder()
                                        .addBytesContents(ByteString.copyFrom(new byte[] { 10, 20, -10, -20 })).build())
                                .addShape(0).setShape(0, 0).build());
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("FP32").setName("name4")
                                .setContents(InferTensorContents.newBuilder().addFp32Contents(134.76f).build())
                                .addShape(1).setShape(0, 0).build());
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("INT64").setName("name5")
                                .setContents(InferTensorContents.newBuilder().addInt64Contents(56436l).build())
                                .addShape(0).setShape(0, 0).build());
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("INT32").setName("name6")
                                .setContents(InferTensorContents.newBuilder().addIntContents(45465).build()).addShape(0)
                                .setShape(0, 0).build());
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("UINT64").setName("name7")
                                .setContents(InferTensorContents.newBuilder().addUint64Contents(536456l).build())
                                .addShape(1).setShape(0, 0).build());
                        outputTensor.add(InferOutputTensor.newBuilder().setDatatype("UINT32").setName("name8")
                                .setContents(InferTensorContents.newBuilder().addUintContents(53645).build())
                                .addShape(0).setShape(0, 0).build());

                        List<ByteString> rawOutputTensor = new ArrayList<>();
                        rawOutputTensor.add(ByteString.copyFrom(convertDoubleToByteArray(34.76d)));
                        rawOutputTensor.add(ByteString.copyFrom(convertBooleanToByteArray(true)));
                        rawOutputTensor.add(ByteString.copyFrom(new byte[] { 10, 20, -10, -20 }));
                        rawOutputTensor.add(ByteString.copyFrom(convertFloatToByteArray(134.76f)));
                        rawOutputTensor.add(ByteString.copyFrom(convertLongToByteArray(56436l)));
                        rawOutputTensor.add(ByteString.copyFrom(convertIntegerToByteArray(45465)));
                        rawOutputTensor.add(ByteString.copyFrom(convertLongToByteArray(536456l)));
                        rawOutputTensor.add(ByteString.copyFrom(convertIntegerToByteArray(53645)));

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
    }

    @Before
    public void resetStatus() {
        this.exceptionCaught = false;
        this.methodCalled = false;
        this.modelsFound.clear();
        this.modelInfo = Optional.empty();
        this.tensorList.clear();
        this.isEngineReady = false;

        if (this.tritonServerService != null) {
            this.tritonServerService.deactivate();
        }
    }

    private byte[] convertDoubleToByteArray(Double value) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES);
        byteBuffer.putDouble(value);
        return byteBuffer.array();
    }

    private byte[] convertFloatToByteArray(Float value) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES);
        byteBuffer.putFloat(value);
        return byteBuffer.array();
    }

    // private byte[] convertByteToByteArray(Byte value) {
    //
    // ByteBuffer byteBuffer = ByteBuffer.allocate(Byte.BYTES);
    // byteBuffer.put(value);
    // byte[] a = byteBuffer.array();
    // return a;
    // }

    private byte[] convertBooleanToByteArray(Boolean value) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        byteBuffer.put(value ? (byte) 1 : (byte) 0);
        return byteBuffer.array();
    }

    private byte[] convertLongToByteArray(Long value) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.putLong(value);
        return byteBuffer.array();
    }

    private byte[] convertIntegerToByteArray(Integer value) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        byteBuffer.putInt(value);
        return byteBuffer.array();
    }
}
