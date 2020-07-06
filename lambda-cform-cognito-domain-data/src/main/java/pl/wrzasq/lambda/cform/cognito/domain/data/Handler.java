/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.cognito.domain.data;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.DomainDescriptionType;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.cognito.domain.data.model.CognitoDomainDataRequest;
import pl.wrzasq.lambda.cform.cognito.domain.data.service.CognitoDomainHandler;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<CognitoDomainDataRequest, DomainDescriptionType> handler;

    static {
        var cognitoIdp = AWSCognitoIdentityProviderClientBuilder.defaultClient();

        var reader = new CognitoDomainHandler(cognitoIdp);

        Handler.handler = new CustomResourceHandler<>(reader::read, reader::read, reader::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<CognitoDomainDataRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
