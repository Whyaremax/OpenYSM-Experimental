package com.elfmcys.yesstevemodel.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class LegacyFolderManifestBuilder {
    private static final AnimationFile[] PLAYER_ANIMATIONS = {
            new AnimationFile("main", "main.animation.json"),
            new AnimationFile("arm", "arm.animation.json"),
            new AnimationFile("extra", "extra.animation.json"),
            new AnimationFile("tac", "tac.animation.json"),
            new AnimationFile("carryon", "carryon.animation.json"),
            new AnimationFile("parcool", "parcool.animation.json"),
            new AnimationFile("swem", "swem.animation.json"),
            new AnimationFile("slashblade", "slashblade.animation.json"),
            new AnimationFile("tlm", "tlm.animation.json"),
            new AnimationFile("immersive_melodies", "im.animation.json"),
            new AnimationFile("irons_spell_books", "iss.animation.json"),
            new AnimationFile("fp_arm", "fp.arm.animation.json")
    };

    private final Resources resources;

    private LegacyFolderManifestBuilder(Resources resources) {
        this.resources = resources;
    }

    static JsonObject build(Resources resources) {
        return new LegacyFolderManifestBuilder(resources).build();
    }

    private JsonObject build() {
        String mainModel = firstExisting("main.json", "models/main.json");
        if (mainModel == null) return null;

        JsonObject root = new JsonObject();
        root.addProperty("spec", 2);
        root.add("metadata", buildMetadata());

        JsonObject properties = new JsonObject();
        properties.addProperty("height_scale", 0.7);
        properties.addProperty("width_scale", 0.7);
        properties.addProperty("free", true);
        properties.addProperty("merge_multiline_expr", true);
        addExtraAnimationLabels(properties);

        JsonArray textures = new JsonArray();
        for (String texture : listTextures()) {
            textures.add(texture);
        }
        if (!textures.isEmpty()) {
            properties.addProperty("default_texture", extractFileName(textures.get(0).getAsString()));
        }
        root.add("properties", properties);

        JsonObject files = new JsonObject();
        JsonObject player = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("main", mainModel);
        String armModel = firstExisting("arm.json", "models/arm.json");
        if (armModel != null) model.addProperty("arm", armModel);
        player.add("model", model);

        JsonObject animations = new JsonObject();
        for (AnimationFile animation : PLAYER_ANIMATIONS) {
            addAnimation(animations, animation.key(), animation.fileName());
        }
        if (!animations.entrySet().isEmpty()) player.add("animation", animations);

        player.add("texture", textures);
        files.add("player", player);
        root.add("files", files);
        return root;
    }

    private JsonObject buildMetadata() {
        JsonObject metadata = new JsonObject();
        byte[] infoBytes = resources.read("info.json");
        if (infoBytes == null) {
            metadata.addProperty("name", "");
            return metadata;
        }
        try {
            JsonObject info = JsonParser.parseString(new String(infoBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            metadata.addProperty("name", firstString(info, "name", "model_name", "display_name", "id"));
            metadata.addProperty("tips", firstString(info, "tips", "description", "desc"));

            JsonElement author = firstElement(info, "author", "authors", "creator");
            if (author != null) {
                metadata.add("authors", normalizeAuthors(author));
            }
        } catch (Exception ignored) {
            metadata.addProperty("name", "");
        }
        return metadata;
    }

    private JsonArray normalizeAuthors(JsonElement author) {
        JsonArray authors = new JsonArray();
        if (author.isJsonArray()) {
            for (JsonElement item : author.getAsJsonArray()) {
                authors.add(authorObject(item));
            }
            return authors;
        }
        authors.add(authorObject(author));
        return authors;
    }

    private JsonObject authorObject(JsonElement author) {
        JsonObject authorObj = new JsonObject();
        authorObj.addProperty("name", author.isJsonObject() ? firstString(author.getAsJsonObject(), "name", "author") : author.getAsString());
        return authorObj;
    }

    private void addAnimation(JsonObject animations, String key, String fileName) {
        String path = firstExisting(fileName, "animations/" + fileName);
        if (path != null) animations.addProperty(key, path);
    }

    private void addExtraAnimationLabels(JsonObject properties) {
        String extraAnimationPath = firstExisting("extra.animation.json", "animations/extra.animation.json");
        if (extraAnimationPath == null) return;

        byte[] animData = resources.read(extraAnimationPath);
        if (animData == null) return;

        try {
            JsonObject animationRoot = JsonParser.parseString(new String(animData, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject animations = animationRoot.getAsJsonObject("animations");
            if (animations == null) return;

            JsonObject labels = new JsonObject();
            for (String animationName : animations.keySet()) {
                if (animationName == null || animationName.isBlank()) continue;
                labels.addProperty(animationName, readableAnimationName(animationName));
            }
            if (!labels.entrySet().isEmpty()) {
                properties.add("extra_animation", labels);
            }
        } catch (Exception ignored) {
        }
    }

    private String firstExisting(String... paths) {
        for (String path : paths) {
            String normalized = normalizePath(path);
            if (resources.exists(normalized)) return normalized;
        }
        return null;
    }

    private List<String> listTextures() {
        List<String> result = new ArrayList<>();
        for (String path : resources.listPaths()) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (!isImagePath(lower)) continue;
            if (lower.contains("/") && !lower.startsWith("textures/")) continue;
            if (lower.equals("ysm-pack.png") || lower.equals("icon.png")) continue;
            result.add(path);
        }
        result.sort(String::compareTo);
        return result;
    }

    private static boolean isImagePath(String path) {
        return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".bmp") || path.endsWith(".webp") || path.endsWith(".avif");
    }

    private static String readableAnimationName(String animationName) {
        int dotIndex = animationName.lastIndexOf('.');
        String label = dotIndex >= 0 && dotIndex + 1 < animationName.length()
                ? animationName.substring(dotIndex + 1)
                : animationName;
        label = label.replace('_', ' ').replace('-', ' ').trim();
        if (label.isEmpty()) return animationName;

        StringBuilder builder = new StringBuilder(label.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < label.length(); i++) {
            char ch = label.charAt(i);
            if (Character.isWhitespace(ch)) {
                builder.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(ch));
                capitalizeNext = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String extractFileName(String fullPath) {
        String name = fullPath;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) name = name.substring(lastSlash + 1);
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx >= 0) name = name.substring(0, dotIdx);
        return name;
    }

    private static String firstString(JsonObject obj, String... keys) {
        JsonElement element = firstElement(obj, keys);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static JsonElement firstElement(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key)) return obj.get(key);
        }
        return null;
    }

    static String normalizePath(String path) {
        path = path.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    interface Resources {
        byte[] read(String relativePath);

        boolean exists(String relativePath);

        List<String> listPaths();
    }

    private record AnimationFile(String key, String fileName) {
    }
}
