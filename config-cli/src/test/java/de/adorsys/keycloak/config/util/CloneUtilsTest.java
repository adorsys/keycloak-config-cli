package de.adorsys.keycloak.config.util;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class CloneUtilsTest {

    @Test
    public void shouldBeEqualAfterClone() throws Exception {
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
                        52.72
                )
        );


        TestObject cloned = CloneUtils.deepClone(object);

        assertThat(cloned, is(equalTo(object)));
    }

    @Test
    public void shouldNotBeModifiedIfOriginIsModified() throws Exception {
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
                        52.72
                )
        );


        TestObject cloned = CloneUtils.deepClone(object);

        object.setStringProperty("my string 2");

        assertThat(cloned, is(not(equalTo(object))));
    }

    @Test
    public void shouldNotBeModifiedIfInnerObjectIsModified() throws Exception {
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
                        52.72
                )
        );


        TestObject cloned = CloneUtils.deepClone(object);

        object.getInnerTestObjectProperty().setStringProperty("my string 2");

        assertThat(cloned, is(not(equalTo(object))));
    }

    @Test
    public void shouldCloneDifferentTypes() throws Exception {
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
                        52.72
                )
        );


        OtherTestObject cloned = CloneUtils.deepClone(object, OtherTestObject.class);

        assertThat(object.equals(cloned), is(true));
    }

    @Test
    public void shouldPatch() throws Exception {
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
                        52.72
                )
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
                        null
                )
        );

        TestObject cloned = CloneUtils.deepClone(origin);

        TestObject patched = CloneUtils.deepPatch(origin, patch);

        assertThat(patched.getStringProperty(), is(equalTo(patch.getStringProperty())));
        assertThat(patched.getIntegerProperty(), is(equalTo(origin.getIntegerProperty())));
        assertThat(patched.getDoubleProperty(), is(equalTo(origin.getDoubleProperty())));
        assertThat(patched.getLongProperty(), is(equalTo(patch.getLongProperty())));
        assertThat(patched.getLocalDateProperty(), is(nullValue()));
        assertThat(patched.getLocalDateTimeProperty(), is(nullValue()));

        TestObject.InnerTestObject patchedInnerObject = patched.getInnerTestObjectProperty();
        assertThat(patchedInnerObject.getStringProperty(), is(equalTo(patch.getInnerTestObjectProperty().getStringProperty())));
        assertThat(patchedInnerObject.getIntegerProperty(), is(equalTo(patch.getInnerTestObjectProperty().getIntegerProperty())));
        assertThat(patchedInnerObject.getDoubleProperty(), is(equalTo(origin.getInnerTestObjectProperty().getDoubleProperty())));

        assertThat(origin, is(equalTo(cloned)));
    }

    @Test
    public void shouldDeepEqual() throws Exception {
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
                        52.72
                )
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
                        52.72
                )
        );

        boolean areEqual = CloneUtils.deepEquals(origin, other);

        assertThat(areEqual, is(true));
    }

    @Test
    public void shouldNotDeepEqual() throws Exception {
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
                        52.72
                )
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
                        52.72
                )
        );

        boolean areEqual = CloneUtils.deepEquals(origin, other);

        assertThat(areEqual, is(false));
    }
}
