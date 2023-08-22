
package com.example.metricssample.spanner;

import com.google.api.gax.tracing.OpenTelemetryMetricsFactory;
import com.google.cloud.spanner.*;
import com.google.cloud.spanner.spi.v1.SpannerRpcViews;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(path = "/spanner")
public class SpannerController {
  private Spanner spanner;
  private DatabaseClient dbClient;
  private String instanceId = "testspanner";
  private String databaseId = "test-metrics";
  private String table = "Players";

  private final OpenTelemetry openTelemetry;

  SpannerController(OpenTelemetry openTelemetry) throws Exception{
    this.openTelemetry = openTelemetry;
    // Instantiate the client.
    SpannerOptions options = SpannerOptions.newBuilder().setApiTracerFactory(createOpenTelemetryTracerFactory()).build();

    spanner = options.getService();
    // And then create the Spanner database client.
    String projectId = options.getProjectId();
    dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

    // Register GFELatency and GFE Header Missing Count Views
    SpannerRpcViews.registerGfeLatencyAndHeaderMissingCountViews();
  }

  private OpenTelemetryMetricsFactory createOpenTelemetryTracerFactory() {
    return new OpenTelemetryMetricsFactory(openTelemetry, "java-spanner", "6.13.0");
  }

  @GetMapping(path = "/", produces = "application/json")
  public List<Person> getPerson() {
    List<Person> list = new ArrayList<>();
    try (ResultSet resultSet =
        dbClient
            .singleUse()
            .read(
                table,
                KeySet.all(), // Read all rows in a table.
                Arrays.asList("id", "name", "email"))) {
      while (resultSet.next()) {
        Person person = new Person();
        person.setId(resultSet.getString(0));
        person.setName(resultSet.getString(1));
        person.setEmail(resultSet.getString(2));
        list.add(person);
      }
    }
    return list;
  }

  @PostMapping(path = "/", consumes = "application/json", produces = "application/json")
  public Person addPerson(@RequestBody Person p) {
    List<Mutation> mutations =
        Collections.singletonList(
            Mutation.newInsertBuilder(table)
                .set("id")
                .to(UUID.randomUUID().toString())
                .set("name")
                .to(p.getName())
                .set("email")
                .to(p.getEmail())
                .build());
    dbClient.write(mutations);
    return p;
  }
}
