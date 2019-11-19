/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.dynamodb.DynamoDbMapperFactory;

@ExtendWith(MockitoExtension.class)
public class DynamoDbMapperFactoryTest {
    @DynamoDBTable(tableName = "")
    @Data
    public static class Document {
        @DynamoDBHashKey(attributeName = DynamoDbMapperFactoryTest.HASH_KEY)
        private String name;
    }

    private static final String TABLE_NAME = "Test";

    private static final String HASH_KEY = "Name";

    @Mock
    private AmazonDynamoDB dynamoDb;

    @Captor
    private ArgumentCaptor<PutItemRequest> putRequest;

    @Test
    public void overrideTableName() throws NoSuchFieldException, IllegalAccessException {
        // just for code coverage
        new DynamoDbMapperFactory();
        DynamoDbMapperFactory.createEncryptionDynamoDbMapper(DynamoDbMapperFactoryTest.TABLE_NAME, "arn:aws:kms:foo");
        var mapper = DynamoDbMapperFactory.createDynamoDbMapper(DynamoDbMapperFactoryTest.TABLE_NAME);

        var hack = DynamoDBMapper.class.getDeclaredField("db");
        hack.setAccessible(true);
        hack.set(mapper, this.dynamoDb);

        var model = new Document();
        model.setName("test");

        mapper.save(model);

        Mockito
            .verify(this.dynamoDb)
            .putItem(this.putRequest.capture());

        Assertions.assertEquals(
            DynamoDbMapperFactoryTest.TABLE_NAME,
            this.putRequest.getValue().getTableName(),
            "DynamoDbMapperFactory.createDynamoDbMapper() should create mapper that overrides table name."
        );
    }
}
