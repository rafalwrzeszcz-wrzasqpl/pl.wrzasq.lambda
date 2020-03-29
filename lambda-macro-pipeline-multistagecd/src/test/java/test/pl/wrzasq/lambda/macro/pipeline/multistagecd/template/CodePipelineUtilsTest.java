/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.pipeline.multistagecd.template;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.codepipeline.ActionTypeId;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.template.CodePipelineUtils;

public class CodePipelineUtilsTest {
    @Test
    public void buildArtifactRef() {
        var name = "build";
        var output = CodePipelineUtils.buildArtifactRef(name);

        Assertions.assertEquals(
            name,
            output.get("Name"),
            "CodePipelineUtils.buildArtifactRef() should return construct with artifact name."
        );
    }

    @Test
    public void buildStage() {
        var name = "Build";
        var actions = this;
        var output = CodePipelineUtils.buildStage(name, actions);

        Assertions.assertEquals(
            name,
            output.get("Name"),
            "CodePipelineUtils.buildStage() should return construct with stage name."
        );
        Assertions.assertSame(
            actions,
            output.get("Actions"),
            "CodePipelineUtils.buildStage() should return construct with stage actions."
        );
    }

    @Test
    public void convertActionTypeId() {
        var type = new ActionTypeId();
        type.setCategory("Deploy");
        type.setProvider("S3");
        type.setOwner("AWS");
        type.setVersion("2");
        var output = CodePipelineUtils.convertActionTypeId(type);

        Assertions.assertTrue(
            output instanceof Map,
            "CodePipelineUtils.convertActionTypeId() should convert model to map."
        );

        var map = (Map) output;
        Assertions.assertEquals(
            type.getCategory(),
            map.get("Category"),
            "CodePipelineUtils.convertActionTypeId() should convert model to map."
        );
        Assertions.assertEquals(
            type.getProvider(),
            map.get("Provider"),
            "CodePipelineUtils.convertActionTypeId() should convert model to map."
        );
        Assertions.assertEquals(
            type.getOwner(),
            map.get("Owner"),
            "CodePipelineUtils.convertActionTypeId() should convert model to map."
        );
        Assertions.assertEquals(
            type.getVersion(),
            map.get("Version"),
            "CodePipelineUtils.convertActionTypeId() should convert model to map."
        );
    }
    @Test
    public void convertActionTypeIdNotModel() {
        var type = "dummy";
        var output = CodePipelineUtils.convertActionTypeId(type);

        Assertions.assertEquals(
            type,
            output,
            "CodePipelineUtils.convertActionTypeId() should return same value if it's not a model object."
        );
    }
}
