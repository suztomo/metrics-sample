package com.example.metricssample.bigtable;

import com.google.api.gax.tracing.OpenTelemetryClientMetricsTracer;
import io.opentelemetry.api.metrics.Meter;

public class BigtableOpenTelemetryClientMetricsTracer extends OpenTelemetryClientMetricsTracer {
    public BigtableOpenTelemetryClientMetricsTracer(Meter meter) {
        super(meter);
    }

    @Override
    public String channelSizeName() {
        return "cloud.google.com.java.bigtable." + super.channelSizeName();
    }
}
