package com.jagex.game.runetek5.graphics.model;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.utils.MathUtils;

public class Mesh {

    public int version = 12;
    public int numVertices;
    public int maxDepth;
    public int numFaces;
    public byte priority;
    public int numTextures;
    public byte[] texturesMapping;
    public int[] verticesX;
    public int[] verticesY;
    public int[] verticesZ;
    public short[] facesA;
    public short[] facesB;
    public short[] facesC;
    public int[] vertexGroups;
    public byte[] faceRenderType;
    public byte[] facesLayer;
    public byte[] facesAlpha;
    public int[] faceGroups;
    public short[] facesMaterial;
    public short[] facesColor;
    public byte[] facesTexture;
    public short[] textureFaceX;
    public short[] textureFaceY;
    public short[] textureFaceZ;
    public int[] texturesScaleX;
    public int[] texturesScaleY;
    public int[] texturesScaleZ;
    public byte[] texturesRotation;
    public byte[] texturesDirection;
    public int[] texturesSpeed;
    public int[] texturesUtrans;
    public int[] texturesVtrans;
    public EmissiveTriangle[] particleEmitters;
    public EffectiveVertex[] particleEffectors;
    public Billboard[] billboards;
    public short[] verticesSubmeshes;
    public short[] facesSubmeshes;

    public Mesh(byte[] data) {
        if (data[data.length - 1] == -1 && data[data.length - 2] == -1) {
            decodeNewFormat(data);
        } else {
            decodeOldFormat(data);
        }
    }

    public Mesh(Mesh[] meshes, int meshCount) {
        this.numVertices = 0;
        this.numFaces = 0;
        this.numTextures = 0;
        int amountOfEmitters = 0;
        int amountOfEffectiveVertices = 0;
        int amountOfBillboards = 0;
        boolean hasRenderType = false;
        boolean hasPriorities = false;
        boolean hasAlpha = false;
        boolean hasFaceTexture = false;
        boolean hasFaceMaterial = false;
        boolean hasFaceLabel = false;
        this.priority = -1;
        for (int meshIndex = 0; meshIndex < meshCount; meshIndex++) {
            Mesh mesh = meshes[meshIndex];
            if (mesh != null) {
                this.numVertices += mesh.numVertices;
                this.numFaces += mesh.numFaces;
                this.numTextures += mesh.numTextures;
                if (mesh.particleEmitters != null) {
                    amountOfEmitters += mesh.particleEmitters.length;
                }
                if (mesh.particleEffectors != null) {
                    amountOfEffectiveVertices += mesh.particleEffectors.length;
                }
                if (mesh.billboards != null) {
                    amountOfBillboards += mesh.billboards.length;
                }
                hasRenderType |= mesh.faceRenderType != null;
                if (mesh.facesLayer != null) {
                    hasPriorities = true;
                } else {
                    if (this.priority == -1) {
                        this.priority = mesh.priority;
                    }
                    if (this.priority != mesh.priority) {
                        hasPriorities = true;
                    }
                }
                hasAlpha |= mesh.facesAlpha != null;
                hasFaceTexture |= mesh.facesTexture != null;
                hasFaceMaterial |= mesh.facesMaterial != null;
                hasFaceLabel |= mesh.faceGroups != null;
            }
        }
        this.verticesX = new int[this.numVertices];
        this.verticesY = new int[this.numVertices];
        this.verticesZ = new int[this.numVertices];
        this.vertexGroups = new int[this.numVertices];
        this.verticesSubmeshes = new short[this.numVertices];
        this.facesA = new short[this.numFaces];
        this.facesB = new short[this.numFaces];
        this.facesC = new short[this.numFaces];
        if (hasRenderType) {
            this.faceRenderType = new byte[this.numFaces];
        }
        if (hasPriorities) {
            this.facesLayer = new byte[this.numFaces];
        }
        if (hasAlpha) {
            this.facesAlpha = new byte[this.numFaces];
        }
        if (hasFaceTexture) {
            this.facesTexture = new byte[this.numFaces];
        }
        this.facesColor = new short[this.numFaces];
        if (hasFaceMaterial) {
            this.facesMaterial = new short[this.numFaces];
        }
        if (hasFaceLabel) {
            this.faceGroups = new int[this.numFaces];
        }
        this.facesSubmeshes = new short[this.numFaces];
        if (this.numTextures > 0) {
            this.texturesMapping = new byte[this.numTextures];
            this.textureFaceX = new short[this.numTextures];
            this.textureFaceY = new short[this.numTextures];
            this.textureFaceZ = new short[this.numTextures];
            this.texturesScaleX = new int[this.numTextures];
            this.texturesScaleY = new int[this.numTextures];
            this.texturesScaleZ = new int[this.numTextures];
            this.texturesRotation = new byte[this.numTextures];
            this.texturesDirection = new byte[this.numTextures];
            this.texturesSpeed = new int[this.numTextures];
            this.texturesUtrans = new int[this.numTextures];
            this.texturesVtrans = new int[this.numTextures];
        }
        if (amountOfBillboards > 0) {
            this.billboards = new Billboard[amountOfBillboards];
        }
        if (amountOfEmitters > 0) {
            this.particleEmitters = new EmissiveTriangle[amountOfEmitters];
        }
        if (amountOfEffectiveVertices > 0) {
            this.particleEffectors = new EffectiveVertex[amountOfEffectiveVertices];
        }
        this.numVertices = 0;
        this.numFaces = 0;
        this.numTextures = 0;
        amountOfEmitters = 0;
        amountOfEffectiveVertices = 0;
        amountOfBillboards = 0;
        for (int meshIndex = 0; meshIndex < meshCount; meshIndex++) {
            short meshFlag = (short) (1 << meshIndex);
            Mesh mesh = meshes[meshIndex];
            if (mesh != null) {
                if (mesh.billboards != null) {
                    for (int billboardIndex = 0; billboardIndex < mesh.billboards.length; billboardIndex++) {
                        Billboard billboard = mesh.billboards[billboardIndex];
                        this.billboards[amountOfBillboards++] = billboard.translateToFace(billboard.appliesOnFace + this.numFaces);
                    }
                }
                for (int faceIndex = 0; faceIndex < mesh.numFaces; faceIndex++) {
                    if (hasRenderType && mesh.faceRenderType != null) {
                        this.faceRenderType[this.numFaces] = mesh.faceRenderType[faceIndex];
                    }
                    if (hasPriorities) {
                        if (mesh.facesLayer != null) {
                            this.facesLayer[this.numFaces] = mesh.facesLayer[faceIndex];
                        } else {
                            this.facesLayer[this.numFaces] = mesh.priority;
                        }
                    }
                    if (hasAlpha && mesh.facesAlpha != null) {
                        this.facesAlpha[this.numFaces] = mesh.facesAlpha[faceIndex];
                    }
                    if (hasFaceMaterial) {
                        if (mesh.facesMaterial != null) {
                            this.facesMaterial[this.numFaces] = mesh.facesMaterial[faceIndex];
                        } else {
                            this.facesMaterial[this.numFaces] = -1;
                        }
                    }
                    if (hasFaceLabel) {
                        if (mesh.faceGroups != null) {
                            this.faceGroups[this.numFaces] = mesh.faceGroups[faceIndex];
                        } else {
                            this.faceGroups[this.numFaces] = -1;
                        }
                    }
                    this.facesA[this.numFaces] = (short) connect(mesh, mesh.facesA[faceIndex], meshFlag);
                    this.facesB[this.numFaces] = (short) connect(mesh, mesh.facesB[faceIndex], meshFlag);
                    this.facesC[this.numFaces] = (short) connect(mesh, mesh.facesC[faceIndex], meshFlag);
                    this.facesSubmeshes[this.numFaces] = meshFlag;
                    this.facesColor[this.numFaces] = mesh.facesColor[faceIndex];
                    this.numFaces++;
                }
                if (mesh.particleEmitters != null) {
                    for (int emitterIndex = 0; emitterIndex < mesh.particleEmitters.length; emitterIndex++) {
                        int x = connect(mesh, mesh.particleEmitters[emitterIndex].faceA, meshFlag);
                        int y = connect(mesh, mesh.particleEmitters[emitterIndex].faceB, meshFlag);
                        int z = connect(mesh, mesh.particleEmitters[emitterIndex].faceC, meshFlag);
                        this.particleEmitters[amountOfEmitters] = mesh.particleEmitters[emitterIndex].transform(x, y, z);
                        amountOfEmitters++;
                    }
                }
                if (mesh.particleEffectors != null) {
                    for (int effectorIndex = 0; effectorIndex < mesh.particleEffectors.length; effectorIndex++) {
                        int connectedVertice = connect(mesh, mesh.particleEffectors[effectorIndex].appliesOnVertex, meshFlag);
                        this.particleEffectors[amountOfEffectiveVertices] = mesh.particleEffectors[effectorIndex].switchVertex(connectedVertice);
                        amountOfEffectiveVertices++;
                    }
                }
            }
        }
        int currentFace = 0;
        this.maxDepth = this.numVertices;
        for (int meshIndex = 0; meshIndex < meshCount; meshIndex++) {
            short meshFlag = (short) (1 << meshIndex);
            Mesh mesh = meshes[meshIndex];
            if (mesh != null) {
                for (int face = 0; face < mesh.numFaces; face++) {
                    if (hasFaceTexture) {
                        this.facesTexture[currentFace++] = (byte) (mesh.facesTexture != null && mesh.facesTexture[face] != -1 ? this.numTextures + mesh.facesTexture[face] : -1);
                    }
                }
                for (int textureIndex = 0; textureIndex < mesh.numTextures; textureIndex++) {
                    byte textureMapping = this.texturesMapping[this.numTextures] = mesh.texturesMapping[textureIndex];
                    if (textureMapping == 0) {
                        this.textureFaceX[this.numTextures] = (short) connect(mesh, mesh.textureFaceX[textureIndex], meshFlag);
                        this.textureFaceY[this.numTextures] = (short) connect(mesh, mesh.textureFaceY[textureIndex], meshFlag);
                        this.textureFaceZ[this.numTextures] = (short) connect(mesh, mesh.textureFaceZ[textureIndex], meshFlag);
                    }
                    if (textureMapping >= 1 && textureMapping <= 3) {
                        this.textureFaceX[this.numTextures] = mesh.textureFaceX[textureIndex];
                        this.textureFaceY[this.numTextures] = mesh.textureFaceY[textureIndex];
                        this.textureFaceZ[this.numTextures] = mesh.textureFaceZ[textureIndex];
                        this.texturesScaleX[this.numTextures] = mesh.texturesScaleX[textureIndex];
                        this.texturesScaleY[this.numTextures] = mesh.texturesScaleY[textureIndex];
                        this.texturesScaleZ[this.numTextures] = mesh.texturesScaleZ[textureIndex];
                        this.texturesRotation[this.numTextures] = mesh.texturesRotation[textureIndex];
                        this.texturesDirection[this.numTextures] = mesh.texturesDirection[textureIndex];
                        this.texturesSpeed[this.numTextures] = mesh.texturesSpeed[textureIndex];
                    }
                    if (textureMapping == 2) {
                        this.texturesUtrans[this.numTextures] = mesh.texturesUtrans[textureIndex];
                        this.texturesVtrans[this.numTextures] = mesh.texturesVtrans[textureIndex];
                    }
                    this.numTextures++;
                }
            }
        }
    }

    public Mesh(int verticeCount, int faceCount, int textureCount) {
        this.verticesX = new int[verticeCount];
        this.verticesY = new int[verticeCount];
        this.verticesZ = new int[verticeCount];
        this.vertexGroups = new int[verticeCount];
        this.facesA = new short[faceCount];
        this.facesB = new short[faceCount];
        this.facesC = new short[faceCount];
        this.faceRenderType = new byte[faceCount];
        this.facesLayer = new byte[faceCount];
        this.facesAlpha = new byte[faceCount];
        this.facesTexture = new byte[faceCount];
        this.facesColor = new short[faceCount];
        this.facesMaterial = new short[faceCount];
        this.faceGroups = new int[faceCount];
        if (textureCount > 0) {
            this.texturesMapping = new byte[textureCount];
            this.textureFaceX = new short[textureCount];
            this.textureFaceY = new short[textureCount];
            this.textureFaceZ = new short[textureCount];
            this.texturesScaleX = new int[textureCount];
            this.texturesScaleY = new int[textureCount];
            this.texturesScaleZ = new int[textureCount];
            this.texturesRotation = new byte[textureCount];
            this.texturesDirection = new byte[textureCount];
            this.texturesSpeed = new int[textureCount];
            this.texturesUtrans = new int[textureCount];
            this.texturesVtrans = new int[textureCount];
        }
    }

    public static Mesh fromResource(Js5ResourceProvider index, int fileId) {
        byte[] data = index.getFile(fileId, 0);
        return data == null ? null : new Mesh(data);
    }

    public int connect(Mesh mesh, int verticeToConnect, short meshIndex) {
        int x = mesh.verticesX[verticeToConnect];
        int y = mesh.verticesY[verticeToConnect];
        int z = mesh.verticesZ[verticeToConnect];
        for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
            if (x == this.verticesX[verticeIndex] && y == this.verticesY[verticeIndex] && z == this.verticesZ[verticeIndex]) {
                this.verticesSubmeshes[verticeIndex] |= meshIndex;
                return verticeIndex;
            }
        }
        this.verticesX[this.numVertices] = x;
        this.verticesY[this.numVertices] = y;
        this.verticesZ[this.numVertices] = z;
        this.verticesSubmeshes[this.numVertices] = meshIndex;
        this.vertexGroups[this.numVertices] = mesh.vertexGroups != null ? mesh.vertexGroups[verticeToConnect] : -1;
        return this.numVertices++;
    }

    public void decodeNewFormat(byte[] data) {
        Packet first = new Packet(data);
        Packet second = new Packet(data);
        Packet third = new Packet(data);
        Packet fourth = new Packet(data);
        Packet fifth = new Packet(data);
        Packet seventh = new Packet(data);
        Packet eight = new Packet(data);
        first.currentPosition = data.length - 23;
        this.numVertices = first.readUnsignedShort();
        this.numFaces = first.readUnsignedShort();
        this.numTextures = first.readUnsignedByte();
        int footerFlags = first.readUnsignedByte();
        boolean hasFaceTypes = (footerFlags & 0x1) == 1;
        boolean hasParticleEffects = (footerFlags & 0x2) == 2;
        boolean hasBillboards = (footerFlags & 0x4) == 4;
        boolean hasVersion = (footerFlags & 0x8) == 8;
        if (hasVersion) {
            first.currentPosition -= 7;
            this.version = first.readUnsignedByte();
            first.currentPosition += 6;
        }
        int modelPriority = first.readUnsignedByte();
        int hasFaceAlpha = first.readUnsignedByte();
        int hasFaceSkins = first.readUnsignedByte();
        int hasFaceTextures = first.readUnsignedByte();
        int hasVertexSkins = first.readUnsignedByte();
        int modelVerticesX = first.readUnsignedShort();
        int modelVerticesY = first.readUnsignedShort();
        int modelVerticesZ = first.readUnsignedShort();
        int faceIndices = first.readUnsignedShort();
        int textureIndices = first.readUnsignedShort();
        int simpleTextureFaceCount = 0;
        int complexTextureFaceCount = 0;
        int cubeTextureFaceCount = 0;
        if (this.numTextures > 0) {
            this.texturesMapping = new byte[this.numTextures];
            first.currentPosition = 0;
            for (int textureIndex = 0; textureIndex < this.numTextures; textureIndex++) {
                byte textureMapping = this.texturesMapping[textureIndex] = first.readByte();
                if (textureMapping == 0) {
                    simpleTextureFaceCount++;
                }
                if (textureMapping >= 1 && textureMapping <= 3) {
                    complexTextureFaceCount++;
                }
                if (textureMapping == 2) {
                    cubeTextureFaceCount++;
                }
            }
        }
        int index = this.numTextures;
        int vertexFlagsOffset = index;
        index += this.numVertices;
        int faceTypesOffset = index;
        if (hasFaceTypes) {
            index += this.numFaces;
        }
        int faceIndexTypesOffset = index;
        index += this.numFaces;
        int i_31 = index;
        if (modelPriority == 255) {
            index += this.numFaces;
        }
        int i_32 = index;
        if (hasFaceSkins == 1) {
            index += this.numFaces;
        }
        int i_33 = index;
        if (hasVertexSkins == 1) {
            index += this.numVertices;
        }
        int i_34 = index;
        if (hasFaceAlpha == 1) {
            index += this.numFaces;
        }
        int i_35 = index;
        index += faceIndices;
        int i_36 = index;
        if (hasFaceTextures == 1) {
            index += this.numFaces * 2;
        }
        int i_37 = index;
        index += textureIndices;
        int i_38 = index;
        index += this.numFaces * 2;
        int i_39 = index;
        index += modelVerticesX;
        int i_40 = index;
        index += modelVerticesY;
        int i_41 = index;
        index += modelVerticesZ;
        int simpleTexPmnOffset = index;
        index += simpleTextureFaceCount * 6;
        int i_43 = index;
        index += complexTextureFaceCount * 6;
        byte textureBytes = 6;
        if (this.version == 14) {
            textureBytes = 7;
        } else if (this.version >= 15) {
            textureBytes = 9;
        }
        int i_45 = index;
        index += complexTextureFaceCount * textureBytes;
        int i_46 = index;
        index += complexTextureFaceCount;
        int i_47 = index;
        index += complexTextureFaceCount;
        int i_48 = index;
        index = cubeTextureFaceCount * 2 + index + complexTextureFaceCount;
        this.verticesX = new int[this.numVertices];
        this.verticesY = new int[this.numVertices];
        this.verticesZ = new int[this.numVertices];
        this.facesA = new short[this.numFaces];
        this.facesB = new short[this.numFaces];
        this.facesC = new short[this.numFaces];
        if (hasVertexSkins == 1) {
            this.vertexGroups = new int[this.numVertices];
        }
        if (hasFaceTypes) {
            this.faceRenderType = new byte[this.numFaces];
        }
        if (modelPriority == 255) {
            this.facesLayer = new byte[this.numFaces];
        } else {
            this.priority = (byte) modelPriority;
        }
        if (hasFaceAlpha == 1) {
            this.facesAlpha = new byte[this.numFaces];
        }
        if (hasFaceSkins == 1) {
            this.faceGroups = new int[this.numFaces];
        }
        if (hasFaceTextures == 1) {
            this.facesMaterial = new short[this.numFaces];
        }
        if (hasFaceTextures == 1 && this.numTextures > 0) {
            this.facesTexture = new byte[this.numFaces];
        }
        this.facesColor = new short[this.numFaces];
        if (this.numTextures > 0) {
            this.textureFaceX = new short[this.numTextures];
            this.textureFaceY = new short[this.numTextures];
            this.textureFaceZ = new short[this.numTextures];
            if (complexTextureFaceCount > 0) {
                this.texturesScaleX = new int[complexTextureFaceCount];
                this.texturesScaleY = new int[complexTextureFaceCount];
                this.texturesScaleZ = new int[complexTextureFaceCount];
                this.texturesRotation = new byte[complexTextureFaceCount];
                this.texturesDirection = new byte[complexTextureFaceCount];
                this.texturesSpeed = new int[complexTextureFaceCount];
            }
            if (cubeTextureFaceCount > 0) {
                this.texturesUtrans = new int[cubeTextureFaceCount];
                this.texturesVtrans = new int[cubeTextureFaceCount];
            }
        }
        first.currentPosition = vertexFlagsOffset;
        second.currentPosition = i_39;
        third.currentPosition = i_40;
        fourth.currentPosition = i_41;
        fifth.currentPosition = i_33;
        int lastVerticeX = 0;
        int lastVerticeY = 0;
        int lastVerticeZ = 0;
        for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
            int offsetFlags = first.readUnsignedByte();
            int verticeXOffset = 0;
            if ((offsetFlags & 0x1) != 0) {
                verticeXOffset = second.readSignedSmart();
            }
            int verticeYOffset = 0;
            if ((offsetFlags & 0x2) != 0) {
                verticeYOffset = third.readSignedSmart();
            }
            int verticeZOffset = 0;
            if ((offsetFlags & 0x4) != 0) {
                verticeZOffset = fourth.readSignedSmart();
            }
            this.verticesX[verticeIndex] = lastVerticeX + verticeXOffset;
            this.verticesY[verticeIndex] = lastVerticeY + verticeYOffset;
            this.verticesZ[verticeIndex] = lastVerticeZ + verticeZOffset;
            lastVerticeX = this.verticesX[verticeIndex];
            lastVerticeY = this.verticesY[verticeIndex];
            lastVerticeZ = this.verticesZ[verticeIndex];
            if (hasVertexSkins == 1) {
                this.vertexGroups[verticeIndex] = fifth.readUnsignedByte();
            }
        }
        first.currentPosition = i_38;
        second.currentPosition = faceTypesOffset;
        third.currentPosition = i_31;
        fourth.currentPosition = i_34;
        fifth.currentPosition = i_32;
        seventh.currentPosition = i_36;
        eight.currentPosition = i_37;
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            this.facesColor[faceIndex] = (short) first.readUnsignedShort();
            if (hasFaceTypes) {
                this.faceRenderType[faceIndex] = second.readByte();
            }
            if (modelPriority == 255) {
                this.facesLayer[faceIndex] = third.readByte();
            }
            if (hasFaceAlpha == 1) {
                this.facesAlpha[faceIndex] = fourth.readByte();
            }
            if (hasFaceSkins == 1) {
                this.faceGroups[faceIndex] = fifth.readUnsignedByte();
            }
            if (hasFaceTextures == 1) {
                this.facesMaterial[faceIndex] = (short) (seventh.readUnsignedShort() - 1);
            }
            if (this.facesTexture != null) {
                if (this.facesMaterial[faceIndex] != -1) {
                    this.facesTexture[faceIndex] = (byte) (eight.readUnsignedByte() - 1);
                } else {
                    this.facesTexture[faceIndex] = -1;
                }
            }
        }
        this.maxDepth = -1;
        first.currentPosition = i_35;
        second.currentPosition = faceIndexTypesOffset;
        calculateMaxDepth(first, second);
        first.currentPosition = simpleTexPmnOffset;
        second.currentPosition = i_43;
        third.currentPosition = i_45;
        fourth.currentPosition = i_46;
        fifth.currentPosition = i_47;
        seventh.currentPosition = i_48;
        decodeTexturedTriangles(first, second, third, fourth, fifth, seventh);
        first.currentPosition = index;
        if (hasParticleEffects) {
            int emitterCount = first.readUnsignedByte();
            if (emitterCount > 0) {
                this.particleEmitters = new EmissiveTriangle[emitterCount];
                for (int emitterIndex = 0; emitterIndex < emitterCount; emitterIndex++) {
                    int particleId = first.readUnsignedShort();
                    int faceIndex = first.readUnsignedShort();
                    byte pri;
                    if (modelPriority == 255) {
                        pri = this.facesLayer[faceIndex];
                    } else {
                        pri = (byte) modelPriority;
                    }
                    this.particleEmitters[emitterIndex] = new EmissiveTriangle(particleId, this.facesA[faceIndex], this.facesB[faceIndex], this.facesC[faceIndex], pri);
                }
            }
            int effectorCount = first.readUnsignedByte();
            if (effectorCount > 0) {
                this.particleEffectors = new EffectiveVertex[effectorCount];
                for (int effectorIndex = 0; effectorIndex < effectorCount; effectorIndex++) {
                    int effectId = first.readUnsignedShort();
                    int appliesOnVertex = first.readUnsignedShort();
                    this.particleEffectors[effectorIndex] = new EffectiveVertex(effectId, appliesOnVertex);
                }
            }
        }
        if (hasBillboards) {
            int billboardCount = first.readUnsignedByte();
            if (billboardCount > 0) {
                this.billboards = new Billboard[billboardCount];
                for (int billboardIndex = 0; billboardIndex < billboardCount; billboardIndex++) {
                    int id = first.readUnsignedShort();
                    int face = first.readUnsignedShort();
                    int depth = first.readUnsignedByte();
                    byte distance = first.readByte();
                    this.billboards[billboardIndex] = new Billboard(id, face, depth, distance);
                }
            }
        }
    }

    public void calculateMaxDepth(Packet first, Packet second) {
        short a = 0;
        short b = 0;
        short c = 0;
        short tmp = 0;
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            int mappingType = second.readUnsignedByte();
            if (mappingType == 1) {
                a = (short) (first.readSignedSmart() + tmp);
                b = (short) (first.readSignedSmart() + a);
                c = (short) (first.readSignedSmart() + b);
                tmp = c;
                this.facesA[faceIndex] = a;
                this.facesB[faceIndex] = b;
                this.facesC[faceIndex] = c;
                if (a > this.maxDepth) {
                    this.maxDepth = a;
                }

                if (b > this.maxDepth) {
                    this.maxDepth = b;
                }

                if (c > this.maxDepth) {
                    this.maxDepth = c;
                }
            }
            if (mappingType == 2) {
                b = c;
                c = (short) (first.readSignedSmart() + tmp);
                tmp = c;
                this.facesA[faceIndex] = a;
                this.facesB[faceIndex] = b;
                this.facesC[faceIndex] = c;
                if (c > this.maxDepth) {
                    this.maxDepth = c;
                }
            }
            if (mappingType == 3) {
                a = c;
                c = (short) (first.readSignedSmart() + tmp);
                tmp = c;
                this.facesA[faceIndex] = a;
                this.facesB[faceIndex] = b;
                this.facesC[faceIndex] = c;
                if (c > this.maxDepth) {
                    this.maxDepth = c;
                }
            }
            if (mappingType == 4) {
                short s_9 = a;
                a = b;
                b = s_9;
                c = (short) (first.readSignedSmart() + tmp);
                tmp = c;
                this.facesA[faceIndex] = a;
                this.facesB[faceIndex] = s_9;
                this.facesC[faceIndex] = c;
                if (c > this.maxDepth) {
                    this.maxDepth = c;
                }
            }
        }
        this.maxDepth++;
    }

    public void decodeTexturedTriangles(Packet rsbytebuffer1, Packet rsbytebuffer2, Packet rsbytebuffer3, Packet rsbytebuffer4, Packet rsbytebuffer5, Packet rsbytebuffer6) {
        for (int textureIndex = 0; textureIndex < this.numTextures; textureIndex++) {
            int mappingType = this.texturesMapping[textureIndex] & 0xff;
            if (mappingType == 0) {
                this.textureFaceX[textureIndex] = (short) rsbytebuffer1.readUnsignedShort();
                this.textureFaceY[textureIndex] = (short) rsbytebuffer1.readUnsignedShort();
                this.textureFaceZ[textureIndex] = (short) rsbytebuffer1.readUnsignedShort();
            }

            if (mappingType == 1) {
                this.textureFaceX[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                this.textureFaceY[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                this.textureFaceZ[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                if (this.version < 15) {
                    this.texturesScaleX[textureIndex] = rsbytebuffer3.readUnsignedShort();
                    if (this.version < 14) {
                        this.texturesScaleY[textureIndex] = rsbytebuffer3.readUnsignedShort();
                    } else {
                        this.texturesScaleY[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    }

                    this.texturesScaleZ[textureIndex] = rsbytebuffer3.readUnsignedShort();
                } else {
                    this.texturesScaleX[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    this.texturesScaleY[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    this.texturesScaleZ[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                }

                this.texturesRotation[textureIndex] = rsbytebuffer4.readByte();
                this.texturesDirection[textureIndex] = rsbytebuffer5.readByte();
                this.texturesSpeed[textureIndex] = rsbytebuffer6.readByte();
            }

            if (mappingType == 2) {
                this.textureFaceX[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                this.textureFaceY[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                this.textureFaceZ[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                if (this.version < 15) {
                    this.texturesScaleX[textureIndex] = rsbytebuffer3.readUnsignedShort();
                    if (this.version < 14) {
                        this.texturesScaleY[textureIndex] = rsbytebuffer3.readUnsignedShort();
                    } else {
                        this.texturesScaleY[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    }

                    this.texturesScaleZ[textureIndex] = rsbytebuffer3.readUnsignedShort();
                } else {
                    this.texturesScaleX[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    this.texturesScaleY[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    this.texturesScaleZ[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                }

                this.texturesRotation[textureIndex] = rsbytebuffer4.readByte();
                this.texturesDirection[textureIndex] = rsbytebuffer5.readByte();
                this.texturesSpeed[textureIndex] = rsbytebuffer6.readByte();
                this.texturesUtrans[textureIndex] = rsbytebuffer6.readByte();
                this.texturesVtrans[textureIndex] = rsbytebuffer6.readByte();
            }

            if (mappingType == 3) {
                this.textureFaceX[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                this.textureFaceY[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                this.textureFaceZ[textureIndex] = (short) rsbytebuffer2.readUnsignedShort();
                if (this.version < 15) {
                    this.texturesScaleX[textureIndex] = rsbytebuffer3.readUnsignedShort();
                    if (this.version < 14) {
                        this.texturesScaleY[textureIndex] = rsbytebuffer3.readUnsignedShort();
                    } else {
                        this.texturesScaleY[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    }

                    this.texturesScaleZ[textureIndex] = rsbytebuffer3.readUnsignedShort();
                } else {
                    this.texturesScaleX[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    this.texturesScaleY[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                    this.texturesScaleZ[textureIndex] = rsbytebuffer3.read24BitUnsignedInteger();
                }

                this.texturesRotation[textureIndex] = rsbytebuffer4.readByte();
                this.texturesDirection[textureIndex] = rsbytebuffer5.readByte();
                this.texturesSpeed[textureIndex] = rsbytebuffer6.readByte();
            }
        }
    }

    public int addVertex(int x, int y, int z) {
        for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
            if (this.verticesX[verticeIndex] == x && y == this.verticesY[verticeIndex] && z == this.verticesZ[verticeIndex]) {
                return verticeIndex;
            }
        }

        this.verticesX[this.numVertices] = x;
        this.verticesY[this.numVertices] = y;
        this.verticesZ[this.numVertices] = z;
        this.maxDepth = this.numVertices + 1;
        return this.numVertices++;
    }

    public int addFace(int faceA, int faceB, int faceC, byte faceRenderType, byte faceTexture, short faceColor, byte faceAlpha, short faceMaterial) {
        this.facesA[this.numFaces] = (short) faceA;
        this.facesB[this.numFaces] = (short) faceB;
        this.facesC[this.numFaces] = (short) faceC;
        this.faceRenderType[this.numFaces] = faceRenderType;
        this.facesTexture[this.numFaces] = faceTexture;
        this.facesColor[this.numFaces] = faceColor;
        this.facesAlpha[this.numFaces] = faceAlpha;
        this.facesMaterial[this.numFaces] = faceMaterial;
        return this.numFaces++;
    }

    public byte addSunTexture() {
        if (this.numTextures >= 255) {
            throw new IllegalStateException();
        } 
    	this.texturesMapping[this.numTextures] = 3;
    	this.textureFaceX[this.numTextures] = 0;
    	this.textureFaceY[this.numTextures] = 32767;
    	this.textureFaceZ[this.numTextures] = 0;
    	this.texturesScaleX[this.numTextures] = 1024;
    	this.texturesScaleY[this.numTextures] = 1024;
    	this.texturesScaleZ[this.numTextures] = 1024;
    	this.texturesRotation[this.numTextures] = 0;
    	this.texturesDirection[this.numTextures] = 0;
    	this.texturesSpeed[this.numTextures] = 0;
        return (byte) (this.numTextures++);
    }

    /**
     * @author Tamatea
     * https://github.com/tamateea/RuneBlend
     */
    public int[][] createVertexGroups(boolean fromNumVertices) {
        int[] vertexGroupCount = new int[256];
        int count = 0;
        int max = fromNumVertices ? this.numVertices : this.maxDepth;

        for (int v = 0; v < max; v++) {
            int group = this.vertexGroups[v];
            if (group >= 0) {
                vertexGroupCount[group]++;
                if (group > count) {
                    count = group;
                }
            }
        }

        int[][] groupedVertices = new int[count + 1][];

        for (int group = 0; group <= count; group++) {
            groupedVertices[group] = new int[vertexGroupCount[group]];
            vertexGroupCount[group] = 0;
        }

        for (int v = 0; v < max; v++) {
            int group = this.vertexGroups[v];
            if (group >= 0) {
                groupedVertices[group][vertexGroupCount[group]++] = v;
            }
        }

        return groupedVertices;
    }

    /**
     * @author Tamatea
     * https://github.com/tamateea/RuneBlend
     */
    public int[][] createFaceGroups() {
        int[] faceGroupCount = new int[256];
        int count = 0;

        for (int face = 0; face < this.numFaces; face++) {
            int group = this.faceGroups[face];
            if (group >= 0) {
                faceGroupCount[group]++;
                if (group > count) {
                    count = group;
                }
            }
        }

        int[][] groupedFaces = new int[count + 1][];

        for (int group = 0; group <= count; group++) {
            groupedFaces[group] = new int[faceGroupCount[group]];
            faceGroupCount[group] = 0;
        }

        for (int face = 0; face < this.numFaces; face++) {
            int group = this.faceGroups[face];
            if (group >= 0) {
                groupedFaces[group][faceGroupCount[group]++] = face;
            }
        }

        return groupedFaces;
    }

    public int[][] createBillboardGroups() {
        int[] billboardGroupCount = new int[256];
        int count = 0;

        for (int billboard = 0; billboard < this.billboards.length; billboard++) {
            int group = this.billboards[billboard].depth;
            if (group >= 0) {
                billboardGroupCount[group]++;
                if (group > count) {
                    count = group;
                }
            }
        }

        int[][] groupedBillboards = new int[count + 1][];

        for (int group = 0; group <= count; group++) {
            groupedBillboards[group] = new int[billboardGroupCount[group]];
            billboardGroupCount[group] = 0;
        }

        for (int billboard = 0; billboard < this.billboards.length; billboard++) {
            int group = this.billboards[billboard].depth;
            if (group >= 0) {
                groupedBillboards[group][billboardGroupCount[group]++] = billboard;
            }
        }

        return groupedBillboards;
    }

    public void recolor(short src, short dst) {
        for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
            if (this.facesColor[faceIndex] == src) {
                this.facesColor[faceIndex] = dst;
            }
        }
    }

    public void retexture(short src, short dst) {
        if (this.facesMaterial != null) {
            for (int faceIndex = 0; faceIndex < this.numFaces; faceIndex++) {
                if (this.facesMaterial[faceIndex] == src) {
                    this.facesMaterial[faceIndex] = dst;
                }
            }
        }

    }

    public void rotateWithoutNormals(int x, int y, int z) {
        if (z != 0) {
            int i_4 = MathUtils.SINE[z];
            int i_5 = MathUtils.COSINE[z];

            for (int v = 0; v < this.numVertices; v++) {
            	int i_7 = i_4 * this.verticesY[v] + i_5 * this.verticesX[v] >> 14;
                this.verticesY[v] = i_5 * this.verticesY[v] - i_4 * this.verticesX[v] >> 14;
                this.verticesX[v] = i_7;
            }
        }

        if (x != 0) {
        	int i_4 = MathUtils.SINE[x];
        	int i_5 = MathUtils.COSINE[x];

            for (int v = 0; v < this.numVertices; v++) {
            	int i_7 = i_5 * this.verticesY[v] - i_4 * this.verticesZ[v] >> 14;
                this.verticesZ[v] = i_4 * this.verticesY[v] + i_5 * this.verticesZ[v] >> 14;
                this.verticesY[v] = i_7;
            }
        }

        if (y != 0) {
        	int i_4 = MathUtils.SINE[y];
        	int i_5 = MathUtils.COSINE[y];

            for (int v = 0; v < this.numVertices; v++) {
            	int i_7 = i_4 * this.verticesZ[v] + i_5 * this.verticesX[v] >> 14;
                this.verticesZ[v] = i_5 * this.verticesZ[v] - i_4 * this.verticesX[v] >> 14;
                this.verticesX[v] = i_7;
            }
        }

    }

    public void decodeOldFormat(byte[] bytes_1) {
        boolean hasFaceTypes = false;
        boolean hasMaterials = false;
        Packet rsbytebuffer4 = new Packet(bytes_1);
        Packet rsbytebuffer5 = new Packet(bytes_1);
        Packet rsbytebuffer6 = new Packet(bytes_1);
        Packet rsbytebuffer7 = new Packet(bytes_1);
        Packet rsbytebuffer8 = new Packet(bytes_1);
        rsbytebuffer4.currentPosition = bytes_1.length - 18;
        this.numVertices = rsbytebuffer4.readUnsignedShort();
        this.numFaces = rsbytebuffer4.readUnsignedShort();
        this.numTextures = rsbytebuffer4.readUnsignedByte();
        int i_9 = rsbytebuffer4.readUnsignedByte();
        int i_10 = rsbytebuffer4.readUnsignedByte();
        int i_11 = rsbytebuffer4.readUnsignedByte();
        int i_12 = rsbytebuffer4.readUnsignedByte();
        int i_13 = rsbytebuffer4.readUnsignedByte();
        int i_14 = rsbytebuffer4.readUnsignedShort();
        int i_15 = rsbytebuffer4.readUnsignedShort();
        int i_16 = rsbytebuffer4.readUnsignedShort();
        int i_17 = rsbytebuffer4.readUnsignedShort();
        byte b_18 = 0;
        int i_42 = b_18 + this.numVertices;
        int i_20 = i_42;
        i_42 += this.numFaces;
        int i_21 = i_42;
        if (i_10 == 255) {
            i_42 += this.numFaces;
        }

        int i_22 = i_42;
        if (i_12 == 1) {
            i_42 += this.numFaces;
        }

        int i_23 = i_42;
        if (i_9 == 1) {
            i_42 += this.numFaces;
        }

        int i_24 = i_42;
        if (i_13 == 1) {
            i_42 += this.numVertices;
        }

        int i_25 = i_42;
        if (i_11 == 1) {
            i_42 += this.numFaces;
        }

        int i_26 = i_42;
        i_42 += i_17;
        int i_27 = i_42;
        i_42 += this.numFaces * 2;
        int i_28 = i_42;
        i_42 += this.numTextures * 6;
        int i_29 = i_42;
        i_42 += i_14;
        int i_30 = i_42;
        i_42 += i_15;
        int i_10000 = i_42 + i_16;
        this.verticesX = new int[this.numVertices];
        this.verticesY = new int[this.numVertices];
        this.verticesZ = new int[this.numVertices];
        this.facesA = new short[this.numFaces];
        this.facesB = new short[this.numFaces];
        this.facesC = new short[this.numFaces];
        if (this.numTextures > 0) {
            this.texturesMapping = new byte[this.numTextures];
            this.textureFaceX = new short[this.numTextures];
            this.textureFaceY = new short[this.numTextures];
            this.textureFaceZ = new short[this.numTextures];
        }

        if (i_13 == 1) {
            this.vertexGroups = new int[this.numVertices];
        }

        if (i_9 == 1) {
            this.faceRenderType = new byte[this.numFaces];
            this.facesTexture = new byte[this.numFaces];
            this.facesMaterial = new short[this.numFaces];
        }

        if (i_10 == 255) {
            this.facesLayer = new byte[this.numFaces];
        } else {
            this.priority = (byte) i_10;
        }

        if (i_11 == 1) {
            this.facesAlpha = new byte[this.numFaces];
        }

        if (i_12 == 1) {
            this.faceGroups = new int[this.numFaces];
        }

        this.facesColor = new short[this.numFaces];
        rsbytebuffer4.currentPosition = b_18;
        rsbytebuffer5.currentPosition = i_29;
        rsbytebuffer6.currentPosition = i_30;
        rsbytebuffer7.currentPosition = i_42;
        rsbytebuffer8.currentPosition = i_24;
        int i_32 = 0;
        int i_33 = 0;
        int i_34 = 0;

        int i_35;
        int i_36;
        int i_39;
        for (i_35 = 0; i_35 < this.numVertices; i_35++) {
            i_36 = rsbytebuffer4.readUnsignedByte();
            int i_37 = 0;
            if ((i_36 & 0x1) != 0) {
                i_37 = rsbytebuffer5.readSignedSmart();
            }

            int i_38 = 0;
            if ((i_36 & 0x2) != 0) {
                i_38 = rsbytebuffer6.readSignedSmart();
            }

            i_39 = 0;
            if ((i_36 & 0x4) != 0) {
                i_39 = rsbytebuffer7.readSignedSmart();
            }

            this.verticesX[i_35] = i_32 + i_37;
            this.verticesY[i_35] = i_33 + i_38;
            this.verticesZ[i_35] = i_34 + i_39;
            i_32 = this.verticesX[i_35];
            i_33 = this.verticesY[i_35];
            i_34 = this.verticesZ[i_35];
            if (i_13 == 1) {
                this.vertexGroups[i_35] = rsbytebuffer8.readUnsignedByte();
            }
        }

        rsbytebuffer4.currentPosition = i_27;
        rsbytebuffer5.currentPosition = i_23;
        rsbytebuffer6.currentPosition = i_21;
        rsbytebuffer7.currentPosition = i_25;
        rsbytebuffer8.currentPosition = i_22;

        for (i_35 = 0; i_35 < this.numFaces; i_35++) {
            this.facesColor[i_35] = (short) rsbytebuffer4.readUnsignedShort();
            if (i_9 == 1) {
                i_36 = rsbytebuffer5.readUnsignedByte();
                if ((i_36 & 0x1) == 1) {
                    this.faceRenderType[i_35] = 1;
                    hasFaceTypes = true;
                } else {
                    this.faceRenderType[i_35] = 0;
                }

                if ((i_36 & 0x2) == 2) {
                    this.facesTexture[i_35] = (byte) (i_36 >> 2);
                    this.facesMaterial[i_35] = this.facesColor[i_35];
                    this.facesColor[i_35] = 127;
                    if (this.facesMaterial[i_35] != -1) {
                        hasMaterials = true;
                    }
                } else {
                    this.facesTexture[i_35] = -1;
                    this.facesMaterial[i_35] = -1;
                }
            }

            if (i_10 == 255) {
                this.facesLayer[i_35] = rsbytebuffer6.readByte();
            }

            if (i_11 == 1) {
                this.facesAlpha[i_35] = rsbytebuffer7.readByte();
            }

            if (i_12 == 1) {
                this.faceGroups[i_35] = rsbytebuffer8.readUnsignedByte();
            }
        }

        this.maxDepth = -1;
        rsbytebuffer4.currentPosition = i_26;
        rsbytebuffer5.currentPosition = i_20;
        short s_43 = 0;
        short s_44 = 0;
        short s_45 = 0;
        short s_46 = 0;

        int i_40;
        for (i_39 = 0; i_39 < this.numFaces; i_39++) {
            i_40 = rsbytebuffer5.readUnsignedByte();
            if (i_40 == 1) {
                s_43 = (short) (rsbytebuffer4.readSignedSmart() + s_46);
                s_44 = (short) (rsbytebuffer4.readSignedSmart() + s_43);
                s_45 = (short) (rsbytebuffer4.readSignedSmart() + s_44);
                s_46 = s_45;
                this.facesA[i_39] = s_43;
                this.facesB[i_39] = s_44;
                this.facesC[i_39] = s_45;
                if (s_43 > this.maxDepth) {
                    this.maxDepth = s_43;
                }

                if (s_44 > this.maxDepth) {
                    this.maxDepth = s_44;
                }

                if (s_45 > this.maxDepth) {
                    this.maxDepth = s_45;
                }
            }

            if (i_40 == 2) {
                s_44 = s_45;
                s_45 = (short) (rsbytebuffer4.readSignedSmart() + s_46);
                s_46 = s_45;
                this.facesA[i_39] = s_43;
                this.facesB[i_39] = s_44;
                this.facesC[i_39] = s_45;
                if (s_45 > this.maxDepth) {
                    this.maxDepth = s_45;
                }
            }

            if (i_40 == 3) {
                s_43 = s_45;
                s_45 = (short) (rsbytebuffer4.readSignedSmart() + s_46);
                s_46 = s_45;
                this.facesA[i_39] = s_43;
                this.facesB[i_39] = s_44;
                this.facesC[i_39] = s_45;
                if (s_45 > this.maxDepth) {
                    this.maxDepth = s_45;
                }
            }

            if (i_40 == 4) {
                short s_41 = s_43;
                s_43 = s_44;
                s_44 = s_41;
                s_45 = (short) (rsbytebuffer4.readSignedSmart() + s_46);
                s_46 = s_45;
                this.facesA[i_39] = s_43;
                this.facesB[i_39] = s_41;
                this.facesC[i_39] = s_45;
                if (s_45 > this.maxDepth) {
                    this.maxDepth = s_45;
                }
            }
        }

        this.maxDepth++;
        rsbytebuffer4.currentPosition = i_28;

        for (i_39 = 0; i_39 < this.numTextures; i_39++) {
            this.texturesMapping[i_39] = 0;
            this.textureFaceX[i_39] = (short) rsbytebuffer4.readUnsignedShort();
            this.textureFaceY[i_39] = (short) rsbytebuffer4.readUnsignedShort();
            this.textureFaceZ[i_39] = (short) rsbytebuffer4.readUnsignedShort();
        }

        if (this.facesTexture != null) {
            boolean hasTextures = false;

            for (i_40 = 0; i_40 < this.numFaces; i_40++) {
                int textureIndex = this.facesTexture[i_40] & 0xff;
                if (textureIndex != 255) {
                    if (this.facesA[i_40] == (this.textureFaceX[textureIndex] & 0xffff) && this.facesB[i_40] == (this.textureFaceY[textureIndex] & 0xffff) && this.facesC[i_40] == (this.textureFaceZ[textureIndex] & 0xffff)) {
                        this.facesTexture[i_40] = -1;
                    } else {
                        hasTextures = true;
                    }
                }
            }

            if (!hasTextures) {
                this.facesTexture = null;
            }
        }

        if (!hasMaterials) {
            this.facesMaterial = null;
        }

        if (!hasFaceTypes) {
            this.faceRenderType = null;
        }

    }

    public void upscale() {
        for (int verticeIndex = 0; verticeIndex < this.numVertices; verticeIndex++) {
            this.verticesX[verticeIndex] <<= 2;
            this.verticesY[verticeIndex] <<= 2;
            this.verticesZ[verticeIndex] <<= 2;
        }

        if (this.numTextures > 0 && this.texturesScaleX != null) {
            for (int textureIndex = 0; textureIndex < this.texturesScaleX.length; textureIndex++) {
                this.texturesScaleX[textureIndex] <<= 2;
                this.texturesScaleY[textureIndex] <<= 2;
                if (this.texturesMapping[textureIndex] != 1) {
                    this.texturesScaleZ[textureIndex] <<= 2;
                }
            }
        }
    }

    public void translate(int deltaX, int deltaY, int deltaZ) {
        for (int i = 0; i < this.numVertices; i++) {
            this.verticesX[i] += deltaX;
            this.verticesY[i] += deltaY;
            this.verticesZ[i] += deltaZ;
        }
    }
}
