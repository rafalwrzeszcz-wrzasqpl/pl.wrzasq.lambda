/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 - 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.edgedeploy.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.lambda.model.TracingMode;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.edgedeploy.model.EdgeDeployRequest;
import pl.wrzasq.lambda.edgedeploy.service.LambdaEdgeManager;
import pl.wrzasq.lambda.edgedeploy.zip.ZipBuilder;
import pl.wrzasq.lambda.json.ObjectMapperFactory;

@ExtendWith(MockitoExtension.class)
public class LambdaEdgeManagerTest {
    private static final String FUNCTION_NAME = "test";

    private static final String FUNCTION_DESCRIPTION = "Test function.";

    private static final Runtime RUNTIME = Runtime.Nodejs810;

    private static final String HANDLER = "index.handler";

    private static final int MEMORY = 1024;

    private static final int TIMEOUT = 30;

    private static final String ROLE_ARN = "arn:iam:test";

    private static final TracingMode TRACING_MODE = TracingMode.Active;

    private static final String PACKAGE_BUCKET = "bucket-test";

    private static final String PACKAGE_KEY = "maven/release/pl/wrzasq/lambda.zip";

    private static final String MASTER_ARN = "arn:aws:lambda:test";

    private static final String VARIABLE_1_KEY = "id";

    private static final String VARIABLE_1_VALUE = "foo";

    private static final String VARIABLE_2_KEY = "content";

    private static final String VARIABLE_2_VALUE = "Bar";

    @Mock
    private AWSLambda lambda;

    @Mock
    private AmazonS3 s3;

    @Mock
    private S3ObjectInputStream s3ObjectInputStream;

    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    @Captor
    ArgumentCaptor<CreateFunctionRequest> createRequest;

    @Captor
    ArgumentCaptor<UpdateFunctionCodeRequest> updateCodeRequest;

    @Captor
    ArgumentCaptor<UpdateFunctionConfigurationRequest> updateConfigurationRequest;

    @Captor
    ArgumentCaptor<DeleteFunctionRequest> deleteRequest;

    @Captor
    ArgumentCaptor<PublishVersionRequest> publishRequest;

    @BeforeEach
    public void setUp()
    {
        this.objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    }

    @Test
    public void create() throws IOException {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        EdgeDeployRequest input = this.buildRequest();

        Map<String, Object> variables = new HashMap<>();
        variables.put(LambdaEdgeManagerTest.VARIABLE_1_KEY, LambdaEdgeManagerTest.VARIABLE_1_VALUE);
        variables.put(LambdaEdgeManagerTest.VARIABLE_2_KEY, LambdaEdgeManagerTest.VARIABLE_2_VALUE);
        input.setConfig(variables);

        ZipBuilder zip = new ZipBuilder();
        zip.writeEntry("index.js", new ByteArrayInputStream(new byte[]{'e', 'x', 'p', 'o', 'r', 't', '{', '}'}));
        ByteBuffer buffer = zip.dump();

        Mockito
            .when(this.s3.getObject(LambdaEdgeManagerTest.PACKAGE_BUCKET, LambdaEdgeManagerTest.PACKAGE_KEY))
            .thenReturn(this.buildS3Object(new ByteArrayInputStream(buffer.array())));

        Mockito
            .when(this.lambda.publishVersion(this.publishRequest.capture()))
            .thenReturn(
                new PublishVersionResult()
                    .withMasterArn(LambdaEdgeManagerTest.MASTER_ARN)
            );

        CustomResourceResponse<PublishVersionResult> response = manager.create(input, null);

        Mockito.verify(this.lambda).createFunction(this.createRequest.capture());

        CreateFunctionRequest createRequest = this.createRequest.getValue();
        PublishVersionRequest publishRequest = this.publishRequest.getValue();

        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            createRequest.getFunctionName(),
            "LambdaEdgeManager.create() should request creation of function with given stackSetName."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_DESCRIPTION,
            createRequest.getDescription(),
            "LambdaEdgeManager.create() should request creation of function with given description."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.RUNTIME.toString(),
            createRequest.getRuntime(),
            "LambdaEdgeManager.create() should request creation of function with given runtime."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.HANDLER,
            createRequest.getHandler(),
            "LambdaEdgeManager.create() should request creation of function with given handler."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.MEMORY,
            createRequest.getMemorySize().intValue(),
            "LambdaEdgeManager.create() should request creation of function with given memory limit."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.TIMEOUT,
            createRequest.getTimeout().intValue(),
            "LambdaEdgeManager.create() should request creation of function with given timeout."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.ROLE_ARN,
            createRequest.getRole(),
            "LambdaEdgeManager.create() should request creation of function with given role."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.TRACING_MODE.name(),
            createRequest.getTracingConfig().getMode(),
            "LambdaEdgeManager.create() should request creation of function with given tracing mode."
        );

        try (
            ZipInputStream stream = new ZipInputStream(
                new ByteArrayInputStream(createRequest.getCode().getZipFile().array())
            )
        ) {
            boolean hasIndex = false;
            boolean hasConfig = false;

            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                switch (entry.getName()) {
                    case "index.js":
                        hasIndex = true;

                        Scanner scanner = new Scanner(stream);

                        Assertions.assertEquals(
                            "export{}",
                            scanner.next(),
                            "LambdaEdgeManager.create() should write file content."
                        );
                        break;

                    case "config.json":
                        hasConfig = true;
                        Map<String, Object> config = this.objectMapper.readValue(
                            stream,
                            new TypeReference<Map<String, Object>>() {}
                        );

                        Assertions.assertEquals(
                            2,
                            config.size(),
                            "LambdaEdgeManager.create() should save all configuration variables."
                        );
                        Assertions.assertTrue(
                            config.containsKey(LambdaEdgeManagerTest.VARIABLE_1_KEY),
                            "LambdaEdgeManager.create() should save all configuration variables."
                        );
                        Assertions.assertEquals(
                            LambdaEdgeManagerTest.VARIABLE_1_VALUE,
                            config.get(LambdaEdgeManagerTest.VARIABLE_1_KEY),
                            "LambdaEdgeManager.create() should save all configuration variables."
                        );
                        Assertions.assertTrue(
                            config.containsKey(LambdaEdgeManagerTest.VARIABLE_2_KEY),
                            "LambdaEdgeManager.create() should save all configuration variables."
                        );
                        Assertions.assertEquals(
                            LambdaEdgeManagerTest.VARIABLE_2_VALUE,
                            config.get(LambdaEdgeManagerTest.VARIABLE_2_KEY),
                            "LambdaEdgeManager.create() should save all configuration variables."
                        );

                        break;

                    default:
                        Assertions.fail(
                            String.format(
                                "LambdaEdgeManager.create() should not add any other files than config.json - %s found.",
                                entry.getName()
                            )
                        );
                }
            }

            Assertions.assertTrue(
                hasIndex && hasConfig,
                "LambdaEdgeManager.create() should build package with content file and config file."
            );
        }

        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            publishRequest.getFunctionName(),
            "LambdaEdgeManager.create() should request new function version publication."
        );

        Assertions.assertEquals(
            LambdaEdgeManagerTest.MASTER_ARN,
            response.getPhysicalResourceId(),
            "LambdaEdgeManager.create() should set master function ARN as it's physical ID."
        );
    }

    @Test
    public void createZipIoException() throws IOException {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        EdgeDeployRequest input = this.buildRequest();

        Mockito
            .when(this.s3.getObject(LambdaEdgeManagerTest.PACKAGE_BUCKET, LambdaEdgeManagerTest.PACKAGE_KEY))
            .thenReturn(this.buildS3Object(this.s3ObjectInputStream));

        Mockito
            .when(this.s3ObjectInputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt()))
            .thenThrow(IOException.class);

        Assertions.assertThrows(
            RuntimeException.class,
            () -> manager.create(input, null),
            "LambdaEdgeManager.create() should throw exception when processing target ZIP package fails."
        );
    }

    @Test
    public void update() throws IOException {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        String configFile = "data.json";

        EdgeDeployRequest input = this.buildRequest();
        input.setConfigFile(configFile);
        input.setConfig(new HashMap<String, Object>());

        ZipBuilder zip = new ZipBuilder();
        zip.writeEntry("index.js", new ByteArrayInputStream(new byte[]{'e', 'x', 'p', 'o', 'r', 't', '{', '}'}));
        ByteBuffer buffer = zip.dump();

        Mockito
            .when(this.s3.getObject(LambdaEdgeManagerTest.PACKAGE_BUCKET, LambdaEdgeManagerTest.PACKAGE_KEY))
            .thenReturn(this.buildS3Object(new ByteArrayInputStream(buffer.array())));

        Mockito
            .when(this.lambda.publishVersion(this.publishRequest.capture()))
            .thenReturn(
                new PublishVersionResult()
                    .withMasterArn(LambdaEdgeManagerTest.MASTER_ARN)
            );

        CustomResourceResponse<PublishVersionResult> response = manager.update(input, null);

        Mockito.verify(this.lambda).updateFunctionCode(this.updateCodeRequest.capture());
        Mockito.verify(this.lambda).updateFunctionConfiguration(this.updateConfigurationRequest.capture());

        UpdateFunctionCodeRequest updateCodeRequest = this.updateCodeRequest.getValue();
        UpdateFunctionConfigurationRequest updateConfigurationRequest = this.updateConfigurationRequest.getValue();
        PublishVersionRequest publishRequest = this.publishRequest.getValue();

        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            updateConfigurationRequest.getFunctionName(),
            "LambdaEdgeManager.update() should request update of given function by it's stackSetName."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_DESCRIPTION,
            updateConfigurationRequest.getDescription(),
            "LambdaEdgeManager.update() should request update of function with given description."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.RUNTIME.toString(),
            updateConfigurationRequest.getRuntime(),
            "LambdaEdgeManager.update() should request update of function with given runtime."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.HANDLER,
            updateConfigurationRequest.getHandler(),
            "LambdaEdgeManager.update() should request update of function with given handler."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.MEMORY,
            updateConfigurationRequest.getMemorySize().intValue(),
            "LambdaEdgeManager.update() should request update of function with given memory limit."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.TIMEOUT,
            updateConfigurationRequest.getTimeout().intValue(),
            "LambdaEdgeManager.update() should request update of function with given timeout."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.ROLE_ARN,
            updateConfigurationRequest.getRole(),
            "LambdaEdgeManager.update() should request update of function with given role."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.TRACING_MODE.name(),
            updateConfigurationRequest.getTracingConfig().getMode(),
            "LambdaEdgeManager.update() should request update of function with given tracing mode."
        );

        try (
            ZipInputStream stream = new ZipInputStream(
                new ByteArrayInputStream(updateCodeRequest.getZipFile().array())
            )
        ) {
            boolean hasIndex = false;
            boolean hasConfig = false;

            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                switch (entry.getName()) {
                    case "index.js":
                        hasIndex = true;
                        break;

                    case "data.json":
                        hasConfig = true;
                        break;

                    default:
                        Assertions.fail(
                            String.format(
                                "LambdaEdgeManager.update() should not add any other files than config.json - %s found.",
                                entry.getName()
                            )
                        );
                }
            }

            Assertions.assertTrue(
                hasIndex && hasConfig,
                "LambdaEdgeManager.update() should build package with content file and config file."
            );
        }

        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            publishRequest.getFunctionName(),
            "LambdaEdgeManager.update() should request new function version publication."
        );

        Assertions.assertEquals(
            LambdaEdgeManagerTest.MASTER_ARN,
            response.getPhysicalResourceId(),
            "LambdaEdgeManager.update() should set master function ARN as it's physical ID."
        );
    }

    @Test
    public void delete() {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        EdgeDeployRequest input = this.buildRequest();

        Mockito
            .when(this.lambda.deleteFunction(this.deleteRequest.capture()))
            .thenReturn(null);

        CustomResourceResponse<PublishVersionResult> result = manager.delete(input, null);

        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            this.deleteRequest.getValue().getFunctionName(),
            "LambdaEdgeManager.delete() should attempt to delete given function."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            result.getData().getFunctionName(),
            "LambdaEdgeManager.delete() should return deleted function stackSetName."
        );
    }

    @Test
    public void deleteNotFound() {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        EdgeDeployRequest input = this.buildRequest();

        Mockito
            .when(this.lambda.deleteFunction(this.deleteRequest.capture()))
            .thenThrow(ResourceNotFoundException.class);

        CustomResourceResponse<PublishVersionResult> result = manager.delete(input, null);

        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            this.deleteRequest.getValue().getFunctionName(),
            "LambdaEdgeManager.delete() should attempt to delete given function."
        );
        Assertions.assertEquals(
            LambdaEdgeManagerTest.FUNCTION_NAME,
            result.getData().getFunctionName(),
            "LambdaEdgeManager.delete() should return function stackSetName even if didn't exist."
        );
    }

    private EdgeDeployRequest buildRequest() {
        EdgeDeployRequest request = new EdgeDeployRequest();
        request.setFunctionName(LambdaEdgeManagerTest.FUNCTION_NAME);
        request.setFunctionDescription(LambdaEdgeManagerTest.FUNCTION_DESCRIPTION);
        request.setRuntime(LambdaEdgeManagerTest.RUNTIME);
        request.setHandler(LambdaEdgeManagerTest.HANDLER);
        request.setMemory(LambdaEdgeManagerTest.MEMORY);
        request.setTimeout(LambdaEdgeManagerTest.TIMEOUT);
        request.setRoleArn(LambdaEdgeManagerTest.ROLE_ARN);
        request.setTracingMode(LambdaEdgeManagerTest.TRACING_MODE);
        request.setPackageBucket(LambdaEdgeManagerTest.PACKAGE_BUCKET);
        request.setPackageKey(LambdaEdgeManagerTest.PACKAGE_KEY);
        return request;
    }

    private S3Object buildS3Object(InputStream inputStream) {
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(inputStream);
        return s3Object;
    }
}
