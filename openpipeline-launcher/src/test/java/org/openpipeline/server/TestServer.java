package org.openpipeline.server;

import org.openpipeline.server.launcher.OpenPipelineLauncher;

public class TestServer {

	// later move this to openpipeline-test

	public static void main(String[] args) throws Exception {
		System.setProperty("app.home",
				"/dev/openpipeline/trunk/openpipeline-launcher");

		String[] apps = { "-a/=../openpipeline-admin/src/main/webapp" };

		OpenPipelineLauncher.main(apps);
	}
}
