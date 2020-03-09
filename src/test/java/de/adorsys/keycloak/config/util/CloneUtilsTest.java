package de.adorsys.keycloak.config.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

public class CloneUtilsTest {

    @Test
    public void shouldBeEqualAfterClone() {
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


        TestObject cloned = CloneUtils.deepClone(object);

        assertEquals(cloned, object);
    }

    @Test
    public void shouldNotBeModifiedIfOriginIsModified() {
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


        TestObject cloned = CloneUtils.deepClone(object);

        object.setStringProperty("my string 2");

        assertNotEquals(cloned, object);
    }

    @Test
    public void shouldNotBeModifiedIfInnerObjectIsModified() {
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


        TestObject cloned = CloneUtils.deepClone(object);

        object.getInnerTestObjectProperty().setStringProperty("my string 2");

        assertNotEquals(cloned, object);
    }

    @Test
    public void shouldCloneDifferentTypes() {
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


        OtherTestObject cloned = CloneUtils.deepClone(object, OtherTestObject.class);

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
    public void shouldIgnorePropertyWhileCloning() {
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


        TestObject cloned = CloneUtils.deepClone(object, "stringProperty");

        assertNull(cloned.getStringProperty());
    }

    @Test
    public void shouldIgnoreDeepPropertyWhileCloning() {
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


        TestObject cloned = CloneUtils.deepClone(object, "innerTestObjectProperty.stringProperty");

        assertNull(cloned.getInnerTestObjectProperty().getStringProperty());
        assertEquals(4321, cloned.getInnerTestObjectProperty().getIntegerProperty());
        assertEquals(52.72, cloned.getInnerTestObjectProperty().getDoubleProperty());
        assertNull(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty());
    }

    @Test
    public void shouldIgnoreDeeperPropertyWhileCloning() {
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
                        new TestObject.InnerTestObject.InnerInnerTestObject(
                                "my deeper string",
                                654,
                                87.32
                        ),
                        null
                ),
                null
        );


        TestObject cloned = CloneUtils.deepClone(
                object,
                "innerTestObjectProperty.innerInnerTestObjectProperty.stringProperty"
        );

        assertNull(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getStringProperty());
        assertEquals(654, cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getIntegerProperty());
        assertEquals(87.32, cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getDoubleProperty());
    }

    @Test
    public void shouldIgnoreDeeperPropertyWhileCloningInnerListObjects() {
        ArrayList<TestObject.InnerTestObject.InnerInnerTestObject> innerInnerTestList = new ArrayList<>();
        TestObject.InnerTestObject.InnerInnerTestObject innerInnerTestObject = new TestObject.InnerTestObject.InnerInnerTestObject(
                "my deeper string",
                9875,
                91.82
        );
        innerInnerTestList.add(innerInnerTestObject);

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
                        innerInnerTestList
                ),
                null
        );


        TestObject cloned = CloneUtils.deepClone(
                object,
                "innerTestObjectProperty.innerInnerTestListProperty.stringProperty"
        );

        List<TestObject.InnerTestObject.InnerInnerTestObject> clonedInnerTestList = cloned.getInnerTestObjectProperty().getInnerInnerTestListProperty();
        assertThat(clonedInnerTestList, hasSize(1));

        TestObject.InnerTestObject.InnerInnerTestObject clonedInnerInnerTestObject = clonedInnerTestList.get(0);

        assertNull(clonedInnerInnerTestObject.getStringProperty());
        assertEquals(9875, clonedInnerInnerTestObject.getIntegerProperty());
        assertEquals(91.82, clonedInnerInnerTestObject.getDoubleProperty());
    }

    @Test
    public void shouldIgnoreTwoDeeperPropertiesWhileCloning() {
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
                        new TestObject.InnerTestObject.InnerInnerTestObject(
                                "my deeper string",
                                654,
                                87.32
                        ),
                        null
                ),
                null
        );


        TestObject cloned = CloneUtils.deepClone(
                object,
                "innerTestObjectProperty.innerInnerTestObjectProperty.stringProperty",
                "innerTestObjectProperty.innerInnerTestObjectProperty.integerProperty"
        );

        assertNull(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getStringProperty());
        assertNull(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getIntegerProperty());
        assertEquals(87.32, cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getDoubleProperty());
    }


    @Test
    public void shouldPatch() {
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

        TestObject cloned = CloneUtils.deepClone(origin);

        TestObject patched = CloneUtils.deepPatch(origin, patch);

        assertEquals(patch.getStringProperty(), patched.getStringProperty());
        assertEquals(origin.getIntegerProperty(), patched.getIntegerProperty());
        assertEquals(origin.getDoubleProperty(), patched.getDoubleProperty());
        assertEquals(patch.getLongProperty(), patched.getLongProperty());
        assertNull(patched.getLocalDateProperty());
        assertNull(patched.getLocalDateTimeProperty());

        TestObject.InnerTestObject patchedInnerObject = patched.getInnerTestObjectProperty();
        assertEquals(patch.getInnerTestObjectProperty().getStringProperty(), patchedInnerObject.getStringProperty());
        assertEquals(patch.getInnerTestObjectProperty().getIntegerProperty(), patchedInnerObject.getIntegerProperty());
        assertEquals(origin.getInnerTestObjectProperty().getDoubleProperty(), patchedInnerObject.getDoubleProperty());

        assertEquals(cloned, origin);
    }

    @Test
    public void shouldDeepEqual() {
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

        assertTrue(CloneUtils.deepEquals(origin, other));
    }

    @Test
    public void shouldNotDeepEqual() {
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

        assertFalse(CloneUtils.deepEquals(origin, other));
    }

    @Test
    public void shouldPatchListProperties() {
        ArrayList<String> originStringList = new ArrayList<>();
        originStringList.add("value in string list");

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
                originStringList
        );

        ArrayList<String> patchStringList = new ArrayList<>();
        patchStringList.add("patched value in string list");

        TestObject patch = new TestObject(
                "my string patched",
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
                patchStringList
        );

        TestObject patched = CloneUtils.patch(origin, patch, TestObject.class);

        List<String> patchedStringList = patched.getStringList();
        assertThat(patchedStringList, hasSize(1));
        assertEquals("patched value in string list", patchedStringList.get(0));
    }

}
