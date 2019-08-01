package com.goodforgoodbusiness.demo.module;

import com.goodforgoodbusiness.endpoint.EndpointModule;

/**
 * This exists mainly so we can easily pick up properties files from the test classpath
 */
public class EndpointTestRunner {
	public static void main(String[] args) throws Exception {
		EndpointModule.main(args);
	}
}
