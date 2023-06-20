package com.example.metricssample;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public BigtableController() throws Exception{
        String projectId = "your-project-id";
        String instanceId = "your-instance-id";

        BigtableDataSettings settings =
                BigtableDataSettings.newBuilder().setProjectId(projectId).setInstanceId(instanceId)
                        .build();
//        BigtableDataSettings.enableBuiltinMetrics();
        BigtableDataSettings.enableGfeOpenCensusStats();
        BigtableDataSettings.enableOpenCensusStats();

        dataClient = BigtableDataClient.create(settings);
        BigtableTableAdminSettings adminSettings =
                BigtableTableAdminSettings.newBuilder()
                        .setProjectId(projectId)
                        .setInstanceId(instanceId)
                        .build();
        adminClient = BigtableTableAdminClient.create(adminSettings);
        if (!adminClient.exists(tableId)) {
            adminClient.createTable(CreateTableRequest.of(tableId).addFamily("cf1"));
            writeToTable();
        }
    }

    @GetMapping(path = "/", produces = "application/json")
    public String getARow() throws Exception{
        Row row = dataClient.readRow(tableId, "rowKey0");
        System.out.printf("Received row: " + row.toString());
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
