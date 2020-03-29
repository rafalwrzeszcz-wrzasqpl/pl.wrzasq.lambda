/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 - 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.lambda.function.template;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.macro.lambda.function.template.LambdaFunctionResource;

public class LambdaFunctionResourceTest {
    @Test
    public void getLogGroupLogicalId() {
        var logicalId = "TestFunction";
        var resource = new LambdaFunctionResource(logicalId, "Serverless");
        // this is just for code coverage now, we can make more detailed tests in future based on error-cases
        // it will be tested in ProcessedTemplateTest that involves full JSON files
        resource.buildDefinitions(Collections.emptyMap());

        Assertions.assertTrue(
            resource.getLogGroupLogicalId().startsWith(logicalId),
            "LambdaFunctionResource.getLogGroupLogicalId() should return ID of created log group."
        );
    }
}
