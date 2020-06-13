/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 - 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.edgedeploy;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.commons.json.ObjectMapperFactory;
import pl.wrzasq.lambda.edgedeploy.model.EdgeDeployRequest;
import pl.wrzasq.lambda.edgedeploy.service.LambdaEdgeManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<EdgeDeployRequest, PublishVersionResult> handler;

    static {
        var objectMapper = ObjectMapperFactory.createObjectMapper();
        var lambda = AWSLambdaClientBuilder.standard()
            // Lambda@Edge needs to be deployed in Virginia!
            .withRegion(Regions.US_EAST_1)
            .build();

        var s3 = AmazonS3ClientBuilder.defaultClient();

        var deploy = new LambdaEdgeManager(lambda, s3, objectMapper);

        Handler.handler = new CustomResourceHandler<>(deploy::create, deploy::update, deploy::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<EdgeDeployRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
