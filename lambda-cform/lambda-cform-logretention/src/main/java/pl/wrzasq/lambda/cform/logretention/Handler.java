/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.logretention;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.logretention.model.RetentionRequest;
import pl.wrzasq.lambda.cform.logretention.service.RetentionManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<RetentionRequest, Object> handler;

    static {
        AWSLogs cloudWatch = AWSLogsClientBuilder.defaultClient();

        RetentionManager deploy = new RetentionManager(cloudWatch);

        Handler.handler = new CustomResourceHandler<>(deploy::provision, deploy::provision, deploy::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<RetentionRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
