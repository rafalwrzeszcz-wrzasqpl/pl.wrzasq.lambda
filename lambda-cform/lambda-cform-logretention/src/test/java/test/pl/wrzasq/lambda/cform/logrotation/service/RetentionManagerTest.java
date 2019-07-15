/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.logrotation.service;

import java.util.ArrayList;
import java.util.Arrays;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.DeleteRetentionPolicyRequest;
import com.amazonaws.services.logs.model.PutRetentionPolicyRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.lambda.cform.logretention.model.RetentionRequest;
import pl.wrzasq.lambda.cform.logretention.service.RetentionManager;

@ExtendWith(MockitoExtension.class)
public class RetentionManagerTest {
    private static final String ID = "test";

    private static final String LOG_GROUP_1 = "/aws/lambda/first";

    private static final String LOG_GROUP_2 = "/aws/lambda/second";

    private static final int DAYS = 7;

    @Mock
    private AWSLogs cloudWatch;

    @Captor
    ArgumentCaptor<DeleteRetentionPolicyRequest> deleteRequest;

    @Captor
    ArgumentCaptor<PutRetentionPolicyRequest> putRequest;

    @Test
    public void provision() {
        RetentionRequest input = this.createRequest();

        RetentionManager manager = new RetentionManager(this.cloudWatch);

        manager.provision(input, null);

        Mockito.verify(this.cloudWatch, Mockito.times(2)).putRetentionPolicy(this.putRequest.capture());

        Assertions.assertEquals(
            2,
            this.putRequest.getAllValues().size(),
            "RetentionManager.provision() should set retention for specified groups."
        );

        PutRetentionPolicyRequest request = this.putRequest.getAllValues().get(0);
        Assertions.assertEquals(
            RetentionManagerTest.LOG_GROUP_1,
            request.getLogGroupName(),
            "RetentionManager.provision() should set retention for specified groups."
        );
        Assertions.assertEquals(
            RetentionManagerTest.DAYS,
            request.getRetentionInDays(),
            "RetentionManager.provision() should set retention for specified groups."
        );

        request = this.putRequest.getAllValues().get(1);
        Assertions.assertEquals(
            RetentionManagerTest.LOG_GROUP_2,
            request.getLogGroupName(),
            "RetentionManager.provision() should set retention for specified groups."
        );
        Assertions.assertEquals(
            RetentionManagerTest.DAYS,
            request.getRetentionInDays(),
            "RetentionManager.provision() should set retention for specified groups."
        );
    }

    @Test
    public void provisionExistingId() {
        RetentionRequest request = new RetentionRequest();
        request.setLogGroups(new ArrayList<>());

        RetentionManager manager = new RetentionManager(this.cloudWatch);

        Assertions.assertEquals(
            RetentionManagerTest.ID,
            manager.provision(request, RetentionManagerTest.ID).getPhysicalResourceId(),
            "RetentionManager.provision() should return ID of existing resource."
        );
    }

    @Test
    public void delete() {
        RetentionRequest input = this.createRequest();

        RetentionManager manager = new RetentionManager(this.cloudWatch);

        manager.delete(input, null);

        Mockito.verify(this.cloudWatch, Mockito.times(2)).deleteRetentionPolicy(this.deleteRequest.capture());

        Assertions.assertEquals(
            2,
            this.deleteRequest.getAllValues().size(),
            "RetentionManager.delete() should set retention for specified groups."
        );

        DeleteRetentionPolicyRequest request = this.deleteRequest.getAllValues().get(0);
        Assertions.assertEquals(
            RetentionManagerTest.LOG_GROUP_1,
            request.getLogGroupName(),
            "RetentionManager.delete() should set retention for specified groups."
        );

        request = this.deleteRequest.getAllValues().get(1);
        Assertions.assertEquals(
            RetentionManagerTest.LOG_GROUP_2,
            request.getLogGroupName(),
            "RetentionManager.delete() should set retention for specified groups."
        );
    }

    private RetentionRequest createRequest() {
        RetentionRequest request = new RetentionRequest();
        request.setRetentionDays(RetentionManagerTest.DAYS);
        request.setLogGroups(
            Arrays.asList(
                RetentionManagerTest.LOG_GROUP_1,
                RetentionManagerTest.LOG_GROUP_2
            )
        );
        return request;
    }
}
