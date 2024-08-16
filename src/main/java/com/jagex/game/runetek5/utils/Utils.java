package com.jagex.game.runetek5.utils;

public class Utils {

    public static long aLong993;
    public static long aLong994;

    public static char cp1252ToChar(byte b) {
        int val = b & 0xff;
        if (val == 0) {
            throw new IllegalArgumentException(Integer.toString(0, 16));
        } else {
            if (val >= 128 && val < 160) {
                char charValue = Utils.UNICODE_TABLE[val - 128];
                if (charValue == 0) {
                    charValue = 63;
                }
                val = charValue;
            }
            return (char) val;
        }
    }

    public static String readString(byte[] bytes_0, int i_1, int i_2) {
        char[] arr_4 = new char[i_2];
        int i_5 = 0;
        for (int i_6 = 0; i_6 < i_2; i_6++) {
            int i_7 = bytes_0[i_6 + i_1] & 0xff;
            if (i_7 != 0) {
                if (i_7 >= 128 && i_7 < 160) {
                    char var_8 = Utils.UNICODE_TABLE[i_7 - 128];
                    if (var_8 == 0) {
                        var_8 = 63;
                    }
                    i_7 = var_8;
                }
                arr_4[i_5++] = (char) i_7;
            }
        }
        return new String(arr_4, 0, i_5);
    }

    public static int stringLengthPlus2(String string_0) {
        return string_0.length() + 2;
    }

    public static int calculateKerning(byte[][] glyphWidths, byte[][] glyphHeights, int[] glyphX, byte[] kerning, int[] glyphY, int start, int end) {
        int x1 = glyphX[start];
        int x2 = x1 + glyphY[start];
        int x3 = glyphX[end];
        int x4 = x3 + glyphY[end];
        int minX = x1;
        if (x3 > x1) {
            minX = x3;
        }
        int maxX = x2;
        if (x4 < x2) {
            maxX = x4;
        }
        int minKerning = kerning[start] & 0xff;
        if ((kerning[end] & 0xff) < minKerning) {
            minKerning = kerning[end] & 0xff;
        }
        byte[] heights = glyphHeights[start];
        byte[] widths = glyphWidths[end];
        int offX = minX - x1;
        int offY = minX - x3;
        for (int i = minX; i < maxX; i++) {
            int i_20 = heights[offX++] + widths[offY++];
            if (i_20 < minKerning) {
                minKerning = i_20;
            }
        }
        return -minKerning;
    }

    public static synchronized long time() {
        long long_1 = System.currentTimeMillis();
        if (long_1 < aLong993) {
            aLong994 += aLong993 - long_1;
        }
        aLong993 = long_1;
        return long_1 + aLong994;
    }

    public static int parseInt(CharSequence charsequence_0) {
        return parseInt(charsequence_0, 10);
    }

    public static int parseInt(CharSequence charsequence_0, int i_1) {
        if (i_1 >= 2 && i_1 <= 36) {
            boolean bool4 = false;
            boolean bool5 = false;
            int i_6 = 0;
            int i_7 = charsequence_0.length();
            for (int i_8 = 0; i_8 < i_7; i_8++) {
                char var_9 = charsequence_0.charAt(i_8);
                if (i_8 == 0) {
                    if (var_9 == 45) {
                        bool4 = true;
                        continue;
                    }
                    if (var_9 == 43) {
                        continue;
                    }
                }
                int i_11;
                if (var_9 >= 48 && var_9 <= 57) {
                    i_11 = var_9 - 48;
                } else if (var_9 >= 65 && var_9 <= 90) {
                    i_11 = var_9 - 55;
                } else {
                    if (var_9 < 97 || var_9 > 122) {
                        throw new NumberFormatException();
                    }
                    i_11 = var_9 - 87;
                }
                if (i_11 >= i_1) {
                    throw new NumberFormatException();
                }
                if (bool4) {
                    i_11 = -i_11;
                }
                int i_10 = i_11 + i_6 * i_1;
                if (i_10 / i_1 != i_6) {
                    throw new NumberFormatException();
                }
                i_6 = i_10;
                bool5 = true;
            }
            if (!bool5) {
                throw new NumberFormatException();
            } else {
                return i_6;
            }
        } else {
            throw new IllegalArgumentException("" + i_1);
        }
    }

    public static String getStringFromLong(long long_0) {
        if (long_0 > 0L && long_0 < 6582952005840035281L) {
            if (long_0 % 37L == 0L) {
                return null;
            } else {
                int i_2 = 0;
                for (long long_3 = long_0; long_3 != 0L; long_3 /= 37L) {
                    ++i_2;
                }
                StringBuilder stringbuilder6 = new StringBuilder(i_2);
                while (long_0 != 0L) {
                    long long_4 = long_0;
                    long_0 /= 37L;
                    stringbuilder6.append(Utils.aCharArray5825[(int) (long_4 - 37L * long_0)]);
                }
                return stringbuilder6.reverse().toString();
            }
        } else {
            return null;
        }
    }

    public static int method2565(CharSequence charsequence_0, char var_1) {
        int i_3 = 0;
        int i_4 = charsequence_0.length();
        for (int i_5 = 0; i_5 < i_4; i_5++) {
            if (charsequence_0.charAt(i_5) == var_1) {
                ++i_3;
            }
        }
        return i_3;
    }

    public static long hashAsLong(CharSequence charsequence_0) {
        int i_2 = charsequence_0.length();
        long long_3 = 0L;
        for (int i_5 = 0; i_5 < i_2; i_5++) {
            long_3 = (long_3 << 5) - long_3 + charsequence_0.charAt(i_5);
        }
        return long_3;
    }

    public static int stringToInt(CharSequence charsequence_0) {
        int i_2 = charsequence_0.length();
        int i_3 = 0;
        for (int i_4 = 0; i_4 < i_2; i_4++) {
            i_3 = (i_3 << 5) - i_3 + charsequence_0.charAt(i_4);
        }
        return i_3;
    }

    public static boolean method4841(CharSequence charsequence_0) {
        boolean bool4 = false;
        boolean bool5 = false;
        int i_6 = 0;
        int i_7 = charsequence_0.length();
        for (int i_8 = 0; i_8 < i_7; i_8++) {
            char var_9 = charsequence_0.charAt(i_8);
            if (i_8 == 0) {
                if (var_9 == 45) {
                    bool4 = true;
                    continue;
                }
                if (var_9 == 43) {
                    continue;
                }
            }
            int i_11;
            if (var_9 >= 48 && var_9 <= 57) {
                i_11 = var_9 - 48;
            } else if (var_9 >= 65 && var_9 <= 90) {
                i_11 = var_9 - 55;
            } else {
                if (var_9 < 97 || var_9 > 122) {
                    return false;
                }
                i_11 = var_9 - 87;
            }
            if (i_11 >= 10) {
                return false;
            }
            if (bool4) {
                i_11 = -i_11;
            }
            int i_10 = i_11 + i_6 * 10;
            if (i_10 / 10 != i_6) {
                return false;
            }
            i_6 = i_10;
            bool5 = true;
        }
        return bool5;
    }

    public static long charseqTouid(CharSequence charsequence_0) {
        long long_2 = 0L;
        int i_4 = charsequence_0.length();
        for (int i_5 = 0; i_5 < i_4; i_5++) {
            long_2 *= 37L;
            char var_6 = charsequence_0.charAt(i_5);
            if (var_6 >= 65 && var_6 <= 90) {
                long_2 += var_6 + 1 - 65;
            } else if (var_6 >= 97 && var_6 <= 122) {
                long_2 += var_6 + 1 - 97;
            } else if (var_6 >= 48 && var_6 <= 57) {
                long_2 += var_6 + 27 - 48;
            }
            if (long_2 >= 177917621779460413L) {
                break;
            }
        }
        while (long_2 % 37L == 0L && long_2 != 0L) {
            long_2 /= 37L;
        }
        return long_2;
    }

    public static char method7778(char var_0) {
        return var_0 == 198 ? 'E' : (var_0 == 230 ? 'e' : (var_0 == 223 ? 's' : (var_0 == 338 ? 'E' : (var_0 == 339 ? 'e' : '\u0000'))));
    }

    public static String formatRequestParameter(CharSequence charsequence_0) {
        int i_2 = charsequence_0.length();
        StringBuilder stringbuilder3 = new StringBuilder(i_2);
        for (int i_4 = 0; i_4 < i_2; i_4++) {
            char var_5 = charsequence_0.charAt(i_4);
            if ((var_5 < 97 || var_5 > 122) && (var_5 < 65 || var_5 > 90) && (var_5 < 48 || var_5 > 57) && var_5 != 46 && var_5 != 45 && var_5 != 42 && var_5 != 95) {
                if (var_5 == 32) {
                    stringbuilder3.append('+');
                } else {
                    byte b_6 = Utils.byteForChar(var_5);
                    stringbuilder3.append('%');
                    int i_7 = b_6 >> 4 & 0xf;
                    if (i_7 >= 10) {
                        stringbuilder3.append((char) (i_7 + 55));
                    } else {
                        stringbuilder3.append((char) (i_7 + 48));
                    }
                    i_7 = b_6 & 0xf;
                    if (i_7 >= 10) {
                        stringbuilder3.append((char) (i_7 + 55));
                    } else {
                        stringbuilder3.append((char) (i_7 + 48));
                    }
                }
            } else {
                stringbuilder3.append(var_5);
            }
        }
        return stringbuilder3.toString();
    }

    public static int method13422(char var_0) {
        int i_3 = var_0 << 4;
        if (Character.isUpperCase(var_0) || Character.isTitleCase(var_0)) {
            char var_4 = Character.toLowerCase(var_0);
            i_3 = (var_4 << 4) + 1;
        }

        return i_3;
    }

    public static String decodeWithUnicodeTable(byte[] bytes_0) {
        return readString(bytes_0, 0, bytes_0.length);
    }

    public static String method6398(Object[] arr_0, int i_1, int i_2) {
        if (i_2 == 0) {
            return "";
        } else if (i_2 == 1) {
            CharSequence charsequence_4 = (CharSequence) arr_0[i_1];
            return charsequence_4 == null ? "null" : charsequence_4.toString();
        } else {
            int i_9 = i_2 + i_1;
            int i_5 = 0;
            for (int i_6 = i_1; i_6 < i_9; i_6++) {
                CharSequence charsequence_11 = (CharSequence) arr_0[i_6];
                if (charsequence_11 == null) {
                    i_5 += 4;
                } else {
                    i_5 += charsequence_11.length();
                }
            }
            StringBuilder stringbuilder10 = new StringBuilder(i_5);
            for (int i_7 = i_1; i_7 < i_9; i_7++) {
                CharSequence charsequence_8 = (CharSequence) arr_0[i_7];
                if (charsequence_8 == null) {
                    stringbuilder10.append("null");
                } else {
                    stringbuilder10.append(charsequence_8);
                }
            }
            return stringbuilder10.toString();
        }
    }

    public static String[] split(String string_0, char var_1) {
	    int i_3 = method2565(string_0, var_1);
	    String[] arr_4 = new String[i_3 + 1];
	    int i_5 = 0;
	    int i_6 = 0;
	
	    for (int i_7 = 0; i_7 < i_3; i_7++) {
	        int i_8;
	        for (i_8 = i_6; string_0.charAt(i_8) != var_1; i_8++) {
	        }
	
	        arr_4[i_5++] = string_0.substring(i_6, i_8);
	        i_6 = i_8 + 1;
	    }
	
	    arr_4[i_3] = string_0.substring(i_6);
	    return arr_4;
	}

	public static byte byteForChar(char c) {
	    byte res;
	    if ((c <= 0 || c >= 128) && (c < 160 || c > 255)) {
	        if (c == 8364) {
	            res = -128;
	        } else if (c == 8218) {
	            res = -126;
	        } else if (c == 402) {
	            res = -125;
	        } else if (c == 8222) {
	            res = -124;
	        } else if (c == 8230) {
	            res = -123;
	        } else if (c == 8224) {
	            res = -122;
	        } else if (c == 8225) {
	            res = -121;
	        } else if (c == 710) {
	            res = -120;
	        } else if (c == 8240) {
	            res = -119;
	        } else if (c == 352) {
	            res = -118;
	        } else if (c == 8249) {
	            res = -117;
	        } else if (c == 338) {
	            res = -116;
	        } else if (c == 381) {
	            res = -114;
	        } else if (c == 8216) {
	            res = -111;
	        } else if (c == 8217) {
	            res = -110;
	        } else if (c == 8220) {
	            res = -109;
	        } else if (c == 8221) {
	            res = -108;
	        } else if (c == 8226) {
	            res = -107;
	        } else if (c == 8211) {
	            res = -106;
	        } else if (c == 8212) {
	            res = -105;
	        } else if (c == 732) {
	            res = -104;
	        } else if (c == 8482) {
	            res = -103;
	        } else if (c == 353) {
	            res = -102;
	        } else if (c == 8250) {
	            res = -101;
	        } else if (c == 339) {
	            res = -100;
	        } else if (c == 382) {
	            res = -98;
	        } else if (c == 376) {
	            res = -97;
	        } else {
	            res = 63;
	        }
	    } else {
	        res = (byte) c;
	    }
	    return res;
	}

	/**
	 * @deprecated Use {@link #isWrongJava(CharSequence)} instead
	 */
	public static boolean method1950(CharSequence charsequence_0) {
		return isWrongJava(charsequence_0);
	}

	public static boolean isWrongJava(CharSequence charsequence_0) {
	    return method4841(charsequence_0);
	}

	public static byte[] characterSequenceToBytearray(CharSequence charsequence_0) {
	    int i_2 = charsequence_0.length();
	    byte[] bytes_3 = new byte[i_2];
	    for (int i_4 = 0; i_4 < i_2; i_4++) {
	        char var_5 = charsequence_0.charAt(i_4);
	        if (var_5 > 0 && var_5 < 128 || var_5 >= 160 && var_5 <= 255) {
	            bytes_3[i_4] = (byte) var_5;
	        } else if (var_5 == 8364) {
	            bytes_3[i_4] = -128;
	        } else if (var_5 == 8218) {
	            bytes_3[i_4] = -126;
	        } else if (var_5 == 402) {
	            bytes_3[i_4] = -125;
	        } else if (var_5 == 8222) {
	            bytes_3[i_4] = -124;
	        } else if (var_5 == 8230) {
	            bytes_3[i_4] = -123;
	        } else if (var_5 == 8224) {
	            bytes_3[i_4] = -122;
	        } else if (var_5 == 8225) {
	            bytes_3[i_4] = -121;
	        } else if (var_5 == 710) {
	            bytes_3[i_4] = -120;
	        } else if (var_5 == 8240) {
	            bytes_3[i_4] = -119;
	        } else if (var_5 == 352) {
	            bytes_3[i_4] = -118;
	        } else if (var_5 == 8249) {
	            bytes_3[i_4] = -117;
	        } else if (var_5 == 338) {
	            bytes_3[i_4] = -116;
	        } else if (var_5 == 381) {
	            bytes_3[i_4] = -114;
	        } else if (var_5 == 8216) {
	            bytes_3[i_4] = -111;
	        } else if (var_5 == 8217) {
	            bytes_3[i_4] = -110;
	        } else if (var_5 == 8220) {
	            bytes_3[i_4] = -109;
	        } else if (var_5 == 8221) {
	            bytes_3[i_4] = -108;
	        } else if (var_5 == 8226) {
	            bytes_3[i_4] = -107;
	        } else if (var_5 == 8211) {
	            bytes_3[i_4] = -106;
	        } else if (var_5 == 8212) {
	            bytes_3[i_4] = -105;
	        } else if (var_5 == 732) {
	            bytes_3[i_4] = -104;
	        } else if (var_5 == 8482) {
	            bytes_3[i_4] = -103;
	        } else if (var_5 == 353) {
	            bytes_3[i_4] = -102;
	        } else if (var_5 == 8250) {
	            bytes_3[i_4] = -101;
	        } else if (var_5 == 339) {
	            bytes_3[i_4] = -100;
	        } else if (var_5 == 382) {
	            bytes_3[i_4] = -98;
	        } else if (var_5 == 376) {
	            bytes_3[i_4] = -97;
	        } else {
	            bytes_3[i_4] = 63;
	        }
	    }
	    return bytes_3;
	}

	public static void sortAlphabetically(String[] names, short[] ids) {
	    ArrayUtils.quicksort(names, ids, 0, names.length - 1);
	}

	public static String formatScript(String fullProgramString, char carriageReturn, String string_2) {
	    int fullLength = fullProgramString.length();
	    int i_5 = string_2.length();
	    int i_6 = fullLength;
	    int i_7 = i_5 - 1;
	    if (i_7 != 0) {
	        int i_8 = 0;
	        while (true) {
	            i_8 = fullProgramString.indexOf(carriageReturn, i_8);
	            if (i_8 < 0) {
	                break;
	            }
	            ++i_8;
	            i_6 += i_7;
	        }
	    }
	    StringBuilder stringbuilder11 = new StringBuilder(i_6);
	    int i_9 = 0;
	    while (true) {
	        int i_10 = fullProgramString.indexOf(carriageReturn, i_9);
	        if (i_10 < 0) {
	            stringbuilder11.append(fullProgramString.substring(i_9));
	            return stringbuilder11.toString();
	        }
	        stringbuilder11.append(fullProgramString, i_9, i_10);
	        stringbuilder11.append(string_2);
	        i_9 = i_10 + 1;
	    }
	}

	public static final char[] UNICODE_TABLE = {'\u20ac', '\0', '\u201a', '\u0192', '\u201e', '\u2026', '\u2020', '\u2021', '\u02c6', '\u2030', '\u0160', '\u2039', '\u0152', '\0', '\u017d', '\0', '\0', '\u2018', '\u2019', '\u201c', '\u201d', '\u2022', '\u2013', '\u2014', '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\0', '\u017e', '\u0178'};

	public static boolean belowZero(int integer) {
	    return integer < 0;
	}

	public static boolean equalOrAboveZero(int integer) {
	    return integer >= 0;
	}

	public static final char[] aCharArray5825 = {'_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

	public static int parse_int(CharSequence charsequence_0) {
	    return parseInt(charsequence_0, 16);
	}

	public static boolean isNumber(char character) {
	    return character >= 48 && character <= 57;
	}
}
