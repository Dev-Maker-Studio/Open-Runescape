package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.LongNode;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.utils.ArrayUtils;
import com.jagex.game.runetek5.utils.MathUtils;

public class ClanSettings {

    public int currentOwner = -1;
    public int replacementOwner = -1;
    public int banCount;
    public String[] bannedUserNames;
    public int memberCount;
    public String clanName;
    public String[] memberNames;
    public byte[] memberRanks;
    public int[] memberJoinDays;
    public boolean allowGuests;
    public byte talkRank;
    public byte kickRank;
    public byte lootshareRank;
    public byte coinShare;
    public long aLong631;
    public long[] bannedUserHashes;
    public IterableNodeMap<Node> variables;
    public int[] sortedAffinedSlots;
    public int updateCount;
    public int anInt623;
    public long[] memberHashes;
    public int[] clanFlags;
    public boolean useHashes;
    public boolean useNames;

    public ClanSettings(Packet buffer) {
        decode(buffer);
    }

    public int getMemberIndex(String memberName) {
        if (memberName != null && !memberName.isEmpty()) {
            for (int index = 0; index < this.memberCount; index++) {
                if (this.memberNames[index].equals(memberName)) {
                    return index;
                }
            }
        }
        return -1;
    }

    //varbit
    public int method1198(int i_1, int i_2, int shift) {
        int mask = shift == 31 ? -1 : (1 << shift + 1) - 1;
        return (this.clanFlags[i_1] & mask) >>> i_2;
    }

    public Integer getIntVar(int i_1) {
        if (this.variables == null) {
            return null;
        } 
        Node<Object> node3 = this.variables.get(i_1);
        return node3 instanceof IntNode ? Integer.valueOf(((IntNode) node3).value) : null;
    }

    public Long getLongVar(int i_1) {
        if (this.variables == null) {
            return null;
        } 
        Node<Object> node3 = this.variables.get(i_1);
        return node3 instanceof LongNode ? Long.valueOf(((LongNode) node3).bitpacked) : null;
    }

    public String getStringVar(int i_1) {
        if (this.variables == null) {
            return null;
        } 
        Node<Object> node3 = this.variables.get(i_1);
        return node3 instanceof ObjectNode ? (String) ((ObjectNode) node3).value : null;
    }

    public void updateOwners() {
        if (this.memberCount == 0) {
            this.currentOwner = -1;
            this.replacementOwner = -1;
        } else {
            this.currentOwner = -1;
            this.replacementOwner = -1;
            int maximumIndex = 0;
            byte maximum = this.memberRanks[0];
            for (int i_4 = 1; i_4 < this.memberCount; i_4++) {
                if (this.memberRanks[i_4] > maximum) {
                    if (maximum == 125) {
                        this.replacementOwner = maximumIndex;
                    }
                    maximumIndex = i_4;
                    maximum = this.memberRanks[i_4];
                } else if (this.replacementOwner == -1 && this.memberRanks[i_4] == 125) {
                    this.replacementOwner = i_4;
                }
            }
            this.currentOwner = maximumIndex;
            this.memberRanks[this.currentOwner] = 126;
        }
    }

    public void banUser(long bannedUserHash, String bannedUserName) {
        if (bannedUserName != null && bannedUserName.isEmpty()) {
            bannedUserName = null;
        }
        if (this.useHashes != bannedUserHash > 0L) {
            throw new RuntimeException("");
        } 
        if (bannedUserName == null == this.useNames) {
            throw new RuntimeException("");
        } 
        if (bannedUserHash > 0L && (this.bannedUserHashes == null || this.banCount >= this.bannedUserHashes.length)
        		|| bannedUserName != null && (this.bannedUserNames == null || this.banCount >= this.bannedUserNames.length)) {
            expandBanTrackers(this.banCount + 5);
        }
        if (this.bannedUserHashes != null) {
            this.bannedUserHashes[this.banCount] = bannedUserHash;
        }
        if (this.bannedUserNames != null) {
            this.bannedUserNames[this.banCount] = bannedUserName;
        }
        this.banCount++;
    }

    public void unbanUser(int bannedUserIndex) {
        if (--this.banCount == 0) {
            this.bannedUserHashes = null;
            this.bannedUserNames = null;
        } else {
            if (this.bannedUserHashes != null) {
                ArrayUtils.arrayCopy(this.bannedUserHashes, bannedUserIndex + 1, this.bannedUserHashes, bannedUserIndex, this.banCount - bannedUserIndex);
            }
            if (this.bannedUserNames != null) {
                ArrayUtils.arrayCopy(this.bannedUserNames, bannedUserIndex + 1, this.bannedUserNames, bannedUserIndex, this.banCount - bannedUserIndex);
            }
        }
    }

    public int updateRank(int memberIndex, byte rank) {
    	if (rank == 126 || rank == 127) {
    		return -1;
    	}
        if (this.currentOwner == memberIndex && (this.replacementOwner == -1 || this.memberRanks[this.replacementOwner] < 125)) {
            return -1;
        } 
        if (this.memberRanks[memberIndex] == rank) {
            return -1;
        } 
        this.memberRanks[memberIndex] = rank;
        updateOwners();
        return memberIndex;
    }

    public int updateClanFlag(int index, int addedFlag, int shift, int bitmaskSize) {
        int mask = (1 << shift) - 1;
        int setMask = bitmaskSize == 31 ? -1 : (1 << bitmaskSize + 1) - 1;
        int clearMask = setMask ^ mask;
        addedFlag <<= shift;
        addedFlag &= clearMask;
        int flags = this.clanFlags[index];
        if ((flags & clearMask) == addedFlag) {
            return -1;
        } 
        flags &= ~clearMask;
        this.clanFlags[index] = flags | addedFlag;
        return index;
    }

    public void expandBanTrackers(int newSize) {
        if (this.useHashes) {
            if (this.bannedUserHashes != null) {
                ArrayUtils.arrayCopy(this.bannedUserHashes, 0, this.bannedUserHashes = new long[newSize], 0, this.banCount);
            } else {
                this.bannedUserHashes = new long[newSize];
            }
        }
        if (this.useNames) {
            if (this.bannedUserNames != null) {
                ArrayUtils.arrayCopy(this.bannedUserNames, 0, this.bannedUserNames = new String[newSize], 0, this.banCount);
            } else {
                this.bannedUserNames = new String[newSize];
            }
        }
    }

    public boolean updateBitpackedVariable(int key, int value, int shift, int bitmaskSize) {
        int mask = (1 << shift) - 1;
        int setMask = bitmaskSize == 31 ? -1 : (1 << bitmaskSize + 1) - 1;
        int clearMask = setMask ^ mask;
        value <<= shift;
        value &= clearMask;
        if (this.variables != null) {
            Node<Object> node = this.variables.get(key);
            if (node != null) {
                if (node instanceof IntNode intnode) {
                    if ((intnode.value & clearMask) == value) {
                        return false;
                    }
                    intnode.value &= ~clearMask;
                    intnode.value |= value;
                    return true;
                }
                node.unlink();
            }
        } else {
            this.variables = new IterableNodeMap<>(4);
        }
        this.variables.put(new IntNode(value), key);
        return true;
    }

    public boolean setLongVariable(int key, long value) {
        if (this.variables != null) {
            Node<Object> node4 = this.variables.get(key);
            if (node4 != null) {
                if (node4 instanceof LongNode class282Sub455) {
                    if (class282Sub455.bitpacked == value) {
                        return false;
                    }
                    class282Sub455.bitpacked = value;
                    return true;
                }
                node4.unlink();
            }
        } else {
            this.variables = new IterableNodeMap<>(4);
        }
        this.variables.put(new LongNode(value), key);
        return true;
    }

    public int[] getSortedAffinedSlots() {
        if (this.sortedAffinedSlots == null) {
            String[] names = new String[this.memberCount];
            this.sortedAffinedSlots = new int[this.memberCount];
            for (int index = 0; index < this.memberCount; this.sortedAffinedSlots[index] = index++) {
                names[index] = this.memberNames[index];
            }
            ArrayUtils.quicksortt(names, this.sortedAffinedSlots);
        }
        return this.sortedAffinedSlots;
    }

    public void addMember(long memberHash, String memberName, int joinDay) {
        if (memberName != null && memberName.isEmpty()) {
            memberName = null;
        }
        if (memberHash > 0L != this.useHashes) {
            throw new RuntimeException("");
        } 
        if (this.useNames == (memberName == null)) {
            throw new RuntimeException("");
        } 
        if (memberHash > 0L && (this.memberHashes == null || this.memberCount >= this.memberHashes.length)
        		|| memberName != null && (this.memberNames == null || this.memberCount >= this.memberNames.length)) {
            expandMemberTrackers(this.memberCount + 5);
        }
        if (this.memberHashes != null) {
            this.memberHashes[this.memberCount] = memberHash;
        }
        if (this.memberNames != null) {
            this.memberNames[this.memberCount] = memberName;
        }
        if (this.currentOwner == -1) {
            this.currentOwner = this.memberCount;
            this.memberRanks[this.memberCount] = 126;
        } else {
            this.memberRanks[this.memberCount] = 0;
        }
        this.clanFlags[this.memberCount] = 0;
        this.memberJoinDays[this.memberCount] = joinDay;
        this.memberCount++;
        this.sortedAffinedSlots = null;
    }

    public boolean putIntVariable(int key, int value) {
        if (this.variables != null) {
            Node<Object> node4 = this.variables.get(key);
            if (node4 != null) {
                if (node4 instanceof IntNode class282Sub385) {
                    if (value == class282Sub385.value) {
                        return false;
                    }
                    class282Sub385.value = value;
                    return true;
                }
                node4.unlink();
            }
        } else {
            this.variables = new IterableNodeMap<>(4);
        }
        this.variables.put(new IntNode(value), key);
        return true;
    }

    public boolean putStringValue(int key, String value) {
        if (value == null) {
            value = "";
        } else if (value.length() > 80) {
            value = value.substring(0, 80);
        }
        if (this.variables != null) {
            Node<Object> node4 = this.variables.get(key);
            if (node4 != null) {
                if (node4 instanceof ObjectNode class282Sub475) {
                    if (class282Sub475.value instanceof String) {
                        if (value.equals(class282Sub475.value)) {
                            return false;
                        }
                        class282Sub475.unlink();
                        class282Sub475.unlink();
                        this.variables.put(new ObjectNode(value), class282Sub475.pointer);
                        return true;
                    }
                }
                node4.unlink();
            }
        } else {
            this.variables = new IterableNodeMap<>(4);
        }
        this.variables.put(new ObjectNode(value), key);
        return true;
    }

    public Integer method1225(int i_1, int i_2, int i_3) {
        if (this.variables == null) {
            return null;
        } 
        Node<Object> node5 = this.variables.get(i_1);
        if (node5 instanceof IntNode) {
            int i_6 = i_3 == 31 ? -1 : (1 << i_3 + 1) - 1;
            return Integer.valueOf((((IntNode) node5).value & i_6) >>> i_2);
        } 
        return null;
    }

    public void expandMemberTrackers(int size) {
        if (this.useHashes) {
            if (this.memberHashes != null) {
                ArrayUtils.arrayCopy(this.memberHashes, 0, this.memberHashes = new long[size], 0, this.memberCount);
            } else {
                this.memberHashes = new long[size];
            }
        }
        if (this.useNames) {
            if (this.memberNames != null) {
                ArrayUtils.arrayCopy(this.memberNames, 0, this.memberNames = new String[size], 0, this.memberCount);
            } else {
                this.memberNames = new String[size];
            }
        }
        if (this.memberRanks != null) {
            ArrayUtils.arrayCopy(this.memberRanks, 0, this.memberRanks = new byte[size], 0, this.memberCount);
        } else {
            this.memberRanks = new byte[size];
        }
        if (this.clanFlags != null) {
            ArrayUtils.arrayCopy(this.clanFlags, 0, this.clanFlags = new int[size], 0, this.memberCount);
        } else {
            this.clanFlags = new int[size];
        }
        if (this.memberJoinDays != null) {
            ArrayUtils.arrayCopy(this.memberJoinDays, 0, this.memberJoinDays = new int[size], 0, this.memberCount);
        } else {
            this.memberJoinDays = new int[size];
        }
    }

    public void removeMember(int memberIndex) {
    	if (memberIndex < 0 || memberIndex >= this.memberCount) {
    		throw new RuntimeException("");
    	}
        this.sortedAffinedSlots = null;
        if (--this.memberCount == 0) {
            this.memberHashes = null;
            this.memberNames = null;
            this.memberRanks = null;
            this.clanFlags = null;
            this.memberJoinDays = null;
            this.currentOwner = -1;
            this.replacementOwner = -1;
        } else {
            ArrayUtils.arrayCopy(this.memberRanks, memberIndex + 1, this.memberRanks, memberIndex, this.memberCount - memberIndex);
            ArrayUtils.arrayCopy(this.clanFlags, memberIndex + 1, this.clanFlags, memberIndex, this.memberCount - memberIndex);
            ArrayUtils.arrayCopy(this.memberJoinDays, memberIndex + 1, this.memberJoinDays, memberIndex, this.memberCount - memberIndex);
            if (this.memberHashes != null) {
                ArrayUtils.arrayCopy(this.memberHashes, memberIndex + 1, this.memberHashes, memberIndex, this.memberCount - memberIndex);
            }
            if (this.memberNames != null) {
                ArrayUtils.arrayCopy(this.memberNames, memberIndex + 1, this.memberNames, memberIndex, this.memberCount - memberIndex);
            }
            if (this.currentOwner == memberIndex || this.replacementOwner == memberIndex) {
                updateOwners();
            }
        }
    }

    public void decode(Packet buffer) {
        int version = buffer.readUnsignedByte();
        if (version >= 1 && version <= 5) {
            int attr = buffer.readUnsignedByte();
            if ((attr & 0x1) != 0) {
                this.useHashes = true;
            }
            if ((attr & 0x2) != 0) {
                this.useNames = true;
            }
            if (!this.useHashes) {
                this.memberHashes = null;
                this.bannedUserHashes = null;
            }
            if (!this.useNames) {
                this.memberNames = null;
                this.bannedUserNames = null;
            }
            this.updateCount = buffer.readInt();
            this.anInt623 = buffer.readInt();
            if (version <= 3 && this.anInt623 != 0) {
                this.anInt623 += 16912800;
            }
            this.memberCount = buffer.readUnsignedShort();
            this.banCount = buffer.readUnsignedByte();
            this.clanName = buffer.readString();
            if (version >= 4) {
                buffer.readInt();
            }
            this.allowGuests = buffer.readUnsignedByte() == 1;
            this.talkRank = buffer.readByte();
            this.kickRank = buffer.readByte();
            this.lootshareRank = buffer.readByte();
            this.coinShare = buffer.readByte();
            
            if (this.memberCount > 0) {
                if (this.useHashes && (this.memberHashes == null || this.memberHashes.length < this.memberCount)) {
                    this.memberHashes = new long[this.memberCount];
                }
                if (this.useNames && (this.memberNames == null || this.memberNames.length < this.memberCount)) {
                    this.memberNames = new String[this.memberCount];
                }
                if (this.memberRanks == null || this.memberRanks.length < this.memberCount) {
                    this.memberRanks = new byte[this.memberCount];
                }
                if (this.clanFlags == null || this.clanFlags.length < this.memberCount) {
                    this.clanFlags = new int[this.memberCount];
                }
                if (this.memberJoinDays == null || this.memberJoinDays.length < this.memberCount) {
                    this.memberJoinDays = new int[this.memberCount];
                }
                for (int index = 0; index < this.memberCount; index++) {
                    if (this.useHashes) {
                        this.memberHashes[index] = buffer.readLong();
                    }
                    if (this.useNames) {
                        this.memberNames[index] = buffer.readNullString();
                    }
                    this.memberRanks[index] = buffer.readByte();
                    if (version >= 2) {
                        this.clanFlags[index] = buffer.readInt();
                    }
                    if (version >= 5) {
                        this.memberJoinDays[index] = buffer.readUnsignedShort();
                    } else {
                        this.memberJoinDays[index] = 0;
                    }
                }
                updateOwners();
            }
            if (this.banCount > 0) {
                if (this.useHashes && (this.bannedUserHashes == null || this.bannedUserHashes.length < this.banCount)) {
                    this.bannedUserHashes = new long[this.banCount];
                }
                if (this.useNames && (this.bannedUserNames == null || this.bannedUserNames.length < this.banCount)) {
                    this.bannedUserNames = new String[this.banCount];
                }
                for (int index = 0; index < this.banCount; index++) {
                    if (this.useHashes) {
                        this.bannedUserHashes[index] = buffer.readLong();
                    }
                    if (this.useNames) {
                        this.bannedUserNames[index] = buffer.readNullString();
                    }
                }
            }
            if (version >= 3) {
                int size = buffer.readUnsignedShort();
                if (size > 0) {
                    this.variables = new IterableNodeMap<>(size < 16 ? MathUtils.nextPowerOfTwo(size) : 16);
                    while (size-- > 0) {
                        int i_6 = buffer.readInt();
                        int key = i_6 & 0x3fffffff;
                        int type = i_6 >>> 30;
                        if (type == 0) {
                            int i_9 = buffer.readInt();
                            this.variables.put(new IntNode(i_9), key);
                        } else if (type == 1) {
                            long long_11 = buffer.readLong();
                            this.variables.put(new LongNode(long_11), key);
                        } else if (type == 2) {
                            String string_13 = buffer.readString();
                            this.variables.put(new ObjectNode(string_13), key);
                        }
                    }
                }
            }
        } else {
            throw new RuntimeException("" + version);
        }
    }
}
