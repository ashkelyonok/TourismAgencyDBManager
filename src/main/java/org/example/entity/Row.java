package org.example.entity;

import lombok.Getter;
import lombok.Setter;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class Row {
    private final Map<String, Object> values;

    public Row() {
        this.values = new LinkedHashMap<>();
    }

    public void setValue(String column, Object value) {
        values.put(column, value);
    }

    public Object getValue(String column) {
        return values.get(column);
    }
}
