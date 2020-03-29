/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.macro.pipeline.multistagecd.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.template.ProcessedTemplate;

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
            "ProcessedTemplate should handle Pipeline resource checkout state."
        );
    }

    @Test
    public void processTemplateWithoutTemplate() {
        var input = new HashMap<String, Object>();

        Assertions.assertSame(
            input,
            new ProcessedTemplate(input).getTemplate(),
            "ProcessedTemplate should not change anything if there is no `Pipeline` section."
        );
    }

    @Test
    public void processTemplateWithoutWebhook() {
        var config = new HashMap<>();
        config.put("resourceName", "TestPipeline");

        var pipeline = new HashMap<>();
        pipeline.put("config", config);
        pipeline.put("sources", Collections.emptyMap());
        pipeline.put("stages", Collections.emptyList());
        pipeline.put("artifacts", Collections.emptyMap());

        var input = new HashMap<String, Object>();
        input.put("Pipeline", pipeline);

        var output = new ProcessedTemplate(input).getTemplate();
        var resources = output.get("Resources");

        Assertions.assertTrue(
            ((Map) resources).containsKey("TestPipeline"),
            "ProcessedTemplate should contain pipeline definition even if no webhook is configured."
        );
        Assertions.assertFalse(
            ((Map) resources).containsKey("TestPipelineWehbook"),
            "ProcessedTemplate should not contain webhook definition if it wasn't configured."
        );
    }

    @Test
    public void processTemplateWithCircularDependency() {
        var actionA = new HashMap<>();
        actionA.put("InputArtifacts", Collections.singletonList("b"));
        actionA.put("OutputArtifacts", Collections.singletonList("a"));

        var actionB = new HashMap<>();
        actionB.put("InputArtifacts", Collections.singletonList("a"));
        actionB.put("OutputArtifacts", Collections.singletonList("b"));

        var stage = new HashMap<>();
        stage.put("Name", "Deploy");
        stage.put("Actions", Arrays.asList(actionA, actionB));

        var stages = new ArrayList<>();
        stages.add(stage);

        var pipeline = new HashMap<>();
        pipeline.put("sources", Collections.emptyMap());
        pipeline.put("stages", stages);
        pipeline.put("artifacts", Collections.emptyMap());

        var input = new HashMap<String, Object>();
        input.put("Pipeline", pipeline);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new ProcessedTemplate(input)
        );
    }
}
