<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Required permissions

`lambda-macro-pipeline-multistagecd` Needs no specific permissions, you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Definition

Section: `Pipeline`.

This macro operates on dedicated template section, `Pipeline`. It has sub-sections to set different aspects of CI/CD
pipeline.

## `config`

Configuration section is optional, and allows to customize managed pipeline. If omitted, default configuration is
applied.

### `resourceName` - string (default: `Pipeline`)

Managed pipeline resource name.

### `hasCheckoutStepParameterName` - string (default: `HasCheckoutStep`)

Name of parameter that controls existence of initial checkout steps.

### `hasNextStageParameterName` - string (default: `HasNextStage`)

Name of parameter that controls existence of stage promotion steps.

### `requiresManualApprovalParameterName` - string (default: `RequiresManualApproval`)

Name of parameter that controls existence of manual approval step.

### `hasCheckoutStepConditionName` - string (default: `HasCheckoutStep`)

Name of condition that controls existence of initial checkout steps (it reflects parameter value).

### `hasNextStageConditionName` - string (default: `HasNextStage`)

Name of parameter that controls existence of stage promotion steps (it reflects parameter value).

### `requiresManualApprovalConditionName` - string (default: `RequiresManualApproval`)

Name of parameter that controls existence of manual approval step (it reflects parameter value and considers also next
stage condition).

### `webhookAuthenticationType` - string | object

Authentication type for Git webhook. If not specified, webhook will not be created.

### `webhookSecretToken` - string | object

Webhook secret token. If not specified, webhook will not be created. This setting can be a string, but also complex
construct (like intrinsic function call)

## `properties`

Any properties that will be passed directly to
[AWS::CodePipeline::Pipeline](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-codepipeline-pipeline.html)
resource.

## `sources`

Definition initial sources. These sources are used as inputs for the initial stage (based on `HasCheckoutStage`
parameter). For any further stage previous stage artifacts are used - for that see **artifacts** section below.

Each source is an entry with key being used as action name and implicitly output artifact name. Entry content is
definition of CodePipeline action:

```yaml
    sources:
        checkout:
            ActionTypeId:
                Category: "Source"
                Owner: "ThirdParty"
                Provider: "GitHub"
                Version: "1"
            Configuration:
                Owner: "yourorganization"
                Repo: "reponame"
                Branch:
                    Ref: "branch"
                OAuthToken: "{{resolve:secretsmanager:GitHub:SecretString:OAuthToken}}"
                PollForSourceChanges: false
```

## `stages`

Content of the pipeline. It is mapped into final `AWS::CodePipeline::Pipeline` stages property elements. However there
are some simplifications:

-   unless not strictly required by your pipeline, you don't need to specify `RunOrder` property (it will be calculated
based on artifacts dependencies);
-   `InputArtifacts` and `OutputArtifacts` are list of simple strings (not objects with `Name` property);
-   in `ActionTypeId` properties `Owner` and `Version` are not required (defaults to `AWS` and `1` respectively);
-   for `CloudFormation` deploy steps no `ActionTypeId` is required at all, this is the default action;
-   actions can not be conditional, but entire stages can be, by specifying `Condition` property of stage;
-   for `CloudFormation` deploy action input artifacts are detected automatically for `TemplatePath` and
`TemplateConfiguration` properties if they are plain strings (it's still possible to define additional artifacts
explicitly).

## `artifacts`

Pipeline artifacts are artifacts of pipeline actions that you want to promote to next deployment stage. The same list
will be used to build sources list of non-initial stage, binding deployment stages to chain after promotion (successful
execution of previous stage). Right now only **S3** is supported as artifacts transition (deploy action is used to
push artifacts into next stage and source is defined in next stage pipeline).

# Example

```yaml
Transform:
    - "WrzasqPlMultistagePipeline"

Parameters:
    HasCheckoutStep:
        Type: String
        Default: 'false'
        AllowedValues:
            - 'true'
            - 'false'

    HasNextStage:
        Type: String
        Default: 'false'
        AllowedValues:
            - 'true'
            - 'false'

    RequiresManualApproval:
        Type: String
        Default: 'true'
        AllowedValues:
            - 'true'
            - 'false'

Resources:
    BuildProject:
        Type: "AWS::CodeBuild::Project"

Pipeline:
    config:
        resourceName: "DeployPipeline"

    properties:
        RestartExecutionOnUpdate: true
        RoleArn: "arn:aws:iam::765719665682:role/service-role/test"
        ArtifactStore:
            Type: "S3"
            Location: "test-local-lambda"

    sources:
        checkout:
            ActionTypeId:
                Category: "Source"
                Owner: "AWS"
                Provider: "S3"
                Version: "1"
            Configuration:
                S3Bucket: "test-local-lambda"
                S3ObjectKey: "pl.wrzasq.lambda/checkout.zip"

    stages:
        -
            # this is example of conditional stage
            Name: "Build"
            Condition: "HasCheckoutStep"
            Actions:
                -
                    Name: "Build"
                    ActionTypeId:
                        Category: "Build"
                        Provider: "CodeBuild"
                    Configuration:
                        ProjectName: !Ref "BuildProject"
                        EnvironmentVariables: |
                            [
                                {
                                    "name": "VERSION_STRING",
                                    "value": "#{codepipeline.PipelineExecutionId}"
                                }
                            ]
                    InputArtifacts:
                        - "checkout"
                    OutputArtifacts:
                        - "build"
        -
            Name: "Deploy"
            Actions:
                -
                    Name: "Database"
                    Configuration:
                        ActionMode: "CREATE_UPDATE"
                        StackName: !Sub "${ServiceName}-db"
                        RoleArn: !ImportValue "infra:DeployRole:Arn"
                        Capabilities: "CAPABILITY_NAMED_IAM"
                        # `build` artifact will automatically be detected
                        TemplatePath: "build::cloudformation/db.yaml"
                        # since this is not a plain string value `templates` artifact needs to be added manually
                        TemplateConfiguration: !Sub "templates::cloudformation/config-${EnvironmentName}.json"
                        OutputFileName: "out.json"
                    InputArtifacts:
                        - "templates"
                    OutputArtifacts:
                        - "database"
                -
                    Name: "EventsListenerLambda"
                    Configuration:
                        ActionMode: "CREATE_UPDATE"
                        StackName: "events-recorder"
                        Capabilities: "CAPABILITY_NAMED_IAM,CAPABILITY_AUTO_EXPAND"
                        TemplatePath: "templates::cloudformation/events-listener.yaml"
                        TemplateConfiguration: "templates::cloudformation/config-dev.json"
                        OutputFileName: "out.json"
                        ParameterOverrides: |
                            {
                                "EventsTableName": { "Fn::GetParam": ["database", "out.json", "EventsTableName"] },
                                "EventsTableArn": { "Fn::GetParam": ["database", "out.json", "EventsTableArn"] }
                            }
                    InputArtifacts:
                        - "database"
                    OutputArtifacts:
                        - "events-listener"
                -
                    Name: "AuthorizerLambda"
                    Configuration:
                        ActionMode: "CREATE_UPDATE"
                        StackName: "authorizer"
                        Capabilities: "CAPABILITY_NAMED_IAM,CAPABILITY_AUTO_EXPAND"
                        TemplatePath: "templates::cloudformation/authorizer.yaml"
                        OutputFileName: "out.json"
                    OutputArtifacts:
                        - "authorizer"
                -
                    Name: "API"
                    Configuration:
                        ActionMode: "CREATE_UPDATE"
                        StackName: "api"
                        Capabilities: "CAPABILITY_NAMED_IAM,CAPABILITY_AUTO_EXPAND"
                        TemplatePath: "templates::cloudformation/api.yaml"
                        OutputFileName: "out.json"
                        ParameterOverrides: |
                            {
                                "AuthorizerLambda": { "Fn::GetParam": ["authorizer", "out.json", "LambdaArn"] },
                                "EventsListenerLambda": { "Fn::GetParam": ["events-listener", "out.json", "LambdaArn"] }
                            }
                    InputArtifacts:
                        - "authorizer"
                        - "events-listener"

    artifacts:
        build:
            sourceBucketName: !ImportValue "wrzasq:ArtifactsBucket:Name"
            nextBucketName: !Ref "NextStageBucketName"
            objectKey: "pl.wrzasq.lambda/build.zip"
        templates:
            sourceBucketName: !ImportValue "wrzasq:ArtifactsBucket:Name"
            nextBucketName: !Ref "NextStageBucketName"
            objectKey: "pl.wrzasq.lambda/templates.zip"
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
                S3Key: "lambda-macro-pipeline-multistagecd-1.1.2-standalone.jar"
            Handler: "pl.wrzasq.lambda.macro.pipeline.multistagecd.Handler::handleRequest"
            MemorySize: 256
            Description: "Custom CloudFormation macro handler."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "MacroFunctionRole.Arn"

    Macro:
        Type: "AWS::CloudFormation::Macro"
        Properties:
            Name: "WrzasqPlMultistagePipeline"
            FunctionName: !GetAtt "MacroFunction.Arn"
```
