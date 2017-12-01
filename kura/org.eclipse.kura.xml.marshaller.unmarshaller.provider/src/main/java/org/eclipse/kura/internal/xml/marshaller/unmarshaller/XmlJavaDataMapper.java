/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.xml.marshaller.unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface XmlJavaDataMapper {

    static final Logger s_logger = LoggerFactory.getLogger(XmlJavaDataMapper.class);
    static final String ESF_NAMESPACE = "esf";
    static final String OCD_NAMESPACE = "ocd";

    public abstract Element marshal(Document doc, Object o) throws Exception;

    public abstract <T> T unmarshal(Document doc) throws Exception;
}
