/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.passwordpolicy.service;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.UpdateAccountPasswordPolicyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;

/**
 * Password policy API implementation.
 */
public class PasswordPolicyManager
{
    /**
     * Static value for physical ID to ensure no deletes will happen in the update.
     */
    private static final String PHYSICAL_RESOURCE_ID = "password-policy";

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(PasswordPolicyManager.class);

    /**
     * AWS IAM API client.
     */
    private AmazonIdentityManagement iam;

    /**
     * Initializes object with given IAM client.
     *
     * @param iam AWS IAM client.
     */
    public PasswordPolicyManager(AmazonIdentityManagement iam)
    {
        this.iam = iam;
    }

    /**
     * Updates password policy for current account.
     *
     * @param input Password policy settings request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Data about published version.
     */
    public CustomResourceResponse<UpdateAccountPasswordPolicyRequest> setPolicy(
        UpdateAccountPasswordPolicyRequest input,
        String physicalResourceId
    )
    {
        this.iam.updateAccountPasswordPolicy(input);

        this.logger.info("Account password policy set.");

        return new CustomResourceResponse<>(
            input,
            // in case of any future changes keep existing ID
            physicalResourceId == null ? PasswordPolicyManager.PHYSICAL_RESOURCE_ID : physicalResourceId
        );
    }

    /**
     * Handles password policy deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<UpdateAccountPasswordPolicyRequest> delete(
        UpdateAccountPasswordPolicyRequest input,
        String physicalResourceId
    )
    {
        this.iam.deleteAccountPasswordPolicy();

        this.logger.info("Account password policy removed.");

        return new CustomResourceResponse<>(null, physicalResourceId);
    }
}
