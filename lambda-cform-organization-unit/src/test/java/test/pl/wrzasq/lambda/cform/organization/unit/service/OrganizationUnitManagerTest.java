/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.organization.unit.service;

import com.amazonaws.services.organizations.AWSOrganizations;
import com.amazonaws.services.organizations.model.ChildNotFoundException;
import com.amazonaws.services.organizations.model.CreateOrganizationalUnitRequest;
import com.amazonaws.services.organizations.model.CreateOrganizationalUnitResult;
import com.amazonaws.services.organizations.model.DeleteOrganizationalUnitRequest;
import com.amazonaws.services.organizations.model.ListParentsRequest;
import com.amazonaws.services.organizations.model.ListParentsResult;
import com.amazonaws.services.organizations.model.OrganizationalUnit;
import com.amazonaws.services.organizations.model.Parent;
import com.amazonaws.services.organizations.model.UpdateOrganizationalUnitRequest;
import com.amazonaws.services.organizations.model.UpdateOrganizationalUnitResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.cform.organization.unit.model.OrganizationUnitRequest;
import pl.wrzasq.lambda.cform.organization.unit.service.OrganizationUnitManager;

@ExtendWith(MockitoExtension.class)
public class OrganizationUnitManagerTest {
    private static final String PHYSICAL_RESOURCE_ID = "ou-123";

    private static final String OU_NAME = "test";

    private static final String PARENT_ID_1 = "r-root";

    private static final String PARENT_ID_2 = "ou-leaf";

    @Mock
    private AWSOrganizations organizations;

    @Captor
    ArgumentCaptor<ListParentsRequest> listParentsRequest;

    @Captor
    ArgumentCaptor<CreateOrganizationalUnitRequest> createRequest;

    @Captor
    ArgumentCaptor<UpdateOrganizationalUnitRequest> updateRequest;

    @Captor
    ArgumentCaptor<DeleteOrganizationalUnitRequest> deleteRequest;

    @Test
    public void sync() {
        var unit = new OrganizationalUnit();
        unit.setId(OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID);

        var manager = new OrganizationUnitManager(this.organizations);

        var input = new OrganizationUnitRequest();
        input.setName(OrganizationUnitManagerTest.OU_NAME);
        input.setParentId(OrganizationUnitManagerTest.PARENT_ID_1);

        Mockito
            .when(this.organizations.createOrganizationalUnit(this.createRequest.capture()))
            .thenReturn(
                new CreateOrganizationalUnitResult()
                    .withOrganizationalUnit(unit)
            );

        var result = manager.sync(input, null);

        Mockito
            .verify(this.organizations, Mockito.never())
            .listParents(Mockito.any(ListParentsRequest.class));
        Mockito
            .verify(this.organizations, Mockito.never())
            .updateOrganizationalUnit(Mockito.any(UpdateOrganizationalUnitRequest.class));

        Assertions.assertSame(
            unit,
            result.getData(),
            "OrganizationUnitManager.sync() should return data of created organizational unit."
        );
        Assertions.assertEquals(
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID,
            result.getPhysicalResourceId(),
            "OrganizationUnitManager.sync() should return unit ID as physical ID."
        );
    }

    @Test
    public void syncAlreadyExists() {
        var unit = new OrganizationalUnit();
        unit.setId(OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID);

        var manager = new OrganizationUnitManager(this.organizations);

        var input = new OrganizationUnitRequest();
        input.setName(OrganizationUnitManagerTest.OU_NAME);
        input.setParentId(OrganizationUnitManagerTest.PARENT_ID_1);

        Mockito
            .when(this.organizations.listParents(this.listParentsRequest.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(OrganizationUnitManagerTest.PARENT_ID_1)
                    )
            );
        Mockito
            .when(this.organizations.updateOrganizationalUnit(this.updateRequest.capture()))
            .thenReturn(
                new UpdateOrganizationalUnitResult()
                    .withOrganizationalUnit(unit)
            );

        var result = manager.sync(
            input,
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID
        );

        Mockito
            .verify(this.organizations, Mockito.never())
            .createOrganizationalUnit(Mockito.any(CreateOrganizationalUnitRequest.class));

        Assertions.assertSame(
            unit,
            result.getData(),
            "OrganizationUnitManager.sync() should return data of existing organizational unit."
        );
        Assertions.assertEquals(
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID,
            result.getPhysicalResourceId(),
            "OrganizationUnitManager.sync() should return unit ID as physical ID."
        );
    }

    @Test
    public void syncNotExistingPhysicalId() {
        var unit = new OrganizationalUnit();
        unit.setId(OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID);

        var manager = new OrganizationUnitManager(this.organizations);

        var input = new OrganizationUnitRequest();
        input.setName(OrganizationUnitManagerTest.OU_NAME);
        input.setParentId(OrganizationUnitManagerTest.PARENT_ID_1);

        Mockito
            .when(this.organizations.listParents(this.listParentsRequest.capture()))
            .thenThrow(ChildNotFoundException.class);
        Mockito
            .when(this.organizations.createOrganizationalUnit(this.createRequest.capture()))
            .thenReturn(
                new CreateOrganizationalUnitResult()
                    .withOrganizationalUnit(unit)
            );

        var result = manager.sync(
            input,
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID
        );

        Mockito
            .verify(this.organizations, Mockito.never())
            .updateOrganizationalUnit(Mockito.any(UpdateOrganizationalUnitRequest.class));

        Assertions.assertSame(
            unit,
            result.getData(),
            "OrganizationUnitManager.sync() should return data of created organizational unit."
        );
        Assertions.assertEquals(
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID,
            result.getPhysicalResourceId(),
            "OrganizationUnitManager.sync() should return unit ID as physical ID."
        );
    }

    @Test
    public void syncChangedParent() {
        var unit = new OrganizationalUnit();
        unit.setId(OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID);

        var manager = new OrganizationUnitManager(this.organizations);

        var input = new OrganizationUnitRequest();
        input.setName(OrganizationUnitManagerTest.OU_NAME);
        input.setParentId(OrganizationUnitManagerTest.PARENT_ID_1);

        Mockito
            .when(this.organizations.listParents(this.listParentsRequest.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(OrganizationUnitManagerTest.PARENT_ID_2)
                    )
            );
        Mockito
            .when(this.organizations.createOrganizationalUnit(this.createRequest.capture()))
            .thenReturn(
                new CreateOrganizationalUnitResult()
                    .withOrganizationalUnit(unit)
            );

        var result = manager.sync(
            input,
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID
        );

        Mockito
            .verify(this.organizations, Mockito.never())
            .updateOrganizationalUnit(Mockito.any(UpdateOrganizationalUnitRequest.class));

        Assertions.assertSame(
            unit,
            result.getData(),
            "OrganizationUnitManager.sync() should return data of created organizational unit."
        );
        Assertions.assertEquals(
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID,
            result.getPhysicalResourceId(),
            "OrganizationUnitManager.sync() should return unit ID as physical ID."
        );
    }

    @Test
    public void delete() {
        var manager = new OrganizationUnitManager(this.organizations);

        manager.delete(null, OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID);

        Mockito.verify(this.organizations).deleteOrganizationalUnit(this.deleteRequest.capture());

        Assertions.assertEquals(
            OrganizationUnitManagerTest.PHYSICAL_RESOURCE_ID,
            this.deleteRequest.getValue().getOrganizationalUnitId(),
            "OrganizationUnitManager.delete() should request deletion of specified physical resource ID."
        );
    }
}
