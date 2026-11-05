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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowRepresentationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldReadSingleValueWorkflowStepParameter() throws Exception {
        String json = """
                {
                  "name": "workflow",
                  "steps": [
                    {
                      "uses": "do-something",
                      "with": {
                        "message": "We are here"
                      }
                    }
                  ]
                }
                """;

        WorkflowRepresentation workflow =
                mapper.readValue(json, WorkflowRepresentation.class);

        assertEquals(
                List.of("We are here"),
                workflow.getSteps().get(0).getWith().get("message")
        );
    }



    @Test
    void shouldReadArrayWorkflowStepParameter() throws Exception {
        String json = """
            {
              "steps": [
                {
                  "uses": "do-something",
                  "with": {
                    "message": ["first", "second"]
                  }
                }
              ]
            }
            """;

        WorkflowRepresentation workflow =
                mapper.readValue(json, WorkflowRepresentation.class);

        assertEquals(
                List.of("first", "second"),
                workflow.getSteps().get(0).getWith().get("message")
        );
    }



}

