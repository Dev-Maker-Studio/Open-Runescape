package com.jagex.game.runetek5.utils

object BufferUtils {

    @JvmStatic
    fun ilog(num: Int): Int {
        var num = num
        var sizeShift = 0
        if (num < 0 || num >= 65536) {
            num = num ushr 16
            sizeShift += 16
        }
        if (num >= 256) {
            num = num ushr 8
            sizeShift += 8
        }
        if (num >= 16) {
            num = num ushr 4
            sizeShift += 4
        }
        if (num >= 4) {
            num = num ushr 2
            sizeShift += 2
        }
        if (num >= 1) {
            num = num ushr 1
            sizeShift++
        }
        return num + sizeShift
    }

    @JvmStatic
    fun generateBytePatterns(tileSize: Int): Array<Array<ByteArray?>> {
        val patterns = Array(8) { arrayOfNulls<ByteArray>(4) }
        val width = tileSize
        val height = tileSize
        var mask = ByteArray(tileSize * tileSize)
        var index = 0

        var row: Int
        var col: Int
        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col <= row) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[0][0] = mask
        mask = ByteArray(height * width)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < height) {
                if (col <= row) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[0][1] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col >= row) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[0][2] = mask
        mask = ByteArray(width * height)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < width) {
                if (col >= row) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[0][3] = mask
        mask = ByteArray(width * height)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < width) {
                if (col <= row shr 1) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[1][0] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (index >= 0 && index < mask.size) {
                    if (col >= row shl 1) {
                        mask[index] = -1
                    }
                }
                ++index
                col++
            }
            row++
        }

        patterns[1][1] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = width - 1
            while (col >= 0) {
                if (col <= row shr 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            row++
        }

        patterns[1][2] = mask
        mask = ByteArray(height * width)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = width - 1
            while (col >= 0) {
                if (col >= row shl 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            --row
        }

        patterns[1][3] = mask
        mask = ByteArray(width * height)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = width - 1
            while (col >= 0) {
                if (col <= row shr 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            --row
        }

        patterns[2][0] = mask
        mask = ByteArray(height * width)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < width) {
                if (col >= row shl 1) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[2][1] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col <= row shr 1) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[2][2] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = width - 1
            while (col >= 0) {
                if (col >= row shl 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            row++
        }

        patterns[2][3] = mask
        mask = ByteArray(width * height)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < width) {
                if (col >= row shr 1) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[3][0] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col <= row shl 1) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[3][1] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = width - 1
            while (col >= 0) {
                if (col >= row shr 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            row++
        }

        patterns[3][2] = mask
        mask = ByteArray(width * height)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = width - 1
            while (col >= 0) {
                if (col <= row shl 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            --row
        }

        patterns[3][3] = mask
        mask = ByteArray(width * height)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = width - 1
            while (col >= 0) {
                if (col >= row shr 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            --row
        }

        patterns[4][0] = mask
        mask = ByteArray(height * width)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < width) {
                if (col <= row shl 1) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[4][1] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col >= row shr 1) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[4][2] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = width - 1
            while (col >= 0) {
                if (col <= row shl 1) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            row++
        }

        patterns[4][3] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col <= width / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[5][0] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (row <= height / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[5][1] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col >= width / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[5][2] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (row >= height / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[5][3] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col <= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[6][0] = mask
        mask = ByteArray(height * width)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < width) {
                if (col <= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[6][1] = mask
        mask = ByteArray(height * width)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = width - 1
            while (col >= 0) {
                if (col <= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            --row
        }

        patterns[6][2] = mask
        mask = ByteArray(height * width)
        index = 0

        row = 0
        while (row < height) {
            col = width - 1
            while (col >= 0) {
                if (col <= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            row++
        }

        patterns[6][3] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = 0
            while (col < width) {
                if (col >= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            row++
        }

        patterns[7][0] = mask
        mask = ByteArray(width * height)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = 0
            while (col < width) {
                if (col >= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                col++
            }
            --row
        }

        patterns[7][1] = mask
        mask = ByteArray(height * width)
        index = 0

        row = height - 1
        while (row >= 0) {
            col = width - 1
            while (col >= 0) {
                if (col >= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            --row
        }

        patterns[7][2] = mask
        mask = ByteArray(width * height)
        index = 0

        row = 0
        while (row < height) {
            col = width - 1
            while (col >= 0) {
                if (col >= row - height / 2) {
                    mask[index] = -1
                }
                ++index
                --col
            }
            row++
        }

        patterns[7][3] = mask
        return patterns
    }
}
