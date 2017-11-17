/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.managedcloudsdk.install;

import com.google.cloud.tools.managedcloudsdk.MessageListener;
import com.google.cloud.tools.managedcloudsdk.gcloud.GcloudCommandExitException;
import com.google.cloud.tools.managedcloudsdk.process.AsyncStreamHandler;
import com.google.cloud.tools.managedcloudsdk.process.CommandExecutor;
import com.google.cloud.tools.managedcloudsdk.process.CommandExecutorFactory;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** Tests for {@link Installer} */
public class InstallerTest {

  @Mock private InstallScriptProvider mockInstallScriptProvider;
  @Mock private CommandExecutorFactory mockCommandExecutorFactory;
  @Mock private CommandExecutor mockCommandExecutor;
  @Mock private MessageListener mockMessageListener;
  @Mock private AsyncStreamHandler<Void> mockStreamHandler;
  @Mock private ListenableFuture<Void> mockResult;

  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  private List<String> fakeCommand = Arrays.asList("scriptexec", "test-install.script");

  @Before
  public void initializeAndConfigureMocks()
      throws IOException, ExecutionException, InterruptedException {
    MockitoAnnotations.initMocks(this);

    Mockito.when(mockInstallScriptProvider.getScriptCommandLine()).thenReturn(fakeCommand);
    Mockito.when(mockCommandExecutorFactory.newCommandExecutor()).thenReturn(mockCommandExecutor);
    Mockito.when(
            mockCommandExecutor.run(
                Mockito.<String>anyList(),
                Mockito.any(AsyncStreamHandler.class),
                Mockito.any(AsyncStreamHandler.class)))
        .thenReturn(0);
    Mockito.when(mockStreamHandler.getResult()).thenReturn(mockResult);
    Mockito.when(mockResult.get()).thenReturn(null);
  }

  @Test
  public void testCall() throws Exception {
    Installer installer =
        new Installer<>(
            tmp.getRoot().toPath(),
            mockInstallScriptProvider,
            false,
            mockMessageListener,
            mockCommandExecutorFactory,
            mockStreamHandler,
            mockStreamHandler);
    installer.install();

    Mockito.verify(mockCommandExecutor).setWorkingDirectory(tmp.getRoot().toPath());
    Mockito.verify(mockCommandExecutor)
        .run(getExpectedCommand(false), mockStreamHandler, mockStreamHandler);
    Mockito.verifyNoMoreInteractions(mockCommandExecutor);
  }

  @Test
  public void testCall_withUsageReporting() throws Exception {
    Installer installer =
        new Installer<>(
            tmp.getRoot().toPath(),
            mockInstallScriptProvider,
            true,
            mockMessageListener,
            mockCommandExecutorFactory,
            mockStreamHandler,
            mockStreamHandler);
    installer.install();

    Mockito.verify(mockCommandExecutor).setWorkingDirectory(tmp.getRoot().toPath());
    Mockito.verify(mockCommandExecutor)
        .run(getExpectedCommand(true), mockStreamHandler, mockStreamHandler);
    Mockito.verifyNoMoreInteractions(mockCommandExecutor);
  }

  @Test
  public void testCall_nonZeroExit() throws Exception {
    Mockito.when(
            mockCommandExecutor.run(
                Mockito.<String>anyList(),
                Mockito.eq(mockStreamHandler),
                Mockito.eq(mockStreamHandler)))
        .thenReturn(10);

    Installer installer =
        new Installer<>(
            tmp.getRoot().toPath(),
            mockInstallScriptProvider,
            false,
            mockMessageListener,
            mockCommandExecutorFactory,
            mockStreamHandler,
            mockStreamHandler);
    try {
      installer.install();
      Assert.fail("GcloudCommandExitException expected but not found.");
    } catch (GcloudCommandExitException ex) {
      Assert.assertEquals("Installer exited with non-zero exit code: 10", ex.getMessage());
    }
    Mockito.verify(mockCommandExecutor).setWorkingDirectory(tmp.getRoot().toPath());
    Mockito.verify(mockCommandExecutor)
        .run(getExpectedCommand(false), mockStreamHandler, mockStreamHandler);
    Mockito.verifyNoMoreInteractions(mockCommandExecutor);
  }

  private List<String> getExpectedCommand(boolean usageReporting) {
    List<String> command = new ArrayList<>(6);
    command.add("scriptexec");
    command.add("test-install.script");
    command.add("--path-update=false");
    command.add("--command-completion=false");
    command.add("--quiet");
    command.add("--usage-reporting=" + usageReporting);
    return command;
  }
}