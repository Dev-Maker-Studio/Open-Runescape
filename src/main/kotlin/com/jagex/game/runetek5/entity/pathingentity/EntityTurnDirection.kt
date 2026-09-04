package com.jagex.game.runetek5.entity.pathingentity

/**
 * Manages entity rotation with acceleration and deceleration.
 */
class EntityTurnDirection {

    @JvmField var rotation: Int = 0
    @JvmField var velocity: Int = 0

    /**
     * Calculates rotation towards a target with acceleration.
     *
     * @param targetRotation The target rotation angle
     * @param acceleration The acceleration rate
     * @param maxVelocity The maximum velocity
     * @return True if actively accelerating towards target
     */
    fun calculateRotation(targetRotation: Int, acceleration: Int, maxVelocity: Int): Boolean {
        val previousVelocity = velocity

        if (rotation == targetRotation && velocity == 0) {
            return false
        }

        val shouldAccelerate: Boolean = if (velocity != 0) {
            when {
                velocity > 0 && targetRotation > rotation -> {
                    val stoppingDistance = velocity * velocity / (acceleration * 2)
                    val projectedPosition = stoppingDistance + rotation
                    projectedPosition < targetRotation && projectedPosition >= rotation
                }
                velocity < 0 && targetRotation < rotation -> {
                    val stoppingDistance = velocity * velocity / (acceleration * 2)
                    val projectedPosition = rotation - stoppingDistance
                    projectedPosition > targetRotation && projectedPosition <= rotation
                }
                else -> false
            }
        } else {
            if (targetRotation > rotation && targetRotation <= acceleration + rotation ||
                targetRotation < rotation && targetRotation >= rotation - acceleration) {
                rotation = targetRotation
                return false
            }
            true
        }

        if (shouldAccelerate) {
            if (targetRotation > rotation) {
                velocity += acceleration
                if (maxVelocity != 0 && velocity > maxVelocity) {
                    velocity = maxVelocity
                }
            } else {
                velocity -= acceleration
                if (maxVelocity != 0 && velocity < -maxVelocity) {
                    velocity = -maxVelocity
                }
            }

            if (previousVelocity != velocity) {
                val stoppingDistance = velocity * velocity / (acceleration * 2)
                if (targetRotation > rotation) {
                    if (stoppingDistance + rotation > targetRotation) {
                        velocity = previousVelocity
                    }
                } else if (rotation - stoppingDistance < targetRotation) {
                    velocity = previousVelocity
                }
            }
        } else {
            if (velocity > 0) {
                velocity -= acceleration
                if (velocity < 0) {
                    velocity = 0
                }
            } else {
                velocity += acceleration
                if (velocity > 0) {
                    velocity = 0
                }
            }
        }

        rotation += (previousVelocity + velocity) shr 1
        return shouldAccelerate
    }

    /**
     * Sets the rotation directly.
     *
     * @param rotation The new rotation angle
     */
    fun setRotation(rotation: Int) {
        this.rotation = rotation
        velocity = 0
    }

    /**
     * Gets the clamped rotation.
     *
     * @return The rotation clamped to 14 bits
     */
    fun getRotation(): Int {
        return rotation and 0x3fff
    }

    /**
     * Clamps the rotation to 14 bits.
     */
    fun clamp() {
        rotation = rotation and 0x3fff
    }
}
