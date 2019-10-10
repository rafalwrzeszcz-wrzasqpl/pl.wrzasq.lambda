/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.lambda.function.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Generic structure of resources section.
 */
@Data
public class ResourcesDefinition {
    /**
     * Resource type.
     */
    @JsonProperty("Type")
    private String type;

    /**
     * Resource properties.
     */
    @JsonProperty("Properties")
    private Map<String, Object> properties;
}
