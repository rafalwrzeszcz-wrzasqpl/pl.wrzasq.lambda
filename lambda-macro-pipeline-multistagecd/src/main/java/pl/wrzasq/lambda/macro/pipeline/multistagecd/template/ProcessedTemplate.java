/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.macro.TemplateDefinition;
import pl.wrzasq.commons.aws.cloudformation.macro.TemplateUtils;
import pl.wrzasq.commons.json.ObjectMapperFactory;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.PipelineAction;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.PipelineArtifact;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.PipelineConfiguraiton;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.PipelineDefinition;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.PipelineStage;
import pl.wrzasq.lambda.macro.pipeline.multistagecd.model.codepipeline.ActionTypeId;

/**
 * Contains template structure after macro logic transformation.
 */
public class ProcessedTemplate implements TemplateDefinition {
    /**
     * Artificial "Pipeline" section.
     */
    private static final String SECTION_PIPELINE = "Pipeline";

    /**
     * !Ref "AWS::NoValue" expression.
     */
    private static final Object NO_VALUE = TemplateUtils.ref("AWS::NoValue");

    /**
     * CloudFormation artifact path separator.
     */
    private static final String ARTIFACT_SEPARATOR = "::";

    /**
     * Values converter.
     */
    private static ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(ProcessedTemplate.class);

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
        this.template = input.containsKey(ProcessedTemplate.SECTION_PIPELINE)
            ? this.processTemplate(input)
            // nothing to process
            : input;
    }

    /**
     * Builds pipeline resource.
     *
     * @param input Initial template structure.
     * @return Template state after processing.
     */
    private Map<String, Object> processTemplate(Map<String, Object> input) {
        logger.info("Found pipeline definition.");

        // default values are set in the classes
        var pipeline = ProcessedTemplate.objectMapper.convertValue(
            input.get(ProcessedTemplate.SECTION_PIPELINE),
            PipelineDefinition.class
        );
        input.remove(ProcessedTemplate.SECTION_PIPELINE);

        var output = new HashMap<>(input);

        // pipeline metadata setup
        output.put(
            TemplateUtils.SECTION_CONDITIONS,
            this.buildConditions(
                TemplateUtils.asMap(input.computeIfAbsent(TemplateUtils.SECTION_CONDITIONS, key -> new HashMap<>())),
                pipeline.getConfig()
            )
        );
        output.put(
            TemplateUtils.SECTION_RESOURCES,
            this.buildResources(
                TemplateUtils.asMap(input.computeIfAbsent(TemplateUtils.SECTION_RESOURCES, key -> new HashMap<>())),
                pipeline
            )
        );

        return output;
    }

    /**
     * Builds conditions used by pipeline.
     *
     * @param conditions Initial conditions.
     * @param config Pipeline config.
     * @return Template conditions.
     */
    private Map<String, Object> buildConditions(Map<String, Object> conditions, PipelineConfiguraiton config) {
        conditions.put(
            config.getHasCheckoutStepConditionName(),
            ProcessedTemplate.buildBooleanCondition(config.getHasCheckoutStepParameterName())
        );
        conditions.put(
            config.getHasNextStageConditionName(),
            ProcessedTemplate.buildBooleanCondition(config.getHasNextStageParameterName())
        );
        conditions.put(
            config.getRequiresManualApprovalConditionName(),
            Collections.singletonMap(
                "Fn::And",
                Arrays.asList(
                    Collections.singletonMap("Condition", config.getHasNextStageConditionName()),
                    ProcessedTemplate.buildBooleanCondition(config.getRequiresManualApprovalParameterName())
                )
            )
        );
        return conditions;
    }

    /**
     * Builds resources used by pipeline.
     *
     * @param resources Resources container.
     * @param pipeline Pipeline definition.
     * @return Template conditions.
     */
    private Map<String, Object> buildResources(Map<String, Object> resources, PipelineDefinition pipeline) {
        var config = pipeline.getConfig();

        resources.put(config.getResourceName(), this.buildPipelineDefinition(pipeline));

        // GitHub webhook
        if (config.getWebhookAuthenticationType() != null && config.getWebhookSecretToken() != null) {
            resources.put(
                String.format("%sWebhook", config.getResourceName()),
                this.buildWebhookDefinition(config, pipeline.getSources().keySet().iterator().next())
            );
        }

        return resources;
    }

    /**
     * Build pipeline definition.
     *
     * @param pipeline Pipeline definition.
     * @return Resource definition.
     */
    private Map<String, Object> buildPipelineDefinition(PipelineDefinition pipeline) {
        var config = pipeline.getConfig();
        var artifacts = pipeline.getArtifacts();

        var stages = new ArrayList<>();

        stages.add(this.buildSourceStage(config, pipeline.getSources(), artifacts));
        stages.addAll(this.buildStages(pipeline.getStages()));
        stages.add(this.buildManualApprovalStage(config));
        stages.add(this.buildPromoteStage(config, artifacts));

        // finalize properties
        var properties = new HashMap<>(pipeline.getProperties());
        properties.put("Stages", stages);

        return TemplateUtils.generateResource(
            "CodePipeline::Pipeline",
            properties,
            null
        );
    }

    /**
     * Builds sources stage.
     *
     * @param config Pipeline configuration.
     * @param sources Source locations.
     * @param artifacts Previous stage artifacts.
     * @return Stage definition.
     */
    private Map<String, Object> buildSourceStage(
        PipelineConfiguraiton config,
        Map<String, Map<String, Object>> sources,
        Map<String, PipelineArtifact> artifacts
    ) {
        return CodePipelineUtils.buildStage(
            "Source",
            ProcessedTemplate.fnIf(
                config.getHasCheckoutStepConditionName(),
                this.buildCheckoutSources(sources),
                this.buildPromotedSources(artifacts)
            )
        );
    }

    /**
     * Builds sources action steps.
     *
     * @param sources Source locations.
     * @return Stage actions.
     */
    private Object buildCheckoutSources(Map<String, Map<String, Object>> sources) {
        var steps = new ArrayList<>(sources.size());

        // keeping order to avoid structure changes in CloudFormation
        for (var id : new TreeSet<>(sources.keySet())) {
            steps.add(
                ProcessedTemplate.buildActionDefinition(
                    sources.get(id),
                    PipelineAction.builder()
                        .name(id)
                        .outputs(Collections.singletonList(id))
                        .build()
                )
            );
        }

        return steps;
    }

    /**
     * Builds sources action steps.
     *
     * @param artifacts Previous stage artifacts.
     * @return Stage actions.
     */
    private Object buildPromotedSources(Map<String, PipelineArtifact> artifacts) {
        var steps = new ArrayList<>(artifacts.size());
        var runOrder = 0;

        var type = ActionTypeId.s3Source();

        /*
        having sources in reverse direction than in promote step, makes us sure that each next stage pipeline is
        launched only when all previous stage artifacts are properly uploaded
         */
        for (var id : new TreeSet<>(artifacts.keySet()).descendingSet()) {
            var artifact = artifacts.get(id);

            var actionConfig = new HashMap<String, Object>();
            actionConfig.put("S3Bucket", artifact.getSourceBucketName());
            actionConfig.put("S3ObjectKey", artifact.getObjectKey());

            steps.add(
                ProcessedTemplate.buildActionDefinition(
                    PipelineAction.builder()
                        .name(id)
                        .type(type)
                        .configuration(actionConfig)
                        .outputs(Collections.singletonList(id))
                        .runOrder(++runOrder)
                        .build()
                )
            );
        }

        return steps;
    }

    /**
     * Builds pipelines stage definition.
     *
     * @param stages Stages definitions.
     * @return Stages definition.
     */
    private List<Object> buildStages(List<PipelineStage> stages) {
        return stages.stream()
            .map(
                stage -> stage.getCondition() == null
                    ? this.buildStage(stage)
                    : ProcessedTemplate.fnIf(stage.getCondition(), this.buildStage(stage), ProcessedTemplate.NO_VALUE)
            )
            .collect(Collectors.toList());
    }

    /**
     * Builds manual approval stage definition.
     *
     * @param config Pipeline configuration.
     * @return Stage definition.
     */
    private Object buildManualApprovalStage(PipelineConfiguraiton config) {
        return ProcessedTemplate.fnIf(
            config.getRequiresManualApprovalConditionName(),
            CodePipelineUtils.buildStage(
                "Review",
                Collections.singletonList(
                    ProcessedTemplate.buildActionDefinition(
                        PipelineAction.builder()
                            .name("Approval")
                            .type(ActionTypeId.manualApproval())
                            .build()
                    )
                )
            ),
            ProcessedTemplate.NO_VALUE
        );
    }

    /**
     * Builds artifacts promotion stage definition.
     *
     * @param config Pipeline configuration.
     * @param artifacts Artifacts configuration.
     * @return Stage definition.
     */
    private Object buildPromoteStage(PipelineConfiguraiton config, Map<String, PipelineArtifact> artifacts) {
        var steps = new ArrayList<>(artifacts.size());
        var runOrder = 0;

        for (var id : new TreeSet<>(artifacts.keySet())) {
            var artifact = artifacts.get(id);

            var type = ActionTypeId.s3Deploy();

            var actionConfig = new HashMap<String, Object>();
            actionConfig.put("BucketName", artifact.getNextBucketName());
            actionConfig.put("ObjectKey", artifact.getObjectKey());
            actionConfig.put("Extract", false);
            // this shifts ownership to target account in case (usual scenario) of cross-account deployment
            actionConfig.put("CannedACL", "bucket-owner-full-control");

            steps.add(
                ProcessedTemplate.buildActionDefinition(
                    PipelineAction.builder()
                        .name(id)
                        .type(type)
                        .configuration(actionConfig)
                        .inputs(Collections.singletonList(id))
                        .runOrder(++runOrder)
                        .build()
                )
            );
        }

        return ProcessedTemplate.fnIf(
            config.getHasNextStageConditionName(),
            CodePipelineUtils.buildStage("Promote", steps),
            ProcessedTemplate.NO_VALUE
        );
    }

    /**
     * Builds pipeline stage definition structure.
     *
     * @param stage Stage setup.
     * @return CodePipeline stage definition.
     */
    private Map<String, Object> buildStage(PipelineStage stage) {
        var actions = stage.getActions();
        actions.forEach(ProcessedTemplate::normalizeAction);

        ProcessedTemplate.orderActions(actions);

        return CodePipelineUtils.buildStage(
            stage.getName(),
            actions.stream().map(ProcessedTemplate::buildActionDefinition).collect(Collectors.toList())
        );
    }

    /**
     * Normalizes all actions.
     *
     * @param action Action setup.
     */
    private static void normalizeAction(PipelineAction action) {
        if (action.getType() == null) {
            action.setType(ActionTypeId.cloudFormationDeploy());
        }

        action.setInputs(ProcessedTemplate.detectInputArtifacts(action));
    }

    /**
     * Orders all actions.
     *
     * @param actions Stage actions.
     */
    private static void orderActions(List<PipelineAction> actions) {
        var visited = new HashSet<PipelineAction>();
        var outputs = new HashMap<String, PipelineAction>();
        actions.forEach(action -> action.getOutputs().forEach(input -> outputs.put(input, action)));

        actions.forEach(action -> ProcessedTemplate.calculateActionOrder(action, visited, outputs));
    }

    /**
     * Calculates action order.
     *
     * @param action Current subject action.
     * @param visited Current state markers.
     * @param outputs Output artifacts mapping.
     */
    private static void calculateActionOrder(
        PipelineAction action,
        Set<PipelineAction> visited,
        Map<String, PipelineAction> outputs
    ) {
        // already calculated
        if (action.getRunOrder() != null) {
            return;
        }

        if (visited.contains(action)) {
            throw new IllegalArgumentException(
                String.format("Circular artifact dependency for %s.", action.getName())
            );
        }

        visited.add(action);

        action.getInputs().stream()
            // dependencies from outside of stage not need to be considered
            .filter(outputs::containsKey)
            .peek(input -> ProcessedTemplate.calculateActionOrder(outputs.get(input), visited, outputs))
            .map(input -> outputs.get(input).getRunOrder())
            .mapToInt(order -> (order == null ? 1 : order) + 1)
            .max()
            .ifPresent(action::setRunOrder);

        visited.remove(action);
    }

    /**
     * Builds list of all used input artifacts.
     *
     * @param action Action definition.
     * @return List of input artifacts.
     */
    private static List<String> detectInputArtifacts(PipelineAction action) {
        var inputs = action.getInputs();

        // check TemplatePath source artifact
        var config = action.getConfiguration();
        var value = config.get("TemplatePath");
        if (value instanceof String) {
            inputs.add(((String) value).split(ProcessedTemplate.ARTIFACT_SEPARATOR)[0]);
        }

        // check TemplateConfiguration source artifact
        value = config.get("TemplateConfiguration");
        if (value instanceof String) {
            inputs.add(((String) value).split(ProcessedTemplate.ARTIFACT_SEPARATOR)[0]);
        }

        return inputs;
    }

    /**
     * Populates pipeline action definition structure.
     *
     * @param action Action configuration.
     * @return CodePipeline stage definition.
     */
    private static Map<String, Object> buildActionDefinition(PipelineAction action) {
        return ProcessedTemplate.buildActionDefinition(new HashMap<>(), action);
    }

    /**
     * Populates pipeline action definition structure.
     *
     * @param data Initial definition.
     * @param action Action configuration.
     * @return CodePipeline stage definition.
     */
    private static Map<String, Object> buildActionDefinition(Map<String, Object> data, PipelineAction action) {
        data.put("Name", action.getName());
        data.compute(
            "ActionTypeId",
            (String key, Object old) -> CodePipelineUtils.convertActionTypeId(
                action.getType() != null ? action.getType() : old
            )
        );

        if (action.getRegion() != null) {
            data.put("Region", action.getRegion());
        }
        if (!action.getConfiguration().isEmpty()) {
            data.put("Configuration", action.getConfiguration());
        }
        if (action.getNamespace() != null) {
            data.put("Namespace", action.getNamespace());
        }
        if (!action.getInputs().isEmpty()) {
            data.put(
                "InputArtifacts",
                ProcessedTemplate.buildArtifactRefs(action.getInputs())
            );
        }
        if (!action.getOutputs().isEmpty()) {
            data.put(
                "OutputArtifacts",
                ProcessedTemplate.buildArtifactRefs(action.getOutputs())
            );
        }
        if (action.getRunOrder() != null) {
            data.put("RunOrder", action.getRunOrder());
        }

        return data;
    }

    /**
     * Builds webhook definition.
     *
     * @param config Pipeline configuration.
     * @param checkoutAction Checkout action name.
     * @return Resource definition.
     */
    private Map<String, Object> buildWebhookDefinition(PipelineConfiguraiton config, String checkoutAction) {
        var filter = new HashMap<String, String>();
        filter.put("JsonPath", "$.ref");
        filter.put("MatchEquals", "refs/heads/{Branch}");

        var properties = new HashMap<String, Object>();
        properties.put("Authentication", config.getWebhookAuthenticationType());
        properties.put(
            "AuthenticationConfiguration",
            Collections.singletonMap("SecretToken", config.getWebhookSecretToken())
        );
        properties.put("TargetPipeline", TemplateUtils.ref(config.getResourceName()));
        properties.put("TargetPipelineVersion", TemplateUtils.getAtt(config.getResourceName(), "Version"));
        properties.put("TargetAction", checkoutAction);
        properties.put("Filters", Collections.singletonList(filter));
        properties.put("RegisterWithThirdParty", true);

        return TemplateUtils.generateResource(
            "CodePipeline::Webhook",
            properties,
            config.getHasCheckoutStepConditionName()
        );
    }

    /**
     * Builds condition based on Y/N parameter.
     *
     * @param param Parameter name.
     * @return Condition definition structure.
     */
    private static Map<String, Object> buildBooleanCondition(String param) {
        return Collections.singletonMap(
            "Fn::Equals",
            Arrays.asList(
                TemplateUtils.ref(param),
                "true"
            )
        );
    }

    /**
     * Converts artifact names into list of artifact reference.
     *
     * @param names Artifact names.
     * @return Artifact references.
     */
    private static List<Map<String, String>> buildArtifactRefs(Collection<String> names) {
        return names.stream().distinct().map(CodePipelineUtils::buildArtifactRef).collect(Collectors.toList());
    }

    /**
     * Builds Fn::If call.
     *
     * @param condition Condition name.
     * @param whenTrue Value in case of positive case.
     * @param whenFalse Value in case of negative case.
     * @return Fn::If call.
     */
    private static Map<String, Object> fnIf(String condition, Object whenTrue, Object whenFalse) {
        return Collections.singletonMap("Fn::If", Arrays.asList(condition, whenTrue, whenFalse));
    }
}
