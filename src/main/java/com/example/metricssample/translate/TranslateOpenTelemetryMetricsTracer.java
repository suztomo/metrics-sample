package com.example.metricssample.translate;

import com.google.api.gax.tracing.OpenTelemetryMetricsTracer;
import com.google.api.gax.tracing.SpanName;
import io.opentelemetry.api.metrics.Meter;

public class TranslateOpenTelemetryMetricsTracer extends OpenTelemetryMetricsTracer {

    public TranslateOpenTelemetryMetricsTracer(Meter meter, SpanName spanName) {
        super(meter, spanName);
        this.attemptLatencyRecorder =
                meter
                        .histogramBuilder("java.translate.attempt_latency")
                        .setDescription("Duration of a translate attempt")
                        .setUnit("ms")
                        .build();
    }

}
