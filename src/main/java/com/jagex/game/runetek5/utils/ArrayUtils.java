package com.jagex.game.runetek5.utils;

import java.util.Random;

import com.jagex.game.runetek5.cache.node.CacheableNodeQueue;
import com.jagex.game.runetek5.cache.node.lru.CacheableNodeByteArray;
import com.jagex.game.runetek5.config.material.texture.operation.Class149;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpRasterizer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public class ArrayUtils {

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
	public static void arrayCopy(int[] ints_0, int i_1, int[] ints_2, int i_3, int i_4) {
	    if (ints_2 == ints_0) {
	        if (i_3 == i_1) {
	            return;
	        }
	
	        if (i_3 > i_1 && i_3 < i_4 + i_1) {
	            --i_4;
	            i_1 += i_4;
	            i_3 += i_4;
	            i_4 = i_1 - i_4;
	
	            for (i_4 += 7; i_1 >= i_4; ints_2[i_3--] = ints_0[i_1--]) {
	                ints_2[i_3--] = ints_0[i_1--];
	                ints_2[i_3--] = ints_0[i_1--];
	                ints_2[i_3--] = ints_0[i_1--];
	                ints_2[i_3--] = ints_0[i_1--];
	                ints_2[i_3--] = ints_0[i_1--];
	                ints_2[i_3--] = ints_0[i_1--];
	                ints_2[i_3--] = ints_0[i_1--];
	            }
	
	            for (i_4 -= 7; i_1 >= i_4; ints_2[i_3--] = ints_0[i_1--]) {
	            }
	
	            return;
	        }
	    }
	
	    i_4 += i_1;
	
	    for (i_4 -= 7; i_1 < i_4; ints_2[i_3++] = ints_0[i_1++]) {
	        ints_2[i_3++] = ints_0[i_1++];
	        ints_2[i_3++] = ints_0[i_1++];
	        ints_2[i_3++] = ints_0[i_1++];
	        ints_2[i_3++] = ints_0[i_1++];
	        ints_2[i_3++] = ints_0[i_1++];
	        ints_2[i_3++] = ints_0[i_1++];
	        ints_2[i_3++] = ints_0[i_1++];
	    }
	
	    for (i_4 += 7; i_1 < i_4; ints_2[i_3++] = ints_0[i_1++]) {
	    }
	
	}

	public static void arrayCopy(byte[] bytes_0, int i_1, byte[] bytes_2, int i_3, int i_4) {
	    if (bytes_2 == bytes_0) {
	        if (i_3 == i_1) {
	            return;
	        }
	
	        if (i_3 > i_1 && i_3 < i_4 + i_1) {
	            --i_4;
	            i_1 += i_4;
	            i_3 += i_4;
	            i_4 = i_1 - i_4;
	
	            for (i_4 += 7; i_1 >= i_4; bytes_2[i_3--] = bytes_0[i_1--]) {
	                bytes_2[i_3--] = bytes_0[i_1--];
	                bytes_2[i_3--] = bytes_0[i_1--];
	                bytes_2[i_3--] = bytes_0[i_1--];
	                bytes_2[i_3--] = bytes_0[i_1--];
	                bytes_2[i_3--] = bytes_0[i_1--];
	                bytes_2[i_3--] = bytes_0[i_1--];
	                bytes_2[i_3--] = bytes_0[i_1--];
	            }
	
	            for (i_4 -= 7; i_1 >= i_4; bytes_2[i_3--] = bytes_0[i_1--]) {
	            }
	
	            return;
	        }
	    }
	
	    i_4 += i_1;
	
	    for (i_4 -= 7; i_1 < i_4; bytes_2[i_3++] = bytes_0[i_1++]) {
	        bytes_2[i_3++] = bytes_0[i_1++];
	        bytes_2[i_3++] = bytes_0[i_1++];
	        bytes_2[i_3++] = bytes_0[i_1++];
	        bytes_2[i_3++] = bytes_0[i_1++];
	        bytes_2[i_3++] = bytes_0[i_1++];
	        bytes_2[i_3++] = bytes_0[i_1++];
	        bytes_2[i_3++] = bytes_0[i_1++];
	    }
	
	    for (i_4 += 7; i_1 < i_4; bytes_2[i_3++] = bytes_0[i_1++]) {
	    }
	
	}

	public static void arrayCopy(long[] longs_0, int i_1, long[] longs_2, int i_3, int i_4) {
	    if (longs_2 == longs_0) {
	        if (i_3 == i_1) {
	            return;
	        }
	
	        if (i_3 > i_1 && i_3 < i_4 + i_1) {
	            --i_4;
	            i_1 += i_4;
	            i_3 += i_4;
	            i_4 = i_1 - i_4;
	
	            for (i_4 += 3; i_1 >= i_4; longs_2[i_3--] = longs_0[i_1--]) {
	                longs_2[i_3--] = longs_0[i_1--];
	                longs_2[i_3--] = longs_0[i_1--];
	                longs_2[i_3--] = longs_0[i_1--];
	            }
	
	            for (i_4 -= 3; i_1 >= i_4; longs_2[i_3--] = longs_0[i_1--]) {
	            }
	
	            return;
	        }
	    }
	
	    i_4 += i_1;
	
	    for (i_4 -= 3; i_1 < i_4; longs_2[i_3++] = longs_0[i_1++]) {
	        longs_2[i_3++] = longs_0[i_1++];
	        longs_2[i_3++] = longs_0[i_1++];
	        longs_2[i_3++] = longs_0[i_1++];
	    }
	
	    for (i_4 += 3; i_1 < i_4; longs_2[i_3++] = longs_0[i_1++]) {
	    }
	
	}

	public static void arrayCopy(Object[] arr_0, int i_1, Object[] arr_2, int i_3, int i_4) {
	    if (arr_2 == arr_0) {
	        if (i_3 == i_1) {
	            return;
	        }
	
	        if (i_3 > i_1 && i_3 < i_4 + i_1) {
	            --i_4;
	            i_1 += i_4;
	            i_3 += i_4;
	            i_4 = i_1 - i_4;
	
	            for (i_4 += 7; i_1 >= i_4; arr_2[i_3--] = arr_0[i_1--]) {
	                arr_2[i_3--] = arr_0[i_1--];
	                arr_2[i_3--] = arr_0[i_1--];
	                arr_2[i_3--] = arr_0[i_1--];
	                arr_2[i_3--] = arr_0[i_1--];
	                arr_2[i_3--] = arr_0[i_1--];
	                arr_2[i_3--] = arr_0[i_1--];
	                arr_2[i_3--] = arr_0[i_1--];
	            }
	
	            for (i_4 -= 7; i_1 >= i_4; arr_2[i_3--] = arr_0[i_1--]) {
	            }
	
	            return;
	        }
	    }
	
	    i_4 += i_1;
	
	    for (i_4 -= 7; i_1 < i_4; arr_2[i_3++] = arr_0[i_1++]) {
	        arr_2[i_3++] = arr_0[i_1++];
	        arr_2[i_3++] = arr_0[i_1++];
	        arr_2[i_3++] = arr_0[i_1++];
	        arr_2[i_3++] = arr_0[i_1++];
	        arr_2[i_3++] = arr_0[i_1++];
	        arr_2[i_3++] = arr_0[i_1++];
	        arr_2[i_3++] = arr_0[i_1++];
	    }
	
	    for (i_4 += 7; i_1 < i_4; arr_2[i_3++] = arr_0[i_1++]) {
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
	public static void quicksort(long[] longs_0, Object[] arr_1, int srcPos, int size) {
	    if (srcPos < size) {
	        int i_5 = (size + srcPos) / 2;
	        int SrcPos = srcPos;
	        long long_7 = longs_0[i_5];
	        longs_0[i_5] = longs_0[size];
	        longs_0[size] = long_7;
	        Object object9 = arr_1[i_5];
	        arr_1[i_5] = arr_1[size];
	        arr_1[size] = object9;
	        int i_10 = long_7 == Long.MAX_VALUE ? 0 : 1;
	        for (int i_11 = srcPos; i_11 < size; i_11++) {
	            if (longs_0[i_11] < (i_11 & i_10) + long_7) {
	                long long_12 = longs_0[i_11];
	                longs_0[i_11] = longs_0[SrcPos];
	                longs_0[SrcPos] = long_12;
	                Object object14 = arr_1[i_11];
	                arr_1[i_11] = arr_1[SrcPos];
	                arr_1[SrcPos++] = object14;
	            }
	        }
	        longs_0[size] = longs_0[SrcPos];
	        longs_0[SrcPos] = long_7;
	        arr_1[size] = arr_1[SrcPos];
	        arr_1[SrcPos] = object9;
	        quicksort(longs_0, arr_1, srcPos, SrcPos - 1);
	        quicksort(longs_0, arr_1, SrcPos + 1, size);
	    }
	}

	public static void quicksort(long[] longs_0, Object[] arr_1) {
	    quicksort(longs_0, arr_1, 0, longs_0.length - 1);
	}

	public static void setVariables(int[] array, int position, int startPosition, int variableToSet) {
	    for (startPosition = startPosition + position - 7; position < startPosition; array[position++] = variableToSet) {
	        array[position++] = variableToSet;
	        array[position++] = variableToSet;
	        array[position++] = variableToSet;
	        array[position++] = variableToSet;
	        array[position++] = variableToSet;
	        array[position++] = variableToSet;
	        array[position++] = variableToSet;
	    }
	    for (startPosition += 7; position < startPosition; array[position++] = variableToSet) { }
	}

	public static byte[] safeFullArraycopy(byte[] originalData) {
	    if (originalData == null) {
	        return null;
	    } 
	    byte[] data = new byte[originalData.length];
	    System.arraycopy(originalData, 0, data, 0, originalData.length);
	    return data;
	}

	public static void quicksort(int[] ints_0, Object[] arr_1, int i_2, int i_3) {
	    if (i_2 < i_3) {
	        int i_5 = (i_3 + i_2) / 2;
	        int i_6 = i_2;
	        int i_7 = ints_0[i_5];
	        ints_0[i_5] = ints_0[i_3];
	        ints_0[i_3] = i_7;
	        Object object8 = arr_1[i_5];
	        arr_1[i_5] = arr_1[i_3];
	        arr_1[i_3] = object8;
	        int i_9 = i_7 == Integer.MAX_VALUE ? 0 : 1;
	        for (int i_10 = i_2; i_10 < i_3; i_10++) {
	            if (ints_0[i_10] < (i_10 & i_9) + i_7) {
	                int i_11 = ints_0[i_10];
	                ints_0[i_10] = ints_0[i_6];
	                ints_0[i_6] = i_11;
	                Object object12 = arr_1[i_10];
	                arr_1[i_10] = arr_1[i_6];
	                arr_1[i_6++] = object12;
	            }
	        }
	        ints_0[i_3] = ints_0[i_6];
	        ints_0[i_6] = i_7;
	        arr_1[i_3] = arr_1[i_6];
	        arr_1[i_6] = object8;
	        quicksort(ints_0, arr_1, i_2, i_6 - 1);
	        quicksort(ints_0, arr_1, i_6 + 1, i_3);
	    }
	}

	public static short[] arraycopy(short[] src) {
		if (null == src)
			return null;
		short[] dst = new short[src.length];
		System.arraycopy(src, 0, dst, 0, src.length);
		return dst;
	}

	public static void quicksort(String[] arr_0, short[] shorts_1, int i_2, int i_3) {
	    if (i_2 < i_3) {
	        int i_5 = (i_3 + i_2) / 2;
	        int i_6 = i_2;
	        String string_7 = arr_0[i_5];
	        arr_0[i_5] = arr_0[i_3];
	        arr_0[i_3] = string_7;
	        short s_8 = shorts_1[i_5];
	        shorts_1[i_5] = shorts_1[i_3];
	        shorts_1[i_3] = s_8;
	        for (int i_9 = i_2; i_9 < i_3; i_9++) {
	            if (string_7 == null || arr_0[i_9] != null && arr_0[i_9].compareTo(string_7) < (i_9 & 0x1)) {
	                String string_10 = arr_0[i_9];
	                arr_0[i_9] = arr_0[i_6];
	                arr_0[i_6] = string_10;
	                short s_11 = shorts_1[i_9];
	                shorts_1[i_9] = shorts_1[i_6];
	                shorts_1[i_6++] = s_11;
	            }
	        }
	        arr_0[i_3] = arr_0[i_6];
	        arr_0[i_6] = string_7;
	        shorts_1[i_3] = shorts_1[i_6];
	        shorts_1[i_6] = s_8;
	        quicksort(arr_0, shorts_1, i_2, i_6 - 1);
	        quicksort(arr_0, shorts_1, i_6 + 1, i_3);
	    }
	}

	public static void quicksort(long[] longs, int[] ints, int startPosition, int endPosition) {
	    if (startPosition < endPosition) {
	        int center = (endPosition + startPosition) / 2;
	        int curPosition = startPosition;
	        long offset = longs[center];
	        longs[center] = longs[endPosition];
	        longs[endPosition] = offset;
	        int i_9 = ints[center];
	        ints[center] = ints[endPosition];
	        ints[endPosition] = i_9;
	        int mask = offset == Long.MAX_VALUE ? 0 : 1;
	
	        for (int index = startPosition; index < endPosition; index++) {
	            if (longs[index] < (index & mask) + offset) {
	                long tmpLong = longs[index];
	                longs[index] = longs[curPosition];
	                longs[curPosition] = tmpLong;
	                int tmpInt = ints[index];
	                ints[index] = ints[curPosition];
	                ints[curPosition++] = tmpInt;
	            }
	        }
	
	        longs[endPosition] = longs[curPosition];
	        longs[curPosition] = offset;
	        ints[endPosition] = ints[curPosition];
	        ints[curPosition] = i_9;
	        quicksort(longs, ints, startPosition, curPosition - 1);
	        quicksort(longs, ints, curPosition + 1, endPosition);
	    }	
	}

	public static void arrayCopy(float[] floats_0, int i_1, float[] floats_2, int i_3, int i_4) {
	    if (floats_2 == floats_0) {
	        if (i_3 == i_1) {
	            return;
	        }
	
	        if (i_3 > i_1 && i_3 < i_4 + i_1) {
	            --i_4;
	            i_1 += i_4;
	            i_3 += i_4;
	            i_4 = i_1 - i_4;
	
	            for (i_4 += 7; i_1 >= i_4; floats_2[i_3--] = floats_0[i_1--]) {
	                floats_2[i_3--] = floats_0[i_1--];
	                floats_2[i_3--] = floats_0[i_1--];
	                floats_2[i_3--] = floats_0[i_1--];
	                floats_2[i_3--] = floats_0[i_1--];
	                floats_2[i_3--] = floats_0[i_1--];
	                floats_2[i_3--] = floats_0[i_1--];
	                floats_2[i_3--] = floats_0[i_1--];
	            }
	
	            for (i_4 -= 7; i_1 >= i_4; floats_2[i_3--] = floats_0[i_1--]) {
	            }
	
	            return;
	        }
	    }
	
	    i_4 += i_1;
	
	    for (i_4 -= 7; i_1 < i_4; floats_2[i_3++] = floats_0[i_1++]) {
	        floats_2[i_3++] = floats_0[i_1++];
	        floats_2[i_3++] = floats_0[i_1++];
	        floats_2[i_3++] = floats_0[i_1++];
	        floats_2[i_3++] = floats_0[i_1++];
	        floats_2[i_3++] = floats_0[i_1++];
	        floats_2[i_3++] = floats_0[i_1++];
	        floats_2[i_3++] = floats_0[i_1++];
	    }
	
	    for (i_4 += 7; i_1 < i_4; floats_2[i_3++] = floats_0[i_1++]) {
	    }
	
	}

	public static void setArrayValuesTo(int[] array, int index, int length, int dstValue) {
	    --index;
	    --length;
	    for (int i_5 = length - 7; index < i_5; array[index] = dstValue) {
	        array[++index] = dstValue;
	        array[++index] = dstValue;
	        array[++index] = dstValue;
	        array[++index] = dstValue;
	        array[++index] = dstValue;
	        array[++index] = dstValue;
	        array[++index] = dstValue;
	        index++;
	    }
	    while (index <= length) {
	        array[++index] = dstValue;
	    }
	}

	public static void method2637(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5) {
	    ArrayUtils.method7170(i_2);
	    int i_7 = 0;
	    int i_8 = i_2 - i_5;
	    if (i_8 < 0) {
	        i_8 = 0;
	    }
	    int i_9 = i_2;
	    int i_10 = -i_2;
	    int i_11 = i_8;
	    int i_12 = -i_8;
	    int i_13 = -1;
	    int i_14 = -1;
	    int[] ints_15 = Class149.anIntArrayArray7072[i_1];
	    int i_16 = i_0 - i_8;
	    int i_17 = i_0 + i_8;
	    setArrayValuesTo(ints_15, i_0 - i_2, i_16, i_4);
	    setArrayValuesTo(ints_15, i_16, i_17, i_3);
	    setArrayValuesTo(ints_15, i_17, i_0 + i_2, i_4);
	    while (i_9 > i_7) {
	        i_13 += 2;
	        i_14 += 2;
	        i_10 += i_13;
	        i_12 += i_14;
	        if (i_12 >= 0 && i_11 >= 1) {
	            StaticMethods.anIntArray36[i_11] = i_7;
	            --i_11;
	            i_12 -= i_11 << 1;
	        }
	        ++i_7;
	        int[] ints_18;
	        int[] ints_19;
	        int i_20;
	        int i_21;
	        int i_22;
	        int i_23;
	        int i_24;
	        if (i_10 >= 0) {
	            --i_9;
	            i_10 -= i_9 << 1;
	            if (i_9 >= i_8) {
	                ints_18 = Class149.anIntArrayArray7072[i_9 + i_1];
	                ints_19 = Class149.anIntArrayArray7072[i_1 - i_9];
	                i_20 = i_0 + i_7;
	                i_21 = i_0 - i_7;
	                setArrayValuesTo(ints_18, i_21, i_20, i_4);
	                setArrayValuesTo(ints_19, i_21, i_20, i_4);
	            } else {
	                ints_18 = Class149.anIntArrayArray7072[i_9 + i_1];
	                ints_19 = Class149.anIntArrayArray7072[i_1 - i_9];
	                i_20 = StaticMethods.anIntArray36[i_9];
	                i_21 = i_0 + i_7;
	                i_22 = i_0 - i_7;
	                i_23 = i_0 + i_20;
	                i_24 = i_0 - i_20;
	                setArrayValuesTo(ints_18, i_22, i_24, i_4);
	                setArrayValuesTo(ints_18, i_24, i_23, i_3);
	                setArrayValuesTo(ints_18, i_23, i_21, i_4);
	                setArrayValuesTo(ints_19, i_22, i_24, i_4);
	                setArrayValuesTo(ints_19, i_24, i_23, i_3);
	                setArrayValuesTo(ints_19, i_23, i_21, i_4);
	            }
	        }
	        ints_18 = Class149.anIntArrayArray7072[i_7 + i_1];
	        ints_19 = Class149.anIntArrayArray7072[i_1 - i_7];
	        i_20 = i_0 + i_9;
	        i_21 = i_0 - i_9;
	        if (i_7 < i_8) {
	            i_22 = i_11 < i_7 ? StaticMethods.anIntArray36[i_7] : i_11;
	            i_23 = i_0 + i_22;
	            i_24 = i_0 - i_22;
	            setArrayValuesTo(ints_18, i_21, i_24, i_4);
	            setArrayValuesTo(ints_18, i_24, i_23, i_3);
	            setArrayValuesTo(ints_18, i_23, i_20, i_4);
	            setArrayValuesTo(ints_19, i_21, i_24, i_4);
	            setArrayValuesTo(ints_19, i_24, i_23, i_3);
	            setArrayValuesTo(ints_19, i_23, i_20, i_4);
	        } else {
	            setArrayValuesTo(ints_18, i_21, i_20, i_4);
	            setArrayValuesTo(ints_19, i_21, i_20, i_4);
	        }
	    }
	}

	public static void method744(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5) {
	    int i_7 = ArrayUtils.method4890(i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_8 = ArrayUtils.method4890(i_3, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_9 = ArrayUtils.method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_10 = ArrayUtils.method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_11 = ArrayUtils.method4890(i_5 + i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_12 = ArrayUtils.method4890(i_3 - i_5, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_13;
	    for (i_13 = i_7; i_13 < i_11; i_13++) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_13], i_9, i_10, i_4);
	    }
	    for (i_13 = i_8; i_13 > i_12; --i_13) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_13], i_9, i_10, i_4);
	    }
	    i_13 = ArrayUtils.method4890(i_5 + i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_14 = ArrayUtils.method4890(i_1 - i_5, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    for (int i_15 = i_11; i_15 <= i_12; i_15++) {
	        int[] ints_16 = Class149.anIntArrayArray7072[i_15];
	        setArrayValuesTo(ints_16, i_9, i_13, i_4);
	        setArrayValuesTo(ints_16, i_14, i_10, i_4);
	    }
	}

	public static void method1174(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5) {
	    ArrayUtils.method7170(i_2);
	    int i_7 = 0;
	    int i_8 = i_2 - i_5;
	    if (i_8 < 0) {
	        i_8 = 0;
	    }
	    int i_9 = i_2;
	    int i_10 = -i_2;
	    int i_11 = i_8;
	    int i_12 = -i_8;
	    int i_13 = -1;
	    int i_14 = -1;
	    int i_16;
	    int i_17;
	    int i_18;
	    int i_19;
	    if (i_1 >= TextureOpRasterizer.anInt7070 && i_1 <= TextureOpRasterizer.anInt7068) {
	        int[] ints_15 = Class149.anIntArrayArray7072[i_1];
	        i_16 = ArrayUtils.method4890(i_0 - i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_17 = ArrayUtils.method4890(i_0 + i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_18 = ArrayUtils.method4890(i_0 - i_8, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_19 = ArrayUtils.method4890(i_0 + i_8, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        setArrayValuesTo(ints_15, i_16, i_18, i_4);
	        setArrayValuesTo(ints_15, i_18, i_19, i_3);
	        setArrayValuesTo(ints_15, i_19, i_17, i_4);
	    }
	    while (i_9 > i_7) {
	        i_13 += 2;
	        i_14 += 2;
	        i_10 += i_13;
	        i_12 += i_14;
	        if (i_12 >= 0 && i_11 >= 1) {
	            --i_11;
	            i_12 -= i_11 << 1;
	            StaticMethods.anIntArray36[i_11] = i_7;
	        }
	        ++i_7;
	        int i_20;
	        int i_21;
	        int[] ints_22;
	        int i_23;
	        if (i_10 >= 0) {
	            --i_9;
	            i_10 -= i_9 << 1;
	            i_23 = i_1 - i_9;
	            i_16 = i_9 + i_1;
	            if (i_16 >= TextureOpRasterizer.anInt7070 && i_23 <= TextureOpRasterizer.anInt7068) {
	                if (i_9 >= i_8) {
	                    i_17 = ArrayUtils.method4890(i_0 + i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    i_18 = ArrayUtils.method4890(i_0 - i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    if (i_16 <= TextureOpRasterizer.anInt7068) {
	                        setArrayValuesTo(Class149.anIntArrayArray7072[i_16], i_18, i_17, i_4);
	                    }
	                    if (i_23 >= TextureOpRasterizer.anInt7070) {
	                        setArrayValuesTo(Class149.anIntArrayArray7072[i_23], i_18, i_17, i_4);
	                    }
	                } else {
	                    i_17 = StaticMethods.anIntArray36[i_9];
	                    i_18 = ArrayUtils.method4890(i_0 + i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    i_19 = ArrayUtils.method4890(i_0 - i_7, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    i_20 = ArrayUtils.method4890(i_0 + i_17, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    i_21 = ArrayUtils.method4890(i_0 - i_17, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    if (i_16 <= TextureOpRasterizer.anInt7068) {
	                        ints_22 = Class149.anIntArrayArray7072[i_16];
	                        setArrayValuesTo(ints_22, i_19, i_21, i_4);
	                        setArrayValuesTo(ints_22, i_21, i_20, i_3);
	                        setArrayValuesTo(ints_22, i_20, i_18, i_4);
	                    }
	                    if (i_23 >= TextureOpRasterizer.anInt7070) {
	                        ints_22 = Class149.anIntArrayArray7072[i_23];
	                        setArrayValuesTo(ints_22, i_19, i_21, i_4);
	                        setArrayValuesTo(ints_22, i_21, i_20, i_3);
	                        setArrayValuesTo(ints_22, i_20, i_18, i_4);
	                    }
	                }
	            }
	        }
	        i_23 = i_1 - i_7;
	        i_16 = i_7 + i_1;
	        if (i_16 >= TextureOpRasterizer.anInt7070 && i_23 <= TextureOpRasterizer.anInt7068) {
	            i_17 = i_0 + i_9;
	            i_18 = i_0 - i_9;
	            if (i_17 >= TextureOpRasterizer.anInt7071 && i_18 <= TextureOpRasterizer.anInt7069) {
	                i_17 = ArrayUtils.method4890(i_17, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                i_18 = ArrayUtils.method4890(i_18, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                if (i_7 < i_8) {
	                    i_19 = i_11 < i_7 ? StaticMethods.anIntArray36[i_7] : i_11;
	                    i_20 = ArrayUtils.method4890(i_0 + i_19, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    i_21 = ArrayUtils.method4890(i_0 - i_19, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                    if (i_16 <= TextureOpRasterizer.anInt7068) {
	                        ints_22 = Class149.anIntArrayArray7072[i_16];
	                        setArrayValuesTo(ints_22, i_18, i_21, i_4);
	                        setArrayValuesTo(ints_22, i_21, i_20, i_3);
	                        setArrayValuesTo(ints_22, i_20, i_17, i_4);
	                    }
	                    if (i_23 >= TextureOpRasterizer.anInt7070) {
	                        ints_22 = Class149.anIntArrayArray7072[i_23];
	                        setArrayValuesTo(ints_22, i_18, i_21, i_4);
	                        setArrayValuesTo(ints_22, i_21, i_20, i_3);
	                        setArrayValuesTo(ints_22, i_20, i_17, i_4);
	                    }
	                } else {
	                    if (i_16 <= TextureOpRasterizer.anInt7068) {
	                        setArrayValuesTo(Class149.anIntArrayArray7072[i_16], i_18, i_17, i_4);
	                    }
	                    if (i_23 >= TextureOpRasterizer.anInt7070) {
	                        setArrayValuesTo(Class149.anIntArrayArray7072[i_23], i_18, i_17, i_4);
	                    }
	                }
	            }
	        }
	    }
	}

	public static void method15405(int i_0, int i_1, int i_2, int i_3, int i_4) {
	    int i_6 = 0;
	    int i_7 = i_3;
	    int i_8 = i_2 * i_2;
	    int i_9 = i_3 * i_3;
	    int i_10 = i_9 << 1;
	    int i_11 = i_8 << 1;
	    int i_12 = i_3 << 1;
	    int i_13 = i_10 + (1 - i_12) * i_8;
	    int i_14 = i_9 - i_11 * (i_12 - 1);
	    int i_15 = i_8 << 2;
	    int i_16 = i_9 << 2;
	    int i_17 = ((i_6 << 1) + 3) * i_10;
	    int i_18 = ((i_3 << 1) - 3) * i_11;
	    int i_19 = i_16 * (i_6 + 1);
	    int i_20 = i_15 * (i_3 - 1);
	    int i_21;
	    int i_22;
	    if (i_1 >= TextureOpRasterizer.anInt7070 && i_1 <= TextureOpRasterizer.anInt7068) {
	        i_21 = ArrayUtils.method4890(i_0 + i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_22 = ArrayUtils.method4890(i_0 - i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_1], i_22, i_21, i_4);
	    }
	    while (i_7 > 0) {
	        if (i_13 < 0) {
	            while (i_13 < 0) {
	                i_13 += i_17;
	                i_14 += i_19;
	                i_17 += i_16;
	                i_19 += i_16;
	                ++i_6;
	            }
	        }
	        if (i_14 < 0) {
	            i_13 += i_17;
	            i_14 += i_19;
	            i_17 += i_16;
	            i_19 += i_16;
	            ++i_6;
	        }
	        i_13 += -i_20;
	        i_14 += -i_18;
	        i_18 -= i_15;
	        i_20 -= i_15;
	        --i_7;
	        i_21 = i_1 - i_7;
	        i_22 = i_7 + i_1;
	        if (i_22 >= TextureOpRasterizer.anInt7070 && i_21 <= TextureOpRasterizer.anInt7068) {
	            int i_23 = ArrayUtils.method4890(i_0 + i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	            int i_24 = ArrayUtils.method4890(i_0 - i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	            if (i_21 >= TextureOpRasterizer.anInt7070) {
	                setArrayValuesTo(Class149.anIntArrayArray7072[i_21], i_24, i_23, i_4);
	            }
	            if (i_22 <= TextureOpRasterizer.anInt7068) {
	                setArrayValuesTo(Class149.anIntArrayArray7072[i_22], i_24, i_23, i_4);
	            }
	        }
	    }
	}

	public static void method4866(int i_0, int i_1, int i_2, int i_3) {
	    int i_5 = 0;
	    int i_6 = i_2;
	    int i_7 = -i_2;
	    int i_8 = -1;
	    int i_9 = ArrayUtils.method4890(i_0 + i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_10 = ArrayUtils.method4890(i_0 - i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    setArrayValuesTo(Class149.anIntArrayArray7072[i_1], i_10, i_9, i_3);
	    while (i_6 > i_5) {
	        i_8 += 2;
	        i_7 += i_8;
	        int i_11;
	        int i_12;
	        int i_13;
	        int i_14;
	        if (i_7 > 0) {
	            --i_6;
	            i_7 -= i_6 << 1;
	            i_11 = i_1 - i_6;
	            i_12 = i_6 + i_1;
	            if (i_12 >= TextureOpRasterizer.anInt7070 && i_11 <= TextureOpRasterizer.anInt7068) {
	                i_13 = ArrayUtils.method4890(i_0 + i_5, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                i_14 = ArrayUtils.method4890(i_0 - i_5, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                if (i_12 <= TextureOpRasterizer.anInt7068) {
	                    setArrayValuesTo(Class149.anIntArrayArray7072[i_12], i_14, i_13, i_3);
	                }
	                if (i_11 >= TextureOpRasterizer.anInt7070) {
	                    setArrayValuesTo(Class149.anIntArrayArray7072[i_11], i_14, i_13, i_3);
	                }
	            }
	        }
	        ++i_5;
	        i_11 = i_1 - i_5;
	        i_12 = i_5 + i_1;
	        if (i_12 >= TextureOpRasterizer.anInt7070 && i_11 <= TextureOpRasterizer.anInt7068) {
	            i_13 = ArrayUtils.method4890(i_0 + i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	            i_14 = ArrayUtils.method4890(i_0 - i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	            if (i_12 <= TextureOpRasterizer.anInt7068) {
	                setArrayValuesTo(Class149.anIntArrayArray7072[i_12], i_14, i_13, i_3);
	            }
	            if (i_11 >= TextureOpRasterizer.anInt7070) {
	                setArrayValuesTo(Class149.anIntArrayArray7072[i_11], i_14, i_13, i_3);
	            }
	        }
	    }
	}

	public static void method4034(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
	    int i_8 = ArrayUtils.method4890(i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_9 = ArrayUtils.method4890(i_3, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_10 = ArrayUtils.method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_11 = ArrayUtils.method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_12 = ArrayUtils.method4890(i_2 + i_6, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_13 = ArrayUtils.method4890(i_3 - i_6, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_14;
	    for (i_14 = i_8; i_14 < i_12; i_14++) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_14], i_10, i_11, i_5);
	    }
	    for (i_14 = i_9; i_14 > i_13; --i_14) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_14], i_10, i_11, i_5);
	    }
	    i_14 = ArrayUtils.method4890(i_0 + i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_15 = ArrayUtils.method4890(i_1 - i_6, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    for (int i_16 = i_12; i_16 <= i_13; i_16++) {
	        int[] ints_17 = Class149.anIntArrayArray7072[i_16];
	        setArrayValuesTo(ints_17, i_10, i_14, i_5);
	        setArrayValuesTo(ints_17, i_14, i_15, i_4);
	        setArrayValuesTo(ints_17, i_15, i_11, i_5);
	    }
	}

	public static void method7728(int i_0, int i_1, int i_2, int i_3, int i_4) {
	    int i_6 = ArrayUtils.method4890(i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_7 = ArrayUtils.method4890(i_3, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	    int i_8 = ArrayUtils.method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    int i_9 = ArrayUtils.method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	    for (int i_10 = i_6; i_10 <= i_7; i_10++) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_10], i_8, i_9, i_4);
	    }
	}

	public static int method4890(int i_0, int i_1, int i_2) {
	    return i_0 < i_1 ? i_1 : (Math.min(i_0, i_2));
	}

	public static void method568(int i_0, int i_1, int i_2, int i_3) {
	    if (i_0 > i_1) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_2], i_1, i_0, i_3);
	    } else {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_2], i_0, i_1, i_3);
	    }
	}

	public static void method11250(int i_0, int i_1, int i_2, int i_3) {
	    if (i_2 >= TextureOpRasterizer.anInt7070 && i_2 <= TextureOpRasterizer.anInt7068) {
	        i_0 = method4890(i_0, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_1 = method4890(i_1, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        method568(i_0, i_1, i_2, i_3);
	    }
	}

	public static byte[] arrayCopy(byte[] bytes_0, int i_1) {
	    byte[] bytes_2 = new byte[i_1];
	    System.arraycopy(bytes_0, 0, bytes_2, 0, i_1);
	    return bytes_2;
	}

	public static float[] arrayCopy(float[] floats_0, int i_1) {
	    float[] floats_2 = new float[i_1];
	    System.arraycopy(floats_0, 0, floats_2, 0, i_1);
	    return floats_2;
	}

	public static short[] arrayCopy(short[] shorts_0, int i_1) {
	    short[] shorts_2 = new short[i_1];
	    System.arraycopy(shorts_0, 0, shorts_2, 0, i_1);
	    return shorts_2;
	}

	public static void quicksort(String[] strings, int[] ints, int fromIndex, int toIndex) {
	    if (fromIndex < toIndex) {
	        int middle = (toIndex + fromIndex) / 2;
	        int i_6 = fromIndex;
	        String string_7 = strings[middle];
	        strings[middle] = strings[toIndex];
	        strings[toIndex] = string_7;
	        int i_8 = ints[middle];
	        ints[middle] = ints[toIndex];
	        ints[toIndex] = i_8;
	        for (int i_9 = fromIndex; i_9 < toIndex; i_9++) {
	            if (string_7 == null || strings[i_9] != null && strings[i_9].compareTo(string_7) < (i_9 & 0x1)) {
	                String string_10 = strings[i_9];
	                strings[i_9] = strings[i_6];
	                strings[i_6] = string_10;
	                int i_11 = ints[i_9];
	                ints[i_9] = ints[i_6];
	                ints[i_6++] = i_11;
	            }
	        }
	        strings[toIndex] = strings[i_6];
	        strings[i_6] = string_7;
	        ints[toIndex] = ints[i_6];
	        ints[i_6] = i_8;
	        quicksort(strings, ints, fromIndex, i_6 - 1);
	        quicksort(strings, ints, i_6 + 1, toIndex);
	    }
	}

	public static void quicksort(int[] ints_0, int[] ints_1, int i_2, int i_3) {
	    if (i_2 < i_3) {
	        int i_5 = (i_3 + i_2) / 2;
	        int i_6 = i_2;
	        int i_7 = ints_0[i_5];
	        ints_0[i_5] = ints_0[i_3];
	        ints_0[i_3] = i_7;
	        int i_8 = ints_1[i_5];
	        ints_1[i_5] = ints_1[i_3];
	        ints_1[i_3] = i_8;
	        int i_9 = i_7 == Integer.MAX_VALUE ? 0 : 1;
	        for (int i_10 = i_2; i_10 < i_3; i_10++) {
	            if (ints_0[i_10] < (i_10 & i_9) + i_7) {
	                int i_11 = ints_0[i_10];
	                ints_0[i_10] = ints_0[i_6];
	                ints_0[i_6] = i_11;
	                int i_12 = ints_1[i_10];
	                ints_1[i_10] = ints_1[i_6];
	                ints_1[i_6++] = i_12;
	            }
	        }
	        ints_0[i_3] = ints_0[i_6];
	        ints_0[i_6] = i_7;
	        ints_1[i_3] = ints_1[i_6];
	        ints_1[i_6] = i_8;
	        quicksort(ints_0, ints_1, i_2, i_6 - 1);
	        quicksort(ints_0, ints_1, i_6 + 1, i_3);
	    }
	}

	public static byte[] generateRandomlySeededBytearray(int seed) {
	    CacheableNodeByteArray bytearrayNode = (CacheableNodeByteArray) ArrayUtils.BYTEARRAY_NODES.get(seed);
	    if (bytearrayNode == null) {
	        byte[] block = new byte[512];
	        Random random = new Random(seed);
	        for (int index = 0; index < 255; index++) {
	            block[index] = (byte) index;
	        }
	        for (int index = 0; index < 255; index++) {
	            int i_6 = 255 - index;
	            int i_7 = StaticMethods.method7931(random, i_6);
	            byte temp = block[i_7];
	            block[i_7] = block[i_6];
	            block[i_6] = block[511 - index] = temp;
	        }
	        bytearrayNode = new CacheableNodeByteArray(block);
	        ArrayUtils.BYTEARRAY_NODES.put(bytearrayNode, seed);
	    }
	    return bytearrayNode.data;
	}

	public static final CacheableNodeQueue BYTEARRAY_NODES = new CacheableNodeQueue(16);

	public static void quicksortt(String[] strings, int[] ints) {
	    quicksort(strings, ints, 0, strings.length - 1);
	}

	public static void method3751(int i_0, int i_1, int i_2, int i_3, int i_4) {
	    int i_6 = 0;
	    int i_7 = i_3;
	    int i_8 = i_2 * i_2;
	    int i_9 = i_3 * i_3;
	    int i_10 = i_9 << 1;
	    int i_11 = i_8 << 1;
	    int i_12 = i_3 << 1;
	    int i_13 = i_10 + (1 - i_12) * i_8;
	    int i_14 = i_9 - i_11 * (i_12 - 1);
	    int i_15 = i_8 << 2;
	    int i_16 = i_9 << 2;
	    int i_17 = ((i_6 << 1) + 3) * i_10;
	    int i_18 = ((i_3 << 1) - 3) * i_11;
	    int i_19 = i_16 * (i_6 + 1);
	    int i_20 = i_15 * (i_3 - 1);
	    setArrayValuesTo(Class149.anIntArrayArray7072[i_1], i_0 - i_2, i_0 + i_2, i_4);
	    while (i_7 > 0) {
	        if (i_13 < 0) {
	            while (i_13 < 0) {
	                i_13 += i_17;
	                i_14 += i_19;
	                i_17 += i_16;
	                i_19 += i_16;
	                ++i_6;
	            }
	        }
	        if (i_14 < 0) {
	            i_13 += i_17;
	            i_14 += i_19;
	            i_17 += i_16;
	            i_19 += i_16;
	            ++i_6;
	        }
	        i_13 += -i_20;
	        i_14 += -i_18;
	        i_18 -= i_15;
	        i_20 -= i_15;
	        --i_7;
	        int i_21 = i_1 - i_7;
	        int i_22 = i_7 + i_1;
	        int i_23 = i_0 + i_6;
	        int i_24 = i_0 - i_6;
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_21], i_24, i_23, i_4);
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_22], i_24, i_23, i_4);
	    }
	}

	public static void quicksort(int[] dimensions, Object[] objects) {
	    quicksort(dimensions, objects, 0, dimensions.length - 1);
	}

	public static void method7170(int i_0) {
	    if (StaticMethods.anIntArray36 == null || StaticMethods.anIntArray36.length < i_0) {
	        StaticMethods.anIntArray36 = new int[i_0];
	    }
	}

	public static void method14584(int i_0, int i_1, int i_2, int i_3, int i_4) {
	    if (i_3 == i_2) {
	        StaticMethods.method6292(i_0, i_1, i_2, i_4);
	    } else if (i_0 - i_2 >= TextureOpRasterizer.anInt7071 && i_0 + i_2 <= TextureOpRasterizer.anInt7069 && i_1 - i_3 >= TextureOpRasterizer.anInt7070 && i_3 + i_1 <= TextureOpRasterizer.anInt7068) {
	        method3751(i_0, i_1, i_2, i_3, i_4);
	    } else {
	        method15405(i_0, i_1, i_2, i_3, i_4);
	    }
	
	}

	public static void method3230(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
	    int i_8 = i_2 + i_6;
	    int i_9 = i_3 - i_6;
	    int i_10;
	    for (i_10 = i_2; i_10 < i_8; i_10++) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_10], i_0, i_1, i_5);
	    }
	    for (i_10 = i_3; i_10 > i_9; --i_10) {
	        setArrayValuesTo(Class149.anIntArrayArray7072[i_10], i_0, i_1, i_5);
	    }
	    i_10 = i_0 + i_6;
	    int i_11 = i_1 - i_6;
	    for (int i_12 = i_8; i_12 <= i_9; i_12++) {
	        int[] ints_13 = Class149.anIntArrayArray7072[i_12];
	        setArrayValuesTo(ints_13, i_0, i_10, i_5);
	        setArrayValuesTo(ints_13, i_10, i_11, i_4);
	        setArrayValuesTo(ints_13, i_11, i_1, i_5);
	    }
	}
}
