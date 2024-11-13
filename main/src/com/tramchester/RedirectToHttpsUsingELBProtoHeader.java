package com.tramchester;


import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class RedirectToHttpsUsingELBProtoHeader implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RedirectToHttpsUsingELBProtoHeader.class);
    public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    private final String secureHost;

    // no cert for these hosts, need first to redirect to secure domain
    private final List<String> unsecureHosts = Arrays.asList("trambuster.com",
            "trambuster.info",
            "trambuster.co.uk",
            "tramchester.co.uk",
            "tramchester.info");

    public RedirectToHttpsUsingELBProtoHeader(TramchesterConfig config) {
        secureHost = config.getSecureHost();
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        final String xForwardHeader = httpServletRequest.getHeader(X_FORWARDED_PROTO); // https is terminated by the ELB
        final String dangerousRawURL = httpServletRequest.getRequestURL().toString();

        final URL url;
        try {
            url = new URI(dangerousRawURL).toURL();
        } catch(URISyntaxException unableToParse) {
            // not logging url here, unsafe
            logger.error("Unable to parse the URL", unableToParse);
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {

            if (xForwardHeader != null) {
                if ("http".equalsIgnoreCase(xForwardHeader)) {

                    logger.info("Found http in " + X_FORWARDED_PROTO+ " need to redirect to https for " + url.getHost());

                    //final URL location = mapUrl(url);

                    if (isValid(url)) {
                        httpServletResponse.sendRedirect(url.toExternalForm());
                    } else {
                        logger.warn("Unrecognised host, respond with bad gateway for " + url.getHost());
                        httpServletResponse.sendError(HttpServletResponse.SC_BAD_GATEWAY);
                    }

                    return;
                }
            }
        }

        catch (MalformedURLException malformedURLException) {
            logger.error("Unable to map URL, send server error. Url: " + url.getHost(), malformedURLException);
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isValid(URL location) {
        String host = location.getHost();
        return host.endsWith(secureHost) || "localhost".equals(host);
    }

    public URL mapUrl(URL originalURL) throws MalformedURLException {
        // Note: only called on initial redirection
        logger.debug("Mapping url host:"+originalURL.getHost() + "  path:'" + originalURL.getFile() +"'");

        //String result = "https"+ originalURL.substring(4);

        final String host = originalURL.getHost();
        String redirectHost = host;

        for (String unsecure: unsecureHosts) {
            if (host.toLowerCase().endsWith(unsecure)) {
                redirectHost = host.replaceFirst(unsecure, secureHost);
                break;
            }
        }

        URL newUrl = new URL("https", redirectHost, originalURL.getPort(), originalURL.getFile());

        if (!host.equals(redirectHost)) {
            logger.info(format("Mapped URL '%s' '%s' '%s' to %s", originalURL.getHost(), originalURL.getPort(),
                    originalURL.getFile(), newUrl));
        } else {
            logger.info("No mapping of host, change to https for '" + originalURL.getHost() + "' '" +  originalURL.getPort(),
                    originalURL.getFile() + "'");
        }

        return newUrl;


    }

}
