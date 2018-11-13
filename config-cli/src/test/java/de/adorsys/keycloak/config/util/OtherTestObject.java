package de.adorsys.keycloak.config.util;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OtherTestObject {
    private final String stringProperty;
    private final Integer integerProperty;
    private final Double doubleProperty;
    private final Long longProperty;
    private final LocalDate localDateProperty;
    private final LocalDateTime localDateTimeProperty;
    private final InnerTestObject innerTestObjectProperty;

    public OtherTestObject(
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtherTestObject that = (OtherTestObject) o;

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
        private final String stringProperty;
        private final Integer integerProperty;
        private final Double doubleProperty;


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

        @Override
        public int hashCode() {
            int result = stringProperty != null ? stringProperty.hashCode() : 0;
            result = 31 * result + (integerProperty != null ? integerProperty.hashCode() : 0);
            result = 31 * result + (doubleProperty != null ? doubleProperty.hashCode() : 0);
            return result;
        }
    }
}
