/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.metrics.dynamodb.service;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CloudWatch metrics handler.
 */
public class CloudWatchDynamoDbMetricGenerator {
    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(CloudWatchDynamoDbMetricGenerator.class);

    /**
     * AWS DynamoDB client.
     */
    private AmazonDynamoDB dynamoDb;

    /**
     * AWS CloudWatch client.
     */
    private AmazonCloudWatch cloudWatch;

    /**
     * Metrics namespace to use.
     */
    private String namespace;

    /**
     * Initializes object.
     *
     * @param dynamoDb DynamoDB client.
     * @param cloudWatch CloudWatch client.
     * @param namespace Metrics namespace.
     */
    public CloudWatchDynamoDbMetricGenerator(AmazonDynamoDB dynamoDb, AmazonCloudWatch cloudWatch, String namespace) {
        this.dynamoDb = dynamoDb;
        this.cloudWatch = cloudWatch;
        this.namespace = namespace;
    }

    /**
     * Table metrics generator.
     *
     * @param tableName Table name.
     */
    public void generateMetrics(String tableName) {
        this.logger.info("Generating metrics for table: {}.", tableName);

        TableDescription table = this.dynamoDb.describeTable(tableName).getTable();

        this.putSingleMetric(tableName, "ItemCount", table.getItemCount(), StandardUnit.None);
        this.putSingleMetric(tableName, "TableSizeBytes", table.getTableSizeBytes(), StandardUnit.Bytes);
    }

    /**
     * Saves single metric value in CloudWatch.
     *
     * @param tableName DynamoDB table name.
     * @param metricName Metric name.
     * @param value Metric value.
     * @param unit Metric unit.
     */
    private void putSingleMetric(String tableName, String metricName, double value, StandardUnit unit) {
        this.cloudWatch.putMetricData(
            new PutMetricDataRequest()
                .withNamespace(this.namespace)
                .withMetricData(
                    new MetricDatum()
                        .withMetricName(metricName)
                        .withValue(value)
                        .withUnit(unit)
                        .withDimensions(
                            new Dimension()
                                .withName("TableName")
                                .withValue(tableName)
                        )
                )
        );
    }
}
