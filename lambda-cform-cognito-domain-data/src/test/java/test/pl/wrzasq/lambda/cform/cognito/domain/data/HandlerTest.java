/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.cognito.domain.data;

import com.amazonaws.services.cognitoidp.model.DomainDescriptionType;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.cognito.domain.data.Handler;
import pl.wrzasq.lambda.cform.cognito.domain.data.model.CognitoDomainDataRequest;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    private CustomResourceHandler<CognitoDomainDataRequest, DomainDescriptionType> handler;

    @Mock
    private Context context;

    private CustomResourceHandler<CognitoDomainDataRequest, DomainDescriptionType> originalHandler;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        this.originalHandler = this.setHandler(this.handler);
    }

    @AfterEach
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        this.setHandler(this.originalHandler);
    }

    @Test
    public void handle() {
        var request = new CfnRequest<CognitoDomainDataRequest>();
        request.setRequestType("Create");
        request.setResourceProperties(new CognitoDomainDataRequest());

        new Handler().handle(request, this.context);

        Mockito.verify(this.handler).handle(request, this.context);
    }

    private CustomResourceHandler<CognitoDomainDataRequest, DomainDescriptionType> setHandler(
        CustomResourceHandler<CognitoDomainDataRequest, DomainDescriptionType> sender
    )
        throws NoSuchFieldException, IllegalAccessException {
        var hack = Handler.class.getDeclaredField("handler");
        hack.setAccessible(true);
        var original = (CustomResourceHandler) hack.get(null);
        hack.set(null, sender);
        return original;
    }
}
