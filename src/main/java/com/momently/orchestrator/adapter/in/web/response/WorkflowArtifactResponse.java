package com.momently.orchestrator.adapter.in.web.response;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response payload exposing a workflow artifact for the console UI.
 */
public record WorkflowArtifactResponse(
    String artifactType,
    String path,
    String contentType,
    JsonNode json,
    String text
) {
}

