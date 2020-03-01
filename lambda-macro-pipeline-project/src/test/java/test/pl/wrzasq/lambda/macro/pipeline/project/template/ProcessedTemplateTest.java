/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.pipeline.project.template;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.macro.pipeline.project.template.ProcessedTemplate;

public class ProcessedTemplateTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void processTemplate() throws IOException {
        var input = this.objectMapper.readValue(
            this.getClass().getResourceAsStream("/input.json"),
            new TypeReference<Map<String, Object>>() {}
        );
        var output = this.objectMapper.readValue(
            this.getClass().getResourceAsStream("/output.json"),
            new TypeReference<Map<String, Object>>() {}
        );

        Assertions.assertEquals(
            output,
            new ProcessedTemplate(input).getTemplate(),
            "ProcessedTemplate should handle WrzasqPl::Pipeline::Project resources."
        );
    }
}
