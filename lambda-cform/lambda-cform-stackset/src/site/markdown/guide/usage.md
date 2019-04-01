<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource manages stack set (empty set, to manage stack set instances use
`pl.wrzasq.lambda:lambda-cform-stackset-instance`).

# Required permissions

`lambda-cform-stackset` Lambda needs following permissions:

-   `cloudformation:CreateStackSet`,
-   `cloudformation:DeleteStackSet`,
-   `cloudformation:DescribeStackSet`,
-   `cloudformation:UpdateStackSet`,
-   `iam:PassRole` permission to pass administration role for the managed stack set.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `stackSetName` (required) - string

Name of the stack to be created.

## `description` - string

Description for human beings.

## `templateUrl` (required) - string

Location where template for stack instances is located (needs to be S3 location).

## `capabilities` - string[]

Can be either `CAPABILITY_IAM` or `CAPABILITY_NAMED_IAM` (if stack needs it). For future-compatibility this parameters
can be a list of strings, but for now it makes no sense to declare both capabilities as `CAPABILITY_NAMED_IAM` is just
a broader scope. Keep in mind that stack sets don't support `CAPABILITY_AUTO_EXPAND`.

## `administrationRoleArn` (required) - ARN

ARN of a role used to provision stack set instances from current account.

## `executionRoleName` (required) - string

Name of the role used by stack set instance on each account (this role needs to exist there and have required
privileges to manage all of the stack resources).

## `parameters` - key-value object

Custom parameters to be passed into the stack template.

## `tags` key-value object

Resource tags to be passed into the stack template.

# Output values

## `id` - string

ID of created stack set.

## `name` - string

Name of created stack set. It's the same as name passed to it. But in future name can be also generated randomly.

**Note:** Stack name is used as a physical resource ID. Changing the name will replace entire stack set.

# Example

```yaml
    # this role will be used by CloudFormation
    StackSetAdministrationRole:
        Type: "AWS::IAM::Role"
        Properties:
            AssumeRolePolicyDocument:
                Statement:
                    -
                        Action: "sts:AssumeRole"
                        Effect: "Allow"
                        Principal:
                            Service:
                                - "cloudformation.amazonaws.com"
            ManagedPolicyArns:
                - "arn:aws:iam::aws:policy/AdministratorAccess"

    # this role will be used by deploy Lambda
    StackSetManagerRole:
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
                                    - "cloudformation:CreateStackSet"
                                    - "cloudformation:DeleteStackSet"
                                    - "cloudformation:DescribeStackSet"
                                    - "cloudformation:DescribeStackSetOperation"
                                    - "cloudformation:UpdateStackSet"
                                Effect: "Allow"
                                Resource:
                                    - "*"
                -
                    PolicyName: "AllowPassingAdministrationRole"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "iam:PassRole"
                                Effect: "Allow"
                                Resource:
                                    - !GetAtt "StackSetAdministrationRole.Arn"

    StackSetManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-stackset-1.0.2-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.stackset.Handler::handle"
            MemorySize: 256
            Description: "AWS CloudFormation stack sets manager deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "StackSetManagerRole.Arn"

    StackSet:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "StackSetManager.Arn"
            stackSetName: "organization-super"
            description: "Organization supervision stack"
            templateUrl: "https://s3.eu-central-1.amazonaws.com/your-bucket/organization-super.yaml"
            capabilities:
                - "CAPABILITY_NAMED_IAM"
            administrationRoleArn: !GetAtt "StackSetAdministrationRole.Arn"
            executionRoleName: "OrganizationAdministrator"
            parameters:
                Param1: "Value1"
            tags:
                "organization:product:version": "v1"
```
