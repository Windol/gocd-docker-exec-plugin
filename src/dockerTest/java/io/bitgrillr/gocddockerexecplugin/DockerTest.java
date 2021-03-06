/**
 * Copyright 2018 Christopher Arnold <cma.arnold@gmail.com> and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bitgrillr.gocddockerexecplugin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import io.bitgrillr.gocddockerexecplugin.docker.DockerUtils;
import io.bitgrillr.gocddockerexecplugin.utils.UnitTestUtils;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JobConsoleLogger.class)
@PowerMockIgnore({ "javax.net.ssl.*" })
public class DockerTest {

  @Test
  public void exec() throws Exception {
    final List<String> console = UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.pullImage("busybox:latest");
    final String containerId = DockerUtils.createContainer("busybox:latest", System.getProperty("user.dir"),
        Collections.emptyMap());
    final int exitCode = DockerUtils.execCommand(containerId, "root", "ls");
    DockerUtils.removeContainer(containerId);

    assertThat(console, either(hasItem("Status: Image is up to date for busybox:latest"))
        .or(hasItem("Status: Downloaded newer image for busybox:latest")));
    assertThat(console, hasItem("Creating container from image 'busybox:latest'"));
    assertThat(console, hasItem("build.gradle"));
    assertEquals("Incorrect exit code", 0, exitCode);
    assertThat(console, hasItem(CoreMatchers.startsWith("Removing container")));
  }

  @Test
  public void defaultUser() throws Exception {
    final List<String> console = UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.pullImage("gocd/gocd-agent-ubuntu-16.04:v17.3.0");
    final String containerId = DockerUtils.createContainer("gocd/gocd-agent-ubuntu-16.04:v17.3.0",
        System.getProperty("user.dir"), Collections.emptyMap());
    final int exitCode = DockerUtils.execCommand(containerId, null, "sh", "-c", "echo \"UID = $(id -u)\"");
    DockerUtils.removeContainer(containerId);

    assertEquals("Expected success", 0, exitCode);
    assertThat("Wrong UID", console, hasItem("UID = 0"));
  }

  @Test
  public void setUser() throws Exception {
    final List<String> console = UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.pullImage("gocd/gocd-agent-ubuntu-16.04:v17.3.0");
    final String containerId = DockerUtils.createContainer("gocd/gocd-agent-ubuntu-16.04:v17.3.0",
        System.getProperty("user.dir"), Collections.emptyMap());
    final int exitCode = DockerUtils.execCommand(containerId, "go", "sh", "-c", "echo \"UID = $(id -u)\"");
    DockerUtils.removeContainer(containerId);

    assertEquals("Expected success", 0, exitCode);
    assertThat("Wrong UID", console, hasItem("UID = 1000"));
  }

  @Test
  public void getContainerUid() throws Exception {
    UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.pullImage("gocd/gocd-agent-ubuntu-16.04:v17.3.0");
    final String containerId = DockerUtils.createContainer("gocd/gocd-agent-ubuntu-16.04:v17.3.0",
        System.getProperty("user.dir"), Collections.emptyMap());
    final String uid = DockerUtils.getContainerUid(containerId);
    DockerUtils.removeContainer(containerId);

    assertEquals("Wrong UID", "0:0", uid);
  }

  @Test(expected = ImageNotFoundException.class)
  public void badPull() throws Exception {
    UnitTestUtils.mockJobConsoleLogger();
    // please, no-one create this image on the hub
    DockerUtils.pullImage("idont:exist");
  }

  @Test(expected = ImageNotFoundException.class)
  public void badCreate() throws Exception {
    UnitTestUtils.mockJobConsoleLogger();
    // again - please, no-one create this image on the hub
    DockerUtils.createContainer("idont:exist", System.getProperty("user.dir"), Collections.emptyMap());
  }

  @Test
  public void badCommand() throws Exception {
    final List<String> console = UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.pullImage("busybox:latest");
    final String containerId = DockerUtils.createContainer("busybox:latest", System.getProperty("user.dir"),
        Collections.emptyMap());
    final int exitCode = DockerUtils.execCommand(containerId, null, "doesntexist");
    DockerUtils.removeContainer(containerId);

    assertNotEquals("Wrong exit code", 0, exitCode);
    assertThat(console, hasItem(containsString("executable file not found")));
  }

  @Test
  public void failedCommand() throws Exception {
    UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.pullImage("busybox:latest");
    final String containerId = DockerUtils.createContainer("busybox:latest", System.getProperty("user.dir"),
        Collections.emptyMap());
    final int exitCode = DockerUtils.execCommand(containerId, null, "false");
    DockerUtils.removeContainer(containerId);

    assertEquals("Wrong exit code", 1, exitCode);
  }

  @Test(expected = ContainerNotFoundException.class)
  public void badRemove() throws Exception {
    UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.removeContainer("shouldnotexist");
  }

  @Test
  public void testEnvVars() throws Exception {
    final List<String> console = UnitTestUtils.mockJobConsoleLogger();

    DockerUtils.pullImage("busybox:latest");
    final String containerId = DockerUtils.createContainer("busybox:latest", System.getProperty("user.dir"),
        Stream.<Map.Entry<String, String>>builder().add(new SimpleEntry<>("TEST", "value")).build()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    final int exitCode = DockerUtils.execCommand(containerId, null, "sh", "-c", "echo \"TEST = $TEST\"");
    DockerUtils.removeContainer(containerId);

    assertThat("non-zero exit code", exitCode, equalTo(0));
    assertThat("env var value not correct", console, hasItem("TEST = value"));
  }
}
