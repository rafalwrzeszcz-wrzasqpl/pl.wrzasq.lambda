/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.passwordpolicy;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.UpdateAccountPasswordPolicyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.passwordpolicy.service.PasswordPolicyManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler
{
    /**
     * CloudFormation response handler.
     */
    private static
        CustomResourceHandler<UpdateAccountPasswordPolicyRequest, UpdateAccountPasswordPolicyRequest> handler;

    static {
        AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

        PasswordPolicyManager deploy = new PasswordPolicyManager(iam);

        Handler.handler = new CustomResourceHandler<>(deploy::setPolicy, deploy::setPolicy, deploy::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<UpdateAccountPasswordPolicyRequest> request, Context context)
    {
        Handler.handler.handle(request, context);
    }
}
