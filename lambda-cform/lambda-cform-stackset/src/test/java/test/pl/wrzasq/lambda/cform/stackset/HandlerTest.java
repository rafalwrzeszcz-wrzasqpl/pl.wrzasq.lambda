/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.stackset;

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
import pl.wrzasq.lambda.cform.stackset.Handler;
import pl.wrzasq.lambda.cform.stackset.model.StackSetRequest;
import pl.wrzasq.lambda.cform.stackset.model.StackSetResponse;

@ExtendWith(MockitoExtension.class)
public class HandlerTest
{
    @Mock
    private CustomResourceHandler<StackSetRequest, StackSetResponse> handler;

    @Mock
    private Context context;

    private CustomResourceHandler<StackSetRequest, StackSetResponse> originalHandler;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException
    {
        this.originalHandler = this.setHandler(this.handler);
    }

    @AfterEach
    public void tearDown() throws NoSuchFieldException, IllegalAccessException
    {
        this.setHandler(this.originalHandler);
    }

    @Test
    public void handle()
    {
        CfnRequest<StackSetRequest> request = new CfnRequest<>();
        request.setRequestType("Create");
        request.setResourceProperties(new StackSetRequest());

        new Handler().handle(request, this.context);

        Mockito.verify(this.handler).handle(request, this.context);
    }

    private CustomResourceHandler<StackSetRequest, StackSetResponse> setHandler(
        CustomResourceHandler<StackSetRequest, StackSetResponse> sender
    )
        throws NoSuchFieldException, IllegalAccessException
    {
        Field hack = Handler.class.getDeclaredField("handler");
        hack.setAccessible(true);
        CustomResourceHandler<StackSetRequest, StackSetResponse> original
            = CustomResourceHandler.class.cast(hack.get(null));
        hack.set(null, sender);
        return original;
    }
}
