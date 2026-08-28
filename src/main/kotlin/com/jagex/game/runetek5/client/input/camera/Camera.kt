package com.jagex.game.runetek5.client.input.camera

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.atan2
import kotlin.math.sqrt

object Camera {

    @JvmField val projectionMatrix = Matrix44()
    @JvmField var cameraPositionAbsoluteZ: Int = 0
    @JvmField var cameraPositionAbsoluteY: Int = 0
    @JvmField var cameraPositionAbsoluteX: Int = 0
    @JvmField var transitionStartZ: Int = 0
    @JvmField var transitionStartX: Int = 0
    @JvmField var transitionStartY: Int = 0
    @JvmField var cameraYaw: Int = 0
    @JvmField var transitionStartYaw: Int = 0
    @JvmField var camAngleX: Float = 0f
    @JvmField var regularLowestYaw: Int = 0
    @JvmField var camAngleY: Float = 0f
    @JvmField var cameraRandomisationA: Int = 0
    @JvmField var cameraRoll: Int = 0
    @JvmField var transitionStartRoll: Int = 0
    @JvmField var lookY: Int = 0
    @JvmField var lookX: Int = 0
    @JvmField var cameraMovementLocalY: Int = 0
    @JvmField var cameraMovementLocalX: Int = 0
    @JvmField var cameraMovementZ: Int = 0
    @JvmField var cameraConstantMoveSpeed: Int = 0
    @JvmField var cameraPitch: Int = 0
    @JvmField var lookAtHeightOffset: Int = 0
    @JvmField var moveCameraAngle: Int = 0
    @JvmField var moveCameraSpeed: Int = 0
    @JvmField var cameraVariableMoveSpeed: Int = 0
    @JvmField var cameraZoom: Int = 600
    @JvmField var cameraBaseX: Int = 0
    @JvmField var cameraBaseY: Int = 0
    @JvmField var cameraCalculationType: Int = 0
    @JvmField var gameAreaWidth: Int = 0
    @JvmField var gameAreaHeight: Int = 0
    @JvmField var viewProjectionMatrix: Matrix44? = null
    @JvmField val sunMatrix = Matrix43()
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
    fun updateFollowPlayerCamera(multiplier: Long) {
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
        camAngleY += Client.cameraYawVelocity * multiplier / 6.0f
        camAngleX += Client.cameraPitchVelocity * multiplier / 6.0f
        processCamera()
    }

    @JvmStatic
    fun updateOrbitTargetCamera(multiplier: Long) {
        val targetX = Client.cameraTargetAbsX
        val targetY = Client.cameraTargetAbsY
        var offsetFromCamera: Int
        var offset: Int
        if (targetX != cameraBaseX) {
            offsetFromCamera = targetX - cameraBaseX
            offset = (offsetFromCamera * multiplier / 320L).toInt()
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
        if (targetY != cameraBaseY) {
            offsetFromCamera = targetY - cameraBaseY
            offset = (offsetFromCamera * multiplier / 320L).toInt()
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
        camAngleY += 8.0f * (Client.cameraYawVelocity * multiplier / 40.0f)
        camAngleX += 8.0f * (multiplier * Client.cameraPitchVelocity / 40.0f)
        processCamera()
    }

    @JvmStatic
    fun storePickingState(srcSunMatrix: Matrix43, srcProjectionMatrix: Matrix44, areaWidth: Int, areaHeight: Int) {
        if (viewProjectionMatrix == null) {
            viewProjectionMatrix = Matrix44(srcProjectionMatrix)
        } else {
            viewProjectionMatrix!!.copyFrom(srcProjectionMatrix)
        }
        sunMatrix.copyFrom(srcSunMatrix)
        gameAreaWidth = areaWidth
        gameAreaHeight = areaHeight
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
        StaticMethods.cameraTargetAbsY = -1
        PingRequester.cameraTargetAbsX = -1
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
        Client.sendCameraAngleToServer = true
    }

    @JvmStatic
    fun updateSmoothTransitionCamera(areaHeight: Int) {
        val cyclesSinceSmooth = Client.cycles - Client.lastCameraSmoothCycle
        if (cyclesSinceSmooth >= 100) {
            cameraCalculationType = 2
            StaticMethods.cameraTargetAbsY = -1
            PingRequester.cameraTargetAbsX = -1
        } else {
            var yaw = camAngleX.toInt()
            if (regularLowestYaw shr 8 > yaw) {
                yaw = regularLowestYaw shr 8
            }
            if (Client.CAM_SHAKING[4] && Client.CUSTOM_LOWEST_YAW[4] + 128 > yaw) {
                yaw = Client.CUSTOM_LOWEST_YAW[4] + 128
            }
            val roll = (camAngleY.toInt() + cameraRandomisationA) and 0x3fff
            val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
            setCameraPosition(
                cameraBaseX, MapRegion.tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(),
                    PlayerUpdate.MY_PLAYER_PLANE
                ) - 200, cameraBaseY, yaw, roll, (yaw shr 3) * 3 + 600 shl 2, areaHeight
            )
            val progress = 1.0f - (100 - cyclesSinceSmooth) * (100 - cyclesSinceSmooth) * (100 - cyclesSinceSmooth) / 1000000.0f
            cameraPositionAbsoluteX = (transitionStartX + progress * (cameraPositionAbsoluteX - transitionStartX)).toInt()
            cameraPositionAbsoluteZ = (transitionStartZ + progress * (cameraPositionAbsoluteZ - transitionStartZ)).toInt()
            cameraPositionAbsoluteY = ((cameraPositionAbsoluteY - transitionStartY) * progress + transitionStartY).toInt()
            cameraYaw = (transitionStartYaw + progress * (cameraYaw - transitionStartYaw)).toInt()
            var rollDelta = cameraRoll - transitionStartRoll
            if (rollDelta > 8192) {
                rollDelta -= 16384
            } else if (rollDelta < -8192) {
                rollDelta += 16384
            }
            cameraRoll = (transitionStartRoll + progress * rollDelta).toInt()
            cameraRoll = cameraRoll and 0x3fff
        }
    }

    @JvmStatic
    fun setCameraPosition(x: Int, z: Int, y: Int, yaw: Int, roll: Int, baseDeltaY: Int, areaHeight: Int) {
        var areaHeightDelta = areaHeight - 334
        if (areaHeightDelta < 0) {
            areaHeightDelta = 0
        } else if (areaHeightDelta > 100) {
            areaHeightDelta = 100
        }
        val zoomModifier = (Client.cameraZoomMax - Client.cameraZoomMin) * areaHeightDelta / 100 + Client.cameraZoomMin
        var baseDeltaYAdjusted = baseDeltaY * zoomModifier shr 8
        val rotationX = 16384 - yaw and 0x3fff
        val rotationZ = 16384 - roll and 0x3fff
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
        cameraRoll = roll
        cameraPitch = 0
    }

    @JvmStatic
    fun storeViewProjectionMatrix(srcViewProjectionMatrix: Matrix44) {
        StaticMethods.storedViewProjectionMatrix.copyFrom(srcViewProjectionMatrix)
    }

    @JvmStatic
    fun lookat(eyeX: Int, eyeY: Int, heightOffset: Int, speed: Int, angle: Int) {
        lookX = eyeX
        lookY = eyeY
        lookAtHeightOffset = heightOffset
        moveCameraSpeed = speed
        moveCameraAngle = angle
        if (moveCameraAngle >= 100) {
            val lookAbsX = lookX * 512 + 256
            val lookAbsY = lookY * 512 + 256
            val lookAbsZ = MapRegion.tileheightForAbsolutePosition(lookAbsX, lookAbsY, PlayerUpdate.MY_PLAYER_PLANE) - lookAtHeightOffset
            val deltaX = lookAbsX - cameraPositionAbsoluteX
            val deltaZ = lookAbsZ - cameraPositionAbsoluteZ
            val deltaY = lookAbsY - cameraPositionAbsoluteY
            val horizontalDistance = sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toInt()
            cameraYaw = (atan2(deltaZ.toDouble(), horizontalDistance.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            cameraRoll = (atan2(deltaX.toDouble(), deltaY.toDouble()) * -2607.5945876176133).toInt() and 0x3fff
            cameraPitch = 0
            if (cameraYaw < 1024) {
                cameraYaw = 1024
            }
            if (cameraYaw > 3072) {
                cameraYaw = 3072
            }
        }
        cameraCalculationType = 5
        StaticMethods.cameraTargetAbsY = -1
        PingRequester.cameraTargetAbsX = -1
    }

    @JvmStatic
    fun calculateCameraPosition() {
        val absoluteX = cameraMovementLocalX * 512 + 256
        val absoluteY = cameraMovementLocalY * 512 + 256
        val absoluteZ = MapRegion.tileheightForAbsolutePosition(absoluteX, absoluteY, PlayerUpdate.MY_PLAYER_PLANE) - cameraMovementZ
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
            if (cameraPositionAbsoluteZ < absoluteZ) {
                cameraPositionAbsoluteZ = (absoluteZ - cameraPositionAbsoluteZ) * cameraVariableMoveSpeed / 1000 + cameraPositionAbsoluteZ + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteZ > absoluteZ) {
                    cameraPositionAbsoluteZ = absoluteZ
                }
            }
            if (cameraPositionAbsoluteZ > absoluteZ) {
                cameraPositionAbsoluteZ -= cameraVariableMoveSpeed * (cameraPositionAbsoluteZ - absoluteZ) / 1000 + cameraConstantMoveSpeed
                if (cameraPositionAbsoluteZ < absoluteZ) {
                    cameraPositionAbsoluteZ = absoluteZ
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
        val lookAbsZ = MapRegion.tileheightForAbsolutePosition(lookAbsX, lookAbsY, PlayerUpdate.MY_PLAYER_PLANE) - lookAtHeightOffset
        val deltaX = lookAbsX - cameraPositionAbsoluteX
        val deltaZ = lookAbsZ - cameraPositionAbsoluteZ
        val deltaY = lookAbsY - cameraPositionAbsoluteY
        val horizontalDistance = sqrt((deltaY * deltaY + deltaX * deltaX).toDouble()).toInt()
        var yaw = (atan2(deltaZ.toDouble(), horizontalDistance.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
        val roll = (atan2(deltaX.toDouble(), deltaY.toDouble()) * -2607.5945876176133).toInt() and 0x3fff
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
        var rollDelta = roll - cameraRoll
        if (rollDelta > 8192) {
            rollDelta -= 16384
        }
        if (rollDelta < -8192) {
            rollDelta += 16384
        }
        rollDelta = rollDelta shr 3
        if (rollDelta > 0) {
            cameraRoll += rollDelta * moveCameraAngle / 1000 + moveCameraSpeed shl 3
            cameraRoll = cameraRoll and 0x3fff
        }
        if (rollDelta < 0) {
            cameraRoll -= moveCameraSpeed - rollDelta * moveCameraAngle / 1000 shl 3
            cameraRoll = cameraRoll and 0x3fff
        }
        var remainingRollDelta = roll - cameraRoll
        if (remainingRollDelta > 8192) {
            remainingRollDelta -= 16384
        }
        if (remainingRollDelta < -8192) {
            remainingRollDelta += 16384
        }
        if (remainingRollDelta < 0 && rollDelta > 0 || remainingRollDelta > 0 && rollDelta < 0) {
            cameraRoll = roll
        }
        cameraPitch = 0
    }

    @JvmStatic
    fun calculateCutsceneCameraPosition() {
        if (Client.cameraPositionPathIndex != -1 && Client.cameraLookAtPathIndex != -1) {
            val splineSpeed = ((Client.cameraSplineSpeedEnd - Client.cameraSplineSpeedStart) * Client.cameraSplineProgress shr 16) + Client.cameraSplineSpeedStart
            Client.cameraSplineProgress += splineSpeed
            if (Client.cameraSplineProgress >= 65535) {
                Client.cameraSplineProgress = 65535
                Client.cutsceneCameraCompleted = !Client.cutsceneCameraPreviouslyCompleted
                Client.cutsceneCameraPreviouslyCompleted = true
            } else {
                Client.cutsceneCameraCompleted = false
                Client.cutsceneCameraPreviouslyCompleted = false
            }
            val progress = Client.cameraSplineProgress / 65535.0f
            val from = FloatArray(3)
            val positionKeyframeIndex = Client.cameraPositionKeyframe * 2
            for (dir in 0 until 3) {
                val ctrl0 = Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex]!![dir] * 3
                val ctrl1 = Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex + 1]!![dir] * 3
                val ctrl2 = (Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex + 2]!![dir] - (Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex + 3]!![dir] - Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex + 2]!![dir])) * 3
                val start = Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex]!![dir]
                val coeffLinear = ctrl1 - ctrl0
                val coeffQuad = ctrl0 - ctrl1 * 2 + ctrl2
                val coeffCubic = Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex + 2]!![dir] - start + ctrl1 - ctrl2
                from[dir] = start + ((coeffCubic * progress + coeffQuad) * progress + coeffLinear) * progress
            }
            val baseCoords = Resource.mapRegion!!.getBaseCoordinates()
            cameraPositionAbsoluteX = from[0].toInt() - baseCoords.x * 512
            cameraPositionAbsoluteZ = from[1].toInt() * -1
            cameraPositionAbsoluteY = from[2].toInt() - baseCoords.y * 512
            val to = FloatArray(3)
            val lookAtKeyframeIndex = Client.cameraLookAtKeyframe * 2
            for (dir in 0 until 3) {
                val ctrl0 = Client.cameraSplinePaths!![Client.cameraLookAtPathIndex]!![lookAtKeyframeIndex]!![dir] * 3
                val ctrl1 = Client.cameraSplinePaths!![Client.cameraLookAtPathIndex]!![lookAtKeyframeIndex + 1]!![dir] * 3
                val ctrl2 = (Client.cameraSplinePaths!![Client.cameraLookAtPathIndex]!![lookAtKeyframeIndex + 2]!![dir] - (Client.cameraSplinePaths!![Client.cameraLookAtPathIndex]!![lookAtKeyframeIndex + 3]!![dir] - Client.cameraSplinePaths!![Client.cameraLookAtPathIndex]!![lookAtKeyframeIndex + 2]!![dir])) * 3
                val start = Client.cameraSplinePaths!![Client.cameraLookAtPathIndex]!![lookAtKeyframeIndex]!![dir]
                val coeffLinear = ctrl1 - ctrl0
                val coeffQuad = ctrl0 - ctrl1 * 2 + ctrl2
                val coeffCubic = ctrl1 + (Client.cameraSplinePaths!![Client.cameraLookAtPathIndex]!![lookAtKeyframeIndex + 2]!![dir] - start) - ctrl2
                to[dir] = progress * ((coeffQuad + progress * coeffCubic) * progress + coeffLinear) + start
            }
            val deltaX = to[0] - from[0]
            val deltaZ = -1.0f * (to[1] - from[1])
            val deltaY = to[2] - from[2]
            val horizontalDistance = sqrt((deltaY * deltaY + deltaX * deltaX).toDouble())
            cameraYaw = (atan2(deltaZ.toDouble(), horizontalDistance) * 2607.5945876176133).toInt() and 0x3fff
            cameraRoll = (-atan2(deltaX.toDouble(), deltaY.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            cameraPitch = ((Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex + 2]!![3] - Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex]!![3]) * Client.cameraSplineProgress shr 16) + Client.cameraSplinePaths!![Client.cameraPositionPathIndex]!![positionKeyframeIndex]!![3]
        }
    }

    @JvmStatic
    fun smoothReset() {
        for (index in 0 until 5) {
            Client.CAM_SHAKING[index] = false
        }
        Client.cameraPositionPathIndex = -1
        Client.cameraLookAtPathIndex = -1
        moveCameraSpeed = 0
        moveCameraAngle = 0
        cameraCalculationType = 2
        StaticMethods.cameraTargetAbsY = -1
        PingRequester.cameraTargetAbsX = -1
    }

    @JvmStatic
    fun startSmoothTransition() {
        for (index in 0 until 5) {
            Client.CAM_SHAKING[index] = false
        }

        Client.cameraPositionPathIndex = -1
        Client.cameraLookAtPathIndex = -1
        moveCameraSpeed = 0
        moveCameraAngle = 0
        cameraCalculationType = 1
        StaticMethods.cameraTargetAbsY = -1
        PingRequester.cameraTargetAbsX = -1
        Client.lastCameraSmoothCycle = Client.cycles
        transitionStartX = cameraPositionAbsoluteX
        transitionStartZ = cameraPositionAbsoluteZ
        transitionStartY = cameraPositionAbsoluteY
        transitionStartYaw = cameraYaw
        transitionStartRoll = cameraRoll
    }
}
