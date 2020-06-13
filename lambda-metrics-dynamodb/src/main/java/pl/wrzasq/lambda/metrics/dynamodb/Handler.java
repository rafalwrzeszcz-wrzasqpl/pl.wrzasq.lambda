/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.metrics.dynamodb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import pl.wrzasq.commons.json.ObjectMapperFactory;
import pl.wrzasq.lambda.metrics.dynamodb.model.TableMetricRequest;
import pl.wrzasq.lambda.metrics.dynamodb.service.CloudWatchDynamoDbMetricGenerator;

/**
 * CloudWatch Events request handler.
 *
 * <p>Required environment variables:</p>
 *
 * <dl>
 *     <dt><code>METRICS_NAMESPACE</code></dt>
 *     <dd>Namespace to use for CloudWatch metrics.</dd>
 * </dl>
 *
 * <p>Recommended memory: 256MB.</p>
 */
@AllArgsConstructor
public class Handler {
    /**
     * Metrics namespace to use.
     */
    private static final String METRICS_NAMESPACE = System.getenv("METRICS_NAMESPACE");

    /**
     * JSON handler.
     */
    private ObjectMapper objectMapper;

    /**
     * DynamoDB metrics generator.
     */
    private CloudWatchDynamoDbMetricGenerator metricGenerator;

    /**
     * Default constructor.
     */
    public Handler() {
        this(
            ObjectMapperFactory.createObjectMapper(),
            new CloudWatchDynamoDbMetricGenerator(
                AmazonDynamoDBClientBuilder.standard().build(),
                AmazonCloudWatchClientBuilder.standard().build(),
                Handler.METRICS_NAMESPACE
            )
        );
    }

    /**
     * Handles invocation.
     *
     * @param inputStream Request input.
     * @param outputStream Output stream.
     * @throws IOException When JSON loading/dumping fails.
     */
    public void handle(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (outputStream) {
            var request = this.objectMapper.readValue(inputStream, TableMetricRequest.class);

            this.metricGenerator.generateMetrics(request.getTableName());
        }
    }
}
