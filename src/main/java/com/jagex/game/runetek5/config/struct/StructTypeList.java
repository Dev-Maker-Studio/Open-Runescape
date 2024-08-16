package com.jagex.game.runetek5.config.struct;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class StructTypeList {

    public final Js5ResourceProvider structProvider;
    public final int totalStructCount;
    public final StructType[] cache;

    public StructTypeList(Js5ResourceProvider strucTypeProvider) {
        this.structProvider = strucTypeProvider;
        this.totalStructCount = this.structProvider.getArchiveFileCount(SharedConfigsType.STRUCTS.id);
        this.cache = new StructType[this.totalStructCount];

        for (int id = 0; id < this.totalStructCount; id++) {
            byte[] data;
            synchronized (this.structProvider) {
                data = this.structProvider.getFile(SharedConfigsType.STRUCTS.id, id);
            }

            StructType structType = new StructType();
            if (data != null) {
                structType.decode(new Packet(data));
            }

            this.cache[id] = structType;
        }

    }

    public StructType getStruct(int id) {
        return id < 0 ? new StructType() : this.cache[id];
    }

}
