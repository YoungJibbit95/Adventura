package dev.voxelgame.client.viewmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadingScreenViewModelTest {
    @Test
    void loadingPhasesExposeStableProgressAndState() {
        LoadingScreenViewModel boot = LoadingScreenViewModel.boot();
        LoadingScreenViewModel joining = LoadingScreenViewModel.joiningServer("127.0.0.1:25565");
        LoadingScreenViewModel streaming = LoadingScreenViewModel.streamingSpawn(3, 4);

        assertEquals(LoadingScreenViewModel.Phase.BOOT, boot.phase());
        assertTrue(boot.indeterminate());
        assertTrue(joining.cancelAvailable());
        assertEquals(0.75, streaming.progress(), 0.0001);
        assertFalse(streaming.complete());
    }

    @Test
    void loadingProgressIsClampedAndErrorStateIsExplicit() {
        assertEquals(1.0, LoadingScreenViewModel.loadingWorld(2.0).progress(), 0.0001);
        assertEquals(LoadingScreenViewModel.Phase.ERROR, LoadingScreenViewModel.error("network closed").phase());
        assertThrows(IllegalArgumentException.class, () -> new LoadingScreenViewModel(
                LoadingScreenViewModel.Phase.BOOT,
                "Boot",
                "",
                0.0,
                false,
                false,
                true
        ));
    }
}
