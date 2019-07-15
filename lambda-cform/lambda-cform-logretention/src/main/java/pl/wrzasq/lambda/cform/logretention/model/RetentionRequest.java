/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.logretention.model;

import java.util.List;

import lombok.Data;

/**
 * Retention and groups CloudFormation request.
 */
@Data
public class RetentionRequest {
    /**
     * List of log groups.
     */
    private List<String> logGroups;

    /**
     * Number of days to retain logs.
     */
    private int retentionDays;
}
