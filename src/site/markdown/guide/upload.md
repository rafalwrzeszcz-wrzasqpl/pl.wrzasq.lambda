<!---
# This file is part of the ChillDev-Lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# S3 upload

Keep in mind that in many cases, when working with **AWS** services, like [**Lambda**](https://aws.amazon.com/lambda/)
it's required that your artifacts are available on [**S3**](https://aws.amazon.com/s3/).

In [**Chillout Development**](https://chilldev.pl/) we use **S3** as our private **Maven** repository. But open source
projects are deployed into [Maven Central Repository](https://search.maven.org/). To expose these packages through
**S3** we use following steps in our **CI/CD** setup:

```bash
# download artifact from Maven Central into local repository
mvn dependency:get …
# deploy it into your private S3 repository
mvn deploy:deploy-file …
``` 

**Note:** **Lambda** package is a completely stand-alone package, you can use it out of the box regardless of your
technology stack. Solution with **Maven** is just one case that we use.
