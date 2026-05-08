package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.compatibility.YsmClientCompat;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.NativeAccelerationMode;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.*;
import org.lwjgl.system.MemoryUtil;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

public class NativeModelRenderer {
    static final float CULL_DETERMINANT_EPSILON = 1.0e-7f;
    private static boolean nativeRendererDisabled = false;
    private static final boolean nativeStats = Boolean.getBoolean("ysm.nativeRenderer.stats");
    private static final long nativeStatsIntervalNanos = Long.getLong("ysm.nativeRenderer.statsIntervalSec", 5L) * 1_000_000_000L;
    private static final boolean nativeDebug = Boolean.getBoolean("ysm.nativeRenderer.debug");
    private static final boolean allowCompatNativeRenderer = Boolean.getBoolean("ysm.nativeRenderer.allowCompat");
    private static long nativeCalls;
    private static long nativeDirectCalls;
    private static long nativeCompatCalls;
    private static long javaCalls;
    private static long nativeNanos;
    private static long javaNanos;
    private static long nativeFallbacks;
    private static long skippedNoLib;
    private static long skippedDisabled;
    private static long skippedNoCache;
    private static String lastFallbackReason = "not attempted";
    private static long lastStatsLogNanos = System.nanoTime();

    static {
        if (nativeDebug || nativeStats || System.getProperty("ysm.nativeRenderer") != null) {
            YesSteveModel.LOGGER.info(
                    "[YSM native renderer] stats={}, debug={}, statsIntervalSec={}, nativeStatus={}",
                    nativeStats,
                    nativeDebug,
                    nativeStatsIntervalNanos / 1_000_000_000L,
                    NativeLibLoader.getStatusSummary()
            );
        }
    }

    public static void renderMesh(VertexConsumer buffer, PoseStack.Pose pose, GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        YsmClientCompat.updatePbrState();
        boolean optifinePresent = YsmClientCompat.isOptifinePresent();
        boolean compatibilityRendererEnabled = GeneralConfig.USE_COMPATIBILITY_RENDERER.get();
        boolean isCompatMode = optifinePresent || compatibilityRendererEnabled;
        boolean blockedByCompatMode = isCompatMode && !allowCompatNativeRenderer;

        NativeAccelerationMode nativeMode = getNativeMode();
        boolean hasNativeLib = NativeLibLoader.isLoaded();
        NativeModelCache nativeCache = model.nativeCache();
        boolean hasNativeCache = nativeCache != null && nativeCache.isReady();
        String nativeCacheFailureReason = nativeCache == null ? "model has no native cache" : nativeCache.failureReason();
        boolean canUseNativeRenderer = nativeMode.allowsRenderer()
                && !nativeRendererDisabled
                && !blockedByCompatMode
                && hasNativeLib
                && hasNativeCache;

        if (canUseNativeRenderer) {
            try {
                long startNanos = nativeStats ? System.nanoTime() : 0L;
                boolean directTransfer = nativeRenderModel(
                        buffer,
                        pose,
                        isCompatMode,
                        nativeCache,
                        boneParams,
                        renderPartMask,
                        packedLight,
                        packedOverlay,
                        red, green, blue, alpha
                );
                recordNativeRender(startNanos, directTransfer);
                return;
            } catch (Throwable throwable) {
                nativeFallbacks++;
                YesSteveModel.LOGGER.error("Native YSM renderer failed", throwable);
                nativeRendererDisabled = true;
                lastFallbackReason = "native render threw: " + throwable.getMessage();
                NativeLibLoader.disableNativeRenderer(lastFallbackReason, throwable);
            }
        }

        recordNativeSkip(nativeMode, hasNativeLib, hasNativeCache, nativeCacheFailureReason, optifinePresent, compatibilityRendererEnabled, blockedByCompatMode);

        long startNanos = nativeStats ? System.nanoTime() : 0L;
        renderModel(
                buffer,
                pose,
                model,
                boneParams,
                renderPartMask,
                packedLight,
                packedOverlay,
                red, green, blue, alpha
        );
        recordJavaRender(startNanos);
    }

    public static NativeAccelerationMode getNativeMode() {
        NativeAccelerationMode configured = GeneralConfig.NATIVE_ACCELERATION_MODE.get();
        String property = System.getProperty("ysm.nativeRenderer");
        if (property == null) return configured;
        return switch (property.toLowerCase(Locale.ROOT)) {
            case "off", "java", "false" -> NativeAccelerationMode.OFF;
            case "force", "on", "native", "render", "render_only" -> NativeAccelerationMode.RENDER_ONLY;
            case "full", "full_experimental" -> NativeAccelerationMode.FULL_EXPERIMENTAL;
            default -> NativeAccelerationMode.parse(property, configured);
        };
    }

    public static NativeStats getStats() {
        double nativeAvgUs = nativeCalls == 0 ? 0.0 : nativeNanos / (nativeCalls * 1000.0);
        double javaAvgUs = javaCalls == 0 ? 0.0 : javaNanos / (javaCalls * 1000.0);
        return new NativeStats(
                getNativeMode(),
                nativeCalls,
                nativeDirectCalls,
                nativeCompatCalls,
                javaCalls,
                nativeFallbacks,
                skippedNoLib,
                skippedDisabled,
                skippedNoCache,
                YsmClientCompat.isOptifinePresent(),
                GeneralConfig.USE_COMPATIBILITY_RENDERER.get(),
                allowCompatNativeRenderer,
                nativeStats,
                nativeAvgUs,
                javaAvgUs,
                lastFallbackReason
        );
    }

    public record NativeStats(NativeAccelerationMode mode,
                              long nativeCalls,
                              long nativeDirectCalls,
                              long nativeCompatCalls,
                              long javaCalls,
                              long nativeFallbacks,
                              long skippedNoLib,
                              long skippedDisabled,
                              long skippedNoCache,
                              boolean optifinePresent,
                              boolean compatibilityRendererEnabled,
                              boolean allowCompatNativeRenderer,
                              boolean timingEnabled,
                              double nativeAvgUs,
                              double javaAvgUs,
                              String lastFallbackReason) {
        public String toDisplayString() {
            return "mode=" + mode
                    + ", nativeCalls=" + nativeCalls
                    + ", nativeDirect=" + nativeDirectCalls
                    + ", nativeCompat=" + nativeCompatCalls
                    + ", javaCalls=" + javaCalls
                    + ", fallbacks=" + nativeFallbacks
                    + ", skippedNoLib=" + skippedNoLib
                    + ", skippedDisabled=" + skippedDisabled
                    + ", skippedNoCache=" + skippedNoCache
                    + ", optifine=" + optifinePresent
                    + ", compatConfig=" + compatibilityRendererEnabled
                    + ", allowCompat=" + allowCompatNativeRenderer
                    + ", timing=" + (timingEnabled ? "on" : "off")
                    + ", nativeAvgUs=" + String.format(Locale.ROOT, "%.3f", nativeAvgUs)
                    + ", javaAvgUs=" + String.format(Locale.ROOT, "%.3f", javaAvgUs)
                    + ", lastFallback=" + lastFallbackReason;
        }
    }

    private static void recordNativeSkip(NativeAccelerationMode nativeMode, boolean hasNativeLib, boolean hasNativeCache, String nativeCacheFailureReason, boolean optifinePresent, boolean compatibilityRendererEnabled, boolean blockedByCompatMode) {
        if (nativeMode == NativeAccelerationMode.OFF) {
            skippedDisabled++;
            lastFallbackReason = "native acceleration is OFF";
        } else if (!nativeMode.allowsRenderer()) {
            skippedDisabled++;
            lastFallbackReason = "AUTO keeps Java renderer until native parity is proven";
        } else if (nativeRendererDisabled) {
            skippedDisabled++;
            lastFallbackReason = "native renderer disabled after a failed condition";
        } else if (blockedByCompatMode) {
            skippedDisabled++;
            if (optifinePresent) {
                lastFallbackReason = "OptiFine path; add -Dysm.nativeRenderer.allowCompat=true to test native per-vertex path";
            } else if (compatibilityRendererEnabled) {
                lastFallbackReason = "UseCompatibilityRenderer=true; disable it or add -Dysm.nativeRenderer.allowCompat=true";
            } else {
                lastFallbackReason = "compatibility renderer path";
            }
        } else if (!hasNativeLib) {
            skippedNoLib++;
            lastFallbackReason = NativeLibLoader.getFallbackReason();
        } else if (!hasNativeCache) {
            skippedNoCache++;
            lastFallbackReason = nativeCacheFailureReason == null || nativeCacheFailureReason.isBlank() ? "model has no native cache" : nativeCacheFailureReason;
        }
        if (!nativeStats && !nativeDebug) return;
        logStatsIfNeeded();
    }

    private static void recordNativeRender(long startNanos, boolean directTransfer) {
        nativeCalls++;
        if (directTransfer) {
            nativeDirectCalls++;
            lastFallbackReason = "none; native direct buffer path";
        } else {
            nativeCompatCalls++;
            lastFallbackReason = "none; native compatibility/per-vertex path";
        }
        if (nativeStats) {
            nativeNanos += System.nanoTime() - startNanos;
        }
        logStatsIfNeeded();
    }

    private static void recordJavaRender(long startNanos) {
        javaCalls++;
        if (nativeStats) {
            javaNanos += System.nanoTime() - startNanos;
        }
        logStatsIfNeeded();
    }

    private static void logStatsIfNeeded() {
        if (!nativeStats && !nativeDebug) return;
        long now = System.nanoTime();
        if (now - lastStatsLogNanos < nativeStatsIntervalNanos) return;
        lastStatsLogNanos = now;
        double nativeAvgUs = nativeCalls == 0 ? 0.0 : nativeNanos / (nativeCalls * 1000.0);
        double javaAvgUs = javaCalls == 0 ? 0.0 : javaNanos / (javaCalls * 1000.0);
        YesSteveModel.LOGGER.info(
                "[YSM native renderer] mode={}, nativeCalls={}, nativeDirect={}, nativeCompat={}, javaCalls={}, fallbacks={}, skippedNoLib={}, skippedDisabled={}, skippedNoCache={}, nativeAvgUs={}, javaAvgUs={}, lastFallback={}",
                getNativeMode(),
                nativeCalls,
                nativeDirectCalls,
                nativeCompatCalls,
                javaCalls,
                nativeFallbacks,
                skippedNoLib,
                skippedDisabled,
                skippedNoCache,
                String.format(Locale.ROOT, "%.3f", nativeAvgUs),
                String.format(Locale.ROOT, "%.3f", javaAvgUs),
                lastFallbackReason
        );
    }

    public static void renderModel(
            VertexConsumer vertexConsumer,
            PoseStack.Pose pose,
            GeoModel mesh,
            float[] boneParams,
            int renderPartMask,
            int packedLight, int packedOverlay,
            float r, float g, float b, float a) {

        if (mesh.bakedBones == null || mesh.bakedBones.isEmpty()) return;

        visitModelVertices(
                mesh,
                boneParams,
                pose.pose(),
                pose.normal(),
                RenderSystem.getProjectionMatrix(),
                renderPartMask,
                packedLight,
                (x, y, z, u, v, nx, ny, nz, light) ->
                        vertexConsumer.vertex(x, y, z, r, g, b, a, u, v, packedOverlay, light, nx, ny, nz)
        );
    }

    static void visitModelVertices(
            GeoModel mesh,
            float[] boneParams,
            Matrix4f rootPoseMat,
            Matrix3f rootNormalMC,
            Matrix4f projMat,
            int renderPartMask,
            int packedLight,
            ModelVertexSink sink) {
        Matrix4f identityMat = new Matrix4f();
        Matrix4f globalBoneMat = new Matrix4f();
        Matrix4f projBoneMat = new Matrix4f();
        Matrix3f localNormalMat = new Matrix3f();
        Matrix3f globalNormalMat = new Matrix3f();

        Vector4f p1 = new Vector4f();
        Vector4f p2 = new Vector4f();
        Vector4f p3 = new Vector4f();
        Vector4f tempPos = new Vector4f();
        Vector3f tempNorm = new Vector3f();
        Matrix4f[] boneLocalTransforms = new Matrix4f[mesh.bakedBones.size()];
        boolean[] boneVisible = new boolean[mesh.bakedBones.size()];

        for (int i = 0; i < mesh.bakedBones.size(); i++) {
            calculateBoneMatrix(i, mesh.bakedBones, boneParams, boneLocalTransforms, boneVisible, identityMat);
        }

        for (int i = 0; i < mesh.bakedBones.size(); i++) {
            // 如果骨骼被標記為不可見直接跳過當前骨骼
            if (!boneVisible[i]) {
                continue;
            }

            GeoModel.BakedBone bone = mesh.bakedBones.get(i);
            if (renderPartMask != 0 && bone.partMask != renderPartMask && bone.partMask != 3) {
                continue;
            }

            Matrix4f localBoneMat = boneLocalTransforms[i];
            globalBoneMat.set(rootPoseMat).mul(localBoneMat);
            projBoneMat.set(projMat).mul(globalBoneMat);

            // 法線全域矩陣
            localBoneMat.normal(localNormalMat);
            globalNormalMat.set(rootNormalMC).mul(localNormalMat);

            int currentPackedLight = bone.glow ? LightTexture.pack(15, 15) : packedLight;

            for (GeoModel.BakedCube cube : bone.cubes) {
                for (GeoModel.BakedQuad quad : cube.quads) {
                    p1.set(quad.positions[0].x(), quad.positions[0].y(), quad.positions[0].z(), 1.0f).mul(projBoneMat);
                    p2.set(quad.positions[1].x(), quad.positions[1].y(), quad.positions[1].z(), 1.0f).mul(projBoneMat);
                    p3.set(quad.positions[2].x(), quad.positions[2].y(), quad.positions[2].z(), 1.0f).mul(projBoneMat);
                    float det = p1.x() * (p2.y() * p3.w() - p3.y() * p2.w()) - p2.x() * (p1.y() * p3.w() - p3.y() * p1.w()) + p3.x() * (p1.y() * p2.w() - p2.y() * p1.w());
                    if (det <= CULL_DETERMINANT_EPSILON && cube.cullable) {
                        continue;
                    }
                    tempNorm.set(quad.normal).mul(globalNormalMat).normalize();
                    for (int v = 0; v < 4; v++) {
                        tempPos.set(quad.positions[v].x(), quad.positions[v].y(), quad.positions[v].z(), 1.0f).mul(globalBoneMat);
                        sink.vertex(
                                tempPos.x(), tempPos.y(), tempPos.z(),
                                quad.uvs[v].x(), quad.uvs[v].y(),
                                tempNorm.x(), tempNorm.y(), tempNorm.z(),
                                currentPackedLight
                        );
                    }
                }
            }
        }
    }

    @FunctionalInterface
    interface ModelVertexSink {
        void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, int packedLight);
    }

    static Matrix4f calculateBoneMatrix(int idx, java.util.List<GeoModel.BakedBone> bones, float[] boneParams, Matrix4f[] cache, boolean[] visibleCache, Matrix4f rootPose) {
        if (cache[idx] != null) return cache[idx];

        GeoModel.BakedBone bone = bones.get(idx);
        Matrix4f parentMatrix = rootPose;
        boolean isVisible = true;

        if (bone.parentIdx != -1) {
            parentMatrix = calculateBoneMatrix(bone.parentIdx, bones, boneParams, cache, visibleCache, rootPose);
            // 如果父骨骼不可見，子骨骼必然跟著不可見
            if (!visibleCache[bone.parentIdx]) {
                isVisible = false;
            }
        }

        Matrix4f localMat = new Matrix4f(parentMatrix);

        int pOffset = idx * 12;
        float animRx = boneParams[pOffset];
        float animRy = boneParams[pOffset + 1];
        float animRz = boneParams[pOffset + 2];
        float animTx = boneParams[pOffset + 3];
        float animTy = boneParams[pOffset + 4];
        float animTz = boneParams[pOffset + 5];
        float animSx = boneParams[pOffset + 6];
        float animSy = boneParams[pOffset + 7];
        float animSz = boneParams[pOffset + 8];

        if (animSx == 0.0f && animSy == 0.0f && animSz == 0.0f) {
            isVisible = false;
        }

        localMat.translate(
                (bone.pivotX - animTx) * 0.0625f,
                (bone.pivotY + animTy) * 0.0625f,
                (bone.pivotZ + animTz) * 0.0625f
        );
        localMat.rotateZ(animRz);
        localMat.rotateY(animRy);
        localMat.rotateX(animRx);

        if (animSx != 1.0f || animSy != 1.0f || animSz != 1.0f) {
            localMat.scale(animSx, animSy, animSz);
        }

        localMat.translate(-bone.pivotX / 16f, -bone.pivotY / 16f, -bone.pivotZ / 16f);

        cache[idx] = localMat;
        visibleCache[idx] = isVisible; // 保存當前骨骼的可見性
        return localMat;
    }

    private static final ByteBuffer matrixTransferBuffer = ByteBuffer.allocateDirect(64 * 4).order(ByteOrder.nativeOrder());
    private static final FloatBuffer matrixTransferFloatBuffer = matrixTransferBuffer.asFloatBuffer();

    private static int currentAnimBufferCapacityBytes = 512 * 12 * 4;
    private static ByteBuffer animTransferBuffer = ByteBuffer.allocateDirect(currentAnimBufferCapacityBytes).order(ByteOrder.nativeOrder());
    private static FloatBuffer animTransferFloatBuffer = animTransferBuffer.asFloatBuffer();

    public static boolean nativeRenderModel(
            VertexConsumer vertexConsumer,
            PoseStack.Pose pose,
            boolean isCompatMode,
            NativeModelCache nativeCache,
            float[] boneVertex,
            int renderPartMask,
            int packedLight, int packedOverlay,
            float r, float g, float b, float a) {

        if (nativeCache == null || !nativeCache.isReady()) return false;

        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        ByteBuffer outBuffer = nativeCache.vertexOutBuffer();
        boolean useDirectMemoryTransfer = !isCompatMode && (vertexConsumer instanceof BufferBuilder);

        matrixTransferFloatBuffer.clear();
        pose.pose().get(0, matrixTransferFloatBuffer);
        pose.normal().get(16, matrixTransferFloatBuffer);
        projMat.get(32, matrixTransferFloatBuffer);

        int requiredBytes = boneVertex.length * 4;
        if (currentAnimBufferCapacityBytes < requiredBytes) {
            currentAnimBufferCapacityBytes = Math.max(currentAnimBufferCapacityBytes * 2, requiredBytes);
            animTransferBuffer = ByteBuffer.allocateDirect(currentAnimBufferCapacityBytes).order(ByteOrder.nativeOrder());
            animTransferFloatBuffer = animTransferBuffer.asFloatBuffer();
        }
        animTransferFloatBuffer.clear();
        animTransferFloatBuffer.put(boneVertex);

        int vertexCount = NativeModelCache.nComputeModelVertices(nativeCache.handle(), outBuffer, matrixTransferBuffer, animTransferBuffer, renderPartMask, packedLight, packedOverlay, r, g, b, a, useDirectMemoryTransfer);

        if (vertexCount < 0) {
            throw new IllegalStateException("native renderer rejected output buffer: code=" + vertexCount
                    + ", capacity=" + outBuffer.capacity()
                    + ", vertexCapacity=" + nativeCache.vertexCapacity());
        }
        if (vertexCount > nativeCache.vertexCapacity()) {
            throw new IllegalStateException("native renderer overflow: vertices=" + vertexCount
                    + ", capacity=" + nativeCache.vertexCapacity());
        }
        if (vertexCount == 0) return useDirectMemoryTransfer;

        useDirectMemoryTransfer = false;
        long address = MemoryUtil.memAddress(outBuffer);
        for (int i = 0; i < vertexCount; i++) {
            long ptr = address + (i * 14L * 4L);
            float vx = MemoryUtil.memGetFloat(ptr);
            float vy = MemoryUtil.memGetFloat(ptr + 4);
            float vz = MemoryUtil.memGetFloat(ptr + 8);
            float vr = MemoryUtil.memGetFloat(ptr + 12);
            float vg = MemoryUtil.memGetFloat(ptr + 16);
            float vb = MemoryUtil.memGetFloat(ptr + 20);
            float va = MemoryUtil.memGetFloat(ptr + 24);
            float u = MemoryUtil.memGetFloat(ptr + 28);
            float v = MemoryUtil.memGetFloat(ptr + 32);
            int overlay = MemoryUtil.memGetInt(ptr + 36);
            int light = MemoryUtil.memGetInt(ptr + 40);
            float nx = MemoryUtil.memGetFloat(ptr + 44);
            float ny = MemoryUtil.memGetFloat(ptr + 48);
            float nz = MemoryUtil.memGetFloat(ptr + 52);
            vertexConsumer.vertex(vx, vy, vz, vr, vg, vb, va, u, v, overlay, light, nx, ny, nz);
        }

        return useDirectMemoryTransfer;
    }
}
