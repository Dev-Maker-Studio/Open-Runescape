package com.jagex.game.runetek5.botdetection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.jagex.game.runetek5.cache.node.Node;

public class ReflectionCheck extends Node {

    public int operationCount;
	public Method[] methodsToCheck;
	public int[] operations;
	public int[] exceptionsEncountered;
	public Field[] fieldsToCheck;
	public int header;
	public int[] fieldInput;
	public byte[][][] argumentBuffers;
	
}