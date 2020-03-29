/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.multistagecd.model;

import lombok.Data;

/**
 * Pipeline artifact.
 */
@Data
public class PipelineArtifact {
    /**
     * Source S3 bucket (for non-checkout stage).
     */
    private Object sourceBucketName;

    /**
     * Target S3 bucket (for next stage).
     */
    private Object nextBucketName;

    /**
     * S3 artifact object key.
     */
    private Object objectKey;
}
