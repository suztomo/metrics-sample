package com.example.metricssample.translate;

import com.google.api.gax.tracing.ApiTracer;
import com.google.api.gax.tracing.OpenTelemetryMetricsFactory;
import com.google.api.gax.tracing.SpanName;
import io.opentelemetry.api.OpenTelemetry;

public class TranslateOpenTelemetryMetricsFactory extends OpenTelemetryMetricsFactory {

    public static final String METER_NAME = "java-translate";

    public TranslateOpenTelemetryMetricsFactory(OpenTelemetry openTelemetry) {
        super(openTelemetry, METER_NAME, "2.21.0");
    }

    @Override
    public ApiTracer newTracer(ApiTracer parent, SpanName spanName, OperationType operationType) {
        return new TranslateOpenTelemetryMetricsTracer(meter, spanName);
    }
}
