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

package org.camunda.bpm.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.Condition;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.pvm.PvmTransition;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

/**
 * Helper class for implementing BPMN 2.0 activities, offering convenience
 * methods specific to BPMN 2.0.
 *
 * This class can be used by inheritance or aggregation.
 *
 * @author Joram Barrez
 */
public class BpmnActivityBehavior {

  private static Logger log = Logger.getLogger(BpmnActivityBehavior.class.getName());

  /**
   * Performs the default outgoing BPMN 2.0 behavior, which is having parallel
   * paths of executions for the outgoing sequence flow.
   *
   * More precisely: every sequence flow that has a condition which evaluates to
   * true (or which doesn't have a condition), is selected for continuation of
   * the process instance. If multiple sequencer flow are selected, multiple,
   * parallel paths of executions are created.
   */
  public void performDefaultOutgoingBehavior(ActivityExecution activityExceution) {
    performOutgoingBehavior(activityExceution, true, false, null);
  }

  /**
   * Performs the default outgoing BPMN 2.0 behavior (@see
   * {@link #performDefaultOutgoingBehavior(ActivityExecution)}), but without
   * checking the conditions on the outgoing sequence flow.
   *
   * This means that every outgoing sequence flow is selected for continuing the
   * process instance, regardless of having a condition or not. In case of
   * multiple outgoing sequence flow, multiple parallel paths of executions will
   * be created.
   */
  public void performIgnoreConditionsOutgoingBehavior(ActivityExecution activityExecution) {
    performOutgoingBehavior(activityExecution, false, false, null);
  }

  /**
   * Actual implementation of leaving an activity.
   *
   * @param execution
   *          The current execution context
   * @param checkConditions
   *          Whether or not to check conditions before determining whether or
   *          not to take a transition.
   * @param throwExceptionIfExecutionStuck
   *          If true, an {@link ProcessEngineException} will be thrown in case no
   *          transition could be found to leave the activity.
   */
  protected void performOutgoingBehavior(ActivityExecution execution,
          boolean checkConditions, boolean throwExceptionIfExecutionStuck, List<ActivityExecution> reusableExecutions) {

    if (log.isLoggable(Level.FINE)) {
      log.fine("Leaving activity '" + execution.getActivity().getId() + "'");
    }

    String defaultSequenceFlow = (String) execution.getActivity().getProperty("default");
    List<PvmTransition> transitionsToTake = new ArrayList<PvmTransition>();

    List<PvmTransition> outgoingTransitions = execution.getActivity().getOutgoingTransitions();
    for (PvmTransition outgoingTransition : outgoingTransitions) {
      if (defaultSequenceFlow == null || !outgoingTransition.getId().equals(defaultSequenceFlow)) {
        Condition condition = (Condition) outgoingTransition.getProperty(BpmnParse.PROPERTYNAME_CONDITION);
        if (condition == null || !checkConditions || condition.evaluate(execution)) {
          transitionsToTake.add(outgoingTransition);
        }
      }
    }

    if (transitionsToTake.size() == 1) {

      execution.leaveActivityViaTransition(transitionsToTake.get(0));

    } else if (transitionsToTake.size() >= 1) {

      if (reusableExecutions == null || reusableExecutions.isEmpty()) {
        execution.leaveActivityViaTransitions(transitionsToTake, Arrays.asList(execution));
      } else {
        execution.leaveActivityViaTransitions(transitionsToTake, reusableExecutions);
      }

    } else {

      if (defaultSequenceFlow != null) {
        PvmTransition defaultTransition = execution.getActivity().findOutgoingTransition(defaultSequenceFlow);
        if (defaultTransition != null) {
          execution.leaveActivityViaTransition(defaultTransition);
        } else {
          throw new ProcessEngineException("Default sequence flow '" + defaultSequenceFlow + "' could not be not found");
        }

      } else if (!outgoingTransitions.isEmpty()) {
        throw new ProcessEngineException("No conditional sequence flow leaving the Flow Node '" + execution.getActivity().getId() + "' could be selected for continuing the process");

      } else {

        Object isForCompensation = execution.getActivity().getProperty(BpmnParse.PROPERTYNAME_IS_FOR_COMPENSATION);
        if(isForCompensation != null && (Boolean) isForCompensation) {

          execution.endCompensation();

        } else {

          if (log.isLoggable(Level.FINE)) {
            log.fine("No outgoing sequence flow found for " + execution.getActivity().getId() + ". Ending execution.");
          }
          execution.end(true);

          if (throwExceptionIfExecutionStuck) {
            throw new ProcessEngineException("No outgoing sequence flow of the inclusive gateway '" + execution.getActivity().getId()
                  + "' could be selected for continuing the process");
          }
        }

      }
    }
  }

}
