package com.jagex.game.runetek5.config.staticelements;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class StaticElementTypeList {

	public static StaticElements list(Js5ResourceProvider elementsResource, String fileName, boolean positioned) {
	    int archiveId = elementsResource.getArchiveId(fileName);
	    if (archiveId == -1) {
	        return new StaticElements(0);
	    } 
	    int[] fileIds = elementsResource.getValidFileIds(archiveId);
	    StaticElements elements = new StaticElements(fileIds.length);
	    int elementIndex = 0;
	    int fileIndex = 0;
		while (elementIndex < elements.elementCount) {
			Packet stream = new Packet(elementsResource.getFile(archiveId, fileIds[fileIndex++]));
			int bitpackedPlacement = stream.readInt();
			int categoryId = stream.readUnsignedShort();
			int allowSkip = stream.readUnsignedByte();
			if (!positioned && allowSkip == 1) {
				elements.elementCount--;
			} else {
				elements.bitpackedPlacement[elementIndex] = bitpackedPlacement;
				elements.mapCategoryIds[elementIndex] = categoryId;
				elementIndex++;
			}
		}
		return elements;
	}
}
