/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.appsync.graphqlapi.data.service;

import java.util.Collections;

import com.amazonaws.services.appsync.AWSAppSync;
import com.amazonaws.services.appsync.model.GetGraphqlApiRequest;
import com.amazonaws.services.appsync.model.GetGraphqlApiResult;
import com.amazonaws.services.appsync.model.GraphqlApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data.AppSyncGraphQlApiDataRequest;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.service.AppSyncGraphQlApiHandler;

@ExtendWith(MockitoExtension.class)
public class AppSyncGraphQlApiHandlerTest {
    private static final String API_ID = "123abc456";

    @Mock
    private AWSAppSync appSync;

    @Captor
    ArgumentCaptor<GetGraphqlApiRequest> getRequest;

    @Test
    public void read() {
        var description = new GraphqlApi();
        description.setUris(Collections.singletonMap("GRAPHQL", "https://foo/graphql"));

        var manager = new AppSyncGraphQlApiHandler(this.appSync);

        var input = new AppSyncGraphQlApiDataRequest();
        input.setApiId(AppSyncGraphQlApiHandlerTest.API_ID);

        Mockito
            .when(this.appSync.getGraphqlApi(this.getRequest.capture()))
            .thenReturn(
                new GetGraphqlApiResult()
                    .withGraphqlApi(description)
            );

        var result = manager.read(input, null);

        Mockito.verify(this.appSync).getGraphqlApi(Mockito.any(GetGraphqlApiRequest.class));

        Assertions.assertEquals(
            AppSyncGraphQlApiHandlerTest.API_ID,
            this.getRequest.getValue().getApiId(),
            "AppSyncGraphQlApiHandler.read() should request information about specified API."
        );
        Assertions.assertEquals(
            "foo",
            result.getData().getDomainName(),
            "AppSyncGraphQlApiHandler.read() should return domain name without schema and path parts."
        );
        Assertions.assertEquals(
            AppSyncGraphQlApiHandlerTest.API_ID,
            result.getPhysicalResourceId(),
            "AppSyncGraphQlApiHandler.read() should return GraphQL API ID as physical identifier."
        );
    }

    @Test
    public void delete() {
        var manager = new AppSyncGraphQlApiHandler(this.appSync);

        manager.delete(null, AppSyncGraphQlApiHandlerTest.API_ID);

        Mockito.verifyNoInteractions(this.appSync);
    }
}
