<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Usage

This **Lambda** package generates metrics for **DynamoDB** table recording items count and table storage size.

**Note:** **DynamoDB** updates items count and storage size roughly every six hours, so there is no point in computing
this metric more often.

## Required permissions

`lambda-metrics-dynamodb` Lambda needs following permissions:

-   `dynamodb:DescribeTable` (at least to tables you want to analyze),
-   `cloudwatch:PutMetricData`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

## Environment variables

-   `METRICS_NAMESPACE`: metrics to be used for storing metrics.

## Metrics

-   `ItemCount`: number of items in the table;
-   `TableSizeBytes`: amount of bytes used by table.

## Dimensions

-   `TableName`: name of DynamoDB table.

# Example

```yaml
    DynamoDbMetricsRole:
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
                    PolicyName: "AllowDescribingDynamoDbTable"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "dynamodb:DescribeTable"
                                Effect: "Allow"
                                Resource:
                                    - !GetAtt "TableA.Arn"
                                    - !GetAtt "TableB.Arn"
                -
                    PolicyName: "AllowRecordingMetrics"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "cloudwatch:PutMetricData"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    DynamoDbMetrics:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java11"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-metrics-dynamodb-1.0.31-standalone.jar"
            Handler: "pl.wrzasq.lambda.metrics.dynamodb.Handler::handle"
            MemorySize: 256
            Description: "DynamoDB metrics generator."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "DynamoDbMetricsRole.Arn"

    MetricsTrigger:
        Type: "AWS::Events::Rule"
        DependsOn:
            - "DynamoDbMetrics"
        Properties:
            ScheduleExpression: "rate(6 hours)"
            State: "ENABLED"
            Targets:
                -
                    Arn: !GetAtt "DynamoDbMetrics.Arn"
                    Id: "tableA"
                    Input: !Sub "{\"tableName\": \"${TableA}\"}"
                -
                    Arn: !GetAtt "DynamoDbMetrics.Arn"
                    Id: "tableB"
                    Input: !Sub "{\"tableName\": \"${TableB}\"}"

    AuthorizerLambdaHeartbeatPermission:
        Type: "AWS::Lambda::Permission"
        Properties:
            FunctionName: !Ref "DynamoDbMetrics"
            Action: "lambda:InvokeFunction"
            Principal: "events.amazonaws.com"
            SourceArn: !GetAtt "MetricsTrigger.Arn"
```
