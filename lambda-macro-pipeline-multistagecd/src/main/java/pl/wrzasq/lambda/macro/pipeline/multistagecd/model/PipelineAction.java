/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.codepipeline.ActionTypeId;

/**
 * Pipeline action definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineAction {
    /**
     * Action name.
     */
    @JsonProperty("Name")
    private String name;

    /**
     * Target region.
     */
    @JsonProperty("Region")
    private String region;

    /**
     * Action type identifier.
     */
    @JsonProperty("ActionTypeId")
    private ActionTypeId type;

    /**
     * Action configuration.
     */
    @JsonProperty("Configuration")
    @Builder.Default
    private Map<String, Object> configuration = new HashMap<>();

    /**
     * Variables namespace.
     */
    @JsonProperty("Namespace")
    private String namespace;

    /**
     * Input artifacts IDs.
     */
    @JsonProperty("InputArtifacts")
    @Builder.Default
    private List<String> inputs = new ArrayList<>();

    /**
     * Output artifacts IDs.
     */
    @JsonProperty("OutputArtifacts")
    @Builder.Default
    private List<String> outputs = new ArrayList<>();

    /**
     * Action order.
     */
    @JsonProperty("RunOrder")
    private Integer runOrder;
}
