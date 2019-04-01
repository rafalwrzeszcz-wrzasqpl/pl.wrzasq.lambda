/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.cform.account.service;

import com.amazonaws.services.organizations.AWSOrganizations;
import com.amazonaws.services.organizations.model.Account;
import com.amazonaws.services.organizations.model.AccountNotFoundException;
import com.amazonaws.services.organizations.model.CreateAccountRequest;
import com.amazonaws.services.organizations.model.CreateAccountResult;
import com.amazonaws.services.organizations.model.CreateAccountState;
import com.amazonaws.services.organizations.model.CreateAccountStatus;
import com.amazonaws.services.organizations.model.DescribeAccountRequest;
import com.amazonaws.services.organizations.model.DescribeAccountResult;
import com.amazonaws.services.organizations.model.DescribeCreateAccountStatusRequest;
import com.amazonaws.services.organizations.model.DescribeCreateAccountStatusResult;
import com.amazonaws.services.organizations.model.DescribeHandshakeRequest;
import com.amazonaws.services.organizations.model.DescribeHandshakeResult;
import com.amazonaws.services.organizations.model.Handshake;
import com.amazonaws.services.organizations.model.HandshakeParty;
import com.amazonaws.services.organizations.model.HandshakePartyType;
import com.amazonaws.services.organizations.model.HandshakeState;
import com.amazonaws.services.organizations.model.InviteAccountToOrganizationRequest;
import com.amazonaws.services.organizations.model.InviteAccountToOrganizationResult;
import com.amazonaws.services.organizations.model.ListParentsRequest;
import com.amazonaws.services.organizations.model.ListParentsResult;
import com.amazonaws.services.organizations.model.MoveAccountRequest;
import com.amazonaws.services.organizations.model.Parent;
import com.amazonaws.services.organizations.model.RemoveAccountFromOrganizationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrzasq.commons.aws.cloudformation.CustomResourceResponse;
import pl.wrzasq.lambda.cform.account.model.AccountRequest;
import pl.wrzasq.lambda.cform.account.service.AccountManager;

@ExtendWith(MockitoExtension.class)
public class AccountManagerTest {
    private static final String STATUS_ID = "fooactioninprogress";

    private static final String PHYSICAL_ID_1 = "1234567890";

    private static final String PHYSICAL_ID_2 = "0123456789";

    private static final String EMAIL_1 = "test@localhost";

    private static final String EMAIL_2 = "another@localhost";

    private static final String ACCOUNT_NAME_1 = "Test Account";

    private static final String ACCOUNT_NAME_2 = "Another Account";

    private static final String ADMINISTRATOR_ROLE_NAME = "OrganizationAdmin";

    private static final String OU_ID_1 = "r-root";

    private static final String OU_ID_2 = "ou-leaf";

    @Mock
    private AWSOrganizations organizations;

    @Captor
    ArgumentCaptor<CreateAccountRequest> createRequest;

    @Captor
    ArgumentCaptor<InviteAccountToOrganizationRequest> inviteRequest;

    @Captor
    ArgumentCaptor<DescribeCreateAccountStatusRequest> describeCreateRequest;

    @Captor
    ArgumentCaptor<DescribeHandshakeRequest> describeHandshakeRequest;

    @Captor
    ArgumentCaptor<ListParentsRequest> listParents;

    @Captor
    ArgumentCaptor<MoveAccountRequest> moveRequest;

    @Captor
    ArgumentCaptor<DescribeAccountRequest> describeRequest;

    @Captor
    ArgumentCaptor<RemoveAccountFromOrganizationRequest> removeRequest;

    @Test
    public void provision() {
        Account account = new Account();

        Mockito
            .when(this.organizations.createAccount(this.createRequest.capture()))
            .thenReturn(
                new CreateAccountResult()
                    .withCreateAccountStatus(
                        new CreateAccountStatus()
                            .withId(AccountManagerTest.STATUS_ID)
                            .withState(CreateAccountState.IN_PROGRESS)
                    )
            );
        Mockito
            .when(this.organizations.describeCreateAccountStatus(this.describeCreateRequest.capture()))
            .thenReturn(
                new DescribeCreateAccountStatusResult()
                    .withCreateAccountStatus(
                        new CreateAccountStatus()
                            .withState(CreateAccountState.SUCCEEDED)
                            .withAccountId(AccountManagerTest.PHYSICAL_ID_1)
                    )
            );
        Mockito
            .when(this.organizations.listParents(this.listParents.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(AccountManagerTest.OU_ID_1)
                    )
            );
        Mockito
            .when(this.organizations.describeAccount(this.describeRequest.capture()))
            .thenReturn(
                new DescribeAccountResult()
                    .withAccount(account)
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setEmail(AccountManagerTest.EMAIL_1);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_1);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);
        input.setOuId(AccountManagerTest.OU_ID_2);

        CustomResourceResponse<Account> result = manager.provision(input, null);

        Mockito
            .verify(this.organizations)
            .moveAccount(this.moveRequest.capture());

        Assertions.assertEquals(
            AccountManagerTest.EMAIL_1,
            this.createRequest.getValue().getEmail(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ACCOUNT_NAME_1,
            this.createRequest.getValue().getAccountName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ADMINISTRATOR_ROLE_NAME,
            this.createRequest.getValue().getRoleName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.STATUS_ID,
            this.describeCreateRequest.getValue().getCreateAccountRequestId(),
            "AccountManager.provision() should keep checking status of account creation status."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.listParents.getValue().getChildId(),
            "AccountManager.provision() should request parents of created account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.moveRequest.getValue().getAccountId(),
            "AccountManager.provision() should request relocation of created account."
        );
        Assertions.assertEquals(
            AccountManagerTest.OU_ID_1,
            this.moveRequest.getValue().getSourceParentId(),
            "AccountManager.provision() should request relocation of created account."
        );
        Assertions.assertEquals(
            AccountManagerTest.OU_ID_2,
            this.moveRequest.getValue().getDestinationParentId(),
            "AccountManager.provision() should request relocation of created account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.describeRequest.getValue().getAccountId(),
            "AccountManager.provision() should request details of created account."
        );

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            result.getPhysicalResourceId(),
            "AccountManager.provision() should return account ID as resource physical ID."
        );
        Assertions.assertSame(
            account,
            result.getData(),
            "AccountManager.provision() should return account data as resource properties."
        );
    }

    @Test
    public void provisionFails() {
        Mockito
            .when(this.organizations.createAccount(Mockito.any(CreateAccountRequest.class)))
            .thenReturn(
                new CreateAccountResult()
                    .withCreateAccountStatus(
                        new CreateAccountStatus()
                            .withState(CreateAccountState.FAILED)
                    )
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setEmail(AccountManagerTest.EMAIL_1);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_1);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> manager.provision(input, null),
            "AccountManager.provision() should throw exception if account creation fails."
        );
    }

    @Test
    public void provisionInvite() {
        Account account = new Account();

        Mockito
            .when(this.organizations.inviteAccountToOrganization(this.inviteRequest.capture()))
            .thenReturn(
                new InviteAccountToOrganizationResult()
                    .withHandshake(
                        new Handshake()
                            .withId(AccountManagerTest.STATUS_ID)
                            .withState(HandshakeState.REQUESTED)
                    )
            );
        Mockito
            .when(this.organizations.describeHandshake(this.describeHandshakeRequest.capture()))
            .thenReturn(
                new DescribeHandshakeResult()
                    .withHandshake(
                        new Handshake()
                            .withState(HandshakeState.ACCEPTED)
                            .withParties(
                                new HandshakeParty()
                                    .withType(HandshakePartyType.ACCOUNT)
                                    .withId(AccountManagerTest.PHYSICAL_ID_1)
                            )
                    )
            );
        Mockito
            .when(this.organizations.listParents(this.listParents.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(AccountManagerTest.OU_ID_1)
                    )
            );
        Mockito
            .when(this.organizations.describeAccount(this.describeRequest.capture()))
            .thenReturn(
                new DescribeAccountResult()
                    .withAccount(account)
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setAccountId(AccountManagerTest.PHYSICAL_ID_1);
        input.setEmail(AccountManagerTest.EMAIL_1);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_1);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);
        input.setOuId(AccountManagerTest.OU_ID_1);

        CustomResourceResponse<Account> result = manager.provision(input, null);

        Mockito
            .verify(this.organizations, Mockito.never())
            .moveAccount(Mockito.any(MoveAccountRequest.class));

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.inviteRequest.getValue().getTarget().getId(),
            "AccountManager.provision() should invite specified acocunt."
        );
        Assertions.assertEquals(
            HandshakePartyType.ACCOUNT.name(),
            this.inviteRequest.getValue().getTarget().getType(),
            "AccountManager.provision() should invite specified account."
        );
        Assertions.assertEquals(
            AccountManagerTest.STATUS_ID,
            this.describeHandshakeRequest.getValue().getHandshakeId(),
            "AccountManager.provision() should keep checking status of account invitation status."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.listParents.getValue().getChildId(),
            "AccountManager.provision() should request parents of invited account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.describeRequest.getValue().getAccountId(),
            "AccountManager.provision() should request details of invited account."
        );

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            result.getPhysicalResourceId(),
            "AccountManager.provision() should return account ID as resource physical ID."
        );
        Assertions.assertSame(
            account,
            result.getData(),
            "AccountManager.provision() should return account data as resource properties."
        );
    }

    @Test
    public void provisionInviteFails() {
        Mockito
            .when(
                this.organizations.inviteAccountToOrganization(Mockito.any(InviteAccountToOrganizationRequest.class))
            )
            .thenReturn(
                new InviteAccountToOrganizationResult()
                    .withHandshake(
                        new Handshake()
                            .withId(AccountManagerTest.STATUS_ID)
                            .withState(HandshakeState.EXPIRED)
                    )
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setAccountId(AccountManagerTest.PHYSICAL_ID_1);
        input.setEmail(AccountManagerTest.EMAIL_1);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_1);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);
        input.setOuId(AccountManagerTest.OU_ID_1);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> manager.provision(input, null),
            "AccountManager.provision() should throw exception if account invitation fails."
        );
    }

    @Test
    public void provisionNotExistingAccount() {
        Account account = new Account();

        Mockito
            .when(this.organizations.describeAccount(this.describeRequest.capture()))
            .thenThrow(AccountNotFoundException.class)
            .thenReturn(
                new DescribeAccountResult()
                    .withAccount(account)
            );
        Mockito
            .when(this.organizations.createAccount(this.createRequest.capture()))
            .thenReturn(
                new CreateAccountResult()
                    .withCreateAccountStatus(
                        new CreateAccountStatus()
                            .withId(AccountManagerTest.STATUS_ID)
                            .withState(CreateAccountState.SUCCEEDED)
                            .withAccountId(AccountManagerTest.PHYSICAL_ID_1)
                    )
            );
        Mockito
            .when(this.organizations.listParents(this.listParents.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(AccountManagerTest.OU_ID_1)
                    )
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setEmail(AccountManagerTest.EMAIL_1);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_1);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);
        input.setOuId(AccountManagerTest.OU_ID_1);

        CustomResourceResponse<Account> result = manager.provision(input, AccountManagerTest.PHYSICAL_ID_2);

        Mockito
            .verify(this.organizations, Mockito.never())
            .moveAccount(Mockito.any(MoveAccountRequest.class));

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_2,
            this.describeRequest.getAllValues().get(0).getAccountId(),
            "AccountManager.provision() should first try to fetch details of existing account."
        );
        Assertions.assertEquals(
            AccountManagerTest.EMAIL_1,
            this.createRequest.getValue().getEmail(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ACCOUNT_NAME_1,
            this.createRequest.getValue().getAccountName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ADMINISTRATOR_ROLE_NAME,
            this.createRequest.getValue().getRoleName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.listParents.getValue().getChildId(),
            "AccountManager.provision() should request parents of created account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.describeRequest.getAllValues().get(1).getAccountId(),
            "AccountManager.provision() should request details of created account."
        );

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            result.getPhysicalResourceId(),
            "AccountManager.provision() should return account ID as resource physical ID."
        );
        Assertions.assertSame(
            account,
            result.getData(),
            "AccountManager.provision() should return account data as resource properties."
        );
    }

    @Test
    public void provisionChangedName() {
        Account account = new Account();
        account.setEmail(AccountManagerTest.EMAIL_1);
        account.setName(AccountManagerTest.ACCOUNT_NAME_1);
        account.setId(AccountManagerTest.PHYSICAL_ID_1);

        Mockito
            .when(this.organizations.describeAccount(this.describeRequest.capture()))
            .thenReturn(
                new DescribeAccountResult()
                    .withAccount(account)
            );
        Mockito
            .when(this.organizations.createAccount(this.createRequest.capture()))
            .thenReturn(
                new CreateAccountResult()
                    .withCreateAccountStatus(
                        new CreateAccountStatus()
                            .withId(AccountManagerTest.STATUS_ID)
                            .withState(CreateAccountState.SUCCEEDED)
                            .withAccountId(AccountManagerTest.PHYSICAL_ID_2)
                    )
            );
        Mockito
            .when(this.organizations.listParents(this.listParents.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(AccountManagerTest.OU_ID_1)
                    )
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setEmail(AccountManagerTest.EMAIL_1);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_2);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);
        input.setOuId(AccountManagerTest.OU_ID_1);

        CustomResourceResponse<Account> result = manager.provision(input, AccountManagerTest.PHYSICAL_ID_1);

        Mockito
            .verify(this.organizations, Mockito.never())
            .inviteAccountToOrganization(Mockito.any(InviteAccountToOrganizationRequest.class));
        Mockito
            .verify(this.organizations, Mockito.never())
            .moveAccount(Mockito.any(MoveAccountRequest.class));

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.describeRequest.getAllValues().get(0).getAccountId(),
            "AccountManager.provision() should fetch details of existing account."
        );
        Assertions.assertEquals(
            AccountManagerTest.EMAIL_1,
            this.createRequest.getValue().getEmail(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ACCOUNT_NAME_2,
            this.createRequest.getValue().getAccountName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ADMINISTRATOR_ROLE_NAME,
            this.createRequest.getValue().getRoleName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_2,
            this.listParents.getValue().getChildId(),
            "AccountManager.provision() should request parents of new account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_2,
            this.describeRequest.getAllValues().get(1).getAccountId(),
            "AccountManager.provision() should request details of new account."
        );

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_2,
            result.getPhysicalResourceId(),
            "AccountManager.provision() should return account ID as resource physical ID."
        );
        Assertions.assertSame(
            account,
            result.getData(),
            "AccountManager.provision() should return account data as resource properties."
        );
    }

    @Test
    public void provisionChangedEmail() {
        Account account = new Account();
        account.setEmail(AccountManagerTest.EMAIL_1);
        account.setName(AccountManagerTest.ACCOUNT_NAME_1);
        account.setId(AccountManagerTest.PHYSICAL_ID_1);

        Mockito
            .when(this.organizations.describeAccount(this.describeRequest.capture()))
            .thenReturn(
                new DescribeAccountResult()
                    .withAccount(account)
            );
        Mockito
            .when(this.organizations.createAccount(this.createRequest.capture()))
            .thenReturn(
                new CreateAccountResult()
                    .withCreateAccountStatus(
                        new CreateAccountStatus()
                            .withId(AccountManagerTest.STATUS_ID)
                            .withState(CreateAccountState.SUCCEEDED)
                            .withAccountId(AccountManagerTest.PHYSICAL_ID_2)
                    )
            );
        Mockito
            .when(this.organizations.listParents(this.listParents.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(AccountManagerTest.OU_ID_1)
                    )
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setEmail(AccountManagerTest.EMAIL_2);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_1);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);
        input.setOuId(AccountManagerTest.OU_ID_1);

        CustomResourceResponse<Account> result = manager.provision(input, AccountManagerTest.PHYSICAL_ID_1);

        Mockito
            .verify(this.organizations, Mockito.never())
            .inviteAccountToOrganization(Mockito.any(InviteAccountToOrganizationRequest.class));
        Mockito
            .verify(this.organizations, Mockito.never())
            .moveAccount(Mockito.any(MoveAccountRequest.class));

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.describeRequest.getAllValues().get(0).getAccountId(),
            "AccountManager.provision() should fetch details of existing account."
        );
        Assertions.assertEquals(
            AccountManagerTest.EMAIL_2,
            this.createRequest.getValue().getEmail(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ACCOUNT_NAME_1,
            this.createRequest.getValue().getAccountName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.ADMINISTRATOR_ROLE_NAME,
            this.createRequest.getValue().getRoleName(),
            "AccountManager.provision() should create account with specified setup."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_2,
            this.listParents.getValue().getChildId(),
            "AccountManager.provision() should request parents of new account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_2,
            this.describeRequest.getAllValues().get(1).getAccountId(),
            "AccountManager.provision() should request details of new account."
        );

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_2,
            result.getPhysicalResourceId(),
            "AccountManager.provision() should return account ID as resource physical ID."
        );
        Assertions.assertSame(
            account,
            result.getData(),
            "AccountManager.provision() should return account data as resource properties."
        );
    }

    @Test
    public void provisionMove() {
        Account account = new Account();
        account.setEmail(AccountManagerTest.EMAIL_1);
        account.setName(AccountManagerTest.ACCOUNT_NAME_1);
        account.setId(AccountManagerTest.PHYSICAL_ID_1);

        Mockito
            .when(this.organizations.describeAccount(this.describeRequest.capture()))
            .thenReturn(
                new DescribeAccountResult()
                    .withAccount(account)
            );
        Mockito
            .when(this.organizations.listParents(this.listParents.capture()))
            .thenReturn(
                new ListParentsResult()
                    .withParents(
                        new Parent()
                            .withId(AccountManagerTest.OU_ID_1)
                    )
            );

        AccountManager manager = this.createAccountManager();

        AccountRequest input = new AccountRequest();
        input.setEmail(AccountManagerTest.EMAIL_1);
        input.setAccountName(AccountManagerTest.ACCOUNT_NAME_1);
        input.setAdministratorRoleName(AccountManagerTest.ADMINISTRATOR_ROLE_NAME);
        input.setOuId(AccountManagerTest.OU_ID_2);

        CustomResourceResponse<Account> result = manager.provision(input, AccountManagerTest.PHYSICAL_ID_1);

        Mockito
            .verify(this.organizations)
            .moveAccount(this.moveRequest.capture());
        Mockito
            .verify(this.organizations, Mockito.never())
            .createAccount(Mockito.any(CreateAccountRequest.class));
        Mockito
            .verify(this.organizations, Mockito.never())
            .inviteAccountToOrganization(Mockito.any(InviteAccountToOrganizationRequest.class));

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.describeRequest.getAllValues().get(0).getAccountId(),
            "AccountManager.provision() should fetch details of existing account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.listParents.getValue().getChildId(),
            "AccountManager.provision() should request parents of existing account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.moveRequest.getValue().getAccountId(),
            "AccountManager.provision() should request relocation of an account."
        );
        Assertions.assertEquals(
            AccountManagerTest.OU_ID_1,
            this.moveRequest.getValue().getSourceParentId(),
            "AccountManager.provision() should request relocation of an account."
        );
        Assertions.assertEquals(
            AccountManagerTest.OU_ID_2,
            this.moveRequest.getValue().getDestinationParentId(),
            "AccountManager.provision() should request relocation of an account."
        );
        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.describeRequest.getAllValues().get(1).getAccountId(),
            "AccountManager.provision() should request details of exiting account."
        );

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            result.getPhysicalResourceId(),
            "AccountManager.provision() should return account ID as resource physical ID."
        );
        Assertions.assertSame(
            account,
            result.getData(),
            "AccountManager.provision() should return account data as resource properties."
        );
    }

    @Test
    public void delete() {
        AccountManager manager = this.createAccountManager();

        manager.delete(null, AccountManagerTest.PHYSICAL_ID_1);

        Mockito.verify(this.organizations).removeAccountFromOrganization(this.removeRequest.capture());

        Assertions.assertEquals(
            AccountManagerTest.PHYSICAL_ID_1,
            this.removeRequest.getValue().getAccountId(),
            "AccountManager.delete() should remove account with specified ID from organization."
        );
    }

    private AccountManager createAccountManager() {
        AccountManager manager = new AccountManager(this.organizations);
        manager.setSleepInterval(1);
        return manager;
    }
}
