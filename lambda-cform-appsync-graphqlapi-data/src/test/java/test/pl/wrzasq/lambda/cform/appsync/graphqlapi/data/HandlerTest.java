/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.appsync.graphqlapi.data;

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
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.Handler;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data.AppSyncGraphQlApiDataRequest;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data.AppSyncGraphQlApiDataResponse;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    private CustomResourceHandler<AppSyncGraphQlApiDataRequest, AppSyncGraphQlApiDataResponse> handler;

    @Mock
    private Context context;

    private CustomResourceHandler<AppSyncGraphQlApiDataRequest, AppSyncGraphQlApiDataResponse> originalHandler;

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
        var request = new CfnRequest<AppSyncGraphQlApiDataRequest>();
        request.setRequestType("Create");
        request.setResourceProperties(new AppSyncGraphQlApiDataRequest());

        new Handler().handle(request, this.context);

        Mockito.verify(this.handler).handle(request, this.context);
    }

    private CustomResourceHandler<AppSyncGraphQlApiDataRequest, AppSyncGraphQlApiDataResponse> setHandler(
        CustomResourceHandler<AppSyncGraphQlApiDataRequest, AppSyncGraphQlApiDataResponse> sender
    )
        throws NoSuchFieldException, IllegalAccessException {
        var hack = Handler.class.getDeclaredField("handler");
        hack.setAccessible(true);
        var original = (CustomResourceHandler) hack.get(null);
        hack.set(null, sender);
        return original;
    }
}
