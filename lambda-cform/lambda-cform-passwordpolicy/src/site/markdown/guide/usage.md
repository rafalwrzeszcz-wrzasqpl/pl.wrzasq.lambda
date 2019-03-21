<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Using in CloudFormation

This resource handler provisions account password policy.

# Required permissions

`lambda-cform-passwordpolicy` Lambda needs following permissions:

-   `iam:DeleteAccountPasswordPolicy`,
-   `iam:UpdateAccountPasswordPolicy`.

Additionally you may want to add following policies to it's role:

-   `arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole` (if you want to see **CloudWatch** logs of
resource handler execution);
-   `arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess` (if you want more detailed tracing, package is built with
**X-Ray** instrumentor).

# Properties

Resource properties are mapped directly to password policy update request, which means that properties are same as in
[UpdateAccountPasswordPolicyRequest](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/identitymanagement/model/UpdateAccountPasswordPolicyRequest.html).

# Output values

Output properties are same as the specified policy. Custom resource ID is set to fixed string.

# Example

```yaml
    PasswordPolicyManagerRole:
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
                    PolicyName: "AllowManagingPasswordPolicy"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "iam:DeleteAccountPasswordPolocy"
                                    - "iam:UpdateAccountPasswordPolicy"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    PasswordPolicyManager:
        Type: "AWS::Lambda::Function"
        Properties:
            Runtime: "java8"
            Code:
                # put your source bucket
                S3Bucket: "your-bucket"
                S3Key: "lambda-cform-passwordpolicy-1.0.2-standalone.jar"
            Handler: "pl.wrzasq.lambda.cform.passwordpolicy.Handler::handle"
            MemorySize: 256
            Description: "AWS password policy manager deployment."
            Timeout: 300
            TracingConfig:
                Mode: "Active"
            Role: !GetAtt "PasswordPolicyManagerRole.Arn"

    PasswordPolicy:
        Type: "AWS::CloudFormation::CustomResource"
        Properties:
            # reference to deploy function
            ServiceToken: !GetAtt "PasswordPolicyManager.Arn"
            minimumPasswordLength: 8
            requireLowercaseCharacters: true
            requireUppercaseCharacters: true
            requireNumbers: true
            requireSymbols: true
            allowUsersToChangePassword: true
            passwordReusePrevention: 2
            maxPasswordAge: 90
            hardExpiry: false
```
