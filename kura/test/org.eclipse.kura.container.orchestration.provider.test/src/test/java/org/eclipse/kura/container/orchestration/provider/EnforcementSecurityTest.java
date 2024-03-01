package org.eclipse.kura.container.orchestration.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerState;
import org.eclipse.kura.container.orchestration.ImageConfiguration;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.eclipse.kura.container.orchestration.provider.impl.ContainerOrchestrationServiceImpl;
import org.eclipse.kura.container.orchestration.provider.impl.enforcement.AllowlistEnforcementMonitor;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.Image;

public class EnforcementSecurityTest {

    private static final String IMAGE_NAME = "nginx";
    private static final String CONTAINER_NAME = "frank";
    private static final String CONTAINER_ID = "1d3dewf34r5";

    private static final String REGISTRY_URL = "https://test";
    private static final String REGISTRY_USERNAME = "test";
    private static final String REGISTRY_PASSWORD = "test1";

    private static final String EMPTY_ALLOWLIST_CONTENT = "";
    private static final String FILLED_ALLOWLIST_CONTENT_NO_SPACE = "sha256:f9d633ff6640178c2d0525017174a688e2c1aef28f0a0130b26bd5554491f0da,sha256:c26ae7472d624ba1fafd296e73cecc4f93f853088e6a9c13c0d52f6ca5865107";
    private static final String FILLED_ALLOWLIST_CONTENT_WITH_SPACES = " sha256:f9d633ff6640178c2d0525017174a688e2c1aef28f0a0130b26bd5554491f0da , sha256:c26ae7472d624ba1fafd296e73cecc4f93f853088e6a9c13c0d52f6ca5865107";

    private static final String CORRECT_DIGEST = "ubuntu@sha256:c26ae7472d624ba1fafd296e73cecc4f93f853088e6a9c13c0d52f6ca5865107";
    private static final String WRONG_DIGEST = "ubuntu@sha256:0000000000000000000000000000000000000000000000000000000000000000";

    private AllowlistEnforcementMonitor allowlistEnforcementMonitor;
    private ContainerOrchestrationServiceImpl mockedContainerOrchImpl;

    ContainerConfiguration containerConfig;

    private Map<String, Object> properties = new HashMap<>();

    public EnforcementSecurityTest() {
        this.properties.clear();
    }

    @Test
    public void shouldAllowStartingWithCorrectAllowlistContent() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationService();
        givenMockedDockerClient(new String[] { CORRECT_DIGEST });
        givenAllowlistEnforcement(FILLED_ALLOWLIST_CONTENT_NO_SPACE);

        whenOnNext();

        thenContainerDigestIsVerified();
    }

    @Test
    public void shouldAllowStartingWithCorrectAllowlistContentWithSpaces() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationService();
        givenMockedDockerClient(new String[] { CORRECT_DIGEST });
        givenAllowlistEnforcement(FILLED_ALLOWLIST_CONTENT_WITH_SPACES);

        whenOnNext();

        thenContainerDigestIsVerified();
    }

    @Test
    public void shouldNotAllowStartingWithEmptyAllowlistContent() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationService();
        givenMockedDockerClient(new String[] { CORRECT_DIGEST });
        givenAllowlistEnforcement(EMPTY_ALLOWLIST_CONTENT);

        whenOnNext();

        thenContainerDigestIsNotVerifiedAndStopped();
    }

    @Test
    public void shouldNotAllowStartingWithWrongContainerDigest() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationService();
        givenMockedDockerClient(new String[] { WRONG_DIGEST });
        givenAllowlistEnforcement(FILLED_ALLOWLIST_CONTENT_NO_SPACE);

        whenOnNext();

        thenContainerDigestIsNotVerifiedAndStopped();
    }

    /*
     * Given
     */

    ContainerInstanceDescriptor containerInstanceDescriptor;

    private void givenMockedContainerOrchestrationService() throws KuraException, InterruptedException {
        this.mockedContainerOrchImpl = spy(new ContainerOrchestrationServiceImpl());

        containerInstanceDescriptor = ContainerInstanceDescriptor.builder().setContainerID(CONTAINER_ID)
                .setContainerName(CONTAINER_NAME).setContainerImage(IMAGE_NAME).setContainerState(ContainerState.ACTIVE)
                .build();
        List<ContainerInstanceDescriptor> containerDescriptors = new ArrayList<>();
        containerDescriptors.add(containerInstanceDescriptor);

        ImageConfiguration imageConfig = new ImageConfiguration.ImageConfigurationBuilder().setImageName(IMAGE_NAME)
                .setImageTag("latest").setImageDownloadTimeoutSeconds(0)
                .setRegistryCredentials(Optional.of(new PasswordRegistryCredentials(Optional.of(REGISTRY_URL),
                        REGISTRY_USERNAME, new Password(REGISTRY_PASSWORD))))
                .build();

        this.containerConfig = ContainerConfiguration.builder().setContainerName(CONTAINER_NAME)
                .setImageConfiguration(imageConfig).setVolumes(Collections.singletonMap("test", "~/test/test"))
                .setDeviceList(Arrays.asList("/dev/gpio1", "/dev/gpio2"))
                .setEnvVars(Arrays.asList("test=test", "test2=test2")).build();

        doReturn(containerDescriptors).when(this.mockedContainerOrchImpl).listContainerDescriptors();

        doNothing().when(this.mockedContainerOrchImpl).pullImage(any(ImageConfiguration.class));
    }

    private void givenMockedDockerClient(String[] digestsList) {
        DockerClient mockedDockerClient = mock(DockerClient.class, Mockito.RETURNS_DEEP_STUBS);
        List<Image> images = new LinkedList<>();
        Image mockImage = mock(Image.class);

        when(mockImage.getRepoTags()).thenReturn(new String[] { IMAGE_NAME, "latest", "nginx:latest" });
        when(mockImage.getRepoDigests()).thenReturn(digestsList);
        when(mockImage.getId()).thenReturn(IMAGE_NAME);
        images.add(mockImage);

        when(mockedDockerClient.listImagesCmd()).thenReturn(mock(ListImagesCmd.class));
        when(mockedDockerClient.listImagesCmd().withImageNameFilter(anyString())).thenReturn(mock(ListImagesCmd.class));
        when(mockedDockerClient.listImagesCmd().withImageNameFilter(anyString()).exec()).thenReturn(images);
        when(mockedDockerClient.stopContainerCmd(anyString())).thenReturn(mock(StopContainerCmd.class));
        when(mockedDockerClient.stopContainerCmd(anyString()).exec()).thenAnswer(answer -> {
            this.containerInstanceDescriptor = ContainerInstanceDescriptor.builder().setContainerID(CONTAINER_ID)
                    .setContainerName(CONTAINER_NAME).setContainerImage(IMAGE_NAME)
                    .setContainerState(ContainerState.FAILED).build();
            return null;
        });

        this.mockedContainerOrchImpl.setDockerClient(mockedDockerClient);
    }

    private void givenAllowlistEnforcement(String rawAllowlistContent) {
        // List<String> allowlistContent = Arrays
        // .asList(rawAllowlistContent.replaceAll("\\s", "").replace("\n", "").trim().split(","));
        this.allowlistEnforcementMonitor = new AllowlistEnforcementMonitor(rawAllowlistContent,
                this.mockedContainerOrchImpl);
    }

    /*
     * When
     */

    private void whenOnNext() {
        this.allowlistEnforcementMonitor.onNext(new Event("start", CONTAINER_ID, "nginx:latest", 1708963202L));
    }

    /*
     * Then
     */

    private void thenContainerDigestIsVerified() {

        assertEquals(ContainerState.ACTIVE, this.containerInstanceDescriptor.getContainerState());

    }

    private void thenContainerDigestIsNotVerifiedAndStopped() {
        assertEquals(ContainerState.FAILED, this.containerInstanceDescriptor.getContainerState());
    }
}
