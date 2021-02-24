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
package io.bitgrillr.gocddockerexecplugin.utils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.plugin.api.task.EnvironmentVariables;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.powermock.api.mockito.PowerMockito;

public class UnitTestUtils {

  private UnitTestUtils() {
  }

  public static List<String> mockJobConsoleLogger() {
    TaskExecutionContext taskExecutionContext = mock(TaskExecutionContext.class);
    Console mockedConsole = mock(Console.class);
    when(taskExecutionContext.console()).thenReturn(mockedConsole);
    EnvironmentVariables environment = mock(EnvironmentVariables.class);
    when(taskExecutionContext.environment()).thenReturn(environment);

    // ReflectionUtil.setStaticField(JobConsoleLogger.class, "context",
    // taskExecutionContext);
    try {
      Field field = JobConsoleLogger.class.getDeclaredField("context");
      field.setAccessible(true);
      field.set(null, taskExecutionContext);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    List<String> console = new ArrayList<>();
    doAnswer(i -> {
      console.add(i.getArgument(0));
      return null;
    }).when(mockedConsole).printLine(anyString());
    return console;
  }

}
