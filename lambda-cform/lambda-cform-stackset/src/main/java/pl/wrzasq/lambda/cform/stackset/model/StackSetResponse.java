/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset.model;

import lombok.Data;

/**
 * StackSet CloudFormation result response.
 */
@Data
public class StackSetResponse
{
    /**
     * Stack set ID.
     */
    private String id;

    /**
     * Stack set stackSetName.
     */
    private String stackSetName;
}
