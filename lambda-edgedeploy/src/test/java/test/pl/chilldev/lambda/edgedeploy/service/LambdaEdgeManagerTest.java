/*
 * This file is part of the ChillDev-Lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.chilldev.lambda.edgedeploy.service;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import pl.chilldev.commons.aws.cloudformation.CustomResourceResponse;
import pl.chilldev.lambda.edgedeploy.model.EdgeDeployRequest;
import pl.chilldev.lambda.edgedeploy.service.LambdaEdgeManager;
import pl.chilldev.lambda.edgedeploy.zip.ZipBuilder;
import pl.chilldev.lambda.json.ObjectMapperFactory;

public class LambdaEdgeManagerTest
{
    private static final String FUNCTION_NAME = "test";

    private static final String FUNCTION_DESCRIPTION = "Test function.";

    private static final Runtime RUNTIME = Runtime.Nodejs810;

    private static final String HANDLER = "index.handler";

    private static final int MEMORY = 1024;

    private static final int TIMEOUT = 30;

    private static final String ROLE_ARN = "arn:iam:test";

    private static final TracingMode TRACING_MODE = TracingMode.Active;

    private static final String PACKAGE_BUCKET = "bucket-test";

    private static final String PACKAGE_KEY = "maven/release/pl/chilldev/lambda.zip";

    private static final String MASTER_ARN = "arn:aws:lambda:test";

    private static final String VARIABLE_1_KEY = "id";

    private static final String VARIABLE_1_VALUE = "foo";

    private static final String VARIABLE_2_KEY = "content";

    private static final String VARIABLE_2_VALUE = "Bar";

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

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

    @Before
    public void setUp()
    {
        this.objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    }

    @Test
    public void create() throws IOException
    {
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

        CustomResourceResponse<PublishVersionResult> response = manager.create(input);

        Mockito.verify(this.lambda).createFunction(this.createRequest.capture());

        CreateFunctionRequest createRequest = this.createRequest.getValue();
        PublishVersionRequest publishRequest = this.publishRequest.getValue();

        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given name.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            createRequest.getFunctionName()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given description.",
            LambdaEdgeManagerTest.FUNCTION_DESCRIPTION,
            createRequest.getDescription()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given runtime.",
            LambdaEdgeManagerTest.RUNTIME.toString(),
            createRequest.getRuntime()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given handler.",
            LambdaEdgeManagerTest.HANDLER,
            createRequest.getHandler()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given memory limit.",
            LambdaEdgeManagerTest.MEMORY,
            createRequest.getMemorySize().intValue()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given timeout.",
            LambdaEdgeManagerTest.TIMEOUT,
            createRequest.getTimeout().intValue()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given role.",
            LambdaEdgeManagerTest.ROLE_ARN,
            createRequest.getRole()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.create() should request creation of function with given tracing mode.",
            LambdaEdgeManagerTest.TRACING_MODE.name(),
            createRequest.getTracingConfig().getMode()
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

                        Assert.assertEquals(
                            "LambdaEdgeManager.create() should write file content.",
                            "export{}",
                            scanner.next()
                        );
                        break;

                    case "config.json":
                        hasConfig = true;
                        Map<String, Object> config = this.objectMapper.readValue(
                            stream,
                            new TypeReference<Map<String, Object>>() {}
                        );

                        Assert.assertEquals(
                            "LambdaEdgeManager.create() should save all configuration variables.",
                            2,
                            config.size()
                        );
                        Assert.assertTrue(
                            "LambdaEdgeManager.create() should save all configuration variables.",
                            config.containsKey(LambdaEdgeManagerTest.VARIABLE_1_KEY)
                        );
                        Assert.assertEquals(
                            "LambdaEdgeManager.create() should save all configuration variables.",
                            LambdaEdgeManagerTest.VARIABLE_1_VALUE,
                            config.get(LambdaEdgeManagerTest.VARIABLE_1_KEY)
                        );
                        Assert.assertTrue(
                            "LambdaEdgeManager.create() should save all configuration variables.",
                            config.containsKey(LambdaEdgeManagerTest.VARIABLE_2_KEY)
                        );
                        Assert.assertEquals(
                            "LambdaEdgeManager.create() should save all configuration variables.",
                            LambdaEdgeManagerTest.VARIABLE_2_VALUE,
                            config.get(LambdaEdgeManagerTest.VARIABLE_2_KEY)
                        );

                        break;

                    default:
                        Assert.fail(
                            String.format(
                                "LambdaEdgeManager.create() should not add any other files than config.json - %s found.",
                                entry.getName()
                            )
                        );
                }
            }

            Assert.assertTrue(
                "LambdaEdgeManager.create() should build package with content file and config file.",
                hasIndex && hasConfig
            );
        }

        Assert.assertEquals(
            "LambdaEdgeManager.create() should request new function version publication.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            publishRequest.getFunctionName()
        );

        Assert.assertEquals(
            "LambdaEdgeManager.create() should set master function ARN as it's physical ID.",
            LambdaEdgeManagerTest.MASTER_ARN,
            response.getPhysicalResourceId()
        );
    }

    @Test(expected = RuntimeException.class)
    public void createZipIoException() throws IOException
    {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        EdgeDeployRequest input = this.buildRequest();

        Mockito
            .when(this.s3.getObject(LambdaEdgeManagerTest.PACKAGE_BUCKET, LambdaEdgeManagerTest.PACKAGE_KEY))
            .thenReturn(this.buildS3Object(this.s3ObjectInputStream));

        Mockito
            .when(this.s3ObjectInputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt()))
            .thenThrow(IOException.class);

        manager.create(input);
    }

    @Test
    public void update() throws IOException
    {
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

        CustomResourceResponse<PublishVersionResult> response = manager.update(input);

        Mockito.verify(this.lambda).updateFunctionCode(this.updateCodeRequest.capture());
        Mockito.verify(this.lambda).updateFunctionConfiguration(this.updateConfigurationRequest.capture());

        UpdateFunctionCodeRequest updateCodeRequest = this.updateCodeRequest.getValue();
        UpdateFunctionConfigurationRequest updateConfigurationRequest = this.updateConfigurationRequest.getValue();
        PublishVersionRequest publishRequest = this.publishRequest.getValue();

        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of given function by it's name.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            updateConfigurationRequest.getFunctionName()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of function with given description.",
            LambdaEdgeManagerTest.FUNCTION_DESCRIPTION,
            updateConfigurationRequest.getDescription()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of function with given runtime.",
            LambdaEdgeManagerTest.RUNTIME.toString(),
            updateConfigurationRequest.getRuntime()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of function with given handler.",
            LambdaEdgeManagerTest.HANDLER,
            updateConfigurationRequest.getHandler()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of function with given memory limit.",
            LambdaEdgeManagerTest.MEMORY,
            updateConfigurationRequest.getMemorySize().intValue()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of function with given timeout.",
            LambdaEdgeManagerTest.TIMEOUT,
            updateConfigurationRequest.getTimeout().intValue()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of function with given role.",
            LambdaEdgeManagerTest.ROLE_ARN,
            updateConfigurationRequest.getRole()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.update() should request update of function with given tracing mode.",
            LambdaEdgeManagerTest.TRACING_MODE.name(),
            updateConfigurationRequest.getTracingConfig().getMode()
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
                        Assert.fail(
                            String.format(
                                "LambdaEdgeManager.update() should not add any other files than config.json - %s found.",
                                entry.getName()
                            )
                        );
                }
            }

            Assert.assertTrue(
                "LambdaEdgeManager.update() should build package with content file and config file.",
                hasIndex && hasConfig
            );
        }

        Assert.assertEquals(
            "LambdaEdgeManager.update() should request new function version publication.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            publishRequest.getFunctionName()
        );

        Assert.assertEquals(
            "LambdaEdgeManager.update() should set master function ARN as it's physical ID.",
            LambdaEdgeManagerTest.MASTER_ARN,
            response.getPhysicalResourceId()
        );
    }

    @Test
    public void delete()
    {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        EdgeDeployRequest input = this.buildRequest();

        Mockito
            .when(this.lambda.deleteFunction(this.deleteRequest.capture()))
            .thenReturn(null);

        CustomResourceResponse<PublishVersionResult> result = manager.delete(input);

        Assert.assertEquals(
            "LambdaEdgeManager.delete() should attempt to delete given function.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            this.deleteRequest.getValue().getFunctionName()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.delete() should return deleted function name.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            result.getData().getFunctionName()
        );
    }

    @Test
    public void deleteNotFound()
    {
        LambdaEdgeManager manager = new LambdaEdgeManager(this.lambda, this.s3, this.objectMapper);

        EdgeDeployRequest input = this.buildRequest();

        Mockito
            .when(this.lambda.deleteFunction(this.deleteRequest.capture()))
            .thenThrow(ResourceNotFoundException.class);

        CustomResourceResponse<PublishVersionResult> result = manager.delete(input);

        Assert.assertEquals(
            "LambdaEdgeManager.delete() should attempt to delete given function.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            this.deleteRequest.getValue().getFunctionName()
        );
        Assert.assertEquals(
            "LambdaEdgeManager.delete() should return function name even if didn't exist.",
            LambdaEdgeManagerTest.FUNCTION_NAME,
            result.getData().getFunctionName()
        );
    }

    private EdgeDeployRequest buildRequest()
    {
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

    private S3Object buildS3Object(InputStream inputStream)
    {
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(inputStream);
        return s3Object;
    }
}
