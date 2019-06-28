package com.goodforgoodbusiness.demo.northwind;

import static com.goodforgoodbusiness.shared.ConfigLoader.loadConfig;
import static java.lang.System.getenv;

import java.io.File;

import com.goodforgoodbusiness.endpoint.EndpointModule;
import com.goodforgoodbusiness.engine.EngineModule;
import com.goodforgoodbusiness.shared.LogConfigurer;
import com.inova8.odata2sparql.OData2SPARQLServer;

/** launches an endpoint that loads the beef turtle files directly */
public class NorthwindRunner {
	public static void main(String[] args) throws Exception {		
		LogConfigurer.init(NorthwindRunner.class, "log4j.debug.properties");
		
		var engineConfig = loadConfig(NorthwindRunner.class, "northwind/engine-1.properties");
		new EngineModule(engineConfig).start();
		
		var endpointConfig = loadConfig(NorthwindRunner.class, "northwind/endpoint-1.properties");
		new EndpointModule(endpointConfig).start();
		
		var schemaConfig = loadConfig(NorthwindRunner.class, "northwind/schema.properties");
		new EndpointModule(schemaConfig).start();
		
		new OData2SPARQLServer(8061, new File(getenv("MODELS_FILE"))).start();
	}
}
