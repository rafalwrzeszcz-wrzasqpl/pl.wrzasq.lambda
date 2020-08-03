<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

**AppSync** GraphQL APIs can be managed with **CloudFormation** using
[`AWS::AppSync::GraphQLApi`](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-appsync-graphqlapi.html)
resource. However, when you try to expose the API through **CloudFront** you will hit the wall - the API resource
exposes only entire URL (include `https://` scheme and `/graphql` path), while CloudFront expects only domain name.
Building API domain by hand in CloudFormation with `Fn::Sub` is not possible because it is unique for every API and do
not utilizes ID.

This custom resource handler exposes additional properties of GraphQL API so they can be used within **CloudFormation**.

**Note:** This resource handler only exposes information about existing domain name, you need to already have a resource
created by `AWS::AppSync::GraphQLApi`.

# Required permissions

`lambda-cform-appsync-graphqlapi-data` Lambda needs following permissions:

-   `appsync:GetGraphqlApi`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `apiId` (required) - string

ID of GraphQL API.

# Output values

## `domainName` - string

Domain name of public endpoint.

**Note:** Custom resource physical ID is set as API ID.

# Example

```yaml
    AppSyncGraphQlApiProviderRole:
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
                    PolicyName: "AllowReadingAppSyncApi"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "appsync:GetGraphqlApi"
                                Effect: "Allow"
                                Resource:
                                    - "*" # you can place particular API ARN here

    AppSyncGraphQlApiProvider:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java11"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-appsync-graphqalapi-data-1.0.1-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.appsync.graphqlapi.data.Handler::handle"
            MemorySize: 256
            Description: "AWS AppSync API domain data provider."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "AppSyncGraphQlApiProviderRole.Arn"

    AppSyncGraphQlApi:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "AppSyncGraphQlApiProvider.Arn"
            apiId: !GetAtt "YourApi.ApiId"
