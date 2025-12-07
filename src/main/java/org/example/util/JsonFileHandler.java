package org.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonFileHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Для красивого форматирования JSON
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static <T> void writeToFile(String filename, List<T> data) throws IOException {
        File file = new File(filename);
        mapper.writeValue(file, data);
    }

    public static <T> List<T> readFromFile(String filename, Class<T> type) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        return mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(List.class, type));
    }
}