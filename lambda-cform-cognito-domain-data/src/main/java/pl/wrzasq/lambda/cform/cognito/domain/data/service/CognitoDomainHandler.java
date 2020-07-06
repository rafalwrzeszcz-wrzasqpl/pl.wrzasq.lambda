/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.cognito.domain.data.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.DescribeUserPoolDomainRequest;
import com.amazonaws.services.cognitoidp.model.DomainDescriptionType;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.cognito.domain.data.model.CognitoDomainDataRequest;

/**
 * Cognito API implementation.
 */
public class CognitoDomainHandler {
    /**
     * AWS Cognito API client.
     */
    private AWSCognitoIdentityProvider cognitoIdp;

    /**
     * Initializes object with given Cognito client.
     *
     * @param cognitoIdp AWS Cognito client.
     */
    public CognitoDomainHandler(AWSCognitoIdentityProvider cognitoIdp) {
        this.cognitoIdp = cognitoIdp;
    }

    /**
     * Handles domain description.
     *
     * @param input Resource creation request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about created project.
     */
    public CustomResourceResponse<DomainDescriptionType> read(
        CognitoDomainDataRequest input,
        String physicalResourceId
    ) {
        var data = this.cognitoIdp.describeUserPoolDomain(
            new DescribeUserPoolDomainRequest()
                .withDomain(input.getDomain())
        )
            .getDomainDescription();

        return new CustomResourceResponse<>(
            data,
            input.getDomain()
        );
    }

    /**
     * Handles project deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<DomainDescriptionType> delete(
        CognitoDomainDataRequest input,
        String physicalResourceId
    ) {
        return new CustomResourceResponse<>(null, physicalResourceId);
    }
}
