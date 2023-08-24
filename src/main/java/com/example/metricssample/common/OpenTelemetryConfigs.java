package com.example.metricssample.common;

import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.metric.MetricConfiguration;
import com.google.cloud.opentelemetry.metric.MetricDescriptorStrategy;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
@Configuration
public class OpenTelemetryConfigs {

    private ProjectConfigs projectConfigs;
    public OpenTelemetryConfigs(ProjectConfigs projectConfigs) {
        this.projectConfigs  = projectConfigs;
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.builder().build();

        MetricExporter metricExporter = GoogleCloudMetricExporter.createWithConfiguration(
                MetricConfiguration.builder()
                        // Configure the cloud project id.  Note: this is autodiscovered by default.
                        .setProjectId(projectConfigs.getProjectId())
                        .setPrefix("custom.googleapis.com")
                        // Configure a strategy for how/when to configure metric descriptors.
                        .setDescriptorStrategy(MetricDescriptorStrategy.SEND_ONCE)
                        .build());
        PrometheusHttpServer prometheusReader = PrometheusHttpServer.builder().setPort(9090).build();
        PeriodicMetricReader metricReader = PeriodicMetricReader.builder(metricExporter).setInterval(Duration.ofSeconds(10)).build();

        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(prometheusReader)
                .registerMetricReader(metricReader)
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setMeterProvider(sdkMeterProvider)
                .build();
    }
}
