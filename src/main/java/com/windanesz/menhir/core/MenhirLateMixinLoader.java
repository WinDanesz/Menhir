package com.windanesz.menhir.core;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.List;

public class MenhirLateMixinLoader implements ILateMixinLoader {
	@Override
	public List<String> getMixinConfigs() {
		List<String> configs = new ArrayList<>();

		return configs;
	}

	@Override
	public boolean shouldMixinConfigQueue(String mixinConfig) {

		return true;
	}
}