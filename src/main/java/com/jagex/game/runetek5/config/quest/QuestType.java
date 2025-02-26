package com.jagex.game.runetek5.config.quest;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.vardomain.VarDomain;

/**
 * 
 * @author Brainpower - 01/05/2022
 *
 */
public class QuestType {

    public String name;
    public String sortName;
    public int type;
    public int difficulty;
    public boolean members;
    public int questPointReward;
    public int[] questPrerequisiteIds;
    public int[][] levelRequirements;
    public int questpointRequirement;
    public int graphicId = -1;
    public int[] varpRequirements;
    public String[] varpRequirementNames;
    public int[] varbitRequirements;
    public String[] varbitRequirementNames;
    QuestTypeList questTypelist;
    public int[][] varpValues;
    public int[][] varbitValues;
    public int[] unusedOpcode10;
    public int[] minVarpValue;
    public int[] maxVarpValue;
    public int[] minVarbitValue;
    public int[] maxVarbitValue;
    public IterableNodeMap params;

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.name = buffer.readJagString();
        } else if (opcode == 2) {
            this.sortName = buffer.readJagString();
        } else if (opcode == 3) {
        	int count = buffer.readUByte();
            this.varpValues = new int[count][3];
            for (int index = 0; index < count; index++) {
                this.varpValues[index][0] = buffer.readUShort();
                this.varpValues[index][1] = buffer.readInt();
                this.varpValues[index][2] = buffer.readInt();
            }
        } else if (opcode == 4) {
        	int count = buffer.readUByte();
            this.varbitValues = new int[count][3];
            for (int index = 0; index < count; index++) {
                this.varbitValues[index][0] = buffer.readUShort();
                this.varbitValues[index][1] = buffer.readInt();
                this.varbitValues[index][2] = buffer.readInt();
            }
        } else if (opcode == 5) {
            buffer.readUShort();
        } else if (opcode == 6) {
            this.type = buffer.readUByte();
        } else if (opcode == 7) {
            this.difficulty = buffer.readUByte();
        } else if (opcode == 8) {
            this.members = true;
        } else if (opcode == 9) {
            this.questPointReward = buffer.readUByte();
        } else if (opcode == 10) {
        	int count = buffer.readUByte();
            this.unusedOpcode10 = new int[count];
            for (int index = 0; index < count; index++) {
                this.unusedOpcode10[index] = buffer.readInt();
            }
        } else if (opcode == 12) {
            buffer.readInt();
        } else if (opcode == 13) {
        	int count = buffer.readUByte();
            this.questPrerequisiteIds = new int[count];
            for (int index = 0; index < count; index++) {
                this.questPrerequisiteIds[index] = buffer.readUShort();
            }
        } else if (opcode == 14) {
        	int count = buffer.readUByte();
            this.levelRequirements = new int[count][2];
            for (int index = 0; index < count; index++) {
                this.levelRequirements[index][0] = buffer.readUByte();
                this.levelRequirements[index][1] = buffer.readUByte();
            }
        } else if (opcode == 15) {
            this.questpointRequirement = buffer.readUShort();
        } else if (opcode == 17) {
            this.graphicId = buffer.readBigSmart();
        } else if (opcode == 18) {
        	int count = buffer.readUByte();
            this.varpRequirements = new int[count];
            this.minVarpValue = new int[count];
            this.maxVarpValue = new int[count];
            this.varpRequirementNames = new String[count];
            for (int index = 0; index < count; index++) {
                this.varpRequirements[index] = buffer.readInt();
                this.minVarpValue[index] = buffer.readInt();
                this.maxVarpValue[index] = buffer.readInt();
                this.varpRequirementNames[index] = buffer.readString();
            }
        } else if (opcode == 19) {
        	int count = buffer.readUByte();
            this.varbitRequirements = new int[count];
            this.minVarbitValue = new int[count];
            this.maxVarbitValue = new int[count];
            this.varbitRequirementNames = new String[count];
            for (int index = 0; index < count; index++) {
                this.varbitRequirements[index] = buffer.readInt();
                this.minVarbitValue[index] = buffer.readInt();
                this.maxVarbitValue[index] = buffer.readInt();
                this.varbitRequirementNames[index] = buffer.readString();
            }
        } else if (opcode == 249) {
        	int count = buffer.readUByte();
            if (this.params == null) {
            	int mapSize = MathUtils.nextPowerOfTwo(count);
                this.params = new IterableNodeMap(mapSize);
            }
            for (int index = 0; index < count; index++) {
                boolean isObject = buffer.readUByte() == 1;
                int key = buffer.readUMedium();
                Node value;
                if (isObject) {
                    value = new ObjectNode(buffer.readString());
                } else {
                    value = new IntNode(buffer.readInt());
                }
                this.params.put(value, key);
            }
        }
    }

    void postDecode() {
        if (this.sortName == null) {
            this.sortName = this.name;
        }
    }
    
    public boolean hasAllRequirements(VarDomain varprovider, int[] playerActualSkillLevels) {
        if (this.questTypelist.getQuestpointsCompletedQuests(varprovider) < this.questpointRequirement) {
            return false;
        } 
        if (this.levelRequirements != null) {
            for (int index = 0; index < this.levelRequirements.length; index++) {
                if (playerActualSkillLevels[this.levelRequirements[index][0]] < this.levelRequirements[index][1]) {
                    return false;
                }
            }
        }
        if (this.questPrerequisiteIds != null) {
            for (int index = 0; index < this.questPrerequisiteIds.length; index++) {
                if (!this.questTypelist.list(this.questPrerequisiteIds[index]).isComplete(varprovider)) {
                    return false;
                }
            }
        }
        if (this.varpRequirements != null) {
            for (int index = 0; index < this.varpRequirements.length; index++) {
                int varp = varprovider.getVarValue(this.varpRequirements[index]);
                if (varp < this.minVarpValue[index] || varp > this.maxVarpValue[index]) {
                    return false;
                }
            }
        }
        if (this.varbitRequirements != null) {
            for (int index = 0; index < this.varbitRequirements.length; index++) {
                int varibit = varprovider.getVarBitValue(this.varbitRequirements[index]);
                if (varibit < this.minVarbitValue[index] || varibit > this.maxVarbitValue[index]) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isStarted(VarDomain provider) {
        if (this.varpValues != null) {
            for (int index = 0; index < this.varpValues.length; index++) {
                if (provider.getVarValue(this.varpValues[index][0]) >= this.varpValues[index][1]) {
                    return true;
                }
            }
        }
        if (this.varbitValues != null) {
            for (int index = 0; index < this.varbitValues.length; index++) {
                if (provider.getVarBitValue(this.varbitValues[index][0]) >= this.varbitValues[index][1]) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isComplete(VarDomain varprovider) {
        if (this.varpValues != null) {
            for (int index = 0; index < this.varpValues.length; index++) {
                if (varprovider.getVarValue(this.varpValues[index][0]) >= this.varpValues[index][2]) {
                    return true;
                }
            }
        }
        if (this.varbitValues != null) {
            for (int index = 0; index < this.varbitValues.length; index++) {
                if (varprovider.getVarBitValue(this.varbitValues[index][0]) >= this.varbitValues[index][2]) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasQuestpointRequirement(VarDomain varprovider) {
        return this.questTypelist.getQuestpointsCompletedQuests(varprovider) >= this.questpointRequirement;
    }

    public boolean hasPrerequisiteRequirement(VarDomain varprovider, int prereqIndex) {
        return (this.questPrerequisiteIds != null && prereqIndex >= 0 && prereqIndex < this.questPrerequisiteIds.length)
        		&& this.questTypelist.list(this.questPrerequisiteIds[prereqIndex]).isComplete(varprovider);
    }

    public boolean hasVarpRequirement(VarDomain varprovider, int varpIndex) {
        if (this.varpRequirements != null && varpIndex >= 0 && varpIndex < this.varpRequirements.length) {
            int varp = varprovider.getVarValue(this.varpRequirements[varpIndex]);
            return varp >= this.minVarpValue[varpIndex] && varp <= this.maxVarpValue[varpIndex];
        } 
        return false;
    }

    public boolean hasVarbitRequirement(VarDomain varprovider, int varbitIndex) {
        if (this.varbitRequirements != null && varbitIndex >= 0 && varbitIndex < this.varbitRequirements.length) {
            int varbit = varprovider.getVarBitValue(this.varbitRequirements[varbitIndex]);
            return varbit >= this.minVarbitValue[varbitIndex] && varbit <= this.maxVarbitValue[varbitIndex];
        } 
        return false;
    }

    public String getParamstring(int key, String defaultString) {
        if (this.params == null) {
            return defaultString;
        } 
        ObjectNode valueObject = (ObjectNode) this.params.get(key);
        return valueObject == null ? defaultString : (String) valueObject.value;
    }

    public boolean hasLevelRequirement(int[] actualPlayerLevels, int skillReqIndex) {
        return (this.levelRequirements != null && skillReqIndex >= 0 && skillReqIndex < this.levelRequirements.length)
        		&& actualPlayerLevels[this.levelRequirements[skillReqIndex][0]] >= this.levelRequirements[skillReqIndex][1];
    }

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }

    public int getParamint(int key, int defaultValue) {
        if (this.params == null) {
            return defaultValue;
        } 
        IntNode valueObject = (IntNode) this.params.get(key);
        return valueObject == null ? defaultValue : valueObject.value;
    }
}
