/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.metrics.dynamodb.service;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.metrics.dynamodb.service.CloudWatchDynamoDbMetricGenerator;

@ExtendWith(MockitoExtension.class)
public class CloudWatchDynamoDbMetricGeneratorTest {
    @Mock
    private AmazonDynamoDB dynamoDb;

    @Mock
    private AmazonCloudWatch cloudWatch;

    @Captor
    private ArgumentCaptor<PutMetricDataRequest> putMetricDataRequest;

    @Test
    public void generateMetrics() {
        var tableName = "Orders";
        var namespace = "Test/DynamoDB";

        var itemCount = 10L;
        var tableSizeBytes = 256L;

        var metricGenerator = new CloudWatchDynamoDbMetricGenerator(
            this.dynamoDb,
            this.cloudWatch,
            namespace
        );

        var table = new TableDescription()
            .withItemCount(itemCount)
            .withTableSizeBytes(tableSizeBytes);

        Mockito
            .when(this.dynamoDb.describeTable(tableName))
            .thenReturn(new DescribeTableResult().withTable(table));

        metricGenerator.generateMetrics(tableName);

        Mockito
            .verify(this.cloudWatch, Mockito.times(2))
            .putMetricData(this.putMetricDataRequest.capture());

        var requests = this.putMetricDataRequest.getAllValues();

        Assertions.assertEquals(
            namespace,
            requests.get(0).getNamespace(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should put metrics in specified namespace."
        );
        Assertions.assertEquals(
            namespace,
            requests.get(1).getNamespace(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should put metrics in specified namespace."
        );

        var metric = requests.get(0).getMetricData().get(0);

        Assertions.assertEquals(
            itemCount,
            metric.getValue(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should save count of documents in table."
        );
        Assertions.assertEquals(
            StandardUnit.None.toString(),
            metric.getUnit(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should pick metric unit."
        );
        Assertions.assertEquals(
            "ItemCount",
            metric.getMetricName(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should set metric name."
        );
        Assertions.assertEquals(
            "TableName",
            metric.getDimensions().get(0).getName(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should set table name dimension."
        );
        Assertions.assertEquals(
            tableName,
            metric.getDimensions().get(0).getValue(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should set table name as dimension value."
        );

        metric = requests.get(1).getMetricData().get(0);

        Assertions.assertEquals(
            tableSizeBytes,
            metric.getValue(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should save table storage size."
        );
        Assertions.assertEquals(
            StandardUnit.Bytes.toString(),
            metric.getUnit(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should pick metric unit."
        );
        Assertions.assertEquals(
            "TableSizeBytes",
            metric.getMetricName(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should set metric name."
        );
        Assertions.assertEquals(
            "TableName",
            metric.getDimensions().get(0).getName(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should set table name dimension."
        );
        Assertions.assertEquals(
            tableName,
            metric.getDimensions().get(0).getValue(),
            "CloudWatchDynamoDbMetricGenerator.generateMetrics() should set table name as dimension value."
        );
    }
}
