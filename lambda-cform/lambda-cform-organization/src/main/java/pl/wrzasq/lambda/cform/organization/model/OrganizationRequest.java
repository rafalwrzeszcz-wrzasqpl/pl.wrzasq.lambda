/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.organization.model;

import com.amazonaws.services.organizations.model.OrganizationFeatureSet;
import lombok.Data;

/**
 * Organization CloudFormation request.
 */
@Data
public class OrganizationRequest
{
    /**
     * Feature set enabled for organization (only applies at creation time).
     */
    private OrganizationFeatureSet featureSet;
}
