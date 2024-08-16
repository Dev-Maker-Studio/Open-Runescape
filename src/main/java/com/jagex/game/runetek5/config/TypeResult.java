package com.jagex.game.runetek5.config;

public class TypeResult {

    public Object mainObject;
    public Object companionObject;

    public TypeResult(Object main, Object companion) {
        this.mainObject = main;
        this.companionObject = companion;
    }

    public String toString() {
        return this.mainObject + ", " + this.companionObject;
    }

    public boolean equals(Object object) {
        if (object instanceof TypeResult typeResult) {
            if (this.mainObject == null) {
                if (typeResult.mainObject != null) {
                    return false;
                }
            } else if (!this.mainObject.equals(typeResult.mainObject)) {
                return false;
            }

            if (this.companionObject == null) {
                return typeResult.companionObject == null;
            }
            return this.companionObject.equals(typeResult.companionObject);
        } 
        return false;
    }

    public int hashCode() {
        int hashcode = 0;
        if (this.mainObject != null) {
            hashcode += this.mainObject.hashCode();
        }

        if (this.companionObject != null) {
            hashcode += 31 * this.companionObject.hashCode();
        }

        return hashcode;
    }

}
