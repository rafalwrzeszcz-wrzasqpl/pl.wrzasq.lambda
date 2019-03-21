/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.organization.service;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.organizations.AWSOrganizations;
import com.amazonaws.services.organizations.model.CreateOrganizationRequest;
import com.amazonaws.services.organizations.model.CreateOrganizationResult;
import com.amazonaws.services.organizations.model.DeleteOrganizationRequest;
import com.amazonaws.services.organizations.model.DescribeOrganizationRequest;
import com.amazonaws.services.organizations.model.DescribeOrganizationResult;
import com.amazonaws.services.organizations.model.Organization;
import com.amazonaws.services.organizations.model.OrganizationFeatureSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.organization.model.OrganizationRequest;
import pl.wrzasq.lambda.cform.organization.service.OrganizationManager;

@ExtendWith(MockitoExtension.class)
public class OrganizationManagerTest
{
    private static final OrganizationFeatureSet FEATURE_SET = OrganizationFeatureSet.ALL;

    @Mock
    private AWSOrganizations organizations;

    @Captor
    ArgumentCaptor<CreateOrganizationRequest> createRequest;

    @Test
    public void sync()
    {
        String id = "o-test";

        Organization organization = new Organization();
        organization.setId(id);

        OrganizationManager manager = new OrganizationManager(this.organizations);

        OrganizationRequest input = new OrganizationRequest();
        input.setFeatureSet(OrganizationManagerTest.FEATURE_SET);

        Mockito
            .when(this.organizations.describeOrganization(Mockito.any(DescribeOrganizationRequest.class)))
            .thenThrow(SdkBaseException.class);
        Mockito
            .when(this.organizations.createOrganization(this.createRequest.capture()))
            .thenReturn(
                new CreateOrganizationResult()
                    .withOrganization(organization)
            );

        CustomResourceResponse<Organization> result = manager.sync(input, null);

        Mockito.verify(this.organizations).describeOrganization(Mockito.any(DescribeOrganizationRequest.class));
        Mockito.verify(this.organizations).createOrganization(Mockito.any(CreateOrganizationRequest.class));

        Assertions.assertEquals(
            OrganizationManagerTest.FEATURE_SET.name(),
            this.createRequest.getValue().getFeatureSet(),
            "OrganizationManager.sync() should pass specified feature set for created organization."
        );
        Assertions.assertSame(
            organization,
            result.getData(),
            "OrganizationManager.sync() should return organization data of created organization."
        );
        Assertions.assertEquals(
            id,
            result.getPhysicalResourceId(),
            "OrganizationManager.sync() should return organization ID as physical ID."
        );
    }

    @Test
    public void syncAlreadyExists()
    {
        String id = "o-test";

        Organization organization = new Organization();
        organization.setId(id);

        OrganizationManager manager = new OrganizationManager(this.organizations);

        OrganizationRequest input = new OrganizationRequest();

        Mockito
            .when(this.organizations.describeOrganization(Mockito.any(DescribeOrganizationRequest.class)))
            .thenReturn(
                new DescribeOrganizationResult()
                    .withOrganization(organization)
            );

        CustomResourceResponse<Organization> result = manager.sync(input, id);

        Mockito.verify(this.organizations).describeOrganization(Mockito.any(DescribeOrganizationRequest.class));
        Mockito.verify(this.organizations, Mockito.never()).createOrganization(Mockito.any());

        Assertions.assertSame(
            organization,
            result.getData(),
            "OrganizationManager.sync() should return organization data of existing organization."
        );
        Assertions.assertEquals(
            id,
            result.getPhysicalResourceId(),
            "OrganizationManager.sync() should return organization ID as physical ID."
        );
    }

    @Test
    public void syncAlreadyExistsOutOfSync()
    {
        String physicalResourceId = "o-another";
        String id = "o-test";

        Organization organization = new Organization();
        organization.setId(id);

        OrganizationManager manager = new OrganizationManager(this.organizations);

        OrganizationRequest input = new OrganizationRequest();

        Mockito
            .when(this.organizations.describeOrganization(Mockito.any(DescribeOrganizationRequest.class)))
            .thenReturn(
                new DescribeOrganizationResult()
                    .withOrganization(organization)
            );

        CustomResourceResponse<Organization> result = manager.sync(input, physicalResourceId);

        Mockito.verify(this.organizations).describeOrganization(Mockito.any(DescribeOrganizationRequest.class));
        Mockito.verify(this.organizations, Mockito.never()).createOrganization(Mockito.any());

        Assertions.assertSame(
            organization,
            result.getData(),
            "OrganizationManager.sync() should return organization data of existing organization."
        );
        Assertions.assertEquals(
            physicalResourceId,
            result.getPhysicalResourceId(),
            "OrganizationManager.sync() should preserve physical resource ID to avoid cleanup phase calls."
        );
    }

    @Test
    public void delete()
    {
        OrganizationManager manager = new OrganizationManager(this.organizations);

        manager.delete(null, null);

        Mockito.verify(this.organizations).deleteOrganization(Mockito.any(DeleteOrganizationRequest.class));
    }
}
