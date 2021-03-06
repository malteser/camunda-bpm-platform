/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.form.handler;

import org.camunda.bpm.engine.impl.delegate.DelegateInvocation;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;

/**
 * @author Roman Smirnov
 *
 */
public class CreateTaskFormInvocation extends DelegateInvocation {

  protected TaskFormHandler taskFormHandler;
  protected TaskEntity task;

  public CreateTaskFormInvocation(TaskFormHandler taskFormHandler, TaskEntity task) {
    this.taskFormHandler = taskFormHandler;
    this.task = task;
  }

  protected void invoke() throws Exception {
    invocationResult = taskFormHandler.createTaskForm(task);
  }

  public Object getTarget() {
    return taskFormHandler;
  }

}
