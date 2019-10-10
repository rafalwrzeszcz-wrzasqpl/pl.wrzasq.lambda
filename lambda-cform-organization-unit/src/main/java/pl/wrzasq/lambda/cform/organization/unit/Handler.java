/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.organization.unit;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.organizations.AWSOrganizations;
import com.amazonaws.services.organizations.AWSOrganizationsClientBuilder;
import com.amazonaws.services.organizations.model.OrganizationalUnit;
import com.sunrun.cfnresponse.CfnRequest;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceHandler;
import pl.wrzasq.lambda.cform.organization.unit.model.OrganizationUnitRequest;
import pl.wrzasq.lambda.cform.organization.unit.service.OrganizationUnitManager;

/**
 * CloudFormation request handler.
 *
 * <p>Recommended memory: 256MB.</p>
 */
public class Handler {
    /**
     * CloudFormation response handler.
     */
    private static CustomResourceHandler<OrganizationUnitRequest, OrganizationalUnit> handler;

    static {
        AWSOrganizations organizations = AWSOrganizationsClientBuilder.defaultClient();

        OrganizationUnitManager deploy = new OrganizationUnitManager(organizations);

        Handler.handler = new CustomResourceHandler<>(deploy::sync, deploy::sync, deploy::delete);
    }

    /**
     * Handles invocation.
     *
     * @param request CloudFormation request.
     * @param context AWS Lambda context.
     */
    public void handle(CfnRequest<OrganizationUnitRequest> request, Context context) {
        Handler.handler.handle(request, context);
    }
}
