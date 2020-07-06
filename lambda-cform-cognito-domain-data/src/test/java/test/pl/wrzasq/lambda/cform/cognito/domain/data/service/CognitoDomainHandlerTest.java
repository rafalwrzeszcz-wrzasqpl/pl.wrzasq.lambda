/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.cognito.domain.data.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.DescribeUserPoolDomainRequest;
import com.amazonaws.services.cognitoidp.model.DescribeUserPoolDomainResult;
import com.amazonaws.services.cognitoidp.model.DomainDescriptionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.cform.cognito.domain.data.model.CognitoDomainDataRequest;
import pl.wrzasq.lambda.cform.cognito.domain.data.service.CognitoDomainHandler;

@ExtendWith(MockitoExtension.class)
public class CognitoDomainHandlerTest {
    private static final String DOMAIN_NAME = "auth.wrzasq.pl";

    @Mock
    private AWSCognitoIdentityProvider cognitoIdp;

    @Captor
    ArgumentCaptor<DescribeUserPoolDomainRequest> describeRequest;

    @Test
    public void read() {
        var description = new DomainDescriptionType();

        var manager = new CognitoDomainHandler(this.cognitoIdp);

        var input = new CognitoDomainDataRequest();
        input.setDomain(CognitoDomainHandlerTest.DOMAIN_NAME);

        Mockito
            .when(this.cognitoIdp.describeUserPoolDomain(this.describeRequest.capture()))
            .thenReturn(
                new DescribeUserPoolDomainResult()
                    .withDomainDescription(description)
            );

        var result = manager.read(input, null);

        Mockito.verify(this.cognitoIdp).describeUserPoolDomain(Mockito.any(DescribeUserPoolDomainRequest.class));

        Assertions.assertEquals(
            CognitoDomainHandlerTest.DOMAIN_NAME,
            this.describeRequest.getValue().getDomain(),
            "CognitoDomainHandler.read() should request information about specified domain name."
        );
        Assertions.assertSame(
            description,
            result.getData(),
            "CognitoDomainHandler.read() should return domain data returned by AWS."
        );
        Assertions.assertEquals(
            CognitoDomainHandlerTest.DOMAIN_NAME,
            result.getPhysicalResourceId(),
            "CognitoDomainHandler.read() should return domain name as physical identifier."
        );
    }

    @Test
    public void delete() {
        var manager = new CognitoDomainHandler(this.cognitoIdp);

        manager.delete(null, CognitoDomainHandlerTest.DOMAIN_NAME);

        Mockito.verifyNoInteractions(this.cognitoIdp);
    }
}
