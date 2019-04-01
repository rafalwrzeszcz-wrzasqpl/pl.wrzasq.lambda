/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.organization;

import java.lang.reflect.Field;

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
import pl.wrzasq.lambda.cform.organization.Handler;
import pl.wrzasq.lambda.cform.organization.model.OrganizationRequest;
import pl.wrzasq.lambda.cform.organization.model.OrganizationResponse;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    private CustomResourceHandler<OrganizationRequest, OrganizationResponse> handler;

    @Mock
    private Context context;

    private CustomResourceHandler<OrganizationRequest, OrganizationResponse> originalHandler;

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
        CfnRequest<OrganizationRequest> request = new CfnRequest<>();
        request.setRequestType("Create");
        request.setResourceProperties(new OrganizationRequest());

        new Handler().handle(request, this.context);

        Mockito.verify(this.handler).handle(request, this.context);
    }

    private CustomResourceHandler<OrganizationRequest, OrganizationResponse> setHandler(
        CustomResourceHandler<OrganizationRequest, OrganizationResponse> sender
    )
        throws NoSuchFieldException, IllegalAccessException {
        Field hack = Handler.class.getDeclaredField("handler");
        hack.setAccessible(true);
        CustomResourceHandler<OrganizationRequest, OrganizationResponse> original
            = CustomResourceHandler.class.cast(hack.get(null));
        hack.set(null, sender);
        return original;
    }
}
