/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 * ---license-end
 */

package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class WorkflowRepresentation {

    private String id;
    private String name;
    private Boolean enabled;
    private String on;
    private WorkflowScheduleRepresentation schedule;
    private WorkflowConcurrencyRepresentation concurrency;

    @JsonProperty("if")
    private String condition;

    private List<WorkflowStepRepresentation> steps;
    private WorkflowStateRepresentation state;
    private Map<String, List<String>> with;
    private String cancelInProgress;
    private String restartInProgress;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getOn() {
        return on;
    }

    public void setOn(String on) {
        this.on = on;
    }

    public WorkflowScheduleRepresentation getSchedule() {
        return schedule;
    }

    public void setSchedule(WorkflowScheduleRepresentation schedule) {
        this.schedule = schedule;
    }

    public WorkflowConcurrencyRepresentation getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(WorkflowConcurrencyRepresentation concurrency) {
        this.concurrency = concurrency;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public List<WorkflowStepRepresentation> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStepRepresentation> steps) {
        this.steps = steps;
    }

    public WorkflowStateRepresentation getState() {
        return state;
    }

    public void setState(WorkflowStateRepresentation state) {
        this.state = state;
    }

    public Map<String, List<String>> getWith() {
        return with;
    }

    public void setWith(Map<String, List<String>> with) {
        this.with = with;
    }

    public String getCancelInProgress() {
        return cancelInProgress;
    }

    public void setCancelInProgress(String cancelInProgress) {
        this.cancelInProgress = cancelInProgress;
    }

    public String getRestartInProgress() {
        return restartInProgress;
    }

    public void setRestartInProgress(String restartInProgress) {
        this.restartInProgress = restartInProgress;
    }

    // ---- Nested representations ----

    public static class WorkflowScheduleRepresentation {
        private String after;

        @JsonProperty("batch-size")
        private Integer batchSize;

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class WorkflowConcurrencyRepresentation {
        @JsonProperty("cancel-in-progress")
        private String cancelInProgress;

        @JsonProperty("restart-in-progress")
        private String restartInProgress;

        public String getCancelInProgress() {
            return cancelInProgress;
        }

        public void setCancelInProgress(String cancelInProgress) {
            this.cancelInProgress = cancelInProgress;
        }

        public String getRestartInProgress() {
            return restartInProgress;
        }

        public void setRestartInProgress(String restartInProgress) {
            this.restartInProgress = restartInProgress;
        }
    }

    public static class WorkflowStepRepresentation {
        private String uses;
        private String after;

        @JsonProperty("scheduled-at")
        private Long scheduledAt;

        private String id;
        private Map<String, List<String>> config;

        public String getUses() {
            return uses;
        }

        public void setUses(String uses) {
            this.uses = uses;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }

        public Long getScheduledAt() {
            return scheduledAt;
        }

        public void setScheduledAt(Long scheduledAt) {
            this.scheduledAt = scheduledAt;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, List<String>> getConfig() {
            return config;
        }

        public void setConfig(Map<String, List<String>> config) {
            this.config = config;
        }
    }

    public static class WorkflowStateRepresentation {
        private List<String> errors;

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
