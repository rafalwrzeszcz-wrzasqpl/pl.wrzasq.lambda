/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.account;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.organizations.AWSOrganizationsClientBuilder;
import com.amazonaws.services.organizations.model.Account;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.account.model.AccountRequest;
import pl.wrzasq.lambda.cform.account.service.AccountManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<AccountRequest, Account> handler;

    static {
        var organizations = AWSOrganizationsClientBuilder.defaultClient();

        var deploy = new AccountManager(organizations);

        Handler.handler = new CustomResourceHandler<>(deploy::provision, deploy::provision, deploy::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<AccountRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
