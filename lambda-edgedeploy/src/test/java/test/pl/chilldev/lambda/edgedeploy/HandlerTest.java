/*
 * This file is part of the ChillDev-Lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.chilldev.lambda.edgedeploy;

import java.lang.reflect.Field;

import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import pl.chilldev.commons.aws.cloudformation.CustomResourceHandler;
import pl.chilldev.lambda.edgedeploy.Handler;
import pl.chilldev.lambda.edgedeploy.model.EdgeDeployRequest;

public class HandlerTest
{
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private CustomResourceHandler<EdgeDeployRequest, PublishVersionResult> handler;

    @Mock
    private Context context;

    private CustomResourceHandler<EdgeDeployRequest, PublishVersionResult> originalHandler;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException
    {
        this.originalHandler = this.setHandler(this.handler);
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException
    {
        this.setHandler(this.originalHandler);
    }

    @Test
    public void handle()
    {
        CfnRequest<EdgeDeployRequest> request = new CfnRequest<>();
        request.setRequestType("Create");
        request.setResourceProperties(new EdgeDeployRequest());

        new Handler().handle(request, this.context);

        Mockito.verify(this.handler).handle(request, this.context);
    }

    private CustomResourceHandler<EdgeDeployRequest, PublishVersionResult> setHandler(
        CustomResourceHandler<EdgeDeployRequest, PublishVersionResult> sender
    )
        throws NoSuchFieldException, IllegalAccessException
    {
        Field hack = Handler.class.getDeclaredField("handler");
        hack.setAccessible(true);
        CustomResourceHandler<EdgeDeployRequest, PublishVersionResult> original
            = CustomResourceHandler.class.cast(hack.get(null));
        hack.set(null, sender);
        return original;
    }
}
