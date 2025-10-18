package io.github.dimaskama.stickynotes.integration;

import io.github.dimaskama.stickynotes.client.NotesManager;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

public class IrisIntegration {

    public static final boolean IS_IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");

    public static void init() {
        if (IS_IRIS_LOADED) {
            Internal.init();
        }
    }

    private static class Internal {

        private static void init() {
            IrisApi.getInstance().assignPipeline(NotesManager.RENDER_PIPELINE, IrisProgram.TEXTURED);
            IrisApi.getInstance().assignPipeline(NotesManager.RENDER_PIPELINE_SEE_THROUGH, IrisProgram.TEXTURED);
        }

    }

}
