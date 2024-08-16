package com.jagex.game.runetek5.buffer;

import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;

public class NormalsBufferDX {

    public DirectXBuffer anInterface32407;
    public DirectXBuffer anInterface32409;
    public boolean allocated;
    public boolean aBool406;

    public NormalsBufferDX(boolean bool1) {
        this.aBool406 = bool1;
    }

    public boolean method861() {
        return this.allocated && !this.aBool406;
    }

    public void deallocate() {
        if (this.anInterface32409 != null) {
            this.anInterface32409.deallocate();
        }
        this.allocated = false;
    }
}