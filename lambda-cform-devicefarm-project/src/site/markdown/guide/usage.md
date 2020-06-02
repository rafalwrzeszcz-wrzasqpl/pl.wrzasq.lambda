<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource handler manages **DeviceFarm** Selenium Grid project. Main purpose is to expose created project to other
resources managed within the stack, eg. to allow **CodePipeline** run **CodeBuild** project with **Selenium**
integration test running in specified DeviceFarm project.

# Required permissions

`lambda-cform-devicefarm-project` Lambda needs following permissions:

-   `devicefarm:CreateTestGridProject`,
-   `devicefarm:DeleteTestGridProject`,
-   `devicefarm:UpdateTestGridProject`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `name` (required) - string

Specifies project name.

## `description` - string

Specifies project description.

# Output values

Deploy handler exposes entire
[TestGridProject](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/devicefarm/model/TestGridProject.html)
object.

**Note:** Custom resource physical ID is set as deployed project
[ARN](https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html).

# Example

```yaml
    DeviceFarmProjectManagerRole:
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
                    PolicyName: "AllowManagingDeviceFarmProjects"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "devicefarm:CreateTestGridProject"
                                    - "devicefarm:DeleteTestGridProject"
                                    - "devicefarm:UpdateTestGridProject"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    DeviceFarmProjectManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java11"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-devicefarm-project-1.0.1-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.devicefarm.project.Handler::handle"
            MemorySize: 256
            Description: "AWS DeviceFarm Selenium Grid Project manager deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "DeviceFarmProjectManagerRole.Arn"

    DeviceFarmProject:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "DeviceFarmProjectManager.Arn"
            name: "IntegrationTest"
```
