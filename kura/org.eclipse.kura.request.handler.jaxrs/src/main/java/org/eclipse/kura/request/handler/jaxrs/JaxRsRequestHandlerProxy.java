/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.request.handler.jaxrs;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.request.handler.jaxrs.annotation.EXEC;
import org.eclipse.kura.request.handler.jaxrs.consumer.RequestArgumentHandler;
import org.eclipse.kura.request.handler.jaxrs.consumer.RequestParameterHandler;
import org.eclipse.kura.request.handler.jaxrs.consumer.ResponseBodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class JaxRsRequestHandlerProxy implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(JaxRsRequestHandlerProxy.class);

    private final Object target;
    private final Map<Endpoint, MethodProxy> endpoints;
    private final Gson gson;

    public JaxRsRequestHandlerProxy(final Object target) {
        this.gson = buildGson();
        this.target = target;
        this.endpoints = probeRequestMethods(target);
    }

    @Override
    public KuraMessage doGet(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        return dispatch(RequestHandlerMethod.GET, reqMessage);
    }

    @Override
    public KuraMessage doPost(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        return dispatch(RequestHandlerMethod.POST, reqMessage);
    }

    @Override
    public KuraMessage doPut(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        return dispatch(RequestHandlerMethod.PUT, reqMessage);
    }

    @Override
    public KuraMessage doExec(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        return dispatch(RequestHandlerMethod.EXEC, reqMessage);
    }

    @Override
    public KuraMessage doDel(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        return dispatch(RequestHandlerMethod.DELETE, reqMessage);
    }

    protected Gson buildGson() {
        return new Gson();
    }

    protected Map<Endpoint, MethodProxy> probeRequestMethods(final Object object) {

        final Map<Endpoint, MethodProxy> result = new HashMap<>();
        final Method[] methods = object.getClass().getMethods();

        for (final Method method : methods) {
            final Optional<Endpoint> endpoint = probeEndpoint(method);

            if (!endpoint.isPresent()) {
                continue;
            }

            buildMethodProxy(method).ifPresent(s -> result.put(endpoint.get(), s));
        }

        return result;
    }

    protected String resourcesToPath(final List<String> resources) {
        return "/" + resources.stream().collect(Collectors.joining("/"));
    }

    protected Optional<Endpoint> probeEndpoint(final Method method) {
        final RequestHandlerMethod requestHandlerMethod;

        final Path path = method.getAnnotation(Path.class);

        if (path == null) {
            logger.debug("{} does not specify the Path annotation, ignoring", method);
            return Optional.empty();
        }

        if (method.isAnnotationPresent(EXEC.class)) {
            logger.debug("found EXEC method: {}", method);
            requestHandlerMethod = RequestHandlerMethod.EXEC;
        } else if (method.isAnnotationPresent(GET.class)) {
            logger.debug("found GET method: {}", method);
            requestHandlerMethod = RequestHandlerMethod.GET;
        } else if (method.isAnnotationPresent(POST.class)) {
            logger.debug("found POST method: {}", method);
            requestHandlerMethod = RequestHandlerMethod.POST;
        } else if (method.isAnnotationPresent(PUT.class)) {
            logger.debug("found PUT method: {}", method);
            requestHandlerMethod = RequestHandlerMethod.PUT;
        } else if (method.isAnnotationPresent(DELETE.class)) {
            logger.debug("found DELETE method: {}", method);
            requestHandlerMethod = RequestHandlerMethod.DELETE;
        } else {
            logger.debug("{} is not a supported REST method, ignoring", method);
            return Optional.empty();
        }

        logger.debug("found method {}, path: {}, rest method: {}", method, path, requestHandlerMethod);

        return Optional.of(new Endpoint(requestHandlerMethod, path.value()));
    }

    protected Optional<MethodProxy> buildMethodProxy(final Method method) {

        final Parameter[] parameters = method.getParameters();

        final List<RequestArgumentHandler<?>> handlers = new ArrayList<>();

        for (final Parameter parameter : parameters) {

            final Class<?> type = parameter.getType();

            if (Arrays.asList(parameter.getAnnotations()).stream().map(a -> a.annotationType())
                    .anyMatch(Context.class::equals)) {
                handlers.add(RequestParameterHandlers.nullArgumentHandler());
            } else if (type == InputStream.class) {
                handlers.add(RequestParameterHandlers.inputStreamArgumentHandler());
            } else {
                handlers.add(RequestParameterHandlers.gsonArgumentHandler(type, gson));
            }

        }

        final RequestParameterHandler parameterHandler = RequestParameterHandlers.fromArgumentHandlers(handlers);

        final ResponseBodyHandler bodyHandler;

        final Class<?> returnType = method.getReturnType();

        if (returnType == Void.class) {
            bodyHandler = ResponseBodyHandlers.voidHandler();
        } else if (returnType == Response.class) {
            bodyHandler = ResponseBodyHandlers.responseHandler(gson);
        } else {
            bodyHandler = ResponseBodyHandlers.gsonHandler(gson);
        }

        return Optional.of(new MethodProxy(method, parameterHandler, bodyHandler));
    }

    protected KuraMessage dispatch(final RequestHandlerMethod restMethod, final KuraMessage request) {

        try {
            final List<String> resources = extractResources(request);

            final String path = resourcesToPath(resources);

            final MethodProxy proxy = Optional.ofNullable(this.endpoints.get(new Endpoint(restMethod, path)))
                    .orElseThrow(() -> new KuraException(KuraErrorCode.NOT_FOUND));

            return invoke(proxy, target, request);
        } catch (final Exception e) {
            return handleException(e);
        }
    }

    protected KuraMessage invoke(final MethodProxy proxy, final Object target, final KuraMessage request)
            throws KuraException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Object[] parameters = proxy.parameterHandler.buildParameters(request);

        final Object result = proxy.method.invoke(target, parameters);

        final KuraPayload resultPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        proxy.bodyHandler.buildBody(result).ifPresent(resultPayload::setBody);

        return new KuraMessage(resultPayload);
    }

    protected KuraMessage handleException(final Throwable e) {

        return DefaultExceptionHandler.toKuraMessage(DefaultExceptionHandler.toWebApplicationException(e),
                Optional.of(gson));
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractResources(final KuraMessage request) throws KuraException {

        try {
            return java.util.Objects.requireNonNull((List<String>) request.getProperties().get(ARGS_KEY.value()));
        } catch (final Exception e) {
            logger.warn("failed to get resources from request");
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

    }

    public static class Endpoint {

        private final RequestHandlerMethod method;
        private final String relativePath;

        public Endpoint(RequestHandlerMethod method, String relativePath) {
            this.method = method;
            this.relativePath = relativePath;
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, relativePath);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Endpoint other = (Endpoint) obj;
            return method == other.method && Objects.equals(relativePath, other.relativePath);
        }

    }

    public static class MethodProxy {

        private Method method;
        private RequestParameterHandler parameterHandler;
        private ResponseBodyHandler bodyHandler;

        public MethodProxy(Method method, RequestParameterHandler parameterHandler, ResponseBodyHandler bodyHandler) {
            this.method = method;
            this.parameterHandler = parameterHandler;
            this.bodyHandler = bodyHandler;
        }
    }

    public enum RequestHandlerMethod {
        GET,
        POST,
        PUT,
        DELETE,
        EXEC
    }
}
