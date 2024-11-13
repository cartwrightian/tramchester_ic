package com.tramchester;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.tramchester.RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO;
import static java.lang.String.format;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;

public class RedirectToAppFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RedirectToAppFilter.class);
    public static final String ELB_HEALTH_CHECKER = "ELB-HealthChecker";

    public RedirectToAppFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String userAgent = httpServletRequest.getHeader("User-Agent");
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        if (userAgent==null) {
            logger.warn("Got null user agent, request from " + request.getRemoteAddr());
        } else if (userAgent.startsWith(ELB_HEALTH_CHECKER)) {
            logger.debug("Response OK to " + ELB_HEALTH_CHECKER);
            servletResponse.setStatus(SC_OK);
            return;
        }

        final String unsafeOriginal = httpServletRequest.getRequestURL().toString().toLowerCase();
        final URL url = new URL(unsafeOriginal);

        if (url.getPath().equals("/")) {
            boolean forwardedSecure = isForwardedSecure(httpServletRequest);
            String redirection = getRedirectURL(url, forwardedSecure);

            logger.debug(format("Redirect from %s to %s", url.getHost(), redirection));
            servletResponse.sendRedirect(redirection);
            return;
        }

        chain.doFilter(request, response);
    }

    @NotNull
    private String getRedirectURL(final URL url, final boolean forwardedSecure) throws MalformedURLException {
        final String redirection;
        final String protocol = url.getProtocol().toLowerCase();

        if (forwardedSecure && "http".equals(protocol)) {
            final URL secureURL = new URL(url.toString().replace(protocol, "https"));
            redirection = secureURL + "app";
        } else {
            redirection = url + "app";
        }
        return redirection;
    }

    private boolean isForwardedSecure(final HttpServletRequest httpServletRequest) {
        final String header = httpServletRequest.getHeader(X_FORWARDED_PROTO); // https is terminated by the ELB
        return "https".equalsIgnoreCase(header);
    }

}
