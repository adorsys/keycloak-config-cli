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

package de.adorsys.keycloak.config.service.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaScriptEvaluatorTest {

    private JavaScriptEvaluator evaluator;
    private Map<String, Object> bindings;

    @BeforeEach
    void setUp() {
        evaluator = new JavaScriptEvaluator();
        bindings = new HashMap<>();
    }

    @Test
    void shouldEvaluateArithmetic() {
        Object result = evaluator.evaluate("1 + 1", bindings);
        assertThat(result, is(2));
    }

    @Test
    void shouldEvaluateLong() {
        Object result = evaluator.evaluate("2147483648", bindings);
        assertThat(result, is(2147483648L));
    }

    @Test
    void shouldEvaluateDouble() {
        Object result = evaluator.evaluate("1.5", bindings);
        assertThat(result, is(1.5d));
    }

    @Test
    void shouldEvaluateBoolean() {
        Object result = evaluator.evaluate("1 === 1", bindings);
        assertThat(result, is(true));
    }

    @Test
    void shouldEvaluateStringConcatenation() {
        Object result = evaluator.evaluate("'foo' + 'bar'", bindings);
        assertThat(result, is("foobar"));
    }

    @Test
    void shouldEvaluateWithBindings() {
        bindings.put("env", Map.of("APP_ENV", "PROD"));
        Object result = evaluator.evaluate("env.APP_ENV === 'PROD'", bindings);
        assertThat(result, is(true));
    }

    @Test
    void shouldReturnNull() {
        Object result = evaluator.evaluate("null", bindings);
        assertThat(result, is(nullValue()));
    }

    @Test
    void shouldEvaluateArray() {
        Object result = evaluator.evaluate("[1, 2]", bindings);
        assertThat(result, is(notNullValue()));
    }

    @Test
    void shouldEvaluateObject() {
        Object result = evaluator.evaluate("({a: 1})", bindings);
        assertThat(result, is(instanceOf(Map.class)));
    }

    @Test
    void shouldThrowOnInvalidScript() {
        assertThrows(Exception.class, () -> evaluator.evaluate("invalid logic", bindings));
    }

    @Test
    void shouldNotAllowHostClassLookup() {
        assertThrows(Exception.class, () -> evaluator.evaluate("java.lang.System.exit(0)", bindings));
    }
}
