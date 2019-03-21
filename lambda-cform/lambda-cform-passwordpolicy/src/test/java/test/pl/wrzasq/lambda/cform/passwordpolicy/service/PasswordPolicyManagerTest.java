/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.passwordpolicy.service;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.UpdateAccountPasswordPolicyRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.passwordpolicy.service.PasswordPolicyManager;

@ExtendWith(MockitoExtension.class)
public class PasswordPolicyManagerTest
{
    @Mock
    private AmazonIdentityManagement iam;

    @Captor
    ArgumentCaptor<UpdateAccountPasswordPolicyRequest> createRequest;

    @Test
    public void setPolicy()
    {
        PasswordPolicyManager manager = new PasswordPolicyManager(this.iam);

        UpdateAccountPasswordPolicyRequest input = new UpdateAccountPasswordPolicyRequest();

        CustomResourceResponse<UpdateAccountPasswordPolicyRequest> result = manager.setPolicy(input);

        Mockito.verify(this.iam).updateAccountPasswordPolicy(input);

        Assertions.assertEquals(
            "password-policy",
            result.getPhysicalResourceId(),
            "PasswordPolicyManager.setPolicy() should always set fixed string as a resource ID."
        );
    }

    @Test
    public void delete()
    {
        PasswordPolicyManager manager = new PasswordPolicyManager(this.iam);

        manager.delete(null);

        Mockito.verify(this.iam).deleteAccountPasswordPolicy();
    }
}
