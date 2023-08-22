package com.example.metricssample.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectConfigs {

    @Value( "${gcp.project-id}" )
    private String projectId;

    public String getProjectId() {
        return projectId;
    }
}
