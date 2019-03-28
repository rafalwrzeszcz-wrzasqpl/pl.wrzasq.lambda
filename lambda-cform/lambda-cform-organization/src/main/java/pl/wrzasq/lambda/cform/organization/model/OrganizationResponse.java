/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.organization.model;

import lombok.Data;

/**
 * Organization data response model.
 */
@Data
public class OrganizationResponse
{
    /**
     * Organization ID.
     */
    private String id;

    /**
     * Organization ARN.
     */
    private String arn;

    /**
     * Root organizational unit ID.
     */
    private String rootId;
}
