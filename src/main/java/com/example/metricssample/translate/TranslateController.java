
package com.example.metricssample.translate;

import com.example.metricssample.common.ProjectConfigs;
import com.google.api.gax.tracing.OpenTelemetryMetricsFactory;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.stub.TranslationServiceStubSettings;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/translate")
public class TranslateController {
  private TranslationServiceClient translationServiceClient;
  private final OpenTelemetry openTelemetry;

  private ProjectConfigs projectConfigs;

  TranslateController(OpenTelemetry openTelemetry, ProjectConfigs projectConfigs) throws Exception{
    this.openTelemetry = openTelemetry;
    this.projectConfigs = projectConfigs;
    TranslationServiceStubSettings stubSettings = TranslationServiceStubSettings.newBuilder()
            .setTracerFactory(createOpenTelemetryTracerFactory())
            .build();
    translationServiceClient = TranslationServiceClient.create(stubSettings.createStub());
  }

  private OpenTelemetryMetricsFactory createOpenTelemetryTracerFactory() {
    return new OpenTelemetryMetricsFactory(openTelemetry, "java-translate", "2.23.0");
  }

  @GetMapping(path = "/{text}", produces = "application/json")
  public String translate(@PathVariable String text) {
    LocationName locationName = LocationName.of(projectConfigs.getProjectId(), "global");
    TranslateTextResponse response = translationServiceClient.translateText(locationName, "es", List.of(text));
    String result = response.getTranslations(0).getTranslatedText();
    System.out.println("Received translation: " + result);
    return result;
  }

}
