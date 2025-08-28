package com.windanesz.menhir.core;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

public class MenhirModContainer extends DummyModContainer {
	public MenhirModContainer() {
		super(new ModMetadata());
		ModMetadata meta = this.getMetadata();
		meta.modId = "menhirscore";
		meta.name = "Menhir Core";
		meta.description = "Core functionality of Menhir";
		meta.version = "1.12.2-1.0.0";
		meta.authorList.add("WinDanesz");
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}
}