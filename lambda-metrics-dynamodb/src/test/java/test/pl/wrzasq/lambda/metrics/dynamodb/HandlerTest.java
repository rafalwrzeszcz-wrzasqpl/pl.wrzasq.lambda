/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.metrics.dynamodb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.metrics.dynamodb.Handler;
import pl.wrzasq.lambda.metrics.dynamodb.model.TableMetricRequest;
import pl.wrzasq.lambda.metrics.dynamodb.service.CloudWatchDynamoDbMetricGenerator;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private InputStream inputStream;

    @Mock
    private OutputStream outputStream;

    @Mock
    private CloudWatchDynamoDbMetricGenerator metricGenerator;

    @Test
    public void handle() throws IOException {
        // for code coverage
        new Handler();

        var tableName = "test";

        var handler = new Handler(
            this.objectMapper,
            this.metricGenerator
        );

        var request = new TableMetricRequest();
        request.setTableName(tableName);

        Mockito
            .when(this.objectMapper.readValue(this.inputStream, TableMetricRequest.class))
            .thenReturn(request);

        handler.handle(this.inputStream, this.outputStream);

        Mockito
            .verify(this.metricGenerator)
            .generateMetrics(tableName);
    }

    @Test
    public void handleCloseOnError() throws IOException {
        var handler = new Handler(
            this.objectMapper,
            this.metricGenerator
        );

        Mockito
            .when(this.objectMapper.readValue(this.inputStream, TableMetricRequest.class))
            .thenThrow(IOException.class);

        Assertions.assertThrows(
            IOException.class,
            () -> handler.handle(this.inputStream, this.outputStream),
            "Handler.handle() should expose exception."
        );

        Mockito.verifyZeroInteractions(this.metricGenerator);
        Mockito.verify(this.outputStream).close();
    }
}
