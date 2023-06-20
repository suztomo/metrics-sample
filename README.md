# How to run this sample?
1. Enable Cloud Monitoring API
2. Update project/instance/table ids in SpannerController and BigtableController
2. Start the app
3. Navigate to http://localhost:8080/spanner and http://localhost:8080/bigtable for a few read requests
4. Wait up to 1 minute, the metrics should show up in Cloud Monitoring dashboard on Metrics explorer tab. The relevant metrics are under Global -> Custom metrics -> OpenCensus/cloud/google.com/java/bigtable(or spanner)
