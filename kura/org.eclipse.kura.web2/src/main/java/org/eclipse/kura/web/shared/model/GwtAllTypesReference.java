/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

/**
 * This is a dummy type for referencing all necessary types
 * <br>
 * The GWT compiler only creates serialization profiles for types
 * which are actually referenced in a service. This type has no
 * functionality other than to reference all requires types which
 * the GWT needs to pick up.
 * <br>
 * For example {@link GwtCloudConnectionState}, it is used on
 * {@link GwtCloudConnectionEntry}, which is only referenced
 * as a generic list type. So the GWT compiler does not pick it up.
 * Adding this enum to this type triggers the GWT compiler to pick
 * it up.
 * <br>
 * So in order to add types which did not get picked up by the GWT
 * compiler automatically you need to:
 * <ul>
 * <li>Add a field in this class</li>
 * <li>Add a method returning this class the interface of the service which
 * requires this type</li>
 * <li>Provide a dummy implementation on the service implementation
 * which return null</li> 
 * </ul>
 * 
 * @see http://stackoverflow.com/a/992745/222044
 */
public class GwtAllTypesReference implements Serializable {

    private static final long serialVersionUID = 1L;

    GwtCloudConnectionState cloudConnectionState;
}
