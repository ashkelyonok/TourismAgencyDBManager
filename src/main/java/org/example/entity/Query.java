package org.example.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Query {
    private String name;
    private String query;
    private String description;

    public Query() {
        this.description = "";
    }

    public Query(@JsonProperty("name") String name,
                 @JsonProperty("query") String query) {
        this.name = name;
        this.query = query;
        this.description = "";
    }

    //must be for jackson
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }
}
