/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.appsync.graphqlapi.data;

import com.amazonaws.services.appsync.AWSAppSyncClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data.AppSyncGraphQlApiDataRequest;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data.AppSyncGraphQlApiDataResponse;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.service.AppSyncGraphQlApiHandler;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<AppSyncGraphQlApiDataRequest, AppSyncGraphQlApiDataResponse> handler;

    static {
        var appSync = AWSAppSyncClientBuilder.defaultClient();

        var reader = new AppSyncGraphQlApiHandler(appSync);

        Handler.handler = new CustomResourceHandler<>(reader::read, reader::read, reader::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<AppSyncGraphQlApiDataRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
