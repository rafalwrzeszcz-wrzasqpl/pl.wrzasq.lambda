<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

By using this macro handler, it's easy to create a **CodeBuild** project pre-configured for use in **CodePipeline**:

-   managed log group with customizable retention period;
-   source and artifacts are by default set to use `CODEPIPELINE`;
-   build environment is by default set to run `LINUX_CONTAINER` on `BUILD_GENERAL1_SMALL`,
-   simplified environment variables setting as key-value map.

# Required permissions

`lambda-macro-pipeline-project` Needs no specific permissions, you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Definition

Type: `WrzasqPl::Pipeline::Project`.

## Properties

All of the properties are transparently forwarder to
[AWS::CodeBuild::Project](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-codebuild-project.html)
resource. Additionally following properties are available to customize extra resources:

## `LogsRetentionInDays` - number

Number of days for log retention (by default `14`).

## `Variables` - string =&gt; string

Key-value mapping of environment variables. It's a more convenient way than `Environment.EnvironmentVariables`, which
requires list of entries instead of simple mapping. Both variables list will be merged, so you can use both (if you,
for whatever, reason want to).

## Output values

Any reference to `Ref` or `GetAtt` on this virtual resource will be replace by analogical call to physical
`AWS::CodeBuild::Project` creation in place of it.

**Note:** When you add this virtual resource to any `DependsOn` claus it will be replaced by log group dependency, to
ensure any execution of created Lambda function will occur after creation of it's managed log group.

# Example

```yaml
Transform:
    - "WrzasqPlPipelineProject"

Resources:
    EnhancedLambda:
        Type: "WrzasqPl::Pipeline::Project"
        Properties:
            ServiceRole: !Ref "BuildRole"
            Environment:
                Image: "aws/codebuild/amazonlinux2-x86_64-standard:2.0"
            Variables:
                COMPONENT_ID: !Ref "ComponentId"
                REPOSITORY_BUCKET: !ImportValue "RepositoryBucket:Name"
            # you can still define Artifacts and Source if you want to override
```

# Installation

```yaml
    MacroFunctionRole:
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
            ManagedPolicyArns:
                - "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"

    MacroFunction:
        Type: "AWS::Serverless::Function"
        Properties:
            Runtime: "java11"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-macro-pipeline-project-1.1.1-standalone.jar"
            Handler: "pl.wrzasq.lambda.macro.pipeline.project.Handler::handleRequest"
            MemorySize: 256
            Description: "Custom CloudFormation macro handler."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "MacroFunctionRole.Arn"

    Macro:
        Type: "AWS::CloudFormation::Macro"
        Properties:
            Name: "WrzasqPlPipelineProject"
            FunctionName: !GetAtt "MacroFunction.Arn"
```
