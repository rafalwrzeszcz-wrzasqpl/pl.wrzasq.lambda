/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackSetRequest;
import com.amazonaws.services.cloudformation.model.CreateStackSetResult;
import com.amazonaws.services.cloudformation.model.DeleteStackSetRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackSetRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackSet;
import com.amazonaws.services.cloudformation.model.StackSetNotFoundException;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackSetRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackSetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.stackset.model.StackSetRequest;
import pl.wrzasq.lambda.cform.stackset.model.StackSetResponse;

/**
 * CloudFormation API implementation.
 */
public class StackSetManager
{
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
     * Initializes object with given CloudFormation client.
     *
     * @param cloudFormation AWS CloudFormation client.
     */
    public StackSetManager(AmazonCloudFormation cloudFormation)
    {
        this.cloudFormation = cloudFormation;
    }

    /**
     * Handles stack set deployment.
     *
     * @param input Resource deployment request.
     * @return Data about published version.
     */
    public CustomResourceResponse<StackSetResponse> deployStackSet(StackSetRequest input)
    {
        StackSetResponse response = new StackSetResponse();

        try {
            StackSet result = this.cloudFormation.describeStackSet(
                new DescribeStackSetRequest()
                    .withStackSetName(input.getStackSetName())
            )
                .getStackSet();
            this.logger.info("Stack set already exists (ARN {}).", result.getStackSetARN());

            this.updateStackSet(input);

            response.setId(result.getStackSetId());
        } catch (StackSetNotFoundException error) {
            response.setId(this.createStackSet(input));
        }

        response.setStackSetName(input.getStackSetName());
        return new CustomResourceResponse<>(response, input.getStackSetName());
    }

    /**
     * Handles stack set deletion.
     *
     * @param input Resource delete request.
     * @return Empty response.
     */
    public CustomResourceResponse<StackSetResponse> deleteStackSet(StackSetRequest input)
    {
        this.cloudFormation.deleteStackSet(
            new DeleteStackSetRequest()
                .withStackSetName(input.getStackSetName())
        );

        this.logger.info("Stack set deleted.");

        return new CustomResourceResponse<>(null);
    }

    /**
     * Creates new stack set.
     *
     * @param input Stack set specification.
     * @return Created stack set ID.
     */
    private String createStackSet(StackSetRequest input)
    {
        CreateStackSetResult result = this.cloudFormation.createStackSet(
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
    private void updateStackSet(StackSetRequest input)
    {
        UpdateStackSetResult result = this.cloudFormation.updateStackSet(
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

        this.logger.info("Updated stack set (operation ID: {}).", result.getOperationId());
    }

    /**
     * Converts key-value mapping into AWS SDK structure.
     *
     * @param input Request data.
     * @return Collection of AWS SDK DTOs.
     */
    private static Collection<Parameter> buildSdkParameters(StackSetRequest input)
    {
        return input.getParameters().entrySet().stream()
            .map(
                (Map.Entry<String, String> entry) ->
                    new Parameter()
                        .withParameterKey(entry.getKey())
                        .withParameterValue(entry.getValue())
            )
            .collect(Collectors.toList());
    }

    /**
     * Converts key-value mapping into AWS SDK structure.
     *
     * @param input Request data.
     * @return Collection of AWS SDK DTOs.
     */
    private static Collection<Tag> buildSdkTags(StackSetRequest input)
    {
        return input.getTags().entrySet().stream()
            .map(
                (Map.Entry<String, String> entry) ->
                    new Tag()
                        .withKey(entry.getKey())
                        .withValue(entry.getValue())
            )
            .collect(Collectors.toList());
    }
}
