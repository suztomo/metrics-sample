package com.example.metricssample.firestore;

import com.google.api.core.ApiFuture;
import com.google.api.gax.tracing.OpenTelemetryMetricsFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.metricssample.bigtable.BigtableController.PROJECT_ID;

@RestController
@RequestMapping(path = "/firestore")
public class FirestoreController {

    private final Firestore db;
    private OpenTelemetry openTelemetry;

    public FirestoreController(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId(PROJECT_ID)
                        .setApiTracerFactory(createOpenTelemetryTracerFactory())
                        .build();
        db = firestoreOptions.getService();
    }

    private OpenTelemetryMetricsFactory createOpenTelemetryTracerFactory() {
        return new OpenTelemetryMetricsFactory(openTelemetry, "java-firestore", "3.13.4");
    }

    @GetMapping(path = "/", produces = "application/json")
    public List<String> getDocuments() throws Exception{
        //Manually create a Books collection
        QuerySnapshot querySnapshot = db.collection("Books").get().get();
        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
        return documents.stream()
                .map(DocumentSnapshot::toString)
                .collect(Collectors.toList());
    }
}
