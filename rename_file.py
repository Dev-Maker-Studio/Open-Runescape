#!/usr/bin/env python3
"""
Method-scoped rename script for deobfuscating params/locals.
Applies renames within specified line ranges (method bodies).
Uses \b word boundaries - safe because _ is a word char.
"""
import re
import sys

def apply_renames(filepath, method_renames):
    """
    method_renames: list of (start_line, end_line, [(old_name, new_name), ...])
    Lines are 1-indexed, inclusive on both ends.
    """
    with open(filepath, 'r') as f:
        lines = f.readlines()

    total = 0
    for start, end, replacements in method_renames:
        for line_num in range(start, end + 1):
            idx = line_num - 1
            if idx < 0 or idx >= len(lines):
                continue
            for old, new in replacements:
                pattern = r'\b' + re.escape(old) + r'\b'
                new_line = re.sub(pattern, new, lines[idx])
                if new_line != lines[idx]:
                    lines[idx] = new_line
                    total += 1

    with open(filepath, 'w') as f:
        f.writelines(lines)
    return total


BASE = "/home/trent/darkan/darkan-bot/src/main/kotlin/com/jagex/game/runetek5"

# ============================================================
# FileOnDisk.kt (311 lines)
# ============================================================
def rename_FileOnDisk():
    PATH = BASE + "/cache/FileOnDisk.kt"
    renames = [
        # load0(buffer, i_2, i_3) - lines 68-167
        # i_2 and i_3 are params on line 68, used to init i_2_mod/i_3_mod on lines 69-70
        # After that only _mod versions are used
        (68, 167, [
            ("i_2_mod", "offsetMod"),
            ("i_3_mod", "lengthMod"),
            ("i_2", "offset"),
            ("i_3", "length"),
            ("long_5", "startPosition"),
            ("i_8", "originalLength"),
            ("i_9", "bytesAvailable"),
            ("long_14", "overlapStart"),
            ("long_11", "overlapEnd"),
            ("i_13", "overlapSize"),
            ("ioexception17", "ioException"),
        ]),
        # load(bytes_1) - lines 170-172
        (170, 172, [
            ("bytes_1", "data"),
        ]),
        # write(bytes_1, i_2, i_3) - lines 175-245
        (175, 245, [
            ("i_2_mod", "offsetMod"),
            ("i_3_mod", "lengthMod"),
            ("bytes_1", "data"),
            ("i_2", "offset"),
            ("i_3", "length"),
            ("i_5", "availableSpace"),
            ("long_10", "readOverlapStart"),
            ("long_7", "readOverlapEnd"),
            ("i_9", "overlapSize"),
            ("ioexception13", "ioException"),
        ]),
        # flushWriteBuffer() - lines 248-278
        (248, 278, [
            ("long_2", "overlapStart"),
            ("long_4", "overlapEnd"),
            ("i_6", "overlapSize"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"FileOnDisk.kt: {total} replacements")
    return total

# ============================================================
# Decompressor.kt (239 lines)
# ============================================================
def rename_Decompressor():
    PATH = BASE + "/cache/Decompressor.kt"
    renames = [
        # put(file, data, size, seek) overload - lines 31-157
        (31, 157, [
            ("i_10", "partIndex"),
        ]),
        # decompress(i_1) - lines 159-230
        (159, 230, [
            ("i_1", "fileId"),
            ("i_18", "dataSize"),
            ("i_6", "sector"),
            ("bytes_24", "output"),
            ("i_8", "bytesRead"),
            ("i_9", "partIndex"),
            ("i_19", "chunkSize"),
            ("b_11", "headerSize"),
            ("i_12", "currentFileId"),
            ("i_13", "currentPart"),
            ("i_14", "nextSector"),
            ("i_15", "indexId"),
            ("i_20", "totalReadSize"),
            ("i_17", "bufIdx"),
            ("bytes_4", "result"),
            ("ioexception22", "ioException"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"Decompressor.kt: {total} replacements")
    return total

# ============================================================
# Matrix43.kt (425 lines)
# ============================================================
def rename_Matrix43():
    PATH = BASE + "/math/Matrix43.kt"
    renames = [
        # updateIdentitiyMatrix() - lines 35-50
        (35, 50, [
            ("f_3", "tempXB"),
            ("f_4", "tempXC"),
            ("f_5", "tempYC"),
        ]),
        # setTextureCoordTransform - lines 114-143
        (114, 143, [
            ("i_2", "scaleU"),
            ("i_3", "scaleV"),
            ("f_7", "cosAngle"),
            ("f_8", "sinAngle"),
        ]),
        # calculatePerspective - lines 275-279
        (275, 279, [
            ("f_1", "x"),
            ("f_2", "y"),
            ("f_3", "z"),
        ]),
        # transformDirection - lines 293-300
        (293, 300, [
            ("floats_1", "direction"),
            ("f_2", "dx"),
            ("f_3", "dy"),
            ("f_4", "dz"),
        ]),
        # cacheInto - lines 302-307
        (302, 307, [
            ("floats_1", "output"),
        ]),
        # transformPointToInteger - lines 337-344
        (337, 344, [
            ("floats_1", "point"),
            ("f_2", "dx"),
            ("f_3", "dy"),
            ("f_4", "dz"),
        ]),
        # fromCoordinateSpace - lines 395-418
        (395, 418, [
            ("class3051", "space"),
            ("f_2", "ww"),
            ("f_3", "wx"),
            ("f_4", "wy"),
            ("f_5", "wz"),
            ("f_6", "xx"),
            ("f_7", "xy"),
            ("f_8", "xz"),
            ("f_9", "yy"),
            ("f_10", "yz"),
            ("f_11", "zz"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"Matrix43.kt: {total} replacements")
    return total

# ============================================================
# Matrix44.kt (580 lines)
# ============================================================
def rename_Matrix44():
    PATH = BASE + "/math/Matrix44.kt"
    renames = [
        # multiplyBy - lines 124-157
        (124, 157, [
            ("f_2", "r00"),
            ("f_3", "r01"),
            ("f_4", "r02"),
            ("f_5", "r03"),
            ("f_6", "r10"),
            ("f_7", "r11"),
            ("f_8", "r12"),
            ("f_9", "r13"),
            ("f_10", "r20"),
            ("f_11", "r21"),
            ("f_12", "r22"),
            ("f_13", "r23"),
            ("f_14", "r30"),
            ("f_15", "r31"),
            ("f_16", "r32"),
            ("f_17", "r33"),
        ]),
        # invert - lines 159-193
        (159, 193, [
            ("f_1", "invDet"),
            ("f_2", "c00"),
            ("f_3", "c01"),
            ("f_4", "c02"),
            ("f_5", "c03"),
            ("f_6", "c10"),
            ("f_7", "c11"),
            ("f_8", "c12"),
            ("f_9", "c13"),
            ("f_10", "c20"),
            ("f_11", "c21"),
            ("f_12", "c22"),
            ("f_13", "c23"),
            ("f_14", "c30"),
            ("f_15", "c31"),
            ("f_16", "c32"),
            ("f_17", "c33"),
        ]),
        # transformPoint - lines 227-234
        (227, 234, [
            ("floats_1", "point"),
            ("f_2", "origX"),
            ("f_3", "origY"),
            ("f_4", "origZ"),
        ]),
        # setPerspective - lines 255-272
        (255, 272, [
            ("f_3", "nearPlaneX"),
            ("f_4", "nearPlaneY"),
        ]),
        # setPerspectiveFromViewport - lines 274-276
        (274, 276, [
            ("f_1", "offsetX"),
            ("f_2", "offsetY"),
            ("f_3", "scaleX"),
            ("f_4", "scaleY"),
            ("f_7", "viewWidth"),
            ("f_8", "viewHeight"),
            ("f_9", "projectionScale"),
        ]),
        # extractRotationInto - lines 282-292
        (282, 292, [
            ("floats_1", "rotation"),
        ]),
        # extractLeftPlane - lines 294-304
        (294, 304, [
            ("floats_1", "plane"),
            ("f_2", "nx"),
            ("f_3", "ny"),
            ("f_4", "nz"),
            ("d_5", "invLength"),
        ]),
        # extractRightPlane - lines 306-316
        (306, 316, [
            ("floats_1", "plane"),
            ("f_2", "nx"),
            ("f_3", "ny"),
            ("f_4", "nz"),
            ("d_5", "invLength"),
        ]),
        # extractTopPlane - lines 318-328
        (318, 328, [
            ("floats_1", "plane"),
            ("f_2", "nx"),
            ("f_3", "ny"),
            ("f_4", "nz"),
            ("d_5", "invLength"),
        ]),
        # extractNearPlane - lines 330-340
        (330, 340, [
            ("floats_1", "plane"),
            ("f_2", "nx"),
            ("f_3", "ny"),
            ("f_4", "nz"),
            ("d_5", "invLength"),
        ]),
        # extractFarPlane - lines 342-352
        (342, 352, [
            ("floats_1", "plane"),
            ("f_2", "nx"),
            ("f_3", "ny"),
            ("f_4", "nz"),
            ("d_5", "invLength"),
        ]),
        # setRotatedTranslation - lines 408-455
        (408, 455, [
            ("i_2", "scaleU"),
            ("i_3", "scaleV"),
            ("f_4", "translateX"),
            ("f_5", "translateY"),
            ("f_6", "translateZ"),
            ("f_7", "cosAngle"),
            ("f_8", "sinAngle"),
            ("floats_9", "temp1"),
            ("floats_10", "temp2"),
            ("floats_11", "temp3"),
            ("floats_12", "temp4"),
            ("floats_13", "temp5"),
        ]),
        # hashCode - lines 457-461
        (457, 461, [
            ("b_1", "seed"),
            ("i_2", "result"),
        ]),
        # extractBottomPlane - lines 560-570
        (560, 570, [
            ("floats_1", "plane"),
            ("f_2", "nx"),
            ("f_3", "ny"),
            ("f_4", "nz"),
            ("d_5", "invLength"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"Matrix44.kt: {total} replacements")
    return total

# ============================================================
# PacketEncoder.kt (591 lines)
# ============================================================
def rename_PacketEncoder():
    PATH = BASE + "/network/packet/PacketEncoder.kt"
    renames = [
        # sendSignUpForm - lines 66-85
        (66, 85, [
            ("string_0", "email"),
            ("string_1", "password"),
            ("i_2", "age"),
            ("bool3", "acceptTerms"),
        ]),
        # sendPlayerOperation - lines 88-121
        (88, 121, [
            ("i_0", "opcode"),
            ("string_1", "playerName"),
            ("bool5", "found"),
        ]),
        # sendClanOperation - lines 216-228
        (216, 228, [
            ("i_0", "playerIndex"),
        ]),
        # sendCheckEmailPacket - lines 238-250
        (238, 250, [
            ("string_0", "email"),
        ]),
        # sendLobbyHyperlink - lines 253-271
        (253, 271, [
            ("string_0", "url"),
            ("string_1", "title"),
            ("string_2", "description"),
            ("bool3", "flag1"),
            ("bool4", "flag2"),
            ("i_8", "flags"),
        ]),
        # sendJoinFriendchat - lines 274-282
        (274, 282, [
            ("string_0", "ownerName"),
        ]),
        # generateSeed - lines 368-387
        (368, 387, [
            ("ints_3", "seeds"),
            ("i_4", "index"),
        ]),
        # reportBug - lines 358-365
        (358, 365, [
            ("i_0", "type"),
            ("string_1", "title"),
            ("string_2", "details"),
        ]),
        # continueInterfacePressed - lines 484-490
        (484, 490, [
            ("i_0", "interfaceHash"),
            ("i_1", "slotId"),
        ]),
        # sendAccountCreationStage - lines 559-565
        (559, 565, [
            ("i_0", "stage"),
        ]),
        # queueCutsceneExit - lines 568-590
        (568, 590, [
            ("bool0", "immediate"),
            ("arr_2", "miscActions"),
        ]),
        # sendInterfaceOnPlayer - lines 493-521
        (493, 521, [
            ("string_0", "playerName"),
            ("bool2", "found"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"PacketEncoder.kt: {total} replacements")
    return total

# ============================================================
# MiniMenu.kt (921 lines)
# ============================================================
def rename_MiniMenu():
    PATH = BASE + "/client/menu/MiniMenu.kt"
    renames = [
        # getActionText - lines 103-105
        (103, 105, [
            ("class282Sub50Sub70", "entry"),
        ]),
        # openSubmenu - lines 108-151
        (108, 151, [
            ("class282Sub50Sub150", "coloredEntry"),
            ("i_1", "entryY"),
            ("i_4", "maxTextWidth"),
            ("i_6", "submenuXPos"),
            ("class282Sub50Sub75", "menuEntry"),
            ("i_9", "submenuHeight"),
            ("i_7", "headerOffset"),
            ("i_8", "submenuYPos"),
        ]),
        # resetEntries - lines 234-251
        (234, 251, [
            ("class282Sub50Sub151", "coloredEntry"),
        ]),
        # removeEntry - lines 358-395
        (358, 395, [
            ("long_2", "bitpacked"),
            ("class282Sub50Sub154", "coloredEntry"),
            ("class282Sub50Sub155", "sortedEntry"),
            ("bool3", "found"),
            ("class282Sub50Sub76", "menuEntry"),
        ]),
        # reset - lines 411-427
        (411, 427, [
            ("class282Sub50Sub151", "coloredEntry"),
        ]),
        # insertSortedEntry - lines 445-460
        (445, 460, [
            ("class282Sub50Sub150", "newEntry"),
            ("bool2", "inserted"),
            ("class282Sub50Sub153", "existingEntry"),
        ]),
        # raycastTerrainIntersection - lines 463-500
        (463, 500, [
            ("f_8", "rayT"),
            ("f_9", "dirX"),
            ("f_10", "dirY"),
            ("f_11", "dirZ"),
            ("f_12", "prevX"),
            ("f_13", "prevY"),
            ("f_14", "prevZ"),
            ("i_6", "recursionDepth"),
        ]),
        # renderOpenMenuSimple - lines 509-554
        (509, 554, [
            ("i_5", "entryIndex"),
            ("class282Sub50Sub76", "menuEntry"),
            ("i_7", "entryY"),
            ("class282Sub50Sub158", "coloredEntry"),
        ]),
        # getMinimenuCursor - lines 617-719
        (617, 719, [
            ("i_4", "hoveredIndex"),
            ("i_5", "iterIndex"),
            ("class282Sub50Sub77", "menuEntry"),
            ("i_8", "entryY"),
            ("class4699", "queueIter"),
            ("class282Sub50Sub1510", "coloredEntry"),
        ]),
        # renderOpenMenuStyled - lines 734-848
        (734, 848, [
            ("i_2", "menuX"),
            ("i_3", "menuY"),
            ("i_4", "width"),
            ("i_5", "height"),
            ("b_6", "headerHeight"),
            ("i_15", "centeredX"),
            ("i_10", "mouseX"),
            ("i_11", "mouseY"),
            ("i_12", "entryIndex"),
            ("class282Sub50Sub713", "menuEntry"),
            ("i_14", "entryY"),
            ("class282Sub50Sub1516", "coloredEntry"),
        ]),
        # addEntry(action, coloredName, ...) - lines 349-355
        (349, 355, [
            ("long_5", "entityUid"),
            ("i_7", "param0"),
            ("i_8", "param1"),
            ("bool9", "isSelectable"),
            ("bool10", "showOtherLevelIcon"),
            ("bool13", "isUncategorized"),
        ]),
        # setViewportOrigin - lines 888-891
        (888, 891, [
            ("i_0", "x"),
            ("i_1", "y"),
        ]),
        # drawMenuEntry - lines 902-917
        (902, 917, [
            ("i_0", "mouseX"),
            ("i_1", "mouseY"),
            ("i_2", "menuX"),
            ("i_4", "menuWidth"),
            ("i_6", "entryY"),
            ("i_10", "normalColor"),
            ("i_11", "hoverColor"),
            ("i_101", "textColor"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"MiniMenu.kt: {total} replacements")
    return total

# ============================================================
# MenuInteraction.kt (603 lines)
# ============================================================
def rename_MenuInteraction():
    PATH = BASE + "/client/menu/MenuInteraction.kt"
    renames = [
        # processMenuclick - lines 40-351
        (40, 351, [
            ("class282Sub1021", "componentSettings"),
        ]),
        # processMenuInput - lines 354-602
        (354, 602, [
            ("i_5", "hoveredIndex"),
            ("i_6", "iterIndex"),
            ("i_11", "entryY"),
            ("class46915", "queueIter"),
            ("class282Sub50Sub1517", "coloredEntry"),
            ("bool16", "isWithinSubmenu"),
            ("bool18", "isSecondaryClick"),
            ("bool20", "isContextMenu"),
            ("bool19", "isDeferredClick"),
            ("i_12", "coloredIndex"),
            ("i_13", "entryRowY"),
            ("class282Sub50Sub1510", "coloredEntry2"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"MenuInteraction.kt: {total} replacements")
    return total

# ============================================================
# Client.kt (~2600 lines)
# ============================================================
def rename_Client():
    PATH = BASE + "/client/Client.kt"
    renames = [
        # redraw() - lines 425-500 (relevant section)
        (425, 500, [
            ("long_2", "timeSinceRedraw"),
            ("bool4", "songLoaded"),
        ]),
        # isInLobbyOrCreation - lines 1870-1872
        (1870, 1872, [
            ("i_0", "state"),
        ]),
        # isInLobbyConnection - lines 1874-1876
        (1874, 1876, [
            ("i_0", "state"),
        ]),
        # processGameTick - lines 1896-1921 (relevant section)
        (1896, 1921, [
            ("i_1", "packetCount"),
        ]),
        # drawGameArea - lines 2216-2250
        (2216, 2250, [
            ("bool4", "isFullRedraw"),
        ]),
        # processDrag - lines 2463-2536
        (2463, 2536, [
            ("i_1", "dragX"),
            ("i_2", "dragY"),
            ("i_3", "localX"),
            ("i_4", "localY"),
            ("i_7", "deltaX"),
            ("i_6", "deltaY"),
        ]),
        # drawGameFrame - lines 2573-2586
        (2573, 2586, [
            ("long_0", "deltaTimeNanos"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"Client.kt: {total} replacements")
    return total

# ============================================================
# Bzip2Compressor.kt (622 lines)
# ============================================================
def rename_Bzip2Compressor():
    PATH = BASE + "/cache/compression/Bzip2Compressor.kt"
    renames = [
        # readByte - lines 30-32
        (30, 32, [
            ("class4590", "blockEntry"),
        ]),
        # readBit - lines 35-37
        (35, 37, [
            ("class4590", "blockEntry"),
        ]),
        # readBits - lines 40-52
        (40, 52, [
            ("i_0", "numBits"),
            ("class4591", "blockEntry"),
            ("i_2", "result"),
            ("i_10000", "unusedBytesRead"),
        ]),
        # buildHuffmanTable - lines 55-111
        (55, 111, [
            ("ints_0", "limitTable"),
            ("ints_1", "baseTable"),
            ("ints_2", "permTable"),
            ("bytes_3", "lengths"),
            ("i_4", "minLen"),
            ("i_5", "maxLen"),
            ("i_6", "symbolCount"),
            ("i_7", "permIndex"),
            ("i_8", "outerIndex"),
            ("i_9", "innerIndex"),
        ]),
        # decodeBlock - lines 115-216
        (115, 216, [
            ("class4590", "blockEntry"),
            ("b_1", "currentByte"),
            ("i_2", "runCount"),
            ("i_3", "blockPosition"),
            ("i_4", "lastByte"),
            ("ints_5", "transformData"),
            ("i_6", "bwtPointer"),
            ("bytes_7", "output"),
            ("i_8", "outputPos"),
            ("i_9", "remaining"),
            ("i_11", "blockEnd"),
            ("b_12", "nextByte"),
            ("i_13", "storedCrc"),
            ("i_10000", "unusedCrc"),
        ]),
        # buildSymbolMap - lines 219-228
        (219, 228, [
            ("class4590", "blockEntry"),
        ]),
        # decompress(class4590) - lines 231-621
        (231, 621, [
            ("class4590", "blockEntry"),
            ("i_19", "minLen"),
            ("ints_20", "limitTable"),
            ("ints_21", "baseTable"),
            ("ints_22", "permTable"),
            ("bool23", "hasMoreBlocks"),
            ("b_24", "tempByte"),
            ("b_25", "tempByte2"),
            ("i_26", "outerIndex"),
            ("i_27", "groupCount"),
            ("i_28", "selectorCount"),
            ("i_29", "selectorIndex"),
            ("i_30", "groupIndex"),
            ("b_31", "minCodeLen"),
            ("b_32", "maxCodeLen"),
            ("i_33", "symbolIndex"),
            ("i_34", "blockSize"),
            ("i_35", "codeLen"),
            ("b_36", "codeBit"),
            ("i_37", "codeValue"),
            ("i_38", "decodedSymbol"),
            ("i_39", "runLength"),
            ("i_40", "mtfPosition"),
            ("bytes_45", "mtfOrder"),
            ("b_47", "initByte"),
            ("i_48", "huffBitLen"),
            ("i_49", "selectorsRemaining"),
            ("b_50", "selectorByte"),
            ("b_51", "currentSelector"),
            ("i_52", "mtfRemainder"),
            ("b_53", "nextSelector"),
            ("b_41", "innerSelector"),
            ("i_42", "yyPos"),
            ("i_43", "mtfBlockIndex"),
            ("i_44", "mtfSubIndex"),
        ]),
    ]
    total = apply_renames(PATH, renames)
    print(f"Bzip2Compressor.kt: {total} replacements")
    return total


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "all"
    grand_total = 0

    if target in ("all", "FileOnDisk"):
        grand_total += rename_FileOnDisk()
    if target in ("all", "Decompressor"):
        grand_total += rename_Decompressor()
    if target in ("all", "Matrix43"):
        grand_total += rename_Matrix43()
    if target in ("all", "Matrix44"):
        grand_total += rename_Matrix44()
    if target in ("all", "PacketEncoder"):
        grand_total += rename_PacketEncoder()
    if target in ("all", "MiniMenu"):
        grand_total += rename_MiniMenu()
    if target in ("all", "MenuInteraction"):
        grand_total += rename_MenuInteraction()
    if target in ("all", "Client"):
        grand_total += rename_Client()
    if target in ("all", "Bzip2Compressor"):
        grand_total += rename_Bzip2Compressor()

    print(f"\nTotal: {grand_total} replacements across all files")
