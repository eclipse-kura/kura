package org.eclipse.kura.annotation;

import org.osgi.service.component.annotations.ComponentPropertyType;

@ComponentPropertyType
public @interface ServicePid {

    String value() default "";

// found in component xml of <implementation class="org.eclipse.kura.core.linux.executor.privileged.PrivilegedExecutorServiceImpl"/>
//    not sure what stis should be
}
