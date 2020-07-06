<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# TODO

# Using in CloudFormation

Since some time **CloudFormation** allows to manage custom domains with
[`AWS::Cognito::UserPoolDomain`](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cognito-userpooldomain.html)
resource. Unfortunately this resource does not expose generated **CloudFront** distribution which makes management
of **Route53** record set impossible.

This custom resource handler exposes properties of custom domain so they can be used within **CloudFormation.

**Note:** This resource handler only exposes information about existing domain name, you need to already have a resource
created by `AWS::Cognito::UserPoolDomain`.

# Required permissions

`lambda-cform-cognito-domain-data` Lambda needs following permissions:

-   `cognito-idp:DescribeUserPoolDomain`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `domain` (required) - string

Target domain name.

# Output values

Deploy handler exposes entire
[DomainDescriptionType](https://docs.amazonaws.cn/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cognitoidp/model/DomainDescriptionType.html)
object.

**Note:** Custom resource physical ID is set as domain name.

# Example

```yaml
    CognitoDomainProviderRole:
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
                    PolicyName: "AllowReadingCognitoDomains"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "cognito-idp:DescribeUserPoolDomain"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    CognitoDomainProvider:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java11"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-cognito-domain-data-1.0.1-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.cognito.domain.data.Handler::handle"
            MemorySize: 256
            Description: "AWS Cognito user pool custom domain data provider."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "CognitoDomainProviderRole.Arn"

    CognitoDomain:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "CognitoDomainProvider.Arn"
            domain: "auth.wrzasq.pl"

    Route53Binding:
        Type: "AWS::Route53::RecordSet"
        Properties:
            HostedZoneId: !Ref ""
            Name: "auth.wrzasq.pl."
            Type: "A"
            AliasTarget:
                HostedZoneId: "Z2FDTNDATAQYW2"
                DNSName: !GetAtt "CognitoDomain.cloudFrontDistribution"
```
