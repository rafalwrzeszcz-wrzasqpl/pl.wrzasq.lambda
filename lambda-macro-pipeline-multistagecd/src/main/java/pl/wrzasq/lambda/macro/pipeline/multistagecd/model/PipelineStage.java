/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Pipeline stage definition.
 */
@Data
public class PipelineStage {
    /**
     * Stage name.
     */
    @JsonProperty("Name")
    private String name;

    /**
     * Condition name for creating stage.
     */
    @JsonProperty("Condition")
    private String condition;

    /**
     * Stage actions.
     */
    @JsonProperty("Actions")
    private List<PipelineAction> actions;
}
