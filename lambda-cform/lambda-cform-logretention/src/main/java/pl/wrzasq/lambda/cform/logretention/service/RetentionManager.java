/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.logretention.service;

import java.util.UUID;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.DeleteRetentionPolicyRequest;
import com.amazonaws.services.logs.model.PutRetentionPolicyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.logretention.model.RetentionRequest;

/**
 * CloudWatch API implementation.
 */
public class RetentionManager {
    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(RetentionManager.class);

    /**
     * AWS CloudWatch API client.
     */
    private AWSLogs cloudWatch;

    /**
     * Initializes object with given CloudWatch client.
     *
     * @param cloudWatch AWS CloudWatch client.
     */
    public RetentionManager(AWSLogs cloudWatch) {
        this.cloudWatch = cloudWatch;
    }

    /**
     * Handles LogGroup retention setting.
     *
     * @param input Resource creation request.
     * @param physicalResourceId Physical ID of existing resource (in this case always null).
     * @return Data about published version.
     */
    public CustomResourceResponse<Object> provision(RetentionRequest input, String physicalResourceId) {
        // new ID needed, just to track it
        if (physicalResourceId == null) {
            physicalResourceId = UUID.randomUUID().toString();
        }

        for (String logGroup : input.getLogGroups()) {
            this.putRetentionPolicy(logGroup, input.getRetentionDays());
        }

        return new CustomResourceResponse<>(null, physicalResourceId);
    }

    /**
     * Handles rule deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<Object> delete(RetentionRequest input, String physicalResourceId) {
        input.getLogGroups()
            .stream()
            .map(DeleteRetentionPolicyRequest::new)
            .forEach(this.cloudWatch::deleteRetentionPolicy);

        this.logger.info(
            "Removed retention policy from CloudWatch LogGroups {}.",
            input.getLogGroups()
        );

        return new CustomResourceResponse<>(null, physicalResourceId);
    }

    /**
     * Sets retention policy for single log group.
     *
     * @param logGroup LogGroup name.
     * @param days Retention days.
     */
    private void putRetentionPolicy(String logGroup, int days) {
        this.cloudWatch.putRetentionPolicy(
            new PutRetentionPolicyRequest(logGroup, days)
        );

        this.logger.info("Setting retention days of LogGroup {} to {}.", logGroup, days);
    }
}
