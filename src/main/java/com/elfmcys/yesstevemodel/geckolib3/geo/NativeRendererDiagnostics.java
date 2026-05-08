package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.NativeModelCache;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NativeRendererDiagnostics {
    private NativeRendererDiagnostics() {
    }

    public static String getStatusSummary() {
        return NativeModelRenderer.getStats().toDisplayString();
    }

    public static ParityReport runParityCheck(GeoModel mesh) {
        if (mesh == null) {
            return ParityReport.failed("no model was selected", 0, 0);
        }
        if (mesh.bakedBones == null || mesh.bakedBones.isEmpty()) {
            return ParityReport.failed("model has no baked bones", 0, 0);
        }
        if (!NativeLibLoader.isLoaded()) {
            return ParityReport.failed("native library is not loaded: " + NativeLibLoader.getFallbackReason(), 0, 0);
        }

        NativeModelCache cache = mesh.getOrBuildNativeCache();
        if (cache == null || !cache.isReady()) {
            String reason = cache == null ? "model has no native cache" : cache.failureReason();
            return ParityReport.failed(reason, 0, 0);
        }

        float[] boneParams = mesh.getBoneTransformData();
        int requiredBoneFloats = mesh.bakedBones.size() * 12;
        if (boneParams == null || boneParams.length < requiredBoneFloats) {
            return ParityReport.failed("animation buffer is too small", 0, 0);
        }

        try {
            List<ParityVertex> javaVertices = collectJavaVertices(mesh, boneParams, cache.vertexCapacity());
            List<QuadInfo> javaQuads = collectJavaQuads(mesh, boneParams);
            List<QuadInfo> javaVisibleQuads = javaQuads.stream().filter(QuadInfo::visible).toList();
            ByteBuffer nativeOut = ByteBuffer
                    .allocateDirect(Math.max(cache.vertexCapacity() * 14 * Float.BYTES, 64))
                    .order(ByteOrder.nativeOrder());
            ByteBuffer matrixBuffer = ByteBuffer.allocateDirect(64 * Float.BYTES).order(ByteOrder.nativeOrder());
            ByteBuffer animBuffer = ByteBuffer.allocateDirect(boneParams.length * Float.BYTES).order(ByteOrder.nativeOrder());
            fillIdentityMatrixBuffer(matrixBuffer.asFloatBuffer());
            animBuffer.asFloatBuffer().put(boneParams);

            int nativeCount = NativeModelCache.nComputeModelVertices(
                    cache.handle(),
                    nativeOut,
                    matrixBuffer,
                    animBuffer,
                    0,
                    0,
                    0,
                    1.0f, 1.0f, 1.0f, 1.0f,
                    false
            );
            if (nativeCount < 0) {
                return ParityReport.failed("native renderer returned error " + nativeCount, javaVertices.size(), 0);
            }
            if (nativeCount > cache.vertexCapacity()) {
                return ParityReport.failed("native renderer exceeded cache capacity", javaVertices.size(), nativeCount);
            }
            List<Integer> nativeQuadIds = collectNativeVisibleQuadIds(cache, matrixBuffer, animBuffer);
            return compareParityVertices(javaVertices, nativeOut.asFloatBuffer(), nativeCount, javaVisibleQuads, javaQuads, nativeQuadIds);
        } catch (Throwable throwable) {
            return ParityReport.failed("parity check threw: " + throwable.getMessage(), 0, 0);
        }
    }

    private static List<ParityVertex> collectJavaVertices(GeoModel mesh, float[] boneParams, int expectedVertices) {
        List<ParityVertex> vertices = new ArrayList<>(Math.max(expectedVertices, 0));
        NativeModelRenderer.visitModelVertices(
                mesh,
                boneParams,
                new Matrix4f(),
                new Matrix3f(),
                new Matrix4f(),
                0,
                0,
                (x, y, z, u, v, nx, ny, nz, light) -> vertices.add(new ParityVertex(x, y, z, u, v, nx, ny, nz))
        );
        return vertices;
    }

    private static List<Integer> collectNativeVisibleQuadIds(NativeModelCache cache, ByteBuffer matrixBuffer, ByteBuffer animBuffer) {
        ByteBuffer quadIdBuffer = ByteBuffer
                .allocateDirect(Math.max(cache.vertexCapacity() / 4, 1) * Integer.BYTES)
                .order(ByteOrder.nativeOrder());
        int count = NativeModelCache.nCollectVisibleQuadIds(cache.handle(), quadIdBuffer, matrixBuffer, animBuffer, 0);
        if (count < 0) {
            List<Integer> error = new ArrayList<>(1);
            error.add(count);
            return error;
        }
        List<Integer> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(quadIdBuffer.getInt(i * Integer.BYTES));
        }
        return ids;
    }

    private static void fillIdentityMatrixBuffer(FloatBuffer buffer) {
        buffer.clear();
        for (int i = 0; i < 64; i++) {
            buffer.put(i, 0.0f);
        }
        Matrix4f identity4 = new Matrix4f();
        Matrix3f identity3 = new Matrix3f();
        identity4.get(0, buffer);
        identity3.get(16, buffer);
        identity4.get(32, buffer);
    }

    private static ParityReport compareParityVertices(List<ParityVertex> javaVertices, FloatBuffer nativeVertices, int nativeCount, List<QuadInfo> javaVisibleQuads, List<QuadInfo> javaQuads, List<Integer> nativeQuadIds) {
        if (javaVertices.size() != nativeCount) {
            return new ParityReport(false, "vertex count mismatch", javaVertices.size(), nativeCount, 0.0, 0.0, checksum(javaVertices), checksum(nativeVertices, nativeCount), NativeLibLoader.getStatusSummary(), firstMismatchDetail(javaVertices, nativeVertices, nativeCount) + ", " + firstQuadMismatchDetail(javaVisibleQuads, javaQuads, nativeQuadIds));
        }

        double maxPositionDelta = 0.0;
        double maxNormalDelta = 0.0;
        String firstMismatch = "";
        for (int i = 0; i < nativeCount; i++) {
            ParityVertex javaVertex = javaVertices.get(i);
            ParityVertex nativeVertex = nativeVertex(nativeVertices, i);

            if (!isFinite(nativeVertex.x, nativeVertex.y, nativeVertex.z, nativeVertex.u, nativeVertex.v, nativeVertex.nx, nativeVertex.ny, nativeVertex.nz)) {
                return new ParityReport(false, "native output contains NaN or Infinity", javaVertices.size(), nativeCount, maxPositionDelta, maxNormalDelta, checksum(javaVertices), checksum(nativeVertices, nativeCount), NativeLibLoader.getStatusSummary(), "index=" + i + ", native=" + nativeVertex.toShortString());
            }

            double positionDelta = maxDelta(javaVertex.x, javaVertex.y, javaVertex.z, nativeVertex.x, nativeVertex.y, nativeVertex.z);
            double normalDelta = maxDelta(javaVertex.nx, javaVertex.ny, javaVertex.nz, nativeVertex.nx, nativeVertex.ny, nativeVertex.nz);
            double uvDelta = Math.max(Math.abs(javaVertex.u - nativeVertex.u), Math.abs(javaVertex.v - nativeVertex.v));
            if (firstMismatch.isEmpty() && Math.max(Math.max(positionDelta, normalDelta), uvDelta) > 0.0005) {
                firstMismatch = "index=" + i + ", java=" + javaVertex.toShortString() + ", native=" + nativeVertex.toShortString();
            }
            maxPositionDelta = Math.max(maxPositionDelta, positionDelta);
            maxPositionDelta = Math.max(maxPositionDelta, uvDelta);
            maxNormalDelta = Math.max(maxNormalDelta, normalDelta);
        }

        boolean passed = maxPositionDelta <= 0.0005 && maxNormalDelta <= 0.0005;
        String reason = passed ? "java/native vertex parity passed" : "java/native vertex delta exceeded tolerance";
        return new ParityReport(passed, reason, javaVertices.size(), nativeCount, maxPositionDelta, maxNormalDelta, checksum(javaVertices), checksum(nativeVertices, nativeCount), NativeLibLoader.getStatusSummary(), firstMismatch);
    }

    private static String firstQuadMismatchDetail(List<QuadInfo> javaVisibleQuads, List<QuadInfo> javaQuads, List<Integer> nativeQuadIds) {
        if (!nativeQuadIds.isEmpty() && nativeQuadIds.get(0) < 0) {
            return "nativeQuadIdsError=" + nativeQuadIds.get(0);
        }
        int sharedCount = Math.min(javaVisibleQuads.size(), nativeQuadIds.size());
        for (int i = 0; i < sharedCount; i++) {
            int javaId = javaVisibleQuads.get(i).id;
            int nativeId = nativeQuadIds.get(i);
            if (javaId != nativeId) {
                return "quadMismatch=" + i + ", javaQuad=" + javaVisibleQuads.get(i).toShortString() + ", nativeQuad=" + describeQuad(javaQuads, nativeId);
            }
        }
        if (nativeQuadIds.size() > javaVisibleQuads.size() && sharedCount < nativeQuadIds.size()) {
            return "nativeExtraQuadAt=" + sharedCount + ", nativeQuad=" + describeQuad(javaQuads, nativeQuadIds.get(sharedCount));
        }
        if (javaVisibleQuads.size() > nativeQuadIds.size() && sharedCount < javaVisibleQuads.size()) {
            return "javaExtraQuadAt=" + sharedCount + ", javaQuad=" + javaVisibleQuads.get(sharedCount).toShortString();
        }
        return "visibleQuadPrefixMatches";
    }

    private static String describeQuad(List<QuadInfo> javaQuads, int id) {
        for (QuadInfo quad : javaQuads) {
            if (quad.id == id) return quad.toShortString();
        }
        return "id=" + id + " (not visible in Java)";
    }

    private static List<QuadInfo> collectJavaQuads(GeoModel mesh, float[] boneParams) {
        List<QuadInfo> quads = new ArrayList<>();
        Matrix4f identityMat = new Matrix4f();
        Matrix4f globalBoneMat = new Matrix4f();
        Matrix4f projBoneMat = new Matrix4f();
        Matrix4f projMat = new Matrix4f();
        Matrix4f[] boneLocalTransforms = new Matrix4f[mesh.bakedBones.size()];
        boolean[] boneVisible = new boolean[mesh.bakedBones.size()];
        Vector4f p1 = new Vector4f();
        Vector4f p2 = new Vector4f();
        Vector4f p3 = new Vector4f();

        for (int i = 0; i < mesh.bakedBones.size(); i++) {
            NativeModelRenderer.calculateBoneMatrix(i, mesh.bakedBones, boneParams, boneLocalTransforms, boneVisible, identityMat);
        }

        int quadId = 0;
        for (int boneIndex = 0; boneIndex < mesh.bakedBones.size(); boneIndex++) {
            GeoModel.BakedBone bone = mesh.bakedBones.get(boneIndex);
            for (int cubeIndex = 0; cubeIndex < bone.cubes.size(); cubeIndex++) {
                GeoModel.BakedCube cube = bone.cubes.get(cubeIndex);
                for (int quadIndex = 0; quadIndex < cube.quads.size(); quadIndex++) {
                    GeoModel.BakedQuad quad = cube.quads.get(quadIndex);
                    if (boneVisible[boneIndex]) {
                        globalBoneMat.set(boneLocalTransforms[boneIndex]);
                        projBoneMat.set(projMat).mul(globalBoneMat);
                        p1.set(quad.positions[0].x(), quad.positions[0].y(), quad.positions[0].z(), 1.0f).mul(projBoneMat);
                        p2.set(quad.positions[1].x(), quad.positions[1].y(), quad.positions[1].z(), 1.0f).mul(projBoneMat);
                        p3.set(quad.positions[2].x(), quad.positions[2].y(), quad.positions[2].z(), 1.0f).mul(projBoneMat);
                        float det = p1.x() * (p2.y() * p3.w() - p3.y() * p2.w()) - p2.x() * (p1.y() * p3.w() - p3.y() * p1.w()) + p3.x() * (p1.y() * p2.w() - p2.y() * p1.w());
                        boolean visible = !cube.cullable || det > NativeModelRenderer.CULL_DETERMINANT_EPSILON;
                        quads.add(new QuadInfo(quadId, boneIndex, bone.name, cubeIndex, quadIndex, cube.cullable, visible, det, new Vector3f(quad.normal), new Vector3f(quad.positions[0]), quad.uvs[0].x(), quad.uvs[0].y()));
                    }
                    quadId++;
                }
            }
        }
        return quads;
    }

    private static String firstMismatchDetail(List<ParityVertex> javaVertices, FloatBuffer nativeVertices, int nativeCount) {
        int sharedCount = Math.min(javaVertices.size(), nativeCount);
        for (int i = 0; i < sharedCount; i++) {
            ParityVertex javaVertex = javaVertices.get(i);
            ParityVertex nativeVertex = nativeVertex(nativeVertices, i);
            double positionDelta = maxDelta(javaVertex.x, javaVertex.y, javaVertex.z, nativeVertex.x, nativeVertex.y, nativeVertex.z);
            double normalDelta = maxDelta(javaVertex.nx, javaVertex.ny, javaVertex.nz, nativeVertex.nx, nativeVertex.ny, nativeVertex.nz);
            double uvDelta = Math.max(Math.abs(javaVertex.u - nativeVertex.u), Math.abs(javaVertex.v - nativeVertex.v));
            if (Math.max(Math.max(positionDelta, normalDelta), uvDelta) > 0.0005) {
                return "firstMismatch=" + i + ", java=" + javaVertex.toShortString() + ", native=" + nativeVertex.toShortString();
            }
        }
        if (nativeCount > javaVertices.size() && sharedCount < nativeCount) {
            return "native has extra vertices starting at " + sharedCount + ", native=" + nativeVertex(nativeVertices, sharedCount).toShortString();
        }
        if (javaVertices.size() > nativeCount && sharedCount < javaVertices.size()) {
            return "java has extra vertices starting at " + sharedCount + ", java=" + javaVertices.get(sharedCount).toShortString();
        }
        return "shared vertex prefix matches";
    }

    private static ParityVertex nativeVertex(FloatBuffer nativeVertices, int index) {
        int base = index * 14;
        return new ParityVertex(
                nativeVertices.get(base),
                nativeVertices.get(base + 1),
                nativeVertices.get(base + 2),
                nativeVertices.get(base + 7),
                nativeVertices.get(base + 8),
                nativeVertices.get(base + 11),
                nativeVertices.get(base + 12),
                nativeVertices.get(base + 13)
        );
    }

    private static boolean isFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) return false;
        }
        return true;
    }

    private static double maxDelta(float ax, float ay, float az, float bx, float by, float bz) {
        return Math.max(Math.max(Math.abs(ax - bx), Math.abs(ay - by)), Math.abs(az - bz));
    }

    private static long checksum(List<ParityVertex> vertices) {
        long hash = 0xcbf29ce484222325L;
        for (ParityVertex vertex : vertices) {
            hash = mix(hash, vertex.x);
            hash = mix(hash, vertex.y);
            hash = mix(hash, vertex.z);
            hash = mix(hash, vertex.u);
            hash = mix(hash, vertex.v);
            hash = mix(hash, vertex.nx);
            hash = mix(hash, vertex.ny);
            hash = mix(hash, vertex.nz);
        }
        return hash;
    }

    private static long checksum(FloatBuffer vertices, int count) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < count; i++) {
            int base = i * 14;
            hash = mix(hash, vertices.get(base));
            hash = mix(hash, vertices.get(base + 1));
            hash = mix(hash, vertices.get(base + 2));
            hash = mix(hash, vertices.get(base + 7));
            hash = mix(hash, vertices.get(base + 8));
            hash = mix(hash, vertices.get(base + 11));
            hash = mix(hash, vertices.get(base + 12));
            hash = mix(hash, vertices.get(base + 13));
        }
        return hash;
    }

    private static long mix(long hash, float value) {
        int quantized = Math.round(value * 100000.0f);
        hash ^= quantized;
        return hash * 0x100000001b3L;
    }

    private record ParityVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        String toShortString() {
            return String.format(Locale.ROOT, "pos=(%.4f,%.4f,%.4f), uv=(%.4f,%.4f), normal=(%.4f,%.4f,%.4f)", x, y, z, u, v, nx, ny, nz);
        }
    }

    private record QuadInfo(int id, int boneIndex, String boneName, int cubeIndex, int quadIndex, boolean cullable, boolean visible, float det, Vector3f normal, Vector3f firstPosition, float firstU, float firstV) {
        String toShortString() {
            return String.format(Locale.ROOT, "id=%d, bone=%d/%s, cube=%d, quad=%d, cullable=%s, visible=%s, javaDet=%.8f, pos0=(%.4f,%.4f,%.4f), uv0=(%.4f,%.4f), normal=(%.4f,%.4f,%.4f)",
                    id,
                    boneIndex,
                    boneName,
                    cubeIndex,
                    quadIndex,
                    cullable,
                    visible,
                    det,
                    firstPosition.x(),
                    firstPosition.y(),
                    firstPosition.z(),
                    firstU,
                    firstV,
                    normal.x(),
                    normal.y(),
                    normal.z());
        }
    }

    public record ParityReport(boolean passed,
                               String reason,
                               int javaVertices,
                               int nativeVertices,
                               double maxPositionDelta,
                               double maxNormalDelta,
                               long javaChecksum,
                               long nativeChecksum,
                               String nativeStatus,
                               String detail) {
        private static ParityReport failed(String reason, int javaVertices, int nativeVertices) {
            return new ParityReport(false, reason, javaVertices, nativeVertices, 0.0, 0.0, 0L, 0L, NativeLibLoader.getStatusSummary(), "");
        }

        public String toDisplayString() {
            return "native parity: " + (passed ? "PASS" : "FAIL")
                    + ", reason=" + reason
                    + ", javaVertices=" + javaVertices
                    + ", nativeVertices=" + nativeVertices
                    + ", maxPosDelta=" + String.format(Locale.ROOT, "%.6f", maxPositionDelta)
                    + ", maxNormalDelta=" + String.format(Locale.ROOT, "%.6f", maxNormalDelta)
                    + checksumSummary()
                    + (detail == null || detail.isBlank() ? "" : ", detail=" + detail);
        }

        private String checksumSummary() {
            if (passed && javaChecksum != nativeChecksum) return "";
            return ", javaChecksum=" + Long.toUnsignedString(javaChecksum, 16)
                    + ", nativeChecksum=" + Long.toUnsignedString(nativeChecksum, 16);
        }
    }
}
