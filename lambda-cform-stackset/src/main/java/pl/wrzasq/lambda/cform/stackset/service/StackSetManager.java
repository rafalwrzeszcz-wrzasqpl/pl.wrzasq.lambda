/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset.service;

import java.util.Collection;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackSetRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackSetRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackSetRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackSetNotFoundException;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackSetRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.commons.aws.cloudformation.StackSetHandler;
import pl.wrzasq.commons.aws.cloudformation.StackUtils;
import pl.wrzasq.lambda.cform.stackset.model.StackSetRequest;
import pl.wrzasq.lambda.cform.stackset.model.StackSetResponse;

/**
 * CloudFormation API implementation.
 */
public class StackSetManager {
    /**
     * Message pattern for drift case.
     */
    private static final String DRIFT_LOG_MESSAGE_PATTERN
        = "Stack set ID {} differs from CloudFormation-provided physical resource ID {}.";

    /**
     * Cast array.
     */
    private static final Capability[] CAPABILITY_CAST = new Capability[0];

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(StackSetManager.class);

    /**
     * AWS CloudFormation API client.
     */
    private AmazonCloudFormation cloudFormation;

    /**
     * Stack set operations helper.
     */
    private StackSetHandler stackSetHandler;

    /**
     * Initializes object with given CloudFormation client.
     *
     * @param cloudFormation AWS CloudFormation client.
     * @param stackSetHandler Stack set operations helper.
     */
    public StackSetManager(AmazonCloudFormation cloudFormation, StackSetHandler stackSetHandler) {
        this.cloudFormation = cloudFormation;
        this.stackSetHandler = stackSetHandler;
    }

    /**
     * Handles stack set deployment.
     *
     * @param input Resource deployment request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about published version.
     */
    public CustomResourceResponse<StackSetResponse> deployStackSet(StackSetRequest input, String physicalResourceId) {
        var response = new StackSetResponse();

        try {
            var stackSet = this.cloudFormation.describeStackSet(
                new DescribeStackSetRequest()
                    .withStackSetName(input.getStackSetName())
            )
                .getStackSet();
            this.logger.info("Stack set already exists (ARN {}).", stackSet.getStackSetARN());

            this.updateStackSet(input);

            // don't do anything here - in worst case it will fail in Delete call in UPDATE_COMPLETE_CLEANUP phase
            // it will not hurt us (even if fails, as it's post-update phase) and allows to simplify logic of this
            // method to avoid handling all combinations, which could lead to unhandled edge cases
            if (!stackSet.getStackSetId().equals(physicalResourceId)) {
                this.logger.warn(
                    StackSetManager.DRIFT_LOG_MESSAGE_PATTERN,
                    stackSet.getStackSetId(),
                    physicalResourceId
                );
            }

            response.setId(stackSet.getStackSetId());
        } catch (StackSetNotFoundException error) {
            response.setId(this.createStackSet(input));
        }

        response.setStackSetName(input.getStackSetName());
        return new CustomResourceResponse<>(response, response.getId());
    }

    /**
     * Handles stack set deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<StackSetResponse> deleteStackSet(StackSetRequest input, String physicalResourceId) {
        var stackSet = this.cloudFormation.describeStackSet(
            new DescribeStackSetRequest()
                .withStackSetName(input.getStackSetName())
        )
            .getStackSet();

        // avoid removing unknown data
        if (!stackSet.getStackSetId().equals(physicalResourceId)) {
            this.logger.error(
                StackSetManager.DRIFT_LOG_MESSAGE_PATTERN,
                stackSet.getStackSetId(),
                physicalResourceId
            );
            throw new IllegalStateException(
                String.format(
                    "Can not delete Stack set - ID %s doesn't match CloudFormation-provided resource ID %s.",
                    stackSet.getStackSetId(),
                    physicalResourceId
                )
            );
        }

        this.cloudFormation.deleteStackSet(
            new DeleteStackSetRequest()
                .withStackSetName(input.getStackSetName())
        );

        this.logger.info("Stack set deleted.");

        return new CustomResourceResponse<>(null, physicalResourceId);
    }

    /**
     * Creates new stack set.
     *
     * @param input Stack set specification.
     * @return Created stack set ID.
     */
    private String createStackSet(StackSetRequest input) {
        var result = this.cloudFormation.createStackSet(
            new CreateStackSetRequest()
                .withStackSetName(input.getStackSetName())
                .withTemplateURL(input.getTemplateUrl())
                .withDescription(input.getDescription())
                .withAdministrationRoleARN(input.getAdministrationRoleArn())
                .withExecutionRoleName(input.getExecutionRoleName())
                .withCapabilities(input.getCapabilities().toArray(StackSetManager.CAPABILITY_CAST))
                .withParameters(StackSetManager.buildSdkParameters(input))
                .withTags(StackSetManager.buildSdkTags(input))
        );

        this.logger.info("Created new stack set, ID {}.", result.getStackSetId());

        return result.getStackSetId();
    }

    /**
     * Updates existing stack set.
     *
     * @param input Stack set specification.
     */
    private void updateStackSet(StackSetRequest input) {
        var result = this.cloudFormation.updateStackSet(
            new UpdateStackSetRequest()
                .withStackSetName(input.getStackSetName())
                .withTemplateURL(input.getTemplateUrl())
                .withDescription(input.getDescription())
                .withAdministrationRoleARN(input.getAdministrationRoleArn())
                .withExecutionRoleName(input.getExecutionRoleName())
                .withCapabilities(input.getCapabilities().toArray(StackSetManager.CAPABILITY_CAST))
                .withParameters(StackSetManager.buildSdkParameters(input))
                .withTags(StackSetManager.buildSdkTags(input))
        );

        this.stackSetHandler.waitForStackSetOperation(input.getStackSetName(), result.getOperationId());

        this.logger.info("Updated stack set (operation ID: {}).", result.getOperationId());
    }

    /**
     * Converts key-value mapping into AWS SDK structure.
     *
     * @param input Request data.
     * @return Collection of AWS SDK DTOs.
     */
    private static Collection<Parameter> buildSdkParameters(StackSetRequest input) {
        return StackUtils.buildSdkList(
            input.getParameters(),
            (key, value) ->
                new Parameter()
                    .withParameterKey(key)
                    .withParameterValue(value)
        );
    }

    /**
     * Converts key-value mapping into AWS SDK structure.
     *
     * @param input Request data.
     * @return Collection of AWS SDK DTOs.
     */
    private static Collection<Tag> buildSdkTags(StackSetRequest input) {
        return StackUtils.buildSdkList(
            input.getTags(),
            (key, value) ->
                new Tag()
                    .withKey(key)
                    .withValue(value)
        );
    }
}
