/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.stackset.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackSetRequest;
import com.amazonaws.services.cloudformation.model.CreateStackSetResult;
import com.amazonaws.services.cloudformation.model.DeleteStackSetRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackSetResult;
import com.amazonaws.services.cloudformation.model.DescribeStackSetRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackSetResult;
import com.amazonaws.services.cloudformation.model.StackSet;
import com.amazonaws.services.cloudformation.model.StackSetNotFoundException;
import com.amazonaws.services.cloudformation.model.UpdateStackSetRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackSetResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.stackset.model.StackSetRequest;
import pl.wrzasq.lambda.cform.stackset.model.StackSetResponse;
import pl.wrzasq.lambda.cform.stackset.service.StackSetManager;

@ExtendWith(MockitoExtension.class)
public class StackSetManagerTest
{
    private static final String PARAMETER_KEY = "param1";

    private static final String PARAMETER_VALUE = "test value";

    private static final String TAG_NAME = "lookup:key";

    private static final String TAG_VALUE = "wrzasqpl";

    private static final String STACK_SET_NAME = "test-stack-set";

    private static final String STACK_SET_ID_1 = "12345abc";

    private static final String STACK_SET_ID_2 = "otherid";

    private static final String STACK_SET_ARN = "arn:aws:test-stack-set";

    private static final String STACK_SET_DESCRIPTION = "test stack set";

    private static final String STACK_SET_TEMPLATE_URL = "https://s3/test";

    private static final String STACK_SET_ADMINISTRATION_ROLE_ARN = "aws:arn:iam:admin";

    private static final String STACK_SET_EXECUTION_ROLE_NAME = "OrganizationAdmin";

    private static final Set<Capability> STACK_SET_CAPABILITIES = Collections.singleton(Capability.CAPABILITY_IAM);

    private static final Map<String, String> STACK_SET_PARAMETERS = Collections.singletonMap(
        StackSetManagerTest.PARAMETER_KEY,
        StackSetManagerTest.PARAMETER_VALUE
    );

    private static final Map<String, String> STACK_SET_TAGS = Collections.singletonMap(
        StackSetManagerTest.TAG_NAME,
        StackSetManagerTest.TAG_VALUE
    );

    @Mock
    private AmazonCloudFormation cloudFormation;

    @Captor
    ArgumentCaptor<DescribeStackSetRequest> describeRequest;

    @Captor
    ArgumentCaptor<CreateStackSetRequest> createRequest;

    @Captor
    ArgumentCaptor<UpdateStackSetRequest> updateRequest;

    @Captor
    ArgumentCaptor<DeleteStackSetRequest> deleteRequest;

    @Test
    public void deployStackSet()
    {
        StackSet stackSet = new StackSet()
            .withStackSetName(StackSetManagerTest.STACK_SET_NAME)
            .withStackSetId(StackSetManagerTest.STACK_SET_ID_1)
            .withStackSetARN(StackSetManagerTest.STACK_SET_ARN);
        String operationId = "foobar";

        StackSetManager manager = new StackSetManager(this.cloudFormation);

        Mockito
            .when(this.cloudFormation.describeStackSet(this.describeRequest.capture()))
            .thenReturn(
                new DescribeStackSetResult()
                    .withStackSet(stackSet)
            );
        Mockito
            .when(this.cloudFormation.updateStackSet(this.updateRequest.capture()))
            .thenReturn(
                new UpdateStackSetResult()
                    .withOperationId(operationId)
            );

        CustomResourceResponse<StackSetResponse> result = manager.deployStackSet(
            StackSetManagerTest.createStackSetRequest(),
            StackSetManagerTest.STACK_SET_ID_1
        );

        Mockito.verify(this.cloudFormation, Mockito.never()).createStackSet(Mockito.any(CreateStackSetRequest.class));

        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_ID_1,
            result.getPhysicalResourceId(),
            "StackSetManager.deployStackSet() should set stack set ID as a physical resource ID."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_NAME,
            result.getData().getStackSetName(),
            "StackSetManager.deployStackSet() should return stack set name."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_ID_1,
            result.getData().getId(),
            "StackSetManager.deployStackSet() should return stack set id."
        );

        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_NAME,
            this.describeRequest.getValue().getStackSetName(),
            "StackSetManager.deployStackSet() should request information of stack set with specified name."
        );

        UpdateStackSetRequest request = this.updateRequest.getValue();
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_NAME,
            request.getStackSetName(),
            "StackSetManager.deployStackSet() should deploy of stack set with specified name."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_TEMPLATE_URL,
            request.getTemplateURL(),
            "StackSetManager.deployStackSet() should deploy of stack set with specified template."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_DESCRIPTION,
            request.getDescription(),
            "StackSetManager.deployStackSet() should deploy stack set with specified description."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_ADMINISTRATION_ROLE_ARN,
            request.getAdministrationRoleARN(),
            "StackSetManager.deployStackSet() should deploy stack set with specified administration role."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_EXECUTION_ROLE_NAME,
            request.getExecutionRoleName(),
            "StackSetManager.deployStackSet() should deploy stack set with specified execution role."
        );
        Assertions.assertEquals(
            1,
            request.getCapabilities().size(),
            "StackSetManager.deployStackSet() should deploy stack set with specified capabilities."
        );
        Assertions.assertTrue(
            request.getCapabilities().contains(Capability.CAPABILITY_IAM.name()),
            "StackSetManager.deployStackSet() should deploy stack set with specified capabilities."
        );
        Assertions.assertEquals(
            1,
            request.getParameters().size(),
            "StackSetManager.deployStackSet() should deploy stack set with specified parameters."
        );
        Assertions.assertEquals(
            StackSetManagerTest.PARAMETER_KEY,
            request.getParameters().get(0).getParameterKey(),
            "StackSetManager.deployStackSet() should deploy stack set with specified parameters."
        );
        Assertions.assertEquals(
            StackSetManagerTest.PARAMETER_VALUE,
            request.getParameters().get(0).getParameterValue(),
            "StackSetManager.deployStackSet() should deploy stack set with specified parameters."
        );
        Assertions.assertEquals(
            1,
            request.getTags().size(),
            "StackSetManager.deployStackSet() should deploy stack set with specified tags."
        );
        Assertions.assertEquals(
            StackSetManagerTest.TAG_NAME,
            request.getTags().get(0).getKey(),
            "StackSetManager.deployStackSet() should deploy stack set with specified tags."
        );
        Assertions.assertEquals(
            StackSetManagerTest.TAG_VALUE,
            request.getTags().get(0).getValue(),
            "StackSetManager.deployStackSet() should deploy stack set with specified tags."
        );
    }

    @Test
    public void deployStackSetOutOfSync()
    {
        String physicalResourceId = "another-id";
        StackSet stackSet = new StackSet()
            .withStackSetName(StackSetManagerTest.STACK_SET_NAME)
            .withStackSetId(StackSetManagerTest.STACK_SET_ID_1)
            .withStackSetARN(StackSetManagerTest.STACK_SET_ARN);
        String operationId = "foobar";

        StackSetManager manager = new StackSetManager(this.cloudFormation);

        Mockito
            .when(this.cloudFormation.describeStackSet(this.describeRequest.capture()))
            .thenReturn(
                new DescribeStackSetResult()
                    .withStackSet(stackSet)
            );
        Mockito
            .when(this.cloudFormation.updateStackSet(this.updateRequest.capture()))
            .thenReturn(
                new UpdateStackSetResult()
                    .withOperationId(operationId)
            );

        CustomResourceResponse<StackSetResponse> result = manager.deployStackSet(
            StackSetManagerTest.createStackSetRequest(),
            physicalResourceId
        );

        Mockito.verify(this.cloudFormation, Mockito.never()).createStackSet(Mockito.any(CreateStackSetRequest.class));

        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_ID_1,
            result.getPhysicalResourceId(),
            "StackSetManager.deployStackSet() should set stack set ID as a physical resource ID."
        );
    }

    @Test
    public void deployStackSetNotExists()
    {
        StackSetManager manager = new StackSetManager(this.cloudFormation);

        Mockito
            .when(this.cloudFormation.describeStackSet(this.describeRequest.capture()))
            .thenThrow(StackSetNotFoundException.class);
        Mockito
            .when(this.cloudFormation.createStackSet(this.createRequest.capture()))
            .thenReturn(
                new CreateStackSetResult()
                    .withStackSetId(StackSetManagerTest.STACK_SET_ID_1)
            );

        CustomResourceResponse<StackSetResponse> result = manager.deployStackSet(
            StackSetManagerTest.createStackSetRequest(),
            null
        );

        Mockito.verify(this.cloudFormation, Mockito.never()).updateStackSet(Mockito.any(UpdateStackSetRequest.class));

        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_ID_1,
            result.getPhysicalResourceId(),
            "StackSetManager.deployStackSet() should set stack set ID as a physical resource ID."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_NAME,
            result.getData().getStackSetName(),
            "StackSetManager.deployStackSet() should return stack set name."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_ID_1,
            result.getData().getId(),
            "StackSetManager.deployStackSet() should return stack set id."
        );

        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_NAME,
            this.describeRequest.getValue().getStackSetName(),
            "StackSetManager.deployStackSet() should request information of stack set with specified name."
        );

        CreateStackSetRequest request = this.createRequest.getValue();
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_NAME,
            request.getStackSetName(),
            "StackSetManager.deployStackSet() should deploy of stack set with specified name."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_TEMPLATE_URL,
            request.getTemplateURL(),
            "StackSetManager.deployStackSet() should deploy of stack set with specified template."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_DESCRIPTION,
            request.getDescription(),
            "StackSetManager.deployStackSet() should deploy stack set with specified description."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_ADMINISTRATION_ROLE_ARN,
            request.getAdministrationRoleARN(),
            "StackSetManager.deployStackSet() should deploy stack set with specified administration role."
        );
        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_EXECUTION_ROLE_NAME,
            request.getExecutionRoleName(),
            "StackSetManager.deployStackSet() should deploy stack set with specified execution role."
        );
        Assertions.assertEquals(
            1,
            request.getCapabilities().size(),
            "StackSetManager.deployStackSet() should deploy stack set with specified capabilities."
        );
        Assertions.assertTrue(
            request.getCapabilities().contains(Capability.CAPABILITY_IAM.name()),
            "StackSetManager.deployStackSet() should deploy stack set with specified capabilities."
        );
        Assertions.assertEquals(
            1,
            request.getParameters().size(),
            "StackSetManager.deployStackSet() should deploy stack set with specified parameters."
        );
        Assertions.assertEquals(
            StackSetManagerTest.PARAMETER_KEY,
            request.getParameters().get(0).getParameterKey(),
            "StackSetManager.deployStackSet() should deploy stack set with specified parameters."
        );
        Assertions.assertEquals(
            StackSetManagerTest.PARAMETER_VALUE,
            request.getParameters().get(0).getParameterValue(),
            "StackSetManager.deployStackSet() should deploy stack set with specified parameters."
        );
        Assertions.assertEquals(
            1,
            request.getTags().size(),
            "StackSetManager.deployStackSet() should deploy stack set with specified tags."
        );
        Assertions.assertEquals(
            StackSetManagerTest.TAG_NAME,
            request.getTags().get(0).getKey(),
            "StackSetManager.deployStackSet() should deploy stack set with specified tags."
        );
        Assertions.assertEquals(
            StackSetManagerTest.TAG_VALUE,
            request.getTags().get(0).getValue(),
            "StackSetManager.deployStackSet() should deploy stack set with specified tags."
        );
    }

    @Test
    public void deleteStackSet()
    {
        StackSetManager manager = new StackSetManager(this.cloudFormation);

        Mockito
            .when(this.cloudFormation.describeStackSet(this.describeRequest.capture()))
            .thenReturn(
                new DescribeStackSetResult()
                    .withStackSet(
                        new StackSet()
                            .withStackSetId(StackSetManagerTest.STACK_SET_ID_1)
                    )
            );
        Mockito
            .when(this.cloudFormation.deleteStackSet(this.deleteRequest.capture()))
            .thenReturn(new DeleteStackSetResult());

        StackSetRequest input = new StackSetRequest();
        input.setStackSetName(StackSetManagerTest.STACK_SET_NAME);
        manager.deleteStackSet(input, StackSetManagerTest.STACK_SET_ID_1);

        Mockito.verify(this.cloudFormation).deleteStackSet(Mockito.any(DeleteStackSetRequest.class));

        Assertions.assertEquals(
            StackSetManagerTest.STACK_SET_NAME,
            this.deleteRequest.getValue().getStackSetName(),
            "StackSetManager.deleteStackSet() should request deletion of specified stack set."
        );
    }

    @Test
    public void deleteStackSetOutOfSync()
    {
        StackSetManager manager = new StackSetManager(this.cloudFormation);

        Mockito
            .when(this.cloudFormation.describeStackSet(this.describeRequest.capture()))
            .thenReturn(
                new DescribeStackSetResult()
                    .withStackSet(
                        new StackSet()
                            .withStackSetId(StackSetManagerTest.STACK_SET_ID_2)
                    )
            );

        StackSetRequest input = new StackSetRequest();
        input.setStackSetName(StackSetManagerTest.STACK_SET_NAME);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> manager.deleteStackSet(input, StackSetManagerTest.STACK_SET_ID_1)
        );

        Mockito.verify(this.cloudFormation, Mockito.never()).deleteStackSet(Mockito.any(DeleteStackSetRequest.class));
    }

    private static StackSetRequest createStackSetRequest()
    {
        StackSetRequest input = new StackSetRequest();
        input.setStackSetName(StackSetManagerTest.STACK_SET_NAME);
        input.setDescription(StackSetManagerTest.STACK_SET_DESCRIPTION);
        input.setTemplateUrl(StackSetManagerTest.STACK_SET_TEMPLATE_URL);
        input.setCapabilities(StackSetManagerTest.STACK_SET_CAPABILITIES);
        input.setAdministrationRoleArn(StackSetManagerTest.STACK_SET_ADMINISTRATION_ROLE_ARN);
        input.setExecutionRoleName(StackSetManagerTest.STACK_SET_EXECUTION_ROLE_NAME);
        input.setParameters(StackSetManagerTest.STACK_SET_PARAMETERS);
        input.setTags(StackSetManagerTest.STACK_SET_TAGS);
        return input;
    }
}
