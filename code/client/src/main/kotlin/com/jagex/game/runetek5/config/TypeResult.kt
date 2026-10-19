package com.jagex.game.runetek5.config

class TypeResult(
    @JvmField var mainObject: Any?,
    @JvmField var companionObject: Any?
) {

    override fun toString(): String {
        return "$mainObject, $companionObject"
    }

    override fun equals(other: Any?): Boolean {
        if (other is TypeResult) {
            if (this.mainObject == null) {
                if (other.mainObject != null) {
                    return false
                }
            } else if (this.mainObject != other.mainObject) {
                return false
            }

            if (this.companionObject == null) {
                return other.companionObject == null
            }
            return this.companionObject == other.companionObject
        }
        return false
    }

    override fun hashCode(): Int {
        var hashcode = 0
        if (this.mainObject != null) {
            hashcode += this.mainObject.hashCode()
        }

        if (this.companionObject != null) {
            hashcode += 31 * this.companionObject.hashCode()
        }

        return hashcode
    }
}
