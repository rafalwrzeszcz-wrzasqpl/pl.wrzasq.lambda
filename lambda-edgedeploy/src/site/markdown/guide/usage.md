<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2018 - 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

Purpose of this **Lambda** is to allow deploying any other package in `us-east-1` region (regardless of your current
execution region). This allows to use desired package as **Lambda@Edge** handler in
[**CloudFront**](https://aws.amazon.com/cloudfront/). Additionally, during deployment ZIP package it injects additional
file with deployment time configuration.

# Required permissions

`edgedeploy` Lambda needs following permissions:

-   read access to **S3** location where your deployment package is located;
-   Lambda management permissions;
-   `iam:PassRole` permission to pass role for the target **Lambda@Edge**.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `functionName` (required) - string

Specified destination function name.

**Note:** Even though **Lambda** is region-scoped, keep in mind that deploying **Lambda@Edge** always targets
`us-east-1`, which means it's good idea to use region name (eg. `AWS::Region`) in function name.

## `functionDescription` - string

Function description.

## `roleArn` (required) - ARN

[IAM role](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-iam-role.html) **ARN**.

## `runtime` - string

**Node.js** runtime to use (keep in mind that **Lambda@Edge** only supports **Node.js** runtimes). By default **8.10**
is used.

## `handler` (required) - string

Entry point handler for invoking function.

## `memory` (required) - integer

Memory (in MBs) allocated for target function.

## `timeout` (required) - integer

Timeout (in seconds) for single invocation.

## `tracingMode` - string

**X-Ray** tracing mode. Can be either `PassThrough` (default value) or `Active`.

## `packageBucket` (required) - string

**S3** bucket from which your destination Lambda package is located.

## `packageKey` (required) - string

Key, where your target Lambda package is stored in **S3**.

## `configFile` - string

Name under which deployment configuration will be exposed in your package (by default it's `config.json`).

## `config` - key-value object

Any configuration options that should be added at deploy-time to your package.

# Output values

Deploy handler exposes entire
[PublishVersionResult](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/lambda/model/PublishVersionResult.html)
object. Most important from it is a `FunctionArn` property, which refers to a **published version of Lambda** (which
means it includes version classifier). It's the property that you can use to assign published version as **CloudFront**
association (see example below).

**Note:** Custom resource physical ID is set as deployed function
[ARN](https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html) (master function, not published
version).

# Example

```yaml
    CloudFrontDistribution:
        Type: "AWS::CloudFront::Distribution"
        Properties:
            DistributionConfig:
                Origins:
                    -
                        Id: "frontend-service"
                        # origin configuration
                DefaultCacheBehavior:
                    TargetOriginId: "frontend-service"
                    AllowedMethods:
                        - "DELETE"
                        - "GET"
                        - "HEAD"
                        - "OPTIONS"
                        - "PATCH"
                        - "POST"
                        - "PUT"
                    CachedMethods:
                        - "GET"
                        - "HEAD"
                        - "OPTIONS"
                    Compress: false
                    ViewerProtocolPolicy: "allow-all"
                    LambdaFunctionAssociations:
                        -
                            EventType: "origin-request"
                            # here is the Lambda@Edge integration point
                            LambdaFunctionARN: !GetAtt "EdgeFunction.FunctionArn"
                Enabled: true

    # this role will be used by target Lambda - Lambda@Edge
    EdgeFunctionRole:
        Type: "AWS::IAM::Role"
        Properties:
            AssumeRolePolicyDocument:
                Statement:
                    -
                        Action: "sts:AssumeRole"
                        Effect: "Allow"
                        Principal:
                            Service:
                                - "edgelambda.amazonaws.com"
                                - "lambda.amazonaws.com"
            ManagedPolicyArns:
                - "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"

    # this role will be used by deploy Lambda
    EdgeDeployRole:
        Type: "AWS::IAM::Role"
        Properties:
            AssumeRolePolicyDocument:
                Statement:
                    -
                        Action: "sts:AssumeRole"
                        Effect: "Allow"
                        Principal:
                            Service:
                                - "lambda.amazonaws.com"
            Policies:
                -
                    PolicyName: "AllowManagingLambdas"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "s3:GetBucketLocation"
                                    - "s3:GetObject"
                                    - "s3:ListBucket"
                                Effect: "Allow"
                                Resource:
                                    # put your source bucket here
                                    - "arn:aws:s3:::chilldev-repository"
                                    - "arn:aws:s3:::chilldev-repository/*"
                -
                    PolicyName: "AllowManagingLambdas"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "iam:CreateServiceLinkedRole"
                                    - "lambda:CreateFunction"
                                    - "lambda:ListTags"
                                    - "lambda:TagResource"
                                    - "lambda:UntagResource"
                                    - "lambda:UpdateFunctionConfiguration"
                                Effect: "Allow"
                                Resource:
                                    - !Sub "*"
                -
                    PolicyName: "AllowManagingEdgeLambdas"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "lambda:AddPermission"
                                    - "lambda:CreateAlias"
                                    - "lambda:DeleteAlias"
                                    - "lambda:DeleteFunction"
                                    - "lambda:GetAlias"
                                    - "lambda:GetFunction"
                                    - "lambda:GetFunctionConfiguration"
                                    - "lambda:GetPolicy"
                                    - "lambda:ListVersionsByFunction"
                                    - "lambda:PublishVersion"
                                    - "lambda:RemovePermission"
                                    - "lambda:UpdateAlias"
                                    - "lambda:UpdateFunctionCode"
                                Effect: "Allow"
                                Resource:
                                    - !Sub "arn:aws:lambda:us-east-1:${AWS::AccountId}:function:*"
                -
                    PolicyName: "AllowPassingEdgeRole"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "iam:PassRole"
                                Effect: "Allow"
                                Resource:
                                    - !GetAtt "EdgeFunctionRole.Arn"

    EdgeDeploy:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-edgedeploy-1.0.1-standalone.jar"
            Handler: "pl.wrzasq.lambda.edgedeploy.Handler::handle"
            MemorySize: 256
            Description: "Lambda@Edge deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "EdgeDeployRole.Arn"

    EdgeFunction:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "EdgeDeploy.Arn"
            functionName: !Sub "${AWS::Region}-edge"
            # reference to destination role
            roleArn: !GetAtt "EdgeFunctionRole.Arn"
            handler: "index.handler"
            memory: 256
            timeout: 30
            packageBucket: "your-bucket"
            packageKey: "your/lambda.zip"
```
