package io.github.dimaskama.stickynotes.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dimaskama.stickynotes.client.Note;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class NotesConfig extends JsonConfig<NotesConfig.Data>{
    public static final Codec<Data> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Note.CODEC.listOf().fieldOf("notes").forGetter(d -> d.notes)
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
        // TODO: 05.04.2024 delete
        ArrayList<Note> list = new ArrayList<>();
        list.add(new Note(new Vec3d(0.0, 80.0, 0.0), Text.literal("Name"), Text.literal("Description"), 0, true));
        return new Data(list);
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
        }
    }

    public static class Data {
        public List<Note> notes;

        public Data(List<Note> notes) {
            this.notes = new ArrayList<>(notes);
        }
    }
}
