/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.devicefarm.project;

import com.amazonaws.services.devicefarm.model.TestGridProject;
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
import pl.wrzasq.lambda.cform.devicefarm.project.Handler;
import pl.wrzasq.lambda.cform.devicefarm.project.model.DeviceFarmProjectRequest;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    private CustomResourceHandler<DeviceFarmProjectRequest, TestGridProject> handler;

    @Mock
    private Context context;

    private CustomResourceHandler<DeviceFarmProjectRequest, TestGridProject> originalHandler;

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
        var request = new CfnRequest<DeviceFarmProjectRequest>();
        request.setRequestType("Create");
        request.setResourceProperties(new DeviceFarmProjectRequest());

        new Handler().handle(request, this.context);

        Mockito.verify(this.handler).handle(request, this.context);
    }

    private CustomResourceHandler<DeviceFarmProjectRequest, TestGridProject> setHandler(
        CustomResourceHandler<DeviceFarmProjectRequest, TestGridProject> sender
    )
        throws NoSuchFieldException, IllegalAccessException {
        var hack = Handler.class.getDeclaredField("handler");
        hack.setAccessible(true);
        var original = (CustomResourceHandler) hack.get(null);
        hack.set(null, sender);
        return original;
    }
}
