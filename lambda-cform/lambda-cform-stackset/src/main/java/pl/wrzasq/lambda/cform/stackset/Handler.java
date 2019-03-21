/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.stackset.model.StackSetRequest;
import pl.wrzasq.lambda.cform.stackset.model.StackSetResponse;
import pl.wrzasq.lambda.cform.stackset.service.StackSetManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler
{
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<StackSetRequest, StackSetResponse> handler;

    static {
        AmazonCloudFormation cloudFormation = AmazonCloudFormationClientBuilder.defaultClient();

        StackSetManager deploy = new StackSetManager(cloudFormation);

        Handler.handler = new CustomResourceHandler<>(
            deploy::deployStackSet,
            deploy::deployStackSet,
            deploy::deleteStackSet
        );
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<StackSetRequest> request, Context context)
    {
        Handler.handler.handle(request, context);
    }
}
