package com.jagex.game.runetek5.graphics.model.buffer;

import com.jagex.game.runetek5.toolkit.companion.NativeStream;

public class ModelBufferDX {

	public NativeStream streamSource;
	public NativeStream anInterface4664;
	public boolean aBool663;
	
	/**
	 * Is usable at this time
	 */
	public boolean allocated;

	public ModelBufferDX(boolean bool1) {
        this.aBool663 = bool1;
    }

	public boolean method1264() {
        return this.allocated && !this.aBool663;
    }

    public void deallocate() {
        if (this.anInterface4664 != null) {
            this.anInterface4664.deallocate();
        }
        this.allocated = false;
    }

}
