/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.lambda.function.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.lambda.json.ObjectMapperFactory;

/**
 * Contains template structure with handled resources references.
 */
public class ProcessedTemplate {
    /**
     * Resources section key.
     */
    private static final String SECTION_RESOURCES = "Resources";

    /**
     * DependsOn clause key.
     */
    private static final String CLAUSE_DEPENDS_ON = "DependsOn";

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
    private Map<String, LambdaFunctionResource> resources = new HashMap<>();

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
        Map<String, Object> output = new HashMap<>(input);

        // re-create resources section
        Map<String, Object> section = ProcessedTemplate.asMap(input.get(ProcessedTemplate.SECTION_RESOURCES));
        Map<String, Object> resources = new HashMap<>();
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            ResourcesDefinition definition = ProcessedTemplate.objectMapper.convertValue(
                entry.getValue(),
                ResourcesDefinition.class
            );
            if (definition.getType().equals(LambdaFunctionResource.RESOURCE_TYPE)) {
                resources.putAll(
                    this.createResource(entry.getKey(), definition.getProperties())
                );
            } else {
                resources.put(entry.getKey(), section.get(entry.getKey()));
            }
        }

        output.put(ProcessedTemplate.SECTION_RESOURCES, resources);

        return output;
    }

    /**
     * Creates our custom resource in place of virtual one.
     *
     * @param key Resource logical ID.
     * @param properties Resource initial properties.
     * @return Physical resources definitions.
     */
    private Map<String, Object> createResource(String key, Map<String, Object> properties) {
        ProcessedTemplate.logger.info("Creating resources for {}.", key);

        LambdaFunctionResource resource = new LambdaFunctionResource(key);
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
        Map<String, Object> output = new HashMap<>(input);

        Map<String, Object> section = ProcessedTemplate.asMap(input.get(ProcessedTemplate.SECTION_RESOURCES));
        section = this.replaceDependencies(section);

        output.put(ProcessedTemplate.SECTION_RESOURCES, this.replaceDependencies(section));

        return output;
    }

    /**
     * Handles DependsOn clauses.
     *
     * @param resources Resources section.
     * @return New clause value.
     */
    private Map<String, Object> replaceDependencies(Map<String, Object> resources) {
        Map<String, Object> output = new HashMap<>();

        for (Map.Entry<String, Object> resource : resources.entrySet()) {
            String logicalId = resource.getKey();
            Map<String, Object> config = ProcessedTemplate.asMap(resource.getValue());

            if (config.containsKey(ProcessedTemplate.CLAUSE_DEPENDS_ON)) {
                Object dependsOn = config.get(ProcessedTemplate.CLAUSE_DEPENDS_ON);
                config.put(ProcessedTemplate.CLAUSE_DEPENDS_ON, this.replaceDependenciesIn(dependsOn));
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

    /**
     * Safely converts value to a typed map.
     *
     * @param value Input object.
     * @return Typed map.
     */
    private static Map<String, Object> asMap(Object value) {
        Map<String, Object> output = new HashMap<>();

        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                output.put(entry.getKey().toString(), entry.getValue());
            }
        }

        return output;
    }
}
