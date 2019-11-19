<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource handler sub-accounts management.

**Note:** When using this handler you have to keep in mind that account is a top-level container of any possible AWS
resource including data you store there. Be extremely careful when modifying anything in your account, especially
when you try to change something that will result in creating new account to replace old one.

**Note:** From organization level it's impossible to physically delete an account. What is possible is only removing
account from organization. After that you will have to terminate account physically using it's root access credentials.

# Required permissions

`lambda-cform-account` Lambda needs following permissions:

-   `organizations:CreateAccount`,
-   `organizations:DescribeAccount`,
-   `organizations:DescribeCreateAccountStatus`,
-   `organizations:DescribeHandshake`,
-   `organizations:InviteAccountToOrganization`,
-   `organizations:ListParents`,
-   `organizations:MoveAccount`,
-   `organizations:RemoveAccountFromOrganization`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

## `accountId` - string

ID of existing account to invite into organization.

**Note:** If you don't specify account ID of an existing account a new one will be created.

## `email` (required) - string

E-mail address to be used for new account creation (will be a login credential for root access).

## `accountName` (required) - string

Label of an account.

## `administratorRoleName` - string

Role name for the administration purposes.

**Note:** Only applicable if new account is going to be created. Makes no effect in case of invitation flow.

# Output values

Deploy handler exposes entire
[Account](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/organizations/model/Account.html)
object.

**Note:** Custom resource physical ID is set as an account ID (no matter if invited or created).

# Example

```yaml
    AccountManagerRole:
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
                    PolicyName: "AllowManagingAccounts"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "organizations:CreateAccount"
                                    - "organizations:DescribeAccount"
                                    - "organizations:DescribeCreateAccountStatus"
                                    - "organizations:DescribeHandshake"
                                    - "organizations:InviteAccountToOrganization"
                                    - "organizations:ListParents"
                                    - "organizations:MoveAccount"
                                    - "organizations:RemoveAccountFromOrganization"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    AccountManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java11"
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

    # notice that there is no accountId in this example
    CreatedAccount:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "AccountManager.Arn"
            email: "you+dev@example.com"
            accountName: "dev"
            administratorRoleName: "OrganizationAdmin"
            # assume OrganizationUnit is a resource created by lambda-cform-organization-unit handler
            ouId: !GetAtt "OrganizationUnit.Id"

    InvitedAccount:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "AccountManager.Arn"
            accountId: "123456789"
            email: "you+prod@example.com"
            accountName: "prod"
            administratorRoleName: "OrganizationAdmin"
            # assume OrganizationUnit is a resource created by lambda-cform-organization-unit handler
            ouId: !GetAtt "OrganizationUnit.Id"
```
