/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.stackset.instance.service;

import java.util.Collections;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackInstancesRequest;
import com.amazonaws.services.cloudformation.model.CreateStackInstancesResult;
import com.amazonaws.services.cloudformation.model.DeleteStackInstancesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackInstanceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackInstanceResult;
import com.amazonaws.services.cloudformation.model.StackInstance;
import com.amazonaws.services.cloudformation.model.StackInstanceNotFoundException;
import com.amazonaws.services.cloudformation.model.UpdateStackInstancesRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackInstancesResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.commons.aws.cloudformation.StackSetHandler;
import pl.wrzasq.lambda.cform.stackset.instance.model.StackInstanceRequest;
import pl.wrzasq.lambda.cform.stackset.instance.service.StackSetInstanceManager;

@ExtendWith(MockitoExtension.class)
public class StackSetInstanceManagerTest
{
    private static final String PARAMETER_KEY = "param1";

    private static final String PARAMETER_VALUE = "test value";

    private static final String STACK_SET_NAME = "test-stack-set";

    private static final String ACCOUNT_ID = "012345";

    private static final String REGION = Regions.EU_CENTRAL_1.getName();

    private static final String OPERATION_ID = "abcdef";

    private static final String PHYSICAL_ID = String.format(
        "%s:%s:%s",
        StackSetInstanceManagerTest.STACK_SET_NAME,
        StackSetInstanceManagerTest.ACCOUNT_ID,
        StackSetInstanceManagerTest.REGION
    );

    private static final Map<String, String> PARAMETERS = Collections.singletonMap(
        StackSetInstanceManagerTest.PARAMETER_KEY,
        StackSetInstanceManagerTest.PARAMETER_VALUE
    );

    @Mock
    private AmazonCloudFormation cloudFormation;

    @Mock
    private StackSetHandler stackSetHandler;

    @Captor
    ArgumentCaptor<DescribeStackInstanceRequest> describeRequest;

    @Captor
    ArgumentCaptor<CreateStackInstancesRequest> createRequest;

    @Captor
    ArgumentCaptor<UpdateStackInstancesRequest> updateRequest;

    @Captor
    ArgumentCaptor<DeleteStackInstancesRequest> deleteRequest;

    @Test
    public void deployStackInstance()
    {
        StackInstance stackInstance = new StackInstance();

        StackSetInstanceManager manager = this.createStackSetInstanceManager();

        Mockito
            .when(this.cloudFormation.describeStackInstance(this.describeRequest.capture()))
            .thenThrow(StackInstanceNotFoundException.class)
            .thenReturn(
                new DescribeStackInstanceResult()
                    .withStackInstance(stackInstance)
            );
        Mockito
            .when(this.cloudFormation.createStackInstances(this.createRequest.capture()))
            .thenReturn(
                new CreateStackInstancesResult()
                    .withOperationId(StackSetInstanceManagerTest.OPERATION_ID)
            );

        CustomResourceResponse<StackInstance> result = manager.deployStackInstance(
            StackSetInstanceManagerTest.createStackInstanceRequest(),
            null
        );

        Mockito
            .verify(this.cloudFormation, Mockito.never())
            .updateStackInstances(Mockito.any(UpdateStackInstancesRequest.class));
        Mockito
            .verify(this.stackSetHandler)
            .waitForStackSetOperation(
                StackSetInstanceManagerTest.STACK_SET_NAME,
                StackSetInstanceManagerTest.OPERATION_ID
            );

        Assertions.assertEquals(
            StackSetInstanceManagerTest.STACK_SET_NAME,
            this.describeRequest.getAllValues().get(0).getStackSetName(),
            "StackSetInstanceManager.deployStackInstance() should request details of subject stack set."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.ACCOUNT_ID,
            this.describeRequest.getAllValues().get(0).getStackInstanceAccount(),
            "StackSetInstanceManager.deployStackInstance() should request stack details from specified account."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.REGION,
            this.describeRequest.getAllValues().get(0).getStackInstanceRegion(),
            "StackSetInstanceManager.deployStackInstance() should request stack details in specified region."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.STACK_SET_NAME,
            this.describeRequest.getAllValues().get(1).getStackSetName(),
            "StackSetInstanceManager.deployStackInstance() should request details of subject stack set."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.ACCOUNT_ID,
            this.describeRequest.getAllValues().get(1).getStackInstanceAccount(),
            "StackSetInstanceManager.deployStackInstance() should request stack details from specified account."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.REGION,
            this.describeRequest.getAllValues().get(1).getStackInstanceRegion(),
            "StackSetInstanceManager.deployStackInstance() should request stack details in specified region."
        );

        CreateStackInstancesRequest request = this.createRequest.getValue();
        Assertions.assertEquals(
            StackSetInstanceManagerTest.STACK_SET_NAME,
            request.getStackSetName(),
            "StackSetInstanceManager.deployStackInstance() should create instance of specified stack set."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.ACCOUNT_ID,
            request.getAccounts().get(0),
            "StackSetInstanceManager.deployStackInstance() should create instance in specified account."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.REGION,
            request.getRegions().get(0),
            "StackSetInstanceManager.deployStackInstance() should create instance in specified region."
        );
        Assertions.assertEquals(
            1,
            request.getParameterOverrides().size(),
            "StackSetInstanceManager.deployStackInstance() should deploy stack instance with specified parameters."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.PARAMETER_KEY,
            request.getParameterOverrides().get(0).getParameterKey(),
            "StackSetInstanceManager.deployStackInstance() should deploy stack instance with specified parameters."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.PARAMETER_VALUE,
            request.getParameterOverrides().get(0).getParameterValue(),
            "StackSetInstanceManager.deployStackInstance() should deploy stack instance with specified parameters."
        );

        Assertions.assertEquals(
            StackSetInstanceManagerTest.PHYSICAL_ID,
            result.getPhysicalResourceId(),
            "StackSetInstanceManager.deployStackInstance() should set compound physical resource ID."
        );
        Assertions.assertSame(
            stackInstance,
            result.getData(),
            "StackSetInstanceManager.deployStackInstance() should return stack instance data as resource properties."
        );
    }

    @Test
    public void deployStackInstanceUpdate()
    {
        StackInstance stackInstance = new StackInstance();

        StackSetInstanceManager manager = this.createStackSetInstanceManager();

        Mockito
            .when(this.cloudFormation.describeStackInstance(this.describeRequest.capture()))
            .thenReturn(
                new DescribeStackInstanceResult()
                    .withStackInstance(stackInstance)
            );
        Mockito
            .when(this.cloudFormation.updateStackInstances(this.updateRequest.capture()))
            .thenReturn(
                new UpdateStackInstancesResult()
                    .withOperationId(StackSetInstanceManagerTest.OPERATION_ID)
            );

        CustomResourceResponse<StackInstance> result = manager.deployStackInstance(
            StackSetInstanceManagerTest.createStackInstanceRequest(),
            null
        );

        Mockito
            .verify(this.cloudFormation, Mockito.never())
            .createStackInstances(Mockito.any(CreateStackInstancesRequest.class));
        Mockito
            .verify(this.stackSetHandler)
            .waitForStackSetOperation(
                StackSetInstanceManagerTest.STACK_SET_NAME,
                StackSetInstanceManagerTest.OPERATION_ID
            );

        Assertions.assertEquals(
            StackSetInstanceManagerTest.STACK_SET_NAME,
            this.describeRequest.getAllValues().get(0).getStackSetName(),
            "StackSetInstanceManager.deployStackInstance() should request details of subject stack set."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.ACCOUNT_ID,
            this.describeRequest.getAllValues().get(0).getStackInstanceAccount(),
            "StackSetInstanceManager.deployStackInstance() should request stack details from specified account."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.REGION,
            this.describeRequest.getAllValues().get(0).getStackInstanceRegion(),
            "StackSetInstanceManager.deployStackInstance() should request stack details in specified region."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.STACK_SET_NAME,
            this.describeRequest.getAllValues().get(1).getStackSetName(),
            "StackSetInstanceManager.deployStackInstance() should request details of subject stack set."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.ACCOUNT_ID,
            this.describeRequest.getAllValues().get(1).getStackInstanceAccount(),
            "StackSetInstanceManager.deployStackInstance() should request stack details from specified account."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.REGION,
            this.describeRequest.getAllValues().get(1).getStackInstanceRegion(),
            "StackSetInstanceManager.deployStackInstance() should request stack details in specified region."
        );

        UpdateStackInstancesRequest request = this.updateRequest.getValue();
        Assertions.assertEquals(
            StackSetInstanceManagerTest.STACK_SET_NAME,
            request.getStackSetName(),
            "StackSetInstanceManager.deployStackInstance() should update instance of specified stack set."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.ACCOUNT_ID,
            request.getAccounts().get(0),
            "StackSetInstanceManager.deployStackInstance() should update instance in specified account."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.REGION,
            request.getRegions().get(0),
            "StackSetInstanceManager.deployStackInstance() should update instance in specified region."
        );
        Assertions.assertEquals(
            1,
            request.getParameterOverrides().size(),
            "StackSetInstanceManager.deployStackInstance() should deploy stack instance with specified parameters."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.PARAMETER_KEY,
            request.getParameterOverrides().get(0).getParameterKey(),
            "StackSetInstanceManager.deployStackInstance() should deploy stack instance with specified parameters."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.PARAMETER_VALUE,
            request.getParameterOverrides().get(0).getParameterValue(),
            "StackSetInstanceManager.deployStackInstance() should deploy stack instance with specified parameters."
        );

        Assertions.assertEquals(
            StackSetInstanceManagerTest.PHYSICAL_ID,
            result.getPhysicalResourceId(),
            "StackSetInstanceManager.deployStackInstance() should set compound physical resource ID."
        );
        Assertions.assertSame(
            stackInstance,
            result.getData(),
            "StackSetInstanceManager.deployStackInstance() should return stack instance data as resource properties."
        );
    }

    @Test
    public void deployStackSetInstanceNullParameters()
    {
        StackInstance stackInstance = new StackInstance();

        StackSetInstanceManager manager = this.createStackSetInstanceManager();

        Mockito
            .when(this.cloudFormation.describeStackInstance(this.describeRequest.capture()))
            .thenThrow(StackInstanceNotFoundException.class)
            .thenReturn(
                new DescribeStackInstanceResult()
                    .withStackInstance(stackInstance)
            );
        Mockito
            .when(this.cloudFormation.createStackInstances(this.createRequest.capture()))
            .thenReturn(
                new CreateStackInstancesResult()
                    .withOperationId(StackSetInstanceManagerTest.OPERATION_ID)
            );

        StackInstanceRequest input = StackSetInstanceManagerTest.createStackInstanceRequest();
        input.setParameterOverrides(null);

        manager.deployStackInstance(input, null);

        CreateStackInstancesRequest request = this.createRequest.getValue();
        Assertions.assertTrue(
            request.getParameterOverrides().isEmpty(),
            "StackSetInstanceManager.deployStackInstance() should set empty mapping if no parameters were given."
        );
    }

    @Test
    public void deleteStackInstance()
    {
        StackSetInstanceManager manager = this.createStackSetInstanceManager();

        manager.deleteStackInstance(null, StackSetInstanceManagerTest.PHYSICAL_ID);

        Mockito.verify(this.cloudFormation).deleteStackInstances(this.deleteRequest.capture());

        Assertions.assertEquals(
            StackSetInstanceManagerTest.STACK_SET_NAME,
            this.deleteRequest.getValue().getStackSetName(),
            "StackSetInstanceManager.deleteStackInstance() should request deletion of specified stack set instance."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.ACCOUNT_ID,
            this.deleteRequest.getValue().getAccounts().get(0),
            "StackSetInstanceManager.deleteStackInstance() should request deletion in specified account."
        );
        Assertions.assertEquals(
            StackSetInstanceManagerTest.REGION,
            this.deleteRequest.getValue().getRegions().get(0),
            "StackSetInstanceManager.deleteStackInstance() should request deletion in specified region."
        );
    }

    private static StackInstanceRequest createStackInstanceRequest()
    {
        StackInstanceRequest request = new StackInstanceRequest();
        request.setStackSetName(StackSetInstanceManagerTest.STACK_SET_NAME);
        request.setAccountId(StackSetInstanceManagerTest.ACCOUNT_ID);
        request.setRegion(StackSetInstanceManagerTest.REGION);
        request.setParameterOverrides(StackSetInstanceManagerTest.PARAMETERS);
        return request;
    }
    
    private StackSetInstanceManager createStackSetInstanceManager()
    {
        return new StackSetInstanceManager(this.cloudFormation, this.stackSetHandler);
    }
}
