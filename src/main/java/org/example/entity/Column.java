package org.example.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Column {
    private String name;
    private String type;
    private boolean primaryKey;
    private boolean nullable;
    private String defaultValue;
    private String foreignKeyTable;
    private String foreignKeyColumn;

    public Column(String name, String type) {
        this.name = name;
        this.type = type;
        this.primaryKey = false;
        this.nullable = true;
        this.defaultValue = null;
        this.foreignKeyTable = null;
        this.foreignKeyColumn = null;
    }
}
