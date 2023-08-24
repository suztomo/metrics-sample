# What is this repo?
This repo is a sample Spring Boot app that demonstrates OpenTelemetry metrics in Translate

# How to run this sample?
This repo is dependent on the branch `otel-poc` of gax. see relevant changes in [gax](https://github.com/googleapis/sdk-platform-java/pull/1807)
1. Enable [Cloud Monitoring API](https://pantheon.corp.google.com/apis/api/monitoring.googleapis.com) and [Translate API](https://pantheon.corp.google.com/apis/api/translate.googleapis.com)
2. Go to the root folder of `sdk-platform-java`, switch to branch `otel-poc`, run `mvn clean install -DskipTests -Dcheckstyle.skip -Dclirr.skip`. 
3. Replace the project id in application.properties to your own project id.
4. Start the app
5. Navigate to http://localhost:8080/translate/hello for a few requests
6. Wait up to 1 minute, the metrics should show up in [Cloud Monitoring dashboard on Metrics explorer tab](https://pantheon.corp.google.com/monitoring/metrics-explorer). The relevant metrics are under Generic Task -> Custom metrics -> attempt_latency
7. Alternatively, navigate to http://localhost:9090 to see the latest metrics without waiting.
