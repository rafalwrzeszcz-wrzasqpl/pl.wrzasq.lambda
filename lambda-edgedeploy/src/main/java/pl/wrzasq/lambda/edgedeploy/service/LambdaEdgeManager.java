/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 - 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.edgedeploy.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.TracingConfig;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.edgedeploy.model.EdgeDeployRequest;
import pl.wrzasq.lambda.edgedeploy.zip.ZipBuilder;

/**
 * Lambda API implementation.
 */
public class LambdaEdgeManager
{
    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(LambdaEdgeManager.class);

    /**
     * AWS Lambda API client.
     */
    private AWSLambda lambda;

    /**
     * AWS S3 API client.
     */
    private AmazonS3 s3;

    /**
     * JSON generator.
     */
    private ObjectMapper objectMapper;

    /**
     * Initializes object with given Lambda client.
     *
     * @param lambda AWS Lambda client.
     * @param s3 AWS S3 client.
     * @param objectMapper JSON generator.
     */
    public LambdaEdgeManager(AWSLambda lambda, AmazonS3 s3, ObjectMapper objectMapper)
    {
        this.lambda = lambda;
        this.s3 = s3;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles function creation.
     *
     * @param input Resource creation request.
     * @return Data about published version.
     */
    public CustomResourceResponse<PublishVersionResult> create(EdgeDeployRequest input)
    {
        this.lambda.createFunction(
            new CreateFunctionRequest()
                .withFunctionName(input.getFunctionName())
                .withDescription(input.getFunctionDescription())
                .withRuntime(input.getRuntime())
                .withCode(
                    new FunctionCode()
                        .withZipFile(this.buildZipFile(input))
                )
                .withHandler(input.getHandler())
                .withMemorySize(input.getMemory())
                .withTimeout(input.getTimeout())
                .withRole(input.getRoleArn())
                .withTracingConfig(
                    new TracingConfig()
                        .withMode(input.getTracingMode())
                )
        );

        return this.publishLambdaVersion(input.getFunctionName());
    }

    /**
     * Handles function update.
     *
     * @param input Resource update request.
     * @return Data about published version.
     */
    public CustomResourceResponse<PublishVersionResult> update(EdgeDeployRequest input)
    {
        this.lambda.updateFunctionCode(
            new UpdateFunctionCodeRequest()
                .withFunctionName(input.getFunctionName())
                .withZipFile(this.buildZipFile(input))
        );

        this.lambda.updateFunctionConfiguration(
            new UpdateFunctionConfigurationRequest()
                .withFunctionName(input.getFunctionName())
                .withDescription(input.getFunctionDescription())
                .withRuntime(input.getRuntime())
                .withHandler(input.getHandler())
                .withMemorySize(input.getMemory())
                .withTimeout(input.getTimeout())
                .withRole(input.getRoleArn())
                .withTracingConfig(
                    new TracingConfig()
                        .withMode(input.getTracingMode())
                )
        );

        return this.publishLambdaVersion(input.getFunctionName());
    }

    /**
     * Handles function deletion.
     *
     * @param input Resource delete request.
     * @return Data about deleted version.
     */
    public CustomResourceResponse<PublishVersionResult> delete(EdgeDeployRequest input)
    {
        try {
            this.lambda.deleteFunction(
                new DeleteFunctionRequest()
                    .withFunctionName(input.getFunctionName())
            );
        } catch (ResourceNotFoundException error) {
            this.logger.warn("Attempt to delete non-existing Lambda {}.", input.getFunctionName(), error);
        }

        return new CustomResourceResponse<>(
            new PublishVersionResult()
                .withFunctionName(input.getFunctionName())
        );
    }

    /**
     * Publishes new version of Lambda.
     *
     * @param functionName Function name.
     * @return Published version data.
     */
    private CustomResourceResponse<PublishVersionResult> publishLambdaVersion(String functionName)
    {
        PublishVersionResult result = this.lambda.publishVersion(
            new PublishVersionRequest()
                .withFunctionName(functionName)
        );

        return new CustomResourceResponse<>(result, result.getMasterArn());
    }

    /**
     * Builds deployment ZIP package.
     *
     * @param input Function setup.
     * @return ZIP file buffer.
     */
    private ByteBuffer buildZipFile(EdgeDeployRequest input)
    {
        ZipBuilder zip = new ZipBuilder();
        try (
            ZipInputStream archive = new ZipInputStream(
                this.s3.getObject(input.getPackageBucket(), input.getPackageKey()).getObjectContent()
            )
        )
        {
            // copy entire package content
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                zip.writeEntry(entry.getName(), archive);
            }

            // dump custom configuration from request
            zip.writeEntry(
                input.getConfigFile(),
                this.objectMapper.writeValueAsBytes(input.getConfig())
            );

            return zip.dump();
        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }
}
