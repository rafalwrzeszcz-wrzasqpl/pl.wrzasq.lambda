/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.stackset.model;

import java.util.Map;
import java.util.Set;

import com.amazonaws.services.cloudformation.model.Capability;
import lombok.Data;

/**
 * StackSet CloudFormation request.
 */
@Data
public class StackSetRequest
{
    /**
     * Stack set stackSetName.
     */
    private String stackSetName;

    /**
     * Stack set description.
     */
    private String description;

    /**
     * URL of stack instance template.
     */
    private String templateUrl;

    /**
     * Acknowledge stack set capabilities.
     */
    private Set<Capability> capabilities;

    /**
     * ARN of administration (current account) role to be used to execute stack set actions.
     */
    private String administrationRoleArn;

    /**
     * Name of the execution role to be used on target accounts.
     */
    private String executionRoleName;

    /**
     * Key-value of custom parameters.
     */
    private Map<String, String> parameters;

    /**
     * Resource tags.
     */
    private Map<String, String> tags;
}
