package com.elfmcys.yesstevemodel.geckolib3.geo.render.built;

import com.elfmcys.yesstevemodel.YesSteveModel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class NativeModelCache implements AutoCloseable {
    private static final boolean nativeDebug = Boolean.getBoolean("ysm.nativeRenderer.debug");

    private long handle;
    private final ByteBuffer vertexOutBuffer;
    private final int vertexCapacity;
    private final String failureReason;

    public static native int nGetNativeAbiVersion();

    public static native String nGetNativeBackendName();

    public static native boolean nSelfTest(ByteBuffer buffer);

    private static native long nInitModelCache(ByteBuffer buffer);

    private static native void nDestroyModelCache(long handle);

    public static native int nComputeModelVertices(long handle, ByteBuffer outBuffer, ByteBuffer matrixBuffer, ByteBuffer animBuffer, int renderPartMask, int packedLight, int packedOverlay, float r, float g, float b, float a, boolean packed);

    public static native int nCollectVisibleQuadIds(long handle, ByteBuffer outBuffer, ByteBuffer matrixBuffer, ByteBuffer animBuffer, int renderPartMask);

    private NativeModelCache(long handle, ByteBuffer vertexOutBuffer, int vertexCapacity, String failureReason) {
        this.handle = handle;
        this.vertexOutBuffer = vertexOutBuffer;
        this.vertexCapacity = vertexCapacity;
        this.failureReason = failureReason;
    }

    public static NativeModelCache build(GeoModel model) {
        if (model.bakedBones == null || model.bakedBones.isEmpty()) {
            return unavailable("model has no baked bones");
        }

        try {
            int cacheBytes = computeCacheBytes(model);
            ByteBuffer buffer = ByteBuffer.allocateDirect(cacheBytes).order(ByteOrder.nativeOrder());
            int quadCount = writeCache(model, buffer);
            buffer.flip();

            int vertexCount = quadCount * 4;
            int unpackedBytes = vertexCount * 14 * Float.BYTES;
            int packedBytes = vertexCount * 36;
            ByteBuffer outBuffer = ByteBuffer.allocateDirect(Math.max(unpackedBytes, packedBytes)).order(ByteOrder.nativeOrder());

            long handle;
            handle = nInitModelCache(buffer);
            if (handle == 0) {
                logCacheFailure("model cache init returned no handle", null);
                return unavailable("model cache init returned no handle");
            }

            if (nativeDebug) {
                YesSteveModel.LOGGER.info(
                        "[YSM native renderer] built cache handle={}, bones={}, quads={}, vertices={}, cacheBytes={}, outBufferBytes={}",
                        handle,
                        model.bakedBones.size(),
                        quadCount,
                        vertexCount,
                        buffer.limit(),
                        outBuffer.capacity()
                );
            }
            return new NativeModelCache(handle, outBuffer, vertexCount, "");
        } catch (Throwable throwable) {
            logCacheFailure("model cache init failed: " + throwable.getMessage(), throwable);
            return unavailable("model cache init failed: " + throwable.getMessage());
        }
    }

    private static int computeCacheBytes(GeoModel model) {
        int bytes = Integer.BYTES;
        for (GeoModel.BakedBone bone : model.bakedBones) {
            bytes += Integer.BYTES * 3 + Float.BYTES * 3 + 1;
            for (GeoModel.BakedCube cube : bone.cubes) {
                bytes += Integer.BYTES + 1;
                bytes += cube.quads.size() * (23 * Float.BYTES);
            }
        }
        return bytes;
    }

    private static int writeCache(GeoModel model, ByteBuffer buffer) {
        int quadCount = 0;
        buffer.putInt(model.bakedBones.size());
        for (GeoModel.BakedBone bone : model.bakedBones) {
            buffer.putInt(bone.parentIdx);
            buffer.putInt(bone.partMask);
            buffer.put((byte) (bone.glow ? 1 : 0));
            buffer.putFloat(bone.pivotX);
            buffer.putFloat(bone.pivotY);
            buffer.putFloat(bone.pivotZ);

            buffer.putInt(bone.cubes.size());
            for (GeoModel.BakedCube cube : bone.cubes) {
                buffer.put((byte) (cube.cullable ? 1 : 0));
                buffer.putInt(cube.quads.size());
                quadCount += cube.quads.size();
                for (GeoModel.BakedQuad quad : cube.quads) {
                    for (int v = 0; v < 4; v++) {
                        buffer.putFloat(quad.positions[v].x());
                        buffer.putFloat(quad.positions[v].y());
                        buffer.putFloat(quad.positions[v].z());
                    }
                    for (int v = 0; v < 4; v++) {
                        buffer.putFloat(quad.uvs[v].x());
                        buffer.putFloat(quad.uvs[v].y());
                    }
                    buffer.putFloat(quad.normal.x());
                    buffer.putFloat(quad.normal.y());
                    buffer.putFloat(quad.normal.z());
                }
            }
        }
        return quadCount;
    }

    private static NativeModelCache unavailable(String reason) {
        return new NativeModelCache(0, null, 0, reason);
    }

    private static void logCacheFailure(String reason, Throwable throwable) {
        if (throwable != null && nativeDebug) {
            YesSteveModel.LOGGER.warn("[YSM native renderer] {}", reason, throwable);
        } else {
            YesSteveModel.LOGGER.warn("[YSM native renderer] {}", reason);
        }
    }

    public boolean isReady() {
        return handle != 0 && vertexOutBuffer != null && vertexCapacity > 0;
    }

    public long handle() {
        return handle;
    }

    public ByteBuffer vertexOutBuffer() {
        return vertexOutBuffer;
    }

    public int vertexCapacity() {
        return vertexCapacity;
    }

    public String failureReason() {
        return failureReason;
    }

    @Override
    public void close() {
        if (handle != 0) {
            nDestroyModelCache(handle);
            handle = 0;
        }
    }
}
