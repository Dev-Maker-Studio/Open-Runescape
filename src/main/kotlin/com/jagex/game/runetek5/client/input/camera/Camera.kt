package com.jagex.game.runetek5.client.input.camera

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.SettingsBits
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.atan2
import kotlin.math.sqrt

object Camera {

    @JvmField val aClass3847167 = Matrix44()
    @JvmField var cameraPositionAbsoluteZ: Int = 0
    @JvmField var cameraPositionAbsoluteY: Int = 0
    @JvmField var cameraPositionAbsoluteX: Int = 0
    @JvmField var anInt9461: Int = 0
    @JvmField var anInt2349: Int = 0
    @JvmField var anInt9575: Int = 0
    @JvmField var cameraYaw: Int = 0
    @JvmField var anInt3968: Int = 0
    @JvmField var camAngleX: Float = 0f
    @JvmField var regularLowestYaw: Int = 0
    @JvmField var camAngleY: Float = 0f
    @JvmField var cameraRandomisationA: Int = 0
    @JvmField var cameraRoll: Int = 0
    @JvmField var anInt5029: Int = 0
    @JvmField var lookY: Int = 0
    @JvmField var lookX: Int = 0
    @JvmField var cameraMovementLocalY: Int = 0
    @JvmField var cameraMovementLocalX: Int = 0
    @JvmField var cameraMovementZ: Int = 0
    @JvmField var cameraConstantMoveSpeed: Int = 0
    @JvmField var cameraPitch: Int = 0
    @JvmField var anInt1527: Int = 0
    @JvmField var moveCameraAngle: Int = 0
    @JvmField var moveCameraSpeed: Int = 0
    @JvmField var cameraVariableMoveSpeed: Int = 0
    @JvmField var cameraZoom: Int = 600
    @JvmField var cameraBaseX: Int = 0
    @JvmField var cameraBaseY: Int = 0
    @JvmField var cameraCalculationType: Int = 0
    @JvmField var anInt176: Int = 0
    @JvmField var anInt184: Int = 0
    @JvmField var aClass384158: Matrix44? = null
    @JvmField val aClass294155 = Matrix43()
    @JvmField var cameraRandomisationV: Int = 0
    @JvmField var cameraStepH: Int = 0
    @JvmField var cameraRandomisationH: Int = 0
    @JvmField var cameraStepV: Int = 0
    @JvmField var cameraStepA: Int = 0

    @JvmStatic
    fun processCamera() {
        if (camAngleX < 1081.0f) {
            camAngleX = 1081.0f
        }
        if (camAngleX > 2980.0f) {
            camAngleX = 2980.0f
        }
        while (camAngleY >= 16384.0f) {
            camAngleY -= 16384.0f
        }
        while (camAngleY < 0.0f) {
            camAngleY += 16384.0f
        }
        val settingsBits = Resource.mapRegion!!.settingsBits
        val scene = Resource.mapRegion!!.getScene()!!
        val playerSceneX = cameraBaseX shr 9
        val playerSceneY = cameraBaseY shr 9
        val tileHeight = MapRegion.tileheightForAbsolutePosition(cameraBaseX, cameraBaseY, PlayerUpdate.MY_PLAYER_PLANE)
        var highestDrawHeightOffset = 0
        if (playerSceneX > 3 && playerSceneY > 3 && playerSceneX < Resource.mapRegion!!.sceneSizeX - 4 && playerSceneY < Resource.mapRegion!!.sceneSizeY - 4) {
            for (x in playerSceneX - 4..playerSceneX + 4) {
                for (y in playerSceneY - 4..playerSceneY + 4) {
                    var plane = PlayerUpdate.MY_PLAYER_PLANE
                    if (plane < 3 && settingsBits.areRoofsHidden(x, y)) {
                        plane++
                    }
                    var lighting = 0
                    val localLighting = Resource.mapRegion!!.getLocalLighting(plane)
                    if (localLighting != null && localLighting[x] != null) {
                        lighting = (localLighting[x]!![y].toInt() and 0xff) * 8 shl 2
                    }
                    if (scene.localHeightmap != null && scene.localHeightmap!![plane] != null) {
                        val drawOffset = tileHeight - (scene.localHeightmap!![plane]!!.getHeight(x, y) - lighting)
                        if (drawOffset > highestDrawHeightOffset) {
                            highestDrawHeightOffset = drawOffset
                        }
                    }
                }
            }
        }
        var yaw = (highestDrawHeightOffset shr 2) * 1536
        if (yaw > 786432) {
            yaw = 786432
        }
        if (yaw < 262144) {
            yaw = 262144
        }
        if (yaw > regularLowestYaw) {
            regularLowestYaw += (yaw - regularLowestYaw) / 24
        } else if (yaw < regularLowestYaw) {
            regularLowestYaw += (yaw - regularLowestYaw) / 80
        }
    }

    @JvmStatic
    fun method6286(multiplier: Long) {
        val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
        val x = abs.x.toInt() + cameraRandomisationH
        val y = abs.z.toInt() + cameraRandomisationV
        if (cameraBaseX - x < -2000 || cameraBaseX - x > 2000 || cameraBaseY - y < -2000 || cameraBaseY - y > 2000) {
            cameraBaseX = x
            cameraBaseY = y
        }
        var offsetFromCamera: Int
        var offset: Int
        if (x != cameraBaseX) {
            offsetFromCamera = x - cameraBaseX
            offset = (multiplier * offsetFromCamera / 320L).toInt()
            if (offsetFromCamera > 0) {
                if (offset == 0) {
                    offset = 1
                } else if (offset > offsetFromCamera) {
                    offset = offsetFromCamera
                }
            } else if (offset == 0) {
                offset = -1
            } else if (offset < offsetFromCamera) {
                offset = offsetFromCamera
            }
            cameraBaseX += offset
        }
        if (y != cameraBaseY) {
            offsetFromCamera = y - cameraBaseY
            offset = (multiplier * offsetFromCamera / 320L).toInt()
            if (offsetFromCamera > 0) {
                if (offset == 0) {
                    offset = 1
                } else if (offset > offsetFromCamera) {
                    offset = offsetFromCamera
                }
            } else if (offset == 0) {
                offset = -1
            } else if (offset < offsetFromCamera) {
                offset = offsetFromCamera
            }
            cameraBaseY += offset
        }
        camAngleY += client.aFloat7266 * multiplier / 6.0f
        camAngleX += client.aFloat7284 * multiplier / 6.0f
        processCamera()
    }

    @JvmStatic
    fun method4974(long_0: Long) {
        val i_2 = client.anInt7262
        val i_3 = client.anInt7376
        var i_4: Int
        var i_5: Int
        if (i_2 != cameraBaseX) {
            i_4 = i_2 - cameraBaseX
            i_5 = (i_4 * long_0 / 320L).toInt()
            if (i_4 > 0) {
                if (i_5 == 0) {
                    i_5 = 1
                } else if (i_5 > i_4) {
                    i_5 = i_4
                }
            } else if (i_5 == 0) {
                i_5 = -1
            } else if (i_5 < i_4) {
                i_5 = i_4
            }
            cameraBaseX += i_5
        }
        if (i_3 != cameraBaseY) {
            i_4 = i_3 - cameraBaseY
            i_5 = (i_4 * long_0 / 320L).toInt()
            if (i_4 > 0) {
                if (i_5 == 0) {
                    i_5 = 1
                } else if (i_5 > i_4) {
                    i_5 = i_4
                }
            } else if (i_5 == 0) {
                i_5 = -1
            } else if (i_5 < i_4) {
                i_5 = i_4
            }
            cameraBaseY += i_5
        }
        camAngleY += 8.0f * (client.aFloat7266 * long_0 / 40.0f)
        camAngleX += 8.0f * (long_0 * client.aFloat7284 / 40.0f)
        processCamera()
    }

    @JvmStatic
    fun method6934(sunMatrix: Matrix43, matrix441: Matrix44, gameAreaWidth: Int, gameAreaHeight: Int) {
        if (aClass384158 == null) {
            aClass384158 = Matrix44(matrix441)
        } else {
            aClass384158!!.copyFrom(matrix441)
        }
        aClass294155.copyFrom(sunMatrix)
        anInt176 = gameAreaWidth
        anInt184 = gameAreaHeight
    }

    @JvmStatic
    fun moveCamera(localX: Int, localY: Int, moveZ: Int, constantSpeed: Int, variableSpeed: Int, calculatePosition: Boolean) {
        cameraMovementLocalX = localX
        cameraMovementLocalY = localY
        cameraMovementZ = moveZ
        cameraConstantMoveSpeed = constantSpeed
        cameraVariableMoveSpeed = variableSpeed
        if (calculatePosition && cameraVariableMoveSpeed >= 100) {
            cameraPositionAbsoluteX = cameraMovementLocalX * 512 + 256
            cameraPositionAbsoluteY = cameraMovementLocalY * 512 + 256
            cameraPositionAbsoluteZ = MapRegion.tileheightForAbsolutePosition(cameraPositionAbsoluteX, cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER_PLANE) - cameraMovementZ
        }
        cameraCalculationType = 5
        StaticMethods.anInt833 = -1
        PingRequester.anInt5864 = -1
    }

    @JvmStatic
    fun setAngle(angleX: Int, angleY: Int, pitch: Int) {
        var angleXShifted = angleX shl 3
        var angleYShifted = angleY shl 3
        var pitchShifted = pitch shl 3
        camAngleX = angleXShifted.toFloat()
        camAngleY = angleYShifted.toFloat()
        if (cameraCalculationType == 5) {
            cameraYaw = angleXShifted
            cameraRoll = angleYShifted
            cameraPitch = pitchShifted
        }
        processCamera()
        client.sendCameraAngleToServer = true
    }

    @JvmStatic
    fun method3920(i_0: Int) {
        val i_2 = client.cycles - client.anInt7429
        if (i_2 >= 100) {
            cameraCalculationType = 2
            StaticMethods.anInt833 = -1
            PingRequester.anInt5864 = -1
        } else {
            var yaw = camAngleX.toInt()
            if (regularLowestYaw shr 8 > yaw) {
                yaw = regularLowestYaw shr 8
            }
            if (client.CAM_SHAKING[4] && client.CUSTOM_LOWEST_YAW[4] + 128 > yaw) {
                yaw = client.CUSTOM_LOWEST_YAW[4] + 128
            }
            val i_4 = (camAngleY.toInt() + cameraRandomisationA) and 0x3fff
            val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
            setCameraPosition(
                cameraBaseX, MapRegion.tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(),
                    PlayerUpdate.MY_PLAYER_PLANE
                ) - 200, cameraBaseY, yaw, i_4, (yaw shr 3) * 3 + 600 shl 2, i_0
            )
            val f_6 = 1.0f - (100 - i_2) * (100 - i_2) * (100 - i_2) / 1000000.0f
            cameraPositionAbsoluteX = (anInt2349 + f_6 * (cameraPositionAbsoluteX - anInt2349)).toInt()
            cameraPositionAbsoluteZ = (anInt9461 + f_6 * (cameraPositionAbsoluteZ - anInt9461)).toInt()
            cameraPositionAbsoluteY = ((cameraPositionAbsoluteY - anInt9575) * f_6 + anInt9575).toInt()
            cameraYaw = (anInt3968 + f_6 * (cameraYaw - anInt3968)).toInt()
            var i_7 = cameraRoll - anInt5029
            if (i_7 > 8192) {
                i_7 -= 16384
            } else if (i_7 < -8192) {
                i_7 += 16384
            }
            cameraRoll = (anInt5029 + f_6 * i_7).toInt()
            cameraRoll = cameraRoll and 0x3fff
        }
    }

    @JvmStatic
    fun setCameraPosition(x: Int, z: Int, y: Int, yaw: Int, pitch: Int, baseDeltaY: Int, gameAreaHeight: Int) {
        var gameAreaHeightDelta = gameAreaHeight - 334
        if (gameAreaHeightDelta < 0) {
            gameAreaHeightDelta = 0
        } else if (gameAreaHeightDelta > 100) {
            gameAreaHeightDelta = 100
        }
        val zoomModifier = (client.aShort7324 - client.aShort7394) * gameAreaHeightDelta / 100 + client.aShort7394
        var baseDeltaYAdjusted = baseDeltaY * zoomModifier shr 8
        val rotationX = 16384 - yaw and 0x3fff
        val rotationZ = 16384 - pitch and 0x3fff
        var deltaX = 0
        var deltaZ = 0
        var deltaY = baseDeltaYAdjusted
        if (rotationX != 0) {
            deltaZ = MathUtils.SINE[rotationX] * -baseDeltaYAdjusted shr 14
            deltaY = MathUtils.COSINE[rotationX] * baseDeltaYAdjusted shr 14
        }
        if (rotationZ != 0) {
            deltaX = MathUtils.SINE[rotationZ] * deltaY shr 14
            deltaY = MathUtils.COSINE[rotationZ] * deltaY shr 14
        }
        cameraPositionAbsoluteX = x - deltaX
        cameraPositionAbsoluteZ = z - deltaZ
        cameraPositionAbsoluteY = y - deltaY
        cameraYaw = yaw
        cameraRoll = pitch
        cameraPitch = 0
    }

    @JvmStatic
    fun method13296(matrix440: Matrix44) {
        StaticMethods.aClass384185.copyFrom(matrix440)
    }

    @JvmStatic
    fun lookat(eyeX: Int, eyeY: Int, i_2: Int, speed: Int, angle: Int) {
        lookX = eyeX
        lookY = eyeY
        anInt1527 = i_2
        moveCameraSpeed = speed
        moveCameraAngle = angle
        if (moveCameraAngle >= 100) {
            val eyeAbsX = lookX * 512 + 256
            val eyeAbsY = lookY * 512 + 256
            val eyeAbsZ = MapRegion.tileheightForAbsolutePosition(eyeAbsX, eyeAbsY, PlayerUpdate.MY_PLAYER_PLANE) - anInt1527
            val cameraDeltaX = eyeAbsX - cameraPositionAbsoluteX
            val cameraDeltaZ = eyeAbsZ - cameraPositionAbsoluteZ
            val cameraDeltaY = eyeAbsY - cameraPositionAbsoluteY
            val i_12 = sqrt((cameraDeltaX * cameraDeltaX + cameraDeltaY * cameraDeltaY).toDouble()).toInt()
            cameraYaw = (atan2(cameraDeltaZ.toDouble(), i_12.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            cameraRoll = (atan2(cameraDeltaX.toDouble(), cameraDeltaY.toDouble()) * -2607.5945876176133).toInt() and 0x3fff
            cameraPitch = 0
            if (cameraYaw < 1024) {
                cameraYaw = 1024
            }
            if (cameraYaw > 3072) {
                cameraYaw = 3072
            }
        }
        cameraCalculationType = 5
        StaticMethods.anInt833 = -1
        PingRequester.anInt5864 = -1
    }

    @JvmStatic
    fun calculateCameraPosition() {
        val absoluteX = cameraMovementLocalX * 512 + 256
        val absoluteY = cameraMovementLocalY * 512 + 256
        val cameraGroundOffset = MapRegion.tileheightForAbsolutePosition(absoluteX, absoluteY, PlayerUpdate.MY_PLAYER_PLANE) - cameraMovementZ
        if (cameraVariableMoveSpeed >= 100) {
            cameraPositionAbsoluteX = cameraMovementLocalX * 512 + 256
            cameraPositionAbsoluteY = cameraMovementLocalY * 512 + 256
            cameraPositionAbsoluteZ = MapRegion.tileheightForAbsolutePosition(cameraPositionAbsoluteX, cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER_PLANE) - cameraMovementZ
        } else {
            if (cameraPositionAbsoluteX < absoluteX) {
                cameraPositionAbsoluteX = (absoluteX - cameraPositionAbsoluteX) * cameraVariableMoveSpeed / 1000 + cameraPositionAbsoluteX + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteX > absoluteX) {
                    cameraPositionAbsoluteX = absoluteX
                }
            }
            if (cameraPositionAbsoluteX > absoluteX) {
                cameraPositionAbsoluteX -= cameraVariableMoveSpeed * (cameraPositionAbsoluteX - absoluteX) / 1000 + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteX < absoluteX) {
                    cameraPositionAbsoluteX = absoluteX
                }
            }
            if (cameraPositionAbsoluteZ < cameraGroundOffset) {
                cameraPositionAbsoluteZ = (cameraGroundOffset - cameraPositionAbsoluteZ) * cameraVariableMoveSpeed / 1000 + cameraPositionAbsoluteZ + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteZ > cameraGroundOffset) {
                    cameraPositionAbsoluteZ = cameraGroundOffset
                }
            }
            if (cameraPositionAbsoluteZ > cameraGroundOffset) {
                cameraPositionAbsoluteZ -= cameraVariableMoveSpeed * (cameraPositionAbsoluteZ - cameraGroundOffset) / 1000 + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteZ < cameraGroundOffset) {
                    cameraPositionAbsoluteZ = cameraGroundOffset
                }
            }
            if (cameraPositionAbsoluteY < absoluteY) {
                cameraPositionAbsoluteY = (absoluteY - cameraPositionAbsoluteY) * cameraVariableMoveSpeed / 1000 + cameraPositionAbsoluteY + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteY > absoluteY) {
                    cameraPositionAbsoluteY = absoluteY
                }
            }
            if (cameraPositionAbsoluteY > absoluteY) {
                cameraPositionAbsoluteY -= cameraVariableMoveSpeed * (cameraPositionAbsoluteY - absoluteY) / 1000 + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteY < absoluteY) {
                    cameraPositionAbsoluteY = absoluteY
                }
            }
        }
        val lookAbsX = lookX * 512 + 256
        val lookAbsY = lookY * 512 + 256
        val lookGroundOffset = MapRegion.tileheightForAbsolutePosition(lookAbsX, lookAbsY, PlayerUpdate.MY_PLAYER_PLANE) - anInt1527
        val widthDelta = lookAbsX - cameraPositionAbsoluteX
        val heightDelta = lookGroundOffset - cameraPositionAbsoluteZ
        val lengthDelta = lookAbsY - cameraPositionAbsoluteY
        val i_7 = sqrt((lengthDelta * lengthDelta + widthDelta * widthDelta).toDouble()).toInt()
        var yaw = (atan2(heightDelta.toDouble(), i_7.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
        val roll = (atan2(widthDelta.toDouble(), lengthDelta.toDouble()) * -2607.5945876176133).toInt() and 0x3fff
        if (yaw < 1024) {
            yaw = 1024
        }
        if (yaw > 3072) {
            yaw = 3072
        }
        if (cameraYaw < yaw) {
            cameraYaw += (yaw - cameraYaw shr 3) * moveCameraAngle / 1000 + moveCameraSpeed shl 3
            if (cameraYaw > yaw) {
                cameraYaw = yaw
            }
        }
        if (cameraYaw > yaw) {
            cameraYaw -= (cameraYaw - yaw shr 3) * moveCameraAngle / 1000 + moveCameraSpeed shl 3
            if (cameraYaw < yaw) {
                cameraYaw = yaw
            }
        }
        var clampedRoll = roll - cameraRoll
        if (clampedRoll > 8192) {
            clampedRoll -= 16384
        }
        if (clampedRoll < -8192) {
            clampedRoll += 16384
        }
        clampedRoll = clampedRoll shr 3
        if (clampedRoll > 0) {
            cameraRoll += clampedRoll * moveCameraAngle / 1000 + moveCameraSpeed shl 3
            cameraRoll = cameraRoll and 0x3fff
        }
        if (clampedRoll < 0) {
            cameraRoll -= moveCameraSpeed - clampedRoll * moveCameraAngle / 1000 shl 3
            cameraRoll = cameraRoll and 0x3fff
        }
        var clampedAdjustedRoll = roll - cameraRoll
        if (clampedAdjustedRoll > 8192) {
            clampedAdjustedRoll -= 16384
        }
        if (clampedAdjustedRoll < -8192) {
            clampedAdjustedRoll += 16384
        }
        if (clampedAdjustedRoll < 0 && clampedRoll > 0 || clampedAdjustedRoll > 0 && clampedRoll < 0) {
            cameraRoll = roll
        }
        cameraPitch = 0
    }

    @JvmStatic
    fun calculateCutsceneCameraPosition() {
        if (client.anInt7277 != -1 && client.anInt7448 != -1) {
            val i_1 = ((client.anInt7182 - client.anInt7327) * client.anInt7256 shr 16) + client.anInt7327
            client.anInt7256 += i_1
            if (client.anInt7256 >= 65535) {
                client.anInt7256 = 65535
                client.aBool7355 = !client.aBool7285
                client.aBool7285 = true
            } else {
                client.aBool7355 = false
                client.aBool7285 = false
            }
            val f_2 = client.anInt7256 / 65535.0f
            val from = FloatArray(3)
            val i_4 = client.anInt7279 * 2
            for (dir in 0 until 3) {
                val i_17 = client.anIntArrayArrayArray7302[client.anInt7277][i_4][dir] * 3
                val i_7 = client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 1][dir] * 3
                val i_8 = (client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][dir] - (client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 3][dir] - client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][dir])) * 3
                val i_9 = client.anIntArrayArrayArray7302[client.anInt7277][i_4][dir]
                val i_10 = i_7 - i_17
                val i_18 = i_17 - i_7 * 2 + i_8
                val i_12 = client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][dir] - i_9 + i_7 - i_8
                from[dir] = i_9 + ((i_12 * f_2 + i_18) * f_2 + i_10) * f_2
            }
            val baseCoords = Resource.mapRegion!!.getBaseCoordinates()
            cameraPositionAbsoluteX = from[0].toInt() - baseCoords.x * 512
            cameraPositionAbsoluteZ = from[1].toInt() * -1
            cameraPositionAbsoluteY = from[2].toInt() - baseCoords.y * 512
            val to = FloatArray(3)
            val i_7 = client.anInt7280 * 2
            for (dir in 0 until 3) {
                val i_9 = client.anIntArrayArrayArray7302[client.anInt7448][i_7][dir] * 3
                val i_10 = client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 1][dir] * 3
                val i_18 = (client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 2][dir] - (client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 3][dir] - client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 2][dir])) * 3
                val i_12 = client.anIntArrayArrayArray7302[client.anInt7448][i_7][dir]
                val i_13 = i_10 - i_9
                val i_14 = i_9 - i_10 * 2 + i_18
                val i_15 = i_10 + (client.anIntArrayArrayArray7302[client.anInt7448][i_7 + 2][dir] - i_12) - i_18
                to[dir] = f_2 * ((i_14 + f_2 * i_15) * f_2 + i_13) + i_12
            }
            val deltaX = to[0] - from[0]
            val deltaZ = -1.0f * (to[1] - from[1])
            val deltaY = to[2] - from[2]
            val d_11 = sqrt((deltaY * deltaY + deltaX * deltaX).toDouble())
            cameraYaw = (atan2(deltaZ.toDouble(), d_11) * 2607.5945876176133).toInt() and 0x3fff
            cameraRoll = (-atan2(deltaX.toDouble(), deltaY.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            cameraPitch = ((client.anIntArrayArrayArray7302[client.anInt7277][i_4 + 2][3] - client.anIntArrayArrayArray7302[client.anInt7277][i_4][3]) * client.anInt7256 shr 16) + client.anIntArrayArrayArray7302[client.anInt7277][i_4][3]
        }
    }

    @JvmStatic
    fun smoothReset() {
        for (index in 0 until 5) {
            client.CAM_SHAKING[index] = false
        }
        client.anInt7277 = -1
        client.anInt7448 = -1
        moveCameraSpeed = 0
        moveCameraAngle = 0
        cameraCalculationType = 2
        StaticMethods.anInt833 = -1
        PingRequester.anInt5864 = -1
    }

    @JvmStatic
    fun method12606() {
        for (index in 0 until 5) {
            client.CAM_SHAKING[index] = false
        }

        client.anInt7277 = -1
        client.anInt7448 = -1
        moveCameraSpeed = 0
        moveCameraAngle = 0
        cameraCalculationType = 1
        StaticMethods.anInt833 = -1
        PingRequester.anInt5864 = -1
        client.anInt7429 = client.cycles
        anInt2349 = cameraPositionAbsoluteX
        anInt9461 = cameraPositionAbsoluteZ
        anInt9575 = cameraPositionAbsoluteY
        anInt3968 = cameraYaw
        anInt5029 = cameraRoll
    }
}
