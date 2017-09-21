package org.eclipse.kura.download.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.deployment.hook.DeploymentHookManager;
import org.eclipse.kura.core.deployment.hook.DeploymentHookManager.HookAssociation;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.deployment.hook.DeploymentHook;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DownloadHookManagerTest {

    private ServiceReference<DeploymentHook> mockHookReference(String kuraServicePid, DeploymentHook deploymentHook) {
        @SuppressWarnings("unchecked")
        final ServiceReference<DeploymentHook> reference = mock(ServiceReference.class);
        when(reference.getProperty(anyString())).then(invocation -> {
            String arg = invocation.getArgumentAt(0, String.class);
            if (ConfigurationService.KURA_SERVICE_PID.equals(arg)) {
                return kuraServicePid;
            }
            return deploymentHook;
        });
        return reference;
    }

    private DeploymentHookManager getDeploymentHookManager() throws NoSuchFieldException {
        final DeploymentHookManager result = new DeploymentHookManager();
        final BundleContext mockBundleContext = mock(BundleContext.class);
        when(mockBundleContext.getService(anyObject())).then(invocation -> {
            final ServiceReference<?> ref = invocation.getArgumentAt(0, ServiceReference.class);
            return ref.getProperty("service");
        });
        TestUtil.setFieldValue(result, "bundleContext", mockBundleContext);
        return result;
    }

    private Properties parseAssociations(String associations) throws IOException {
        final Properties properties = new Properties();
        properties.load(new StringReader(associations));
        return properties;
    }

    @Test
    public void shouldRegisterAssociations() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.updateAssociations(parseAssociations("type1=pid1\ntype2=pid2\ntype3=pid3\n#bad_type=bad_pid"));

        final Map<String, HookAssociation> associations = manager.getHookAssociations();

        assertEquals(3, associations.size());

        assertEquals(true, associations.containsKey("type1"));
        assertEquals(true, associations.containsKey("type2"));
        assertEquals(true, associations.containsKey("type3"));
        assertEquals(false, associations.containsKey("bad_type"));

        assertEquals("pid1", associations.get("type1").getHookId());
        assertEquals("pid2", associations.get("type2").getHookId());
        assertEquals("pid3", associations.get("type3").getHookId());

        assertTrue(manager.getHook("type1") == null);
        assertTrue(manager.getHook("type2") == null);
        assertTrue(manager.getHook("type3") == null);
    }

    @Test
    public void shouldClearAssociations() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.updateAssociations(null);

        final Map<String, HookAssociation> associations = manager.getHookAssociations();
        assertEquals(0, associations.size());
    }

    @Test
    public void shouldRegisterHooks() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.bindHook(mockHookReference("pid1", mock(DeploymentHook.class)));
        manager.bindHook(mockHookReference("pid2", mock(DeploymentHook.class)));
        manager.bindHook(mockHookReference("pid3", mock(DeploymentHook.class)));

        final Map<String, DeploymentHook> registeredHooks = manager.getRegisteredHooks();
        assertEquals(3, registeredHooks.size());

        assertTrue(registeredHooks.containsKey("pid1"));
        assertTrue(registeredHooks.containsKey("pid2"));
        assertTrue(registeredHooks.containsKey("pid3"));

    }

    @Test
    public void shouldUnregisterHooks() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.bindHook(mockHookReference("pid1", mock(DeploymentHook.class)));
        manager.bindHook(mockHookReference("pid2", mock(DeploymentHook.class)));
        manager.bindHook(mockHookReference("pid3", mock(DeploymentHook.class)));

        manager.unbindHook(mockHookReference("pid2", mock(DeploymentHook.class)));

        final Map<String, DeploymentHook> registeredHooks = manager.getRegisteredHooks();
        assertEquals(2, registeredHooks.size());

        assertTrue(registeredHooks.containsKey("pid1"));
        assertFalse(registeredHooks.containsKey("pid2"));
        assertTrue(registeredHooks.containsKey("pid3"));
    }

    @Test
    public void shouldBindHooks() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();

        final ServiceReference<DeploymentHook> ref1 = mockHookReference("pid1", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref2 = mockHookReference("pid2", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref3 = mockHookReference("pid3", mock(DeploymentHook.class));

        manager.bindHook(ref1);
        manager.bindHook(ref2);
        manager.bindHook(ref3);

        manager.updateAssociations(parseAssociations("type1=pid1\ntype2=pid2\ntype3=pid3\n#bad_type=bad_pid"));

        final Map<String, HookAssociation> associations = manager.getHookAssociations();
        assertEquals(3, associations.size());

        for (HookAssociation association : associations.values()) {
            assertEquals(true, association.getDeploymentHook() != null);
            ServiceReference<DeploymentHook> ref = null;
            if (association.getHookId().equals("pid1")) {
                ref = ref1;
            } else if (association.getHookId().equals("pid2")) {
                ref = ref2;
            } else if (association.getHookId().equals("pid3")) {
                ref = ref3;
            }
            assertEquals(true, ref.getProperty("service") == association.getDeploymentHook());
        }

        assertTrue(manager.getHook("type1") == ref1.getProperty("service"));
        assertTrue(manager.getHook("type2") == ref2.getProperty("service"));
        assertTrue(manager.getHook("type3") == ref3.getProperty("service"));
    }

    @Test
    public void shouldBindHooks2() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();

        final ServiceReference<DeploymentHook> ref1 = mockHookReference("pid1", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref2 = mockHookReference("pid2", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref3 = mockHookReference("pid3", mock(DeploymentHook.class));

        manager.updateAssociations(parseAssociations("type1=pid1\ntype2=pid2\ntype3=pid3\n#bad_type=bad_pid"));

        manager.bindHook(ref1);
        manager.bindHook(ref2);
        manager.bindHook(ref3);

        final Map<String, HookAssociation> associations = manager.getHookAssociations();
        assertEquals(3, associations.size());

        for (HookAssociation association : associations.values()) {
            assertEquals(true, association.getDeploymentHook() != null);
            ServiceReference<DeploymentHook> ref = null;
            if (association.getHookId().equals("pid1")) {
                ref = ref1;
            } else if (association.getHookId().equals("pid2")) {
                ref = ref2;
            } else if (association.getHookId().equals("pid3")) {
                ref = ref3;
            }
            assertEquals(true, ref.getProperty("service") == association.getDeploymentHook());
        }
        assertTrue(manager.getHook("type1") == ref1.getProperty("service"));
        assertTrue(manager.getHook("type2") == ref2.getProperty("service"));
        assertTrue(manager.getHook("type3") == ref3.getProperty("service"));
    }

    @Test
    public void shouldUnbindHooks() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();

        final ServiceReference<DeploymentHook> ref1 = mockHookReference("pid1", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref2 = mockHookReference("pid2", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref3 = mockHookReference("pid3", mock(DeploymentHook.class));

        manager.bindHook(ref1);
        manager.bindHook(ref2);
        manager.bindHook(ref3);

        manager.updateAssociations(parseAssociations("type1=pid1\ntype2=pid2\ntype3=pid3\n#bad_type=bad_pid"));

        manager.unbindHook(ref2);

        final Map<String, HookAssociation> associations = manager.getHookAssociations();
        assertEquals(3, associations.size());

        assertTrue(associations.get("type2") != null);
        assertTrue(associations.get("type2").getDeploymentHook() == null);
        assertTrue(manager.getHook("type2") == null);
    }

    @Test
    public void shouldUnbindHooks2() throws KuraException, IOException, NoSuchFieldException {
        final DeploymentHookManager manager = getDeploymentHookManager();

        final ServiceReference<DeploymentHook> ref1 = mockHookReference("pid1", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref2 = mockHookReference("pid2", mock(DeploymentHook.class));
        final ServiceReference<DeploymentHook> ref3 = mockHookReference("pid3", mock(DeploymentHook.class));

        manager.bindHook(ref1);
        manager.bindHook(ref2);
        manager.bindHook(ref3);

        manager.unbindHook(ref2);

        manager.updateAssociations(parseAssociations("type1=pid1\ntype2=pid2\ntype3=pid3\n#bad_type=bad_pid"));

        final Map<String, HookAssociation> associations = manager.getHookAssociations();
        assertEquals(3, associations.size());

        assertTrue(associations.get("type2") != null);
        assertTrue(associations.get("type2").getDeploymentHook() == null);
        assertTrue(manager.getHook("type2") == null);
    }
}
