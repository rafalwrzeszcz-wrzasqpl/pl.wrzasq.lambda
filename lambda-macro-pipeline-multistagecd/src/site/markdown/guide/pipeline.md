<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

One of the base components of DevOps approach is **CI**/**CD** pipeline. It's the way to express project deployment
process in terms of automated, formalized steps. Together with
[**Iac**](https://en.wikipedia.org/wiki/Infrastructure_as_code) it allows to cover most of the maintenance aspects with
software toolset and allow to apply all of the software development best practices to infrastructure and processes of
your project.

**AWS** provides services that cover these aspects - **CodePipeline** to build processes and **CloudFormation** to
manage infrastructure. These two services play nicely with each other allowing pipeline to be managed with
**CloudFormation** and automate stacks deployment with **CodePipeline**.

However the format is very verbose and requires a lot of boilerplate code. When following micro-services architecture
and maintaining multiple pipelines this can become a noticeable overhead.

This macro aims to simplify and unify pipelines definition by reducing some code constructs and automating some tasks:

-   automatically defines steps for passing artifacts between stages;
-   control pipeline structure with input parameters;
-   simplifying artifacts definition;
-   automatically detecting dependency artifacts (partially);
-   automatically ordering actions within stage based on dependencies.

# Concepts

Main concept of this pipeline macro is a deployment stage (not to be confused with a **CodePipeline** pipeline stage).
Deployment stage is a stage of your product (very often achieved by separating on AWS accounts level, but can be also
done by using prefixes or scoping eg. within different **VPC**).

With a _DevOps_ approach you usually want to express as much as possible in the code and also control processes with
such code. A deployment pipeline is one of such examples. Following a
[dev-prod parity](https://12factor.net/dev-prod-parity) you most likely want to use same code to control deployment over
all of your stages (eg. `DEV` -> `TEST` -> `LIVE`). This macro simplifies creation of such pipelines automatically
handling a lot of repetitive boilerplate code. But under the hood it just generates `AWS::CodePipeline::Pipeline`
resource through series of transformations.

**Note:** It's important to remember that macro only controls template structure, not actual resources. Template
generated on each of your deployment stages will be exactly the same and resources will be controlled by parameters
passed to their respective stacks. For example on every stage you will have a template will all sources definitions,
manual approval step checks, promotion actions. Actual creation of these resources will be controlled by the template
parameters and conditions.
