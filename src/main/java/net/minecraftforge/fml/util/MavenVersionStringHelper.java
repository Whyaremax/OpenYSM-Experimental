package net.minecraftforge.fml.util;

public final class MavenVersionStringHelper {
    private MavenVersionStringHelper() {
    }

    public static String artifactVersionToString(Object version) {
        return version == null ? "" : version.toString();
    }
}
