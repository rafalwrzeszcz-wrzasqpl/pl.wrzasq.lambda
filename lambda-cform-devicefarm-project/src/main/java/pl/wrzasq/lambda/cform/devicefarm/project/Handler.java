/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.devicefarm.project;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.devicefarm.AWSDeviceFarmClientBuilder;
import com.amazonaws.services.devicefarm.model.TestGridProject;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.devicefarm.project.model.DeviceFarmProjectRequest;
import pl.wrzasq.lambda.cform.devicefarm.project.service.DeviceFarmProjectManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<DeviceFarmProjectRequest, TestGridProject> handler;

    static {
        // DeviceFarm projects are "global" but managed from us-west-1 region
        var deviceFarm = AWSDeviceFarmClientBuilder.standard().withRegion(Regions.US_WEST_2).build();

        var deploy = new DeviceFarmProjectManager(deviceFarm);

        Handler.handler = new CustomResourceHandler<>(deploy::create, deploy::update, deploy::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<DeviceFarmProjectRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
