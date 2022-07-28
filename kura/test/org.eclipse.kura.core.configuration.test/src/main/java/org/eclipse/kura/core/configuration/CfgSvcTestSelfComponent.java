package org.eclipse.kura.core.configuration;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;

public class CfgSvcTestSelfComponent implements SelfConfiguringComponent {

    public static final String PID = "org.eclipse.kura.core.configuration.CfgSvcTestSelfComponent";

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        try {
            Map<String, Object> componentConfigurationProperties = new HashMap<>();
            componentConfigurationProperties.put(KURA_SERVICE_PID, PID);
            componentConfigurationProperties.put(SERVICE_PID, PID);
            return new ComponentConfigurationImpl(PID, getDefinition(), componentConfigurationProperties);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private Tocd getDefinition() throws KuraException {

        ObjectFactory objectFactory = new ObjectFactory();
        Tocd tocd = objectFactory.createTocd();

        tocd.setName("CfgSvcTestSelfComponent");
        tocd.setId(PID);
        tocd.setDescription("Self Configuring Component Test");

        Tad tad = objectFactory.createTad();
        tad.setId("TestADId");
        tad.setName("TestADName");
        tad.setType(Tscalar.STRING);
        tad.setCardinality(1);
        tad.setRequired(true);
        tad.setDefault("TestADDefaultValue");
        tad.setDescription("This is only a test parameter.");
        tocd.addAD(tad);

        return tocd;
    }

    public void updated(Map<String, Object> properties) {

    }
}
