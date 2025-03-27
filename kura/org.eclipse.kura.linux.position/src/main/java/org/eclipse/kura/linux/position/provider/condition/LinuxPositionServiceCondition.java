package org.eclipse.kura.linux.position.provider.condition;

import org.eclipse.kura.linux.position.provider.LinuxPositionProviderConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.condition.Condition;

@Component(property = Condition.CONDITION_ID + "=" + LinuxPositionProviderConstants.CONDITION_ID)
public class LinuxPositionServiceCondition implements Condition {
}
