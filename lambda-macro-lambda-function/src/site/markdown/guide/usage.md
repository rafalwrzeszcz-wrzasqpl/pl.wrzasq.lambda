<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This macro handler automates setup of additional Lambda function resources:

-   managed log group with customizable retention period;
-   memory consumption metric;
-   warnings logs filter metric;
-   errors logs filter metric;
-   errors metric alert.

# Required permissions

`lambda-macro-lambda-function` Needs no specific permissions, you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Definition

Type: `WrzasqPl::Lambda::Function`.

## Properties

All of the properties are transparently forwarder to
[AWS::Lambda::Function](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-lambda-function.html)
resource. Additionally following properties are available to customize extra resources:

## `LogsRetentionInDays` - number

Number of days for log retention (by default `7`).

## `ErrorsFilterPattern` - string

Log filter to detect error events (by default `ERROR -LOG_ERROR`).

## `WarningsFilterPattern` - string

Log filter to detect warning events (by default `WARN`).

## `ErrorsAlarmActions` - string[]

List of target ARNs that will receive notification when errors alarm is turned on.

## Output values

Any reference to `Ref` or `GetAtt` on this virtual resource will be replace by analogical call to physical
`AWS::Lambda::Function` creation in place of it.

**Note:** When you add this virtual resource to any `DependsOn` claus it will be replaced by log group dependency, to
ensure any execution of created Lambda function will occur after creation of it's managed log group.

# Example

```yaml
Transform:
    - "WrzasqPlLambdaFunction"

Resources:
    EnhancedLambda:
        Type: "WrzasqPl::Lambda::Function"
        Properties:
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-account-1.0.4-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.account.Handler::handle"
            MemorySize: 256
            Description: "AWS Account manager deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "AccountManagerRole.Arn"
            LogsRetentionInDays: 10
            ErrorsFilterPattern: "error"
            WarningsFilterPattern: "warning"
            ErrorsAlarmActions:
                - !Ref AlarmsQueue
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
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-macro-lambda-function-1.0.36-standalone.jar"
            Handler: "pl.wrzasq.lambda.macro.lambda.function.Handler::handle"
            MemorySize: 256
            Description: "Custom CloudFormation macro handler."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "MacroFunctionRole.Arn"

    Macro:
        Type: "AWS::CloudFormation::Macro"
        Properties:
            Name: "WrzasqPlLambdaFunction"
            FunctionName: !GetAtt "MacroFunction.Arn"
```
