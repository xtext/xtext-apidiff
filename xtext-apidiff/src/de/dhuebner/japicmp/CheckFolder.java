package de.dhuebner.japicmp;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import japicmp.cli.CliParser;
import japicmp.config.Options;
import japicmp.exception.JApiCmpException;
import japicmp.model.AccessModifier;
import japicmp.util.Optional;

public class CheckFolder {
	public static void main(String[] args) throws Exception {
		Options cmd = Options.newDefault();
		Properties properties = new Properties(CheckFolder.createDefaults());
		File propsFile = new File("japicmp.properties");
		if (propsFile.exists()) {
			properties.load(new FileInputStream(propsFile));
		}
		String from = properties.getProperty("old.version");
		String to = properties.getProperty("new.version");
		if (properties.getProperty("docuName") == null) {
			properties.setProperty("docuName", "Xtext API Changes (" + from + " - " + to + ")");
		}
		if (properties.getProperty("old.location") == null) {
			properties.setProperty("old.location", "tmf-xtext-Update-" + from + "/plugins");
		}
		if (properties.getProperty("new.location") == null) {
			properties.setProperty("new.location", "tmf-xtext-Update-" + to + "/plugins");
		}
		if (properties.getProperty("xmlOutputFile") == null) {
			properties.setProperty("xmlOutputFile", properties.getProperty("htmlOutputFolder") + "/report.xml");
		}
		if (properties.getProperty("htmlOutputFile") == null) {
			properties.setProperty("htmlOutputFile", properties.getProperty("htmlOutputFolder") + "/plain-report.html");
		}
		Options options = CheckFolder.createOptions(cmd, properties);
		FolderComparator checkFolder = new FolderComparator();
		checkFolder.createReport(options, properties);
	}

	public static Properties createDefaults() {
		Properties properties = new Properties();
		properties.setProperty("package.exclude", "*.internal.*");
		properties.setProperty("package.include", "");
		properties.setProperty("old.version", "2.8.3");
		properties.setProperty("new.version", "2.9.0");
		properties.setProperty("cpLocation", "eclipse/plugins");
		properties.setProperty("checkOnlyJarsStartWith", "org.eclipse.xt");
		properties.setProperty("htmlOutputFolder", "output");
		return properties;
	}

	private static Options createOptions(Options options, Properties properties) {
		options.setOutputOnlyModifications(true);
		String packageExclude = properties.getProperty("package.exclude");
		String packageInclude = properties.getProperty("package.include");
		for (String pattern : packageExclude.split(",")) {
			options.addExcludeFromArgument(Optional.of(pattern), false);
		}
		for (String pattern : packageInclude.split(",")) {
			options.addIncludeFromArgument(Optional.of(pattern), false);
		}
		options.setXmlOutputFile(Optional.of(properties.getProperty("xmlOutputFile")));
		options.setHtmlOutputFile(Optional.of(properties.getProperty("htmlOutputFile")));
		return options;
	}

}
