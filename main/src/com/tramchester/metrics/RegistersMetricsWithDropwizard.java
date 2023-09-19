package com.tramchester.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistersMetricsWithDropwizard implements RegistersMetrics {
    private static final Logger logger = LoggerFactory.getLogger(RegistersMetricsWithDropwizard.class);

    private final MetricRegistry metricRegistry;

    public RegistersMetricsWithDropwizard(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    private void registerMetricForClass(Class<?> klass, String category, String name, Gauge<Integer> method) {
        metricRegistry.register(MetricRegistry.name(klass, category, name), method);
    }

    @Override
    public void add(HasMetrics hasMetrics, String category, String name, Gauge<Integer> method) {
        registerMetricForClass(hasMetrics.getClass(), category, name, method);
    }

    public void registerMetricsFor(HasMetrics hasMetrics) {
        if (hasMetrics.areMetricsEnabled()) {
            hasMetrics.registerMetrics(this);
        }
        else {
            logger.warn(hasMetrics.getClass().getCanonicalName() + " is not enabled");
        }
    }
}
