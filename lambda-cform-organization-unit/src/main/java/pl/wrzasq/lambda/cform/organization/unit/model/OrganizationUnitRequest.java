/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.organization.unit.model;

import lombok.Data;

/**
 * Organizational Unit CloudFormation request.
 */
@Data
public class OrganizationUnitRequest {
    /**
     * Organizational unit name.
     */
    private String name;

    /**
     * ID of the parent organizational node (unit or root).
     */
    private String parentId;
}
