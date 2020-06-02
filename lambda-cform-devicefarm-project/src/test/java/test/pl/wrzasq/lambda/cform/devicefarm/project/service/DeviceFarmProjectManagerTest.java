/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2020 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.devicefarm.project.service;

import com.amazonaws.services.devicefarm.AWSDeviceFarm;
import com.amazonaws.services.devicefarm.model.CreateTestGridProjectRequest;
import com.amazonaws.services.devicefarm.model.CreateTestGridProjectResult;
import com.amazonaws.services.devicefarm.model.DeleteTestGridProjectRequest;
import com.amazonaws.services.devicefarm.model.TestGridProject;
import com.amazonaws.services.devicefarm.model.UpdateTestGridProjectRequest;
import com.amazonaws.services.devicefarm.model.UpdateTestGridProjectResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.cform.devicefarm.project.model.DeviceFarmProjectRequest;
import pl.wrzasq.lambda.cform.devicefarm.project.service.DeviceFarmProjectManager;

@ExtendWith(MockitoExtension.class)
public class DeviceFarmProjectManagerTest {
    private static final String PHYSICAL_ID = "arn:aws:project";

    private static final String PROJECT_NAME = "test";

    private static final String PROJECT_DESCRIPTION = "Test project.";

    @Mock
    private AWSDeviceFarm deviceFarm;

    @Captor
    ArgumentCaptor<CreateTestGridProjectRequest> createRequest;

    @Captor
    ArgumentCaptor<UpdateTestGridProjectRequest> updateRequest;

    @Captor
    ArgumentCaptor<DeleteTestGridProjectRequest> deleteRequest;

    @Test
    public void create() {
        var manager = new DeviceFarmProjectManager(this.deviceFarm);

        var request = new DeviceFarmProjectRequest();
        request.setName(DeviceFarmProjectManagerTest.PROJECT_NAME);
        request.setDescription(DeviceFarmProjectManagerTest.PROJECT_DESCRIPTION);

        var project = new TestGridProject()
            .withArn(DeviceFarmProjectManagerTest.PHYSICAL_ID);

        Mockito
            .when(this.deviceFarm.createTestGridProject(Mockito.any(CreateTestGridProjectRequest.class)))
            .thenReturn(
                new CreateTestGridProjectResult()
                    .withTestGridProject(project)
            );

        var response = manager.create(request, null);

        Mockito.verify(this.deviceFarm).createTestGridProject(this.createRequest.capture());

        Assertions.assertEquals(
            DeviceFarmProjectManagerTest.PROJECT_NAME,
            this.createRequest.getValue().getName(),
            "DeviceFarmProjectManager.create() should pass desired project name to service."
        );

        Assertions.assertEquals(
            DeviceFarmProjectManagerTest.PROJECT_DESCRIPTION,
            this.createRequest.getValue().getDescription(),
            "DeviceFarmProjectManager.create() should pass desired project description to service."
        );

        Assertions.assertEquals(
            project,
            response.getData(),
            "DeviceFarmProjectManager.create() should return created project data as response properties."
        );

        Assertions.assertEquals(
            DeviceFarmProjectManagerTest.PHYSICAL_ID,
            response.getPhysicalResourceId(),
            "DeviceFarmProjectManager.create() should return created project ARN as resource physical ID."
        );
    }

    @Test
    public void update() {
        var manager = new DeviceFarmProjectManager(this.deviceFarm);

        var request = new DeviceFarmProjectRequest();
        request.setName(DeviceFarmProjectManagerTest.PROJECT_NAME);
        request.setDescription(DeviceFarmProjectManagerTest.PROJECT_DESCRIPTION);

        var project = new TestGridProject()
            .withArn(DeviceFarmProjectManagerTest.PHYSICAL_ID);

        Mockito
            .when(this.deviceFarm.updateTestGridProject(Mockito.any(UpdateTestGridProjectRequest.class)))
            .thenReturn(
                new UpdateTestGridProjectResult()
                    .withTestGridProject(project)
            );

        var response = manager.update(request, DeviceFarmProjectManagerTest.PHYSICAL_ID);

        Mockito.verify(this.deviceFarm).updateTestGridProject(this.updateRequest.capture());

        Assertions.assertEquals(
            DeviceFarmProjectManagerTest.PROJECT_NAME,
            this.updateRequest.getValue().getName(),
            "DeviceFarmProjectManager.update() should pass desired project name to service."
        );

        Assertions.assertEquals(
            DeviceFarmProjectManagerTest.PROJECT_DESCRIPTION,
            this.updateRequest.getValue().getDescription(),
            "DeviceFarmProjectManager.update() should pass desired project description to service."
        );

        Assertions.assertEquals(
            project,
            response.getData(),
            "DeviceFarmProjectManager.update() should return project data as response properties."
        );

        Assertions.assertEquals(
            DeviceFarmProjectManagerTest.PHYSICAL_ID,
            response.getPhysicalResourceId(),
            "DeviceFarmProjectManager.update() should return project ARN as resource physical ID."
        );
    }

    @Test
    public void delete() {
        var manager = new DeviceFarmProjectManager(this.deviceFarm);

        manager.delete(null, DeviceFarmProjectManagerTest.PHYSICAL_ID);

        Mockito.verify(this.deviceFarm).deleteTestGridProject(this.deleteRequest.capture());

        Assertions.assertEquals(
            DeviceFarmProjectManagerTest.PHYSICAL_ID,
            this.deleteRequest.getValue().getProjectArn(),
            "DeviceFarmProjectManager.delete() should attempt to delete given project."
        );
    }
}
