package com.continuuity.passport.impl;

import com.continuuity.passport.core.exceptions.*;
import com.continuuity.passport.core.meta.Account;
import com.continuuity.passport.core.meta.Component;
import com.continuuity.passport.core.meta.Credentials;
import com.continuuity.passport.core.meta.VPC;
import com.continuuity.passport.core.service.DataManagementService;
import com.continuuity.passport.core.status.Status;
import com.continuuity.passport.dal.AccountDAO;
import com.continuuity.passport.dal.NonceDAO;
import com.continuuity.passport.dal.VpcDAO;
import com.continuuity.passport.dal.db.AccountDBAccess;
import com.continuuity.passport.dal.db.NonceDBAccess;
import com.continuuity.passport.dal.db.VpcDBAccess;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class DataManagementServiceImpl implements DataManagementService {


  private AccountDAO accountDAO = null;

  private VpcDAO vpcDao = null;

  private NonceDAO nonceDAO = null;


  @Inject
  public DataManagementServiceImpl(@Named("passport.config") Map<String,String> config) {
    accountDAO = new AccountDBAccess();

    accountDAO.configure(config);
    vpcDao = new VpcDBAccess();
    //TODO: Remove configure
    vpcDao.configure(config);

    nonceDAO = new NonceDBAccess(config);
  }

  /**
   * Register an {@code Account} in the system
   *
   * @param account Account information
   * @return Instance of {@code Status}
   * @throws RuntimeException
   */
  @Override
  public Account registerAccount(Account account) throws RuntimeException, AccountAlreadyExistsException {
    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");

    }
    try {
      return accountDAO.createAccount(account);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public Status confirmRegistration(Account account, String password) throws RuntimeException {

    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");

    }
    try {
      accountDAO.confirmRegistration(account, password);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
    return null;
  }

  @Override
  public void confirmDownload(int accountId) throws RuntimeException {
    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      accountDAO.confirmDownload(accountId);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * Register a component with the account- Example: register VPC, Register DataSet
   *
   * @param accountId
   * @param credentials
   * @param component
   * @return Instance of {@code Status}
   * @throws RuntimeException
   */
  @Override
  public Status registerComponents(String accountId, Credentials credentials, Component component)
                                                                                    throws RuntimeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Unregister a {@code Component} in the system
   *
   * @param accountId
   * @param credentials
   * @param component
   * @return Instance of {@code Status}
   * @throws RuntimeException
   */
  @Override
  public Status unRegisterComponent(String accountId, Credentials credentials, Component component)
                                                                                    throws RetryException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Delete an {@code Account} in the system
   *
   * @param accountId   account to be deleted
   * @return Instance of {@code Status}
   * @throws RuntimeException
   */
  @Override
  public void deleteAccount(int accountId) throws RuntimeException, AccountNotFoundException {
    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      accountDAO.deleteAccount(accountId);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * @param accountId
   * @param credentials
   * @param component
   * @return Instance of {@code Status}
   * @throws RuntimeException
   */
  @Override
  public Status updateComponent(String accountId, Credentials credentials, Component component)
                                                                          throws RetryException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * GetAccount object
   *
   * @param accountId Id of the account
   * @return Instance of {@code Account}
   */
  @Override
  public Account getAccount(int accountId) throws RuntimeException {

    Account account = null;
    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
     account= accountDAO.getAccount(accountId);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
    return account;
  }

  @Override
  public VPC getVPC(int accountId, int vpcId) {
    if(vpcDao == null) {
      throw new RuntimeException("Could not initialize data access object");
    }
    try {
      return vpcDao.getVPC(accountId,vpcId);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public void deleteVPC(int accountId, int vpcId) throws RuntimeException, VPCNotFoundException {
    if(vpcDao == null) {
      throw new RuntimeException("Could not initialize data access object");
    }
    try {
       vpcDao.removeVPC(accountId, vpcId);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }


  }

  @Override
  public Account getAccount(String emailId) throws RuntimeException {
    Account account = null;
    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      account= accountDAO.getAccount(emailId);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
    return account;
  }

  @Override
  public List<VPC> getVPC(int accountId) {
    List<VPC> vpcs;
    if(vpcDao == null) {
      throw new RuntimeException("Could not initialize data access object");
    }
    try {
       vpcs = vpcDao.getVPC(accountId);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
    return vpcs;
  }

  /**
   * Get VPC List based on the ApiKey
   *
   * @param apiKey apiKey of the account
   * @return List of {@code VPC}
   */
  @Override
  public List<VPC> getVPC(String apiKey) {
    List<VPC> vpcs;
    if(vpcDao == null) {
      throw new RuntimeException("Could not initialize data access object");
    }
    try {
      vpcs = vpcDao.getVPC(apiKey);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
    return vpcs;

  }

  /**
   * Update account with passed Params
   *
   * @param accountId accountId
   * @param params    Map<"keyName", "value">
   */
  @Override
  public void updateAccount(int accountId, Map<String, Object> params) throws RuntimeException {

    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      accountDAO.updateAccount(accountId, params);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

  }

  @Override
  public void changePassword(int accountId, String oldPassword, String newPassword) throws RuntimeException {
    if (accountDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      accountDAO.changePassword(accountId, oldPassword, newPassword);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

  }

  @Override
  public int getActivationNonce(int id) throws RuntimeException {
    if (nonceDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      return nonceDAO.getNonce(id, NonceDAO.NONCE_TYPE.ACTIVATION);
    }catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

  }

  @Override
  public int getSessionNonce(int id) throws RuntimeException {
    if (nonceDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      return nonceDAO.getNonce(id, NonceDAO.NONCE_TYPE.SESSION);
    }catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

  }

  @Override
  public int getActivationId(int nonce) throws RuntimeException {
    if (nonceDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      return nonceDAO.getId(nonce, NonceDAO.NONCE_TYPE.ACTIVATION);
    }catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public int getSessionId(int nonce) throws RuntimeException {
    if (nonceDAO ==null) {
      throw new RuntimeException("Could not init data access Object");
    }
    try {
      return nonceDAO.getId(nonce, NonceDAO.NONCE_TYPE.SESSION);
    }catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public VPC addVPC(int accountId, VPC vpc) throws RuntimeException {
    if(vpcDao == null) {
      throw new RuntimeException("Could not initialize data access object");
    }
    try {
     return vpcDao.addVPC(accountId, vpc);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e.getMessage());
    }
  }


}
