<!---
# This file is part of the ChillDev-Lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Deleting replicas

Even though **Lambda@Edge** can only be managed from `us-east-1` region, functions deployed as **Lambda@Edge** (bound
to **CloudFront** distribution) are being replicated across all regions in **AWS** when only they are used in other
location.

Unfortunately [replicas can't be deleted](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/lambda-edge-delete-replicas.html)
manually. This will cause automated resource deletion, triggered by **CloudFormation** to fail.

This will not break stack update process, as unused resources are deleted in `UPDATE_COMPLETE_CLEANUP_IN_PROGRESS`
phase, but will leave the function in `us-east-1` region deployed. You will have to wait until all replicas are purged
(usually few hours) and manually delete.
