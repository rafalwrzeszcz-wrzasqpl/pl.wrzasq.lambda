/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.AttributeEncryptor;
import com.amazonaws.services.dynamodbv2.datamodeling.AttributeTransformer;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.encryption.providers.DirectKmsMaterialProvider;
import com.amazonaws.services.kms.AWSKMSClientBuilder;

/**
 * Default Jackson ObjectMapper provider.
 */
public class DynamoDbMapperFactory {
    /**
     * Creates DynamoDB mapper with encryption handling.
     *
     * @param tableName DynamoDB table name.
     * @param keyId KMS key ARN.
     * @return DynamoDB mapping client.
     */
    public static DynamoDBMapper createEncryptionDynamoDbMapper(String tableName, String keyId) {
        var kms = AWSKMSClientBuilder.standard()
            .build();
        var encryptionMaterialsProvider = new DirectKmsMaterialProvider(kms, keyId);

        return DynamoDbMapperFactory.createDynamoDbMapper(
            tableName,
            new AttributeEncryptor(encryptionMaterialsProvider)
        );
    }

    /**
     * Creates DynamoDB mapper.
     *
     * @param tableName DynamoDB table name.
     * @return DynamoDB mapping client.
     */
    public static DynamoDBMapper createDynamoDbMapper(String tableName) {
        return DynamoDbMapperFactory.createDynamoDbMapper(tableName, null);
    }

    /**
     * Creates DynamoDB mapper.
     *
     * @param tableName DynamoDB table name.
     * @param attributeTransformer Custom attributes transformation logic.
     * @return DynamoDB mapping client.
     */
    private static DynamoDBMapper createDynamoDbMapper(String tableName, AttributeTransformer attributeTransformer) {
        var dynamoDb = AmazonDynamoDBClientBuilder.standard().build();
        var mapperConfig = DynamoDBMapperConfig.builder()
            .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(tableName))
            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.PUT)
            .build();
        return new DynamoDBMapper(dynamoDb, mapperConfig, attributeTransformer);
    }
}
