package com.jagex.game.runetek5.math;

/**
 * 
 * @author Steven - 16/08/2023
 *
 */
public class Vector3Pool {

	public static Vector3[] pool = new Vector3[0];
	public static int cursor;
	public static int capacity;
	
	public static void initVectorStack() {
	    capacity = 100;
	    pool = new Vector3[capacity];
	    cursor = 0;
	}
	
	public static Vector3 vector3From(Vector3 source) {
	    synchronized (pool) {
	        Vector3 vector3;
	        if (cursor == 0) {
	            vector3 = new Vector3(source);
	        } else {
	            pool[--cursor].copy(source);
	            vector3 = pool[cursor];
	        }
	        return vector3;
	    }
	}
	
	public static Vector3 vector3From(float x, float y, float z) {
	    synchronized (pool) {
	        Vector3 vector3;
	        if (cursor == 0) {
	            vector3 = new Vector3(x, y, z);
	        } else {
	            pool[--cursor].set(x, y, z);
	            vector3 = pool[cursor];
	        }
	        return vector3;
	    }
	}
	
	public static Vector3 delta(Vector3 a, Vector3 b) {
	    Vector3 temp = vector3From(a);
	    temp.subtract(b);
	    return temp;
	}
}
