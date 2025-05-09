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

package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.assets.OtherTestObject;
import de.adorsys.keycloak.config.assets.TestObject;
import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@ExtendWith(GithubActionsExtension.class)
class CloneUtilTest {
    @Test
    void shouldBeEqualAfterClone() {
        TestObject object = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );


        TestObject cloned = CloneUtil.deepClone(object);

        assertEquals(cloned, object);
    }

    @Test
    void shouldNotBeModifiedIfOriginIsModified() {
        TestObject object = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );


        TestObject cloned = CloneUtil.deepClone(object);

        object.setStringProperty("my string 2");

        assertNotEquals(cloned, object);
    }

    @Test
    void shouldNotBeModifiedIfInnerObjectIsModified() {
        TestObject object = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );


        TestObject cloned = CloneUtil.deepClone(object);

        object.getInnerTestObjectProperty().setStringProperty("my string 2");

        assertNotEquals(cloned, object);
    }

    @Test
    void shouldCloneDifferentTypes() {
        TestObject object = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );


        OtherTestObject cloned = CloneUtil.deepClone(object, OtherTestObject.class);

        assertEquals(cloned.getStringProperty(), object.getStringProperty());
        assertEquals(cloned.getIntegerProperty(), object.getIntegerProperty());
        assertEquals(cloned.getDoubleProperty(), object.getDoubleProperty());
        assertEquals(cloned.getLongProperty(), object.getLongProperty());

        assertEquals(cloned.getLocalDateProperty(), object.getLocalDateProperty());
        assertEquals(cloned.getLocalDateTimeProperty(), object.getLocalDateTimeProperty());

        TestObject.InnerTestObject innerTestObject = object.getInnerTestObjectProperty();
        OtherTestObject.InnerTestObject clonedInnerTestObject = cloned.getInnerTestObjectProperty();

        assertEquals(clonedInnerTestObject.getStringProperty(), innerTestObject.getStringProperty());
        assertEquals(clonedInnerTestObject.getIntegerProperty(), innerTestObject.getIntegerProperty());
        assertEquals(clonedInnerTestObject.getDoubleProperty(), innerTestObject.getDoubleProperty());
    }

    @Test
    void shouldIgnorePropertyWhileCloning() {
        TestObject object = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );


        TestObject cloned = CloneUtil.deepClone(object, "stringProperty");

        assertNull(cloned.getStringProperty());
    }

    @Test
    void shouldPatch() {
        TestObject origin = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );

        TestObject patch = new TestObject(
                "my string 1",
                null,
                null,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string 1",
                        4322,
                        null,
                        null,
                        null
                ),
                null
        );

        TestObject cloned = CloneUtil.deepClone(origin);

        TestObject patched = CloneUtil.patch(origin, patch);

        assertEquals(patch.getStringProperty(), patched.getStringProperty());
        assertEquals(origin.getIntegerProperty(), patched.getIntegerProperty());
        assertEquals(origin.getDoubleProperty(), patched.getDoubleProperty());
        assertEquals(patch.getLongProperty(), patched.getLongProperty());
        assertNull(patched.getLocalDateProperty());
        assertNull(patched.getLocalDateTimeProperty());

        assertEquals(cloned, origin);
    }

    @Test
    void shouldDeepEqual() {
        TestObject origin = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );

        TestObject other = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );

        assertTrue(CloneUtil.deepEquals(origin, other));
    }

    @Test
    void shouldDeepEqualArraysWithIgnoredValue() {
        var origin = List.of(
                new TestObject(
                        "my string",
                        1234,
                        123.123,
                        1235L,
                        null,
                        null,
                        new TestObject.InnerTestObject(
                                "my other string",
                                4321,
                                52.72,
                                null,
                                null
                        ),
                        null
                ),
                new TestObject(
                        "your string",
                        4567,
                        456.123,
                        4567L,
                        null,
                        null,
                        new TestObject.InnerTestObject(
                                "my other string",
                                7894,
                                88.88,
                                null,
                                null
                        ),
                        null
                )
        );

        var other = origin.stream()
                .map(o -> CloneUtil.deepClone(o, "stringProperty"))
                .toList();
        other.get(0).setStringProperty("wrong string");
        other.get(1).setStringProperty("wrong string again");

        assertTrue(CloneUtil.deepEquals(origin, other, "stringProperty"));
    }

    @Test
    void shouldNotDeepEqual() {
        TestObject origin = new TestObject(
                "my string",
                1234,
                123.123,
                1234L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );

        TestObject other = new TestObject(
                "my string",
                1234,
                123.123,
                1235L,
                null,
                null,
                new TestObject.InnerTestObject(
                        "my other string",
                        4321,
                        52.72,
                        null,
                        null
                ),
                null
        );

        assertFalse(CloneUtil.deepEquals(origin, other));
    }

    @Test
    void shouldReturnNull() {
        Object object = new Object();

        assertThat(CloneUtil.deepClone(null), nullValue());
        assertThat(CloneUtil.deepClone(null, TestObject.class), nullValue());
        assertThat(CloneUtil.patch(null, null), nullValue());
        assertThat(CloneUtil.patch(object, null), is(object));
        assertThat(CloneUtil.deepEquals(null, null), is(true));
        assertThat(CloneUtil.deepEquals(object, null), is(false));
        assertThat(CloneUtil.deepEquals(null, object), is(false));
    }
}
