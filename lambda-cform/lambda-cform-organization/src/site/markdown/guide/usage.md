<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource handler manages organization state (should be applied only on root account).

# Required permissions

`lambda-cform-organization` Lambda needs following permissions:

-   `organizations:CreateOrganization`,
-   `organizations:DeleteOrganization`,
-   `organizations:DescribeOrganization`,
-   `organizations:ListRoots`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `featureSet` (required) - string

Specifies set of features enabled for accounts in organization. Can be either `CONSOLIDATED_BILLING` or `ALL`.

**Note:** It only applies during organization creation, will not apply any changes if the value changes between updates.

# Output values

Deploy handler exposes structure with two elements:

```json
{
    "organization": object,
    "root": object
}
```

-   `organization`: entire
[Organization](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/iam/model/Organization.html)
object;
-   `root`: entire
[Root](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/organizations/model/Root.html)
object.

**Note:** Custom resource physical ID is set as created organization ID.

# Example

```yaml
    OrganizationManagerRole:
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
                    PolicyName: "AllowManagingOrganizations"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "organizations:CreateOrganization"
                                    - "organizations:DeleteOrganization"
                                    - "organizations:DescribeOrganization"
                                    - "organizations:ListRoots"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    OrganizationManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-organization-1.0.1-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.organization.Handler::handle"
            MemorySize: 256
            Description: "AWS Organization manager deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "OrganizationManagerRole.Arn"

    Organization:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "OrganizationManager.Arn"
            featureSet: "ALL"
```
