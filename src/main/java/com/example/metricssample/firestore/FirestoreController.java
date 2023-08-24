package com.example.metricssample.firestore;

import com.example.metricssample.common.ProjectConfigs;
import com.google.api.gax.tracing.OpenTelemetryMetricsFactory;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/firestore")
public class FirestoreController {

    private final Firestore db;
    private final OpenTelemetry openTelemetry;
    private final ProjectConfigs projectConfigs;
    public FirestoreController(OpenTelemetry openTelemetry, ProjectConfigs projectConfigs) {
        this.openTelemetry = openTelemetry;
        this.projectConfigs = projectConfigs;
        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId(projectConfigs.getProjectId())
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
