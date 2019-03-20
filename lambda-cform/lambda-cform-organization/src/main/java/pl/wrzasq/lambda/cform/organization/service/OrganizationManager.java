/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.organization.service;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.organizations.AWSOrganizations;
import com.amazonaws.services.organizations.model.CreateOrganizationRequest;
import com.amazonaws.services.organizations.model.CreateOrganizationResult;
import com.amazonaws.services.organizations.model.DeleteOrganizationRequest;
import com.amazonaws.services.organizations.model.DescribeOrganizationRequest;
import com.amazonaws.services.organizations.model.DescribeOrganizationResult;
import com.amazonaws.services.organizations.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.organization.model.OrganizationRequest;

/**
 * Organizations API implementation.
 */
public class OrganizationManager
{
    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(OrganizationManager.class);

    /**
     * AWS Organizations API client.
     */
    private AWSOrganizations organizations;

    /**
     * Initializes object with given Organizations client.
     *
     * @param organizations AWS Organizations client.
     */
    public OrganizationManager(AWSOrganizations organizations)
    {
        this.organizations = organizations;
    }

    /**
     * Handles organization creation.
     *
     * @param input Resource creation request.
     * @return Data about published version.
     */
    public CustomResourceResponse<Organization> sync(OrganizationRequest input)
    {
        try {
            DescribeOrganizationResult result = this.organizations.describeOrganization(
                new DescribeOrganizationRequest()
            );

            this.logger.info("Organization already exists (ARN {}).", result.getOrganization().getArn());

            return new CustomResourceResponse<>(result.getOrganization(), result.getOrganization().getId());
        } catch (SdkBaseException error) {
            this.logger.info("Exception occurred during organization data fetching, probably doesn't exist.", error);

            CreateOrganizationResult result = this.organizations.createOrganization(
                new CreateOrganizationRequest()
                    .withFeatureSet(input.getFeatureSet())
            );

            this.logger.info("Created new organization, ARN {}.", result.getOrganization().getArn());

            return new CustomResourceResponse<>(result.getOrganization(), result.getOrganization().getId());
        }
    }

    /**
     * Handles organization deletion.
     *
     * @param input Resource delete request.
     * @return Empty response.
     */
    public CustomResourceResponse<Organization> delete(OrganizationRequest input)
    {
        this.organizations.deleteOrganization(new DeleteOrganizationRequest());

        this.logger.info("Organization deleted.");

        return new CustomResourceResponse<>(null);
    }
}
