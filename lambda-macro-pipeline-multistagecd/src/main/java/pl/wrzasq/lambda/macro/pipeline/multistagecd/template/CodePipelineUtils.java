/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.codepipeline.ActionTypeId;

/**
 * CodePipeline utilities.
 */
public class CodePipelineUtils {
    /**
     * "Name" property key.
     */
    private static final String KEY_NAME = "Name";

    /**
     * "Actions" property key.
     */
    private static final String KEY_ACTIONS = "Actions";

    /**
     * Builds artifact structure.
     *
     * @param name Artifact name.
     * @return CloudFormation structure.
     */
    public static Map<String, String> buildArtifactRef(String name) {
        return Collections.singletonMap(CodePipelineUtils.KEY_NAME, name);
    }

    /**
     * Builds stage structure.
     *
     * @param name Stage name.
     * @param actions Pipeline actions.
     * @return CloudFormation structure.
     */
    public static Map<String, Object> buildStage(String name, Object actions) {
        var stage = new HashMap<String, Object>();
        stage.put(CodePipelineUtils.KEY_NAME, name);
        stage.put(CodePipelineUtils.KEY_ACTIONS, actions);
        return stage;
    }

    /**
     * Converts type model to plain map.
     *
     * @param input CodePipeline model.
     * @return CloudFormation structure.
     */
    public static Object convertActionTypeId(Object input) {
        if (input instanceof ActionTypeId) {
            var type = (ActionTypeId) input;
            var value = new HashMap<String, String>();
            value.put("Category", type.getCategory());
            value.put("Owner", type.getOwner());
            value.put("Provider", type.getProvider());
            value.put("Version", type.getVersion());
            return value;
        } else {
            return input;
        }
    }
}
