<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource manages instances for stack sets.

Each stack instance is a separate resource - if you want to deploy into multiple accounts and/or regions you have to
define separate resource for each of the cases.

# Required permissions

`lambda-cform-stackset-instance` Lambda needs following permissions:

-   `cloudformation:CreateStackInstances`,
-   `cloudformation:DeleteStackInstances`,
-   `cloudformation:DescribeStackInstance`,
-   `cloudformation:DescribeStackSetOperation`.
-   `cloudformation:UpdateStackInstances`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `stackSetName` (required) - string

Name of the stack set.

## `accountId` (required) - string

ID of target account.

## `region` (required) - string

Destination region where to deploy the stack.

## `parameters` - key-value object

Custom parameters to be passed into the stack template.

# Output values

Deploy handler exposes entire
[StackInstance](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudformation/model/StackInstance.html)
object.

**Note:** Custom resource physical ID is set as `${stackSetName}:${accountId}:${region}`.

# Example

```yaml
    StackInstanceManagerRole:
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
            Policies:
                -
                    PolicyName: "AllowManagingStackSets"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "cloudformation:CreateStackInstances"
                                    - "cloudformation:DeleteStackInstances"
                                    - "cloudformation:DescribeStackInstance"
                                    - "cloudformation:DescribeStackSetOperation"
                                    - "cloudformation:UpdateStackInstances"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    StackInstanceManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java11"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-stackset-instance-1.0.6-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.stackset.instance.Handler::handle"
            MemorySize: 256
            Description: "AWS CloudFormation stack instance manager deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "StackInstanceManagerRole.Arn"

    StackInstance:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "StackInstanceManager.Arn"
            # reference to resource provisioned by lambda-cform-stackset
            stackSetName: !GetAtt "StackSet.StackSetName"
            # reference to resource provisioned by lambda-cform-account
            accountId: !GetAtt "Account.Id"
            region: !Ref "AWS::Region"
            parameters:
                Param1: "Value1"
```
