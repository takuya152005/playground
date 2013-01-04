package ch.rasc.maven.plugin.execwar.run;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.JasperListener;
import org.apache.catalina.core.JreMemoryLeakPreventionListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.ThreadLocalLeakPreventionListener;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.mbeans.GlobalResourcesLifecycleListener;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.yaml.snakeyaml.Yaml;

public class Runner {

	private static Tomcat tomcat;

	private static Config config;

	public static void main(String[] args) throws Exception {

		Path configFile = Paths.get("config.yaml");

		if (Files.exists(configFile)) {
			try (InputStream is = Files.newInputStream(configFile)) {
				Yaml yaml = new Yaml();
				config = yaml.loadAs(is, Config.class);
			}
		} else {
			config = new Config();
		}

		System.out.println(config);

		for (Map.Entry<String, Object> entry : config.getSystemProperties().entrySet()) {
			System.setProperty(entry.getKey(), entry.getValue().toString());
		}

		final Path extractDir = Paths.get("tc");

		boolean extractWar = true;

		if (Files.exists(extractDir)) {
			Path timestampFile = extractDir.resolve("EXECWAR_TIMESTAMP");
			if (Files.exists(timestampFile)) {
				byte[] extractTimestampBytes = Files.readAllBytes(timestampFile);
				String extractTimestamp = new String(extractTimestampBytes, StandardCharsets.UTF_8);

				String timestamp = null;
				try (InputStream is = Runner.class.getResourceAsStream("/EXECWAR_TIMESTAMP");
						ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

					copy(is, bos);
					timestamp = new String(bos.toByteArray(), StandardCharsets.UTF_8);
				}

				if (Long.valueOf(timestamp) <= Long.valueOf(extractTimestamp)) {
					extractWar = false;
				}

			}
		}

		Path loggingPropertyFile = extractDir.resolve("logging.properties");
		Path loggingDir = extractDir.resolve("logs");
		Path tempDir = extractDir.resolve("temp");
		final Path defaultWebxmlFile = extractDir.resolve("web.xml");

		if (extractWar) {
			if (Files.exists(extractDir)) {
				Files.walkFileTree(extractDir, new DeleteDirectory());
			}

			Files.createDirectories(extractDir);
			Files.createDirectory(tempDir);
			Files.createDirectory(loggingDir);

			CodeSource src = Runner.class.getProtectionDomain().getCodeSource();
			List<String> warList = new ArrayList<>();

			if (src != null) {
				URL jar = src.getLocation();
				ZipInputStream zip = new ZipInputStream(jar.openStream());
				ZipEntry ze = null;

				while ((ze = zip.getNextEntry()) != null) {
					String entryName = ze.getName();
					if (entryName.endsWith(".war")) {
						warList.add(entryName);
					}
				}
			}

			for (String war : warList) {
				Path warFile = extractDir.resolve(war);
				try (InputStream is = Runner.class.getResourceAsStream("/" + war)) {
					Files.copy(is, warFile);
				}
			}

			try (InputStream is = Runner.class.getResourceAsStream("/conf/web.xml")) {
				Files.copy(is, defaultWebxmlFile);
			}

			try (InputStream is = Runner.class.getResourceAsStream("/conf/logging.properties")) {
				Files.copy(is, loggingPropertyFile);
			}

			Path timestampFile = extractDir.resolve("EXECWAR_TIMESTAMP");
			try (InputStream is = Runner.class.getResourceAsStream("/EXECWAR_TIMESTAMP")) {
				Files.copy(is, timestampFile);
			}

		}

		List<String> warAbsolutePaths = new ArrayList<>();

		try (DirectoryStream<Path> wars = Files.newDirectoryStream(extractDir, "*.war")) {
			for (Path war : wars) {
				warAbsolutePaths.add(war.toAbsolutePath().toString());
			}
		}

		System.setProperty("java.io.tmpdir", tempDir.toAbsolutePath().toString());
		System.setProperty("log.dir", loggingDir.toAbsolutePath().toString());
		System.setProperty("java.util.logging.config.file", loggingPropertyFile.toAbsolutePath().toString());
		System.setProperty("java.util.logging.manager", "org.apache.juli.ClassLoaderLogManager");

		List<Connector> connectors = config.createConnectorObjects();
		for (Connector connector : connectors) {
			try {
				try (ServerSocket srv = new ServerSocket(connector.getPort())) {
					// nothing here
				}
			} catch (IOException e) {
				getLogger().severe("PORT " + connector.getPort() + " ALREADY IN USE");
				return;
			}
		}

		tomcat = new Tomcat() {

			@Override
			public Context addWebapp(@SuppressWarnings("hiding") Host host, String url, String name, String path) {
				Context ctx = new StandardContext();
				ctx.setName(name);
				ctx.setPath(url);
				ctx.setDocBase(path);

				ContextConfig ctxCfg = new ContextConfig();
				ctx.addLifecycleListener(ctxCfg);
				ctxCfg.setDefaultWebXml(defaultWebxmlFile.toAbsolutePath().toString());
				getHost().addChild(ctx);

				return ctx;
			}

		};

		tomcat.setBaseDir(extractDir.toAbsolutePath().toString());
		tomcat.setSilent(config.isSilent());

		for (String s : new String[] { "org.apache.coyote.http11.Http11NioProtocol",
				"org.apache.tomcat.util.net.NioSelectorPool", Runner.class.getName() }) {
			if (config.isSilent()) {
				Logger.getLogger(s).setLevel(Level.WARNING);
			} else {
				Logger.getLogger(s).setLevel(Level.INFO);
			}
		}

		// Create all server objects;
		tomcat.getHost();

		if (config.getListeners().contains(AprLifecycleListener.class.getName())) {
			tomcat.getServer().addLifecycleListener(new AprLifecycleListener());
		}
		if (config.getListeners().contains(JasperListener.class.getName())) {
			tomcat.getServer().addLifecycleListener(new JasperListener());
		}
		if (config.getListeners().contains(JreMemoryLeakPreventionListener.class.getName())) {
			tomcat.getServer().addLifecycleListener(new JreMemoryLeakPreventionListener());
		}
		if (config.getListeners().contains(ThreadLocalLeakPreventionListener.class.getName())) {
			tomcat.getServer().addLifecycleListener(new ThreadLocalLeakPreventionListener());
		}

		for (Connector connector : connectors) {
			tomcat.setConnector(connector);
			tomcat.getService().addConnector(connector);
		}

		if (config.getJvmRoute() != null && !config.getJvmRoute().isEmpty()) {
			tomcat.getEngine().setJvmRoute(config.getJvmRoute());
		}

		if (config.isEnableNaming()) {
			tomcat.enableNaming();

			if (config.getListeners().contains(GlobalResourcesLifecycleListener.class.getName())) {
				tomcat.getServer().addLifecycleListener(new GlobalResourcesLifecycleListener());
			}
		}

		// no context configured. add a default one
		if (config.getContexts().isEmpty()) {
			ch.rasc.maven.plugin.execwar.run.Context ctx = new ch.rasc.maven.plugin.execwar.run.Context();
			ctx.setContextPath("");
			ctx.setWar(warAbsolutePaths.iterator().next());
			config.setContexts(Collections.singletonList(ctx));
		}

		List<Context> contextsWithoutSessionPersistence = new ArrayList<>();
		for (ch.rasc.maven.plugin.execwar.run.Context configuredContext : config.getContexts()) {

			String contextPath = configuredContext.getContextPath();
			if (contextPath == null) {
				contextPath = "";
			}

			Context ctx = tomcat.addWebapp(contextPath, configuredContext.getWar());
			ctx.setSwallowOutput(true);

			for (ContextEnvironment env : configuredContext.getEnvironments()) {
				ctx.getNamingResources().addEnvironment(env);
			}

			for (ContextResource res : configuredContext.createContextResourceObjects()) {
				ctx.getNamingResources().addResource(res);
			}

			for (ApplicationParameter param : configuredContext.getParameters()) {
				ctx.addApplicationParameter(param);
			}

			if (configuredContext.getContextFile() != null) {
				Path contextFilePath = Paths.get(configuredContext.getContextFile());
				if (Files.exists(contextFilePath)) {
					try {
						URL contextFileURL = contextFilePath.toUri().toURL();
						ctx.setConfigFile(contextFileURL);
					} catch (Exception e) {
						getLogger().severe("Problem with the context file: " + e.getMessage());
					}
				}
			} else {
				URL contextFileURL = getContextXml(configuredContext.getWar());
				if (contextFileURL != null) {
					ctx.setConfigFile(contextFileURL);
				}
			}

			if (!configuredContext.isSessionPersistence()) {
				contextsWithoutSessionPersistence.add(ctx);
			}
		}

		tomcat.start();

		// Disable session persistence support
		for (Context ctx : contextsWithoutSessionPersistence) {
			((StandardManager) ctx.getManager()).setPathname(null);
		}

		tomcat.getServer().await();
	}

	private static URL getContextXml(String warPath) throws IOException {
		String urlStr = "jar:file:" + warPath + "!/META-INF/context.xml";
		URL url = new URL(urlStr);
		try (InputStream is = url.openConnection().getInputStream()) {
			if (is != null) {
				return url;
			}
		} catch (FileNotFoundException e) {
			// ignore this exception
		}

		return null;
	}

	private static Logger getLogger() {
		return Logger.getLogger(Runner.class.getName());
	}

	private static void copy(InputStream source, OutputStream sink) throws IOException {
		byte[] buf = new byte[8192];
		int n;
		while ((n = source.read(buf)) > 0) {
			sink.write(buf, 0, n);
		}
	}

	public static void stop(@SuppressWarnings("unused") String[] args) throws LifecycleException {
		tomcat.stop();
	}

}
