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
package org.camunda.bpm.engine.impl.pvm.runtime.operation;

import java.util.logging.Logger;

import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public abstract class PvmAtomicOperationCreateScope implements PvmAtomicOperation {

  private static Logger log = Logger.getLogger(PvmAtomicOperationCreateScope.class.getName());

  public void execute(PvmExecutionImpl execution) {

    // reset activity instance id before creating the scope
    execution.setActivityInstanceId(execution.getParentActivityInstanceId());

    PvmExecutionImpl propagatingExecution = null;
    PvmActivity activity = execution.getActivity();
    if (activity.isScope()) {
      propagatingExecution = execution.createExecution();
      propagatingExecution.setActivity(activity);
      propagatingExecution.setTransition(execution.getTransition());
      execution.setTransition(null);
      execution.setActive(false);
      execution.setActivity(null);
      log.fine("create scope: parent "+execution+" continues as execution "+propagatingExecution);
      propagatingExecution.initialize();

    } else {
      propagatingExecution = execution;
      // <LEGACY>: in general, io mappings may only exist when the activity is scope
      // however, for multi instance activities, the inner activity does not become a scope
      // due to the presence of an io mapping. In that case, it is ok to execute the io mapping
      // anyway because the multi-instance body already ensures variable isolation
      propagatingExecution.executeIoMapping();
    }


    scopeCreated(propagatingExecution);
  }

  /**
   * Called with the propagating execution
   */
  protected abstract void scopeCreated(PvmExecutionImpl execution);

}
