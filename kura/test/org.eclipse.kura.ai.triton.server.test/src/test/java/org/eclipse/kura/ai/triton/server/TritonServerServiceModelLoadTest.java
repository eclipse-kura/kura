package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.junit.Rule;
import org.junit.Test;

import inference.GRPCInferenceServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

public class TritonServerServiceModelLoadTest {

    TritonServerServiceImpl tritonServer;
    boolean exceptionCaught = false;
    boolean methodCalled = false;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Test
    public void shouldNotLoadModel() throws KuraException, IOException {
        givenTritonServerServiceImpl();

        whenLoadModel();

        thenModelExceptionIsCaught();
    }

    @Test
    public void shouldLoadModel() throws KuraException, IOException {
        givenTritonServerServiceImpl();

        whenLoadModel();

        thenModelIsLoaded();
    }

    @Test
    public void shouldNotUnloadModel() throws KuraException, IOException {
        givenTritonServerServiceImpl();

        whenUnloadModel();

        thenModelExceptionIsCaught();
    }

    @Test
    public void shouldNotGetModelLoadState() throws KuraException, IOException {
        givenTritonServerServiceImpl();

        whenGetModelLoadState();

        thenModelExceptionIsCaught();
    }

    private void givenTritonServerServiceImpl() throws IOException {
        this.exceptionCaught = false;
        this.methodCalled = true;
        this.tritonServer = new TritonServerServiceImpl();
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.address", "localhost");
        properties.put("server.ports", "4000,4001,4002");
        properties.put("enable.local", "false");
        this.tritonServer.activate(properties);

        GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase serviceImpl = mock(
                GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase.class,
                delegatesTo(new GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase() {

                    @Override
                    public void repositoryModelLoad(inference.GrpcService.RepositoryModelLoadRequest request,
                            io.grpc.stub.StreamObserver<inference.GrpcService.RepositoryModelLoadResponse> responseObserver) {
                        TritonServerServiceModelLoadTest.this.methodCalled = true;
                        responseObserver.onNext(inference.GrpcService.RepositoryModelLoadResponse.getDefaultInstance());
                        responseObserver.onCompleted();
                    }

                }));

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
        this.tritonServer.setGrpcStub(GRPCInferenceServiceGrpc.newBlockingStub(channel));
    }

    private void whenLoadModel() throws KuraIOException {
        try {
            this.tritonServer.loadModel("myModel", Optional.empty());
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenGetModelLoadState() throws KuraIOException {
        try {
            this.tritonServer.isModelLoaded("myModel");
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenUnloadModel() throws KuraIOException {
        try {
            this.tritonServer.unloadModel("myModel");
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    private void thenModelExceptionIsCaught() {
        assertTrue(this.exceptionCaught);
    }

    private void thenModelIsLoaded() {
        assertTrue(this.methodCalled);
    }

}
