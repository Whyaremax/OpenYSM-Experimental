package com.elfmcys.yesstevemodel.nativeharness;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.NativeModelCache;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class NativeRendererHarness {
    private static final float EPSILON = 0.0005f;
    private static Method initModelCache;
    private static Method destroyModelCache;

    private NativeRendererHarness() {
    }

    public static void main(String[] args) throws Exception {
        Path nativeLibrary = args.length > 0 ? Path.of(args[0]) : defaultNativeLibrary();
        if (!Files.isRegularFile(nativeLibrary)) {
            throw new IllegalStateException("Native library not found: " + nativeLibrary);
        }

        System.load(nativeLibrary.toAbsolutePath().toString());
        initModelCache = NativeModelCache.class.getDeclaredMethod("nInitModelCache", ByteBuffer.class);
        destroyModelCache = NativeModelCache.class.getDeclaredMethod("nDestroyModelCache", long.class);
        initModelCache.setAccessible(true);
        destroyModelCache.setAccessible(true);

        int abi = NativeModelCache.nGetNativeAbiVersion();
        if (abi != 1) throw new IllegalStateException("Unexpected native ABI: " + abi);
        if (!NativeModelCache.nSelfTest(ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()))) {
            throw new IllegalStateException("Native self-test failed");
        }

        runVisibleQuadTest();
        runParentChildBoneTest();
        runRenderPartMaskTest();
        runPackedOutputTest();
        runGlowLightTest();
        runCullTest(false, 4);
        runCullTest(true, 0);
        runProjectionCullWithNonIdentityProjectionTest();
        runNearZeroCullTest();
        runHiddenBoneTest();
        runAnimatedTransformTest();
        runNonUniformNormalTest();
        runSyntheticBenchmarkIfEnabled();

        System.out.println("Native renderer harness PASS: backend=" + NativeModelCache.nGetNativeBackendName()
                + ", lib=" + nativeLibrary.toAbsolutePath());
    }

    private static Path defaultNativeLibrary() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String folder = arch.contains("64") ? "windows-x64" : "windows-x86";
            return Path.of("src/generated/resources/META-INF/native", folder, "ysm-core.dll");
        }
        if (os.contains("linux")) {
            return Path.of("src/generated/resources/META-INF/native/linux-x64/libysm-core.so");
        }
        if (os.contains("mac")) {
            String folder = arch.contains("aarch64") || arch.contains("arm64") ? "macos-arm64" : "macos-x64";
            return Path.of("src/generated/resources/META-INF/native", folder, "libysm-core.dylib");
        }
        throw new IllegalStateException("Unsupported harness platform: " + os + " " + arch);
    }

    private static void runVisibleQuadTest() throws Exception {
        float[][] positions = {
                {0.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 0.0f},
                {0.0f, 1.0f, 0.0f}
        };
        ByteBuffer out = compute(singleQuadModel(false, positions, 0.0f, 0.0f, 0.0f), identityMatrices(), defaultAnim(), 4);
        assertVertex(out.asFloatBuffer(), 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        assertVertex(out.asFloatBuffer(), 2, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
    }

    private static void runParentChildBoneTest() throws Exception {
        float rootPivotX = 0.0f;
        float rootPivotY = 0.0f;
        float rootPivotZ = 0.0f;
        float childPivotX = 3.0f;
        float childPivotY = -2.0f;
        float childPivotZ = 1.0f;

        float[] anim = defaultAnim(2);
        anim[2] = 0.35f;
        anim[3] = -4.0f;
        anim[4] = 2.0f;
        anim[12] = -0.2f;
        anim[13] = 0.15f;
        anim[14] = 0.4f;
        anim[15] = 1.0f;
        anim[16] = -3.0f;
        anim[17] = 2.0f;
        anim[18] = 1.2f;
        anim[19] = 0.9f;
        anim[20] = 1.1f;

        ByteBuffer model = model(
                new BoneSpec(-1, 0, false, rootPivotX, rootPivotY, rootPivotZ, false),
                new BoneSpec(0, 0, false, childPivotX, childPivotY, childPivotZ, false, new QuadSpec(unitQuad(), new Vec3(0.0f, 0.0f, 1.0f)))
        );
        ByteBuffer out = compute(model, identityMatrices(), anim, 4);
        Mat4 expectedMatrix = boneMatrix(rootPivotX, rootPivotY, rootPivotZ, anim, 0);
        expectedMatrix.mul(boneMatrix(childPivotX, childPivotY, childPivotZ, anim, 12));
        Vec3 expected = expectedMatrix.transformPoint(1.0f, 1.0f, 0.0f);
        Vec3 expectedNormal = expectedMatrix.normalMatrix().transformNormal(0.0f, 0.0f, 1.0f).normalize();
        assertVertex(out.asFloatBuffer(), 2, expected.x, expected.y, expected.z, 1.0f, 1.0f, expectedNormal.x, expectedNormal.y, expectedNormal.z);
    }

    private static void runRenderPartMaskTest() throws Exception {
        ByteBuffer model = model(
                new BoneSpec(-1, 1, false, 0.0f, 0.0f, 0.0f, false, new QuadSpec(unitQuad(), new Vec3(0.0f, 0.0f, 1.0f))),
                new BoneSpec(-1, 2, false, 0.0f, 0.0f, 0.0f, false, new QuadSpec(translatedQuad(2.0f, 0.0f, 0.0f), new Vec3(0.0f, 0.0f, 1.0f))),
                new BoneSpec(-1, 3, false, 0.0f, 0.0f, 0.0f, false, new QuadSpec(translatedQuad(4.0f, 0.0f, 0.0f), new Vec3(0.0f, 0.0f, 1.0f)))
        );
        compute(model, identityMatrices(), defaultAnim(3), 12, 0, 0x00f000f0, 0, false);
        compute(model, identityMatrices(), defaultAnim(3), 8, 1, 0x00f000f0, 0, false);
        compute(model, identityMatrices(), defaultAnim(3), 8, 2, 0x00f000f0, 0, false);
        compute(model, identityMatrices(), defaultAnim(3), 4, 4, 0x00f000f0, 0, false);
    }

    private static void runPackedOutputTest() throws Exception {
        int overlay = 0x11223344;
        int light = 0x005500aa;
        ByteBuffer out = compute(
                singleQuadModel(false, unitQuad(), 0.0f, 0.0f, 0.0f),
                identityMatrices(),
                defaultAnim(),
                4,
                0,
                light,
                overlay,
                0.5f, 0.25f, 1.0f, 0.75f,
                true
        );
        assertPackedVertex(out, 2, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, overlay, light, 127, 63, 255, 191, 0, 0, 127);
    }

    private static void runGlowLightTest() throws Exception {
        int packedLight = 0x00123456;
        int glowLight = (15 << 4) | (15 << 20);
        ByteBuffer out = compute(
                model(new BoneSpec(-1, 0, true, 0.0f, 0.0f, 0.0f, false, new QuadSpec(unitQuad(), new Vec3(0.0f, 0.0f, 1.0f)))),
                identityMatrices(),
                defaultAnim(),
                4,
                0,
                packedLight,
                0,
                false
        );
        int actualLight = out.getInt(10 * Float.BYTES);
        if (actualLight != glowLight) {
            throw new IllegalStateException("Glow light expected " + glowLight + ", got " + actualLight);
        }
    }

    private static void runCullTest(boolean backFace, int expectedVertices) throws Exception {
        float[][] positions = backFace
                ? new float[][]{{0.0f, 0.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {1.0f, 1.0f, 0.0f}, {1.0f, 0.0f, 0.0f}}
                : new float[][]{{0.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f}, {1.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f}};
        compute(singleQuadModel(true, positions, 0.0f, 0.0f, 0.0f), identityMatrices(), defaultAnim(), expectedVertices);
    }

    private static void runProjectionCullWithNonIdentityProjectionTest() throws Exception {
        compute(singleQuadModel(true, unitQuad(), 0.0f, 0.0f, 0.0f), projectionScaleMatrices(2.0f, 1.0f, 1.0f), defaultAnim(), 4);
        compute(singleQuadModel(true, unitQuad(), 0.0f, 0.0f, 0.0f), projectionScaleMatrices(-1.0f, 1.0f, 1.0f), defaultAnim(), 0);
    }

    private static void runNearZeroCullTest() throws Exception {
        float[][] positions = {
                {0.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 0.0f},
                {1.0f, 0.00000001f, 0.0f},
                {0.0f, 0.00000001f, 0.0f}
        };
        compute(singleQuadModel(true, positions, 0.0f, 0.0f, 0.0f), identityMatrices(), defaultAnim(), 0);
    }

    private static void runHiddenBoneTest() throws Exception {
        float[] hiddenAnim = defaultAnim();
        hiddenAnim[6] = 0.0f;
        hiddenAnim[7] = 0.0f;
        hiddenAnim[8] = 0.0f;
        compute(singleQuadModel(false, unitQuad(), 0.0f, 0.0f, 0.0f), identityMatrices(), hiddenAnim, 0);
    }

    private static void runAnimatedTransformTest() throws Exception {
        float pivotX = 2.0f;
        float pivotY = -3.0f;
        float pivotZ = 5.0f;
        float[] anim = defaultAnim();
        anim[0] = 0.2f;
        anim[1] = -0.35f;
        anim[2] = 0.7f;
        anim[3] = 1.25f;
        anim[4] = -2.0f;
        anim[5] = 0.5f;
        anim[6] = 1.1f;
        anim[7] = 0.8f;
        anim[8] = 1.3f;

        ByteBuffer out = compute(singleQuadModel(false, unitQuad(), pivotX, pivotY, pivotZ), identityMatrices(), anim, 4);
        Mat4 expectedMatrix = boneMatrix(pivotX, pivotY, pivotZ, anim);
        Vec3 normal = expectedMatrix.normalMatrix().transformNormal(0.0f, 0.0f, 1.0f).normalize();
        Vec3 expected = expectedMatrix.transformPoint(1.0f, 1.0f, 0.0f);
        assertVertex(out.asFloatBuffer(), 2, expected.x, expected.y, expected.z, 1.0f, 1.0f, normal.x, normal.y, normal.z);
    }

    private static void runNonUniformNormalTest() throws Exception {
        float[] anim = defaultAnim();
        anim[0] = 0.25f;
        anim[1] = -0.4f;
        anim[2] = 0.15f;
        anim[6] = 2.0f;
        anim[7] = 0.5f;
        anim[8] = 1.5f;

        Vec3 rawNormal = new Vec3(0.35f, 0.75f, 0.56f).normalize();
        ByteBuffer out = compute(singleQuadModel(false, unitQuad(), 0.0f, 0.0f, 0.0f, rawNormal), identityMatrices(), anim, 4);
        Vec3 expectedNormal = boneMatrix(0.0f, 0.0f, 0.0f, anim).normalMatrix().transformNormal(rawNormal.x, rawNormal.y, rawNormal.z).normalize();
        assertVertex(out.asFloatBuffer(), 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, expectedNormal.x, expectedNormal.y, expectedNormal.z);
    }

    private static void runSyntheticBenchmarkIfEnabled() throws Exception {
        if (!Boolean.getBoolean("ysm.nativeHarnessBenchmark")) return;

        ByteBuffer modelBuffer = model(
                new BoneSpec(-1, 0, false, 0.0f, 0.0f, 0.0f, false),
                new BoneSpec(0, 0, false, 2.0f, -1.0f, 0.0f, true, new QuadSpec(unitQuad(), new Vec3(0.0f, 0.0f, 1.0f)))
        );
        long handle = (long) initModelCache.invoke(null, modelBuffer);
        if (handle == 0) throw new IllegalStateException("nInitModelCache returned no handle for benchmark");

        try {
            ByteBuffer matrices = projectionScaleMatrices(1.25f, 1.0f, 1.0f);
            float[] anim = defaultAnim(2);
            anim[2] = 0.2f;
            anim[12] = -0.1f;
            anim[13] = 0.15f;
            anim[14] = 0.25f;
            ByteBuffer animBuffer = ByteBuffer.allocateDirect(anim.length * Float.BYTES).order(ByteOrder.nativeOrder());
            animBuffer.asFloatBuffer().put(anim);
            ByteBuffer out = ByteBuffer.allocateDirect(4 * 14 * Float.BYTES).order(ByteOrder.nativeOrder());

            int warmup = 200;
            int iterations = 2_000;
            for (int i = 0; i < warmup; i++) {
                NativeModelCache.nComputeModelVertices(handle, out, matrices, animBuffer, 0, 0x00f000f0, 0, 1.0f, 1.0f, 1.0f, 1.0f, false);
            }
            long start = System.nanoTime();
            int vertices = 0;
            for (int i = 0; i < iterations; i++) {
                vertices = NativeModelCache.nComputeModelVertices(handle, out, matrices, animBuffer, 0, 0x00f000f0, 0, 1.0f, 1.0f, 1.0f, 1.0f, false);
            }
            double avgUs = (System.nanoTime() - start) / (iterations * 1000.0);
            System.out.println("Native renderer harness benchmark: avgComputeUs="
                    + String.format(Locale.ROOT, "%.3f", avgUs)
                    + ", vertices=" + vertices
                    + ", iterations=" + iterations);
        } finally {
            destroyModelCache.invoke(null, handle);
        }
    }

    private static ByteBuffer compute(ByteBuffer modelBuffer, ByteBuffer matrixBuffer, float[] anim, int expectedVertices) throws Exception {
        return compute(modelBuffer, matrixBuffer, anim, expectedVertices, 0, 0x00f000f0, 0, false);
    }

    private static ByteBuffer compute(ByteBuffer modelBuffer, ByteBuffer matrixBuffer, float[] anim, int expectedVertices, int renderPartMask, int packedLight, int packedOverlay, boolean packed) throws Exception {
        return compute(modelBuffer, matrixBuffer, anim, expectedVertices, renderPartMask, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f, packed);
    }

    private static ByteBuffer compute(ByteBuffer modelBuffer, ByteBuffer matrixBuffer, float[] anim, int expectedVertices, int renderPartMask, int packedLight, int packedOverlay, float r, float g, float b, float a, boolean packed) throws Exception {
        long handle = (long) initModelCache.invoke(null, modelBuffer);
        if (handle == 0) throw new IllegalStateException("nInitModelCache returned no handle");

        try {
            ByteBuffer animBuffer = ByteBuffer.allocateDirect(anim.length * Float.BYTES).order(ByteOrder.nativeOrder());
            animBuffer.asFloatBuffer().put(anim);
            int stride = packed ? 36 : 14 * Float.BYTES;
            ByteBuffer out = ByteBuffer.allocateDirect(Math.max(expectedVertices, 4) * stride).order(ByteOrder.nativeOrder());
            int vertices = NativeModelCache.nComputeModelVertices(
                    handle,
                    out,
                    matrixBuffer,
                    animBuffer,
                    renderPartMask,
                    packedLight,
                    packedOverlay,
                    r, g, b, a,
                    packed
            );
            if (vertices != expectedVertices) {
                throw new IllegalStateException("Expected " + expectedVertices + " vertices, got " + vertices);
            }
            assertVisibleQuadCount(handle, matrixBuffer, animBuffer, renderPartMask, expectedVertices / 4);
            return out;
        } finally {
            destroyModelCache.invoke(null, handle);
        }
    }

    private static ByteBuffer singleQuadModel(boolean cullable, float[][] positions, float pivotX, float pivotY, float pivotZ) {
        return singleQuadModel(cullable, positions, pivotX, pivotY, pivotZ, new Vec3(0.0f, 0.0f, 1.0f));
    }

    private static ByteBuffer singleQuadModel(boolean cullable, float[][] positions, float pivotX, float pivotY, float pivotZ, Vec3 normal) {
        return model(new BoneSpec(-1, 0, false, pivotX, pivotY, pivotZ, cullable, new QuadSpec(positions, normal)));
    }

    private static ByteBuffer model(BoneSpec... bones) {
        int bytes = Integer.BYTES;
        for (BoneSpec bone : bones) {
            bytes += Integer.BYTES * 3 + Float.BYTES * 3 + 1;
            if (bone.quads.length > 0) {
                bytes += Integer.BYTES + 1 + bone.quads.length * 23 * Float.BYTES;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        buffer.putInt(bones.length);
        for (BoneSpec bone : bones) {
            buffer.putInt(bone.parentIdx);
            buffer.putInt(bone.partMask);
            buffer.put((byte) (bone.glow ? 1 : 0));
            buffer.putFloat(bone.pivotX);
            buffer.putFloat(bone.pivotY);
            buffer.putFloat(bone.pivotZ);
            buffer.putInt(bone.quads.length == 0 ? 0 : 1);
            if (bone.quads.length == 0) continue;

            buffer.put((byte) (bone.cullable ? 1 : 0));
            buffer.putInt(bone.quads.length);
            for (QuadSpec quad : bone.quads) {
                for (float[] position : quad.positions) {
                    buffer.putFloat(position[0]);
                    buffer.putFloat(position[1]);
                    buffer.putFloat(position[2]);
                }
                buffer.putFloat(0.0f).putFloat(0.0f);
                buffer.putFloat(1.0f).putFloat(0.0f);
                buffer.putFloat(1.0f).putFloat(1.0f);
                buffer.putFloat(0.0f).putFloat(1.0f);
                buffer.putFloat(quad.normal.x).putFloat(quad.normal.y).putFloat(quad.normal.z);
            }
        }
        buffer.flip();
        return buffer;
    }

    private static void assertVisibleQuadCount(long handle, ByteBuffer matrixBuffer, ByteBuffer animBuffer, int renderPartMask, int expectedQuads) {
        ByteBuffer ids = ByteBuffer.allocateDirect(Math.max(expectedQuads, 1) * Integer.BYTES).order(ByteOrder.nativeOrder());
        int quads = NativeModelCache.nCollectVisibleQuadIds(handle, ids, matrixBuffer, animBuffer, renderPartMask);
        if (quads != expectedQuads) {
            throw new IllegalStateException("Expected " + expectedQuads + " visible quads, got " + quads);
        }
    }

    private static ByteBuffer identityMatrices() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(64 * Float.BYTES).order(ByteOrder.nativeOrder());
        FloatBuffer floats = buffer.asFloatBuffer();
        putIdentity4(floats, 0);
        putIdentity3(floats, 16);
        putIdentity4(floats, 32);
        return buffer;
    }

    private static ByteBuffer projectionScaleMatrices(float x, float y, float z) {
        ByteBuffer buffer = identityMatrices();
        FloatBuffer floats = buffer.asFloatBuffer();
        putIdentity4(floats, 32);
        floats.put(32, x);
        floats.put(37, y);
        floats.put(42, z);
        return buffer;
    }

    private static float[] defaultAnim() {
        return defaultAnim(1);
    }

    private static float[] defaultAnim(int boneCount) {
        float[] anim = new float[boneCount * 12];
        for (int i = 0; i < boneCount; i++) {
            int offset = i * 12;
            anim[offset + 6] = 1.0f;
            anim[offset + 7] = 1.0f;
            anim[offset + 8] = 1.0f;
        }
        return anim;
    }

    private static float[][] unitQuad() {
        return new float[][]{
                {0.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 0.0f},
                {0.0f, 1.0f, 0.0f}
        };
    }

    private static float[][] translatedQuad(float x, float y, float z) {
        float[][] source = unitQuad();
        float[][] translated = new float[source.length][3];
        for (int i = 0; i < source.length; i++) {
            translated[i][0] = source[i][0] + x;
            translated[i][1] = source[i][1] + y;
            translated[i][2] = source[i][2] + z;
        }
        return translated;
    }

    private static void putIdentity4(FloatBuffer buffer, int offset) {
        buffer.put(offset, 1.0f);
        buffer.put(offset + 5, 1.0f);
        buffer.put(offset + 10, 1.0f);
        buffer.put(offset + 15, 1.0f);
    }

    private static void putIdentity3(FloatBuffer buffer, int offset) {
        buffer.put(offset, 1.0f);
        buffer.put(offset + 4, 1.0f);
        buffer.put(offset + 8, 1.0f);
    }

    private static Mat4 boneMatrix(float pivotX, float pivotY, float pivotZ, float[] anim) {
        return boneMatrix(pivotX, pivotY, pivotZ, anim, 0);
    }

    private static Mat4 boneMatrix(float pivotX, float pivotY, float pivotZ, float[] anim, int offset) {
        Mat4 matrix = new Mat4();
        float px = pivotX * 0.0625f;
        float py = pivotY * 0.0625f;
        float pz = pivotZ * 0.0625f;
        matrix.translate(px - anim[offset + 3] * 0.0625f, py + anim[offset + 4] * 0.0625f, pz + anim[offset + 5] * 0.0625f);
        matrix.rotateZ(anim[offset + 2]);
        matrix.rotateY(anim[offset + 1]);
        matrix.rotateX(anim[offset]);
        matrix.scale(anim[offset + 6], anim[offset + 7], anim[offset + 8]);
        matrix.translate(-px, -py, -pz);
        return matrix;
    }

    private static void assertVertex(FloatBuffer vertices, int index, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        int base = index * 14;
        assertClose("x", x, vertices.get(base));
        assertClose("y", y, vertices.get(base + 1));
        assertClose("z", z, vertices.get(base + 2));
        assertClose("u", u, vertices.get(base + 7));
        assertClose("v", v, vertices.get(base + 8));
        assertClose("nx", nx, vertices.get(base + 11));
        assertClose("ny", ny, vertices.get(base + 12));
        assertClose("nz", nz, vertices.get(base + 13));
    }

    private static void assertPackedVertex(ByteBuffer vertices, int index, float x, float y, float z, float u, float v,
                                           int overlay, int light, int r, int g, int b, int a, int nx, int ny, int nz) {
        int base = index * 36;
        assertClose("packed x", x, vertices.getFloat(base));
        assertClose("packed y", y, vertices.getFloat(base + 4));
        assertClose("packed z", z, vertices.getFloat(base + 8));
        assertByte("packed r", r, vertices.get(base + 12));
        assertByte("packed g", g, vertices.get(base + 13));
        assertByte("packed b", b, vertices.get(base + 14));
        assertByte("packed a", a, vertices.get(base + 15));
        assertClose("packed u", u, vertices.getFloat(base + 16));
        assertClose("packed v", v, vertices.getFloat(base + 20));
        if (vertices.getInt(base + 24) != overlay) {
            throw new IllegalStateException("packed overlay expected " + overlay + ", got " + vertices.getInt(base + 24));
        }
        if (vertices.getInt(base + 28) != light) {
            throw new IllegalStateException("packed light expected " + light + ", got " + vertices.getInt(base + 28));
        }
        assertByte("packed nx", nx, vertices.get(base + 32));
        assertByte("packed ny", ny, vertices.get(base + 33));
        assertByte("packed nz", nz, vertices.get(base + 34));
    }

    private static void assertByte(String label, int expected, byte actual) {
        int actualUnsigned = Byte.toUnsignedInt(actual);
        if (actualUnsigned != expected) {
            throw new IllegalStateException(label + " expected " + expected + ", got " + actualUnsigned);
        }
    }

    private static void assertClose(String label, float expected, float actual) {
        if (Math.abs(expected - actual) > EPSILON) {
            throw new IllegalStateException(label + " expected " + expected + ", got " + actual);
        }
    }

    private static final class BoneSpec {
        final int parentIdx;
        final int partMask;
        final boolean glow;
        final float pivotX;
        final float pivotY;
        final float pivotZ;
        final boolean cullable;
        final QuadSpec[] quads;

        BoneSpec(int parentIdx, int partMask, boolean glow, float pivotX, float pivotY, float pivotZ, boolean cullable, QuadSpec... quads) {
            this.parentIdx = parentIdx;
            this.partMask = partMask;
            this.glow = glow;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.cullable = cullable;
            this.quads = quads;
        }
    }

    private static final class QuadSpec {
        final float[][] positions;
        final Vec3 normal;

        QuadSpec(float[][] positions, Vec3 normal) {
            this.positions = positions;
            this.normal = normal;
        }
    }

    private static final class Vec3 {
        final float x;
        final float y;
        final float z;

        Vec3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Vec3 normalize() {
            float len = (float) Math.sqrt(x * x + y * y + z * z);
            if (len <= 1.0e-8f) return this;
            return new Vec3(x / len, y / len, z / len);
        }
    }

    private static final class Mat4 {
        private final float[] m = new float[16];

        Mat4() {
            m[0] = 1.0f;
            m[5] = 1.0f;
            m[10] = 1.0f;
            m[15] = 1.0f;
        }

        void translate(float x, float y, float z) {
            Mat4 translation = new Mat4();
            translation.m[12] = x;
            translation.m[13] = y;
            translation.m[14] = z;
            mul(translation);
        }

        void rotateX(float angle) {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            Mat4 rotation = new Mat4();
            rotation.m[5] = c;
            rotation.m[6] = s;
            rotation.m[9] = -s;
            rotation.m[10] = c;
            mul(rotation);
        }

        void rotateY(float angle) {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            Mat4 rotation = new Mat4();
            rotation.m[0] = c;
            rotation.m[2] = -s;
            rotation.m[8] = s;
            rotation.m[10] = c;
            mul(rotation);
        }

        void rotateZ(float angle) {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            Mat4 rotation = new Mat4();
            rotation.m[0] = c;
            rotation.m[1] = s;
            rotation.m[4] = -s;
            rotation.m[5] = c;
            mul(rotation);
        }

        void scale(float x, float y, float z) {
            Mat4 scaling = new Mat4();
            scaling.m[0] = x;
            scaling.m[5] = y;
            scaling.m[10] = z;
            mul(scaling);
        }

        Vec3 transformPoint(float x, float y, float z) {
            return new Vec3(
                    m[0] * x + m[4] * y + m[8] * z + m[12],
                    m[1] * x + m[5] * y + m[9] * z + m[13],
                    m[2] * x + m[6] * y + m[10] * z + m[14]
            );
        }

        Vec3 transformNormal(float x, float y, float z) {
            return new Vec3(
                    m[0] * x + m[4] * y + m[8] * z,
                    m[1] * x + m[5] * y + m[9] * z,
                    m[2] * x + m[6] * y + m[10] * z
            );
        }

        Mat4 normalMatrix() {
            Mat4 out = new Mat4();
            float a00 = m[0], a01 = m[4], a02 = m[8];
            float a10 = m[1], a11 = m[5], a12 = m[9];
            float a20 = m[2], a21 = m[6], a22 = m[10];

            float c00 = a11 * a22 - a12 * a21;
            float c01 = a12 * a20 - a10 * a22;
            float c02 = a10 * a21 - a11 * a20;
            float c10 = a02 * a21 - a01 * a22;
            float c11 = a00 * a22 - a02 * a20;
            float c12 = a01 * a20 - a00 * a21;
            float c20 = a01 * a12 - a02 * a11;
            float c21 = a02 * a10 - a00 * a12;
            float c22 = a00 * a11 - a01 * a10;

            float det = a00 * c00 + a01 * c01 + a02 * c02;
            if (Math.abs(det) <= 1.0e-8f) {
                out.m[0] = a00;
                out.m[1] = a10;
                out.m[2] = a20;
                out.m[4] = a01;
                out.m[5] = a11;
                out.m[6] = a21;
                out.m[8] = a02;
                out.m[9] = a12;
                out.m[10] = a22;
                return out;
            }

            float invDet = 1.0f / det;
            out.m[0] = c00 * invDet;
            out.m[1] = c10 * invDet;
            out.m[2] = c20 * invDet;
            out.m[4] = c01 * invDet;
            out.m[5] = c11 * invDet;
            out.m[6] = c21 * invDet;
            out.m[8] = c02 * invDet;
            out.m[9] = c12 * invDet;
            out.m[10] = c22 * invDet;
            return out;
        }

        private void mul(Mat4 right) {
            float[] out = new float[16];
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    out[col * 4 + row] =
                            m[row] * right.m[col * 4]
                                    + m[4 + row] * right.m[col * 4 + 1]
                                    + m[8 + row] * right.m[col * 4 + 2]
                                    + m[12 + row] * right.m[col * 4 + 3];
                }
            }
            System.arraycopy(out, 0, m, 0, m.length);
        }
    }
}
