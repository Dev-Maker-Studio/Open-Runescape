package com.jagex.game.runetek5.utils;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.inputsubscriber.NameType;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.tools.LocalizedText;

public class TextUtils {

	// [ , �, _, -, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �, �]
	public static final char[] ALLOWED_SPECIAL_CHARS = {' ', '\u00a0', '_', '-', '\u00e0', '\u00e1', '\u00e2', '\u00e4', '\u00e3', '\u00c0', '\u00c1', '\u00c2', '\u00c4', '\u00c3', '\u00e8', '\u00e9', '\u00ea', '\u00eb', '\u00c8', '\u00c9', '\u00ca', '\u00cb', '\u00ed', '\u00ee', '\u00ef', '\u00cd', '\u00ce', '\u00cf', '\u00f2', '\u00f3', '\u00f4', '\u00f6', '\u00f5', '\u00d2', '\u00d3', '\u00d4', '\u00d6', '\u00d5', '\u00f9', '\u00fa', '\u00fb', '\u00fc', '\u00d9', '\u00da', '\u00db', '\u00dc', '\u00e7', '\u00c7', '\u00ff', '\u0178', '\u00f1', '\u00d1', '\u00df'};
	public static final char[] OTHER_ALLOWED_CHARS = {'[', ']', '#'};
	
	
	public static String createNumberCenteredWithinText(int number, boolean dynamicRadix) {
	    return dynamicRadix && number >= 0 ? TextUtils.centerIntegerInString(number, true) : Integer.toString(number);
	}

	public static String centerIntegerInString(int number, boolean dynamicRadix) {
	    if (dynamicRadix && number >= 0) {
	        int characterCount = 2;
	        for (int i_5 = number / 10; i_5 != 0; characterCount++) {
	            i_5 /= 10;
	        }
	        char[] characters = new char[characterCount];
	        characters[0] = 43;
	        for (int countDescending = characterCount - 1; countDescending > 0; --countDescending) {
	            int cachedNumber = number;
	            number /= 10;
	            int i_9 = cachedNumber - number * 10;
	            if (i_9 >= 10) {
	                characters[countDescending] = (char) (i_9 + 87);
	            } else {
	                characters[countDescending] = (char) (i_9 + 48);
	            }
	        }
	        return new String(characters);
	    } else {
	        return Integer.toString(number, 10);
	    }
	}

	public static boolean matches(String findRaw, String findFormatted, String currentRaw, String currentFormatted, byte b_4) {
	    if (findRaw != null) {
	        if (currentRaw != null) {
	            if (!findRaw.startsWith("#")) {
	                if (b_4 >= -1) {
	                    throw new IllegalStateException();
	                }
	                if (!currentRaw.startsWith("#")) {
	                    return findFormatted.equals(currentFormatted);
	                }
	            }
	            return findRaw.equals(currentRaw);
	        }
	        if (b_4 >= -1) {
	            throw new IllegalStateException();
	        }
	    }
	    return false;
	}

	public static String decodeUidName(long long_0) {
	    if (long_0 > 0L && long_0 < 6582952005840035281L) {
	        if (long_0 % 37L == 0L) {
	            return null;
	        } else {
	            int i_2 = 0;
	            for (long long_3 = long_0; long_3 != 0L; long_3 /= 37L) {
	                ++i_2;
	            }
	            char var_6;
	            StringBuilder stringBuilder;
	            for (stringBuilder = new StringBuilder(i_2); long_0 != 0L; stringBuilder.append(var_6)) {
	                long long_4 = long_0;
	                long_0 /= 37L;
	                var_6 = Utils.aCharArray5825[(int) (long_4 - 37L * long_0)];
	                if (var_6 == 95) {
	                    int i_7 = stringBuilder.length() - 1;
	                    stringBuilder.setCharAt(i_7, Character.toUpperCase(stringBuilder.charAt(i_7)));
	                    var_6 = 160;
	                }
	            }
	            stringBuilder.reverse();
	            stringBuilder.setCharAt(0, Character.toUpperCase(stringBuilder.charAt(0)));
	            return stringBuilder.toString();
	        }
	    } else {
	        return null;
	    }
	}

	public static char method2599(char character, Language language) {
	    if (character >= 192 && character <= 255) {
	        if (character <= 198) {
	            return 'A';
	        }
	        if (character == 199) {
	            return 'C';
	        }
	        if (character <= 203) {
	            return 'E';
	        }
	        if (character <= 207) {
	            return 'I';
	        }
	        if (character == 209 && language != Language.SPANISH) {
	            return 'N';
	        }
	        if (character >= 210 && character <= 214) {
	            return 'O';
	        }
	        if (character >= 217 && character <= 220) {
	            return 'U';
	        }
	        if (character == 221) {
	            return 'Y';
	        }
	        if (character == 223) {
	            return 's';
	        }
	        if (character >= 224 && character <= 230) {
	            return 'a';
	        }
	        if (character == 231) {
	            return 'c';
	        }
	        if (character >= 232 && character <= 235) {
	            return 'e';
	        }
	        if (character >= 236 && character <= 239) {
	            return 'i';
	        }
	        if (character == 241 && language != Language.SPANISH) {
	            return 'n';
	        }
	        if (character >= 242 && character <= 246) {
	            return 'o';
	        }
	        if (character >= 249 && character <= 252) {
	            return 'u';
	        }
	        if (character == 253 || character == 255) {
	            return 'y';
	        }
	    }
	    return character == 338 ? 'O' : (character == 339 ? 'o' : (character == 376 ? 'Y' : character));
	}

	public static int method7557(CharSequence str1, CharSequence str2, Language language) {
	    int i_4 = str1.length();
	    int i_5 = str2.length();
	    int i_6 = 0;
	    int i_7 = 0;
	    char var_8 = 0;
	    char var_9 = 0;
	    while (i_6 - var_8 < i_4 || i_7 - var_9 < i_5) {
	        if (i_6 - var_8 >= i_4) {
	            return -1;
	        }
	        if (i_7 - var_9 >= i_5) {
	            return 1;
	        }
	        char var_10;
	        if (var_8 != 0) {
	            var_10 = var_8;
	        } else {
	            var_10 = str1.charAt(i_6++);
	        }
	        char var_15;
	        if (var_9 != 0) {
	            var_15 = var_9;
	        } else {
	            var_15 = str2.charAt(i_7++);
	        }
	        var_8 = Utils.method7778(var_10);
	        var_9 = Utils.method7778(var_15);
	        var_10 = method2599(var_10, language);
	        var_15 = method2599(var_15, language);
	        if (var_10 != var_15 && Character.toUpperCase(var_10) != Character.toUpperCase(var_15)) {
	            var_10 = Character.toLowerCase(var_10);
	            var_15 = Character.toLowerCase(var_15);
	            if (var_15 != var_10) {
	                return Utils.method13422(var_10) - Utils.method13422(var_15);
	            }
	        }
	    }
	    int i_16 = Math.min(i_4, i_5);
	    char var_13;
	    int i_18;
	    for (i_18 = 0; i_18 < i_16; i_18++) {
	        if (language == Language.FRENCH) {
	            i_6 = i_4 - 1 - i_18;
	            i_7 = i_5 - 1 - i_18;
	        } else {
	            i_7 = i_18;
	            i_6 = i_18;
	        }
	        char var_17 = str1.charAt(i_6);
	        var_13 = str2.charAt(i_7);
	        if (var_13 != var_17 && Character.toUpperCase(var_17) != Character.toUpperCase(var_13)) {
	            var_17 = Character.toLowerCase(var_17);
	            var_13 = Character.toLowerCase(var_13);
	            if (var_17 != var_13) {
	                return Utils.method13422(var_17) - Utils.method13422(var_13);
	            }
	        }
	    }
	    i_18 = i_4 - i_5;
	    if (i_18 != 0) {
	        return i_18;
	    } else {
	        for (int i_19 = 0; i_19 < i_16; i_19++) {
	            var_13 = str1.charAt(i_19);
	            char var_14 = str2.charAt(i_19);
	            if (var_13 != var_14) {
	                return Utils.method13422(var_13) - Utils.method13422(var_14);
	            }
	        }
	        return 0;
	    }
	}

	/**
	 * [/, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 
	 * A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, 
	 * a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y]
	 */
	public static boolean isAlphabeticalOrNumeric(char character) {
	    return character >= 48 && character <= 57 || character >= 65 && character <= 90 || character >= 97 && character <= 122;
	}

	public static String formatDisplayname(CharSequence charSequence) {
	    return TextUtils.format(charSequence);
	}

	/**
	 * [�,  , _, -]
	 */
	public static boolean isSpaceCharacter(char character) {
	    return character == 160 || character == 32 || character == 95 || character == 45;
	}

	public static boolean allowedCharacter(char character) {
	    if (Character.isISOControl(character)) {
	        return false;
	    } 
	    if (isAlphabeticalOrNumeric(character)) {
	        return true;
	    } 
	    char[] special = ALLOWED_SPECIAL_CHARS;
	    for (int index = 0; index < special.length; index++) {
	        char specialChar = special[index];
	        if (character == specialChar) {
	            return true;
	        }
	    }
	    char[] other = OTHER_ALLOWED_CHARS;
	    for (int index = 0; index < other.length; index++) {
	        char otherChar = other[index];
	        if (character == otherChar) {
	            return true;
	        }
	    }
	    return false;
	}

	public static String format(CharSequence charSequence) {
	    if (charSequence == null) {
	        return null;
	    } 
	    int start = 0;
	    int end;
	    for (end = charSequence.length(); start < end && isSpaceCharacter(charSequence.charAt(start)); start++) {
	    }
	    while (end > start && isSpaceCharacter(charSequence.charAt(end - 1))) {
	        --end;
	    }
	    int length = end - start;
	    if (length >= 1 && length <= getMaxLength(null)) {
	        StringBuilder sb = new StringBuilder(length);
	        for (int index = start; index < end; index++) {
	            char character = charSequence.charAt(index);
	            if (allowedCharacter(character)) {
	                char formatted = TextUtils.normalizeCharacter(character);
	                if (formatted != 0) {
	                    sb.append(formatted);
	                }
	            }
	        }
	        if (sb.length() == 0) {
	            return null;
	        } 
	        return sb.toString();           
	    } 
	    return null;
	}

	public static int getMaxLength(NameType nameType) {
	    if (nameType == null) {
	        return 12;
	    } else {
	        switch (nameType.type) {
	            case 5:
	                return 20;
	            default:
	                return 12;
	        }
	    }
	}

	public static boolean method3600(byte b_0) {
	    int i_2 = b_0 & 0xff;
	    return i_2 != 0 && (i_2 < 128 || i_2 >= 160 || Utils.UNICODE_TABLE[i_2 - 128] != 0);
	}

	/**
	 * Decodes a URL-encoded string and returns the decoded string.
	 *
	 * @param encodedUrl the URL-encoded string to decode
	 * @return the decoded string
	 */
	public static String decodeUrl(String encodedUrl) {
	    StringBuilder sb = new StringBuilder();
	    int length = encodedUrl.length();
	    for (int charIndex = 0; charIndex < length; charIndex++) {
	        char character = encodedUrl.charAt(charIndex);
	        if (character == 37 && length > charIndex + 2) {
	            character = encodedUrl.charAt(charIndex + 1);
	            int cp1252;
	            if (character >= 48 && character <= 57) {
	                cp1252 = character - 48;
	            } else if (character >= 97 && character <= 102) {
	                cp1252 = character + 10 - 97;
	            } else {
	                if (character < 65 || character > 70) {
	                    sb.append('%');
	                    continue;
	                }
	                cp1252 = character + 10 - 65;
	            }
	            cp1252 *= 16;
	            char nextChar = encodedUrl.charAt(charIndex + 2);
	            if (nextChar >= 48 && nextChar <= 57) {
	                cp1252 += nextChar - 48;
	            } else if (nextChar >= 97 && nextChar <= 102) {
	                cp1252 += nextChar + 10 - 97;
	            } else {
	                if (nextChar < 65 || nextChar > 70) {
	                    sb.append('%');
	                    continue;
	                }
	                cp1252 += nextChar + 10 - 65;
	            }
	            if (cp1252 != 0 && method3600((byte) cp1252)) {
	                sb.append(Utils.cp1252ToChar((byte) cp1252));
	            }
	            charIndex += 2;
	        } else if (character == 43) {
	            sb.append(' ');
	        } else {
	            sb.append(character);
	        }
	    }
	    return sb.toString();
	}

	public static int getLength(String text) {
	    return text.length() + 1;
	}

	public static String setTextColor(int col) {
	    return "<col=" + Integer.toHexString(col) + ">";
	}

	public static String calculateCombatBasedColor(int i_0, int i_1) {
	    int i_3 = i_1 - i_0;
	    return i_3 < -9 ? setTextColor(16711680) :
	    	(i_3 < -6 ? setTextColor(16723968) :
	    		(i_3 < -3 ? setTextColor(16740352) :
	    			(i_3 < 0 ? setTextColor(16756736) :
	    			(i_3 > 9 ? setTextColor(65280) :
	    				(i_3 > 6 ? setTextColor(4259584) :
	    				(i_3 > 3 ? setTextColor(8453888) :
	    					(i_3 > 0 ? setTextColor(12648192) :
	    					setTextColor(16776960))))))));
	}

	public static String getCrown(int image) {
	    return "<img=" + image + ">";
	}

	public static String atLtGtTags(String text) {
	    int length = text.length();
	    int extraLength = 0;
	    for (int index = 0; index < length; index++) {
	        char chr = text.charAt(index);
	        if (chr == 60 || chr == 62) {
	            extraLength += 3;
	        }
	    }
	    StringBuilder sb = new StringBuilder(extraLength + length);
	    for (int index = 0; index < length; index++) {
	        char chr = text.charAt(index);
	        if (chr == 60) {
	            sb.append("<lt>");
	        } else if (chr == 62) {
	            sb.append("<gt>");
	        } else {
	            sb.append(chr);
	        }
	    }
	    return sb.toString();
	}

	public static String formatStackSize(int stackSize) {
	    String unformatted = Integer.toString(stackSize);
	    for (int charIndex = unformatted.length() - 3; charIndex > 0; charIndex -= 3) {
	        unformatted = unformatted.substring(0, charIndex) + "," + unformatted.substring(charIndex);
	    }
	    return unformatted.length() > 9 ? " " + 
	    	setTextColor(65408) + unformatted.substring(0, unformatted.length() - 8) +
	    		LocalizedText.M.translate(client.CURRENT_LANGUAGE) + " (" + unformatted + ")" + "</col>" 
	    	: (unformatted.length() > 6 ? " " + setTextColor(16777215) + unformatted.substring(0, unformatted.length() - 4) +
	    		LocalizedText.K.translate(client.CURRENT_LANGUAGE) + " (" + unformatted + ")" + "</col>" 
	    	: " " + setTextColor(16776960) + unformatted + "</col>");
	}

	public static char normalizeCharacter(char c) {
	    switch (c) {
	        case '\u00d2':
	        case '\u00d3':
	        case '\u00d4':
	        case '\u00d5':
	        case '\u00d6':
	        case '\u00f2':
	        case '\u00f3':
	        case '\u00f4':
	        case '\u00f5':
	        case '\u00f6':
	            return 'o';
	        case '\u00c7':
	        case '\u00e7':
	            return 'c';
	        case '\u00df':
	            return 'b';
	        case '\u00ff':
	        case '\u0178':
	            return 'y';
	        case '#':
	        case '[':
	        case ']':
	            return c;
	        case '\u00c8':
	        case '\u00c9':
	        case '\u00ca':
	        case '\u00cb':
	        case '\u00e8':
	        case '\u00e9':
	        case '\u00ea':
	        case '\u00eb':
	            return 'e';
	        case '\u00d1':
	        case '\u00f1':
	            return 'n';
	        case '\u00c0':
	        case '\u00c1':
	        case '\u00c2':
	        case '\u00c3':
	        case '\u00c4':
	        case '\u00e0':
	        case '\u00e1':
	        case '\u00e2':
	        case '\u00e3':
	        case '\u00e4':
	            return 'a';
	        case ' ':
	        case '-':
	        case '_':
	        case '\u00a0':
	            return '_';
	        default:
	            return Character.toLowerCase(c);
	        case '\u00d9':
	        case '\u00da':
	        case '\u00db':
	        case '\u00dc':
	        case '\u00f9':
	        case '\u00fa':
	        case '\u00fb':
	        case '\u00fc':
	            return 'u';
	        case '\u00cd':
	        case '\u00ce':
	        case '\u00cf':
	        case '\u00ed':
	        case '\u00ee':
	        case '\u00ef':
	            return 'i';
	    }
	}

	public static String formatAmount(int i_0, Language xlanguage1) {
	    return i_0 < 100000 ? "<col=ffff00>" + i_0 + "</col>" : (i_0 < 10000000 ? "<col=ffffff>" + i_0 / 1000 + LocalizedText.K2.translate(xlanguage1) + "</col>" : "<col=00ff80>" + i_0 / 1000000 + LocalizedText.M2.translate(xlanguage1) + "</col>");
	}
}
