package com.tramchester;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.tramchester.RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static java.lang.String.format;

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
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final String userAgent = httpServletRequest.getHeader("User-Agent");
        final HttpServletResponse servletResponse = (HttpServletResponse) response;
        if (userAgent==null) {
            logger.warn("Got null user agent, request from " + request.getRemoteAddr());
        } else if (userAgent.startsWith(ELB_HEALTH_CHECKER)) {
            logger.debug("Response OK to " + ELB_HEALTH_CHECKER);
            servletResponse.setStatus(SC_OK);
            return;
        }

        final String unsafeOriginal = httpServletRequest.getRequestURL().toString().toLowerCase();

        final URI originalURI;
        try {
            originalURI = new URI(unsafeOriginal);
        } catch(URISyntaxException unableToParse) {
            // not logging url here, unsafe
            logger.error("Unable to parse the URL", unableToParse);
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            if (originalURI.getPath().equals("/")) {
                final boolean forwardedSecure = isForwardedSecure(httpServletRequest);
                final String redirection = getRedirectURL(originalURI, forwardedSecure);

                logger.debug(format("Redirect from %s to %s", originalURI.getHost(), redirection));
                servletResponse.sendRedirect(redirection);
                return;
            }
        }
        catch (URISyntaxException uriSyntaxException) {
            logger.error("Unable to parse the URL", uriSyntaxException);
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        chain.doFilter(request, response);
    }

    @NotNull
    private String getRedirectURL(final URI uri, final boolean forwardedSecure) throws URISyntaxException {
        final String redirection;
        final String protocol = uri.getScheme().toLowerCase();

        if (forwardedSecure && "http".equals(protocol)) {
            final URI secureURL = new URI(uri.toString().replace(protocol, "https"));
            redirection = secureURL.toASCIIString() + "app";
        } else {
            redirection = uri.toASCIIString() + "app";
        }
        return redirection;
    }

    private boolean isForwardedSecure(final HttpServletRequest httpServletRequest) {
        final String header = httpServletRequest.getHeader(X_FORWARDED_PROTO); // https is terminated by the ELB
        return "https".equalsIgnoreCase(header);
    }

}
