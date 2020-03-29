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
import lombok.Data;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.codepipeline.ActionTypeId;

/**
 * Pipeline action definition.
 */
@Data
public class PipelineAction {
    /**
     * Action name.
     */
    @JsonProperty("Name")
    private String name;

    /**
     * Action type identifier.
     */
    @JsonProperty("ActionTypeId")
    private ActionTypeId type;

    /**
     * Action configuration.
     */
    @JsonProperty("Configuration")
    private Map<String, Object> configuration = new HashMap<>();

    /**
     * Input artifacts IDs.
     */
    @JsonProperty("InputArtifacts")
    private List<String> inputs = new ArrayList<>();

    /**
     * Output artifacts IDs.
     */
    @JsonProperty("OutputArtifacts")
    private List<String> outputs = new ArrayList<>();

    /**
     * Action order.
     */
    @JsonProperty("RunOrder")
    private Integer runOrder;
}
