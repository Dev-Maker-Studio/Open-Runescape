package com.jagex.game.runetek5.utils

import java.util.Random

import com.jagex.game.runetek5.cache.node.CacheableNodeQueue
import com.jagex.game.runetek5.cache.node.lru.CacheableNodeByteArray
import com.jagex.game.runetek5.config.material.texture.operation.TextureShapeRasterizer
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpRasterizer
import com.jagex.game.runetek5.staticmethods.StaticMethods

object ArrayUtils {

	/**
	 * ChatGPT 4:
	 * public static void array_copy(int[] source, int srcPos, int[] destination, int destPos, int length) {
    if (destination == source) {
        if (destPos == srcPos) {
            return;
        }

        if (destPos > srcPos && destPos < srcPos + length) {
            destPos += length - 1;
            srcPos += length - 1;
            for (int i = length - 1; i >= 0; i--) {
                destination[destPos--] = source[srcPos--];
            }
            return;
        }
    }

    for (int i = 0; i < length; i++) {
        destination[destPos++] = source[srcPos++];
    }
}
	 */
	@JvmStatic
	fun arrayCopy(ints_0: IntArray, i_1: Int, ints_2: IntArray, i_3: Int, i_4: Int) {
		var i_1 = i_1
		var i_3 = i_3
		var i_4 = i_4
		if (ints_2 === ints_0) {
			if (i_3 == i_1) {
				return
			}

			if (i_3 > i_1 && i_3 < i_4 + i_1) {
				--i_4
				i_1 += i_4
				i_3 += i_4
				i_4 = i_1 - i_4

				i_4 += 7
				while (i_1 >= i_4) {
					ints_2[i_3--] = ints_0[i_1--]
					ints_2[i_3--] = ints_0[i_1--]
					ints_2[i_3--] = ints_0[i_1--]
					ints_2[i_3--] = ints_0[i_1--]
					ints_2[i_3--] = ints_0[i_1--]
					ints_2[i_3--] = ints_0[i_1--]
					ints_2[i_3--] = ints_0[i_1--]
					ints_2[i_3--] = ints_0[i_1--]
				}

				i_4 -= 7
				while (i_1 >= i_4) {
					ints_2[i_3--] = ints_0[i_1--]
				}

				return
			}
		}

		i_4 += i_1

		i_4 -= 7
		while (i_1 < i_4) {
			ints_2[i_3++] = ints_0[i_1++]
			ints_2[i_3++] = ints_0[i_1++]
			ints_2[i_3++] = ints_0[i_1++]
			ints_2[i_3++] = ints_0[i_1++]
			ints_2[i_3++] = ints_0[i_1++]
			ints_2[i_3++] = ints_0[i_1++]
			ints_2[i_3++] = ints_0[i_1++]
			ints_2[i_3++] = ints_0[i_1++]
		}

		i_4 += 7
		while (i_1 < i_4) {
			ints_2[i_3++] = ints_0[i_1++]
		}

	}

	@JvmStatic
	fun arrayCopy(bytes_0: ByteArray, i_1: Int, bytes_2: ByteArray, i_3: Int, i_4: Int) {
		var i_1 = i_1
		var i_3 = i_3
		var i_4 = i_4
		if (bytes_2 === bytes_0) {
			if (i_3 == i_1) {
				return
			}

			if (i_3 > i_1 && i_3 < i_4 + i_1) {
				--i_4
				i_1 += i_4
				i_3 += i_4
				i_4 = i_1 - i_4

				i_4 += 7
				while (i_1 >= i_4) {
					bytes_2[i_3--] = bytes_0[i_1--]
					bytes_2[i_3--] = bytes_0[i_1--]
					bytes_2[i_3--] = bytes_0[i_1--]
					bytes_2[i_3--] = bytes_0[i_1--]
					bytes_2[i_3--] = bytes_0[i_1--]
					bytes_2[i_3--] = bytes_0[i_1--]
					bytes_2[i_3--] = bytes_0[i_1--]
					bytes_2[i_3--] = bytes_0[i_1--]
				}

				i_4 -= 7
				while (i_1 >= i_4) {
					bytes_2[i_3--] = bytes_0[i_1--]
				}

				return
			}
		}

		i_4 += i_1

		i_4 -= 7
		while (i_1 < i_4) {
			bytes_2[i_3++] = bytes_0[i_1++]
			bytes_2[i_3++] = bytes_0[i_1++]
			bytes_2[i_3++] = bytes_0[i_1++]
			bytes_2[i_3++] = bytes_0[i_1++]
			bytes_2[i_3++] = bytes_0[i_1++]
			bytes_2[i_3++] = bytes_0[i_1++]
			bytes_2[i_3++] = bytes_0[i_1++]
			bytes_2[i_3++] = bytes_0[i_1++]
		}

		i_4 += 7
		while (i_1 < i_4) {
			bytes_2[i_3++] = bytes_0[i_1++]
		}

	}

	@JvmStatic
	fun arrayCopy(longs_0: LongArray, i_1: Int, longs_2: LongArray, i_3: Int, i_4: Int) {
		var i_1 = i_1
		var i_3 = i_3
		var i_4 = i_4
		if (longs_2 === longs_0) {
			if (i_3 == i_1) {
				return
			}

			if (i_3 > i_1 && i_3 < i_4 + i_1) {
				--i_4
				i_1 += i_4
				i_3 += i_4
				i_4 = i_1 - i_4

				i_4 += 3
				while (i_1 >= i_4) {
					longs_2[i_3--] = longs_0[i_1--]
					longs_2[i_3--] = longs_0[i_1--]
					longs_2[i_3--] = longs_0[i_1--]
					longs_2[i_3--] = longs_0[i_1--]
				}

				i_4 -= 3
				while (i_1 >= i_4) {
					longs_2[i_3--] = longs_0[i_1--]
				}

				return
			}
		}

		i_4 += i_1

		i_4 -= 3
		while (i_1 < i_4) {
			longs_2[i_3++] = longs_0[i_1++]
			longs_2[i_3++] = longs_0[i_1++]
			longs_2[i_3++] = longs_0[i_1++]
			longs_2[i_3++] = longs_0[i_1++]
		}

		i_4 += 3
		while (i_1 < i_4) {
			longs_2[i_3++] = longs_0[i_1++]
		}

	}

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	fun <T> arrayCopy(arr_0: Array<T>, i_1: Int, arr_2: Array<in T>, i_3: Int, i_4: Int) {
		var i_1 = i_1
		var i_3 = i_3
		var i_4 = i_4
		if (arr_2 === arr_0) {
			if (i_3 == i_1) {
				return
			}

			if (i_3 > i_1 && i_3 < i_4 + i_1) {
				--i_4
				i_1 += i_4
				i_3 += i_4
				i_4 = i_1 - i_4

				i_4 += 7
				while (i_1 >= i_4) {
					arr_2[i_3--] = arr_0[i_1--]
					arr_2[i_3--] = arr_0[i_1--]
					arr_2[i_3--] = arr_0[i_1--]
					arr_2[i_3--] = arr_0[i_1--]
					arr_2[i_3--] = arr_0[i_1--]
					arr_2[i_3--] = arr_0[i_1--]
					arr_2[i_3--] = arr_0[i_1--]
					arr_2[i_3--] = arr_0[i_1--]
				}

				i_4 -= 7
				while (i_1 >= i_4) {
					arr_2[i_3--] = arr_0[i_1--]
				}

				return
			}
		}

		i_4 += i_1

		i_4 -= 7
		while (i_1 < i_4) {
			arr_2[i_3++] = arr_0[i_1++]
			arr_2[i_3++] = arr_0[i_1++]
			arr_2[i_3++] = arr_0[i_1++]
			arr_2[i_3++] = arr_0[i_1++]
			arr_2[i_3++] = arr_0[i_1++]
			arr_2[i_3++] = arr_0[i_1++]
			arr_2[i_3++] = arr_0[i_1++]
			arr_2[i_3++] = arr_0[i_1++]
		}

		i_4 += 7
		while (i_1 < i_4) {
			arr_2[i_3++] = arr_0[i_1++]
		}

	}

	/**
	 * ChatGPT 4:
	 * public static void quickSort(long[] arr, Object[] objects, int startIndex, int endIndex) {
    if (startIndex >= endIndex) {
        return;
    }
    int midIndex = (startIndex + endIndex) / 2;
    long pivotValue = arr[midIndex];
    int leftIndex = startIndex;
    int rightIndex = endIndex;
    while (leftIndex <= rightIndex) {
        while (arr[leftIndex] < pivotValue) {
            leftIndex++;
        }
        while (arr[rightIndex] > pivotValue) {
            rightIndex--;
        }
        if (leftIndex <= rightIndex) {
            // Swap values and objects
            long tempValue = arr[leftIndex];
            arr[leftIndex] = arr[rightIndex];
            arr[rightIndex] = tempValue;
            Object tempObject = objects[leftIndex];
            objects[leftIndex] = objects[rightIndex];
            objects[rightIndex] = tempObject;
            leftIndex++;
            rightIndex--;
        }
    }
    quickSort(arr, objects, startIndex, rightIndex);
    quickSort(arr, objects, leftIndex, endIndex);
}
	 */
	@JvmStatic
	fun <T> quicksort(longs_0: LongArray, arr_1: Array<T>, srcPos: Int, size: Int) {
		var SrcPos = srcPos
		if (srcPos < size) {
			val i_5 = (size + srcPos) / 2
			SrcPos = srcPos
			val long_7 = longs_0[i_5]
			longs_0[i_5] = longs_0[size]
			longs_0[size] = long_7
			val object9 = arr_1[i_5]
			arr_1[i_5] = arr_1[size]
			arr_1[size] = object9
			val i_10 = if (long_7 == Long.MAX_VALUE) 0 else 1
			for (i_11 in srcPos until size) {
				if (longs_0[i_11] < (i_11 and i_10) + long_7) {
					val long_12 = longs_0[i_11]
					longs_0[i_11] = longs_0[SrcPos]
					longs_0[SrcPos] = long_12
					val object14 = arr_1[i_11]
					arr_1[i_11] = arr_1[SrcPos]
					arr_1[SrcPos++] = object14
				}
			}
			longs_0[size] = longs_0[SrcPos]
			longs_0[SrcPos] = long_7
			arr_1[size] = arr_1[SrcPos]
			arr_1[SrcPos] = object9
			quicksort(longs_0, arr_1, srcPos, SrcPos - 1)
			quicksort(longs_0, arr_1, SrcPos + 1, size)
		}
	}

	@JvmStatic
	fun <T> quicksort(longs_0: LongArray, arr_1: Array<T>) {
		quicksort(longs_0, arr_1, 0, longs_0.size - 1)
	}

	@JvmStatic
	fun setVariables(array: IntArray, position: Int, startPosition: Int, variableToSet: Int) {
		var position = position
		var startPosition = startPosition
		startPosition = startPosition + position - 7
		while (position < startPosition) {
			array[position++] = variableToSet
			array[position++] = variableToSet
			array[position++] = variableToSet
			array[position++] = variableToSet
			array[position++] = variableToSet
			array[position++] = variableToSet
			array[position++] = variableToSet
			array[position++] = variableToSet
		}
		startPosition += 7
		while (position < startPosition) {
			array[position++] = variableToSet
		}
	}

	@JvmStatic
	fun safeFullArraycopy(originalData: ByteArray?): ByteArray? {
		if (originalData == null) {
			return null
		}
		val data = ByteArray(originalData.size)
		System.arraycopy(originalData, 0, data, 0, originalData.size)
		return data
	}

	@JvmStatic
	fun <T> quicksort(ints_0: IntArray, arr_1: Array<T>, i_2: Int, i_3: Int) {
		var i_6 = i_2
		if (i_2 < i_3) {
			val i_5 = (i_3 + i_2) / 2
			i_6 = i_2
			val i_7 = ints_0[i_5]
			ints_0[i_5] = ints_0[i_3]
			ints_0[i_3] = i_7
			val object8 = arr_1[i_5]
			arr_1[i_5] = arr_1[i_3]
			arr_1[i_3] = object8
			val i_9 = if (i_7 == Int.MAX_VALUE) 0 else 1
			for (i_10 in i_2 until i_3) {
				if (ints_0[i_10] < (i_10 and i_9) + i_7) {
					val i_11 = ints_0[i_10]
					ints_0[i_10] = ints_0[i_6]
					ints_0[i_6] = i_11
					val object12 = arr_1[i_10]
					arr_1[i_10] = arr_1[i_6]
					arr_1[i_6++] = object12
				}
			}
			ints_0[i_3] = ints_0[i_6]
			ints_0[i_6] = i_7
			arr_1[i_3] = arr_1[i_6]
			arr_1[i_6] = object8
			quicksort(ints_0, arr_1, i_2, i_6 - 1)
			quicksort(ints_0, arr_1, i_6 + 1, i_3)
		}
	}

	@JvmStatic
	fun arraycopy(src: ShortArray?): ShortArray? {
		if (null == src)
			return null
		val dst = ShortArray(src.size)
		System.arraycopy(src, 0, dst, 0, src.size)
		return dst
	}

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	fun quicksort(arr_0: Array<String?>, shorts_1: ShortArray, i_2: Int, i_3: Int) {
		var i_6 = i_2
		if (i_2 < i_3) {
			val i_5 = (i_3 + i_2) / 2
			i_6 = i_2
			val string_7 = arr_0[i_5]
			arr_0[i_5] = arr_0[i_3]
			arr_0[i_3] = string_7
			val s_8 = shorts_1[i_5]
			shorts_1[i_5] = shorts_1[i_3]
			shorts_1[i_3] = s_8
			for (i_9 in i_2 until i_3) {
				if (string_7 == null || arr_0[i_9] != null && arr_0[i_9]!!.compareTo(string_7) < (i_9 and 0x1)) {
					val string_10 = arr_0[i_9]
					arr_0[i_9] = arr_0[i_6]
					arr_0[i_6] = string_10
					val s_11 = shorts_1[i_9]
					shorts_1[i_9] = shorts_1[i_6]
					shorts_1[i_6++] = s_11
				}
			}
			arr_0[i_3] = arr_0[i_6]
			arr_0[i_6] = string_7
			shorts_1[i_3] = shorts_1[i_6]
			shorts_1[i_6] = s_8
			quicksort(arr_0, shorts_1, i_2, i_6 - 1)
			quicksort(arr_0, shorts_1, i_6 + 1, i_3)
		}
	}

	@JvmStatic
	fun quicksort(longs: LongArray, ints: IntArray, startPosition: Int, endPosition: Int) {
		var curPosition = startPosition
		if (startPosition < endPosition) {
			val center = (endPosition + startPosition) / 2
			curPosition = startPosition
			val offset = longs[center]
			longs[center] = longs[endPosition]
			longs[endPosition] = offset
			val i_9 = ints[center]
			ints[center] = ints[endPosition]
			ints[endPosition] = i_9
			val mask = if (offset == Long.MAX_VALUE) 0 else 1

			for (index in startPosition until endPosition) {
				if (longs[index] < (index and mask) + offset) {
					val tmpLong = longs[index]
					longs[index] = longs[curPosition]
					longs[curPosition] = tmpLong
					val tmpInt = ints[index]
					ints[index] = ints[curPosition]
					ints[curPosition++] = tmpInt
				}
			}

			longs[endPosition] = longs[curPosition]
			longs[curPosition] = offset
			ints[endPosition] = ints[curPosition]
			ints[curPosition] = i_9
			quicksort(longs, ints, startPosition, curPosition - 1)
			quicksort(longs, ints, curPosition + 1, endPosition)
		}
	}

	@JvmStatic
	fun arrayCopy(floats_0: FloatArray, i_1: Int, floats_2: FloatArray, i_3: Int, i_4: Int) {
		var i_1 = i_1
		var i_3 = i_3
		var i_4 = i_4
		if (floats_2 === floats_0) {
			if (i_3 == i_1) {
				return
			}

			if (i_3 > i_1 && i_3 < i_4 + i_1) {
				--i_4
				i_1 += i_4
				i_3 += i_4
				i_4 = i_1 - i_4

				i_4 += 7
				while (i_1 >= i_4) {
					floats_2[i_3--] = floats_0[i_1--]
					floats_2[i_3--] = floats_0[i_1--]
					floats_2[i_3--] = floats_0[i_1--]
					floats_2[i_3--] = floats_0[i_1--]
					floats_2[i_3--] = floats_0[i_1--]
					floats_2[i_3--] = floats_0[i_1--]
					floats_2[i_3--] = floats_0[i_1--]
					floats_2[i_3--] = floats_0[i_1--]
				}

				i_4 -= 7
				while (i_1 >= i_4) {
					floats_2[i_3--] = floats_0[i_1--]
				}

				return
			}
		}

		i_4 += i_1

		i_4 -= 7
		while (i_1 < i_4) {
			floats_2[i_3++] = floats_0[i_1++]
			floats_2[i_3++] = floats_0[i_1++]
			floats_2[i_3++] = floats_0[i_1++]
			floats_2[i_3++] = floats_0[i_1++]
			floats_2[i_3++] = floats_0[i_1++]
			floats_2[i_3++] = floats_0[i_1++]
			floats_2[i_3++] = floats_0[i_1++]
			floats_2[i_3++] = floats_0[i_1++]
		}

		i_4 += 7
		while (i_1 < i_4) {
			floats_2[i_3++] = floats_0[i_1++]
		}

	}

	@JvmStatic
	fun setArrayValuesTo(array: IntArray, index: Int, length: Int, dstValue: Int) {
		var index = index
		var length = length
		--index
		--length
		val i_5 = length - 7
		while (index < i_5) {
			array[++index] = dstValue
			array[++index] = dstValue
			array[++index] = dstValue
			array[++index] = dstValue
			array[++index] = dstValue
			array[++index] = dstValue
			array[++index] = dstValue
			array[++index] = dstValue
			index++
		}
		while (index <= length) {
			array[++index] = dstValue
		}
	}

	@JvmStatic
	fun method2637(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
		method7170(i_2)
		var i_7 = 0
		var i_8 = i_2 - i_5
		if (i_8 < 0) {
			i_8 = 0
		}
		var i_9 = i_2
		var i_10 = -i_2
		var i_11 = i_8
		var i_12 = -i_8
		var i_13 = -1
		var i_14 = -1
		val ints_15 = TextureShapeRasterizer.anIntArrayArray7072!![i_1]
		val i_16 = i_0 - i_8
		val i_17 = i_0 + i_8
		setArrayValuesTo(ints_15, i_0 - i_2, i_16, i_4)
		setArrayValuesTo(ints_15, i_16, i_17, i_3)
		setArrayValuesTo(ints_15, i_17, i_0 + i_2, i_4)
		while (i_9 > i_7) {
			i_13 += 2
			i_14 += 2
			i_10 += i_13
			i_12 += i_14
			if (i_12 >= 0 && i_11 >= 1) {
				StaticMethods.anIntArray36!![i_11] = i_7
				--i_11
				i_12 -= i_11 shl 1
			}
			++i_7
			var ints_18: IntArray
			var ints_19: IntArray
			var i_20: Int
			var i_21: Int
			var i_22: Int
			var i_23: Int
			var i_24: Int
			if (i_10 >= 0) {
				--i_9
				i_10 -= i_9 shl 1
				if (i_9 >= i_8) {
					ints_18 = TextureShapeRasterizer.anIntArrayArray7072!![i_9 + i_1]
					ints_19 = TextureShapeRasterizer.anIntArrayArray7072!![i_1 - i_9]
					i_20 = i_0 + i_7
					i_21 = i_0 - i_7
					setArrayValuesTo(ints_18, i_21, i_20, i_4)
					setArrayValuesTo(ints_19, i_21, i_20, i_4)
				} else {
					ints_18 = TextureShapeRasterizer.anIntArrayArray7072!![i_9 + i_1]
					ints_19 = TextureShapeRasterizer.anIntArrayArray7072!![i_1 - i_9]
					i_20 = StaticMethods.anIntArray36!![i_9]
					i_21 = i_0 + i_7
					i_22 = i_0 - i_7
					i_23 = i_0 + i_20
					i_24 = i_0 - i_20
					setArrayValuesTo(ints_18, i_22, i_24, i_4)
					setArrayValuesTo(ints_18, i_24, i_23, i_3)
					setArrayValuesTo(ints_18, i_23, i_21, i_4)
					setArrayValuesTo(ints_19, i_22, i_24, i_4)
					setArrayValuesTo(ints_19, i_24, i_23, i_3)
					setArrayValuesTo(ints_19, i_23, i_21, i_4)
				}
			}
			ints_18 = TextureShapeRasterizer.anIntArrayArray7072!![i_7 + i_1]
			ints_19 = TextureShapeRasterizer.anIntArrayArray7072!![i_1 - i_7]
			i_20 = i_0 + i_9
			i_21 = i_0 - i_9
			if (i_7 < i_8) {
				i_22 = if (i_11 < i_7) StaticMethods.anIntArray36!![i_7] else i_11
				i_23 = i_0 + i_22
				i_24 = i_0 - i_22
				setArrayValuesTo(ints_18, i_21, i_24, i_4)
				setArrayValuesTo(ints_18, i_24, i_23, i_3)
				setArrayValuesTo(ints_18, i_23, i_20, i_4)
				setArrayValuesTo(ints_19, i_21, i_24, i_4)
				setArrayValuesTo(ints_19, i_24, i_23, i_3)
				setArrayValuesTo(ints_19, i_23, i_20, i_4)
			} else {
				setArrayValuesTo(ints_18, i_21, i_20, i_4)
				setArrayValuesTo(ints_19, i_21, i_20, i_4)
			}
		}
	}

	@JvmStatic
	fun method744(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
		val i_7 = method4890(i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_8 = method4890(i_3, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_9 = method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_10 = method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_11 = method4890(i_5 + i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_12 = method4890(i_3 - i_5, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		var i_13: Int
		i_13 = i_7
		while (i_13 < i_11) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_13], i_9, i_10, i_4)
			i_13++
		}
		i_13 = i_8
		while (i_13 > i_12) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_13], i_9, i_10, i_4)
			--i_13
		}
		i_13 = method4890(i_5 + i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_14 = method4890(i_1 - i_5, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		for (i_15 in i_11..i_12) {
			val ints_16 = TextureShapeRasterizer.anIntArrayArray7072!![i_15]
			setArrayValuesTo(ints_16, i_9, i_13, i_4)
			setArrayValuesTo(ints_16, i_14, i_10, i_4)
		}
	}

	@JvmStatic
	fun method1174(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
		method7170(i_2)
		var i_7 = 0
		var i_8 = i_2 - i_5
		if (i_8 < 0) {
			i_8 = 0
		}
		var i_9 = i_2
		var i_10 = -i_2
		var i_11 = i_8
		var i_12 = -i_8
		var i_13 = -1
		var i_14 = -1
		var i_16: Int
		var i_17: Int
		var i_18: Int
		var i_19: Int
		if (i_1 >= TextureOpRasterizer.anInt7070 && i_1 <= TextureOpRasterizer.anInt7068) {
			val ints_15 = TextureShapeRasterizer.anIntArrayArray7072!![i_1]
			i_16 = method4890(i_0 - i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			i_17 = method4890(i_0 + i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			i_18 = method4890(i_0 - i_8, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			i_19 = method4890(i_0 + i_8, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			setArrayValuesTo(ints_15, i_16, i_18, i_4)
			setArrayValuesTo(ints_15, i_18, i_19, i_3)
			setArrayValuesTo(ints_15, i_19, i_17, i_4)
		}
		while (i_9 > i_7) {
			i_13 += 2
			i_14 += 2
			i_10 += i_13
			i_12 += i_14
			if (i_12 >= 0 && i_11 >= 1) {
				--i_11
				i_12 -= i_11 shl 1
				StaticMethods.anIntArray36!![i_11] = i_7
			}
			++i_7
			var i_20: Int
			var i_21: Int
			var ints_22: IntArray
			var i_23: Int
			if (i_10 >= 0) {
				--i_9
				i_10 -= i_9 shl 1
				i_23 = i_1 - i_9
				i_16 = i_9 + i_1
				if (i_16 >= TextureOpRasterizer.anInt7070 && i_23 <= TextureOpRasterizer.anInt7068) {
					if (i_9 >= i_8) {
						i_17 = method4890(i_0 + i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						i_18 = method4890(i_0 - i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						if (i_16 <= TextureOpRasterizer.anInt7068) {
							setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_16], i_18, i_17, i_4)
						}
						if (i_23 >= TextureOpRasterizer.anInt7070) {
							setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_23], i_18, i_17, i_4)
						}
					} else {
						i_17 = StaticMethods.anIntArray36!![i_9]
						i_18 = method4890(i_0 + i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						i_19 = method4890(i_0 - i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						i_20 = method4890(i_0 + i_17, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						i_21 = method4890(i_0 - i_17, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						if (i_16 <= TextureOpRasterizer.anInt7068) {
							ints_22 = TextureShapeRasterizer.anIntArrayArray7072!![i_16]
							setArrayValuesTo(ints_22, i_19, i_21, i_4)
							setArrayValuesTo(ints_22, i_21, i_20, i_3)
							setArrayValuesTo(ints_22, i_20, i_18, i_4)
						}
						if (i_23 >= TextureOpRasterizer.anInt7070) {
							ints_22 = TextureShapeRasterizer.anIntArrayArray7072!![i_23]
							setArrayValuesTo(ints_22, i_19, i_21, i_4)
							setArrayValuesTo(ints_22, i_21, i_20, i_3)
							setArrayValuesTo(ints_22, i_20, i_18, i_4)
						}
					}
				}
			}
			i_23 = i_1 - i_7
			i_16 = i_7 + i_1
			if (i_16 >= TextureOpRasterizer.anInt7070 && i_23 <= TextureOpRasterizer.anInt7068) {
				i_17 = i_0 + i_9
				i_18 = i_0 - i_9
				if (i_17 >= TextureOpRasterizer.anInt7071 && i_18 <= TextureOpRasterizer.anInt7069) {
					i_17 = method4890(i_17, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
					i_18 = method4890(i_18, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
					if (i_7 < i_8) {
						i_19 = if (i_11 < i_7) StaticMethods.anIntArray36!![i_7] else i_11
						i_20 = method4890(i_0 + i_19, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						i_21 = method4890(i_0 - i_19, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
						if (i_16 <= TextureOpRasterizer.anInt7068) {
							ints_22 = TextureShapeRasterizer.anIntArrayArray7072!![i_16]
							setArrayValuesTo(ints_22, i_18, i_21, i_4)
							setArrayValuesTo(ints_22, i_21, i_20, i_3)
							setArrayValuesTo(ints_22, i_20, i_17, i_4)
						}
						if (i_23 >= TextureOpRasterizer.anInt7070) {
							ints_22 = TextureShapeRasterizer.anIntArrayArray7072!![i_23]
							setArrayValuesTo(ints_22, i_18, i_21, i_4)
							setArrayValuesTo(ints_22, i_21, i_20, i_3)
							setArrayValuesTo(ints_22, i_20, i_17, i_4)
						}
					} else {
						if (i_16 <= TextureOpRasterizer.anInt7068) {
							setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_16], i_18, i_17, i_4)
						}
						if (i_23 >= TextureOpRasterizer.anInt7070) {
							setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_23], i_18, i_17, i_4)
						}
					}
				}
			}
		}
	}

	@JvmStatic
	fun method15405(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
		var i_6 = 0
		var i_7 = i_3
		val i_8 = i_2 * i_2
		val i_9 = i_3 * i_3
		val i_10 = i_9 shl 1
		val i_11 = i_8 shl 1
		val i_12 = i_3 shl 1
		var i_13 = i_10 + (1 - i_12) * i_8
		var i_14 = i_9 - i_11 * (i_12 - 1)
		val i_15 = i_8 shl 2
		val i_16 = i_9 shl 2
		var i_17 = ((i_6 shl 1) + 3) * i_10
		var i_18 = ((i_3 shl 1) - 3) * i_11
		var i_19 = i_16 * (i_6 + 1)
		var i_20 = i_15 * (i_3 - 1)
		var i_21: Int
		var i_22: Int
		if (i_1 >= TextureOpRasterizer.anInt7070 && i_1 <= TextureOpRasterizer.anInt7068) {
			i_21 = method4890(i_0 + i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			i_22 = method4890(i_0 - i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_1], i_22, i_21, i_4)
		}
		while (i_7 > 0) {
			if (i_13 < 0) {
				while (i_13 < 0) {
					i_13 += i_17
					i_14 += i_19
					i_17 += i_16
					i_19 += i_16
					++i_6
				}
			}
			if (i_14 < 0) {
				i_13 += i_17
				i_14 += i_19
				i_17 += i_16
				i_19 += i_16
				++i_6
			}
			i_13 += -i_20
			i_14 += -i_18
			i_18 -= i_15
			i_20 -= i_15
			--i_7
			i_21 = i_1 - i_7
			i_22 = i_7 + i_1
			if (i_22 >= TextureOpRasterizer.anInt7070 && i_21 <= TextureOpRasterizer.anInt7068) {
				val i_23 = method4890(i_0 + i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
				val i_24 = method4890(i_0 - i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
				if (i_21 >= TextureOpRasterizer.anInt7070) {
					setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_21], i_24, i_23, i_4)
				}
				if (i_22 <= TextureOpRasterizer.anInt7068) {
					setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_22], i_24, i_23, i_4)
				}
			}
		}
	}

	@JvmStatic
	fun method4866(i_0: Int, i_1: Int, i_2: Int, i_3: Int) {
		var i_5 = 0
		var i_6 = i_2
		var i_7 = -i_2
		var i_8 = -1
		val i_9 = method4890(i_0 + i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_10 = method4890(i_0 - i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_1], i_10, i_9, i_3)
		while (i_6 > i_5) {
			i_8 += 2
			i_7 += i_8
			var i_11: Int
			var i_12: Int
			var i_13: Int
			var i_14: Int
			if (i_7 > 0) {
				--i_6
				i_7 -= i_6 shl 1
				i_11 = i_1 - i_6
				i_12 = i_6 + i_1
				if (i_12 >= TextureOpRasterizer.anInt7070 && i_11 <= TextureOpRasterizer.anInt7068) {
					i_13 = method4890(i_0 + i_5, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
					i_14 = method4890(i_0 - i_5, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
					if (i_12 <= TextureOpRasterizer.anInt7068) {
						setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_12], i_14, i_13, i_3)
					}
					if (i_11 >= TextureOpRasterizer.anInt7070) {
						setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_11], i_14, i_13, i_3)
					}
				}
			}
			++i_5
			i_11 = i_1 - i_5
			i_12 = i_5 + i_1
			if (i_12 >= TextureOpRasterizer.anInt7070 && i_11 <= TextureOpRasterizer.anInt7068) {
				i_13 = method4890(i_0 + i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
				i_14 = method4890(i_0 - i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
				if (i_12 <= TextureOpRasterizer.anInt7068) {
					setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_12], i_14, i_13, i_3)
				}
				if (i_11 >= TextureOpRasterizer.anInt7070) {
					setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_11], i_14, i_13, i_3)
				}
			}
		}
	}

	@JvmStatic
	fun method4034(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
		val i_8 = method4890(i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_9 = method4890(i_3, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_10 = method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_11 = method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_12 = method4890(i_2 + i_6, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_13 = method4890(i_3 - i_6, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		var i_14: Int
		i_14 = i_8
		while (i_14 < i_12) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_14], i_10, i_11, i_5)
			i_14++
		}
		i_14 = i_9
		while (i_14 > i_13) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_14], i_10, i_11, i_5)
			--i_14
		}
		i_14 = method4890(i_0 + i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_15 = method4890(i_1 - i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		for (i_16 in i_12..i_13) {
			val ints_17 = TextureShapeRasterizer.anIntArrayArray7072!![i_16]
			setArrayValuesTo(ints_17, i_10, i_14, i_5)
			setArrayValuesTo(ints_17, i_14, i_15, i_4)
			setArrayValuesTo(ints_17, i_15, i_11, i_5)
		}
	}

	@JvmStatic
	fun method7728(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
		val i_6 = method4890(i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_7 = method4890(i_3, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
		val i_8 = method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		val i_9 = method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
		for (i_10 in i_6..i_7) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_10], i_8, i_9, i_4)
		}
	}

	@JvmStatic
	fun method4890(i_0: Int, i_1: Int, i_2: Int): Int {
		return if (i_0 < i_1) i_1 else (Math.min(i_0, i_2))
	}

	@JvmStatic
	fun method568(i_0: Int, i_1: Int, i_2: Int, i_3: Int) {
		if (i_0 > i_1) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_2], i_1, i_0, i_3)
		} else {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_2], i_0, i_1, i_3)
		}
	}

	@JvmStatic
	fun method11250(i_0: Int, i_1: Int, i_2: Int, i_3: Int) {
		var i_0 = i_0
		var i_1 = i_1
		if (i_2 >= TextureOpRasterizer.anInt7070 && i_2 <= TextureOpRasterizer.anInt7068) {
			i_0 = method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			i_1 = method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069)
			method568(i_0, i_1, i_2, i_3)
		}
	}

	@JvmStatic
	fun arrayCopy(bytes_0: ByteArray, i_1: Int): ByteArray {
		val bytes_2 = ByteArray(i_1)
		System.arraycopy(bytes_0, 0, bytes_2, 0, i_1)
		return bytes_2
	}

	@JvmStatic
	fun arrayCopy(floats_0: FloatArray, i_1: Int): FloatArray {
		val floats_2 = FloatArray(i_1)
		System.arraycopy(floats_0, 0, floats_2, 0, i_1)
		return floats_2
	}

	@JvmStatic
	fun arrayCopy(shorts_0: ShortArray, i_1: Int): ShortArray {
		val shorts_2 = ShortArray(i_1)
		System.arraycopy(shorts_0, 0, shorts_2, 0, i_1)
		return shorts_2
	}

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	fun quicksort(strings: Array<String?>, ints: IntArray, fromIndex: Int, toIndex: Int) {
		var i_6 = fromIndex
		if (fromIndex < toIndex) {
			val middle = (toIndex + fromIndex) / 2
			i_6 = fromIndex
			val string_7 = strings[middle]
			strings[middle] = strings[toIndex]
			strings[toIndex] = string_7
			val i_8 = ints[middle]
			ints[middle] = ints[toIndex]
			ints[toIndex] = i_8
			for (i_9 in fromIndex until toIndex) {
				if (string_7 == null || strings[i_9] != null && strings[i_9]!!.compareTo(string_7) < (i_9 and 0x1)) {
					val string_10 = strings[i_9]
					strings[i_9] = strings[i_6]
					strings[i_6] = string_10
					val i_11 = ints[i_9]
					ints[i_9] = ints[i_6]
					ints[i_6++] = i_11
				}
			}
			strings[toIndex] = strings[i_6]
			strings[i_6] = string_7
			ints[toIndex] = ints[i_6]
			ints[i_6] = i_8
			quicksort(strings, ints, fromIndex, i_6 - 1)
			quicksort(strings, ints, i_6 + 1, toIndex)
		}
	}

	@JvmStatic
	fun quicksort(ints_0: IntArray, ints_1: IntArray, i_2: Int, i_3: Int) {
		var i_6 = i_2
		if (i_2 < i_3) {
			val i_5 = (i_3 + i_2) / 2
			i_6 = i_2
			val i_7 = ints_0[i_5]
			ints_0[i_5] = ints_0[i_3]
			ints_0[i_3] = i_7
			val i_8 = ints_1[i_5]
			ints_1[i_5] = ints_1[i_3]
			ints_1[i_3] = i_8
			val i_9 = if (i_7 == Int.MAX_VALUE) 0 else 1
			for (i_10 in i_2 until i_3) {
				if (ints_0[i_10] < (i_10 and i_9) + i_7) {
					val i_11 = ints_0[i_10]
					ints_0[i_10] = ints_0[i_6]
					ints_0[i_6] = i_11
					val i_12 = ints_1[i_10]
					ints_1[i_10] = ints_1[i_6]
					ints_1[i_6++] = i_12
				}
			}
			ints_0[i_3] = ints_0[i_6]
			ints_0[i_6] = i_7
			ints_1[i_3] = ints_1[i_6]
			ints_1[i_6] = i_8
			quicksort(ints_0, ints_1, i_2, i_6 - 1)
			quicksort(ints_0, ints_1, i_6 + 1, i_3)
		}
	}

	@JvmStatic
	fun generateRandomlySeededBytearray(seed: Int): ByteArray {
		var bytearrayNode = BYTEARRAY_NODES.get(seed.toLong()) as CacheableNodeByteArray?
		if (bytearrayNode == null) {
			val block = ByteArray(512)
			val random = Random(seed.toLong())
			for (index in 0..254) {
				block[index] = index.toByte()
			}
			for (index in 0..254) {
				val i_6 = 255 - index
				val i_7 = StaticMethods.method7931(random, i_6)
				val temp = block[i_7]
				block[i_7] = block[i_6]
				block[i_6] = temp
				block[511 - index] = temp
			}
			bytearrayNode = CacheableNodeByteArray(block)
			BYTEARRAY_NODES.put(bytearrayNode, seed.toLong())
		}
		return bytearrayNode.data
	}

	@JvmField
	val BYTEARRAY_NODES = CacheableNodeQueue(16)

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	fun quicksortt(strings: Array<String?>, ints: IntArray) {
		quicksort(strings, ints, 0, strings.size - 1)
	}

	@JvmStatic
	fun method3751(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
		var i_6 = 0
		var i_7 = i_3
		val i_8 = i_2 * i_2
		val i_9 = i_3 * i_3
		val i_10 = i_9 shl 1
		val i_11 = i_8 shl 1
		val i_12 = i_3 shl 1
		var i_13 = i_10 + (1 - i_12) * i_8
		var i_14 = i_9 - i_11 * (i_12 - 1)
		val i_15 = i_8 shl 2
		val i_16 = i_9 shl 2
		var i_17 = ((i_6 shl 1) + 3) * i_10
		var i_18 = ((i_3 shl 1) - 3) * i_11
		var i_19 = i_16 * (i_6 + 1)
		var i_20 = i_15 * (i_3 - 1)
		setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_1], i_0 - i_2, i_0 + i_2, i_4)
		while (i_7 > 0) {
			if (i_13 < 0) {
				while (i_13 < 0) {
					i_13 += i_17
					i_14 += i_19
					i_17 += i_16
					i_19 += i_16
					++i_6
				}
			}
			if (i_14 < 0) {
				i_13 += i_17
				i_14 += i_19
				i_17 += i_16
				i_19 += i_16
				++i_6
			}
			i_13 += -i_20
			i_14 += -i_18
			i_18 -= i_15
			i_20 -= i_15
			--i_7
			val i_21 = i_1 - i_7
			val i_22 = i_7 + i_1
			val i_23 = i_0 + i_6
			val i_24 = i_0 - i_6
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_21], i_24, i_23, i_4)
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_22], i_24, i_23, i_4)
		}
	}

	@JvmStatic
	fun <T> quicksort(dimensions: IntArray, objects: Array<T>) {
		quicksort(dimensions, objects, 0, dimensions.size - 1)
	}

	@JvmStatic
	fun method7170(i_0: Int) {
		if (StaticMethods.anIntArray36 == null || StaticMethods.anIntArray36!!.size < i_0) {
			StaticMethods.anIntArray36 = IntArray(i_0)
		}
	}

	@JvmStatic
	fun method14584(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
		if (i_3 == i_2) {
			StaticMethods.method6292(i_0, i_1, i_2, i_4)
		} else if (i_0 - i_2 >= TextureOpRasterizer.anInt7071 && i_0 + i_2 <= TextureOpRasterizer.anInt7069 && i_1 - i_3 >= TextureOpRasterizer.anInt7070 && i_3 + i_1 <= TextureOpRasterizer.anInt7068) {
			method3751(i_0, i_1, i_2, i_3, i_4)
		} else {
			method15405(i_0, i_1, i_2, i_3, i_4)
		}

	}

	@JvmStatic
	fun method3230(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
		val i_8 = i_2 + i_6
		val i_9 = i_3 - i_6
		var i_10: Int
		i_10 = i_2
		while (i_10 < i_8) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_10], i_0, i_1, i_5)
			i_10++
		}
		i_10 = i_3
		while (i_10 > i_9) {
			setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_10], i_0, i_1, i_5)
			--i_10
		}
		i_10 = i_0 + i_6
		val i_11 = i_1 - i_6
		for (i_12 in i_8..i_9) {
			val ints_13 = TextureShapeRasterizer.anIntArrayArray7072!![i_12]
			setArrayValuesTo(ints_13, i_0, i_10, i_5)
			setArrayValuesTo(ints_13, i_10, i_11, i_4)
			setArrayValuesTo(ints_13, i_11, i_1, i_5)
		}
	}
}
