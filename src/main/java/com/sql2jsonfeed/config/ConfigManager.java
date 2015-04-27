package com.sql2jsonfeed.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sql2jsonfeed.channel.ChannelManager;
import com.sql2jsonfeed.definition.ChannelDefinition;
import com.sql2jsonfeed.definition.DatasourceDefinition;
import com.sql2jsonfeed.definition.DomainDefinition;
import com.sql2jsonfeed.definition.TypeDefinition;
import com.sql2jsonfeed.sql.SqlTemplates;

/**
 * Loads and manages config and definition files
 * 
 * @author Catalin
 *
 */
public class ConfigManager {

	private final Path homeFolder;
	private final Path configFolder;

	private String configFileName = "elasticchannel.yaml";
	
	public static final String ES_CONFIG_INDEX = "ecconfig_";
	public static final String ES_CONFIG_TYPE = "channels_";
	
	/**
	 * Main config file.
	 */
	private Config config = null;

	// List of managers for each channel defined in the config file.
	private ArrayList<ChannelManager> channelManagers = new ArrayList<ChannelManager>();

	public ConfigManager() {
		super();

		if (System.getProperty("ec.path.home") != null) {
			homeFolder = Paths.get(System.getProperty("ec.path.home"));
		} else {
			homeFolder = Paths.get(System.getProperty("user.dir"));
		}

		if (System.getProperty("ec.path.config") != null) {
			configFolder = Paths.get(System.getProperty("ec.path.config"));
		} else {
			configFolder = homeFolder.resolve("config");
		}

		if (System.getProperty("ec.filename.config") != null) {
			configFileName = System.getProperty("ec.filename.config");
		}
	}

	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	/**
	 * Load the main config file and all domain files. It throws ConfigException
	 * in case of any error.
	 * 
	 * @throws ConfigException
	 */
	public void setup() {
		// 1. Main config
		loadConfig();

		// 2. Domain definitions
		Map<String, ChannelDefinition> channels = config.getChannels();
		// At this point they are already validated.

		for (Map.Entry<String, ChannelDefinition> channelEntry : channels
				.entrySet()) {

			ChannelDefinition channelDefinition = channelEntry.getValue();
			String filePath = channelDefinition.getDomainDefinitionFile();

			if (StringUtils.isEmpty(filePath)) {
				// Defaults to ES Type value for yaml extension
				filePath = channelDefinition.getEsType() + ".yaml";
			}

			DomainDefinition domainDefinition = loadDomainDefinition(filePath);
			ChannelManager channelManager = new ChannelManager(
					channelEntry.getKey(), this, channelDefinition,
					domainDefinition);

			// TODO remove later
			System.out.println(channelManager);

			channelManagers.add(channelManager);
		}
	}

	public ArrayList<ChannelManager> getChannelManagers() {
		return channelManagers;
	}

	/**
	 * Load main config file and the other files.
	 */
	public void loadConfig() {
		// TODO log here

		// Config file first
		URL configFileUrl = resolveConfigFile(configFileName);

		try {
			config = mapper.readValue(configFileUrl, Config.class);
		} catch (IOException e) {
			throw new ConfigException("Unable to load config file: "
					+ configFileUrl, e);
		}

		// TODO Log4J
		System.out.println(config);

		// Validate the file content
		config.validate();
	}

	private DomainDefinition loadDomainDefinition(String filePath) {
		URL domainFileUrl = resolveConfigFile(filePath);
		LinkedHashMap<String, TypeDefinition> typesMap = null;

		try {
			// Parse YAML domain file
			typesMap = mapper.readValue(domainFileUrl,
					new TypeReference<LinkedHashMap<String, TypeDefinition>>() {
					});
		} catch (IOException e) {
			throw new ConfigException("Unable to load domain definition file: "
					+ domainFileUrl, e);
		}

		DomainDefinition domainDefinition = new DomainDefinition(typesMap);

		// TODO Log4J
		System.out.println(domainDefinition);

		return domainDefinition;
	}

	/**
	 * Try to resolve the file path by looking into various folders or classpath
	 * 
	 * @param filePath
	 * @return an URL object if file could be found or NULL otherwise
	 */
	public URL resolveConfigFile(String filePath) {
		String origFilePath = filePath;

		// first, try it as a path on the file system
		Path path1 = Paths.get(filePath);
		if (Files.exists(path1)) {
			try {
				return path1.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new ConfigException("Failed to resolve path [" + path1
						+ "]", e);
			}
		}

		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		// next, try it relative to the config location
		Path path2 = configFolder.resolve(filePath);
		if (Files.exists(path2)) {
			try {
				return path2.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new ConfigException(
						"Failed to resolve path [" + path2 + "]", e);
			}
		}
		// try and load it from the classpath directly
		URL resource = ClassLoader.getSystemClassLoader().getResource(filePath);
		if (resource != null) {
			return resource;
		}
		// try and load it from the classpath with config/ prefix
		if (!filePath.startsWith("config/")) {
			resource = ClassLoader.getSystemClassLoader().getResource(
					"config/" + filePath);
			if (resource != null) {
				return resource;
			}
		}
		throw new ConfigException("Failed to resolve config path ["
				+ origFilePath + "], tried file path [" + path1
				+ "], path file [" + path2 + "], and classpath");
	}

	/**
	 * Lookup the configure SQl templates for the input SQL driver
	 * @param driverClassName
	 * @return
	 */
	public SqlTemplates getSqlTemplates(String driverClassName) {
		SqlTemplates sqlTemplates = config.getSqlTemplatesMap().get(driverClassName);
		if (sqlTemplates == null) {
			sqlTemplates = SqlTemplates.DEFAULT;
		}
		return sqlTemplates;
	}
	
	public DatasourceDefinition getDatasourceDef(String dsName) {
		return config.getDatasourceMap().get(dsName);
	}

	public int getDefaultBatchSize() {
		return config.getBatchSize();
	}

	public String getDefaultEsClusterName() {
		return config.getClusterName();
	}

	@Override
	public String toString() {
		return "ConfigManager [homeFolder=" + homeFolder + ", configFolder="
				+ configFolder + ", configFileName=" + configFileName
				+ ", config=" + config + ", channelManagers=" + channelManagers
				+ "]";
	}
}
