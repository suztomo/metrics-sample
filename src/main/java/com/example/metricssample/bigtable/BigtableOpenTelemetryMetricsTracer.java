package com.example.metricssample.bigtable;

import com.google.api.gax.tracing.OpenTelemetryMetricsTracer;
import com.google.api.gax.tracing.SpanName;
import io.opentelemetry.api.metrics.Meter;

public class BigtableOpenTelemetryMetricsTracer extends OpenTelemetryMetricsTracer {
    public BigtableOpenTelemetryMetricsTracer(Meter meter, SpanName spanName) {
        super(meter, spanName);
    }

    @Override
    public String attemptLatencyName() {
        //it was cloud.google.com/java/bigtable with OpenCensus, but slashes are not allowed anymore in OpenTelemetry.
        // However, it will be overridden by view name(which allows slashes) if views are configured.
        return "cloud.google.com-java-bigtable";
    }
}
