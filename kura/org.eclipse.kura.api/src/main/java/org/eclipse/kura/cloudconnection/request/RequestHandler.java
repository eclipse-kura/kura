/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.request;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Implemented by services that want to implement remote resource management.
 *
 * <ul>
 * <li>{@link RequestHandler#doGet} is used to implement a READ request for a resource identified in the supplied
 * {@link KuraMessage)}</li>
 * <li>{@link RequestHandler#doPut} is used to implement a CREATE or UPDATE request for a resource identified in the
 * supplied {@link KuraMessage}</li>
 * <li>{@link RequestHandler#doDel} is used to implement a DELETE request for a resource identified in the supplied
 * {@link KuraMessage}</li>
 * <li>{@link RequestHandler#doPost} is used to implement other operations on a resource identified in the supplied
 * {@link KuraMessage}</li>
 * <li>{@link RequestHandler#doExec} is used to perform application operation not necessary tied to a given
 * resource.</li>
 * </ul>
 * 
 * Every request is also associated to a {@link RequestHandlerContext} that specifies the request context and can be
 * used to send notification messages, keeping the link with the original cloud stack that started the interaction.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ConsumerType
public interface RequestHandler {

    /**
     * Used to implement a READ request for a resource identified by the supplied {@link CloudletResources)}
     *
     * @param context
     *            a request context that can be used, for example, to publish notification messages to the remote cloud
     *            platform
     * @param reqMessage
     *            represents, as a {@link KuraMessage}, the received message
     * @return the response to be provided back as {@link KuraMessage}
     * @throws KuraException
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    public default KuraMessage doGet(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    /**
     * Used to implement a CREATE or UPDATE request for a resource identified by the supplied {@link CloudletResources)}
     *
     * @param context
     *            a request context that can be used, for example, to publish notification messages to the remote cloud
     *            platform
     * @param reqMessage
     *            represents as a {@link KuraMessage} the received message
     * @return the response to be provided back as {@link KuraMessage}
     * @throws KuraException
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    public default KuraMessage doPut(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    /**
     * Used to implement other operations on a resource identified by the supplied {@link CloudletResources)}
     *
     * @param context
     *            a request context that can be used, for example, to publish notification messages to the remote cloud
     *            platform
     * @param reqMessage
     *            represents as a {@link KuraMessage} the received message
     * @return the response to be provided back as {@link KuraMessage}
     * @throws KuraException
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    public default KuraMessage doPost(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    /**
     * Used to implement a DELETE request for a resource identified by the supplied {@link CloudletResources)}
     *
     * @param context
     *            a request context that can be used, for example, to publish notification messages to the remote cloud
     *            platform
     * @param reqMessage
     *            represents as a {@link KuraMessage} the received message
     * @return the response to be provided back as {@link KuraMessage}
     * @throws KuraException
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    public default KuraMessage doDel(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    /**
     * Used to perform application operation not necessary tied to a given resource
     *
     * @param context
     *            a request context that can be used, for example, to publish notification messages to the remote cloud
     *            platform
     * @param reqMessage
     *            represents as a {@link KuraMessage} the received message
     * @return the response to be provided back as {@link KuraMessage}
     * @throws KuraException
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    public default KuraMessage doExec(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

}
