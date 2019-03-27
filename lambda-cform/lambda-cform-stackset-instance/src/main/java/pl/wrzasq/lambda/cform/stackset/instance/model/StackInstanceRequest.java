/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset.instance.model;

import java.util.Map;

import lombok.Data;

/**
 * Stack instance CloudFormation request.
 */
@Data
public class StackInstanceRequest
{
    /**
     * Containing stack set.
     */
    private String stackSetName;

    /**
     * Account ID.
     */
    private String accountId;

    /**
     * Target region.
     */
    private String region;

    /**
     * Overrides of default stack set parameters.
     */
    private Map<String, String> parameterOverrides;
}
