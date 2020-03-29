/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.project.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.macro.ResourcesDefinition;
import pl.wrzasq.commons.aws.cloudformation.macro.TemplateDefinition;
import pl.wrzasq.commons.aws.cloudformation.macro.TemplateUtils;
import pl.wrzasq.lambda.json.ObjectMapperFactory;

/**
 * Contains template structure with handled resources references.
 */
public class ProcessedTemplate implements TemplateDefinition {
    /**
     * Values converter.
     */
    private static ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(ProcessedTemplate.class);

    /**
     * Mapping of handled resources.
     */
    private Map<String, PipelineProjectResource> resources = new HashMap<>();

    /**
     * Template structure.
     */
    @Getter
    private Map<String, Object> template;

    /**
     * Template initializer.
     *
     * @param input Initial template structure.
     */
    public ProcessedTemplate(Map<String, Object> input) {
        this.template = this.replaceReferences(
            this.createResources(input)
        );
    }

    /**
     * Finds our custom resources and creates a replacements.
     *
     * @param input Initial template structure.
     * @return Template state after processing.
     */
    private Map<String, Object> createResources(Map<String, Object> input) {
        var output = new HashMap<>(input);

        // re-create resources section
        var section = TemplateUtils.asMap(input.get(TemplateUtils.SECTION_RESOURCES));
        var resources = new HashMap<>();
        for (var entry : section.entrySet()) {
            var definition = ProcessedTemplate.objectMapper.convertValue(
                entry.getValue(),
                ResourcesDefinition.class
            );
            if (definition.getType().equals(PipelineProjectResource.RESOURCE_TYPE)) {
                resources.putAll(
                    this.createResource(entry.getKey(), definition.getProperties(), definition.getCondition())
                );
            } else {
                resources.put(entry.getKey(), section.get(entry.getKey()));
            }
        }

        output.put(TemplateUtils.SECTION_RESOURCES, resources);

        return output;
    }

    /**
     * Creates our custom resource in place of virtual one.
     *
     * @param key Resource logical ID.
     * @param properties Resource initial properties.
     * @param condition Resource creation condition.
     * @return Physical resources definitions.
     */
    private Map<String, Object> createResource(String key, Map<String, Object> properties, String condition) {
        ProcessedTemplate.logger.info("Creating resources for {}.", key);

        var resource = new PipelineProjectResource(key, condition);
        this.resources.put(key, resource);
        return resource.buildDefinitions(properties);
    }

    /**
     * Replaces references to virtual resources.
     *
     * @param input Input template structure.
     * @return Template state after processing.
     */
    private Map<String, Object> replaceReferences(Map<String, Object> input) {
        var output = new HashMap<>(input);

        var section = TemplateUtils.asMap(input.get(TemplateUtils.SECTION_RESOURCES));
        section = this.replaceDependencies(section);

        output.put(TemplateUtils.SECTION_RESOURCES, this.replaceDependencies(section));

        return output;
    }

    /**
     * Handles DependsOn clauses.
     *
     * @param resources Resources section.
     * @return New clause value.
     */
    private Map<String, Object> replaceDependencies(Map<String, Object> resources) {
        var output = new HashMap<String, Object>();

        for (var resource : resources.entrySet()) {
            var logicalId = resource.getKey();
            var config = TemplateUtils.asMap(resource.getValue());

            if (config.containsKey(TemplateUtils.PROPERTY_KEY_DEPENDSON)) {
                var dependsOn = config.get(TemplateUtils.PROPERTY_KEY_DEPENDSON);
                config.put(TemplateUtils.PROPERTY_KEY_DEPENDSON, this.replaceDependenciesIn(dependsOn));
            }

            output.put(logicalId, config);
        }

        return output;
    }

    /**
     * Handles DependsOn clause of single resource.
     *
     * @param dependsOn Depends on clause.
     * @return Computed new DependsOn clause.
     */
    private Object replaceDependenciesIn(Object dependsOn) {
        if (dependsOn instanceof List) {
            return ((List<?>) dependsOn).stream()
                .map(Object::toString)
                .map(this::resolveDependency)
                .collect(Collectors.toList());
        } else {
            return this.resolveDependency(dependsOn.toString());
        }
    }

    /**
     * Tries to resolve dependencies against lambda functions.
     *
     * <p>
     *     Effective dependency will be LogGroup, to ensure no Lambda execution happens before log group creation.
     * </p>
     *
     * @param dependency Source dependency ID.
     * @return Resolved dependency ID.
     */
    private String resolveDependency(String dependency) {
        return this.resources.containsKey(dependency)
            ? this.resources.get(dependency).getLogGroupLogicalId()
            : dependency;
    }
}
