package org.eclipse.kura.web.server.servlet;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.locale.LocaleContextHolder;

public class LocaleServlet extends HttpServlet {

    private static final long serialVersionUID = -1489531064222389767L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String localeName = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null)
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("GWT_LOCALE")) {
                    localeName = cookie.getValue();
                    break;
                }
            }

        if (localeName == null || localeName.equals(""))
            localeName = System.getProperty("osgi.nl");
        Locale locale = null;
        if (localeName != null)
            locale = new Locale(localeName);
        else
            locale = req.getLocale();
        try {
            LocaleContextHolder.setLocale(locale);
            super.service(req, resp);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
