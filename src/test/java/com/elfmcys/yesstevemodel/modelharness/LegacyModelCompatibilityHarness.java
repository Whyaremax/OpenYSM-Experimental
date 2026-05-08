package com.elfmcys.yesstevemodel.modelharness;

import com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer;
import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import rip.ysm.legacy.YesModelUtils;
import rip.ysm.security.YsmCrypt;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class LegacyModelCompatibilityHarness {
    private LegacyModelCompatibilityHarness() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path builtinRoot = projectRoot.resolve("src/main/resources/assets/yes_steve_model/builtin");
        Path flatFixture = projectRoot.resolve("build/tmp/legacyModelCompatibilityHarness/default-flat");

        Counts counts = new Counts();
        verifyBuiltinFolders(builtinRoot, counts);
        verifyGeneratedFlatFolderFixture(builtinRoot.resolve("default"), flatFixture, counts);

        for (Path fixtureRoot : fixtureRoots(args)) {
            verifyExternalFixtures(fixtureRoot, counts);
        }

        if (counts.folderModels <= 0) {
            throw new IllegalStateException("No unencrypted folder models were verified");
        }

        System.out.println("Legacy model compatibility harness PASS: builtinFolders=" + counts.folderModels
                + ", generatedFlatFolders=" + counts.generatedFlatFolders
                + ", modernBinaryYsm=" + counts.modernBinaryYsm
                + ", lowBinaryYsm=" + counts.lowBinaryYsm
                + ", legacyArchiveYsm=" + counts.legacyArchiveYsm
                + ", zipFolders=" + counts.zipFolders);
    }

    private static List<Path> fixtureRoots(String[] args) {
        List<Path> roots = new ArrayList<>();
        for (String arg : args) {
            if (arg != null && !arg.isBlank()) {
                roots.add(Path.of(arg).toAbsolutePath().normalize());
            }
        }

        String property = System.getProperty("ysm.compatFixtureDir", "").trim();
        if (!property.isEmpty()) {
            roots.add(Path.of(property).toAbsolutePath().normalize());
        }

        return roots;
    }

    private static void verifyBuiltinFolders(Path root, Counts counts) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Builtin model root not found: " + root);
        }

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root) && YSMFolderDeserializer.isModelFolder(dir)) {
                    verifyFolderModel(dir, counts, false);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void verifyGeneratedFlatFolderFixture(Path source, Path target, Counts counts) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalStateException("Source model not found for flat-folder fixture: " + source);
        }

        deleteRecursively(target);
        copyModelWithoutDescriptor(source, target);
        if (!YSMFolderDeserializer.isModelFolder(target)) {
            throw new IllegalStateException("Generated legacy flat folder was not recognized: " + target);
        }

        RawYsmModel model = verifyFolderModel(target, counts, true);
        if (model.properties.extraAnimations.isEmpty()) {
            throw new IllegalStateException("Generated legacy flat folder did not synthesize extra animation labels");
        }
    }

    private static RawYsmModel verifyFolderModel(Path dir, Counts counts, boolean generatedFlat) {
        try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(dir)) {
            RawYsmModel model = deserializer.deserialize();
            assertModelBasics(model, dir.toString());
            if (generatedFlat) {
                counts.generatedFlatFolders++;
            } else {
                counts.folderModels++;
            }
            return model;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify folder model: " + dir, e);
        }
    }

    private static void verifyExternalFixtures(Path root, Counts counts) throws IOException {
        if (!Files.exists(root)) {
            throw new IllegalStateException("Compatibility fixture path not found: " + root);
        }

        if (Files.isRegularFile(root)) {
            verifyFixtureFile(root, counts);
            return;
        }
        if (YSMFolderDeserializer.isModelFolder(root)) {
            verifyFolderModel(root, counts, false);
            return;
        }

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root) && YSMFolderDeserializer.isModelFolder(dir)) {
                    verifyFolderModel(dir, counts, false);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                verifyFixtureFile(file, counts);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void verifyFixtureFile(Path file, Counts counts) {
        String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".ysm")) {
                verifyYsmFile(file, counts);
            } else if (lower.endsWith(".zip")) {
                try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(file)) {
                    RawYsmModel model = deserializer.deserialize();
                    assertModelBasics(model, file.toString());
                    counts.zipFolders++;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify compatibility fixture: " + file, e);
        }
    }

    private static void verifyYsmFile(Path file, Counts counts) throws Exception {
        byte[] raw = Files.readAllBytes(file);
        try {
            byte[] decrypted = YsmCrypt.decryptYsmFile(raw);
            try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(decrypted)) {
                RawYsmModel model = deserializer.deserializeKeepOpen();
                deserializer.parseYSMFooter(model);
                assertModelBasics(model, file.toString());
                if (model.formatVersion <= 15) {
                    counts.lowBinaryYsm++;
                } else {
                    counts.modernBinaryYsm++;
                }
            }
        } catch (Exception modernError) {
            Map<String, byte[]> legacyFiles = YesModelUtils.input(raw);
            if (legacyFiles.isEmpty()) {
                throw modernError;
            }
            try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(legacyFiles)) {
                RawYsmModel model = deserializer.deserialize();
                assertModelBasics(model, file.toString());
                counts.legacyArchiveYsm++;
            }
        }
    }

    private static void assertModelBasics(RawYsmModel model, String source) {
        if (model == null) {
            throw new IllegalStateException("Deserializer returned null for " + source);
        }
        if (model.mainEntity.mainModel == null) {
            throw new IllegalStateException("Missing main model for " + source);
        }
        if (model.mainEntity.mainModel.bones.isEmpty()) {
            throw new IllegalStateException("Main model has no bones for " + source);
        }
        if (model.mainEntity.textures.isEmpty()) {
            throw new IllegalStateException("Main model has no textures for " + source);
        }
    }

    private static void copyModelWithoutDescriptor(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path src : stream.toList()) {
                Path relative = source.relativize(src);
                if (relative.toString().isEmpty()) {
                    Files.createDirectories(target);
                    continue;
                }
                String normalized = relative.toString().replace('\\', '/');
                if (normalized.equals("ysm.json")) {
                    continue;
                }

                Path dest = target.resolve(relative.toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        List<Path> paths;
        try (Stream<Path> stream = Files.walk(path)) {
            paths = new ArrayList<>(stream.toList());
        }
        Collections.reverse(paths);
        for (Path item : paths) {
            Files.deleteIfExists(item);
        }
    }

    private static final class Counts {
        int folderModels;
        int generatedFlatFolders;
        int modernBinaryYsm;
        int lowBinaryYsm;
        int legacyArchiveYsm;
        int zipFolders;
    }
}
