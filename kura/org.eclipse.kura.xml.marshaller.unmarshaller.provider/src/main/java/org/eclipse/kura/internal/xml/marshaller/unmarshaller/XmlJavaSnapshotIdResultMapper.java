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

import java.util.List;

import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaSnapshotIdResultMapper implements XmlJavaDataMapper {

    private static final String SNAPSHOT_IDS = "snapshot-ids";
    private static final String SNAPSHOTIDS = "snapshotIds";

    @Override
    public Element marshal(Document doc, Object object) {
        Element snapshotIDs = doc.createElement(ESF_NAMESPACE + ":" + SNAPSHOT_IDS);
        snapshotIDs.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:esf", "http://eurotech.com/esf/2.0");
        // TODO: add xml schema to EUROTECH site
        snapshotIDs.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ocd",
                "http://www.osgi.org/xmlns/metatype/v1.2.0");
        doc.appendChild(snapshotIDs);

        XmlSnapshotIdResult xmlSnapshotIdResult = (XmlSnapshotIdResult) object;
        List<Long> snapshotIdVals = xmlSnapshotIdResult.getSnapshotIds();

        if (snapshotIdVals != null) {
            for (Long snapId : snapshotIdVals) {
                Element snapshotIds = doc.createElement(ESF_NAMESPACE + ":" + SNAPSHOTIDS);
                snapshotIds.setTextContent(snapId.toString());
                snapshotIDs.appendChild(snapshotIds);
            }
        }

        return snapshotIDs;
    }

    @Override
    public <T> T unmarshal(Document doc) {
        throw new IllegalArgumentException();
    }

}
