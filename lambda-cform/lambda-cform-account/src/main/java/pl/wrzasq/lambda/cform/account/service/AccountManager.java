/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.lambda.cform.account.service;

import com.amazonaws.services.organizations.AWSOrganizations;
import com.amazonaws.services.organizations.model.Account;
import com.amazonaws.services.organizations.model.AccountNotFoundException;
import com.amazonaws.services.organizations.model.CreateAccountRequest;
import com.amazonaws.services.organizations.model.CreateAccountState;
import com.amazonaws.services.organizations.model.CreateAccountStatus;
import com.amazonaws.services.organizations.model.DescribeAccountRequest;
import com.amazonaws.services.organizations.model.DescribeCreateAccountStatusRequest;
import com.amazonaws.services.organizations.model.DescribeHandshakeRequest;
import com.amazonaws.services.organizations.model.Handshake;
import com.amazonaws.services.organizations.model.HandshakeParty;
import com.amazonaws.services.organizations.model.HandshakePartyType;
import com.amazonaws.services.organizations.model.HandshakeState;
import com.amazonaws.services.organizations.model.InviteAccountToOrganizationRequest;
import com.amazonaws.services.organizations.model.ListParentsRequest;
import com.amazonaws.services.organizations.model.MoveAccountRequest;
import com.amazonaws.services.organizations.model.Parent;
import com.amazonaws.services.organizations.model.RemoveAccountFromOrganizationRequest;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.account.model.AccountRequest;

/**
 * Organizations API implementation.
 */
public class AccountManager {
    /**
     * Default sleep interval (1 minute).
     */
    private static final long DEFAULT_SLEEP_INTERVAL = 60000;

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(AccountManager.class);

    /**
     * AWS Organizations API client.
     */
    private AWSOrganizations organizations;

    /**
     * Sleep interval for status change checks.
     */
    @Setter
    private long sleepInterval = AccountManager.DEFAULT_SLEEP_INTERVAL;

    /**
     * Initializes object with given Organizations client.
     *
     * @param organizations AWS Organizations client.
     */
    public AccountManager(AWSOrganizations organizations) {
        this.organizations = organizations;
    }

    /**
     * Handles account creation.
     *
     * @param input Resource creation request.
     * @param physicalResourceId Physical ID of existing resource (in this case always null).
     * @return Data about published version.
     */
    public CustomResourceResponse<Account> provision(AccountRequest input, String physicalResourceId) {
        if (physicalResourceId != null) {
            physicalResourceId = resolveExistingAccount(physicalResourceId, input);
        }

        // new account needed
        if (physicalResourceId == null) {
            physicalResourceId = this.initializeAccount(input);
        }

        // check current location in organization structure
        Parent parent = this.organizations.listParents(
            new ListParentsRequest()
                .withChildId(physicalResourceId)
        )
            .getParents()
            .get(0);
        if (!parent.getId().equals(input.getOuId())) {
            this.logger.info("Moving account {} from {} to {}.", physicalResourceId, parent.getId(), input.getOuId());

            this.organizations.moveAccount(
                new MoveAccountRequest()
                    .withAccountId(physicalResourceId)
                    .withSourceParentId(parent.getId())
                    .withDestinationParentId(input.getOuId())
            );
        }

        Account account = this.organizations.describeAccount(
            new DescribeAccountRequest()
                .withAccountId(physicalResourceId)
        )
            .getAccount();

        return new CustomResourceResponse<>(account, physicalResourceId);
    }

    /**
     * Handles organization deletion.
     *
     * @param input Resource delete request.
     * @param physicalResourceId Physical ID of existing resource (if present).
     * @return Empty response.
     */
    public CustomResourceResponse<Account> delete(AccountRequest input, String physicalResourceId) {
        this.organizations.removeAccountFromOrganization(
            new RemoveAccountFromOrganizationRequest()
                .withAccountId(physicalResourceId)
        );

        this.logger.info(
            "Removed account {} from organization - keep in mind that account still exists and needs to be terminated "
                + "manually!",
            physicalResourceId
        );

        return new CustomResourceResponse<>(null, physicalResourceId);
    }

    /**
     * Manages new account for organization.
     *
     * @param input Account specification.
     * @return Account ID.
     */
    private String initializeAccount(AccountRequest input) {
        return input.getAccountId() == null
            ? this.createAccount(input.getEmail(), input.getAccountName(), input.getAdministratorRoleName())
            : this.inviteAccount(input.getAccountId());
    }

    /**
     * Creates plain, new account.
     *
     * @param email E-mail address for root access.
     * @param name Account label.
     * @param roleName Administration role name.
     * @return Account ID.
     */
    private String createAccount(String email, String name, String roleName) {
        CreateAccountStatus status = this.organizations.createAccount(
            new CreateAccountRequest()
                .withEmail(email)
                .withAccountName(name)
                .withRoleName(roleName)
        )
            .getCreateAccountStatus();

        this.logger.info("New account creation initialized for {} ({}).", email, name);

        // wait until account creation process is finished
        while (CreateAccountState.fromValue(status.getState()) == CreateAccountState.IN_PROGRESS) {
            this.logger.info("Account creation {} in progress…", name);
            this.sleep();

            status = this.organizations.describeCreateAccountStatus(
                new DescribeCreateAccountStatusRequest()
                    .withCreateAccountRequestId(status.getId())
            )
                .getCreateAccountStatus();
        }

        this.logger.info("Account creation status: {}.", status.getState());

        if (CreateAccountState.fromValue(status.getState()) == CreateAccountState.SUCCEEDED) {
            return status.getAccountId();
        } else {
            throw new IllegalStateException(
                String.format(
                    "Failed to create account for %s (%s) - reason: %s.",
                    email,
                    name,
                    status.getFailureReason()
                )
            );
        }
    }

    /**
     * Invites existing account to organization.
     *
     * @param accountId Existing account ID.
     * @return Account ID.
     */
    private String inviteAccount(String accountId) {
        Handshake handshake = this.organizations.inviteAccountToOrganization(
            new InviteAccountToOrganizationRequest()
                .withTarget(
                    new HandshakeParty()
                        .withType(HandshakePartyType.ACCOUNT)
                        .withId(accountId)
                )
        )
            .getHandshake();

        this.logger.info("Invited account {} to the organization", accountId);

        while (HandshakeState.fromValue(handshake.getState()) == HandshakeState.REQUESTED) {
            this.logger.info("Account {} handshake in progress…", accountId);
            this.sleep();

            handshake = this.organizations.describeHandshake(
                new DescribeHandshakeRequest()
                    .withHandshakeId(handshake.getId())
            )
                .getHandshake();
        }

        if (HandshakeState.fromValue(handshake.getState()) == HandshakeState.ACCEPTED) {
            return accountId;
        } else {
            throw new IllegalStateException(
                String.format(
                    "Failed to invite account %s - status: %s.",
                    accountId,
                    handshake.getState()
                )
            );
        }
    }

    /**
     * Resolves ID of existing resource.
     *
     * @param accountId Currently provisioned ID.
     * @param input Desired account specification.
     * @return Resolved account ID.
     */
    private String resolveExistingAccount(String accountId, AccountRequest input) {
        try {
            Account account = this.organizations.describeAccount(
                new DescribeAccountRequest()
                    .withAccountId(accountId)
            )
                .getAccount();

            if (input.getEmail().equals(account.getEmail()) && input.getAccountName().equals(account.getName())) {
                return account.getId();
            } else {
                this.logger.warn("Account {} core data changed - will re-create account!", account.getArn());

                return null;
            }
        } catch (AccountNotFoundException error) {
            this.logger.warn("Account {} not found - resolving to new account.", accountId);

            return null;
        }
    }

    /**
     * Performs a wait.
     */
    private void sleep() {
        try {
            Thread.sleep(this.sleepInterval);
        } catch (InterruptedException error) {
            this.logger.error("Wait interval interrupted.", error);
        }
    }
}
