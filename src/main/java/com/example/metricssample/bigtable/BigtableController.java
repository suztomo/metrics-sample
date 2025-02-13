package com.example.metricssample.bigtable;

import com.example.metricssample.common.ProjectConfigs;
import com.google.api.gax.grpc.ChannelPoolSettings;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.tracing.OpenTelemetryMetricsFactory;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.metric.MetricConfiguration;
import com.google.cloud.opentelemetry.metric.MetricDescriptorStrategy;
import com.google.common.collect.ImmutableList;
import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static com.example.metricssample.bigtable.BigtableOpenTelemetryApiMetricsTracer.BIGTABLE_ATTEMPT_LATENCY;
import static com.example.metricssample.bigtable.BigtableOpenTelemetryMetricsFactory.METER_NAME;

@RestController
@RequestMapping(path = "/bigtable")
public class BigtableController {
    private BigtableDataClient dataClient;
    private BigtableTableAdminClient adminClient;
    private static final String tableId = "test-metrics-table";
    private static final String COLUMN_FAMILY = "cf1";
    private static final String COLUMN_QUALIFIER_GREETING = "greeting";
    private static final String COLUMN_QUALIFIER_NAME = "name";
    private static final String ROW_KEY_PREFIX = "rowKey";

    private OpenTelemetry openTelemetry;
    private ProjectConfigs projectConfigs;

    public BigtableController(OpenTelemetry openTelemetry, ProjectConfigs projectConfigs) throws Exception {
        this.openTelemetry = openTelemetry;
        this.projectConfigs = projectConfigs;
        String instanceId = "test-routing-headers";
        //Register OpenCensus views for gRPC metrics
        RpcViews.registerAllViews();

        OpenTelemetryMetricsFactory openTelemetryTracerFactory = createOpenTelemetryTracerFactory();
        BigtableDataSettings.Builder builder = BigtableDataSettings.newBuilder()
                .setProjectId(projectConfigs.getProjectId())
                .setInstanceId(instanceId);
        ChannelPoolSettings channelPoolSettings = ChannelPoolSettings.builder()
                .setInitialChannelCount(2)
                .setMinChannelCount(2)
                .setMaxChannelCount(10)
                .build();
        InstantiatingGrpcChannelProvider transportChannelProvider = InstantiatingGrpcChannelProvider.newBuilder().setChannelPoolSettings(channelPoolSettings).build();
        builder.stubSettings().setTracerFactory(openTelemetryTracerFactory).setTransportChannelProvider(transportChannelProvider);

        BigtableDataSettings.enableBuiltinMetrics();
        BigtableDataSettings.enableGfeOpenCensusStats();
        BigtableDataSettings.enableOpenCensusStats();

        dataClient = BigtableDataClient.create(builder.build());
        BigtableTableAdminSettings adminSettings =
                BigtableTableAdminSettings.newBuilder()
                        .setProjectId(projectConfigs.getProjectId())
                        .setInstanceId(instanceId)
                        .build();
        adminClient = BigtableTableAdminClient.create(adminSettings);
        if (!adminClient.exists(tableId)) {
            adminClient.createTable(CreateTableRequest.of(tableId).addFamily("cf1"));
            writeToTable();
        }
    }
    private static final Aggregation AGGREGATION_WITH_MILLIS_HISTOGRAM =
            Aggregation.explicitBucketHistogram(
                    ImmutableList.of(
                            0.0, 0.01, 0.05, 0.1, 0.3, 0.6, 0.8, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 13.0,
                            16.0, 20.0, 25.0, 30.0, 40.0, 50.0, 65.0, 80.0, 100.0, 130.0, 160.0, 200.0, 250.0,
                            300.0, 400.0, 500.0, 650.0, 800.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0,
                            100000.0));
    private OpenTelemetryMetricsFactory createOpenTelemetryTracerFactory() {
        //Default resource is "Generic Task"
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "bigtable")));

        MetricExporter cloudMonitoringExporter = GoogleCloudMetricExporter.createWithConfiguration(
                MetricConfiguration.builder()
                        // Configure the cloud project id.  Note: this is autodiscovered by default.
                        .setProjectId(projectConfigs.getProjectId())
                        .setPrefix("custom.googleapis.com")
                        // Configure a strategy for how/when to configure metric descriptors.
                        .setDescriptorStrategy(MetricDescriptorStrategy.SEND_ONCE)
                        .build());

        View view = View.builder()
                .setName("custom.googleapis.com/opentelemetry/cloud.google.com/java/bigtable/attempt_latency")
                .setDescription("Attempt latency in msecs")
                .setAggregation(AGGREGATION_WITH_MILLIS_HISTOGRAM)
                .build();
        InstrumentSelector instrumentSelector = InstrumentSelector.builder()
                .setName(BIGTABLE_ATTEMPT_LATENCY)
                .setMeterName(METER_NAME)
                .setType(InstrumentType.HISTOGRAM)
                .setUnit("ms")
                .build();
        View operationLatencyView = View.builder()
                .setName("custom.googleapis.com/opentelemetry/cloud.google.com/java/bigtable/operation_latency")
                .setDescription("Attempt latency in msecs")
                .setAggregation(AGGREGATION_WITH_MILLIS_HISTOGRAM)
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
                .build();

        return new BigtableOpenTelemetryMetricsFactory(this.openTelemetry);
    }

    @GetMapping(path = "/", produces = "application/json")
    public String getARow() throws Exception {
        Row row = dataClient.readRow(tableId, "rowKey0");
        System.out.println("Received row: " + row.toString());
        return row.toString();
    }

    @GetMapping(path = "/rows", produces = "application/json")
    public String getRows() throws Exception {
        Iterable<Row> rows = dataClient.readRows(Query.create(tableId).limit(10));
        rows.forEach(row -> System.out.println("Received row: " + row.toString()));
        return rows.toString();
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
