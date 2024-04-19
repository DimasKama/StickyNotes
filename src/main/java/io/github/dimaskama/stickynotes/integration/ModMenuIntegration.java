package io.github.dimaskama.stickynotes.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import io.github.dimaskama.stickynotes.client.screen.NotesListScreen;
import io.github.dimaskama.stickynotes.client.screen.WorldsNotesScreen;

import java.util.List;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            List<Note> list = StickyNotes.getCurrentWorldNotes();
            return list != null ? new NotesListScreen(parent, list, true) : new WorldsNotesScreen(parent, true);
        };
    }
}
