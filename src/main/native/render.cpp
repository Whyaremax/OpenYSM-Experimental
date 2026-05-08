#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <functional>
#include <vector>

#include "jni.h"

namespace {
constexpr int ABI_VERSION = 1;
constexpr int FLOAT_VERTEX_BYTES = 14 * 4;
constexpr int PACKED_VERTEX_BYTES = 36;
constexpr float CULL_DETERMINANT_EPSILON = 1.0e-7f;

struct NativeBone {
    int parentIdx;
    int partMask;
    bool glow;
    float pivotX;
    float pivotY;
    float pivotZ;
};

struct NativeQuad {
    int quadIdx;
    int boneIdx;
    bool cullable;
    float x[4];
    float y[4];
    float z[4];
    float u[4];
    float v[4];
    float nx;
    float ny;
    float nz;
};

struct Mat4 {
    float m[16];

    Mat4() {
        identity();
    }

    explicit Mat4(const float *data) {
        std::memcpy(m, data, sizeof(m));
    }

    void identity() {
        std::memset(m, 0, sizeof(m));
        m[0] = 1.0f;
        m[5] = 1.0f;
        m[10] = 1.0f;
        m[15] = 1.0f;
    }

    void mul(const Mat4 &right) {
        float out[16];
        for (int col = 0; col < 4; ++col) {
            for (int row = 0; row < 4; ++row) {
                out[col * 4 + row] =
                    m[0 * 4 + row] * right.m[col * 4 + 0] +
                    m[1 * 4 + row] * right.m[col * 4 + 1] +
                    m[2 * 4 + row] * right.m[col * 4 + 2] +
                    m[3 * 4 + row] * right.m[col * 4 + 3];
            }
        }
        std::memcpy(m, out, sizeof(m));
    }

    Mat4 normalMatrix4x4() const {
        Mat4 out;
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
        if (std::fabs(det) <= 1.0e-8f) {
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
};

struct Vec3 {
    float x;
    float y;
    float z;
};

struct Vec4 {
    float x;
    float y;
    float z;
    float w;
};

struct NativeModel {
    std::vector<NativeBone> bones;
    std::vector<NativeQuad> quads;
    std::vector<int> evalOrder;
    std::vector<Mat4> localTransforms;
    std::vector<char> visible;
    std::vector<Mat4> globalTransforms;
    std::vector<Mat4> projectedTransforms;
    std::vector<Mat4> normalTransforms;
    std::vector<int> lights;
};

class BufferReader {
public:
    BufferReader(const uint8_t *data, jlong capacity) : data(data), capacity(capacity) {
    }

    bool readInt(int &value) {
        return read(&value, sizeof(value));
    }

    bool readFloat(float &value) {
        return read(&value, sizeof(value));
    }

    bool readByte(uint8_t &value) {
        return read(&value, sizeof(value));
    }

private:
    bool read(void *out, jlong size) {
        if (offset < 0 || size < 0 || offset + size > capacity) return false;
        std::memcpy(out, data + offset, static_cast<size_t>(size));
        offset += size;
        return true;
    }

    const uint8_t *data;
    jlong capacity;
    jlong offset = 0;
};

static Vec4 transformPoint(const Mat4 &mat, float x, float y, float z, float w) {
    return {
        mat.m[0] * x + mat.m[4] * y + mat.m[8] * z + mat.m[12] * w,
        mat.m[1] * x + mat.m[5] * y + mat.m[9] * z + mat.m[13] * w,
        mat.m[2] * x + mat.m[6] * y + mat.m[10] * z + mat.m[14] * w,
        mat.m[3] * x + mat.m[7] * y + mat.m[11] * z + mat.m[15] * w
    };
}

static Vec3 transformNormal(const Mat4 &mat, float x, float y, float z) {
    Vec3 out {
        mat.m[0] * x + mat.m[4] * y + mat.m[8] * z,
        mat.m[1] * x + mat.m[5] * y + mat.m[9] * z,
        mat.m[2] * x + mat.m[6] * y + mat.m[10] * z
    };
    float lenSq = out.x * out.x + out.y * out.y + out.z * out.z;
    if (lenSq > 1.0e-8f) {
        float invLen = 1.0f / std::sqrt(lenSq);
        out.x *= invLen;
        out.y *= invLen;
        out.z *= invLen;
    }
    return out;
}

static float cullDeterminant(const NativeQuad &quad, const Mat4 &projectedBoneMat) {
    Vec4 p0 = transformPoint(projectedBoneMat, quad.x[0], quad.y[0], quad.z[0], 1.0f);
    Vec4 p1 = transformPoint(projectedBoneMat, quad.x[1], quad.y[1], quad.z[1], 1.0f);
    Vec4 p2 = transformPoint(projectedBoneMat, quad.x[2], quad.y[2], quad.z[2], 1.0f);
    return p0.x * (p1.y * p2.w - p2.y * p1.w)
        - p1.x * (p0.y * p2.w - p2.y * p0.w)
        + p2.x * (p0.y * p1.w - p1.y * p0.w);
}

static uint8_t colorByte(float value) {
    float clamped = std::max(0.0f, std::min(1.0f, value));
    return static_cast<uint8_t>(clamped * 255.0f);
}

static int8_t normalByte(float value) {
    float clamped = std::max(-1.0f, std::min(1.0f, value));
    return static_cast<int8_t>(clamped * 127.0f);
}

static bool writePackedVertex(uint8_t *out, jlong capacity, int vertexIndex, const Vec4 &pos, float r, float g, float b,
                              float a, float u, float v, int overlay, int light, const Vec3 &normal) {
    jlong offset = static_cast<jlong>(vertexIndex) * PACKED_VERTEX_BYTES;
    if (offset < 0 || offset + PACKED_VERTEX_BYTES > capacity) return false;
    std::memcpy(out + offset, &pos.x, 4);
    std::memcpy(out + offset + 4, &pos.y, 4);
    std::memcpy(out + offset + 8, &pos.z, 4);
    out[offset + 12] = colorByte(r);
    out[offset + 13] = colorByte(g);
    out[offset + 14] = colorByte(b);
    out[offset + 15] = colorByte(a);
    std::memcpy(out + offset + 16, &u, 4);
    std::memcpy(out + offset + 20, &v, 4);
    std::memcpy(out + offset + 24, &overlay, 4);
    std::memcpy(out + offset + 28, &light, 4);
    out[offset + 32] = static_cast<uint8_t>(normalByte(normal.x));
    out[offset + 33] = static_cast<uint8_t>(normalByte(normal.y));
    out[offset + 34] = static_cast<uint8_t>(normalByte(normal.z));
    out[offset + 35] = 0;
    return true;
}

static bool writeFloatVertex(uint8_t *out, jlong capacity, int vertexIndex, const Vec4 &pos, float r, float g, float b,
                             float a, float u, float v, int overlay, int light, const Vec3 &normal) {
    jlong offset = static_cast<jlong>(vertexIndex) * FLOAT_VERTEX_BYTES;
    if (offset < 0 || offset + FLOAT_VERTEX_BYTES > capacity) return false;
    float values[14] = {
        pos.x, pos.y, pos.z,
        r, g, b, a,
        u, v,
        0.0f, 0.0f,
        normal.x, normal.y, normal.z
    };
    std::memcpy(out + offset, values, sizeof(values));
    std::memcpy(out + offset + 9 * 4, &overlay, 4);
    std::memcpy(out + offset + 10 * 4, &light, 4);
    return true;
}

static bool buildEvalOrder(NativeModel *model) {
    std::vector<char> state(model->bones.size(), 0);
    std::function<bool(int)> dfs = [&](int idx) {
        if (idx < 0 || idx >= static_cast<int>(model->bones.size())) return false;
        if (state[idx] == 1) return false;
        if (state[idx] == 2) return true;
        state[idx] = 1;
        int parent = model->bones[idx].parentIdx;
        if (parent != -1 && !dfs(parent)) return false;
        state[idx] = 2;
        model->evalOrder.push_back(idx);
        return true;
    };
    for (int i = 0; i < static_cast<int>(model->bones.size()); ++i) {
        if (!dfs(i)) return false;
    }
    return true;
}

static Mat4 buildLocalBoneTransform(const NativeBone &bone, const float *params) {
    float rx = params[0];
    float ry = params[1];
    float rz = params[2];
    float tx = params[3];
    float ty = params[4];
    float tz = params[5];
    float sx = params[6];
    float sy = params[7];
    float sz = params[8];

    float sinX = std::sin(rx);
    float cosX = std::cos(rx);
    float sinY = std::sin(ry);
    float cosY = std::cos(ry);
    float sinZ = std::sin(rz);
    float cosZ = std::cos(rz);

    float m00 = cosZ * cosY * sx;
    float m10 = sinZ * cosY * sx;
    float m20 = -sinY * sx;

    float m01 = (cosZ * sinY * sinX - sinZ * cosX) * sy;
    float m11 = (sinZ * sinY * sinX + cosZ * cosX) * sy;
    float m21 = cosY * sinX * sy;

    float m02 = (cosZ * sinY * cosX + sinZ * sinX) * sz;
    float m12 = (sinZ * sinY * cosX - cosZ * sinX) * sz;
    float m22 = cosY * cosX * sz;

    float pivotX = bone.pivotX * 0.0625f;
    float pivotY = bone.pivotY * 0.0625f;
    float pivotZ = bone.pivotZ * 0.0625f;
    float translateX = pivotX - tx * 0.0625f;
    float translateY = pivotY + ty * 0.0625f;
    float translateZ = pivotZ + tz * 0.0625f;

    Mat4 out;
    out.m[0] = m00;
    out.m[1] = m10;
    out.m[2] = m20;
    out.m[4] = m01;
    out.m[5] = m11;
    out.m[6] = m21;
    out.m[8] = m02;
    out.m[9] = m12;
    out.m[10] = m22;
    out.m[12] = translateX - (m00 * pivotX + m01 * pivotY + m02 * pivotZ);
    out.m[13] = translateY - (m10 * pivotX + m11 * pivotY + m12 * pivotZ);
    out.m[14] = translateZ - (m20 * pivotX + m21 * pivotY + m22 * pivotZ);
    return out;
}

static void updateLocalTransforms(NativeModel *model, const float *anim) {
    for (int idx : model->evalOrder) {
        NativeBone &bone = model->bones[idx];
        char visible = bone.parentIdx == -1 ? 1 : model->visible[bone.parentIdx];

        const float *params = anim + idx * 12;
        float animSx = params[6];
        float animSy = params[7];
        float animSz = params[8];

        if (animSx == 0.0f && animSy == 0.0f && animSz == 0.0f) visible = 0;

        Mat4 local = buildLocalBoneTransform(bone, params);
        if (bone.parentIdx != -1) {
            Mat4 parent = model->localTransforms[bone.parentIdx];
            parent.mul(local);
            local = parent;
        }
        model->localTransforms[idx] = local;
        model->visible[idx] = visible;
    }
}

static void updateGlobalTransforms(NativeModel *model, const Mat4 &rootPoseMat, const Mat4 *rootNormalMat,
                                   const Mat4 &projMat, int packedLight) {
    int glowLight = (15 << 4) | (15 << 20);
    for (size_t i = 0; i < model->bones.size(); ++i) {
        if (model->visible[i] == 0) continue;
        Mat4 global = rootPoseMat;
        global.mul(model->localTransforms[i]);
        model->globalTransforms[i] = global;

        Mat4 projected = projMat;
        projected.mul(global);
        model->projectedTransforms[i] = projected;

        if (rootNormalMat != nullptr) {
            Mat4 normal = *rootNormalMat;
            normal.mul(model->localTransforms[i].normalMatrix4x4());
            model->normalTransforms[i] = normal;
            model->lights[i] = model->bones[i].glow ? glowLight : packedLight;
        }
    }
}
}

extern "C" JNIEXPORT jint JNICALL Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nGetNativeAbiVersion(
    JNIEnv *, jclass) {
    return ABI_VERSION;
}

extern "C" JNIEXPORT jstring JNICALL Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nGetNativeBackendName(
    JNIEnv *env, jclass) {
    return env->NewStringUTF("scalar");
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nSelfTest(
    JNIEnv *env, jclass, jobject buffer) {
    if (buffer == nullptr) return JNI_FALSE;
    void *address = env->GetDirectBufferAddress(buffer);
    jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (address == nullptr || capacity < 16) return JNI_FALSE;

    Mat4 identity;
    Vec4 out = transformPoint(identity, 1.0f, 2.0f, 3.0f, 1.0f);
    return out.x == 1.0f && out.y == 2.0f && out.z == 3.0f && out.w == 1.0f ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nInitModelCache(
    JNIEnv *env, jclass, jobject buffer) {
    auto *data = static_cast<const uint8_t *>(env->GetDirectBufferAddress(buffer));
    jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (data == nullptr || capacity < 4) return 0;

    BufferReader reader(data, capacity);
    auto *model = new NativeModel();
    int boneCount = 0;
    if (!reader.readInt(boneCount) || boneCount <= 0 || boneCount > 4096) {
        delete model;
        return 0;
    }

    model->bones.resize(static_cast<size_t>(boneCount));
    model->localTransforms.resize(static_cast<size_t>(boneCount));
    model->visible.resize(static_cast<size_t>(boneCount));
    model->globalTransforms.resize(static_cast<size_t>(boneCount));
    model->projectedTransforms.resize(static_cast<size_t>(boneCount));
    model->normalTransforms.resize(static_cast<size_t>(boneCount));
    model->lights.resize(static_cast<size_t>(boneCount));

    for (int i = 0; i < boneCount; ++i) {
        NativeBone &bone = model->bones[i];
        uint8_t glow = 0;
        int cubeCount = 0;
        if (!reader.readInt(bone.parentIdx)
            || !reader.readInt(bone.partMask)
            || !reader.readByte(glow)
            || !reader.readFloat(bone.pivotX)
            || !reader.readFloat(bone.pivotY)
            || !reader.readFloat(bone.pivotZ)
            || !reader.readInt(cubeCount)) {
            delete model;
            return 0;
        }
        if (bone.parentIdx < -1 || bone.parentIdx >= boneCount || cubeCount < 0 || cubeCount > 65536) {
            delete model;
            return 0;
        }
        bone.glow = glow != 0;

        for (int c = 0; c < cubeCount; ++c) {
            uint8_t cullable = 0;
            int quadCount = 0;
            if (!reader.readByte(cullable) || !reader.readInt(quadCount) || quadCount < 0 || quadCount > 65536) {
                delete model;
                return 0;
            }
            for (int q = 0; q < quadCount; ++q) {
                NativeQuad quad {};
                quad.quadIdx = static_cast<int>(model->quads.size());
                quad.boneIdx = i;
                quad.cullable = cullable != 0;
                for (int v = 0; v < 4; ++v) {
                    if (!reader.readFloat(quad.x[v]) || !reader.readFloat(quad.y[v]) || !reader.readFloat(quad.z[v])) {
                        delete model;
                        return 0;
                    }
                }
                for (int v = 0; v < 4; ++v) {
                    if (!reader.readFloat(quad.u[v]) || !reader.readFloat(quad.v[v])) {
                        delete model;
                        return 0;
                    }
                }
                if (!reader.readFloat(quad.nx) || !reader.readFloat(quad.ny) || !reader.readFloat(quad.nz)) {
                    delete model;
                    return 0;
                }
                model->quads.push_back(quad);
            }
        }
    }

    if (!buildEvalOrder(model)) {
        delete model;
        return 0;
    }
    model->quads.shrink_to_fit();
    return reinterpret_cast<jlong>(model);
}

extern "C" JNIEXPORT void JNICALL Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nDestroyModelCache(
    JNIEnv *, jclass, jlong handle) {
    delete reinterpret_cast<NativeModel *>(handle);
}

extern "C" JNIEXPORT jint JNICALL Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nComputeModelVertices(
    JNIEnv *env, jclass, jlong handle, jobject outBuffer, jobject matrixBuffer, jobject animBuffer,
    jint renderPartMask, jint packedLight, jint packedOverlay, jfloat r, jfloat g, jfloat b, jfloat a,
    jboolean packTo36Bytes) {
    auto *model = reinterpret_cast<NativeModel *>(handle);
    if (model == nullptr || model->quads.empty()) return 0;

    auto *out = static_cast<uint8_t *>(env->GetDirectBufferAddress(outBuffer));
    auto *matrices = static_cast<float *>(env->GetDirectBufferAddress(matrixBuffer));
    auto *anim = static_cast<float *>(env->GetDirectBufferAddress(animBuffer));
    jlong outCapacity = env->GetDirectBufferCapacity(outBuffer);
    jlong matrixCapacity = env->GetDirectBufferCapacity(matrixBuffer);
    jlong animCapacity = env->GetDirectBufferCapacity(animBuffer);
    if (out == nullptr || matrices == nullptr || anim == nullptr) return -1;
    if (matrixCapacity < 64 * 4 || animCapacity < static_cast<jlong>(model->bones.size()) * 12 * 4) return -2;

    Mat4 rootPoseMat(matrices);
    const float *rootNormalArr = matrices + 16;
    Mat4 projMat(matrices + 32);

    Mat4 rootNormalMat;
    rootNormalMat.m[0] = rootNormalArr[0];
    rootNormalMat.m[1] = rootNormalArr[1];
    rootNormalMat.m[2] = rootNormalArr[2];
    rootNormalMat.m[4] = rootNormalArr[3];
    rootNormalMat.m[5] = rootNormalArr[4];
    rootNormalMat.m[6] = rootNormalArr[5];
    rootNormalMat.m[8] = rootNormalArr[6];
    rootNormalMat.m[9] = rootNormalArr[7];
    rootNormalMat.m[10] = rootNormalArr[8];

    updateLocalTransforms(model, anim);
    updateGlobalTransforms(model, rootPoseMat, &rootNormalMat, projMat, packedLight);

    int vertexCount = 0;
    for (const NativeQuad &quad : model->quads) {
        int boneIdx = quad.boneIdx;
        if (model->visible[boneIdx] == 0) continue;
        const NativeBone &bone = model->bones[boneIdx];
        if (renderPartMask != 0 && bone.partMask != renderPartMask && bone.partMask != 3) continue;
        const Mat4 &global = model->globalTransforms[boneIdx];
        if (quad.cullable && cullDeterminant(quad, model->projectedTransforms[boneIdx]) <= CULL_DETERMINANT_EPSILON) continue;

        Vec3 normal = transformNormal(model->normalTransforms[boneIdx], quad.nx, quad.ny, quad.nz);
        for (int i = 0; i < 4; ++i) {
            Vec4 pos = transformPoint(global, quad.x[i], quad.y[i], quad.z[i], 1.0f);
            bool wrote = packTo36Bytes
                ? writePackedVertex(out, outCapacity, vertexCount, pos, r, g, b, a, quad.u[i], quad.v[i], packedOverlay, model->lights[boneIdx], normal)
                : writeFloatVertex(out, outCapacity, vertexCount, pos, r, g, b, a, quad.u[i], quad.v[i], packedOverlay, model->lights[boneIdx], normal);
            if (!wrote) return -3;
            vertexCount++;
        }
    }
    return vertexCount;
}

extern "C" JNIEXPORT jint JNICALL Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nCollectVisibleQuadIds(
    JNIEnv *env, jclass, jlong handle, jobject outBuffer, jobject matrixBuffer, jobject animBuffer, jint renderPartMask) {
    auto *model = reinterpret_cast<NativeModel *>(handle);
    if (model == nullptr || model->quads.empty()) return 0;

    auto *out = static_cast<uint8_t *>(env->GetDirectBufferAddress(outBuffer));
    auto *matrices = static_cast<float *>(env->GetDirectBufferAddress(matrixBuffer));
    auto *anim = static_cast<float *>(env->GetDirectBufferAddress(animBuffer));
    jlong outCapacity = env->GetDirectBufferCapacity(outBuffer);
    jlong matrixCapacity = env->GetDirectBufferCapacity(matrixBuffer);
    jlong animCapacity = env->GetDirectBufferCapacity(animBuffer);
    if (out == nullptr || matrices == nullptr || anim == nullptr) return -1;
    if (matrixCapacity < 64 * 4 || animCapacity < static_cast<jlong>(model->bones.size()) * 12 * 4) return -2;

    Mat4 rootPoseMat(matrices);
    Mat4 projMat(matrices + 32);

    updateLocalTransforms(model, anim);
    updateGlobalTransforms(model, rootPoseMat, nullptr, projMat, 0);

    int quadCount = 0;
    for (const NativeQuad &quad : model->quads) {
        int boneIdx = quad.boneIdx;
        if (model->visible[boneIdx] == 0) continue;
        const NativeBone &bone = model->bones[boneIdx];
        if (renderPartMask != 0 && bone.partMask != renderPartMask && bone.partMask != 3) continue;
        if (quad.cullable && cullDeterminant(quad, model->projectedTransforms[boneIdx]) <= CULL_DETERMINANT_EPSILON) continue;

        jlong offset = static_cast<jlong>(quadCount) * 4;
        if (offset < 0 || offset + 4 > outCapacity) return -3;
        std::memcpy(out + offset, &quad.quadIdx, 4);
        quadCount++;
    }
    return quadCount;
}

static const JNINativeMethod gMethods[] = {
    {(char *) "nGetNativeAbiVersion", (char *) "()I",
     reinterpret_cast<void *>(Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nGetNativeAbiVersion)},
    {(char *) "nGetNativeBackendName", (char *) "()Ljava/lang/String;",
     reinterpret_cast<void *>(Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nGetNativeBackendName)},
    {(char *) "nSelfTest", (char *) "(Ljava/nio/ByteBuffer;)Z",
     reinterpret_cast<void *>(Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nSelfTest)},
    {(char *) "nInitModelCache", (char *) "(Ljava/nio/ByteBuffer;)J",
     reinterpret_cast<void *>(Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nInitModelCache)},
    {(char *) "nDestroyModelCache", (char *) "(J)V",
     reinterpret_cast<void *>(Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nDestroyModelCache)},
    {(char *) "nComputeModelVertices", (char *) "(JLjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;IIIFFFFZ)I",
     reinterpret_cast<void *>(Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nComputeModelVertices)},
    {(char *) "nCollectVisibleQuadIds", (char *) "(JLjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;I)I",
     reinterpret_cast<void *>(Java_com_elfmcys_yesstevemodel_geckolib3_geo_render_built_NativeModelCache_nCollectVisibleQuadIds)}
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass clazz = env->FindClass("com/elfmcys/yesstevemodel/geckolib3/geo/render/built/NativeModelCache");
    if (clazz == nullptr) return JNI_ERR;
    if (env->RegisterNatives(clazz, gMethods, static_cast<jint>(sizeof(gMethods) / sizeof(gMethods[0]))) < 0) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}
