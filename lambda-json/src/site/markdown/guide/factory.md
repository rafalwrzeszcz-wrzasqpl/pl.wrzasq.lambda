<!---
# This file is part of the ChillDev-Lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Jackson ObjectMapper factory

This module provides default setup of **Jackson** `ObjectMapper` for handling **JSON** across multiple **Lambda**s in
the same way. It comes with **Java Time API** (JDK8) module loaded. It's also configure to ignore unknown properties
during deserialization for better flexibility (so that one can use partial models and also be future-compatible as much
as possible). Dates are configured to be serialized as timestamps (doesn't mean **Unix** timestamps - just a string
representation, instead of atomic properties).
