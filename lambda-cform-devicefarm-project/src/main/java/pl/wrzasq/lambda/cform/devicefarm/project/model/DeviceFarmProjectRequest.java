/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.devicefarm.project.model;

import lombok.Data;

/**
 * DeviceFarm project CloudFormation request.
 */
@Data
public class DeviceFarmProjectRequest {
    /**
     * Project name.
     */
    private String name;

    /**
     * Project description.
     */
    private String description;
}
