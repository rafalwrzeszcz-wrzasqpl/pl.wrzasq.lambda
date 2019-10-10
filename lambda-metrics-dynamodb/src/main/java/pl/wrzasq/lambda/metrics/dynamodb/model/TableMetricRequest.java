/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.metrics.dynamodb.model;

import lombok.Data;

/**
 * Request specifying subject table.
 */
@Data
public class TableMetricRequest {
    /**
     * DynamoDb table name.
     */
    private String tableName;
}
