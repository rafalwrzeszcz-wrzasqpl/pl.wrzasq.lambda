/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset.instance.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackInstancesRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackInstancesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackInstanceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackSetOperationRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackInstance;
import com.amazonaws.services.cloudformation.model.StackInstanceNotFoundException;
import com.amazonaws.services.cloudformation.model.StackSetOperation;
import com.amazonaws.services.cloudformation.model.StackSetOperationStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackInstancesRequest;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.stackset.instance.model.StackInstanceRequest;

/**
 * CloudFormation API implementation.
 */
public class StackSetInstanceManager
{
    /**
     * Default sleep interval (1 minute).
     */
    private static final long DEFAULT_SLEEP_INTERVAL = 60000;

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(StackSetInstanceManager.class);

    /**
     * AWS CloudFormation API client.
     */
    private AmazonCloudFormation cloudFormation;

    /**
     * Sleep interval for status change checks.
     */
    @Setter
    private long sleepInterval = StackSetInstanceManager.DEFAULT_SLEEP_INTERVAL;

    /**
     * Initializes object with given CloudFormation client.
     *
     * @param cloudFormation AWS CloudFormation client.
     */
    public StackSetInstanceManager(AmazonCloudFormation cloudFormation)
    {
        this.cloudFormation = cloudFormation;
    }

    /**
     * Handles stack set deployment.
     *
     * @param input Resource deployment request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about published version.
     */
    public CustomResourceResponse<StackInstance> deployStackInstance(
        StackInstanceRequest input,
        String physicalResourceId
    )
    {
        String operationId;
        try {
            operationId = this.updateStackInstance(input);
        } catch (StackInstanceNotFoundException error) {
            operationId = this.createStackInstance(input);
        }

        // wait until operation is finished
        StackSetOperation operation;
        do {
            operation = this.cloudFormation.describeStackSetOperation(
                new DescribeStackSetOperationRequest()
                    .withStackSetName(input.getStackSetName())
                    .withOperationId(operationId)
            )
                .getStackSetOperation();

            switch (StackSetOperationStatus.fromValue(operation.getStatus())) {
                case FAILED:
                case STOPPED:
                    this.logger.error("Stack operation {} failed with status.", operationId, operation.getStatus());
                    throw new IllegalStateException(
                        String.format(
                            "Stack operation %s (%s) for stack %s in %s %s failed with status %s.",
                            operation.getAction(),
                            operationId,
                            operation.getStackSetId(),
                            input.getAccountId(),
                            input.getRegion(),
                            operation.getStatus()
                        )
                    );

                case RUNNING:
                case STOPPING:
                    this.logger.info("Stack operation {} in progress.", operationId);
                    this.sleep();
                    break;

                case SUCCEEDED:
                    this.logger.info("Stack operation {} succeeded.", operationId);
                    break;
            }
        } while (StackSetOperationStatus.fromValue(operation.getStatus()) != StackSetOperationStatus.SUCCEEDED);

        StackInstance stackInstance = this.cloudFormation.describeStackInstance(
            new DescribeStackInstanceRequest()
                .withStackSetName(input.getStackSetName())
                .withStackInstanceAccount(input.getAccountId())
                .withStackInstanceRegion(input.getRegion())
        )
            .getStackInstance();

        return new CustomResourceResponse<>(stackInstance, StackSetInstanceManager.buildPhysicalResourceId(input));
    }

    /**
     * Handles stack instance deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<StackInstance> deleteStackInstance(
        StackInstanceRequest input,
        String physicalResourceId
    )
    {
        StackInstanceRequest spec = StackSetInstanceManager.parsePhysicalResourceId(physicalResourceId);

        this.cloudFormation.deleteStackInstances(
            new DeleteStackInstancesRequest()
                .withStackSetName(spec.getStackSetName())
                .withAccounts(spec.getAccountId())
                .withRegions(spec.getRegion())
        );

        this.logger.info("Stack instance deleted.");

        return new CustomResourceResponse<>(null, physicalResourceId);
    }

    /**
     * Creates new stack instance.
     *
     * @param input Stack instance specification.
     * @return Stack set operation ID.
     */
    private String createStackInstance(StackInstanceRequest input)
    {
        this.logger.info(
            "Stack set {} instance not found for account {} in {}, creating new one.",
            input.getStackSetName(),
            input.getAccountId(),
            input.getRegion()
        );

        return this.cloudFormation.createStackInstances(
            new CreateStackInstancesRequest()
                .withStackSetName(input.getStackSetName())
                .withAccounts(input.getAccountId())
                .withRegions(input.getRegion())
                .withParameterOverrides(StackSetInstanceManager.buildSdkParameters(input))
        )
            .getOperationId();
    }

    /**
     * Updates existing stack instance.
     *
     * @param input Stack instance specification.
     * @return Stack set operation ID.
     */
    private String updateStackInstance(StackInstanceRequest input)
    {
        StackInstance stackInstance = this.cloudFormation.describeStackInstance(
            new DescribeStackInstanceRequest()
                .withStackSetName(input.getStackSetName())
                .withStackInstanceAccount(input.getAccountId())
                .withStackInstanceRegion(input.getRegion())
        )
            .getStackInstance();

        this.logger.info(
            "Updating only parameters for stack set {} instance for account {} in {}, creating new one - ID {}.",
            input.getStackSetName(),
            input.getAccountId(),
            input.getRegion(),
            stackInstance.getStackId()
        );

        // we only update parameters
        return this.cloudFormation.updateStackInstances(
            new UpdateStackInstancesRequest()
                .withStackSetName(input.getStackSetName())
                .withAccounts(input.getAccountId())
                .withRegions(input.getRegion())
                .withParameterOverrides(StackSetInstanceManager.buildSdkParameters(input))
        )
            .getOperationId();
    }

    /**
     * Converts key-value mapping into AWS SDK structure.
     *
     * @param input Request data.
     * @return Collection of AWS SDK DTOs.
     */
    private static Collection<Parameter> buildSdkParameters(StackInstanceRequest input)
    {
        return input.getParameterOverrides().entrySet().stream()
            .map(
                (Map.Entry<String, String> entry) ->
                    new Parameter()
                        .withParameterKey(entry.getKey())
                        .withParameterValue(entry.getValue())
            )
            .collect(Collectors.toList());
    }

    /**
     * Converts string identifier into stack instance specification.
     *
     * @param physicalResourceId Compound identifier.
     * @return Stack instance request specification.
     */
    private static StackInstanceRequest parsePhysicalResourceId(String physicalResourceId)
    {
        String[] parts = physicalResourceId.split(":");
        StackInstanceRequest request = new StackInstanceRequest();
        request.setStackSetName(parts[0]);
        request.setAccountId(parts[1]);
        request.setRegion(parts[2]);
        return request;
    }

    /**
     * Converts stack instance specification into string specification.
     *
     * @param input Stack instance request specification.
     * @return Compound identifier.
     */
    private static String buildPhysicalResourceId(StackInstanceRequest input)
    {
        return String.format(
            "%s:%s:%s",
            input.getStackSetName(),
            input.getAccountId(),
            input.getRegion()
        );
    }

    /**
     * Performs a wait.
     */
    private void sleep()
    {
        try {
            Thread.sleep(this.sleepInterval);
        } catch (InterruptedException error) {
            this.logger.error("Wait interval interrupted.", error);
        }
    }
}
