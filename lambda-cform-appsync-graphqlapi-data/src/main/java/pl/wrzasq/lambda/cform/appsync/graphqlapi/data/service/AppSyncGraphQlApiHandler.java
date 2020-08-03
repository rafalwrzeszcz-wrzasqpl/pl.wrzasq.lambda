/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.appsync.graphqlapi.data.service;

import com.amazonaws.services.appsync.AWSAppSync;
import com.amazonaws.services.appsync.model.GetGraphqlApiRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data.AppSyncGraphQlApiDataRequest;
import pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data.AppSyncGraphQlApiDataResponse;

/**
 * AppSync API implementation.
 */
public class AppSyncGraphQlApiHandler {
    /**
     * Schema URI prefix.
     */
    private static final String APPSYNC_API_URI_PREFIX = "https://";

    /**
     * Path URI suffix.
     */
    private static final String APPSYNC_API_URI_SUFFIX = "/graphql";

    /**
     * AWS AppSync API client.
     */
    private AWSAppSync appSync;

    /**
     * Initializes object with given AppSync client.
     *
     * @param appSync AWS AppSync client.
     */
    public AppSyncGraphQlApiHandler(AWSAppSync appSync) {
        this.appSync = appSync;
    }

    /**
     * Handles domain description.
     *
     * @param input Resource creation request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about created project.
     */
    public CustomResourceResponse<AppSyncGraphQlApiDataResponse> read(
        AppSyncGraphQlApiDataRequest input,
        String physicalResourceId
    ) {
        var data = this.appSync.getGraphqlApi(
            new GetGraphqlApiRequest()
                .withApiId(input.getApiId())
        )
            .getGraphqlApi();

        return new CustomResourceResponse<>(
            new AppSyncGraphQlApiDataResponse(
                AppSyncGraphQlApiHandler.extractDomainFromUri(data.getUris().get("GRAPHQL"))
            ),
            input.getApiId()
        );
    }

    /**
     * Handles project deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<AppSyncGraphQlApiDataResponse> delete(
        AppSyncGraphQlApiDataRequest input,
        String physicalResourceId
    ) {
        return new CustomResourceResponse<>(null, physicalResourceId);
    }

    /**
     * Extracts domain from API URL.
     *
     * @param uri Full API URI.
     * @return Plain domain name.
     */
    private static String extractDomainFromUri(String uri) {
        return uri.substring(
            AppSyncGraphQlApiHandler.APPSYNC_API_URI_PREFIX.length(),
            uri.length() - AppSyncGraphQlApiHandler.APPSYNC_API_URI_SUFFIX.length()
        );
    }
}
