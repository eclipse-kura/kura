package org.eclipse.kura.web.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

public class HttpSessionTracker implements HttpSessionListener, HttpSessionAttributeListener, HttpSessionIdListener {

    private static final Logger logger = LoggerFactory.getLogger(HttpSessionTracker.class);

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String s) {
        logger.info("event: {}, id: {}", event, s);

    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        logger.info("added event: {}", event);
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        logger.info("removed event: {}", event);
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        logger.info("replaced event: {}", event);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        logger.info("session created event: {}", se);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        logger.info("session destroyed event: {}", se);
    }

}
