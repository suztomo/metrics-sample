package com.example.metricssample.bigtable;

import com.google.api.gax.tracing.ApiTracer;
import com.google.api.gax.tracing.ClientMetricsTracer;
import com.google.api.gax.tracing.OpenTelemetryTracerFactory;
import com.google.api.gax.tracing.SpanName;
import io.opentelemetry.api.OpenTelemetry;

public class BigtableOpenTelemetryMetricsTracerFactory extends OpenTelemetryTracerFactory {

    public static final String METER_NAME = "java-bigtable";

    public BigtableOpenTelemetryMetricsTracerFactory(OpenTelemetry openTelemetry) {
        super(openTelemetry, METER_NAME, "2.31.1");
    }

    @Override
    public ApiTracer newTracer(ApiTracer parent, SpanName spanName, OperationType operationType) {
        return new BigtableOpenTelemetryApiMetricsTracer(meter, spanName);
    }

    @Override
    public ClientMetricsTracer newClientMetricsTracer() {
        return new BigtableOpenTelemetryClientMetricsTracer(meter);
    }
}
