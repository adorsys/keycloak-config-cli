package de.adorsys.keycloak.config.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestObject {
    private String stringProperty;
    private Integer integerProperty;
    private Double doubleProperty;
    private Long longProperty;
    private LocalDate localDateProperty;
    private LocalDateTime localDateTimeProperty;
    private InnerTestObject innerTestObjectProperty;

    public TestObject(
            @JsonProperty("stringProperty") String stringProperty,
            @JsonProperty("integerProperty") Integer integerProperty,
            @JsonProperty("doubleProperty") Double doubleProperty,
            @JsonProperty("longProperty") Long longProperty,
            @JsonProperty("localDateProperty") LocalDate localDateProperty,
            @JsonProperty("localDateTimeProperty") LocalDateTime localDateTimeProperty,
            @JsonProperty("innerTestObjectProperty") InnerTestObject innerTestObjectProperty
    ) {
        this.stringProperty = stringProperty;
        this.integerProperty = integerProperty;
        this.doubleProperty = doubleProperty;
        this.longProperty = longProperty;
        this.localDateProperty = localDateProperty;
        this.localDateTimeProperty = localDateTimeProperty;
        this.innerTestObjectProperty = innerTestObjectProperty;
    }

    public String getStringProperty() {
        return stringProperty;
    }

    public Integer getIntegerProperty() {
        return integerProperty;
    }

    public Double getDoubleProperty() {
        return doubleProperty;
    }

    public Long getLongProperty() {
        return longProperty;
    }

    public LocalDate getLocalDateProperty() {
        return localDateProperty;
    }

    public LocalDateTime getLocalDateTimeProperty() {
        return localDateTimeProperty;
    }

    public InnerTestObject getInnerTestObjectProperty() {
        return innerTestObjectProperty;
    }

    public void setStringProperty(String stringProperty) {
        this.stringProperty = stringProperty;
    }

    public void setIntegerProperty(Integer integerProperty) {
        this.integerProperty = integerProperty;
    }

    public void setDoubleProperty(Double doubleProperty) {
        this.doubleProperty = doubleProperty;
    }

    public void setLongProperty(Long longProperty) {
        this.longProperty = longProperty;
    }

    public void setLocalDateProperty(LocalDate localDateProperty) {
        this.localDateProperty = localDateProperty;
    }

    public void setLocalDateTimeProperty(LocalDateTime localDateTimeProperty) {
        this.localDateTimeProperty = localDateTimeProperty;
    }

    public void setInnerTestObjectProperty(InnerTestObject innerTestObjectProperty) {
        this.innerTestObjectProperty = innerTestObjectProperty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestObject that = (TestObject) o;

        if (stringProperty != null ? !stringProperty.equals(that.stringProperty) : that.stringProperty != null)
            return false;
        if (integerProperty != null ? !integerProperty.equals(that.integerProperty) : that.integerProperty != null)
            return false;
        if (doubleProperty != null ? !doubleProperty.equals(that.doubleProperty) : that.doubleProperty != null)
            return false;
        if (longProperty != null ? !longProperty.equals(that.longProperty) : that.longProperty != null)
            return false;
        if (localDateProperty != null ? !localDateProperty.equals(that.localDateProperty) : that.localDateProperty != null)
            return false;
        if (localDateTimeProperty != null ? !localDateTimeProperty.equals(that.localDateTimeProperty) : that.localDateTimeProperty != null)
            return false;
        return innerTestObjectProperty != null ? innerTestObjectProperty.equals(that.innerTestObjectProperty) : that.innerTestObjectProperty == null;
    }

    public boolean equals(OtherTestObject that) {
        if (that == null) return false;

        if (stringProperty != null ? !stringProperty.equals(that.getStringProperty()) : that.getStringProperty() != null)
            return false;
        if (integerProperty != null ? !integerProperty.equals(that.getIntegerProperty()) : that.getIntegerProperty() != null)
            return false;
        if (doubleProperty != null ? !doubleProperty.equals(that.getDoubleProperty()) : that.getDoubleProperty() != null)
            return false;
        if (longProperty != null ? !longProperty.equals(that.getLongProperty()) : that.getLongProperty() != null)
            return false;
        if (localDateProperty != null ? !localDateProperty.equals(that.getLocalDateProperty()) : that.getLocalDateProperty() != null)
            return false;
        if (localDateTimeProperty != null ? !localDateTimeProperty.equals(that.getLocalDateTimeProperty()) : that.getLocalDateTimeProperty() != null)
            return false;
        return innerTestObjectProperty != null ? innerTestObjectProperty.equals(that.getInnerTestObjectProperty()) : that.getInnerTestObjectProperty() == null;
    }

    @Override
    public int hashCode() {
        int result = stringProperty != null ? stringProperty.hashCode() : 0;
        result = 31 * result + (integerProperty != null ? integerProperty.hashCode() : 0);
        result = 31 * result + (doubleProperty != null ? doubleProperty.hashCode() : 0);
        result = 31 * result + (longProperty != null ? longProperty.hashCode() : 0);
        result = 31 * result + (localDateProperty != null ? localDateProperty.hashCode() : 0);
        result = 31 * result + (localDateTimeProperty != null ? localDateTimeProperty.hashCode() : 0);
        result = 31 * result + (innerTestObjectProperty != null ? innerTestObjectProperty.hashCode() : 0);
        return result;
    }

    static class InnerTestObject {
        private String stringProperty;
        private Integer integerProperty;
        private Double doubleProperty;


        public InnerTestObject(
                @JsonProperty("stringProperty") String stringProperty,
                @JsonProperty("integerProperty") Integer integerProperty,
                @JsonProperty("doubleProperty") Double doubleProperty
        ) {
            this.stringProperty = stringProperty;
            this.integerProperty = integerProperty;
            this.doubleProperty = doubleProperty;
        }

        public String getStringProperty() {
            return stringProperty;
        }

        public Integer getIntegerProperty() {
            return integerProperty;
        }

        public Double getDoubleProperty() {
            return doubleProperty;
        }

        public void setStringProperty(String stringProperty) {
            this.stringProperty = stringProperty;
        }

        public void setIntegerProperty(Integer integerProperty) {
            this.integerProperty = integerProperty;
        }

        public void setDoubleProperty(Double doubleProperty) {
            this.doubleProperty = doubleProperty;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InnerTestObject that = (InnerTestObject) o;

            if (stringProperty != null ? !stringProperty.equals(that.stringProperty) : that.stringProperty != null)
                return false;
            if (integerProperty != null ? !integerProperty.equals(that.integerProperty) : that.integerProperty != null)
                return false;
            return doubleProperty != null ? doubleProperty.equals(that.doubleProperty) : that.doubleProperty == null;
        }

        public boolean equals(OtherTestObject.InnerTestObject that) {
            if (that == null) return false;

            if (stringProperty != null ? !stringProperty.equals(that.getStringProperty()) : that.getStringProperty() != null)
                return false;
            if (integerProperty != null ? !integerProperty.equals(that.getIntegerProperty()) : that.getIntegerProperty() != null)
                return false;
            return doubleProperty != null ? doubleProperty.equals(that.getDoubleProperty()) : that.getDoubleProperty() == null;
        }

        @Override
        public int hashCode() {
            int result = stringProperty != null ? stringProperty.hashCode() : 0;
            result = 31 * result + (integerProperty != null ? integerProperty.hashCode() : 0);
            result = 31 * result + (doubleProperty != null ? doubleProperty.hashCode() : 0);
            return result;
        }
    }
}
