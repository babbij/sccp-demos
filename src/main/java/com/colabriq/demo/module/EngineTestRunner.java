package com.goodforgoodbusiness.demo.module;

import static com.goodforgoodbusiness.shared.ConfigLoader.loadConfig;

import com.goodforgoodbusiness.shared.LogConfigurer;

public class EngineTestRunner {
	public static void main(String[] args) throws Exception {
		var config = loadConfig(EngineTestRunner.class, args[0]);
		LogConfigurer.init(EngineTestRunner.class, config.getString("log.properties", "log4j.debug.properties"));
		
//		var injector = createInjector(new EngineModule(config));
//		injector.getInstance(Webapp.class).start();
	}
}
