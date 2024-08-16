package com.jagex.game.runetek5.sound;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.midi.VorbisNode;
import com.jagex.game.runetek5.sound.jagtheora.VorbisFileReference;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.trigger.sound.SoundTrigger;

public class VorbisSoundManager {

	public static void method1575(int fileId, int i_1, boolean bool2) {
	    if (StaticMethods.INDEX36_FILE_CACHE.get(fileId) == null) {
	        if (!client.jagtheoraLoaded) {
	            SoundTrigger.playVorbis(fileId, bool2);
	        } else {
	            VorbisNode vorbisnode = new VorbisNode(fileId, new VorbisFileReference(Resource.VORBIS, fileId), i_1, bool2);
	            vorbisnode.vfReference.setLanguage(client.CURRENT_LANGUAGE.getLowercaseAbbreviation());
	            StaticMethods.INDEX36_FILE_CACHE.put(vorbisnode, fileId);
	        }
	    }
	}

}
