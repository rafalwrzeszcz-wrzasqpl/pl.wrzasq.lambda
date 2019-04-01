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
import com.amazonaws.services.organizations.model.DeleteOrganizationRequest;
import com.amazonaws.services.organizations.model.DescribeOrganizationRequest;
import com.amazonaws.services.organizations.model.ListRootsRequest;
import com.amazonaws.services.organizations.model.Organization;
import com.amazonaws.services.organizations.model.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.organization.model.OrganizationRequest;
import pl.wrzasq.lambda.cform.organization.model.OrganizationResponse;

/**
 * Organizations API implementation.
 */
public class OrganizationManager {
    /**
     * Message pattern for drift case.
     */
    private static final String DRIFT_LOG_MESSAGE_PATTERN
        = "Organization ID {} differs from CloudFormation-provided physical resource ID {}.";

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
    public OrganizationManager(AWSOrganizations organizations) {
        this.organizations = organizations;
    }

    /**
     * Handles organization creation.
     *
     * @param input Resource creation request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about published version.
     */
    public CustomResourceResponse<OrganizationResponse> sync(OrganizationRequest input, String physicalResourceId) {
        Organization organization;
        try {
            organization = this.organizations.describeOrganization(
                new DescribeOrganizationRequest()
            )
                .getOrganization();

            // don't do anything here - in worst case it will fail in Delete call in UPDATE_COMPLETE_CLEANUP phase
            // it will not hurt us (even if fails, as it's post-update phase) and allows to simplify logic of this
            // method to avoid handling all combinations, which could lead to unhandled edge cases
            if (!organization.getId().equals(physicalResourceId)) {
                this.logger.warn(
                    OrganizationManager.DRIFT_LOG_MESSAGE_PATTERN,
                    organization.getId(),
                    physicalResourceId
                );
            }

            this.logger.info("Organization already exists (ARN {}).", organization.getArn());
        } catch (SdkBaseException error) {
            this.logger.info("Exception occurred during organization data fetching, probably doesn't exist.", error);

            organization = this.organizations.createOrganization(
                new CreateOrganizationRequest()
                    .withFeatureSet(input.getFeatureSet())
            )
                .getOrganization();

            this.logger.info("Created new organization, ARN {}.", organization.getArn());
        }

        Root root = this.organizations.listRoots(new ListRootsRequest()).getRoots().get(0);

        OrganizationResponse organizationResponse = new OrganizationResponse();
        organizationResponse.setId(organization.getId());
        organizationResponse.setArn(organization.getArn());
        organizationResponse.setRootId(root.getId());

        return new CustomResourceResponse<>(organizationResponse, organization.getId());
    }

    /**
     * Handles organization deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<OrganizationResponse> delete(OrganizationRequest input, String physicalResourceId) {
        Organization organization = this.organizations.describeOrganization(
            new DescribeOrganizationRequest()
        )
            .getOrganization();

        // avoid removing unknown data
        if (!organization.getId().equals(physicalResourceId)) {
            this.logger.error(
                OrganizationManager.DRIFT_LOG_MESSAGE_PATTERN,
                organization.getId(),
                physicalResourceId
            );
            throw new IllegalStateException(
                String.format(
                    "Can not delete Organization - ID %s doesn't match CloudFormation-provided resource ID %s.",
                    organization.getId(),
                    physicalResourceId
                )
            );
        }

        this.organizations.deleteOrganization(new DeleteOrganizationRequest());

        this.logger.info("Organization deleted.");

        return new CustomResourceResponse<>(null, physicalResourceId);
    }
}
