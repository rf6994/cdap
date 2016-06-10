/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.internal.app.runtime;

import co.cask.cdap.app.runtime.ProgramRunner;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Utility class to provide common functionality that shares among different {@link ProgramRunner}.
 */
public final class ProgramRunners {

  /**
   * Impersonates as the given user to start a guava service
   *
   * @param user user to impersonate
   * @param service guava service start start
   */
  public static void startAsUser(String user, final Service service) throws IOException, InterruptedException {
    runAsUser(user, new Callable<ListenableFuture<Service.State>>() {
      @Override
      public ListenableFuture<Service.State> call() throws Exception {
        return service.start();
      }
    });
  }

  /**
   * Impersonates as the given user to perform an action.
   *
   * @param user user to impersonate
   * @param callable action to perform
   */
  public static <T> T runAsUser(String user, final Callable<T> callable) throws IOException, InterruptedException {
    return UserGroupInformation.createRemoteUser(user)
      .doAs(new PrivilegedExceptionAction<T>() {
        @Override
        public T run() throws Exception {
          return callable.call();
        }
      });
  }


  public static void startAsProxyUser(String user, final Service service) throws IOException, InterruptedException {
    runAsProxyUser(user, new Callable<ListenableFuture<Service.State>>() {
      @Override
      public ListenableFuture<Service.State> call() throws Exception {
        return service.start();
      }
    });
  }

  /**
   * Impersonates as the given user to perform an action.
   *
   * @param user user to impersonate
   * @param callable action to perform
   */
  public static <T> T runAsProxyUser(String user, final Callable<T> callable) throws IOException, InterruptedException {
    return UserGroupInformation.createProxyUser(user, UserGroupInformation.getLoginUser())
      .doAs(new PrivilegedExceptionAction<T>() {
        @Override
        public T run() throws Exception {
          return callable.call();
        }
      });
  }


  public static <T> T runAsUGI(UserGroupInformation ugi,
                               final Callable<T> callable) throws IOException, InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<T>() {
      @Override
      public T run() throws Exception {
        return callable.call();
      }
    });
  }

  /**
   * Updates the given arguments to always have the logical start time set.
   *
   * @param arguments the runtime arguments
   * @return the logical start time
   */
  public static long updateLogicalStartTime(Map<String, String> arguments) {
    String value = arguments.get(ProgramOptionConstants.LOGICAL_START_TIME);
    try {
      // value is only empty/null in in some unit tests
      long logicalStartTime = Strings.isNullOrEmpty(value) ? System.currentTimeMillis() : Long.parseLong(value);
      arguments.put(ProgramOptionConstants.LOGICAL_START_TIME, Long.toString(logicalStartTime));
      return logicalStartTime;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format(
        "%s is set to an invalid value %s. Please ensure it is a timestamp in milliseconds.",
        ProgramOptionConstants.LOGICAL_START_TIME, value));
    }
  }

  private ProgramRunners() {
    // no-op
  }
}
