package net.minecraftforge.forgespi.locating;

import java.nio.file.Path;

public interface IModFile {
    SecureJar getSecureJar();

    final class SecureJar {
        private final Path rootPath;

        public SecureJar(Path rootPath) {
            this.rootPath = rootPath;
        }

        public Path getRootPath() {
            return rootPath;
        }
    }
}
