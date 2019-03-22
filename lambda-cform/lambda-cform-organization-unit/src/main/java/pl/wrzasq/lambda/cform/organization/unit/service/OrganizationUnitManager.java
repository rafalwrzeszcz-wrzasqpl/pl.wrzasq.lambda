/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.organization.unit.service;

import com.amazonaws.services.organizations.AWSOrganizations;
import com.amazonaws.services.organizations.model.ChildNotFoundException;
import com.amazonaws.services.organizations.model.CreateOrganizationalUnitRequest;
import com.amazonaws.services.organizations.model.DeleteOrganizationalUnitRequest;
import com.amazonaws.services.organizations.model.ListParentsRequest;
import com.amazonaws.services.organizations.model.OrganizationalUnit;
import com.amazonaws.services.organizations.model.Parent;
import com.amazonaws.services.organizations.model.UpdateOrganizationalUnitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.organization.unit.model.OrganizationUnitRequest;

/**
 * Organizations API implementation.
 */
public class OrganizationUnitManager
{
    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(OrganizationUnitManager.class);

    /**
     * AWS Organizations API client.
     */
    private AWSOrganizations organizations;

    /**
     * Initializes object with given Organizations client.
     *
     * @param organizations AWS Organizations client.
     */
    public OrganizationUnitManager(AWSOrganizations organizations)
    {
        this.organizations = organizations;
    }

    /**
     * Handles organization creation.
     *
     * @param input Resource creation request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about published version.
     */
    public CustomResourceResponse<OrganizationalUnit> sync(OrganizationUnitRequest input, String physicalResourceId)
    {
        // check if the parent ID got changed
        if (physicalResourceId != null) {
            try {
                Parent parent = this.organizations.listParents(
                    new ListParentsRequest()
                        .withChildId(physicalResourceId)
                )
                    .getParents()
                    .get(0);

                // organizational unit can only be renamed, change of parent will require creation of new unit
                if (!input.getParentId().equals(parent.getId())) {
                    this.logger.info(
                        "Organizational Unit with ID {} was requested to be placed in a different parent."
                            + " This will cause it to be re-created."
                            + " Old parent ID: {}, new parent ID: {}.",
                        physicalResourceId,
                        parent.getId(),
                        input.getParentId()
                    );

                    physicalResourceId = null;
                }
            } catch (ChildNotFoundException error) {
                // it's fine, we will just create new one
                this.logger.warn("Organizational Unit with ID {} not found, creating new one.", physicalResourceId);

                physicalResourceId = null;
            }
        }

        OrganizationalUnit unit = physicalResourceId == null
            ? this.organizations.createOrganizationalUnit(
                new CreateOrganizationalUnitRequest()
                    .withName(input.getName())
                    .withParentId(input.getParentId())
            )
                .getOrganizationalUnit()
            : this.organizations.updateOrganizationalUnit(
                new UpdateOrganizationalUnitRequest()
                    .withOrganizationalUnitId(physicalResourceId)
                    .withName(input.getName())
            )
                .getOrganizationalUnit();

        return new CustomResourceResponse<>(unit, unit.getId());
    }

    /**
     * Handles organization unit deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<OrganizationalUnit> delete(OrganizationUnitRequest input, String physicalResourceId)
    {
        this.organizations.deleteOrganizationalUnit(
            new DeleteOrganizationalUnitRequest()
                .withOrganizationalUnitId(physicalResourceId)
        );

        this.logger.info("Organizational unit {} deleted.", physicalResourceId);

        return new CustomResourceResponse<>(null, physicalResourceId);
    }
}
