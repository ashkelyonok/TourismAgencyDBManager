package org.example.entity;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Table {
    private String name;
    private List<Column> columns;
    private List<Row> data;

    public Table(String name) {
        this.name = name;
        this.columns = new ArrayList<>();
        this.data = new ArrayList<>();
    }

    public void addColumn(Column column) {
        this.columns.add(column);
    }
}
