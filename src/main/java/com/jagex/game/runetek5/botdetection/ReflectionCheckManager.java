package com.jagex.game.runetek5.botdetection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import static com.jagex.game.runetek5.botdetection.ReflectionCheckType.*;

public class ReflectionCheckManager {
	
	public static NodeCollection PENDING_REFLECTION_CHECKS = new NodeCollection();

    public static void setupPendingChecks() {
        PENDING_REFLECTION_CHECKS = new NodeCollection();
    }

    public static boolean hasPendingChecks() {
        ReflectionCheck checks = (ReflectionCheck) PENDING_REFLECTION_CHECKS.getFirst();
        return checks != null;
    }

    public static void requestReflectionCheck(Packet buffer) {
        ReflectionCheck check = new ReflectionCheck();
        check.operationCount = buffer.readUByte();
        check.header = buffer.readInt();
        check.operations = new int[check.operationCount];
        check.exceptionsEncountered = new int[check.operationCount];
        check.fieldsToCheck = new Field[check.operationCount];
        check.fieldInput = new int[check.operationCount];
        check.methodsToCheck = new Method[check.operationCount];
        check.argumentBuffers = new byte[check.operationCount][][];

        for (int operationIndex = 0; operationIndex < check.operationCount; operationIndex++) {
            try {
                int operation = buffer.readUByte();
                if (operation == GET_FIELD_VALUE || operation == SET_FIELD_VALUE || operation == GET_FIELD_MODIFIERS) {
                	String classType = buffer.readString();
                	String fieldName = buffer.readString();
                    int fieldInput = 0;
                    if (operation == SET_FIELD_VALUE) {
                        fieldInput = buffer.readInt();
                    }
                    check.operations[operationIndex] = operation;
                    check.fieldInput[operationIndex] = fieldInput;
                    if (getTypeClass(classType).getClassLoader() == null) {
                        throw new SecurityException();
                    }
                    check.fieldsToCheck[operationIndex] = getTypeClass(classType).getDeclaredField(fieldName);
                } else if (operation == GET_METHOD_RESULT || operation == GET_METHOD_MODIFIERS) {
                	String classType = buffer.readString();
                	String expectedMethodName = buffer.readString();
                	int expectedParameterCount = buffer.readUByte();
                    String[] argumentTypeNames = new String[expectedParameterCount];
                    for (int argumentIndex = 0; argumentIndex < expectedParameterCount; argumentIndex++) {
                        argumentTypeNames[argumentIndex] = buffer.readString();
                    }
                    String methodReturnType = buffer.readString();
                    byte[][] argumentBuffer = new byte[expectedParameterCount][];
                    if (operation == GET_METHOD_RESULT) {
                        for (int argumentIndex = 0; argumentIndex < expectedParameterCount; argumentIndex++) {
                            int bufferLength = buffer.readInt();
                            argumentBuffer[argumentIndex] = new byte[bufferLength];
                            buffer.readBytes(argumentBuffer[argumentIndex], 0, bufferLength);
                        }
                    }
                    check.operations[operationIndex] = operation;
                    Class<?>[] expectedParameterTypes = new Class[expectedParameterCount];
                    for (int argumentIndex = 0; argumentIndex < expectedParameterCount; argumentIndex++) {
                        expectedParameterTypes[argumentIndex] = getTypeClass(argumentTypeNames[argumentIndex]);
                    }
                    Class<?> expectedReturnType = getTypeClass(methodReturnType);
                    if (getTypeClass(classType).getClassLoader() == null) {
                        throw new SecurityException();
                    }
                    Method[] methodsToCheck = getTypeClass(classType).getDeclaredMethods();
                    for (int methodIndex = 0; methodIndex < methodsToCheck.length; methodIndex++) {
                        Method methodToCheck = methodsToCheck[methodIndex];
                        if (methodToCheck.getName().equals(expectedMethodName)) {
                            Class<?>[] actualParameterTypes = methodToCheck.getParameterTypes();
                            if (expectedParameterTypes.length == actualParameterTypes.length) {
                                boolean parametersValid = true;
                                for (int argumentIndex = 0; argumentIndex < expectedParameterTypes.length; argumentIndex++) {
                                    if (expectedParameterTypes[argumentIndex] != actualParameterTypes[argumentIndex]) {
                                        parametersValid = false;
                                        break;
                                    }
                                }
                                if (parametersValid && expectedReturnType == methodToCheck.getReturnType()) {
                                    check.methodsToCheck[operationIndex] = methodToCheck;
                                }
                            }
                        }
                    }
                    check.argumentBuffers[operationIndex] = argumentBuffer;
                }
            } catch (ClassNotFoundException classNotFoundException) {
                check.exceptionsEncountered[operationIndex] = -1;
            } catch (SecurityException securityException) {
                check.exceptionsEncountered[operationIndex] = -2;
            } catch (NullPointerException nullpointerException) {
                check.exceptionsEncountered[operationIndex] = -3;
            } catch (Exception exception) {
                check.exceptionsEncountered[operationIndex] = -4;
            } catch (Throwable throwable) {
                check.exceptionsEncountered[operationIndex] = -5;
            }
        }
        PENDING_REFLECTION_CHECKS.addFirst(check);
    }

	public static void collectResult(Packet.Bit bitBuffer) {
	    ReflectionCheck reflectionCheck = (ReflectionCheck) PENDING_REFLECTION_CHECKS.getFirst();
	    if (reflectionCheck != null) {
	        int bufferStartPosition = bitBuffer.currentPosition;
			bitBuffer.writeInt(reflectionCheck.header);
	        for (int operationIndex = 0; operationIndex < reflectionCheck.operationCount; operationIndex++) {
	            if (reflectionCheck.exceptionsEncountered[operationIndex] != 0) {
	                bitBuffer.writeByte(reflectionCheck.exceptionsEncountered[operationIndex]);
	            } else try {
                    int operation = reflectionCheck.operations[operationIndex];
                    if (operation == GET_FIELD_VALUE) {
                    	Field fieldToCheck = reflectionCheck.fieldsToCheck[operationIndex];
                        int fieldValue = fieldToCheck.getInt(null);
                        bitBuffer.writeByte(0);
                        bitBuffer.writeInt(fieldValue);
                    } else if (operation == SET_FIELD_VALUE) {
                    	Field fieldToCheck = reflectionCheck.fieldsToCheck[operationIndex];
                        fieldToCheck.setInt(null, reflectionCheck.fieldInput[operationIndex]);
                        bitBuffer.writeByte(0);
                    } else if (operation == GET_FIELD_MODIFIERS) {
                    	Field fieldToCheck = reflectionCheck.fieldsToCheck[operationIndex];
                    	int modifiers = fieldToCheck.getModifiers();
                        bitBuffer.writeByte(0);
                        bitBuffer.writeInt(modifiers);
                    } else if (operation == GET_METHOD_RESULT) {
                        Method methodToCheck = reflectionCheck.methodsToCheck[operationIndex];
                        byte[][] argumentBuffer = reflectionCheck.argumentBuffers[operationIndex];
                        Object[] arguments = new Object[argumentBuffer.length];

                        for (int argumentIndex = 0; argumentIndex < argumentBuffer.length; argumentIndex++) {
                            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(argumentBuffer[argumentIndex]));
                            arguments[argumentIndex] = objectInputStream.readObject();
                        }

                        Object methodResult = methodToCheck.invoke(null, arguments);
                        if (methodResult == null) {
                            bitBuffer.writeByte(0);
                        } else if (methodResult instanceof Number) {
                            bitBuffer.writeByte(1);
                            bitBuffer.writeLong(((Number) methodResult).longValue());
                        } else if (methodResult instanceof String) {
                            bitBuffer.writeByte(2);
                            bitBuffer.writeString((String) methodResult);
                        } else {
                            bitBuffer.writeByte(4);
                        }
                    } else if (operation == GET_METHOD_MODIFIERS) {
                        Method methodToCheck = reflectionCheck.methodsToCheck[operationIndex];
                        int modifiers = methodToCheck.getModifiers();
                        bitBuffer.writeByte(0);
                        bitBuffer.writeInt(modifiers);
                    }
                } catch (ClassNotFoundException classnotfoundexception) {
                    bitBuffer.writeByte(-10);
                } catch (InvalidClassException invalidclassexception) {
                    bitBuffer.writeByte(-11);
                } catch (StreamCorruptedException streamcorruptedexception) {
                    bitBuffer.writeByte(-12);
                } catch (OptionalDataException optionaldataexception) {
                    bitBuffer.writeByte(-13);
                } catch (IllegalAccessException illegalaccessexception) {
                    bitBuffer.writeByte(-14);
                } catch (IllegalArgumentException illegalargumentexception) {
                    bitBuffer.writeByte(-15);
                } catch (InvocationTargetException invocationtargetexception) {
                    bitBuffer.writeByte(-16);
                } catch (SecurityException securityexception) {
                    bitBuffer.writeByte(-17);
                } catch (IOException ioexception) {
                    bitBuffer.writeByte(-18);
                } catch (NullPointerException nullpointerexception) {
                    bitBuffer.writeByte(-19);
                } catch (Exception exception) {
                    bitBuffer.writeByte(-20);
                } catch (Throwable throwable) {
                    bitBuffer.writeByte(-21);
                }
	        }
	        bitBuffer.writeCrc(bufferStartPosition);
	        reflectionCheck.unlink();
	    }
	}
	
	public static Class<?> getTypeClass(String type) throws ClassNotFoundException {
		if ("B".equals(type)) {
			return Byte.TYPE;
		}
		if ("I".equals(type)) {
			return Integer.TYPE;
		}
		if ("S".equals(type)) {
			return Short.TYPE;
		}
		if ("J".equals(type)) {
			return Long.TYPE;
		}
		if ("Z".equals(type)) {
			return Boolean.TYPE;
		}
		if ("F".equals(type)) {
			return Float.TYPE;
		}
		if ("D".equals(type)) {
			return Double.TYPE;
		}
		if ("C".equals(type)) {
			return Character.TYPE;
		}
		if ("void".equals(type)) {
			return Void.TYPE;
		}
		return Class.forName(type);
	}
}
