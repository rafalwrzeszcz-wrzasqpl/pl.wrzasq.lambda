<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource handler manages organizational units of the organization.

**Note:** To simplify the naming of project packages the Lambda (and it's package) is called `organization unit`, but
actual AWS resource type which it manages is called `organizational unit` (different is the _organization**al**_ word).

# Required permissions

`lambda-cform-organization-unit` Lambda needs following permissions:

-   `organizations:CreateOrganizationalUnit`,
-   `organizations:DeleteOrganizationalUnit`,
-   `organizations:DescribeOrganizationalUnit`,
-   `organizations:ListParents`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `name` (required) - string

Name of the OU.

## `parentId` (required) - string

ID of the parent node in the organization structure (root or another OU).

**Note:** Changing parent effectively removes the OU and creates new one in new tree location.

# Output values

Deploy handler exposes entire
[OrganizationalUnit](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/organizations/model/OrganizationalUnit.html)
object.

**Note:** Custom resource physical ID is set as created organizational unit ID.

# Example

```yaml
    OrganizationUnitManagerRole:
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
                                    - "organizations:CreateOrganizationalUnit"
                                    - "organizations:DeleteOrganizationalUnit"
                                    - "organizations:DescribeOrganizationalUnit"
                                    - "organizations:ListParents"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    OrganizationUnitManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-organization-unit-1.0.3-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.organization.unit.Handler::handle"
            MemorySize: 256
            Description: "AWS Organizational Unit manager deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "OrganizationUnitManagerRole.Arn"

    OrganizationUnit:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "OrganizationUnitManager.Arn"
            name: "internal services"
            # assume Organization is a resource created by lambda-cform-organization handler
            parentId: !GetAtt "Organization.RootId"
```
