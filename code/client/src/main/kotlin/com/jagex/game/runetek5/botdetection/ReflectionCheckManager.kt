package com.jagex.game.runetek5.botdetection

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.io.OptionalDataException
import java.io.StreamCorruptedException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.botdetection.ReflectionCheckType.Companion.GET_FIELD_VALUE
import com.jagex.game.runetek5.botdetection.ReflectionCheckType.Companion.SET_FIELD_VALUE
import com.jagex.game.runetek5.botdetection.ReflectionCheckType.Companion.GET_FIELD_MODIFIERS
import com.jagex.game.runetek5.botdetection.ReflectionCheckType.Companion.GET_METHOD_RESULT
import com.jagex.game.runetek5.botdetection.ReflectionCheckType.Companion.GET_METHOD_MODIFIERS

object ReflectionCheckManager {

    @JvmField var PENDING_REFLECTION_CHECKS: NodeCollection = NodeCollection()

    @JvmStatic
    fun setupPendingChecks() {
        PENDING_REFLECTION_CHECKS = NodeCollection()
    }

    @JvmStatic
    fun hasPendingChecks(): Boolean {
        val checks = PENDING_REFLECTION_CHECKS.getFirst() as ReflectionCheck?
        return checks != null
    }

    @JvmStatic
    fun requestReflectionCheck(buffer: Packet) {
        val check = ReflectionCheck()
        check.operationCount = buffer.readUByte()
        check.header = buffer.readInt()
        check.operations = IntArray(check.operationCount)
        check.exceptionsEncountered = IntArray(check.operationCount)
        check.fieldsToCheck = arrayOfNulls<Field>(check.operationCount) as Array<Field>?
        check.fieldInput = IntArray(check.operationCount)
        check.methodsToCheck = arrayOfNulls<Method>(check.operationCount) as Array<Method>?
        check.argumentBuffers = Array<Array<ByteArray?>>(check.operationCount) { arrayOf() }

        for (operationIndex in 0 until check.operationCount) {
            try {
                val operation = buffer.readUByte()
                if (operation == GET_FIELD_VALUE || operation == SET_FIELD_VALUE || operation == GET_FIELD_MODIFIERS) {
                    val classType = buffer.readString()
                    val fieldName = buffer.readString()
                    var fieldInput = 0
                    if (operation == SET_FIELD_VALUE) {
                        fieldInput = buffer.readInt()
                    }
                    check.operations!![operationIndex] = operation
                    check.fieldInput!![operationIndex] = fieldInput
                    if (getTypeClass(classType).classLoader == null) {
                        throw SecurityException()
                    }
                    check.fieldsToCheck!![operationIndex] = getTypeClass(classType).getDeclaredField(fieldName)
                } else if (operation == GET_METHOD_RESULT || operation == GET_METHOD_MODIFIERS) {
                    val classType = buffer.readString()
                    val expectedMethodName = buffer.readString()
                    val expectedParameterCount = buffer.readUByte()
                    val argumentTypeNames = Array(expectedParameterCount) { "" }
                    for (argumentIndex in 0 until expectedParameterCount) {
                        argumentTypeNames[argumentIndex] = buffer.readString()
                    }
                    val methodReturnType = buffer.readString()
                    val argumentBuffer = Array<ByteArray?>(expectedParameterCount) { null }
                    if (operation == GET_METHOD_RESULT) {
                        for (argumentIndex in 0 until expectedParameterCount) {
                            val bufferLength = buffer.readInt()
                            argumentBuffer[argumentIndex] = ByteArray(bufferLength)
                            buffer.readBytes(argumentBuffer[argumentIndex]!!, 0, bufferLength)
                        }
                    }
                    check.operations!![operationIndex] = operation
                    val expectedParameterTypes = Array<Class<*>?>(expectedParameterCount) { null }
                    for (argumentIndex in 0 until expectedParameterCount) {
                        expectedParameterTypes[argumentIndex] = getTypeClass(argumentTypeNames[argumentIndex])
                    }
                    val expectedReturnType = getTypeClass(methodReturnType)
                    if (getTypeClass(classType).classLoader == null) {
                        throw SecurityException()
                    }
                    val methodsToCheck = getTypeClass(classType).declaredMethods
                    for (methodIndex in methodsToCheck.indices) {
                        val methodToCheck = methodsToCheck[methodIndex]
                        if (methodToCheck.name == expectedMethodName) {
                            val actualParameterTypes = methodToCheck.parameterTypes
                            if (expectedParameterTypes.size == actualParameterTypes.size) {
                                var parametersValid = true
                                for (argumentIndex in expectedParameterTypes.indices) {
                                    if (expectedParameterTypes[argumentIndex] != actualParameterTypes[argumentIndex]) {
                                        parametersValid = false
                                        break
                                    }
                                }
                                if (parametersValid && expectedReturnType == methodToCheck.returnType) {
                                    check.methodsToCheck!![operationIndex] = methodToCheck
                                }
                            }
                        }
                    }
                    check.argumentBuffers!![operationIndex] = argumentBuffer
                }
            } catch (classNotFoundException: ClassNotFoundException) {
                check.exceptionsEncountered!![operationIndex] = -1
            } catch (securityException: SecurityException) {
                check.exceptionsEncountered!![operationIndex] = -2
            } catch (nullpointerException: NullPointerException) {
                check.exceptionsEncountered!![operationIndex] = -3
            } catch (exception: Exception) {
                check.exceptionsEncountered!![operationIndex] = -4
            } catch (throwable: Throwable) {
                check.exceptionsEncountered!![operationIndex] = -5
            }
        }
        PENDING_REFLECTION_CHECKS.addFirst(check)
    }

    @JvmStatic
    fun collectResult(bitBuffer: Packet.Bit) {
        val reflectionCheck = PENDING_REFLECTION_CHECKS.getFirst() as ReflectionCheck?
        if (reflectionCheck != null) {
            val bufferStartPosition = bitBuffer.currentPosition
            bitBuffer.writeInt(reflectionCheck.header)
            for (operationIndex in 0 until reflectionCheck.operationCount) {
                if (reflectionCheck.exceptionsEncountered!![operationIndex] != 0) {
                    bitBuffer.writeByte(reflectionCheck.exceptionsEncountered!![operationIndex])
                } else try {
                    val operation = reflectionCheck.operations!![operationIndex]
                    if (operation == GET_FIELD_VALUE) {
                        val fieldToCheck = reflectionCheck.fieldsToCheck!![operationIndex]
                        val fieldValue = fieldToCheck!!.getInt(null)
                        bitBuffer.writeByte(0)
                        bitBuffer.writeInt(fieldValue)
                    } else if (operation == SET_FIELD_VALUE) {
                        val fieldToCheck = reflectionCheck.fieldsToCheck!![operationIndex]
                        fieldToCheck!!.setInt(null, reflectionCheck.fieldInput!![operationIndex])
                        bitBuffer.writeByte(0)
                    } else if (operation == GET_FIELD_MODIFIERS) {
                        val fieldToCheck = reflectionCheck.fieldsToCheck!![operationIndex]
                        val modifiers = fieldToCheck!!.modifiers
                        bitBuffer.writeByte(0)
                        bitBuffer.writeInt(modifiers)
                    } else if (operation == GET_METHOD_RESULT) {
                        val methodToCheck = reflectionCheck.methodsToCheck!![operationIndex]
                        val argumentBuffer = reflectionCheck.argumentBuffers!![operationIndex]
                        val arguments = Array<Any?>(argumentBuffer.size) { null }

                        for (argumentIndex in argumentBuffer.indices) {
                            val objectInputStream = ObjectInputStream(ByteArrayInputStream(argumentBuffer[argumentIndex]))
                            arguments[argumentIndex] = objectInputStream.readObject()
                        }

                        val methodResult = methodToCheck!!.invoke(null, *arguments)
                        when {
                            methodResult == null -> {
                                bitBuffer.writeByte(0)
                            }
                            methodResult is Number -> {
                                bitBuffer.writeByte(1)
                                bitBuffer.writeLong(methodResult.toLong())
                            }
                            methodResult is String -> {
                                bitBuffer.writeByte(2)
                                bitBuffer.writeString(methodResult)
                            }
                            else -> {
                                bitBuffer.writeByte(4)
                            }
                        }
                    } else if (operation == GET_METHOD_MODIFIERS) {
                        val methodToCheck = reflectionCheck.methodsToCheck!![operationIndex]
                        val modifiers = methodToCheck!!.modifiers
                        bitBuffer.writeByte(0)
                        bitBuffer.writeInt(modifiers)
                    }
                } catch (classnotfoundexception: ClassNotFoundException) {
                    bitBuffer.writeByte(-10)
                } catch (invalidclassexception: InvalidClassException) {
                    bitBuffer.writeByte(-11)
                } catch (streamcorruptedexception: StreamCorruptedException) {
                    bitBuffer.writeByte(-12)
                } catch (optionaldataexception: OptionalDataException) {
                    bitBuffer.writeByte(-13)
                } catch (illegalaccessexception: IllegalAccessException) {
                    bitBuffer.writeByte(-14)
                } catch (illegalargumentexception: IllegalArgumentException) {
                    bitBuffer.writeByte(-15)
                } catch (invocationtargetexception: InvocationTargetException) {
                    bitBuffer.writeByte(-16)
                } catch (securityexception: SecurityException) {
                    bitBuffer.writeByte(-17)
                } catch (ioexception: IOException) {
                    bitBuffer.writeByte(-18)
                } catch (nullpointerexception: NullPointerException) {
                    bitBuffer.writeByte(-19)
                } catch (exception: Exception) {
                    bitBuffer.writeByte(-20)
                } catch (throwable: Throwable) {
                    bitBuffer.writeByte(-21)
                }
            }
            bitBuffer.writeCrc(bufferStartPosition)
            reflectionCheck.unlink()
        }
    }

    @JvmStatic
    @Throws(ClassNotFoundException::class)
    fun getTypeClass(type: String): Class<*> {
        return when (type) {
            "B" -> Byte::class.javaPrimitiveType!!
            "I" -> Int::class.javaPrimitiveType!!
            "S" -> Short::class.javaPrimitiveType!!
            "J" -> Long::class.javaPrimitiveType!!
            "Z" -> Boolean::class.javaPrimitiveType!!
            "F" -> Float::class.javaPrimitiveType!!
            "D" -> Double::class.javaPrimitiveType!!
            "C" -> Char::class.javaPrimitiveType!!
            "void" -> Void.TYPE
            else -> Class.forName(type)
        }
    }
}
