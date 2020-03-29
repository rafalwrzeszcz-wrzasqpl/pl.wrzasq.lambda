/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Pipeline setup.
 */
@Data
public class PipelineDefinition {
    /**
     * Pipeline configuration.
     */
    private PipelineConfiguraiton config = new PipelineConfiguraiton();

    /**
     * Straight properties for AWS::CodePipeline::Pipeline resource.
     */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Pipeline data sources.
     */
    private Map<String, Map<String, Object>> sources;

    /**
     * Pipelines stages.
     */
    private List<PipelineStage> stages;

    /**
     * Pipeline end artifacts.
     */
    private Map<String, PipelineArtifact> artifacts;
}
