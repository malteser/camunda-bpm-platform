/* Licensed under the Apache License, Version 2.0 (the "License");
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

package org.camunda.bpm.qa.performance.engine.framework.activitylog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.qa.performance.engine.framework.PerfTestResult;
import org.camunda.bpm.qa.performance.engine.framework.PerfTestResults;
import org.camunda.bpm.qa.performance.engine.framework.aggregate.TabularResultAggregator;
import org.camunda.bpm.qa.performance.engine.framework.aggregate.TabularResultSet;

public class ActivityCountAggregator extends TabularResultAggregator {

  public static final long INTERVAL = 1000;
  public static final long TIME_UNIT = 1000;

  public ActivityCountAggregator(String resultsFolderPath) {
    super(resultsFolderPath);
    sortResults(false);
  }

  protected TabularResultSet createAggregatedResultsInstance() {
    TabularResultSet tabularResultSet = new TabularResultSet();

    List<String> resultColumnNames = tabularResultSet.getResultColumnNames();
    resultColumnNames.add("Seconds");

    return tabularResultSet;
  }

  protected void processResults(PerfTestResults results, TabularResultSet tabularResultSet) {
    // add columns for activity names
    List<String> resultColumnNames = tabularResultSet.getResultColumnNames();
    List<String> activities = results.getConfiguration().getWatchActivities();
    for (String activity : activities) {
      resultColumnNames.add("");
      resultColumnNames.add(activity);
      resultColumnNames.add("");
    }

    // process pass results
    int numberOfRuns = results.getConfiguration().getNumberOfRuns();
    for (PerfTestResult passResult : results.getPassResults()) {
      if (!passResult.getActivityResults().isEmpty()) {
        processPassResult(tabularResultSet, activities, passResult, numberOfRuns);
      }
    }
  }

  protected void processPassResult(TabularResultSet tabularResultSet, List<String> activities, PerfTestResult passResult, int numberOfRuns) {
    addPassResultRow(tabularResultSet, activities, passResult, numberOfRuns);

    // get first and last timestamp
    Date firstStartTime = null;
    Date lastEndTime = null;

    for (List<ActivityPerfTestResult> activityResults : passResult.getActivityResults().values()) {
      for (ActivityPerfTestResult activityResult : activityResults) {
        if (firstStartTime == null || activityResult.getStartTime().before(firstStartTime)) {
          firstStartTime = activityResult.getStartTime();
        }

        if (lastEndTime == null || activityResult.getEndTime().after(lastEndTime)) {
          lastEndTime = activityResult.getEndTime();
        }
      }
    }

    long firstTimestamp = firstStartTime.getTime();
    long lastTimestamp = lastEndTime.getTime();
    List<Map<String, ActivityCount>> resultTable = new ArrayList<Map<String, ActivityCount>>();

    for (long t = firstTimestamp; t <= lastTimestamp + INTERVAL; t += INTERVAL) {
      Map<String, ActivityCount> activitiesMap = new HashMap<String, ActivityCount>();
      for (String activity : activities) {
        activitiesMap.put(activity, new ActivityCount());
      }
      resultTable.add(activitiesMap);
    }


    for (List<ActivityPerfTestResult> activityResults : passResult.getActivityResults().values()) {
      for (ActivityPerfTestResult activityResult : activityResults) {
        String activityId = activityResult.getActivityId();
        int startSlot = calculateTimeSlot(activityResult.getStartTime(), firstTimestamp);
        int endSlot = calculateTimeSlot(activityResult.getEndTime(), firstTimestamp);
        resultTable.get(startSlot).get(activityId).incrementStarted();
        resultTable.get(endSlot).get(activityId).incrementEnded();
        resultTable.get(endSlot).get(activityId).addDuration(activityResult.getDuration());
      }
    }

    ArrayList<Object> row = null;
    Map<String, ActivityCount> sumMap = new HashMap<String, ActivityCount>();
    for (String activity : activities) {
      sumMap.put(activity, new ActivityCount());
    }

    for (int i = 0; i < resultTable.size(); i++) {
      row = new ArrayList<Object>();
      row.add(i * INTERVAL / TIME_UNIT);
      for (String activity : activities) {
        sumMap.get(activity).addStarted(resultTable.get(i).get(activity).getStarted());
        sumMap.get(activity).addEnded(resultTable.get(i).get(activity).getEnded());
        sumMap.get(activity).addDuration(resultTable.get(i).get(activity).getDuration());
        long avgDuration = 0;
        if (sumMap.get(activity).getEnded() > 0) {
          avgDuration = sumMap.get(activity).getDuration() / sumMap.get(activity).getEnded();
        }
        row.add(sumMap.get(activity).getStarted());
        row.add(sumMap.get(activity).getEnded());
        row.add(avgDuration);
      }
      tabularResultSet.addResultRow(row);
    }
  }

  protected int calculateTimeSlot(Date date, long firstTimestamp) {
    return Math.round((date.getTime() - firstTimestamp) / INTERVAL);
  }

  protected void addPassResultRow(TabularResultSet tabularResultSet, List<String> activities, PerfTestResult passResult, int numberOfRuns) {
    ArrayList<Object> row = new ArrayList<Object>();
    // add pass header
    row.add("Pass (Runs: " + numberOfRuns + ", Threads: " + passResult.getNumberOfThreads() + ", Duration: " + passResult.getDuration() + ")");
    for (String ignored : activities) {
      row.add("started");
      row.add("ended");
      row.add("&Oslash; duration");
    }
    tabularResultSet.addResultRow(row);
  }

  class ActivityCount {
    int started = 0;
    int ended = 0;
    long duration = 0;

    public void incrementStarted() {
      ++started;
    }

    public void addStarted(int started) {
      this.started += started;
    }

    public int getStarted() {
      return started;
    }

    public void incrementEnded() {
      ++ended;
    }

    public void addEnded(int ended) {
      this.ended += ended;
    }

    public int getEnded() {
      return ended;
    }

    public void addDuration(Long duration) {
      if (duration != null) {
        this.duration += duration;
      }
    }

    public long getDuration() {
      return duration;
    }

  }

}
