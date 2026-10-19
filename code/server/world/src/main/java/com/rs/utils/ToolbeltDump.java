package com.rs.utils;

import org.darkan.core.cache.EnumDefinitions;
import org.darkan.core.cache.ItemDefinitions;

import java.io.IOException;

public class ToolbeltDump {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {
		//Cache.init();

		var general = EnumDefinitions.getEnum(5350);
		var fishing = EnumDefinitions.getEnum(5353);
		var crafting = EnumDefinitions.getEnum(5356);
		var farming = EnumDefinitions.getEnum(5359);
		var dung = EnumDefinitions.getEnum(5731);

		System.out.println(EnumDefinitions.getEnum(5732).getValues().values());

		for (Object itemId : dung.getValues().values())
			System.out.println(ItemDefinitions.getDefs((int) itemId).getName().toUpperCase().replace(" ", "_") + "(0000, " + itemId + "),");
	}

}
