package com.example.metricssample.bigtable;

import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.tracing.OpenTelemetryTracerFactory;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.metric.MetricConfiguration;
import com.google.cloud.opentelemetry.metric.MetricDescriptorStrategy;
import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static com.example.metricssample.bigtable.BigtableOpenTelemetryApiMetricsTracer.BIGTABLE_ATTEMPT_LATENCY;
import static com.example.metricssample.bigtable.BigtableOpenTelemetryMetricsTracerFactory.METER_NAME;

@RestController
@RequestMapping(path = "/bigtable")
public class BigtableController {
    public static final String PROJECT_ID = "blakes-playground-341721";
    private BigtableDataClient dataClient;
    private BigtableTableAdminClient adminClient;
    private static final String tableId = "test-metrics-table";
    private static final String COLUMN_FAMILY = "cf1";
    private static final String COLUMN_QUALIFIER_GREETING = "greeting";
    private static final String COLUMN_QUALIFIER_NAME = "name";
    private static final String ROW_KEY_PREFIX = "rowKey";

    public BigtableController() throws Exception {
        String instanceId = "test-routing-headers";
        RpcViews.registerAllViews();

        OpenTelemetryTracerFactory openTelemetryTracerFactory = createOpenTelemetryTracerFactory();
        BigtableDataSettings.Builder builder = BigtableDataSettings.newBuilder()
                .setProjectId(PROJECT_ID)
                .setInstanceId(instanceId);
        builder.stubSettings().setTracerFactory(openTelemetryTracerFactory);

//        BigtableDataSettings.enableBuiltinMetrics();
        BigtableDataSettings.enableGfeOpenCensusStats();
        BigtableDataSettings.enableOpenCensusStats();

        dataClient = BigtableDataClient.create(builder.build());
        BigtableTableAdminSettings adminSettings =
                BigtableTableAdminSettings.newBuilder()
                        .setProjectId(PROJECT_ID)
                        .setInstanceId(instanceId)
                        .build();
        adminClient = BigtableTableAdminClient.create(adminSettings);
        if (!adminClient.exists(tableId)) {
            adminClient.createTable(CreateTableRequest.of(tableId).addFamily("cf1"));
            writeToTable();
        }
    }

    private static OpenTelemetryTracerFactory createOpenTelemetryTracerFactory() {
        //Default resource is "Generic Task"
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "bigtable")));

//        MetricExporter cloudMonitoringExporter = GoogleCloudMetricExporter.createWithDefaultConfiguration();
        MetricExporter cloudMonitoringExporter = GoogleCloudMetricExporter.createWithConfiguration(
                MetricConfiguration.builder()
                        // Configure the cloud project id.  Note: this is autodiscovered by default.
                        .setProjectId(PROJECT_ID)
                        .setPrefix("custom.googleapis.com/opentelemetry")
                        // Configure a strategy for how/when to configure metric descriptors.
                        .setDescriptorStrategy(MetricDescriptorStrategy.SEND_ONCE)
                        .build());

        View view = View.builder()
                .setName("cloud.google.com/java/bigtable/attempt_latency")
                .setDescription("Attempt latency in msecs")
                .build();
        InstrumentSelector instrumentSelector = InstrumentSelector.builder()
                .setName(BIGTABLE_ATTEMPT_LATENCY)
                .setMeterName(METER_NAME)
                .setType(InstrumentType.HISTOGRAM)
                .setUnit("ms")
                .build();
        View operationLatencyView = View.builder()
                .setName("cloud.google.com/java/bigtable/operation_latency")
                .setDescription("Attempt latency in msecs")
                .build();
        InstrumentSelector operationLatencyInstrumentSelector = InstrumentSelector.builder()
                .setName("operation_latency")
                .setMeterName(METER_NAME)
                .setType(InstrumentType.HISTOGRAM)
                .setUnit("ms")
                .build();
        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(cloudMonitoringExporter).setInterval(Duration.ofSeconds(20)).build())
                .setResource(resource)
                .registerView(instrumentSelector, view)
                .registerView(operationLatencyInstrumentSelector, operationLatencyView)
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(sdkMeterProvider)
                .buildAndRegisterGlobal();

        return new BigtableOpenTelemetryMetricsTracerFactory(openTelemetry);
    }

    @GetMapping(path = "/", produces = "application/json")
    public String getARow() throws Exception {
        Row row = dataClient.readRow(tableId, "rowKey0");
        System.out.println("Received row: " + row.toString());
        return row.toString();
    }

    public void writeToTable() {
        try {
            System.out.println("\nWriting some greetings to the table");
            String[] names = {"World", "Bigtable", "Java"};
            for (int i = 0; i < names.length; i++) {
                String greeting = "Hello " + names[i] + "!";
                RowMutation rowMutation =
                        RowMutation.create(tableId, ROW_KEY_PREFIX + i)
                                .setCell(COLUMN_FAMILY, COLUMN_QUALIFIER_NAME, names[i])
                                .setCell(COLUMN_FAMILY, COLUMN_QUALIFIER_GREETING, greeting);
                dataClient.mutateRow(rowMutation);
                System.out.println(greeting);
            }
        } catch (NotFoundException e) {
            System.err.println("Failed to write to non-existent table: " + e.getMessage());
        }
    }
}
