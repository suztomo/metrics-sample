package com.example.metricssample.common;

import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.metric.MetricConfiguration;
import com.google.cloud.opentelemetry.metric.MetricDescriptorStrategy;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static com.example.metricssample.bigtable.BigtableController.PROJECT_ID;

@Configuration
public class OpenTelemetryConfigs {

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault();

        MetricExporter metricExporter = GoogleCloudMetricExporter.createWithConfiguration(
                MetricConfiguration.builder()
                        // Configure the cloud project id.  Note: this is autodiscovered by default.
                        .setProjectId(PROJECT_ID)
                        .setPrefix("custom.googleapis.com")
                        // Configure a strategy for how/when to configure metric descriptors.
                        .setDescriptorStrategy(MetricDescriptorStrategy.SEND_ONCE)
                        .build());
        PeriodicMetricReader metricReader = PeriodicMetricReader.builder(metricExporter).setInterval(Duration.ofSeconds(20)).build();

        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setMeterProvider(sdkMeterProvider)
                .build();
    }
}
