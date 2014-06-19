/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Date;

import org.eclipse.kura.web.client.util.DateUtils;
import org.eclipse.kura.web.client.util.MessageUtils;

import com.extjs.gxt.ui.client.data.BaseModel;

public class GwtSnapshot extends BaseModel implements Serializable 
{
	private static final long serialVersionUID = 204571826084819719L;

	
	public GwtSnapshot()
	{}
	
    @Override
    @SuppressWarnings({"unchecked"})
    public <X> X get(String property) {
    	if ("createdOnFormatted".equals(property)) {
    		if (((Date) get("createdOn")).getTime() == 0) {
    			return (X) (MessageUtils.get("snapSeeded"));
    		}    		
    		return (X) (DateUtils.formatDateTime((Date) get("createdOn")));
    	}
    	else if ("snapshotId".equals(property)) {
    		return (X) new Long(((Date) get("createdOn")).getTime());
    	}
    	else {
    		return super.get(property);
    	}
    }
	
    public Date getCreatedOn() {
        return (Date) get("createdOn");
    }

    public long getSnapshotId() {
        return ((Date) get("createdOn")).getTime();
    }

    public String getCreatedOnFormatted() {
        return DateUtils.formatDateTime((Date) get("createdOn"));
    }    

    public void setCreatedOn(Date createdOn) {
        set("createdOn", createdOn);
    }
}
