#!/usr/bin/env python3
"""Rename obfuscated param/local variable names across multiple files."""

import re

def apply_renames(filepath, renames):
    """Apply line-specific renames to a file using regex word boundaries."""
    with open(filepath, 'r') as f:
        lines = f.readlines()
    total = 0
    for line_num, replacements in renames.items():
        idx = line_num - 1
        if idx < 0 or idx >= len(lines):
            print(f"  WARNING: line {line_num} out of range for {filepath}")
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

# ============================================================
# FileOnDisk.kt
# ============================================================
fileon_disk_path = "src/main/kotlin/com/jagex/game/runetek5/cache/FileOnDisk.kt"
fileon_disk_renames = {}

# load0 params: i_2=offset, i_3=length
for ln in [68, 69, 70, 72, 73, 76, 77]:
    fileon_disk_renames[ln] = [("i_2", "offset"), ("i_3", "length")]
# i_2_mod, i_3_mod
for ln in [69, 70, 72, 73, 76, 77, 91, 93, 94, 97, 101, 102, 109, 110,
           112, 114, 116, 117, 118, 123, 124, 125, 126, 129, 130, 131, 152, 154, 164]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].extend([("i_2_mod", "offsetMod"), ("i_3_mod", "lengthMod")])
# long_5=startPosition
for ln in [82, 138, 140, 141, 144, 146, 147, 152]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("long_5", "startPosition"))
# i_8=originalLength
for ln in [83, 138, 144, 146, 147]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("i_8", "originalLength"))
# i_9=bytesAvailable
for ln in [84, 86, 87, 88, 91, 92, 93, 94, 102, 103, 107, 108, 109, 110,
           114, 116, 117, 118, 119, 124, 125, 126, 129]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("i_9", "bytesAvailable"))
# long_14=overlapStart, long_11=overlapEnd in load0
for ln in [136, 139, 141, 150, 151, 152, 153, 154, 155]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("long_14", "overlapStart"))
for ln in [137, 144, 145, 146, 147, 150, 151, 152, 153, 154, 155]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("long_11", "overlapEnd"))
# i_13=overlapSize in load0
for ln in [151, 152]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("i_13", "overlapSize"))
# ioexception17 in load0
for ln in [159, 161]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("ioexception17", "ioException"))
# load: bytes_1=data
for ln in [170, 171]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("bytes_1", "data"))
# write: bytes_1=data, i_2=offset, i_3=length
for ln in [175, 177, 178, 179, 180, 183, 187, 189, 191, 192, 193, 197, 203, 204,
           211, 213, 217, 218, 219, 220, 225, 228, 229, 234, 235, 236, 237]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].extend([("bytes_1", "data"), ("i_2", "offset"), ("i_3", "length")])
for ln in [177, 178, 179, 180, 183, 187, 189, 190, 191, 192, 193, 197, 203, 204,
           211, 213, 217, 218, 219, 220, 225, 228, 229, 234, 235, 236, 237]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].extend([("i_2_mod", "offsetMod"), ("i_3_mod", "lengthMod")])
# i_5=availableSpace in write
for ln in [188, 189, 190, 191, 192]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("i_5", "availableSpace"))
# long_10=readOverlapStart, long_7=readOverlapEnd in write
for ln in [209, 212, 214, 223, 224, 225]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("long_10", "readOverlapStart"))
for ln in [210, 218, 220, 223, 224, 225]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("long_7", "readOverlapEnd"))
# i_9=overlapSize in write
for ln in [224, 225]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("i_9", "overlapSize"))
# ioexception13 in write
for ln in [241, 243]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("ioexception13", "ioException"))
# flushWriteBuffer: long_2=overlapStart, long_4=overlapEnd
for ln in [259, 262, 264, 271, 272, 273]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("long_2", "overlapStart"))
for ln in [260, 267, 269, 271, 272, 273]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("long_4", "overlapEnd"))
# i_6=overlapSize in flushWriteBuffer
for ln in [272, 273]:
    fileon_disk_renames.setdefault(ln, [])
    fileon_disk_renames[ln].append(("i_6", "overlapSize"))


# ============================================================
# Decompressor.kt
# ============================================================
decompressor_path = "src/main/kotlin/com/jagex/game/runetek5/cache/Decompressor.kt"
decompressor_renames = {}

# put() - i_10=partIndex
for ln in [61, 91, 116, 117, 132, 133, 147]:
    decompressor_renames[ln] = [("i_10", "partIndex")]

# decompress() params/locals
for ln in [159, 162, 165, 167, 187, 193, 203, 208]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_1", "fileId"))
for ln in [167, 169, 172, 173, 176, 181]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_18", "dataSize"))
for ln in [168, 172, 177, 180, 194, 195, 196, 205, 206, 214]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_6", "sector"))
for ln in [173, 212]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("bytes_24", "output"))
for ln in [174, 176, 212]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_8", "bytesRead"))
for ln in [175, 208, 215]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_9", "partIndex"))
for ln in [181, 188, 189, 192, 198, 199, 201, 202, 210]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_19", "chunkSize"))
for ln in [182, 191, 192, 201, 202, 210, 211]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("b_11", "headerSize"))
for ln in [183, 193, 194, 203, 204, 208]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_12", "currentFileId"))
for ln in [184, 194, 204, 208]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_13", "currentPart"))
for ln in [185, 195, 205, 209, 214]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_14", "nextSector"))
for ln in [186, 196, 206, 208]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_15", "indexId"))
for ln in [210]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_20", "totalReadSize"))
for ln in [211, 212]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("i_17", "bufIdx"))
for ln in [222, 223]:
    decompressor_renames.setdefault(ln, [])
    decompressor_renames[ln].append(("bytes_4", "result"))


# ============================================================
# Matrix43.kt
# ============================================================
matrix43_path = "src/main/kotlin/com/jagex/game/runetek5/math/Matrix43.kt"
matrix43_renames = {}

# updateIdentitiyMatrix: f_3=tempXB, f_4=tempXC, f_5=tempYC
for ln in [38, 39, 40]:
    matrix43_renames[ln] = [("f_3", "tempXB")]
for ln in [41, 42, 43]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_4", "tempXC"))
for ln in [44, 45, 46]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_5", "tempYC"))

# setTextureCoordTransform: i_2=scaleU, i_3=scaleV, f_7=cosAngle, f_8=sinAngle
for ln in [114, 123, 124, 125, 126, 127, 128, 136, 137, 139, 140]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].extend([("i_2", "scaleU"), ("i_3", "scaleV")])
for ln in [116, 117, 123, 124, 125, 126, 127, 128]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].extend([("f_7", "cosAngle"), ("f_8", "sinAngle")])

# calculatePerspective: f_1=x, f_2=y, f_3=z
for ln in [275, 276, 277, 278]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].extend([("f_1", "x"), ("f_2", "y"), ("f_3", "z")])

# transformDirection: floats_1=direction, f_2=origX, f_3=origY, f_4=origZ
for ln in [293, 294, 295, 296, 297, 298, 299]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].extend([("floats_1", "direction"), ("f_2", "origX"), ("f_3", "origY"), ("f_4", "origZ")])

# cacheInto: floats_1=output
for ln in [302, 303, 304, 305, 306]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("floats_1", "output"))

# transformPointToInteger: floats_1=point, f_2=dx, f_3=dy, f_4=dz
for ln in [337, 338, 339, 340, 341, 342, 343]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].extend([("floats_1", "point"), ("f_2", "dx"), ("f_3", "dy"), ("f_4", "dz")])

# fromCoordinateSpace: class3051=space, quaternion products
for ln in range(395, 418):
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("class3051", "space"))
for ln in [396, 406]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_2", "ww"))
for ln in [397, 407, 408]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_3", "wx"))
for ln in [398, 408, 412]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_4", "wy"))
for ln in [399, 407, 409]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_5", "wz"))
for ln in [400, 406, 410]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_6", "xx"))
for ln in [401, 407, 409, 411]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_7", "xy"))
for ln in [402, 408, 412]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_8", "xz"))
for ln in [403, 410, 413, 414]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_9", "yy"))
for ln in [404, 411, 413]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_10", "yz"))
for ln in [405, 406, 410, 414]:
    matrix43_renames.setdefault(ln, [])
    matrix43_renames[ln].append(("f_11", "zz"))


# ============================================================
# Matrix44.kt
# ============================================================
matrix44_path = "src/main/kotlin/com/jagex/game/runetek5/math/Matrix44.kt"
matrix44_renames = {}

# multiplyBy: f_2..f_17 -> r00..r33
mult_names = {
    "f_2": "r00", "f_3": "r01", "f_4": "r02", "f_5": "r03",
    "f_6": "r10", "f_7": "r11", "f_8": "r12", "f_9": "r13",
    "f_10": "r20", "f_11": "r21", "f_12": "r22", "f_13": "r23",
    "f_14": "r30", "f_15": "r31", "f_16": "r32", "f_17": "r33"
}
for ln in range(125, 157):
    matrix44_renames.setdefault(ln, [])
    for old, new in mult_names.items():
        matrix44_renames[ln].append((old, new))

# invert: f_1=invDet, f_2..f_17 -> c00..c33
inv_names = {
    "f_1": "invDet",
    "f_2": "c00", "f_3": "c01", "f_4": "c02", "f_5": "c03",
    "f_6": "c10", "f_7": "c11", "f_8": "c12", "f_9": "c13",
    "f_10": "c20", "f_11": "c21", "f_12": "c22", "f_13": "c23",
    "f_14": "c30", "f_15": "c31", "f_16": "c32", "f_17": "c33"
}
for ln in range(160, 193):
    matrix44_renames.setdefault(ln, [])
    for old, new in inv_names.items():
        matrix44_renames[ln].append((old, new))

# transformPoint: floats_1=point, f_2=origX, f_3=origY, f_4=origZ
for ln in [227, 228, 229, 230, 231, 232, 233, 234]:
    matrix44_renames.setdefault(ln, [])
    matrix44_renames[ln].extend([("floats_1", "point"), ("f_2", "origX"), ("f_3", "origY"), ("f_4", "origZ")])

# setPerspective: f_3=nearPlaneX, f_4=nearPlaneY
for ln in [255, 256, 261]:
    matrix44_renames.setdefault(ln, [])
    matrix44_renames[ln].extend([("f_3", "nearPlaneX"), ("f_4", "nearPlaneY")])

# setPerspectiveFromViewport: f_1..f_9 params
for ln in [274, 275]:
    matrix44_renames.setdefault(ln, [])
    matrix44_renames[ln].extend([
        ("f_1", "offsetX"), ("f_2", "offsetY"), ("f_3", "scaleX"), ("f_4", "scaleY"),
        ("f_7", "viewWidth"), ("f_8", "viewHeight"), ("f_9", "depth")
    ])

# extractRotationInto: floats_1=rotation
for ln in range(282, 292):
    matrix44_renames.setdefault(ln, [])
    matrix44_renames[ln].append(("floats_1", "rotation"))

# extract*Plane methods: floats_1=plane, f_2=nx, f_3=ny, f_4=nz, d_5=invLength
for ln_range in [
    range(294, 305),  # extractLeftPlane
    range(306, 316),  # extractRightPlane
    range(318, 328),  # extractTopPlane
    range(330, 340),  # extractNearPlane
    range(342, 352),  # extractFarPlane
    range(560, 570),  # extractBottomPlane
]:
    for ln in ln_range:
        matrix44_renames.setdefault(ln, [])
        matrix44_renames[ln].extend([
            ("floats_1", "plane"), ("f_2", "nx"), ("f_3", "ny"), ("f_4", "nz"), ("d_5", "invLength")
        ])

# setRotatedTranslation
for ln in range(408, 455):
    matrix44_renames.setdefault(ln, [])
    matrix44_renames[ln].extend([
        ("i_2", "scaleU"), ("i_3", "scaleV"),
        ("f_4", "translateX"), ("f_5", "translateY"), ("f_6", "translateZ"),
        ("f_7", "cosAngle"), ("f_8", "sinAngle"),
        ("floats_9", "temp1"), ("floats_10", "temp2"), ("floats_11", "temp3"),
        ("floats_12", "temp4"), ("floats_13", "temp5"),
    ])

# hashCode: b_1=seed, i_2=result
for ln in [457, 458, 459, 460]:
    matrix44_renames.setdefault(ln, [])
    matrix44_renames[ln].extend([("b_1", "seed"), ("i_2", "result")])


# ============================================================
# MiniMenu.kt
# ============================================================
minimenu_path = "src/main/kotlin/com/jagex/game/runetek5/client/menu/MiniMenu.kt"
minimenu_renames = {}

# getActionText: class282Sub50Sub70=entry
for ln in [103, 104]:
    minimenu_renames[ln] = [("class282Sub50Sub70", "entry")]

# openSubmenu: class282Sub50Sub150=coloredEntry, i_1=yPosition
for ln in [108, 114, 120, 124, 125, 148]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub150", "coloredEntry"))
for ln in [108, 136]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_1", "yPosition"))
for ln in [111, 117, 118, 123, 127, 128]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_4", "maxWidth"))
for ln in [113, 126, 127, 128, 131, 133, 145]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_6", "xPos"))
for ln in [114, 115, 116, 120]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub75", "menuEntry"))
for ln in [124]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_9", "submenuTotalHeight"))
for ln in [135]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_7", "headerHeight"))
for ln in [136, 137, 138, 139, 141, 143, 146]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_8", "yPos"))

# addEntry overload
for ln in [349, 352]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].extend([
        ("long_5", "entityUid"), ("i_7", "param0"), ("i_8", "param1"),
        ("bool9", "isSelectable"), ("bool10", "showOtherLevelIcon"), ("bool13", "isUncategorized")
    ])

# removeEntry
for ln in [363, 364, 365, 366]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("long_2", "bitpackedKey"))
for ln in [364, 365, 366, 368, 369]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub154", "coloredEntry"))
for ln in [372, 373, 374, 391]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub155", "uncatColoredEntry"))
for ln in [375, 382, 387]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("bool3", "found"))
for ln in [376, 377, 378, 385]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub76", "currentEntry"))

# resetEntries + reset: class282Sub50Sub151=coloredEntry
for ln in [235, 236, 237, 238, 239, 240, 242]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub151", "coloredEntry"))
for ln in [412, 413, 414, 415, 416, 417, 419]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub151", "coloredEntry"))

# insertSortedEntry
for ln in [445, 447, 450, 451]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub150", "coloredEntry"))
for ln in [446, 452, 457]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("bool2", "inserted"))
for ln in [448, 449, 450, 455]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub153", "existingEntry"))

# renderOpenMenuSimple
for ln in [515, 519, 525]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_5", "entryIndex"))
for ln in [516, 522, 524, 534, 536, 545, 546, 548, 550]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub76", "menuEntry"))
for ln in [517, 523, 524, 532, 534, 536, 547, 548]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_7", "entryY"))
for ln in [529, 530, 531, 533, 534, 536, 538, 539]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub158", "coloredEntry"))

# renderOpenMenuStyled
for ln in [735]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].extend([("i_2", "menuX"), ("i_3", "menuY"), ("i_4", "width"), ("i_5", "height")])
for ln in [738]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_5", "height"))
for ln in [739]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("b_6", "headerOffset"))
for ln in [770, 775, 787, 800, 815, 825, 839, 844]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_12", "entryIndex"))
for ln in [771, 778, 801, 802, 808, 818, 840, 841, 845]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub713", "menuEntry"))
for ln in [772, 779, 780, 781, 791, 792, 793, 803, 804, 805, 819, 820, 829, 830, 831, 842, 843]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_14", "entryY"))
for ln in [773, 788, 790, 792, 796, 826, 828, 830, 833, 836]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub1516", "coloredEntry"))

# raycastTerrainIntersection
for ln in [464, 474, 496]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("f_8", "t"))
for ln in [465]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("f_9", "dirX"))
for ln in [466]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("f_10", "dirY"))
for ln in [467]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("f_11", "dirZ"))
for ln in [468, 488, 493]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("f_12", "prevX"))
for ln in [469, 488, 494]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("f_13", "prevY"))
for ln in [470, 488, 495]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("f_14", "prevZ"))

# getMinimenuCursor
for ln in [631, 637, 646, 650, 665, 673, 678, 690, 698, 703]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_4", "hoveredIndex"))
for ln in [632, 638, 648, 651, 666, 679, 692, 704]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_5", "iterIndex"))
for ln in [634, 641, 643, 645, 669, 671, 673, 694, 696, 698]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("i_8", "entryY"))
for ln in [633, 653, 654, 656, 658, 706, 707, 709, 711]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub77", "menuEntry"))
for ln in [663, 680, 705]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class4699", "queueIter"))
for ln in [681, 682, 684, 686]:
    minimenu_renames.setdefault(ln, [])
    minimenu_renames[ln].append(("class282Sub50Sub1510", "coloredEntry"))


# ============================================================
# MenuInteraction.kt
# ============================================================
menuinteraction_path = "src/main/kotlin/com/jagex/game/runetek5/client/menu/MenuInteraction.kt"
menuinteraction_renames = {}

# processMenuclick: class282Sub1021=componentSettings
for ln in [259, 260]:
    menuinteraction_renames[ln] = [("class282Sub1021", "componentSettings")]

# processMenuInput - isOpen section
for ln in [405, 408, 411, 412, 413, 425, 426, 428, 443, 444, 445, 447, 457, 460, 474, 475, 476, 478, 490, 493]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("i_5", "hoveredIndex"))
for ln in [405, 406, 412, 415, 420, 425, 428, 436, 437, 444, 447, 457, 460, 468, 476, 490, 493, 502]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("i_6", "iterIndex"))
for ln in [406, 415, 416, 420, 421, 447, 448, 452, 453, 478, 479, 484, 485, 527, 528, 530, 534, 535, 537]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("i_11", "entryY"))
for ln in [409, 429, 494, 545]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("class46915", "queueIter"))
for ln in [495, 496, 498, 503]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("class282Sub50Sub1517", "coloredEntry"))

# processMenuInput - !isOpen section (collapsed hover)
for ln in [520, 521, 522, 524, 527, 528, 529, 530, 534, 535, 537, 541, 543, 544, 548, 554]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("i_12", "coloredIndex"))
for ln in [525, 527, 528, 534, 535]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("i_13", "entryRowY"))
for ln in [546, 547, 548, 549, 550, 555]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("class282Sub50Sub1510", "coloredEntry"))

# processMenuInput - closed menu section
for ln in [509, 510, 511, 512, 514, 517]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("bool16", "isWithinSubmenu"))
for ln in [565]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("bool16", "isPrimaryClick"))
for ln in [566]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("bool18", "isSecondaryClick"))
for ln in [567, 568, 569, 571]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("bool20", "isContextMenu"))
for ln in [587, 588]:
    menuinteraction_renames.setdefault(ln, [])
    menuinteraction_renames[ln].append(("bool19", "isDeferredClick"))


# ============================================================
# PacketEncoder.kt
# ============================================================
packetencoder_path = "src/main/kotlin/com/jagex/game/runetek5/network/packet/PacketEncoder.kt"
packetencoder_renames = {}

# sendSignUpForm
for ln in [66, 71, 72, 73, 74, 79]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].extend([
        ("string_0", "email"), ("string_1", "password"), ("i_2", "age"), ("bool3", "hasConsent")
    ])

# sendPlayerOperation
for ln in [88, 95, 96, 113, 118, 119]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].extend([("i_0", "opcode"), ("string_1", "targetName")])
for ln in [89, 90, 91, 93, 94, 108, 112, 113, 118]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].extend([("ints_4", "playerIndices"), ("bool5", "found"), ("i_3", "playerCount")])

# sendClanOperation
for ln in [216, 217, 218, 223]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("i_0", "playerIndex"))

# sendCheckEmailPacket
for ln in [238, 243]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("string_0", "email"))

# sendLobbyHyperlink
for ln in [253, 257, 258, 259, 260, 262, 263, 265, 266]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].extend([
        ("string_0", "url1"), ("string_1", "url2"), ("string_2", "url3"),
        ("bool3", "flag1"), ("bool4", "flag2")
    ])
for ln in [261, 263, 266, 268]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("i_8", "flags"))

# sendJoinFriendchat
for ln in [274, 275, 278, 279]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("string_0", "ownerName"))

# generateSeed
for ln in [370, 371, 372, 373, 376, 377, 378, 379, 386]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("ints_3", "seed"))

# reportBug
for ln in [358, 360, 361, 362, 363]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].extend([("i_0", "bugType"), ("string_1", "description"), ("string_2", "details")])

# continueInterfacePressed
for ln in [484, 485, 487, 488]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].extend([("i_0", "interfaceHash"), ("i_1", "slotId")])

# sendAccountCreationStage
for ln in [559, 562]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("i_0", "stage"))

# queueCutsceneExit
for ln in [568, 570, 587]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("bool0", "isForced"))
for ln in [571, 573, 574]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("arr_2", "miscActions"))
for ln in [575, 576]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("class964", "action"))

# sendInterfaceOnPlayer
for ln in [493, 500, 515]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("string_0", "targetName"))
for ln in [495, 510, 514]:
    packetencoder_renames.setdefault(ln, [])
    packetencoder_renames[ln].append(("bool2", "found"))


# ============================================================
# Bzip2Compressor.kt
# ============================================================
bzip2_path = "src/main/kotlin/com/jagex/game/runetek5/cache/compression/Bzip2Compressor.kt"
bzip2_renames = {}

# readBits
for ln in [40, 41, 49, 50]:
    bzip2_renames.setdefault(ln, [])
    bzip2_renames[ln].extend([("i_0", "numBits"), ("class4591", "entry"), ("i_2", "result")])

# buildHuffmanTable
for ln in range(55, 112):
    bzip2_renames.setdefault(ln, [])
    bzip2_renames[ln].extend([
        ("ints_0", "limitTable"), ("ints_1", "baseTable"), ("ints_2", "permTable"),
        ("bytes_3", "lengths"), ("i_4", "minLen"), ("i_5", "maxLen"), ("i_6", "numSymbols"),
        ("i_7", "permIndex"), ("i_8", "loopI"), ("i_9", "loopJ"),
    ])

# decodeBlock
for ln in range(115, 216):
    bzip2_renames.setdefault(ln, [])
    bzip2_renames[ln].extend([
        ("b_1", "currentByte"), ("i_2", "runCount"), ("i_3", "blockPos"), ("i_4", "lastByte"),
        ("ints_5", "table"), ("i_6", "bwtPtr"), ("bytes_7", "output"), ("i_8", "outPos"),
        ("i_9", "remaining"), ("b_12", "nextByte"), ("i_11", "blockLimit"), ("i_13", "blockCrc"),
    ])

# decompress (main)
for ln in range(231, 621):
    bzip2_renames.setdefault(ln, [])
    bzip2_renames[ln].extend([
        ("i_19", "minLen"), ("ints_20", "limitArr"), ("ints_21", "baseArr"), ("ints_22", "permArr"),
        ("bool23", "hasMore"), ("b_24", "bitValue"), ("b_25", "byteValue"),
        ("i_26", "loopI"), ("i_27", "loopJ"), ("i_28", "numSelectors"),
        ("i_29", "selectorIdx"), ("i_30", "groupIdx"),
        ("bytes_45", "mtfOrder"), ("b_47", "initIdx"),
        ("b_31", "minBits"), ("b_32", "maxBits"),
        ("i_33", "innerIdx"), ("i_34", "writePos"), ("i_35", "codeBits"),
        ("b_36", "nextBit"), ("i_37", "codeValue"), ("i_38", "symbol"),
        ("i_39", "runLength"), ("i_40", "mtfBlock"),
        ("i_48", "codeLenBits"), ("i_49", "selectorCountdown"),
        ("b_50", "initCountdown"), ("b_51", "groupSelector"),
        ("i_42", "mtfPos"), ("i_43", "blockI"), ("i_44", "blockJ"),
        ("i_52", "yyPos"), ("b_41", "nextSelector"), ("b_53", "currentSelector"),
    ])


# ============================================================
# Client.kt
# ============================================================
client_path = "src/main/kotlin/com/jagex/game/runetek5/client/Client.kt"
client_renames = {}

# drawGameFrame
for ln in [2573, 2577, 2580]:
    client_renames[ln] = [("long_0", "deltaTimeNanos")]

# drawGameArea
for ln in [2216, 2243]:
    client_renames[ln] = [("bool4", "isFullRedraw")]

# processTick
for ln in [427, 499]:
    client_renames[ln] = [("long_2", "timeSinceRedraw")]
for ln in [429, 430]:
    client_renames[ln] = [("bool4", "songLoaded")]

# isInLobbyOrCreation / isInLobbyConnection
for ln in [1870, 1871]:
    client_renames[ln] = [("i_0", "gameState")]
for ln in [1874, 1875]:
    client_renames[ln] = [("i_0", "gameState")]

# processIncomingPackets
for ln in [1908, 1909, 1910]:
    client_renames[ln] = [("i_1", "packetCount")]

# getComponentSettings
for ln in [1674, 1675]:
    client_renames[ln] = [("class282Sub101", "componentSettings")]

# initializeInterfaces
for ln in [1628, 1629]:
    client_renames[ln] = [("i_0", "index")]

# cacheDepthFlags
for ln in [1688]:
    client_renames[ln] = [("i_3", "flagIndex")]

# updateDraggedComponent
for ln in [2467, 2469, 2471, 2472, 2474, 2475, 2486, 2489]:
    client_renames.setdefault(ln, [])
    client_renames[ln].append(("i_1", "dragX"))
for ln in [2468, 2470, 2477, 2478, 2480, 2481, 2487, 2490]:
    client_renames.setdefault(ln, [])
    client_renames[ln].append(("i_2", "dragY"))
for ln in [2483, 2486, 2489, 2499, 2526]:
    client_renames.setdefault(ln, [])
    client_renames[ln].append(("i_3", "adjustedX"))
for ln in [2484, 2487, 2490, 2500, 2527]:
    client_renames.setdefault(ln, [])
    client_renames[ln].append(("i_4", "adjustedY"))
for ln in [2517, 2519]:
    client_renames.setdefault(ln, [])
    client_renames[ln].append(("i_7", "dragDeltaX"))
for ln in [2518, 2519]:
    client_renames.setdefault(ln, [])
    client_renames[ln].append(("i_6", "dragDeltaY"))

# random obstacle placement
for ln in [2124, 2125, 2128]:
    client_renames.setdefault(ln, [])
    client_renames[ln].append(("i_2", "randomBits"))


# ============================================================
# Apply all renames
# ============================================================
if __name__ == "__main__":
    total = 0

    files = [
        (fileon_disk_path, fileon_disk_renames, "FileOnDisk.kt"),
        (decompressor_path, decompressor_renames, "Decompressor.kt"),
        (matrix43_path, matrix43_renames, "Matrix43.kt"),
        (matrix44_path, matrix44_renames, "Matrix44.kt"),
        (minimenu_path, minimenu_renames, "MiniMenu.kt"),
        (menuinteraction_path, menuinteraction_renames, "MenuInteraction.kt"),
        (packetencoder_path, packetencoder_renames, "PacketEncoder.kt"),
        (bzip2_path, bzip2_renames, "Bzip2Compressor.kt"),
        (client_path, client_renames, "Client.kt"),
    ]

    for path, renames, name in files:
        count = apply_renames(path, renames)
        print(f"  {name}: {count} replacements")
        total += count

    print(f"\nTotal: {total} replacements across {len(files)} files")
