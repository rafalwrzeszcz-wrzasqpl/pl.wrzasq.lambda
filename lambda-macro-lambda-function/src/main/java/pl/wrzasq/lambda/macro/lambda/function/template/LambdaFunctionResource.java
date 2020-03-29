/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 - 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.lambda.function.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import pl.wrzasq.commons.aws.cloudformation.macro.TemplateUtils;

/**
 * Model for handled resource.
 */
@AllArgsConstructor
public class LambdaFunctionResource {
    /**
     * Resource type identifier.
     */
    public static final String RESOURCE_TYPE = "WrzasqPl::Lambda::Function";

    /**
     * Metrics namespace.
     */
    private static final String METRICS_NAMESPACE = "WrzasqPl/Lambda";

    /**
     * Default retention period for logs (in days).
     */
    private static final Number DEFAULT_LOGS_RETENTION_DAYS = 7;

    /**
     * Default alarm check period (in seconds).
     */
    private static final Number DEFAULT_ALERT_PERIOD = 300;

    /**
     * Value used for counting metrics (each metric record is single occurrence).
     */
    private static final String COUNTER_METRIC_VALUE = "1";

    /**
     * LogGroupName property name.
     */
    private static final String PROPERTY_LOG_GROUP_NAME = "LogGroupName";

    /**
     * LogsRetentionInDays property name.
     */
    private static final String PROPERTY_LOGS_RETENTION_IN_DAYS = "LogsRetentionInDays";

    /**
     * Namespace property name.
     */
    private static final String PROPERTY_NAMESPACE = "Namespace";

    /**
     * MetricName property name.
     */
    private static final String PROPERTY_METRIC_NAME = "MetricName";

    /**
     * Statistic property name.
     */
    private static final String PROPERTY_STATISTIC = "Statistic";

    /**
     * ComparisonOperator property name.
     */
    private static final String PROPERTY_COMPARISON_OPERATOR = "ComparisonOperator";

    /**
     * Threshold property name.
     */
    private static final String PROPERTY_THRESHOLD = "Threshold";

    /**
     * EvaluationPeriods property name.
     */
    private static final String PROPERTY_EVALUATION_PERIODS = "EvaluationPeriods";

    /**
     * Period property name.
     */
    private static final String PROPERTY_PERIOD = "Period";

    /**
     * TreatMissingData property name.
     */
    private static final String PROPERTY_TREAT_MISSING_DATA = "TreatMissingData";

    /**
     * MetricValue property name.
     */
    private static final String PROPERTY_METRIC_VALUE = "MetricValue";

    /**
     * MetricNamespace property name.
     */
    private static final String PROPERTY_METRIC_NAMESPACE = "MetricNamespace";

    /**
     * MetricTransformations property name.
     */
    private static final String PROPERTY_METRIC_TRANSFORMATIONS = "MetricTransformations";

    /**
     * FilterPattern property name.
     */
    private static final String PROPERTY_FILTER_PATTERN = "FilterPattern";

    /**
     * Resource name.
     */
    private String logicalId;

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
        this.createMemoryMetricFilter(resources);
        this.createErrorsMetricFilter(resources, properties);
        this.createErrorsAlarm(resources, properties);
        this.createWarningsMetricFilter(resources, properties);

        // leave all properties as function properties
        this.createLambdaFunction(resources, properties);

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
            LambdaFunctionResource.PROPERTY_LOG_GROUP_NAME,
            TemplateUtils.sub(String.format("/aws/lambda/${%s}", this.logicalId))
        );

        TemplateUtils.popProperty(
            properties,
            LambdaFunctionResource.PROPERTY_LOGS_RETENTION_IN_DAYS,
            value -> resourceProperties.put("RetentionInDays", value),
            LambdaFunctionResource.DEFAULT_LOGS_RETENTION_DAYS
        );

        this.generateResource(
            resources,
            "LogGroup",
            "Logs::LogGroup",
            resourceProperties
        );
    }

    /**
     * Creates memory metric filter resource definition.
     *
     * @param resources Resources container.
     */
    private void createMemoryMetricFilter(Map<String, Object> resources) {
        this.createMetricFilter(
            resources,
            "MemoryMetricFilter",
            "Memory",
            "$max_memory_used",
            "[label=\"REPORT\", "
                + "..., "
                + "memory_label=\"Used:\", "
                + "max_memory_used, unit=\"MB\", "
                + "xray_label=\"XRAY\", "
                + "trace_label=\"TraceId:\", "
                + "traced, "
                + "segment_label=\"SegmentId:\", "
                + "segment, "
                + "sampled_label=\"Sampled:\", "
                + "sampled_value]"
        );
    }

    /**
     * Creates errors metric filter resource definition.
     *
     * @param resources Resources container.
     * @param properties Resource properties.
     */
    private void createErrorsMetricFilter(Map<String, Object> resources, Map<String, Object> properties) {
        TemplateUtils.popProperty(
            properties,
            "ErrorsFilterPattern",
            filter -> this.createMetricFilter(
                resources,
                "ErrorsMetricFilter",
                "Errors",
                LambdaFunctionResource.COUNTER_METRIC_VALUE,
                filter
            ),
            "ERROR -LOG_ERROR"
        );
    }

    /**
     * Creates errors alarm resource definition.
     *
     * @param resources Resources container.
     * @param properties Resource properties.
     */
    private void createErrorsAlarm(Map<String, Object> resources, Map<String, Object> properties) {
        var resourceProperties = new HashMap<String, Object>();
        resourceProperties.put(LambdaFunctionResource.PROPERTY_NAMESPACE, LambdaFunctionResource.METRICS_NAMESPACE);
        resourceProperties.put(
            LambdaFunctionResource.PROPERTY_METRIC_NAME,
            TemplateUtils.sub(String.format("${%s}-Errors", this.logicalId))
        );
        resourceProperties.put(LambdaFunctionResource.PROPERTY_STATISTIC, "Sum");
        resourceProperties.put(LambdaFunctionResource.PROPERTY_COMPARISON_OPERATOR, "GreaterThanThreshold");
        resourceProperties.put(LambdaFunctionResource.PROPERTY_THRESHOLD, 0);
        resourceProperties.put(LambdaFunctionResource.PROPERTY_EVALUATION_PERIODS, 1);
        resourceProperties.put(LambdaFunctionResource.PROPERTY_PERIOD, LambdaFunctionResource.DEFAULT_ALERT_PERIOD);
        resourceProperties.put(LambdaFunctionResource.PROPERTY_TREAT_MISSING_DATA, "notBreaching");

        TemplateUtils.popProperty(
            properties,
            "ErrorsAlarmActions",
            value -> resourceProperties.put("AlarmActions", value),
            null
        );

        this.generateResource(
            resources,
            "ErrorsAlarm",
            "CloudWatch::Alarm",
            resourceProperties
        );
    }

    /**
     * Creates warnings metric filter resource definition.
     *
     * @param resources Resources container.
     * @param properties Resource properties.
     */
    private void createWarningsMetricFilter(Map<String, Object> resources, Map<String, Object> properties) {
        TemplateUtils.popProperty(
            properties,
            "WarningsFilterPattern",
            filter -> this.createMetricFilter(
                resources,
                "WarningsMetricFilter",
                "Warnings",
                LambdaFunctionResource.COUNTER_METRIC_VALUE,
                filter
            ),
            "WARN"
        );
    }

    /**
     * Generic method for metric filter creation.
     *
     * @param resources Resources container.
     * @param resourceName Resource logical ID.
     * @param metricNameSuffix Metric name postfix.
     * @param metricValue Metric value.
     * @param filterPattern Metric log filter pattern.
     */
    private void createMetricFilter(
        Map<String, Object> resources,
        String resourceName,
        String metricNameSuffix,
        String metricValue,
        Object filterPattern
    ) {
        var transformation = new HashMap<>();
        transformation.put(LambdaFunctionResource.PROPERTY_METRIC_NAMESPACE, LambdaFunctionResource.METRICS_NAMESPACE);
        transformation.put(
            LambdaFunctionResource.PROPERTY_METRIC_NAME,
            TemplateUtils.sub(String.format("${%s}-%s", this.logicalId, metricNameSuffix))
        );
        transformation.put(LambdaFunctionResource.PROPERTY_METRIC_VALUE, metricValue);

        var resourceProperties = new HashMap<String, Object>();
        resourceProperties.put(
            LambdaFunctionResource.PROPERTY_LOG_GROUP_NAME,
            TemplateUtils.ref(this.getLogGroupLogicalId())
        );
        resourceProperties.put(
            LambdaFunctionResource.PROPERTY_METRIC_TRANSFORMATIONS,
            Collections.singletonList(transformation)
        );
        // note that this is not a string - it may be defined as a CloudFormation function call!
        resourceProperties.put(LambdaFunctionResource.PROPERTY_FILTER_PATTERN, filterPattern);

        this.generateResource(
            resources,
            resourceName,
            "Logs::MetricFilter",
            resourceProperties
        );
    }

    /**
     * Creates function resource definition.
     *
     * @param resources Resources container.
     * @param properties Resource properties.
     */
    private void createLambdaFunction(Map<String, Object> resources, Map<String, Object> properties) {
        this.generateResource(
            resources,
            "",
            "Lambda::Function",
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
            TemplateUtils.generateResource(type, properties, null)
        );
    }
}
