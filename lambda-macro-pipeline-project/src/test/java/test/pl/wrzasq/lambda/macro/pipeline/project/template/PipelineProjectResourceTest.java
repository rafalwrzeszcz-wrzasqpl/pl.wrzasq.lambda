/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.pipeline.project.template;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.macro.pipeline.project.template.PipelineProjectResource;

public class PipelineProjectResourceTest {
    @Test
    public void getLogGroupLogicalId() {
        var logicalId = "TestProject";
        var resource = new PipelineProjectResource(logicalId);
        // this is just for code coverage now, we can make more detailed tests in future based on error-cases
        // it will be tested in ProcessedTemplateTest that involves full JSON files
        resource.buildDefinitions(new HashMap<>());

        Assertions.assertTrue(
            resource.getLogGroupLogicalId().startsWith(logicalId),
            "PipelineProjectResource.getLogGroupLogicalId() should return ID of created log group."
        );
    }
}
