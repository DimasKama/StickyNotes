package io.github.dimaskama.stickynotes.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dimaskama.stickynotes.client.Note;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.Map;

public class NotesConfig extends JsonConfig<NotesConfig.Data>{
    public static final Codec<Data> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, Note.CODEC.listOf()).fieldOf("world_to_map").forGetter(d -> Multimaps.asMap(d.worldToNotes))
            ).apply(instance, Data::new)
    );
    private boolean dirty;

    public NotesConfig(String path) {
        super(path);
    }

    @Override
    protected Codec<Data> getCodec() {
        return CODEC;
    }

    @Override
    protected Data createDefaultData() {
        return new Data(Map.of());
    }

    public void markDirty() {
        dirty = true;
    }

    public void onClientStopping(MinecraftClient client) {
        saveIfDirty(false);
    }

    public void saveIfDirty(boolean log) {
        if (dirty) {
            save(log);
            dirty = false;
        }
    }

    public static class Data {
        public ArrayListMultimap<String, Note> worldToNotes;

        public Data(Map<String, List<Note>> map) {
            worldToNotes = ArrayListMultimap.create();
            map.forEach((key, value) -> worldToNotes.putAll(key, value));
        }
    }
}
