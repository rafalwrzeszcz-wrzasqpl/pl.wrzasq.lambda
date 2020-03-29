/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.project.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import pl.wrzasq.commons.aws.cloudformation.macro.TemplateUtils;

/**
 * Model for handled resource.
 */
@AllArgsConstructor
public class PipelineProjectResource {
    /**
     * Resource type identifier.
     */
    public static final String RESOURCE_TYPE = "WrzasqPl::Pipeline::Project";

    /**
     * Default retention period for logs (in days).
     */
    private static final Number DEFAULT_LOGS_RETENTION_DAYS = 14;

    /**
     * LogGroupName property name.
     */
    private static final String PROPERTY_LOG_GROUP_NAME = "LogGroupName";

    /**
     * LogsRetentionInDays property name.
     */
    private static final String PROPERTY_LOGS_RETENTION_IN_DAYS = "LogsRetentionInDays";

    /**
     * Artifacts property name.
     */
    private static final String PROPERTY_ARTIFACTS = "Artifacts";

    /**
     * Source property name.
     */
    private static final String PROPERTY_SOURCE = "Source";

    /**
     * Environment property name.
     */
    private static final String PROPERTY_ENVIRONMENT = "Environment";

    /**
     * Property key "Type".
     */
    private static final String PROPERTY_KEY_TYPE = "Type";

    /**
     * Property key "ComputeType".
     */
    private static final String PROPERTY_KEY_COMPUTE_TYPE = "ComputeType";

    /**
     * Property key "EnvironmentVariables".
     */
    private static final String PROPERTY_KEY_ENVIRONMENTVARIABLES = "EnvironmentVariables";

    /**
     * Resource name.
     */
    private String logicalId;

    /**
     * Creation condition.
     */
    private String condition;

    /**
     * Builds definition of physical resources.
     *
     * @param properties Properties for our custom resource.
     * @return Definitions of all resources.
     */
    public Map<String, Object> buildDefinitions(Map<String, Object> properties) {
        var resources = new HashMap<String, Object>();

        // generate all sub-resources
        this.createLogGroup(resources, properties);

        // leave all properties as project properties
        this.createCodeBuildProject(resources, properties);

        return resources;
    }

    /**
     * Creates log group resource definition.
     *
     * @param resources Resources container.
     * @param properties Resource properties.
     */
    private void createLogGroup(Map<String, Object> resources, Map<String, Object> properties) {
        // default setup
        var resourceProperties = new HashMap<String, Object>();
        resourceProperties.put(
            PipelineProjectResource.PROPERTY_LOG_GROUP_NAME,
            TemplateUtils.sub(String.format("/aws/codebuild/${%s}", this.logicalId))
        );

        TemplateUtils.popProperty(
            properties,
            PipelineProjectResource.PROPERTY_LOGS_RETENTION_IN_DAYS,
            value -> resourceProperties.put("RetentionInDays", value),
            PipelineProjectResource.DEFAULT_LOGS_RETENTION_DAYS
        );

        this.generateResource(
            resources,
            "LogGroup",
            "Logs::LogGroup",
            resourceProperties
        );
    }

    /**
     * Creates project resource definition.
     *
     * @param resources Resources container.
     * @param properties Resource properties.
     */
    private void createCodeBuildProject(Map<String, Object> resources, Map<String, Object> properties) {
        // default setup for CodePipeline
        properties.computeIfAbsent(
            PipelineProjectResource.PROPERTY_ARTIFACTS,
            key -> PipelineProjectResource.generateArtifactsDelegation()
        );
        properties.computeIfAbsent(
            PipelineProjectResource.PROPERTY_SOURCE,
            key -> PipelineProjectResource.generateArtifactsDelegation()
        );

        var environment = TemplateUtils.asMap(
            properties.computeIfAbsent(
                PipelineProjectResource.PROPERTY_ENVIRONMENT,
                key -> new HashMap<>()
            )
        );
        properties.put(
            PipelineProjectResource.PROPERTY_ENVIRONMENT,
            environment
        );

        // sensible defaults for environment
        environment.putIfAbsent(PipelineProjectResource.PROPERTY_KEY_TYPE, "LINUX_CONTAINER");
        environment.putIfAbsent(PipelineProjectResource.PROPERTY_KEY_COMPUTE_TYPE, "BUILD_GENERAL1_SMALL");

        TemplateUtils.popProperty(
            properties,
            "Variables",
            value -> environment.put(
                PipelineProjectResource.PROPERTY_KEY_ENVIRONMENTVARIABLES,
                PipelineProjectResource.addMapToEntries(
                    environment.computeIfAbsent(
                        PipelineProjectResource.PROPERTY_KEY_ENVIRONMENTVARIABLES,
                        key -> new ArrayList<>()
                    ),
                    value
                )
            ),
            null
        );

        this.generateResource(
            resources,
            "",
            "CodeBuild::Project",
            properties
        );
    }

    /**
     * Builds logical ID of LogGroup resource.
     *
     * @return LogGroup logical ID.
     */
    public String getLogGroupLogicalId() {
        return String.format("%sLogGroup", this.logicalId);
    }

    /**
     * Adds resource to collection.
     *
     * @param resources Resources container.
     * @param suffix Resource logical ID suffix.
     * @param type CloudFormation resource type.
     * @param properties Resource configuration.
     */
    private void generateResource(
        Map<String, Object> resources,
        String suffix,
        String type,
        Map<String, Object> properties
    ) {
        resources.put(
            String.format("%s%s", this.logicalId, suffix),
            TemplateUtils.generateResource(type, properties, this.condition)
        );
    }

    /**
     * Generates artifacts reference for CodePipeline.
     *
     * @return CodePipeline setup for CodeBuild.
     */
    private static Map<String, Object> generateArtifactsDelegation() {
        var setup = new HashMap<String, Object>();
        setup.put(PipelineProjectResource.PROPERTY_KEY_TYPE, "CODEPIPELINE");
        return setup;
    }

    /**
     * Converts key-value mapping to list of entries.
     *
     * @param container Entries container.
     * @param map Key-value container.
     * @return List container.
     */
    private static List<Object> addMapToEntries(Object container, Object map) {
        var list = new ArrayList<>();

        if (container instanceof List) {
            list.addAll((List<?>) container);
        }

        TemplateUtils.asMap(map)
            .forEach((key, value) -> list.add(PipelineProjectResource.buildEnvironmentVariable(key, value)));

        return list;
    }

    /**
     * Builds CodeBuild environment variable definition.
     *
     * @param key Variable name.
     * @param value Variable value.
     * @return Variable entry.
     */
    private static Map<String, Object> buildEnvironmentVariable(String key, Object value) {
        var container = new HashMap<String, Object>();
        container.put("Name", key);
        container.put("Value", value);
        return container;
    }
}
