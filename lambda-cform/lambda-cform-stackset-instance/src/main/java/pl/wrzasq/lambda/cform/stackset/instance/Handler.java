/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset.instance;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.StackInstance;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.commons.aws.cloudformation.StackSetHandler;
import pl.wrzasq.lambda.cform.stackset.instance.model.StackInstanceRequest;
import pl.wrzasq.lambda.cform.stackset.instance.service.StackSetInstanceManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<StackInstanceRequest, StackInstance> handler;

    static {
        AmazonCloudFormation cloudFormation = AmazonCloudFormationClientBuilder.defaultClient();

        StackSetHandler stackSetHandler = new StackSetHandler(cloudFormation);

        StackSetInstanceManager deploy = new StackSetInstanceManager(cloudFormation, stackSetHandler);

        Handler.handler = new CustomResourceHandler<>(
            deploy::deployStackInstance,
            deploy::deployStackInstance,
            deploy::deleteStackInstance
        );
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<StackInstanceRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
