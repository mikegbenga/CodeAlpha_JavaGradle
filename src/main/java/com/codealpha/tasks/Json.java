package com.codealpha.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Instant;

/** Gson setup in one place. Gson is the project's one external runtime dependency. */
final class Json {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .disableHtmlEscaping()
            .create();

    private Json() {
    }

    static String write(Object value) {
        return GSON.toJson(value);
    }

    static <T> T read(String json, Class<T> type) {
        try {
            T value = GSON.fromJson(json, type);
            if (value == null) {
                throw new IllegalArgumentException("request body is empty");
            }
            return value;
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("request body is not valid JSON");
        }
    }

    private static final class InstantAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            return Instant.parse(in.nextString());
        }
    }
}
