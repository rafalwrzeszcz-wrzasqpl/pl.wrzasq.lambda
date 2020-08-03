/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.appsync.graphqlapi.data.data;

import lombok.Data;

/**
 * AppSync GraphQL API query request.
 */
@Data
public class AppSyncGraphQlApiDataRequest {
    /**
     * GraphQL API ID.
     */
    private String apiId;
}
