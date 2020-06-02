/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.devicefarm.project.service;

import com.amazonaws.services.devicefarm.AWSDeviceFarm;
import com.amazonaws.services.devicefarm.model.CreateTestGridProjectRequest;
import com.amazonaws.services.devicefarm.model.DeleteTestGridProjectRequest;
import com.amazonaws.services.devicefarm.model.TestGridProject;
import com.amazonaws.services.devicefarm.model.UpdateTestGridProjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.devicefarm.project.model.DeviceFarmProjectRequest;

/**
 * DeviceFarm API implementation.
 */
public class DeviceFarmProjectManager {
    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(DeviceFarmProjectManager.class);

    /**
     * AWS DeviceFarm API client.
     */
    private AWSDeviceFarm deviceFarm;

    /**
     * Initializes object with given DeviceFarm client.
     *
     * @param deviceFarm AWS DeviceFarm client.
     */
    public DeviceFarmProjectManager(AWSDeviceFarm deviceFarm) {
        this.deviceFarm = deviceFarm;
    }

    /**
     * Handles project creation.
     *
     * @param input Resource creation request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about created project.
     */
    public CustomResourceResponse<TestGridProject> create(DeviceFarmProjectRequest input, String physicalResourceId) {
        var project = this.deviceFarm.createTestGridProject(
            new CreateTestGridProjectRequest()
                .withName(input.getName())
                .withDescription(input.getDescription())
        )
            .getTestGridProject();

        return new CustomResourceResponse<>(
            project,
            project.getArn()
        );
    }

    /**
     * Handles project update.
     *
     * @param input Resource update request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about updated project.
     */
    public CustomResourceResponse<TestGridProject> update(DeviceFarmProjectRequest input, String physicalResourceId) {
        var project = this.deviceFarm.updateTestGridProject(
            new UpdateTestGridProjectRequest()
                .withProjectArn(physicalResourceId)
                .withName(input.getName())
                .withDescription(input.getDescription())
        )
            .getTestGridProject();

        return new CustomResourceResponse<>(
            project,
            project.getArn()
        );
    }

    /**
     * Handles project deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<TestGridProject> delete(DeviceFarmProjectRequest input, String physicalResourceId) {
        this.deviceFarm.deleteTestGridProject(
            new DeleteTestGridProjectRequest()
                .withProjectArn(physicalResourceId)
        );

        this.logger.info("Project deleted.");

        return new CustomResourceResponse<>(null, physicalResourceId);
    }
}
