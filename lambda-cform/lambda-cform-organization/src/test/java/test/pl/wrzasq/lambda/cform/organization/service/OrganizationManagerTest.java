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
import com.amazonaws.services.organizations.model.ListRootsRequest;
import com.amazonaws.services.organizations.model.ListRootsResult;
import com.amazonaws.services.organizations.model.Organization;
import com.amazonaws.services.organizations.model.OrganizationFeatureSet;
import com.amazonaws.services.organizations.model.Root;
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
import pl.wrzasq.lambda.cform.organization.model.OrganizationWithRoot;
import pl.wrzasq.lambda.cform.organization.service.OrganizationManager;

@ExtendWith(MockitoExtension.class)
public class OrganizationManagerTest
{
    private static final OrganizationFeatureSet FEATURE_SET = OrganizationFeatureSet.ALL;

    private static final String PHYSICAL_ID_1 = "o-test";

    private static final String PHYSICAL_ID_2 = "o-another";

    @Mock
    private AWSOrganizations organizations;

    @Captor
    ArgumentCaptor<CreateOrganizationRequest> createRequest;

    @Test
    public void sync()
    {
        Organization organization = new Organization()
            .withId(OrganizationManagerTest.PHYSICAL_ID_1);
        Root root = new Root();

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
        Mockito
            .when(this.organizations.listRoots(Mockito.any(ListRootsRequest.class)))
            .thenReturn(
                new ListRootsResult()
                    .withRoots(root)
            );

        CustomResourceResponse<OrganizationWithRoot> result = manager.sync(input, null);

        Mockito.verify(this.organizations).describeOrganization(Mockito.any(DescribeOrganizationRequest.class));
        Mockito.verify(this.organizations).createOrganization(Mockito.any(CreateOrganizationRequest.class));

        Assertions.assertEquals(
            OrganizationManagerTest.FEATURE_SET.name(),
            this.createRequest.getValue().getFeatureSet(),
            "OrganizationManager.sync() should pass specified feature set for created organization."
        );
        Assertions.assertSame(
            organization,
            result.getData().getOrganization(),
            "OrganizationManager.sync() should return organization data of created organization."
        );
        Assertions.assertSame(
            root,
            result.getData().getRoot(),
            "OrganizationManager.sync() should return organization root of created organization."
        );
        Assertions.assertEquals(
            OrganizationManagerTest.PHYSICAL_ID_1,
            result.getPhysicalResourceId(),
            "OrganizationManager.sync() should return organization ID as physical ID."
        );
    }

    @Test
    public void syncAlreadyExists()
    {
        Organization organization = new Organization()
            .withId(OrganizationManagerTest.PHYSICAL_ID_1);
        Root root = new Root();

        OrganizationManager manager = new OrganizationManager(this.organizations);

        OrganizationRequest input = new OrganizationRequest();

        Mockito
            .when(this.organizations.describeOrganization(Mockito.any(DescribeOrganizationRequest.class)))
            .thenReturn(
                new DescribeOrganizationResult()
                    .withOrganization(organization)
            );
        Mockito
            .when(this.organizations.listRoots(Mockito.any(ListRootsRequest.class)))
            .thenReturn(
                new ListRootsResult()
                    .withRoots(root)
            );

        CustomResourceResponse<OrganizationWithRoot> result = manager.sync(
            input,
            OrganizationManagerTest.PHYSICAL_ID_1
        );

        Mockito.verify(this.organizations).describeOrganization(Mockito.any(DescribeOrganizationRequest.class));
        Mockito.verify(this.organizations, Mockito.never()).createOrganization(Mockito.any());

        Assertions.assertSame(
            organization,
            result.getData().getOrganization(),
            "OrganizationManager.sync() should return organization data of existing organization."
        );
        Assertions.assertSame(
            root,
            result.getData().getRoot(),
            "OrganizationManager.sync() should return organization root of existing organization."
        );
        Assertions.assertEquals(
            OrganizationManagerTest.PHYSICAL_ID_1,
            result.getPhysicalResourceId(),
            "OrganizationManager.sync() should return organization ID as physical ID."
        );
    }

    @Test
    public void syncAlreadyExistsOutOfSync()
    {
        Organization organization = new Organization()
            .withId(OrganizationManagerTest.PHYSICAL_ID_1);
        Root root = new Root();

        OrganizationManager manager = new OrganizationManager(this.organizations);

        OrganizationRequest input = new OrganizationRequest();

        Mockito
            .when(this.organizations.describeOrganization(Mockito.any(DescribeOrganizationRequest.class)))
            .thenReturn(
                new DescribeOrganizationResult()
                    .withOrganization(organization)
            );
        Mockito
            .when(this.organizations.listRoots(Mockito.any(ListRootsRequest.class)))
            .thenReturn(
                new ListRootsResult()
                    .withRoots(root)
            );

        CustomResourceResponse<OrganizationWithRoot> result = manager.sync(
            input,
            OrganizationManagerTest.PHYSICAL_ID_2
        );

        Mockito.verify(this.organizations).describeOrganization(Mockito.any(DescribeOrganizationRequest.class));
        Mockito.verify(this.organizations, Mockito.never()).createOrganization(Mockito.any());

        Assertions.assertSame(
            organization,
            result.getData().getOrganization(),
            "OrganizationManager.sync() should return organization data of existing organization."
        );
        Assertions.assertSame(
            root,
            result.getData().getRoot(),
            "OrganizationManager.sync() should return organization root of existing organization."
        );
        Assertions.assertEquals(
            OrganizationManagerTest.PHYSICAL_ID_1,
            result.getPhysicalResourceId(),
            "OrganizationManager.sync() should return organization ID as physical ID."
        );
    }

    @Test
    public void delete()
    {
        Mockito
            .when(this.organizations.describeOrganization(Mockito.any(DescribeOrganizationRequest.class)))
            .thenReturn(
                new DescribeOrganizationResult()
                    .withOrganization(
                        new Organization()
                            .withId(OrganizationManagerTest.PHYSICAL_ID_1)
                    )
            );

        OrganizationManager manager = new OrganizationManager(this.organizations);

        manager.delete(null, OrganizationManagerTest.PHYSICAL_ID_1);

        Mockito.verify(this.organizations).deleteOrganization(Mockito.any(DeleteOrganizationRequest.class));
    }

    @Test
    public void deleteOutOfSync()
    {
        Mockito
            .when(this.organizations.describeOrganization(Mockito.any(DescribeOrganizationRequest.class)))
            .thenReturn(
                new DescribeOrganizationResult()
                    .withOrganization(
                        new Organization()
                            .withId(OrganizationManagerTest.PHYSICAL_ID_1)
                    )
            );

        OrganizationManager manager = new OrganizationManager(this.organizations);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> manager.delete(null, OrganizationManagerTest.PHYSICAL_ID_2)
        );

        Mockito
            .verify(this.organizations, Mockito.never())
            .deleteOrganization(Mockito.any(DeleteOrganizationRequest.class));
    }
}
