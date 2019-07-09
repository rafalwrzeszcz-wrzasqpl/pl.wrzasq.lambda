<!---
# This file is part of the pl.wrzasq.lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# DynamoDB mapper factory

This module provides factory for **DynamoDB** mapper that allows creation of mapper for cases when tables are defined at
runtime (for example specified by environment variable of **Lambda**). There is also a variable of factory method that
accepts **KMS** key ARN as the second parameter to create encryption client that handles data encryption on client-side
which effect in both at-rest and in-transit data encryption.
