package de.adorsys.keycloak.config.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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
                        52.72,
                        null,
                        null
                ),
                null
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
                        52.72,
                        null,
                        null
                ),
                null
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
                        52.72,
                        null,
                        null
                ),
                null
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
                        52.72,
                        null,
                        null
                ),
                null
        );


        OtherTestObject cloned = CloneUtils.deepClone(object, OtherTestObject.class);

        assertThat(object.getStringProperty(), is(equalTo(cloned.getStringProperty())));
        assertThat(object.getIntegerProperty(), is(equalTo(cloned.getIntegerProperty())));
        assertThat(object.getDoubleProperty(), is(equalTo(cloned.getDoubleProperty())));
        assertThat(object.getLongProperty(), is(equalTo(cloned.getLongProperty())));

        assertThat(object.getLocalDateProperty(), is(equalTo(cloned.getLocalDateProperty())));
        assertThat(object.getLocalDateTimeProperty(), is(equalTo(cloned.getLocalDateTimeProperty())));

        TestObject.InnerTestObject innerTestObject = object.getInnerTestObjectProperty();
        OtherTestObject.InnerTestObject clonedInnerTestObject = cloned.getInnerTestObjectProperty();

        assertThat(innerTestObject.getStringProperty(), is(equalTo(clonedInnerTestObject.getStringProperty())));
        assertThat(innerTestObject.getIntegerProperty(), is(equalTo(clonedInnerTestObject.getIntegerProperty())));
        assertThat(innerTestObject.getDoubleProperty(), is(equalTo(clonedInnerTestObject.getDoubleProperty())));
    }

    @Test
    public void shouldIgnorePropertyWhileCloning() throws Exception {
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

        assertThat(cloned.getStringProperty(), is(nullValue()));
    }

    @Test
    public void shouldIgnoreDeepPropertyWhileCloning() throws Exception {
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

        assertThat(cloned.getInnerTestObjectProperty().getStringProperty(), is(nullValue()));
        assertThat(cloned.getInnerTestObjectProperty().getIntegerProperty(), is(4321));
        assertThat(cloned.getInnerTestObjectProperty().getDoubleProperty(), is(52.72));
        assertThat(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty(), is(nullValue()));
    }

    @Test
    public void shouldIgnoreDeeperPropertyWhileCloning() throws Exception {
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

        assertThat(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getStringProperty(), is(nullValue()));
        assertThat(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getIntegerProperty(), is(654));
        assertThat(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getDoubleProperty(), is(87.32));
    }

    @Test
    public void shouldIgnoreDeeperPropertyWhileCloningInnerListObjects() throws Exception {
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

        assertThat(clonedInnerInnerTestObject.getStringProperty(), is(nullValue()));
        assertThat(clonedInnerInnerTestObject.getIntegerProperty(), is(9875));
        assertThat(clonedInnerInnerTestObject.getDoubleProperty(), is(91.82));
    }

    @Test
    public void shouldIgnoreTwoDeeperPropertiesWhileCloning() throws Exception {
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

        assertThat(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getStringProperty(), is(nullValue()));
        assertThat(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getIntegerProperty(), is(nullValue()));
        assertThat(cloned.getInnerTestObjectProperty().getInnerInnerTestObjectProperty().getDoubleProperty(), is(87.32));
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

        boolean areEqual = CloneUtils.deepEquals(origin, other);

        assertThat(areEqual, is(false));
    }

    @Test
    public void shouldPatchListProperties() throws Exception {
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
        assertThat(patchedStringList.get(0), is(equalTo("patched value in string list")));
    }

}
