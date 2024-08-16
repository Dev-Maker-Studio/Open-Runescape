package com.jagex.game.runetek5.graphics.model;

import com.jagex.game.runetek5.config.sequence.frameset.AnimBase;
import com.jagex.game.runetek5.config.sequence.frameset.AnimFrame;
import com.jagex.game.runetek5.config.sequence.frameset.FrameSet;
import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.graphics.texture.TextureTransformationMatrix;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.utils.ArrayUtils;

public abstract class Model {

    protected boolean isLocked;
    
    /**
     * *****************************************************************
     * 					(TODO:): GPU handling
     * *****************************************************************
     */
        
    public abstract void uploadToGpu();
    
    /**
     * *****************************************************************
     * 					(TODO:) Multi threading
     * *****************************************************************
     */
    
    public abstract void awaitLock();

    public abstract void unlock();
    
    /**
     * *****************************************************************
     * 					(TODO:): Drawing
     * *****************************************************************
     */

    public abstract boolean calculateProjection(int camX, int camY, Matrix43 modelMatrix, boolean notRendered, int sizeShift);
    
    public abstract void drawMatrixThreaded(Matrix43 projectionMatrix, BoundingBoxPolygon boundingBox, int rasterizerFunctionMask);
    
    /**
     * *****************************************************************
     * 					(TODO:) Adjustment
     * *****************************************************************
     */
    
    public abstract void translate(int x, int y, int z);

    public abstract void mirror();

    public abstract void scale(int x, int y, int z);
    
    public abstract void setContrast(int contrast);
    
    /**
     * [J]
     * @param src
     * @param dst
     */
    public abstract void recolor(short src, short dst);
    
    /**
     * [J]
     * @param src
     * @param dst
     */
    public abstract void rematerial(short src, short dst);

    public abstract void tint(int hue, int saturation, int lightness, int opacity);
    
    public abstract void applyShading(int functionMask);
    
    public abstract void setAmbient(int ambient);

    public abstract void applyEnvironmentAlpha(byte alpha, byte[] transparency);
    
    public abstract Shadow createShadowSprite(Shadow shadow);
    
    public abstract void applyShading(Model model, int x, int y, int z, boolean renderTypeAdjustment);
    
    public abstract Model copy(byte copyType, int functionMask, boolean recalculateBeforeCopy);

    public abstract void offsetSubmesh(Matrix43 var1, int functionMask, boolean var3);
    
    public abstract void updateParticlePosition(Matrix43 projectionMatrix);
    
    /**
     * *****************************************************************
     * 					(TODO:) Getters
     * *****************************************************************
     */

    public abstract int getSize2d();

    public abstract int getSize3d();

    public abstract int getMinX();
    
    public abstract int getMaxX();
    
    public abstract int getMinY();
    
    public abstract int getMaxY();
    
    public abstract int getMinZ();
    
    public abstract int getMaxZ();

    public abstract int getFunctionMask();
    
    public abstract int getAmbient();
    
    public abstract int getContrast();

    public abstract byte[] getAlpha();
    
    public abstract EmissiveTriangle[] getEmissiveTriangles();
    
	public abstract EffectiveVertex[] getEffectiveVertices();
	
    public abstract boolean areTexturesCached();

    public abstract boolean hasAnimatedTexture();
    
    public abstract boolean accountForLayers();
    
    /**
     * *****************************************************************
     * 					(TODO:) Rotate
     * *****************************************************************
     */

    public abstract void rotateWithNormals(int rotation);

    public abstract void zaxisRotateWithoutNormals(int rotation);
    
    public abstract void xaxisRotateWithoutNormals(int rotation);
    
    public abstract void yaxisRotateWithoutNormals(int rotation);

    
    /**
     * *****************************************************************
     * 					(TODO:) Animate
     * *****************************************************************
     */
    
    public abstract boolean startAnimation();
    
    public abstract void endAnimation();
    
    public abstract void animatePartialtransform(int baseTransformType, int[] label, int x, int y, int z, int rotation, boolean lights);
    
    public abstract void animateTransform(int baseTransformType, int[] label, int x, int y, int z, boolean lights, int submesh, int[] verticesData);
    
    public void animateUnrotatedSubmesh(FrameSet firstFrameset, int firstFrameId, FrameSet nextFrameset, int nextFrameId, int cycle, int length, int submesh, boolean lights, int[] alwaysNull) {
        if (firstFrameId != -1) {
            awaitLock();
            if (!startAnimation()) {
                unlock();
            } else {
                AnimFrame firstFrame = firstFrameset.frames[firstFrameId];
                AnimBase firstBase = firstFrame.base;
                AnimFrame nextFrame = null;
                if (nextFrameset != null) {
                    nextFrame = nextFrameset.frames[nextFrameId];
                    if (firstBase != nextFrame.base) {
                        nextFrame = null;
                    }
                }
                interpolateFrames(firstBase, firstFrame, nextFrame, cycle, length, 0, null, false, lights, submesh, alwaysNull);
                endAnimation();
                unlock();
            }
        }
    }
    
    public void animateModelFrame(FrameSet firstFrameset, int firstFrameId, FrameSet nextFrameset, int nextFrameId, int cycle, int length, int rotation, boolean lights) {
        if (firstFrameId != -1) {
            awaitLock();
            if (!startAnimation()) {
                unlock();
            } else {
                AnimFrame firstFrame = firstFrameset.frames[firstFrameId];
                AnimBase firstBase = firstFrame.base;
                AnimFrame nextFrame = null;
                if (nextFrameset != null) {
                    nextFrame = nextFrameset.frames[nextFrameId];
                    if (firstBase != nextFrame.base) {
                        nextFrame = null;
                    }
                }
                interpolateFrames(firstBase, firstFrame, nextFrame, cycle, length, rotation, null, false, lights, 65535, null);
                endAnimation();
                unlock();
            }
        }
    }

    public void doubleAnimation(FrameSet animatingSeqCurrentFrameset, int animatingSeqCurrentFrameId, FrameSet animatingSeqNextFrameset, int animatingSeqNextFrameId, int animatingSeqCycle, int animatingSeqDuration, FrameSet idleSeqCurrentFrameset, int idleSeqCurrentFrameId, FrameSet idleSeqNextFrameset, int idleSeqNextFrameId, int idleSeqCycle, int idleSeqDuration, boolean[] animateLabels, boolean lights) {
        if (animatingSeqCurrentFrameId != -1) {
            if (animateLabels != null && idleSeqCurrentFrameId != -1) {
                awaitLock();
                if (!startAnimation()) {
                    unlock();
                } else {
                    AnimFrame animatingSeqCurrentFrame = animatingSeqCurrentFrameset.frames[animatingSeqCurrentFrameId];
                    AnimBase animatingSeqBase = animatingSeqCurrentFrame.base;
                    AnimFrame animatingSeqNextFrame = null;
                    if (animatingSeqNextFrameset != null) {
                        animatingSeqNextFrame = animatingSeqNextFrameset.frames[animatingSeqNextFrameId];
                        if (animatingSeqBase != animatingSeqNextFrame.base) {
                            animatingSeqNextFrame = null;
                        }
                    }
                    interpolateFrames(animatingSeqBase, animatingSeqCurrentFrame, animatingSeqNextFrame, animatingSeqCycle, animatingSeqDuration, 0, animateLabels, false, lights, 65535, null);
                    AnimFrame idleSeqCurrentFrame = idleSeqCurrentFrameset.frames[idleSeqCurrentFrameId];
                    AnimFrame idleSeqNextFrame = null;
                    if (idleSeqNextFrameset != null) {
                        idleSeqNextFrame = idleSeqNextFrameset.frames[idleSeqNextFrameId];
                        if (animatingSeqBase != idleSeqNextFrame.base) {
                            idleSeqNextFrame = null;
                        }
                    }
                    animatePartialtransform(0, new int[0], 0, 0, 0, 0, lights);
                    interpolateFrames(idleSeqCurrentFrame.base, idleSeqCurrentFrame, idleSeqNextFrame, idleSeqCycle, idleSeqDuration, 0, animateLabels, true, lights, 65535, null);
                    endAnimation();
                    unlock();
                }
            } else {
                animateModelFrame(animatingSeqCurrentFrameset, animatingSeqCurrentFrameId, animatingSeqNextFrameset, animatingSeqNextFrameId, animatingSeqCycle, animatingSeqDuration, 0, lights);
            }
        }
    }
    
    public void interpolateFrames(AnimBase base, AnimFrame currentFrame, AnimFrame nextFrame, int cycle, int length, int rotation, boolean[] animateLabels, boolean condition, boolean lights, int submesh, int[] alwaysNull) {
        int currentTransformation;
        if (nextFrame != null && cycle != 0) {
            currentTransformation = 0;
            int nextTransformation = 0;
            for (int transformIndex = 0; transformIndex < base.numTransformation; transformIndex++) {
                boolean transformCurrent = currentTransformation < currentFrame.numTransformations && transformIndex == currentFrame.baseIndices[currentTransformation];
                boolean transformNext = nextTransformation < nextFrame.numTransformations && transformIndex == nextFrame.baseIndices[nextTransformation];
                if (transformCurrent || transformNext) {
                    if (animateLabels != null && animateLabels[transformIndex] != condition && base.transformationType[transformIndex] != 0) {
                        if (transformCurrent) {
                            currentTransformation++;
                        }
                        if (transformNext) {
                            nextTransformation++;
                        }
                    } else {
                        short defaultTransformState = 0;
                        int transformationType = base.transformationType[transformIndex];
                        if (transformationType == 3 || transformationType == 10) {
                            defaultTransformState = 128;
                        }
                        short currentTransformX;
                        short currentTransformY;
                        short currentTransformZ;
                        short currentLabel;
                        byte currentInterpolation;
                        if (transformCurrent) {
                            currentTransformX = currentFrame.transformationX[currentTransformation];
                            currentTransformY = currentFrame.transformationY[currentTransformation];
                            currentTransformZ = currentFrame.transformationZ[currentTransformation];
                            currentLabel = currentFrame.labels[currentTransformation];
                            currentInterpolation = currentFrame.tweeningProperties[currentTransformation];
                            currentTransformation++;
                        } else {
                            currentTransformX = defaultTransformState;
                            currentTransformY = defaultTransformState;
                            currentTransformZ = defaultTransformState;
                            currentLabel = -1;
                            currentInterpolation = 0;
                        }
                        short nextTransformX;
                        short nextTransformY;
                        short nextTransformZ;
                        short nextLabel;
                        byte nextInterpolation;
                        if (transformNext) {
                            nextTransformX = nextFrame.transformationX[nextTransformation];
                            nextTransformY = nextFrame.transformationY[nextTransformation];
                            nextTransformZ = nextFrame.transformationZ[nextTransformation];
                            nextLabel = nextFrame.labels[nextTransformation];
                            nextInterpolation = nextFrame.tweeningProperties[nextTransformation];
                            nextTransformation++;
                        } else {
                            nextTransformX = defaultTransformState;
                            nextTransformY = defaultTransformState;
                            nextTransformZ = defaultTransformState;
                            nextLabel = -1;
                            nextInterpolation = 0;
                        }
                        int interpolatedX;
                        int interpolatedY;
                        int interpolatedZ;
                        if ((currentInterpolation & 0x2) == 0 && (nextInterpolation & 0x1) == 0) {
                            if (transformationType == 2) {
                                int deltaX = nextTransformX - currentTransformX & 0x3fff;
                                int deltaY = nextTransformY - currentTransformY & 0x3fff;
                                int deltaZ = nextTransformZ - currentTransformZ & 0x3fff;
                                if (deltaX >= 8192) {
                                    deltaX -= 16384;
                                }
                                if (deltaY >= 8192) {
                                    deltaY -= 16384;
                                }
                                if (deltaZ >= 8192) {
                                    deltaZ -= 16384;
                                }
                                interpolatedX = currentTransformX + deltaX * cycle / length & 0x3fff;
                                interpolatedY = currentTransformY + deltaY * cycle / length & 0x3fff;
                                interpolatedZ = currentTransformZ + deltaZ * cycle / length & 0x3fff;
                            } else if (transformationType == 9) {
                                int deltaX = nextTransformX - currentTransformX & 0x3fff;
                                if (deltaX >= 8192) {
                                    deltaX -= 16384;
                                }
                                interpolatedX = currentTransformX + deltaX * cycle / length & 0x3fff;
                                interpolatedZ = 0;
                                interpolatedY = 0;
                            } else if (transformationType == 7) {
                                int deltaX = nextTransformX - currentTransformX & 0x3f;
                                if (deltaX >= 32) {
                                    deltaX -= 64;
                                }
                                interpolatedX = currentTransformX + deltaX * cycle / length & 0x3f;
                                interpolatedY = currentTransformY + (nextTransformY - currentTransformY) * cycle / length;
                                interpolatedZ = currentTransformZ + (nextTransformZ - currentTransformZ) * cycle / length;
                            } else {
                                interpolatedX = currentTransformX + (nextTransformX - currentTransformX) * cycle / length;
                                interpolatedY = currentTransformY + (nextTransformY - currentTransformY) * cycle / length;
                                interpolatedZ = currentTransformZ + (nextTransformZ - currentTransformZ) * cycle / length;
                            }
                        } else {
                            interpolatedX = currentTransformX;
                            interpolatedY = currentTransformY;
                            interpolatedZ = currentTransformZ;
                        }
                        if (currentLabel != -1) {
                            animateTransform(0, base.labels[currentLabel], 0, 0, 0, rotation, lights, submesh & base.submeshes[currentLabel], alwaysNull);
                        } else if (nextLabel != -1) {
                            animateTransform(0, base.labels[nextLabel], 0, 0, 0, rotation, lights, submesh & base.submeshes[nextLabel], alwaysNull);
                        }
                        animateTransform(transformationType, base.labels[transformIndex], interpolatedX, interpolatedY, interpolatedZ, rotation, lights, submesh & base.submeshes[transformIndex], alwaysNull);
                    }
                }
            }
        } else {
            for (currentTransformation = 0; currentTransformation < currentFrame.numTransformations; currentTransformation++) {
                short currentIndice = currentFrame.baseIndices[currentTransformation];
                if (animateLabels == null || animateLabels[currentIndice] == condition || base.transformationType[currentIndice] == 0) {
                    short currentLabel = currentFrame.labels[currentTransformation];
                    if (currentLabel != -1) {
                        animateTransform(0, base.labels[currentLabel], 0, 0, 0, rotation, lights, submesh & base.submeshes[currentLabel], alwaysNull);
                    }
                    animateTransform(base.transformationType[currentIndice], base.labels[currentIndice], currentFrame.transformationX[currentTransformation], currentFrame.transformationY[currentTransformation], currentFrame.transformationZ[currentTransformation], rotation, lights, submesh & base.submeshes[currentIndice], alwaysNull);
                }
            }
        }
    }
    
    public void animateTransform(int baseTransformType, int[] label, int transformX, int transformY, int transformZ, int adjustmentType, boolean lights, int submesh, int[] alwaysNull) {
        int x = transformX;
        int z = transformZ;
        int y;
        if (adjustmentType == 1) {
            if (baseTransformType != 0 && baseTransformType != 1) {
                if (baseTransformType == 3) {
                    y = x;
                    x = z;
                    z = y;
                } else if (baseTransformType == 2) {
                    y = x;
                    x = -z & 0x3fff;
                    z = y & 0x3fff;
                }
            } else {
                y = -x;
                x = z;
                z = y;
            }
        } else if (adjustmentType == 2) {
            if (baseTransformType != 0 && baseTransformType != 1) {
                if (baseTransformType == 2) {
                    x = -x & 0x3fff;
                    z = -z & 0x3fff;
                }
            } else {
                x = -x;
                z = -z;
            }
        } else if (adjustmentType == 3) {
            if (baseTransformType != 0 && baseTransformType != 1) {
                if (baseTransformType == 3) {
                    y = x;
                    x = z;
                    z = y;
                } else if (baseTransformType == 2) {
                    y = x;
                    x = z & 0x3fff;
                    z = -y & 0x3fff;
                }
            } else {
                y = x;
                x = -z;
                z = y;
            }
        }
        if (submesh != 65535) {
            animateTransform(baseTransformType, label, x, transformY, z, lights, submesh, alwaysNull);
        } else {
            animatePartialtransform(baseTransformType, label, x, transformY, z, adjustmentType, lights);
        }
    }
    
    public void animateShadowFrame(FrameSet frameset, int shadowFrameId) {
        if (shadowFrameId != -1) {
            awaitLock();
            if (!startAnimation()) {
                unlock();
            } else {
                AnimFrame frame = frameset.frames[shadowFrameId];
                AnimBase base = frame.base;
                for (int transformIndex = 0; transformIndex < frame.numTransformations; transformIndex++) {
                    short transform = frame.baseIndices[transformIndex];
                    if (base.shadowed[transform]) {
                        if (frame.labels[transformIndex] != -1) {
                            animateShadowtransform(0, 0, 0, 0);
                        }
                        animateShadowtransform(base.transformationType[transform], frame.transformationX[transformIndex], frame.transformationY[transformIndex], frame.transformationZ[transformIndex]);
                    }
                }
                endAnimation();
                unlock();
            }
        }
    }
    
    public abstract void animateShadowtransform(int transformType, int x, int y, int z);

    
    /**
     * *****************************************************************
     * 					(TODO:) Ground adjustment
     * *****************************************************************
     */

    public abstract void contourToGround(int contourType, int contourModifier, Ground currentLvl, Ground nextLvl, int sceneX, int averageHeight, int sceneY);
    
    /**
     * [J]
     */
    protected void hillChange(Ground ground, int offX, int y, int offY, int width, int length, int xan, int zan) {
        int deltaXSw = -width / 2;
        int deltaYSw = -length / 2;
        int southWest = ground.getAverageHeight(deltaXSw + offX, deltaYSw + offY);
        int deltaXSe = width / 2;
        int deltaYSe = -length / 2;
        int southEast = ground.getAverageHeight(deltaXSe + offX, deltaYSe + offY);
        int deltaXNw = -width / 2;
        int deltaYNw = length / 2;
        int northWest = ground.getAverageHeight(deltaXNw + offX, deltaYNw + offY);
        int deltaXNe = width / 2;
        int deltaYNe = length / 2;
        int northEast = ground.getAverageHeight(deltaXNe + offX, deltaYNe + offY);
        int lowestSouth = Math.min(southWest, southEast);
        int lowestNorth = Math.min(northWest, northEast);
        int lowestEast = Math.min(southEast, northEast);
        int lowestWest = Math.min(southWest, northWest);
        if (length != 0) {
            int rotation = (int) (Math.atan2(lowestSouth - lowestNorth, length) * 2607.5945876176133D) & 0x3fff;
            if (rotation != 0) {
                if (xan != 0) {
                    if (rotation > 8192) {
                        int inverse = 16384 - xan;
                        if (rotation < inverse) {
                            rotation = inverse;
                        }
                    } else if (rotation > xan) {
                        rotation = xan;
                    }
                }
                xaxisRotateWithoutNormals(rotation);
            }
        }
        if (width != 0) {
            int rotation = (int) (Math.atan2(lowestWest - lowestEast, width) * 2607.5945876176133D) & 0x3fff;
            if (rotation != 0) {
                if (zan != 0) {
                    if (rotation > 8192) {
                        int inverse = 16384 - zan;
                        if (rotation < inverse) {
                            rotation = inverse;
                        }
                    } else if (rotation > zan) {
                        rotation = zan;
                    }
                }
                zaxisRotateWithoutNormals(rotation);
            }
        }
        int height = southWest + northEast;
        if (southEast + northWest < height) {
            height = southEast + northWest;
        }
        height = (height >> 1) - y;
        if (height != 0) {
            translate(0, height, 0);
        }
    }

    
    /**
     * *****************************************************************
     * 					(TODO:) Texture matrix
     * *****************************************************************
     */
    
    protected TextureTransformationMatrix generateTextureMatrix(Mesh mesh, int[] faces, int numFaces) {
        int[] centerX = null;
        int[] centerY = null;
        int[] centerZ = null;
        float[][] matrix = null;
        if (mesh.facesTexture != null) {
            int numTexturedFaces = mesh.numTextures;
            int[] minX = new int[numTexturedFaces];
            int[] maxX = new int[numTexturedFaces];
            int[] minY = new int[numTexturedFaces];
            int[] maxY = new int[numTexturedFaces];
            int[] minZ = new int[numTexturedFaces];
            int[] maxZ = new int[numTexturedFaces];
            for (int faceIndex = 0; faceIndex < numTexturedFaces; faceIndex++) {
                minX[faceIndex] = Integer.MAX_VALUE;
                maxX[faceIndex] = -2147483647;
                minY[faceIndex] = Integer.MAX_VALUE;
                maxY[faceIndex] = -2147483647;
                minZ[faceIndex] = Integer.MAX_VALUE;
                maxZ[faceIndex] = -2147483647;
            }
            for (int faceIndex = 0; faceIndex < numFaces; faceIndex++) {
                int face = faces[faceIndex];
                if (mesh.facesTexture[face] != -1) {
                    int index = mesh.facesTexture[face] & 0xff;
                    for (int axis = 0; axis < 3; axis++) {
                        short verticeIndex;
                        if (axis == 0) {
                            verticeIndex = mesh.facesA[face];
                        } else if (axis == 1) {
                            verticeIndex = mesh.facesB[face];
                        } else {
                            verticeIndex = mesh.facesC[face];
                        }
                        int x = mesh.verticesX[verticeIndex];
                        int y = mesh.verticesY[verticeIndex];
                        int z = mesh.verticesZ[verticeIndex];
                        if (x < minX[index]) {
                            minX[index] = x;
                        }
                        if (x > maxX[index]) {
                            maxX[index] = x;
                        }
                        if (y < minY[index]) {
                            minY[index] = y;
                        }
                        if (y > maxY[index]) {
                            maxY[index] = y;
                        }
                        if (z < minZ[index]) {
                            minZ[index] = z;
                        }
                        if (z > maxZ[index]) {
                            maxZ[index] = z;
                        }
                    }
                }
            }
            centerX = new int[numTexturedFaces];
            centerY = new int[numTexturedFaces];
            centerZ = new int[numTexturedFaces];
            matrix = new float[numTexturedFaces][];
            for (int faceIndex = 0; faceIndex < numTexturedFaces; faceIndex++) {
                byte textureMapping = mesh.texturesMapping[faceIndex];
                if (textureMapping > 0) {
                    centerX[faceIndex] = (minX[faceIndex] + maxX[faceIndex]) / 2;
                    centerY[faceIndex] = (minY[faceIndex] + maxY[faceIndex]) / 2;
                    centerZ[faceIndex] = (minZ[faceIndex] + maxZ[faceIndex]) / 2;
                    float scaleX;
                    float scaleY;
                    float scaleZ;
                    if (textureMapping == 1) {
                        int x = mesh.texturesScaleX[faceIndex];
                        if (x == 0) {
                            scaleX = 1.0F;
                            scaleZ = 1.0F;
                        } else if (x > 0) {
                            scaleX = 1.0F;
                            scaleZ = x / 1024.0F;
                        } else {
                            scaleZ = 1.0F;
                            scaleX = (-x) / 1024.0F;
                        }
                        scaleY = 64.0F / mesh.texturesScaleY[faceIndex];
                    } else if (textureMapping == 2) {
                        scaleX = 64.0F / mesh.texturesScaleX[faceIndex];
                        scaleY = 64.0F / mesh.texturesScaleY[faceIndex];
                        scaleZ = 64.0F / mesh.texturesScaleZ[faceIndex];
                    } else {
                        scaleX = mesh.texturesScaleX[faceIndex] / 1024.0F;
                        scaleY = mesh.texturesScaleY[faceIndex] / 1024.0F;
                        scaleZ = mesh.texturesScaleZ[faceIndex] / 1024.0F;
                    }
                    matrix[faceIndex] = createTextureMatrix(mesh.textureFaceX[faceIndex], mesh.textureFaceY[faceIndex], mesh.textureFaceZ[faceIndex], mesh.texturesRotation[faceIndex] & 0xff, scaleX, scaleY, scaleZ);
                }
            }
        }
        return new TextureTransformationMatrix(this, centerX, centerY, centerZ, matrix);
    }

    public float[] createTextureMatrix(int n, int m, int p, int rotation, float scaleX, float scaleY, float scaleZ) {
        float[] rotationMap = new float[9];
        float[] result = new float[9];
        float cos = (float) Math.cos(rotation * 0.024543693F);
        float sin = (float) Math.sin(rotation * 0.024543693F);
        float omc = 1.0F - cos;
        rotationMap[0] = cos;
        rotationMap[1] = 0.0F;
        rotationMap[2] = sin;
        rotationMap[3] = 0.0F;
        rotationMap[4] = 1.0F;
        rotationMap[5] = 0.0F;
        rotationMap[6] = -sin;
        rotationMap[7] = 0.0F;
        rotationMap[8] = cos;
        float[] matrix3x3 = new float[9];
        float x = 1.0F;
        float y = 0.0F;
        cos = m / 32767.0F;
        sin = -((float) Math.sqrt(1.0F - cos * cos));
        omc = 1.0F - cos;
        float size = (float) Math.sqrt(p * p + n * n);
        if (size == 0.0F && cos == 0.0F) {
            result = rotationMap;
        } else {
            if (size != 0.0F) {
                x = (-p) / size;
                y = n / size;
            }
            matrix3x3[0] = cos + x * x * omc;
            matrix3x3[1] = y * sin;
            matrix3x3[2] = y * x * omc;
            matrix3x3[3] = -y * sin;
            matrix3x3[4] = cos;
            matrix3x3[5] = x * sin;
            matrix3x3[6] = x * y * omc;
            matrix3x3[7] = -x * sin;
            matrix3x3[8] = cos + y * y * omc;
            result[0] = rotationMap[0] * matrix3x3[0] + rotationMap[1] * matrix3x3[3] + rotationMap[2] * matrix3x3[6];
            result[1] = rotationMap[0] * matrix3x3[1] + rotationMap[1] * matrix3x3[4] + rotationMap[2] * matrix3x3[7];
            result[2] = rotationMap[0] * matrix3x3[2] + rotationMap[1] * matrix3x3[5] + rotationMap[2] * matrix3x3[8];
            result[3] = rotationMap[3] * matrix3x3[0] + rotationMap[4] * matrix3x3[3] + rotationMap[5] * matrix3x3[6];
            result[4] = rotationMap[3] * matrix3x3[1] + rotationMap[4] * matrix3x3[4] + rotationMap[5] * matrix3x3[7];
            result[5] = rotationMap[3] * matrix3x3[2] + rotationMap[4] * matrix3x3[5] + rotationMap[5] * matrix3x3[8];
            result[6] = rotationMap[6] * matrix3x3[0] + rotationMap[7] * matrix3x3[3] + rotationMap[8] * matrix3x3[6];
            result[7] = rotationMap[6] * matrix3x3[1] + rotationMap[7] * matrix3x3[4] + rotationMap[8] * matrix3x3[7];
            result[8] = rotationMap[6] * matrix3x3[2] + rotationMap[7] * matrix3x3[5] + rotationMap[8] * matrix3x3[8];
        }
        result[0] *= scaleX;
        result[1] *= scaleX;
        result[2] *= scaleX;
        result[3] *= scaleY;
        result[4] *= scaleY;
        result[5] *= scaleY;
        result[6] *= scaleZ;
        result[7] *= scaleZ;
        result[8] *= scaleZ;
        return result;
    }
    
    
    /**
     * *****************************************************************
     * 					(TODO:) UV mapping
     * *****************************************************************
     */
    
    protected void mapCylindericalUv(int verticeX, int verticeY, int verticeZ, int centerX, int centerY, int centerZ, float[] matrix, float offset, int direction, float f_10, float[] uv) {
        verticeX -= centerX;
        verticeY -= centerY;
        verticeZ -= centerZ;
        float y = verticeX * matrix[0] + verticeY * matrix[1] + verticeZ * matrix[2];
        float f_13 = verticeX * matrix[3] + verticeY * matrix[4] + verticeZ * matrix[5];
        float x = verticeX * matrix[6] + verticeY * matrix[7] + verticeZ * matrix[8];
        float u = (float) Math.atan2(y, x) / 6.2831855F + 0.5F;
        if (offset != 1.0F) {
            u *= offset;
        }
        float v = f_13 + 0.5F + f_10;
        float tmp;
        if (direction == 1) {
            tmp = u;
            u = -v;
            v = tmp;
        } else if (direction == 2) {
            u = -u;
            v = -v;
        } else if (direction == 3) {
            tmp = u;
            u = v;
            v = -tmp;
        }
        uv[0] = u;
        uv[1] = v;
    }
    
    protected void mapSphericalUv(int verticeX, int verticeY, int verticeZ, int centerX, int centerY, int centerZ, float[] matrix, int direction, float f_9, float[] uv) {
        verticeX -= centerX;
        verticeY -= centerY;
        verticeZ -= centerZ;
        float f_11 = verticeX * matrix[0] + verticeY * matrix[1] + verticeZ * matrix[2];
        float f_12 = verticeX * matrix[3] + verticeY * matrix[4] + verticeZ * matrix[5];
        float f_13 = verticeX * matrix[6] + verticeY * matrix[7] + verticeZ * matrix[8];
        float f_14 = (float) Math.sqrt(f_11 * f_11 + f_12 * f_12 + f_13 * f_13);
        float u = (float) Math.atan2(f_11, f_13) / 6.2831855F + 0.5F;
        float v = (float) Math.asin(f_12 / f_14) / 3.1415927F + 0.5F + f_9;
        float tmp;
        if (direction == 1) {
            tmp = u;
            u = -v;
            v = tmp;
        } else if (direction == 2) {
            u = -u;
            v = -v;
        } else if (direction == 3) {
            tmp = u;
            u = v;
            v = -tmp;
        }
        uv[0] = u;
        uv[1] = v;
    }
    
    protected void mapRectangularUv(int verticeX, int verticeY, int verticeZ, int centerX, int centerY, int centerZ, int type, float[] matrix, int direction, float f_10, float f_11, float f_12, float[] uv) {
        verticeX -= centerX;
        verticeY -= centerY;
        verticeZ -= centerZ;
        float f_14 = verticeX * matrix[0] + verticeY * matrix[1] + verticeZ * matrix[2];
        float f_15 = verticeX * matrix[3] + verticeY * matrix[4] + verticeZ * matrix[5];
        float f_16 = verticeX * matrix[6] + verticeY * matrix[7] + verticeZ * matrix[8];
        float u;
        float v;
        if (type == 0) {
            u = f_14 + f_10 + 0.5F;
            v = -f_16 + f_12 + 0.5F;
        } else if (type == 1) {
            u = f_14 + f_10 + 0.5F;
            v = f_16 + f_12 + 0.5F;
        } else if (type == 2) {
            u = -f_14 + f_10 + 0.5F;
            v = -f_15 + f_11 + 0.5F;
        } else if (type == 3) {
            u = f_14 + f_10 + 0.5F;
            v = -f_15 + f_11 + 0.5F;
        } else if (type == 4) {
            u = f_16 + f_12 + 0.5F;
            v = -f_15 + f_11 + 0.5F;
        } else {
            u = -f_16 + f_12 + 0.5F;
            v = -f_15 + f_11 + 0.5F;
        }
        float tmp;
        if (direction == 1) {
            tmp = u;
            u = -v;
            v = tmp;
        } else if (direction == 2) {
            u = -u;
            v = -v;
        } else if (direction == 3) {
            tmp = u;
            u = v;
            v = -tmp;
        }
        uv[0] = u;
        uv[1] = v;
    }
    
    protected int getCubeFace(float f_1, float f_2, float f_3) {
        float f_4 = f_1 < 0.0F ? -f_1 : f_1;
        float f_5 = f_2 < 0.0F ? -f_2 : f_2;
        float f_6 = f_3 < 0.0F ? -f_3 : f_3;
        return f_5 > f_4 && f_5 > f_6 ? (f_2 > 0.0F ? 0 : 1) : (f_6 > f_4 && f_6 > f_5 ? (f_3 > 0.0F ? 2 : 3) : (f_1 > 0.0F ? 4 : 5));
    }
    
    /**
     * *****************************************************************
     * 					(TODO:) Misc / static
     * *****************************************************************
     */

	public static void addBillboard(long[] billboardUids, int[] faces) {
	    ArrayUtils.quicksort(billboardUids, faces, 0, billboardUids.length - 1);
	}
}
