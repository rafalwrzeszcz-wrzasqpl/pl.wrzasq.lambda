<!---
# This file is part of the ChillDev-Lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# ChillDev-Lambda

**ChillDev-Lambda** is a set of generic **AWS Lambda**s.

[![Build Status](https://travis-ci.org/chilloutdevelopment/pl.chilldev.lambda.svg)](https://travis-ci.org/chilloutdevelopment/pl.chilldev.lambda)
[![Coverage Status](https://coveralls.io/repos/chilloutdevelopment/pl.chilldev.lambda/badge.png?branch=develop)](https://coveralls.io/r/chilloutdevelopment/pl.chilldev.lambda)
[![Known Vulnerabilities](https://snyk.io/test/github/chilloutdevelopment/pl.chilldev.lambda/badge.svg)](https://snyk.io/test/github/chilloutdevelopment/pl.chilldev.lambda)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/chilloutdevelopment/pl.chilldev.lambda)

# Usage

Each **Lambda** module is a stand-alone, independent package. Ready to be deployed into **AWS** out of the box.

Keep in mind, that you can use **Lambda** packages regardless of your technology - they are completely independent,
running in [_FaaS_](https://en.wikipedia.org/wiki/Function_as_a_service) environment, communicating through
**JSON**-based interface.

You can use virtually any tool and integrate them with any project. No piece of your code will directly interact with
this source code.

# Lambdas

## [Lambda@Edge deploy](https://chilloutdevelopment.github.io/pl.chilldev.lambda/lambda-edgedeploy/)

This is a [**CloudFormation**](https://aws.amazon.com/cloudformation/) custom resource handler (implemented using
[`pl.chilldev.commons:commons-aws`](https://chilloutdevelopment.github.io/pl.chilldev.commons/commons-aws/)). It allows
to deploy [**Lambda@Edge**](https://aws.amazon.com/lambda/edge/) functions from any region and expose their to
[`AWS::CloudFront::Distribution`](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cloudfront-distribution.html)
resource.

# Resources

-   [GitHub page with API documentation](https://chilloutdevelopment.github.io/pl.chilldev.lambda)
-   [Contribution guide](https://github.com/chilloutdevelopment/pl.chilldev.lambda/blob/develop/CONTRIBUTING.md)
-   [Issues tracker](https://github.com/chilloutdevelopment/pl.chilldev.lambda/issues)
-   [Maven packages](https://search.maven.org/search?q=g:pl.chilldev.lambda)
-   [Chillout Development @ GitHub](https://github.com/chilloutdevelopment)
-   [Chillout Development @ Facebook](https://www.facebook.com/chilldev)
-   [Post on Wrzasq.pl](http://wrzasq.pl/blog/deploying-lambda-edge-with-pl-chilldev-lambda.html)

# Authors

This project is published under [MIT license](https://github.com/chilloutdevelopment/pl.chilldev.lambda/tree/master/LICENSE).

**pl.chilldev.lambda** is brought to you by [Chillout Development](https://chilldev.pl).

List of contributors:

-   [Rafał "Wrzasq" Wrzeszcz](https://github.com/rafalwrzeszcz) ([wrzasq.pl](https://wrzasq.pl)).
