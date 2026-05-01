package dev.voxelgame.server.save;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

final class SaveFiles {
    private SaveFiles() {
    }

    static void writePropertiesAtomically(Path path, Properties properties, String comment) throws IOException {
        Path target = path.toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = Files.createTempFile(parent, tempPrefix(target), ".tmp");
        boolean moved = false;
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                properties.store(out, comment);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    private static String tempPrefix(Path target) {
        String fileName = target.getFileName().toString();
        return fileName.length() >= 3 ? fileName + "." : "save-" + fileName + ".";
    }
}
