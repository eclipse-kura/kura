package org.eclipse.kura.ai.triton.server;

import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraIOException;
import org.junit.Rule;
import org.junit.Test;

import inference.GRPCInferenceServiceGrpc;
import inference.GRPCInferenceServiceGrpc.GRPCInferenceServiceBlockingStub;
import inference.GrpcService;
import io.grpc.ManagedChannel;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

public class TritonServerServiceModelLoadTest {

    TritonServerServiceImpl tritonServer;
    boolean exceptionCaught = false;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Test
    public void shouldNotLoadModel() throws KuraIOException {
        givenTritonServerServiceImpl();

        whenLoadModel();

        thenModelExceptionIsCaught();
    }

    @Test
    public void shouldNotUnloadModel() throws KuraIOException {
        givenTritonServerServiceImpl();

        whenUnloadModel();

        thenModelExceptionIsCaught();
    }

    @Test
    public void shouldNotGetModelLoadState() throws KuraIOException {
        givenTritonServerServiceImpl();

        whenGetModelLoadState();

        thenModelExceptionIsCaught();
    }

    private void givenTritonServerServiceImpl() {
        this.exceptionCaught = false;
        this.tritonServer = new TritonServerServiceImpl();
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.address", "localhost");
        properties.put("server.ports", "4000,4001,4002");
        properties.put("enable.local", "false");
        this.tritonServer.activate(properties);

        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

        // Create a HelloWorldClient using the in-process channel;
        GRPCInferenceServiceBlockingStub grpcStub = GRPCInferenceServiceGrpc.newBlockingStub(channel);
        this.tritonServer.setGrpcStub(grpcStub);
        // client = new HelloWorldClient(channel);
    }

    ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder("myservice");
    GrpcService.RepositoryModelLoadResponse
    
    @Override
    public inference.GrpcService.RepositoryModelLoadResponse repositoryModelLoad(
            inference.GrpcService.RepositoryModelLoadRequest request) {
        return null;
    }

    // private final GrpcService.RepositoryModelLoadResponse serviceImpl = mock(
    // GrpcService.RepositoryModelLoadResponse.class, delegatesTo(new GrpcService.RepositoryModelLoadResponse() {
    // // By default the client will receive Status.UNIMPLEMENTED for all RPCs.
    // // You might need to implement necessary behaviors for your test here, like this:
    // //
    // // @Override
    // // public void sayHello(HelloRequest request, StreamObserver<HelloReply> respObserver) {
    // // respObserver.onNext(HelloReply.getDefaultInstance());
    // // respObserver.onCompleted();
    // // }
    // }));

    private void whenLoadModel() throws KuraIOException {
        try {
            this.tritonServer.loadModel("myModel", Optional.empty());
        } catch (KuraIOException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenGetModelLoadState() throws KuraIOException {
        try {
            this.tritonServer.isModelLoaded("myModel");
        } catch (KuraIOException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenUnloadModel() throws KuraIOException {
        try {
            this.tritonServer.unloadModel("myModel");
        } catch (KuraIOException e) {
            this.exceptionCaught = true;
        }
    }

    private void thenModelExceptionIsCaught() {
        assertTrue(exceptionCaught);
    }

}
