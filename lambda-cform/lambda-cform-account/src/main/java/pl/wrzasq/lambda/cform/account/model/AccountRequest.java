/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.account.model;

import lombok.Data;

/**
 * Account CloudFormation request.
 */
@Data
public class AccountRequest
{
    /**
     * Existing account ID to invite into organization.
     */
    private String accountId;

    /**
     * E-mail address for which to create an account.
     */
    private String email;

    /**
     * Name to label the account.
     */
    private String accountName;

    /**
     * Role name for the administration purposes.
     */
    private String administratorRoleName;

    /**
     * Organizational Unit ID in which to place an account.
     */
    private String ouId;
}
