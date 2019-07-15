<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource sets log retention time for already existing **CloudWatch** log groups.

**Note:** **CloudFormation** provider [LogGroup](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-logs-loggroup.html)
resource which can also set retention policy. You should use `lambda-cform-logretention` only in case you can not
control log group creation directly. An example can be a log group of **Lambda** function that is used directly in
CloudFormation (eg. as a custom resource handler).

# Required permissions

`lambda-cform-logretention` Lambda needs following permissions:

-   `logs:DeleteRetentionPolicy`,
-   `logs:PutRetentionPolicy`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `retentionDays` (required) - int

Number of days to keep logs for.

## `logGroups` (required) - string[]

List of log groups to apply policy to.

# Output values

This resource type does not expose any attributes.

**Note:** Custom resource physical ID is set a random string and is maintained between deploys to avoid re-creation. It
doesn't carry any information.

# Example

```yaml
    RetentionManagerRole:
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
                    PolicyName: "AllowManagingRetention"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "logs:DeleteRetentionPolicy"
                                    - "logs:PutRetentionPolicy"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    RetentionManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-logretention-1.0.29-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.logretention.Handler::handle"
            MemorySize: 256
            Description: "AWS CloudWatch Logs retention deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "RetentionManagerRole.Arn"

    ShortRetention:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "RetentionManager.Arn"
            retentionDays: 7
            logGroups:
                - "/aws/lambda/yourlambda-1"
                - "/aws/lambda/yourlambda-2"

    LongRetention:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "RetentionManager.Arn"
            retentionDays: 30
            logGroups:
                - "/aws/codebuild/BuildProject-123"
```
