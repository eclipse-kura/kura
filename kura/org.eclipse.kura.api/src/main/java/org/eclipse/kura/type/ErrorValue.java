package org.eclipse.kura.type;

public class ErrorValue implements TypedValue<String> {

    private final String value;

    public ErrorValue(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(TypedValue<String> o) {
        return value.compareTo(o.getValue());
    }

    @Override
    public DataType getType() {
        return DataType.ERROR;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ErrorValue)) {
            return false;
        }
        ErrorValue other = (ErrorValue) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
}
