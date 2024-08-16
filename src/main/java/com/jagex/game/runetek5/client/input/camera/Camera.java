package com.jagex.game.runetek5.client.input.camera;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.utils.MathUtils;

public class Camera {

    public static final Matrix44 aClass3847167 = new Matrix44();
    public static int cameraPositionAbsoluteZ;
    public static int cameraPositionAbsoluteY;
    public static int cameraPositionAbsoluteX;
    public static int anInt9461;
    public static int anInt2349;
    public static int anInt9575;
    public static int cameraYaw;
    public static int anInt3968;
    public static float camAngleX;
    public static int regularLowestYaw;
    public static float camAngleY;
    public static int cameraRandomisationA;
    public static int cameraRoll;
    public static int anInt5029;
    public static int lookY;
    public static int lookX;
    public static int cameraMovementLocalY;
    public static int cameraMovementLocalX;
    
	public static int cameraMovementZ;
	public static int cameraConstantMoveSpeed;
	public static int cameraPitch;
	public static int anInt1527;
	public static int moveCameraAngle;
	public static int moveCameraSpeed;
	
	public static int cameraVariableMoveSpeed;
	public static int cameraZoom = 600;

	public static int cameraBaseX;
	public static int cameraBaseY;

    public static void processCamera() {
        if (camAngleX < 1081.0F) {
            camAngleX = 1081.0F;
        }
        if (camAngleX > 2980.0F) {
            camAngleX = 2980.0F;
        }
        while (camAngleY >= 16384.0F) {
            camAngleY -= 16384.0F;
        }
        while (camAngleY < 0.0F) {
            camAngleY += 16384.0F;
        }
        SettingsBits settingsBits = Resource.mapRegion.getSettingsBits();
        Scene scene = Resource.mapRegion.getScene();
        int playerSceneX = Camera.cameraBaseX >> 9;
        int playerSceneY = Camera.cameraBaseY >> 9;
        int tileHeight = MapRegion.tileheightForAbsolutePosition(Camera.cameraBaseX, Camera.cameraBaseY, PlayerUpdate.MY_PLAYER_PLANE);
        int highestDrawHeightOffset = 0;
        if (playerSceneX > 3 && playerSceneY > 3 && playerSceneX < Resource.mapRegion.getSceneSizeX() - 4 && playerSceneY < Resource.mapRegion.getSceneSizeY() - 4) {
            for (int x = playerSceneX - 4; x <= playerSceneX + 4; x++) {
                for (int y = playerSceneY - 4; y <= playerSceneY + 4; y++) {
                    int plane = PlayerUpdate.MY_PLAYER_PLANE;
                    if (plane < 3 && settingsBits.areRoofsHidden(x, y)) {
                        plane++;
                    }
                    int lighting = 0;
                    byte[][] localLighting = Resource.mapRegion.getLocalLighting(plane);
                    if (localLighting != null) {
                        lighting = (localLighting[x][y] & 0xff) * 8 << 2;
                    }
                    if (scene.localHeightmap != null && scene.localHeightmap[plane] != null) {
                        int drawOffset = tileHeight - (scene.localHeightmap[plane].getHeight(x, y) - lighting);
                        if (drawOffset > highestDrawHeightOffset) {
                            highestDrawHeightOffset = drawOffset;
                        }
                    }
                }
            }
        }
        int yaw = (highestDrawHeightOffset >> 2) * 1536;
        if (yaw > 786432) {
            yaw = 786432;
        }
        if (yaw < 262144) {
            yaw = 262144;
        }
        if (yaw > regularLowestYaw) {
            regularLowestYaw += (yaw - regularLowestYaw) / 24;
        } else if (yaw < regularLowestYaw) {
            regularLowestYaw += (yaw - regularLowestYaw) / 80;
        }
    }

    public static void method6286(long multiplier) {
        Vector3 abs = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
        int x = (int) abs.x + Camera.cameraRandomisationH;
        int y = (int) abs.z + Camera.cameraRandomisationV;
        if (Camera.cameraBaseX - x < -2000 || Camera.cameraBaseX - x > 2000 || Camera.cameraBaseY - y < -2000 || Camera.cameraBaseY - y > 2000) {
            Camera.cameraBaseX = x;
            Camera.cameraBaseY = y;
        }
        int offsetFromCamera;
        int offset;
        if (x != Camera.cameraBaseX) {
            offsetFromCamera = x - Camera.cameraBaseX;
            offset = (int) (multiplier * offsetFromCamera / 320L);
            if (offsetFromCamera > 0) {
                if (offset == 0) {
                    offset = 1;
                } else if (offset > offsetFromCamera) {
                    offset = offsetFromCamera;
                }
            } else if (offset == 0) {
                offset = -1;
            } else if (offset < offsetFromCamera) {
                offset = offsetFromCamera;
            }
            Camera.cameraBaseX += offset;
        }
        if (y != Camera.cameraBaseY) {
            offsetFromCamera = y - Camera.cameraBaseY;
            offset = (int) (multiplier * offsetFromCamera / 320L);
            if (offsetFromCamera > 0) {
                if (offset == 0) {
                    offset = 1;
                } else if (offset > offsetFromCamera) {
                    offset = offsetFromCamera;
                }
            } else if (offset == 0) {
                offset = -1;
            } else if (offset < offsetFromCamera) {
                offset = offsetFromCamera;
            }
            Camera.cameraBaseY += offset;
        }
        camAngleY += client.aFloat7266 * multiplier / 6.0F;
        camAngleX += client.aFloat7284 * multiplier / 6.0F;
        processCamera();
    }

    public static void method4974(long long_0) {
        int i_2 = client.anInt7262;
        int i_3 = client.anInt7376;
        int i_4;
        int i_5;
        if (i_2 != Camera.cameraBaseX) {
            i_4 = i_2 - Camera.cameraBaseX;
            i_5 = (int) (i_4 * long_0 / 320L);
            if (i_4 > 0) {
                if (i_5 == 0) {
                    i_5 = 1;
                } else if (i_5 > i_4) {
                    i_5 = i_4;
                }
            } else if (i_5 == 0) {
                i_5 = -1;
            } else if (i_5 < i_4) {
                i_5 = i_4;
            }
            Camera.cameraBaseX += i_5;
        }
        if (i_3 != Camera.cameraBaseY) {
            i_4 = i_3 - Camera.cameraBaseY;
            i_5 = (int) (i_4 * long_0 / 320L);
            if (i_4 > 0) {
                if (i_5 == 0) {
                    i_5 = 1;
                } else if (i_5 > i_4) {
                    i_5 = i_4;
                }
            } else if (i_5 == 0) {
                i_5 = -1;
            } else if (i_5 < i_4) {
                i_5 = i_4;
            }
            Camera.cameraBaseY += i_5;
        }
        camAngleY += 8.0F * (client.aFloat7266 * long_0 / 40.0F);
        camAngleX += 8.0F * (long_0 * client.aFloat7284 / 40.0F);
        processCamera();
    }

    public static void method6934(Matrix43 sunMatrix, Matrix44 matrix441, int gameAreaWidth, int gameAreaHeight) {
        if (Camera.aClass384158 == null) {
            Camera.aClass384158 = new Matrix44(matrix441);
        } else {
            Camera.aClass384158.copyFrom(matrix441);
        }
        Camera.aClass294155.copyFrom(sunMatrix);
        Camera.anInt176 = gameAreaWidth;
        Camera.anInt184 = gameAreaHeight;
    }

    public static void moveCamera(int localX, int localY, int moveZ, int constantSpeed, int variableSpeed, boolean calculatePosition) {
        cameraMovementLocalX = localX;
        cameraMovementLocalY = localY;
        cameraMovementZ = moveZ;
        cameraConstantMoveSpeed = constantSpeed;
        cameraVariableMoveSpeed = variableSpeed;
        if (calculatePosition && cameraVariableMoveSpeed >= 100) {
            cameraPositionAbsoluteX = cameraMovementLocalX * 512 + 256;
            cameraPositionAbsoluteY = cameraMovementLocalY * 512 + 256;
            cameraPositionAbsoluteZ = MapRegion.tileheightForAbsolutePosition(cameraPositionAbsoluteX, cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER_PLANE) - cameraMovementZ;
        }
        cameraCalculationType = 5;
        StaticMethods.anInt833 = -1;
        PingRequester.anInt5864 = -1;
    }

    public static void setAngle(int angleX, int angleY, int pitch) {
        angleX <<= 3;
        angleY <<= 3;
        pitch <<= 3;
        camAngleX = angleX;
        camAngleY = angleY;
        if (Camera.cameraCalculationType == 5) {
            cameraYaw = angleX;
            cameraRoll = angleY;
            cameraPitch = pitch;
        }
        processCamera();
        client.sendCameraAngleToServer = true;
    }

    public static void method3920(int i_0) {
        int i_2 = client.cycles - client.anInt7429;
        if (i_2 >= 100) {
            Camera.cameraCalculationType = 2;
            StaticMethods.anInt833 = -1;
            PingRequester.anInt5864 = -1;
        } else {
            int yaw = (int) camAngleX;
            if (regularLowestYaw >> 8 > yaw) {
                yaw = regularLowestYaw >> 8;
            }
            if (client.CAM_SHAKING[4] && client.CUSTOM_LOWEST_YAW[4] + 128 > yaw) {
                yaw = client.CUSTOM_LOWEST_YAW[4] + 128;
            }
            int i_4 = (int) camAngleY + cameraRandomisationA & 0x3fff;
            Vector3 abs = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
            setCameraPosition(Camera.cameraBaseX, MapRegion.tileheightForAbsolutePosition((int) abs.x, (int) abs.z,
            		PlayerUpdate.MY_PLAYER_PLANE) - 200, Camera.cameraBaseY, yaw, i_4, (yaw >> 3) * 3 + 600 << 2, i_0);
            float f_6 = 1.0F - ((100 - i_2) * (100 - i_2) * (100 - i_2)) / 1000000.0F;
            cameraPositionAbsoluteX = (int) (anInt2349 + f_6 * (cameraPositionAbsoluteX - anInt2349));
            cameraPositionAbsoluteZ = (int) (anInt9461 + f_6 * (cameraPositionAbsoluteZ - anInt9461));
            cameraPositionAbsoluteY = (int) ((cameraPositionAbsoluteY - anInt9575) * f_6 + anInt9575);
            cameraYaw = (int) (anInt3968 + f_6 * (cameraYaw - anInt3968));
            int i_7 = cameraRoll - anInt5029;
            if (i_7 > 8192) {
                i_7 -= 16384;
            } else if (i_7 < -8192) {
                i_7 += 16384;
            }
            cameraRoll = (int) (anInt5029 + f_6 * i_7);
            cameraRoll &= 0x3fff;
        }
    }

    public static void setCameraPosition(int x, int z, int y, int yaw, int pitch, int baseDeltaY, int gameAreaHeight) {
        int gameAreaHeightDelta = gameAreaHeight - 334;
        if (gameAreaHeightDelta < 0) {
            gameAreaHeightDelta = 0;
        } else if (gameAreaHeightDelta > 100) {
            gameAreaHeightDelta = 100;
        }
        int zoomModifier = (client.aShort7324 - client.aShort7394) * gameAreaHeightDelta / 100 + client.aShort7394;
        baseDeltaY = baseDeltaY * zoomModifier >> 8;
        int rotationX = 16384 - yaw & 0x3fff;
        int rotationZ = 16384 - pitch & 0x3fff;
        int deltaX = 0;
        int deltaZ = 0;
        int deltaY = baseDeltaY;
        if (rotationX != 0) {
            deltaZ = MathUtils.SINE[rotationX] * -baseDeltaY >> 14;
            deltaY = MathUtils.COSINE[rotationX] * baseDeltaY >> 14;
        }
        if (rotationZ != 0) {
            deltaX = MathUtils.SINE[rotationZ] * deltaY >> 14;
            deltaY = MathUtils.COSINE[rotationZ] * deltaY >> 14;
        }
        cameraPositionAbsoluteX = x - deltaX;
        cameraPositionAbsoluteZ = z - deltaZ;
        cameraPositionAbsoluteY = y - deltaY;
        cameraYaw = yaw;
        cameraRoll = pitch;
        cameraPitch = 0;
    }

    public static void method13296(Matrix44 matrix440) {
        StaticMethods.aClass384185.copyFrom(matrix440);
    }

	public static void lookat(int eyeX, int eyeY, int i_2, int speed, int angle) {
	    lookX = eyeX;
	    lookY = eyeY;
	    Camera.anInt1527 = i_2;
	    Camera.moveCameraSpeed = speed;
	    Camera.moveCameraAngle = angle;
	    if (Camera.moveCameraAngle >= 100) {
	        int eyeAbsX = lookX * 512 + 256;
	        int eyeAbsY = lookY * 512 + 256;
	        int eyeAbsZ = MapRegion.tileheightForAbsolutePosition(eyeAbsX, eyeAbsY, PlayerUpdate.MY_PLAYER_PLANE) - Camera.anInt1527;
	        int cameraDeltaX = eyeAbsX - cameraPositionAbsoluteX;
	        int cameraDeltaZ = eyeAbsZ - cameraPositionAbsoluteZ;
	        int cameraDeltaY = eyeAbsY - cameraPositionAbsoluteY;
	        int i_12 = (int) Math.sqrt(cameraDeltaX * cameraDeltaX + cameraDeltaY * cameraDeltaY);
	        cameraYaw = (int) (Math.atan2(cameraDeltaZ, i_12) * 2607.5945876176133D) & 0x3fff;
	        cameraRoll = (int) (Math.atan2(cameraDeltaX, cameraDeltaY) * -2607.5945876176133) & 0x3fff;
	        Camera.cameraPitch = 0;
	        if (cameraYaw < 1024) {
	            cameraYaw = 1024;
	        }
	        if (cameraYaw > 3072) {
	            cameraYaw = 3072;
	        }
	    }
	    Camera.cameraCalculationType = 5;
	    StaticMethods.anInt833 = -1;
	    PingRequester.anInt5864 = -1;
	}

	public static void calculateCameraPosition() {
	    int absoluteX = cameraMovementLocalX * 512 + 256;
	    int absoluteY = cameraMovementLocalY * 512 + 256;
	    int cameraGroundOffset = MapRegion.tileheightForAbsolutePosition(absoluteX, absoluteY, PlayerUpdate.MY_PLAYER_PLANE) - Camera.cameraMovementZ;
	    if (cameraVariableMoveSpeed >= 100) {
	        cameraPositionAbsoluteX = cameraMovementLocalX * 512 + 256;
	        cameraPositionAbsoluteY = cameraMovementLocalY * 512 + 256;
	        cameraPositionAbsoluteZ = MapRegion.tileheightForAbsolutePosition(cameraPositionAbsoluteX, cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER_PLANE) - Camera.cameraMovementZ;
	    } else {
	        if (cameraPositionAbsoluteX < absoluteX) {
	            cameraPositionAbsoluteX = (absoluteX - cameraPositionAbsoluteX) * cameraVariableMoveSpeed / 1000 + cameraPositionAbsoluteX + Camera.cameraConstantMoveSpeed;
	            if (cameraPositionAbsoluteX > absoluteX) {
	                cameraPositionAbsoluteX = absoluteX;
	            }
	        }
	        if (cameraPositionAbsoluteX > absoluteX) {
	            cameraPositionAbsoluteX -= cameraVariableMoveSpeed * (cameraPositionAbsoluteX - absoluteX) / 1000 + Camera.cameraConstantMoveSpeed;
	            if (cameraPositionAbsoluteX < absoluteX) {
	                cameraPositionAbsoluteX = absoluteX;
	            }
	        }
	        if (cameraPositionAbsoluteZ < cameraGroundOffset) {
	            cameraPositionAbsoluteZ = (cameraGroundOffset - cameraPositionAbsoluteZ) * cameraVariableMoveSpeed / 1000 + cameraPositionAbsoluteZ + Camera.cameraConstantMoveSpeed;
	            if (cameraPositionAbsoluteZ > cameraGroundOffset) {
	                cameraPositionAbsoluteZ = cameraGroundOffset;
	            }
	        }
	        if (cameraPositionAbsoluteZ > cameraGroundOffset) {
	            cameraPositionAbsoluteZ -= cameraVariableMoveSpeed * (cameraPositionAbsoluteZ - cameraGroundOffset) / 1000 + Camera.cameraConstantMoveSpeed;
	            if (cameraPositionAbsoluteZ < cameraGroundOffset) {
	                cameraPositionAbsoluteZ = cameraGroundOffset;
	            }
	        }
	        if (cameraPositionAbsoluteY < absoluteY) {
	            cameraPositionAbsoluteY = (absoluteY - cameraPositionAbsoluteY) * cameraVariableMoveSpeed / 1000 + cameraPositionAbsoluteY + Camera.cameraConstantMoveSpeed;
	            if (cameraPositionAbsoluteY > absoluteY) {
	                cameraPositionAbsoluteY = absoluteY;
	            }
	        }
	        if (cameraPositionAbsoluteY > absoluteY) {
	            cameraPositionAbsoluteY -= cameraVariableMoveSpeed * (cameraPositionAbsoluteY - absoluteY) / 1000 + Camera.cameraConstantMoveSpeed;
	            if (cameraPositionAbsoluteY < absoluteY) {
	                cameraPositionAbsoluteY = absoluteY;
	            }
	        }
	    }
	    absoluteX = lookX * 512 + 256;
	    absoluteY = lookY * 512 + 256;
	    cameraGroundOffset = MapRegion.tileheightForAbsolutePosition(absoluteX, absoluteY, PlayerUpdate.MY_PLAYER_PLANE) - Camera.anInt1527;
	    int widthDelta = absoluteX - cameraPositionAbsoluteX;
	    int heightDelta = cameraGroundOffset - cameraPositionAbsoluteZ;
	    int lengthDelta = absoluteY - cameraPositionAbsoluteY;
	    int i_7 = (int) Math.sqrt(lengthDelta * lengthDelta + widthDelta * widthDelta);
	    int yaw = (int) (Math.atan2(heightDelta, i_7) * 2607.5945876176133D) & 0x3fff;
	    int roll = (int) (Math.atan2(widthDelta, lengthDelta) * -2607.5945876176133) & 0x3fff;
	    if (yaw < 1024) {
	        yaw = 1024;
	    }	
	    if (yaw > 3072) {
	        yaw = 3072;
	    }
	    if (cameraYaw < yaw) {
	        cameraYaw += (yaw - cameraYaw >> 3) * Camera.moveCameraAngle / 1000 + Camera.moveCameraSpeed << 3;
	        if (cameraYaw > yaw) {
	            cameraYaw = yaw;
	        }
	    }	
	    if (cameraYaw > yaw) {
	        cameraYaw -= (cameraYaw - yaw >> 3) * Camera.moveCameraAngle / 1000 + Camera.moveCameraSpeed << 3;
	        if (cameraYaw < yaw) {
	            cameraYaw = yaw;
	        }
	    }	
	    int clampedRoll = roll - cameraRoll;
	    if (clampedRoll > 8192) {
	        clampedRoll -= 16384;
	    }	
	    if (clampedRoll < -8192) {
	        clampedRoll += 16384;
	    }	
	    clampedRoll >>= 3;
	    if (clampedRoll > 0) {
	        cameraRoll += clampedRoll * Camera.moveCameraAngle / 1000 + Camera.moveCameraSpeed << 3;
	        cameraRoll &= 0x3fff;
	    }	
	    if (clampedRoll < 0) {
	        cameraRoll -= Camera.moveCameraSpeed - clampedRoll * Camera.moveCameraAngle / 1000 << 3;
	        cameraRoll &= 0x3fff;
	    }	
	    int clampedAdjustedRoll = roll - cameraRoll;
	    if (clampedAdjustedRoll > 8192) {
	        clampedAdjustedRoll -= 16384;
	    }	
	    if (clampedAdjustedRoll < -8192) {
	        clampedAdjustedRoll += 16384;
	    }	
	    if (clampedAdjustedRoll < 0 && clampedRoll > 0 || clampedAdjustedRoll > 0 && clampedRoll < 0) {
	        cameraRoll = roll;
	    }	
	    Camera.cameraPitch = 0;
	}

	public static int cameraCalculationType;

	public static void calculateCutsceneCameraPosition() {
	    if (client.anInt7277 != -1 && client.anInt7448 != -1) {
	        int i_1 = ((client.anInt7182 - client.anInt7327) * client.anInt7256 >> 16) + client.anInt7327;
	        client.anInt7256 += i_1;
	        if (client.anInt7256 >= 65535) {
	            client.anInt7256 = 65535;
	            client.aBool7355 = !client.aBool7285;
	            client.aBool7285 = true;
	        } else {
	            client.aBool7355 = false;
	            client.aBool7285 = false;
	        }
	        float f_2 = client.anInt7256 / 65535.0F;
	        float[] from = new float[3];
	        int i_4 = client.anInt7279 * 2;
	        for (int dir = 0; dir < 3; dir++) {
	            int i_17 = client.anIntArrayArrayArray7302[client.anInt7277][i_4][dir] * 3;
	            int i_7 = client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 1][dir] * 3;
	            int i_8 = (client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][dir] - (client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 3][dir] - client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][dir])) * 3;
	            int i_9 = client.anIntArrayArrayArray7302[client.anInt7277][i_4][dir];
	            int i_10 = i_7 - i_17;
	            int i_18 = i_17 - i_7 * 2 + i_8;
	            int i_12 = client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][dir] - i_9 + i_7 - i_8;
	            from[dir] = i_9 + ((i_12 * f_2 + i_18) * f_2 + i_10) * f_2;
	        }
	        CoordGrid baseCoords = Resource.mapRegion.getBaseCoordinates();
	        cameraPositionAbsoluteX = (int) from[0] - baseCoords.x * 512;
	        cameraPositionAbsoluteZ = (int) from[1] * -1;
	        cameraPositionAbsoluteY = (int) from[2] - baseCoords.y * 512;
	        float[] to = new float[3];
	        int i_7 = client.anInt7280 * 2;
	        for (int dir = 0; dir < 3; dir++) {
	        	int i_9 = client.anIntArrayArrayArray7302[client.anInt7448][i_7][dir] * 3;
	        	int i_10 = client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 1][dir] * 3;
	        	int i_18 = (client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 2][dir] - (client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 3][dir] - client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 2][dir])) * 3;
	        	int i_12 = client.anIntArrayArrayArray7302[client.anInt7448][i_7][dir];
	            int i_13 = i_10 - i_9;
	            int i_14 = i_9 - i_10 * 2 + i_18;
	            int i_15 = i_10 + (client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 2][dir] - i_12) - i_18;
	            to[dir] = f_2 * ((i_14 + f_2 * i_15) * f_2 + i_13) + i_12;
	        }
	        float deltaX = to[0] - from[0];
	        float deltaZ = -1.0f * (to[1] - from[1]);
	        float deltaY = to[2] - from[2];
	        double d_11 = Math.sqrt(deltaY * deltaY + deltaX * deltaX);
	        cameraYaw = (int) (Math.atan2(deltaZ, d_11) * 2607.5945876176133D) & 0x3fff;
	        cameraRoll = (int) (-Math.atan2(deltaX, deltaY) * 2607.5945876176133D) & 0x3fff;
	        cameraPitch = ((client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][3] - client.anIntArrayArrayArray7302[client.anInt7277][i_4][3]) * client.anInt7256 >> 16) + client.anIntArrayArrayArray7302[client.anInt7277][i_4][3];
	    }
	}

	public static void smoothReset() {
	    for (int index = 0; index < 5; index++) {
	        client.CAM_SHAKING[index] = false;
	    }
	    client.anInt7277 = -1;
	    client.anInt7448 = -1;
	    moveCameraSpeed = 0;
	    moveCameraAngle = 0;
	    cameraCalculationType = 2;
	    StaticMethods.anInt833 = -1;
	    PingRequester.anInt5864 = -1;
	}

	public static void method12606() {
	    for (int index = 0; index < 5; index++) {
	        client.CAM_SHAKING[index] = false;
	    }
	
	    client.anInt7277 = -1;
	    client.anInt7448 = -1;
	    moveCameraSpeed = 0;
	    moveCameraAngle = 0;
	    cameraCalculationType = 1;
	    StaticMethods.anInt833 = -1;
	    PingRequester.anInt5864 = -1;
	    client.anInt7429 = client.cycles;
	    anInt2349 = cameraPositionAbsoluteX;
	    anInt9461 = cameraPositionAbsoluteZ;
	    anInt9575 = cameraPositionAbsoluteY;
	    anInt3968 = cameraYaw;
	    anInt5029 = cameraRoll;
	}

	public static int anInt176;
	public static int anInt184;
	public static Matrix44 aClass384158;
	public static final Matrix43 aClass294155 = new Matrix43();
	public static int cameraRandomisationV;
	public static int cameraStepH;
	public static int cameraRandomisationH;
	public static int cameraStepV;
	public static int cameraStepA;
}
