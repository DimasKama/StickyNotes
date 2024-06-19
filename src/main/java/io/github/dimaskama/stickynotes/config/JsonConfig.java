package io.github.dimaskama.stickynotes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.dimaskama.stickynotes.client.StickyNotes;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class JsonConfig<D> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final transient String path;
    private final D data;

    public JsonConfig(String path) {
        this.path = path;
        data = createDefaultData();
    }

    protected abstract Codec<D> getCodec();

    protected abstract D createDefaultData();

    public String getPath() {
        return path;
    }

    public D getData() {
        return data;
    }

    public void loadOrCreate() {
        File file = new File(getPath());
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!(parent.exists() || parent.mkdirs())) {
                StickyNotes.LOGGER.warn("Can't create config: " + file.getAbsolutePath());
                return;
            }
            try {
                saveWithoutCatch();
            } catch (IOException e) {
                StickyNotes.LOGGER.warn("Exception occurred while writing new config. ", e);
            }
        } else {
            load(file);
        }
    }

    private void load(File file) {
        try (FileReader f = new FileReader(file)) {
            deserialize(JsonParser.parseReader(f));
        } catch (Exception e) {
            StickyNotes.LOGGER.warn("Exception occurred while reading config. ", e);
        }
    }

    protected void deserialize(JsonElement element) {
        D d = getCodec().decode(JsonOps.INSTANCE, element).getOrThrow().getFirst();
        for (Field field : d.getClass().getDeclaredFields()) {
            try {
                if (isAcceptedModifiers(field.getModifiers())) {
                    field.set(data, field.get(d));
                }
            } catch (IllegalAccessException ignored) {}
        }
    }

    public void save() {
        save(true);
    }

    public void save(boolean log) {
        try {
            saveWithoutCatch();
            if (log) StickyNotes.LOGGER.info("Config saved: " + getPath());
        } catch (IOException e) {
            StickyNotes.LOGGER.warn("Exception occurred while saving config. ", e);
        }
    }

    public void saveWithoutCatch() throws IOException {
        try (FileWriter w = new FileWriter(getPath())) {
            GSON.toJson(serialize(), w);
        }
    }

    protected JsonElement serialize() {
        return getCodec()
                .encode(data, JsonOps.INSTANCE, JsonOps.INSTANCE.empty())
                .getOrThrow();
    }

    public void reset() {
        D newData = createDefaultData();
        for (Field field : newData.getClass().getDeclaredFields()) {
            if (isAcceptedModifiers(field.getModifiers())) {
                try {
                    field.set(data, field.get(newData));
                } catch (IllegalAccessException ignored) {}
            }
        }
    }

    public static boolean isAcceptedModifiers(int mod) {
        return (mod & (Modifier.FINAL | Modifier.TRANSIENT)) == 0 && (mod & Modifier.PUBLIC) != 0;
    }
}
