package com.jagex.game.runetek5.config.enumtype;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

/**
 * 
 * @author Brainpower - 25/03/2023
 *
 */
public class EnumType {

    public char keyType;
    public char valueType;
    public String defaultStringValue = "null";
    public int defaultIntValue;
    public int size;
    public Map<Integer, Object> parameters;
    public Object[] values;
    public HashMap<Object, int[]> valueReferences;


    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }

            decode(buffer, opcode);
        }
    }
    
    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.keyType = Utils.cp1252ToChar(stream.readByte());
        } else if (opcode == 2) {
            this.valueType = Utils.cp1252ToChar(stream.readByte());
        } else if (opcode == 3) {
            this.defaultStringValue = stream.readString();
        } else if (opcode == 4) {
            this.defaultIntValue = stream.readInt();
        } else if (opcode == 5 || opcode == 6) {
            this.size = stream.readUShort();
            this.parameters = new HashMap<Integer, Object>(this.size);

            for (int index = 0; index < this.size; index++) {
                int key = stream.readInt();
                Object value;
                if (opcode == 5) {
                    value = stream.readString();
                } else {
                    value = stream.readInt();
                }

                this.parameters.put(key, value);
            }
        } else if (opcode == 7 || opcode == 8) {
            int numValues = stream.readUShort();
            this.size = stream.readUShort();
            this.values = new Object[numValues];

            for (int index = 0; index < this.size; index++) {
                int valueToSet = stream.readUShort();
                if (opcode == 7) {
                    this.values[valueToSet] = stream.readString();
                } else {
                    this.values[valueToSet] = stream.readInt();
                }
            }
        }
    }
    
    public void buildValueMap() {
        HashMap<Object, LinkedList<Integer>> hashmap = new HashMap<Object, LinkedList<Integer>>();
        if (this.values != null) {
            for (int index = 0; index < this.values.length; index++) {
                if (this.values[index] != null) {
                    Object key = this.values[index];
                    LinkedList<Integer> valueIndices = hashmap.get(key);
                    if (valueIndices == null) {
                        valueIndices = new LinkedList<Integer>();
                        hashmap.put(key, valueIndices);
                    }

                    valueIndices.add(index);
                }
            }
        } else {
            if (this.parameters == null) {
                throw new IllegalStateException();
            }

            Iterator<Entry<Integer, Object>> parameterIterator = this.parameters.entrySet().iterator();
            while (parameterIterator.hasNext()) {
                Entry<Integer, Object> entry = parameterIterator.next();
                Object key = entry.getValue();
                LinkedList<Integer> valueIndices = hashmap.get(key);
                if (valueIndices == null) {
                    valueIndices = new LinkedList<Integer>();
                    hashmap.put(key, valueIndices);
                }
                valueIndices.add(entry.getKey());
            }
        }

        this.valueReferences = new HashMap<Object, int[]>();
        Iterator<Entry<Object, LinkedList<Integer>>> hashmapIterator = hashmap.entrySet().iterator();
        
        while (hashmapIterator.hasNext()) {
        	Entry<Object, LinkedList<Integer>> entry = hashmapIterator.next();
        	LinkedList<Integer> referenceList = entry.getValue();
            int[] references = new int[referenceList.size()];
            int index = 0;
            Iterator<Integer> intIt = referenceList.iterator();
            
            while (intIt.hasNext()) {
            	Integer reference = intIt.next();
                references[index++] = reference.intValue();
            }

            if (this.values == null) {
                Arrays.sort(references);
            }
            this.valueReferences.put(entry.getKey(), references);
        }
    }
   
    public Object getValue(int key) {
        return this.values != null ? (key >= 0 && key < this.values.length ? this.values[key] : null) : (this.parameters != null ? this.parameters.get(key) : null);
    }

    public String getStringValue(int key) {
        Object object = getValue(key);
        return object == null ? this.defaultStringValue : (String) object;
    }
    
    public int getIntValue(int key) {
        Object value = getValue(key);
        return value == null ? this.defaultIntValue : ((Integer) value).intValue();
    }
    
    public int[] getValueReferences(Serializable serializable) {
        if (this.size == 0) {
            return null;
        } 
        if (this.valueReferences == null) {
            buildValueMap();
        }

        return this.valueReferences.get(serializable);
    }

    public boolean containsKey(Serializable serializable) {
        if (this.size == 0) {
            return false;
        } 
        if (this.valueReferences == null) {
            buildValueMap();
        }

        return this.valueReferences.containsKey(serializable);
    }

    
    public int getSize() {
        return this.size;
    }
}
