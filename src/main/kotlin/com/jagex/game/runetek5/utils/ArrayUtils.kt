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
	fun arrayCopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, length: Int) {
		var srcPos = srcPos
		var destPos = destPos
		var length = length
		if (dest === src) {
			if (destPos == srcPos) {
				return
			}

			if (destPos > srcPos && destPos < length + srcPos) {
				--length
				srcPos += length
				destPos += length
				length = srcPos - length

				length += 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
				}

				length -= 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
				}

				return
			}
		}

		length += srcPos

		length -= 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
		}

		length += 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
		}

	}

	@JvmStatic
	fun arrayCopy(src: ByteArray, srcPos: Int, dest: ByteArray, destPos: Int, length: Int) {
		var srcPos = srcPos
		var destPos = destPos
		var length = length
		if (dest === src) {
			if (destPos == srcPos) {
				return
			}

			if (destPos > srcPos && destPos < length + srcPos) {
				--length
				srcPos += length
				destPos += length
				length = srcPos - length

				length += 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
				}

				length -= 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
				}

				return
			}
		}

		length += srcPos

		length -= 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
		}

		length += 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
		}

	}

	@JvmStatic
	fun arrayCopy(src: LongArray, srcPos: Int, dest: LongArray, destPos: Int, length: Int) {
		var srcPos = srcPos
		var destPos = destPos
		var length = length
		if (dest === src) {
			if (destPos == srcPos) {
				return
			}

			if (destPos > srcPos && destPos < length + srcPos) {
				--length
				srcPos += length
				destPos += length
				length = srcPos - length

				length += 3
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
				}

				length -= 3
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
				}

				return
			}
		}

		length += srcPos

		length -= 3
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
		}

		length += 3
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
		}

	}

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	fun <T> arrayCopy(src: Array<T>, srcPos: Int, dest: Array<in T>, destPos: Int, length: Int) {
		var srcPos = srcPos
		var destPos = destPos
		var length = length
		if (dest === src) {
			if (destPos == srcPos) {
				return
			}

			if (destPos > srcPos && destPos < length + srcPos) {
				--length
				srcPos += length
				destPos += length
				length = srcPos - length

				length += 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
				}

				length -= 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
				}

				return
			}
		}

		length += srcPos

		length -= 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
		}

		length += 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
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
	fun <T> quicksort(keys: LongArray, values: Array<T>, startIndex: Int, endIndex: Int) {
		var partitionIndex = startIndex
		if (startIndex < endIndex) {
			val midIndex = (endIndex + startIndex) / 2
			partitionIndex = startIndex
			val pivotValue = keys[midIndex]
			keys[midIndex] = keys[endIndex]
			keys[endIndex] = pivotValue
			val pivotObject = values[midIndex]
			values[midIndex] = values[endIndex]
			values[endIndex] = pivotObject
			val tieBreakMask = if (pivotValue == Long.MAX_VALUE) 0 else 1
			for (i in startIndex until endIndex) {
				if (keys[i] < (i and tieBreakMask) + pivotValue) {
					val tempKey = keys[i]
					keys[i] = keys[partitionIndex]
					keys[partitionIndex] = tempKey
					val tempValue = values[i]
					values[i] = values[partitionIndex]
					values[partitionIndex++] = tempValue
				}
			}
			keys[endIndex] = keys[partitionIndex]
			keys[partitionIndex] = pivotValue
			values[endIndex] = values[partitionIndex]
			values[partitionIndex] = pivotObject
			quicksort(keys, values, startIndex, partitionIndex - 1)
			quicksort(keys, values, partitionIndex + 1, endIndex)
		}
	}

	@JvmStatic
	fun <T> quicksort(keys: LongArray, values: Array<T>) {
		quicksort(keys, values, 0, keys.size - 1)
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
	fun <T> quicksort(keys: IntArray, values: Array<T>, startIndex: Int, endIndex: Int) {
		var partitionIndex = startIndex
		if (startIndex < endIndex) {
			val midIndex = (endIndex + startIndex) / 2
			partitionIndex = startIndex
			val pivotValue = keys[midIndex]
			keys[midIndex] = keys[endIndex]
			keys[endIndex] = pivotValue
			val pivotObject = values[midIndex]
			values[midIndex] = values[endIndex]
			values[endIndex] = pivotObject
			val tieBreakMask = if (pivotValue == Int.MAX_VALUE) 0 else 1
			for (i in startIndex until endIndex) {
				if (keys[i] < (i and tieBreakMask) + pivotValue) {
					val tempKey = keys[i]
					keys[i] = keys[partitionIndex]
					keys[partitionIndex] = tempKey
					val tempValue = values[i]
					values[i] = values[partitionIndex]
					values[partitionIndex++] = tempValue
				}
			}
			keys[endIndex] = keys[partitionIndex]
			keys[partitionIndex] = pivotValue
			values[endIndex] = values[partitionIndex]
			values[partitionIndex] = pivotObject
			quicksort(keys, values, startIndex, partitionIndex - 1)
			quicksort(keys, values, partitionIndex + 1, endIndex)
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
	fun quicksort(names: Array<String?>, ids: ShortArray, startIndex: Int, endIndex: Int) {
		var partitionIndex = startIndex
		if (startIndex < endIndex) {
			val midIndex = (endIndex + startIndex) / 2
			partitionIndex = startIndex
			val pivotName = names[midIndex]
			names[midIndex] = names[endIndex]
			names[endIndex] = pivotName
			val pivotId = ids[midIndex]
			ids[midIndex] = ids[endIndex]
			ids[endIndex] = pivotId
			for (i in startIndex until endIndex) {
				if (pivotName == null || names[i] != null && names[i]!!.compareTo(pivotName) < (i and 0x1)) {
					val tempName = names[i]
					names[i] = names[partitionIndex]
					names[partitionIndex] = tempName
					val tempId = ids[i]
					ids[i] = ids[partitionIndex]
					ids[partitionIndex++] = tempId
				}
			}
			names[endIndex] = names[partitionIndex]
			names[partitionIndex] = pivotName
			ids[endIndex] = ids[partitionIndex]
			ids[partitionIndex] = pivotId
			quicksort(names, ids, startIndex, partitionIndex - 1)
			quicksort(names, ids, partitionIndex + 1, endIndex)
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
			val pivotInt = ints[center]
			ints[center] = ints[endPosition]
			ints[endPosition] = pivotInt
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
			ints[curPosition] = pivotInt
			quicksort(longs, ints, startPosition, curPosition - 1)
			quicksort(longs, ints, curPosition + 1, endPosition)
		}
	}

	@JvmStatic
	fun arrayCopy(src: FloatArray, srcPos: Int, dest: FloatArray, destPos: Int, length: Int) {
		var srcPos = srcPos
		var destPos = destPos
		var length = length
		if (dest === src) {
			if (destPos == srcPos) {
				return
			}

			if (destPos > srcPos && destPos < length + srcPos) {
				--length
				srcPos += length
				destPos += length
				length = srcPos - length

				length += 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
					dest[destPos--] = src[srcPos--]
				}

				length -= 7
				while (srcPos >= length) {
					dest[destPos--] = src[srcPos--]
				}

				return
			}
		}

		length += srcPos

		length -= 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
			dest[destPos++] = src[srcPos++]
		}

		length += 7
		while (srcPos < length) {
			dest[destPos++] = src[srcPos++]
		}

	}

	@JvmStatic
	fun setArrayValuesTo(array: IntArray, index: Int, length: Int, dstValue: Int) {
		var index = index
		var length = length
		--index
		--length
		val unrolledEnd = length - 7
		while (index < unrolledEnd) {
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
	fun drawUnclippedFilledAndStrokedCircle(centerX: Int, centerY: Int, outerRadius: Int, fillColor: Int, strokeColor: Int, strokeWidth: Int) {
		ensureCircleBufferCapacity(outerRadius)
		var x = 0
		var innerRadius = outerRadius - strokeWidth
		if (innerRadius < 0) {
			innerRadius = 0
		}
		var outerY = outerRadius
		var outerError = -outerRadius
		var innerY = innerRadius
		var innerError = -innerRadius
		var outerDelta = -1
		var innerDelta = -1
		val scanline = TextureShapeRasterizer.pixelBuffer!![centerY]
		val innerLeft = centerX - innerRadius
		val innerRight = centerX + innerRadius
		setArrayValuesTo(scanline, centerX - outerRadius, innerLeft, strokeColor)
		setArrayValuesTo(scanline, innerLeft, innerRight, fillColor)
		setArrayValuesTo(scanline, innerRight, centerX + outerRadius, strokeColor)
		while (outerY > x) {
			outerDelta += 2
			innerDelta += 2
			outerError += outerDelta
			innerError += innerDelta
			if (innerError >= 0 && innerY >= 1) {
				StaticMethods.circleInnerRadiusBuffer!![innerY] = x
				--innerY
				innerError -= innerY shl 1
			}
			++x
			var upperRow: IntArray
			var lowerRow: IntArray
			var right: Int
			var left: Int
			var innerRight2: Int
			var innerLeft2: Int
			var innerExtent: Int
			if (outerError >= 0) {
				--outerY
				outerError -= outerY shl 1
				if (outerY >= innerRadius) {
					upperRow = TextureShapeRasterizer.pixelBuffer!![outerY + centerY]
					lowerRow = TextureShapeRasterizer.pixelBuffer!![centerY - outerY]
					right = centerX + x
					left = centerX - x
					setArrayValuesTo(upperRow, left, right, strokeColor)
					setArrayValuesTo(lowerRow, left, right, strokeColor)
				} else {
					upperRow = TextureShapeRasterizer.pixelBuffer!![outerY + centerY]
					lowerRow = TextureShapeRasterizer.pixelBuffer!![centerY - outerY]
					innerExtent = StaticMethods.circleInnerRadiusBuffer!![outerY]
					right = centerX + x
					left = centerX - x
					innerRight2 = centerX + innerExtent
					innerLeft2 = centerX - innerExtent
					setArrayValuesTo(upperRow, left, innerLeft2, strokeColor)
					setArrayValuesTo(upperRow, innerLeft2, innerRight2, fillColor)
					setArrayValuesTo(upperRow, innerRight2, right, strokeColor)
					setArrayValuesTo(lowerRow, left, innerLeft2, strokeColor)
					setArrayValuesTo(lowerRow, innerLeft2, innerRight2, fillColor)
					setArrayValuesTo(lowerRow, innerRight2, right, strokeColor)
				}
			}
			upperRow = TextureShapeRasterizer.pixelBuffer!![x + centerY]
			lowerRow = TextureShapeRasterizer.pixelBuffer!![centerY - x]
			right = centerX + outerY
			left = centerX - outerY
			if (x < innerRadius) {
				innerExtent = if (innerY < x) StaticMethods.circleInnerRadiusBuffer!![x] else innerY
				innerRight2 = centerX + innerExtent
				innerLeft2 = centerX - innerExtent
				setArrayValuesTo(upperRow, left, innerLeft2, strokeColor)
				setArrayValuesTo(upperRow, innerLeft2, innerRight2, fillColor)
				setArrayValuesTo(upperRow, innerRight2, right, strokeColor)
				setArrayValuesTo(lowerRow, left, innerLeft2, strokeColor)
				setArrayValuesTo(lowerRow, innerLeft2, innerRight2, fillColor)
				setArrayValuesTo(lowerRow, innerRight2, right, strokeColor)
			} else {
				setArrayValuesTo(upperRow, left, right, strokeColor)
				setArrayValuesTo(lowerRow, left, right, strokeColor)
			}
		}
	}

	@JvmStatic
	fun drawClippedStrokedRectangle(left: Int, right: Int, top: Int, bottom: Int, color: Int, borderWidth: Int) {
		val clippedTop = clamp(top, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val clippedBottom = clamp(bottom, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val clippedLeft = clamp(left, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val clippedRight = clamp(right, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val topBorderEnd = clamp(borderWidth + top, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val bottomBorderStart = clamp(bottom - borderWidth, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		var y: Int
		y = clippedTop
		while (y < topBorderEnd) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], clippedLeft, clippedRight, color)
			y++
		}
		y = clippedBottom
		while (y > bottomBorderStart) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], clippedLeft, clippedRight, color)
			--y
		}
		y = clamp(borderWidth + left, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val rightBorderStart = clamp(right - borderWidth, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		for (row in topBorderEnd..bottomBorderStart) {
			val scanline = TextureShapeRasterizer.pixelBuffer!![row]
			setArrayValuesTo(scanline, clippedLeft, y, color)
			setArrayValuesTo(scanline, rightBorderStart, clippedRight, color)
		}
	}

	@JvmStatic
	fun drawClippedFilledAndStrokedCircle(centerX: Int, centerY: Int, outerRadius: Int, fillColor: Int, strokeColor: Int, strokeWidth: Int) {
		ensureCircleBufferCapacity(outerRadius)
		var x = 0
		var innerRadius = outerRadius - strokeWidth
		if (innerRadius < 0) {
			innerRadius = 0
		}
		var outerY = outerRadius
		var outerError = -outerRadius
		var innerY = innerRadius
		var innerError = -innerRadius
		var outerDelta = -1
		var innerDelta = -1
		var rowAbove: Int
		var rowBelow: Int
		var right: Int
		var innerRight: Int
		if (centerY >= TextureOpRasterizer.clipMinY && centerY <= TextureOpRasterizer.clipMaxY) {
			val scanline = TextureShapeRasterizer.pixelBuffer!![centerY]
			rowAbove = clamp(centerX - outerRadius, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			rowBelow = clamp(centerX + outerRadius, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			right = clamp(centerX - innerRadius, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			innerRight = clamp(centerX + innerRadius, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			setArrayValuesTo(scanline, rowAbove, right, strokeColor)
			setArrayValuesTo(scanline, right, innerRight, fillColor)
			setArrayValuesTo(scanline, innerRight, rowBelow, strokeColor)
		}
		while (outerY > x) {
			outerDelta += 2
			innerDelta += 2
			outerError += outerDelta
			innerError += innerDelta
			if (innerError >= 0 && innerY >= 1) {
				--innerY
				innerError -= innerY shl 1
				StaticMethods.circleInnerRadiusBuffer!![innerY] = x
			}
			++x
			var innerExtent: Int
			var left: Int
			var upperRow: IntArray
			var mirrorY: Int
			if (outerError >= 0) {
				--outerY
				outerError -= outerY shl 1
				mirrorY = centerY - outerY
				rowAbove = outerY + centerY
				if (rowAbove >= TextureOpRasterizer.clipMinY && mirrorY <= TextureOpRasterizer.clipMaxY) {
					if (outerY >= innerRadius) {
						rowBelow = clamp(centerX + x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						right = clamp(centerX - x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						if (rowAbove <= TextureOpRasterizer.clipMaxY) {
							setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowAbove], right, rowBelow, strokeColor)
						}
						if (mirrorY >= TextureOpRasterizer.clipMinY) {
							setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![mirrorY], right, rowBelow, strokeColor)
						}
					} else {
						rowBelow = StaticMethods.circleInnerRadiusBuffer!![outerY]
						right = clamp(centerX + x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						innerRight = clamp(centerX - x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						innerExtent = clamp(centerX + rowBelow, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						left = clamp(centerX - rowBelow, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						if (rowAbove <= TextureOpRasterizer.clipMaxY) {
							upperRow = TextureShapeRasterizer.pixelBuffer!![rowAbove]
							setArrayValuesTo(upperRow, innerRight, left, strokeColor)
							setArrayValuesTo(upperRow, left, innerExtent, fillColor)
							setArrayValuesTo(upperRow, innerExtent, right, strokeColor)
						}
						if (mirrorY >= TextureOpRasterizer.clipMinY) {
							upperRow = TextureShapeRasterizer.pixelBuffer!![mirrorY]
							setArrayValuesTo(upperRow, innerRight, left, strokeColor)
							setArrayValuesTo(upperRow, left, innerExtent, fillColor)
							setArrayValuesTo(upperRow, innerExtent, right, strokeColor)
						}
					}
				}
			}
			mirrorY = centerY - x
			rowAbove = x + centerY
			if (rowAbove >= TextureOpRasterizer.clipMinY && mirrorY <= TextureOpRasterizer.clipMaxY) {
				rowBelow = centerX + outerY
				right = centerX - outerY
				if (rowBelow >= TextureOpRasterizer.clipMinX && right <= TextureOpRasterizer.clipMaxX) {
					rowBelow = clamp(rowBelow, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
					right = clamp(right, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
					if (x < innerRadius) {
						innerRight = if (innerY < x) StaticMethods.circleInnerRadiusBuffer!![x] else innerY
						innerExtent = clamp(centerX + innerRight, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						left = clamp(centerX - innerRight, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
						if (rowAbove <= TextureOpRasterizer.clipMaxY) {
							upperRow = TextureShapeRasterizer.pixelBuffer!![rowAbove]
							setArrayValuesTo(upperRow, right, left, strokeColor)
							setArrayValuesTo(upperRow, left, innerExtent, fillColor)
							setArrayValuesTo(upperRow, innerExtent, rowBelow, strokeColor)
						}
						if (mirrorY >= TextureOpRasterizer.clipMinY) {
							upperRow = TextureShapeRasterizer.pixelBuffer!![mirrorY]
							setArrayValuesTo(upperRow, right, left, strokeColor)
							setArrayValuesTo(upperRow, left, innerExtent, fillColor)
							setArrayValuesTo(upperRow, innerExtent, rowBelow, strokeColor)
						}
					} else {
						if (rowAbove <= TextureOpRasterizer.clipMaxY) {
							setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowAbove], right, rowBelow, strokeColor)
						}
						if (mirrorY >= TextureOpRasterizer.clipMinY) {
							setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![mirrorY], right, rowBelow, strokeColor)
						}
					}
				}
			}
		}
	}

	@JvmStatic
	fun drawClippedFilledEllipse(centerX: Int, centerY: Int, radiusX: Int, radiusY: Int, color: Int) {
		var x = 0
		var y = radiusY
		val rxSq = radiusX * radiusX
		val rySq = radiusY * radiusY
		val twoRySq = rySq shl 1
		val twoRxSq = rxSq shl 1
		val diameterY = radiusY shl 1
		var decisionParam1 = twoRySq + (1 - diameterY) * rxSq
		var decisionParam2 = rySq - twoRxSq * (diameterY - 1)
		val fourRxSq = rxSq shl 2
		val fourRySq = rySq shl 2
		var gradientX = ((x shl 1) + 3) * twoRySq
		var gradientY = ((radiusY shl 1) - 3) * twoRxSq
		var stepX = fourRySq * (x + 1)
		var stepY = fourRxSq * (radiusY - 1)
		var rowAbove: Int
		var rowBelow: Int
		if (centerY >= TextureOpRasterizer.clipMinY && centerY <= TextureOpRasterizer.clipMaxY) {
			rowAbove = clamp(centerX + radiusX, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			rowBelow = clamp(centerX - radiusX, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![centerY], rowBelow, rowAbove, color)
		}
		while (y > 0) {
			if (decisionParam1 < 0) {
				while (decisionParam1 < 0) {
					decisionParam1 += gradientX
					decisionParam2 += stepX
					gradientX += fourRySq
					stepX += fourRySq
					++x
				}
			}
			if (decisionParam2 < 0) {
				decisionParam1 += gradientX
				decisionParam2 += stepX
				gradientX += fourRySq
				stepX += fourRySq
				++x
			}
			decisionParam1 += -stepY
			decisionParam2 += -gradientY
			gradientY -= fourRxSq
			stepY -= fourRxSq
			--y
			rowAbove = centerY - y
			rowBelow = y + centerY
			if (rowBelow >= TextureOpRasterizer.clipMinY && rowAbove <= TextureOpRasterizer.clipMaxY) {
				val clippedRight = clamp(centerX + x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
				val clippedLeft = clamp(centerX - x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
				if (rowAbove >= TextureOpRasterizer.clipMinY) {
					setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowAbove], clippedLeft, clippedRight, color)
				}
				if (rowBelow <= TextureOpRasterizer.clipMaxY) {
					setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowBelow], clippedLeft, clippedRight, color)
				}
			}
		}
	}

	@JvmStatic
	fun drawClippedFilledCircle(centerX: Int, centerY: Int, radius: Int, color: Int) {
		var x = 0
		var y = radius
		var error = -radius
		var delta = -1
		val clippedRight = clamp(centerX + radius, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val clippedLeft = clamp(centerX - radius, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![centerY], clippedLeft, clippedRight, color)
		while (y > x) {
			delta += 2
			error += delta
			var rowAbove: Int
			var rowBelow: Int
			var right: Int
			var left: Int
			if (error > 0) {
				--y
				error -= y shl 1
				rowAbove = centerY - y
				rowBelow = y + centerY
				if (rowBelow >= TextureOpRasterizer.clipMinY && rowAbove <= TextureOpRasterizer.clipMaxY) {
					right = clamp(centerX + x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
					left = clamp(centerX - x, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
					if (rowBelow <= TextureOpRasterizer.clipMaxY) {
						setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowBelow], left, right, color)
					}
					if (rowAbove >= TextureOpRasterizer.clipMinY) {
						setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowAbove], left, right, color)
					}
				}
			}
			++x
			rowAbove = centerY - x
			rowBelow = x + centerY
			if (rowBelow >= TextureOpRasterizer.clipMinY && rowAbove <= TextureOpRasterizer.clipMaxY) {
				right = clamp(centerX + y, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
				left = clamp(centerX - y, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
				if (rowBelow <= TextureOpRasterizer.clipMaxY) {
					setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowBelow], left, right, color)
				}
				if (rowAbove >= TextureOpRasterizer.clipMinY) {
					setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowAbove], left, right, color)
				}
			}
		}
	}

	@JvmStatic
	fun drawClippedStrokedAndFilledRectangle(left: Int, right: Int, top: Int, bottom: Int, fillColor: Int, strokeColor: Int, borderWidth: Int) {
		val clippedTop = clamp(top, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val clippedBottom = clamp(bottom, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val clippedLeft = clamp(left, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val clippedRight = clamp(right, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val topBorderEnd = clamp(top + borderWidth, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val bottomBorderStart = clamp(bottom - borderWidth, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		var y: Int
		y = clippedTop
		while (y < topBorderEnd) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], clippedLeft, clippedRight, strokeColor)
			y++
		}
		y = clippedBottom
		while (y > bottomBorderStart) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], clippedLeft, clippedRight, strokeColor)
			--y
		}
		y = clamp(left + borderWidth, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val rightBorderStart = clamp(right - borderWidth, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		for (row in topBorderEnd..bottomBorderStart) {
			val scanline = TextureShapeRasterizer.pixelBuffer!![row]
			setArrayValuesTo(scanline, clippedLeft, y, strokeColor)
			setArrayValuesTo(scanline, y, rightBorderStart, fillColor)
			setArrayValuesTo(scanline, rightBorderStart, clippedRight, strokeColor)
		}
	}

	@JvmStatic
	fun fillClippedRectangle(left: Int, right: Int, top: Int, bottom: Int, color: Int) {
		val clippedTop = clamp(top, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val clippedBottom = clamp(bottom, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
		val clippedLeft = clamp(left, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		val clippedRight = clamp(right, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
		for (y in clippedTop..clippedBottom) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], clippedLeft, clippedRight, color)
		}
	}

	@JvmStatic
	fun clamp(value: Int, min: Int, max: Int): Int {
		return if (value < min) min else (Math.min(value, max))
	}

	@JvmStatic
	fun drawHorizontalLine(x1: Int, x2: Int, y: Int, color: Int) {
		if (x1 > x2) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], x2, x1, color)
		} else {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], x1, x2, color)
		}
	}

	@JvmStatic
	fun drawClippedHorizontalLine(x1: Int, x2: Int, y: Int, color: Int) {
		var x1 = x1
		var x2 = x2
		if (y >= TextureOpRasterizer.clipMinY && y <= TextureOpRasterizer.clipMaxY) {
			x1 = clamp(x1, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			x2 = clamp(x2, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
			drawHorizontalLine(x1, x2, y, color)
		}
	}

	@JvmStatic
	fun arrayCopy(src: ByteArray, length: Int): ByteArray {
		val dest = ByteArray(length)
		System.arraycopy(src, 0, dest, 0, length)
		return dest
	}

	@JvmStatic
	fun arrayCopy(src: FloatArray, length: Int): FloatArray {
		val dest = FloatArray(length)
		System.arraycopy(src, 0, dest, 0, length)
		return dest
	}

	@JvmStatic
	fun arrayCopy(src: ShortArray, length: Int): ShortArray {
		val dest = ShortArray(length)
		System.arraycopy(src, 0, dest, 0, length)
		return dest
	}

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	fun quicksort(strings: Array<String?>, ints: IntArray, fromIndex: Int, toIndex: Int) {
		var partitionIndex = fromIndex
		if (fromIndex < toIndex) {
			val middle = (toIndex + fromIndex) / 2
			partitionIndex = fromIndex
			val pivotString = strings[middle]
			strings[middle] = strings[toIndex]
			strings[toIndex] = pivotString
			val pivotInt = ints[middle]
			ints[middle] = ints[toIndex]
			ints[toIndex] = pivotInt
			for (i in fromIndex until toIndex) {
				if (pivotString == null || strings[i] != null && strings[i]!!.compareTo(pivotString) < (i and 0x1)) {
					val tempString = strings[i]
					strings[i] = strings[partitionIndex]
					strings[partitionIndex] = tempString
					val tempInt = ints[i]
					ints[i] = ints[partitionIndex]
					ints[partitionIndex++] = tempInt
				}
			}
			strings[toIndex] = strings[partitionIndex]
			strings[partitionIndex] = pivotString
			ints[toIndex] = ints[partitionIndex]
			ints[partitionIndex] = pivotInt
			quicksort(strings, ints, fromIndex, partitionIndex - 1)
			quicksort(strings, ints, partitionIndex + 1, toIndex)
		}
	}

	@JvmStatic
	fun quicksort(keys: IntArray, values: IntArray, startIndex: Int, endIndex: Int) {
		var partitionIndex = startIndex
		if (startIndex < endIndex) {
			val midIndex = (endIndex + startIndex) / 2
			partitionIndex = startIndex
			val pivotKey = keys[midIndex]
			keys[midIndex] = keys[endIndex]
			keys[endIndex] = pivotKey
			val pivotValue = values[midIndex]
			values[midIndex] = values[endIndex]
			values[endIndex] = pivotValue
			val tieBreakMask = if (pivotKey == Int.MAX_VALUE) 0 else 1
			for (i in startIndex until endIndex) {
				if (keys[i] < (i and tieBreakMask) + pivotKey) {
					val tempKey = keys[i]
					keys[i] = keys[partitionIndex]
					keys[partitionIndex] = tempKey
					val tempValue = values[i]
					values[i] = values[partitionIndex]
					values[partitionIndex++] = tempValue
				}
			}
			keys[endIndex] = keys[partitionIndex]
			keys[partitionIndex] = pivotKey
			values[endIndex] = values[partitionIndex]
			values[partitionIndex] = pivotValue
			quicksort(keys, values, startIndex, partitionIndex - 1)
			quicksort(keys, values, partitionIndex + 1, endIndex)
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
				val remaining = 255 - index
				val swapIndex = StaticMethods.boundedRandom(random, remaining)
				val temp = block[swapIndex]
				block[swapIndex] = block[remaining]
				block[remaining] = temp
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
	fun drawUnclippedFilledEllipse(centerX: Int, centerY: Int, radiusX: Int, radiusY: Int, color: Int) {
		var x = 0
		var y = radiusY
		val rxSq = radiusX * radiusX
		val rySq = radiusY * radiusY
		val twoRySq = rySq shl 1
		val twoRxSq = rxSq shl 1
		val diameterY = radiusY shl 1
		var decisionParam1 = twoRySq + (1 - diameterY) * rxSq
		var decisionParam2 = rySq - twoRxSq * (diameterY - 1)
		val fourRxSq = rxSq shl 2
		val fourRySq = rySq shl 2
		var gradientX = ((x shl 1) + 3) * twoRySq
		var gradientY = ((radiusY shl 1) - 3) * twoRxSq
		var stepX = fourRySq * (x + 1)
		var stepY = fourRxSq * (radiusY - 1)
		setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![centerY], centerX - radiusX, centerX + radiusX, color)
		while (y > 0) {
			if (decisionParam1 < 0) {
				while (decisionParam1 < 0) {
					decisionParam1 += gradientX
					decisionParam2 += stepX
					gradientX += fourRySq
					stepX += fourRySq
					++x
				}
			}
			if (decisionParam2 < 0) {
				decisionParam1 += gradientX
				decisionParam2 += stepX
				gradientX += fourRySq
				stepX += fourRySq
				++x
			}
			decisionParam1 += -stepY
			decisionParam2 += -gradientY
			gradientY -= fourRxSq
			stepY -= fourRxSq
			--y
			val rowAbove = centerY - y
			val rowBelow = y + centerY
			val pixelRight = centerX + x
			val pixelLeft = centerX - x
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowAbove], pixelLeft, pixelRight, color)
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![rowBelow], pixelLeft, pixelRight, color)
		}
	}

	@JvmStatic
	fun <T> quicksort(dimensions: IntArray, objects: Array<T>) {
		quicksort(dimensions, objects, 0, dimensions.size - 1)
	}

	@JvmStatic
	fun ensureCircleBufferCapacity(radius: Int) {
		if (StaticMethods.circleInnerRadiusBuffer == null || StaticMethods.circleInnerRadiusBuffer!!.size < radius) {
			StaticMethods.circleInnerRadiusBuffer = IntArray(radius)
		}
	}

	@JvmStatic
	fun drawFilledEllipse(centerX: Int, centerY: Int, radiusX: Int, radiusY: Int, color: Int) {
		if (radiusY == radiusX) {
			StaticMethods.drawFilledCircle(centerX, centerY, radiusX, color)
		} else if (centerX - radiusX >= TextureOpRasterizer.clipMinX && centerX + radiusX <= TextureOpRasterizer.clipMaxX && centerY - radiusY >= TextureOpRasterizer.clipMinY && radiusY + centerY <= TextureOpRasterizer.clipMaxY) {
			drawUnclippedFilledEllipse(centerX, centerY, radiusX, radiusY, color)
		} else {
			drawClippedFilledEllipse(centerX, centerY, radiusX, radiusY, color)
		}

	}

	@JvmStatic
	fun drawUnclippedStrokedAndFilledRectangle(left: Int, right: Int, top: Int, bottom: Int, fillColor: Int, strokeColor: Int, borderWidth: Int) {
		val topBorderEnd = top + borderWidth
		val bottomBorderStart = bottom - borderWidth
		var y: Int
		y = top
		while (y < topBorderEnd) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], left, right, strokeColor)
			y++
		}
		y = bottom
		while (y > bottomBorderStart) {
			setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], left, right, strokeColor)
			--y
		}
		y = left + borderWidth
		val rightBorderStart = right - borderWidth
		for (row in topBorderEnd..bottomBorderStart) {
			val scanline = TextureShapeRasterizer.pixelBuffer!![row]
			setArrayValuesTo(scanline, left, y, strokeColor)
			setArrayValuesTo(scanline, y, rightBorderStart, fillColor)
			setArrayValuesTo(scanline, rightBorderStart, right, strokeColor)
		}
	}
}
