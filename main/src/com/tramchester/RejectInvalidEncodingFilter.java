package com.tramchester;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.Utf8Appendable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RejectInvalidEncodingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RejectInvalidEncodingFilter.class);

    public static final String ENCODING_NAME = UrlEncoded.ENCODING.displayName();

    public RejectInvalidEncodingFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        final String query = httpServletRequest.getQueryString();

        if (query!=null) {
            try {
                final MultiMap<String> queryParameters = new MultiMap<>();
                UrlEncoded.decodeTo(query, queryParameters, UrlEncoded.ENCODING);
                queryParameters.clear();
            } catch (Utf8Appendable.NotUtf8Exception unableToDecode) {
                logger.warn("Unable to decode a query to " + ENCODING_NAME + " from " + httpServletRequest.getRemoteAddr());
                final HttpServletResponse servletResponse = (HttpServletResponse) response;
                servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        chain.doFilter(request, response);
    }

}
