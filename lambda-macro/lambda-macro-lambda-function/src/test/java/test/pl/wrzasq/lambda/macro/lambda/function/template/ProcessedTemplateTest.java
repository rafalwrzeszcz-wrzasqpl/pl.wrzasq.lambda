/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.lambda.function.template;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.macro.lambda.function.template.ProcessedTemplate;

public class ProcessedTemplateTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void processTemplate() throws IOException {
        Map<String, Object> input = this.objectMapper.readValue(
            this.getClass().getResourceAsStream("/input.json"),
            new TypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> output = this.objectMapper.readValue(
            this.getClass().getResourceAsStream("/output.json"),
            new TypeReference<Map<String, Object>>() {}
        );

        Assertions.assertEquals(
            output,
            new ProcessedTemplate(input).getTemplate(),
            "ProcessedTemplate should handle WrzasqPl::Lambda::Function resources."
        );
    }
}
