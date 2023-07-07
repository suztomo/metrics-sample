# What is this repo?
This repo is a sample Spring Boot app that demonstrates OpenCensus metrics in Bigtable/Spanner, and OpenTelemetry metrics in Bigtable

# How to run this sample?
This repo is dependent on SNAPSHOT versions of gax and bigtable, see relevant changes in [gax](https://github.com/googleapis/sdk-platform-java/pull/1807) and [bigtable](https://github.com/googleapis/java-bigtable/pull/1813).
1. Enable Cloud Monitoring API
2. Update project/instance/table ids in SpannerController and BigtableController
2. Start the app
3. Navigate to http://localhost:8080/spanner and http://localhost:8080/bigtable for a few read requests
4. Wait up to 1 minute, the metrics should show up in Cloud Monitoring dashboard on Metrics explorer tab. The relevant metrics are under Global -> Custom metrics -> OpenCensus/cloud/google.com/java/bigtable(or spanner)
