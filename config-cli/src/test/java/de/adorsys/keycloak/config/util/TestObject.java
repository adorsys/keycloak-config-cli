package de.adorsys.keycloak.config.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class TestObject {
    private String stringProperty;
    private Integer integerProperty;
    private Double doubleProperty;
    private Long longProperty;
    private LocalDate localDateProperty;
    private LocalDateTime localDateTimeProperty;
    private InnerTestObject innerTestObjectProperty;

    private List<String> stringList;

    public TestObject(
            @JsonProperty("stringProperty") String stringProperty,
            @JsonProperty("integerProperty") Integer integerProperty,
            @JsonProperty("doubleProperty") Double doubleProperty,
            @JsonProperty("longProperty") Long longProperty,
            @JsonProperty("localDateProperty") LocalDate localDateProperty,
            @JsonProperty("localDateTimeProperty") LocalDateTime localDateTimeProperty,
            @JsonProperty("innerTestObjectProperty") InnerTestObject innerTestObjectProperty,
            @JsonProperty("stringList") List<String> stringList
    ) {
        this.stringProperty = stringProperty;
        this.integerProperty = integerProperty;
        this.doubleProperty = doubleProperty;
        this.longProperty = longProperty;
        this.localDateProperty = localDateProperty;
        this.localDateTimeProperty = localDateTimeProperty;
        this.innerTestObjectProperty = innerTestObjectProperty;
        this.stringList = stringList;
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

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
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
        private InnerInnerTestObject innerInnerTestObjectProperty;
        private List<InnerInnerTestObject> innerInnerTestListProperty;


        public InnerTestObject(
                @JsonProperty("stringProperty") String stringProperty,
                @JsonProperty("integerProperty") Integer integerProperty,
                @JsonProperty("doubleProperty") Double doubleProperty,
                @JsonProperty("innerInnerTestObjectProperty") InnerInnerTestObject innerInnerTestObjectProperty,
                @JsonProperty("innerInnerTestListProperty") List<InnerInnerTestObject> innerInnerTestListProperty
        ) {
            this.stringProperty = stringProperty;
            this.integerProperty = integerProperty;
            this.doubleProperty = doubleProperty;
            this.innerInnerTestObjectProperty = innerInnerTestObjectProperty;
            this.innerInnerTestListProperty = innerInnerTestListProperty;
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

        public InnerInnerTestObject getInnerInnerTestObjectProperty() {
            return innerInnerTestObjectProperty;
        }

        public void setInnerInnerTestObjectProperty(InnerInnerTestObject innerInnerTestObjectProperty) {
            this.innerInnerTestObjectProperty = innerInnerTestObjectProperty;
        }

        public List<InnerInnerTestObject> getInnerInnerTestListProperty() {
            return innerInnerTestListProperty;
        }

        public void setInnerInnerTestListProperty(List<InnerInnerTestObject> innerInnerTestListProperty) {
            this.innerInnerTestListProperty = innerInnerTestListProperty;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InnerTestObject that = (InnerTestObject) o;
            return Objects.equals(stringProperty, that.stringProperty) &&
                    Objects.equals(integerProperty, that.integerProperty) &&
                    Objects.equals(doubleProperty, that.doubleProperty) &&
                    Objects.equals(innerInnerTestObjectProperty, that.innerInnerTestObjectProperty) &&
                    Objects.equals(innerInnerTestListProperty, that.innerInnerTestListProperty);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stringProperty, integerProperty, doubleProperty, innerInnerTestObjectProperty, innerInnerTestListProperty);
        }

        public static class InnerInnerTestObject {
            private String stringProperty;
            private Integer integerProperty;
            private Double doubleProperty;

            public InnerInnerTestObject(
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

            public void setStringProperty(String stringProperty) {
                this.stringProperty = stringProperty;
            }

            public Integer getIntegerProperty() {
                return integerProperty;
            }

            public void setIntegerProperty(Integer integerProperty) {
                this.integerProperty = integerProperty;
            }

            public Double getDoubleProperty() {
                return doubleProperty;
            }

            public void setDoubleProperty(Double doubleProperty) {
                this.doubleProperty = doubleProperty;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                InnerInnerTestObject that = (InnerInnerTestObject) o;
                return Objects.equals(stringProperty, that.stringProperty) &&
                        Objects.equals(integerProperty, that.integerProperty) &&
                        Objects.equals(doubleProperty, that.doubleProperty);
            }

            @Override
            public int hashCode() {
                return Objects.hash(stringProperty, integerProperty, doubleProperty);
            }
        }
    }
}
