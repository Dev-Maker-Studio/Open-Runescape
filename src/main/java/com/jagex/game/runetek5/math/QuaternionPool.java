package com.jagex.game.runetek5.math;

/**
 * 
 * @author Steven - 16/08/2023
 * Represent a cache used to minimize the amount of memory
 * allocated when using quaternions
 * 
 * Documentation: http://www.math.umd.edu/~immortal/MATH431/book/ch_quaternions.pdf
 *
 */
public class QuaternionPool {
	
	public static Quaternion[] pool = new Quaternion[0];
	public static int cursor;
	public static int capacity;
	
	public static void initialize() {
	    capacity = 10;
	    pool = new Quaternion[capacity];
	    cursor = 0;
	}

	public static Quaternion newQuaternion() {
	    synchronized (pool) {
	        Quaternion quaternion;
	        if (cursor == 0) {
	            quaternion = new Quaternion();
	        } else {
	            pool[--cursor].reset();
	            quaternion = pool[cursor];
	        }
	        return quaternion;
	    }
	}

	public static Quaternion quaternionFrom(Quaternion source) {
	    synchronized (pool) {
	        Quaternion destination;
	        if (cursor == 0) {
	            destination = new Quaternion(source);
	        } else {
	            pool[--cursor].copy(source);
	            destination = pool[cursor];
	        }
	        return destination;
	    }
	}

	/**
	 *  Define a quaternion that represents a pure vector (also known as a pure quaternion) 
	 */
	public static Quaternion pureQuaternionFrom(float x, float y, float z) {
	    synchronized (pool) {
	        Quaternion quaternion;
	        if (cursor == 0) {
	            quaternion = new Quaternion(x, y, z);
	        } else {
	            pool[--cursor].set(x, y, z, (float) 0.0);
	            quaternion = pool[cursor];
	        }
	        return quaternion;
	    }
	}
	
	public static Quaternion negateQuaternion(Quaternion source) {
	    Quaternion quaternion = quaternionFrom(source);
	    quaternion.negate();
	    return quaternion;
	}

	public static Quaternion multiply(Quaternion source, Quaternion with) {
	    Quaternion destination = quaternionFrom(source);
	    destination.multiply(with);
	    return destination;
	}
}
