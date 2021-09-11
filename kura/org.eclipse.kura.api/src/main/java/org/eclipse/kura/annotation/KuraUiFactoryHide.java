package org.eclipse.kura.annotation;

import org.osgi.service.component.annotations.ComponentPropertyType;

@ComponentPropertyType
public @interface KuraUiFactoryHide {

    boolean value() default true;
}
