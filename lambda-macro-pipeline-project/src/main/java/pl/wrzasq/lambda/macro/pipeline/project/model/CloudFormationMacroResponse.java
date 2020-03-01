/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.macro.pipeline.project.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * CloudFormation event response structure.
 */
@AllArgsConstructor
@Data
public class CloudFormationMacroResponse {
    /**
     * OK status.
     */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * Request ID.
     */
    private String requestId;

    /**
     * Operation status.
     */
    private String status;

    /**
     * Template fragment.
     */
    private Map<String, Object> fragment;
}
