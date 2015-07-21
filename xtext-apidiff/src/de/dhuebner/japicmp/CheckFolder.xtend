package de.dhuebner.japicmp

import com.google.common.base.Optional
import io.airlift.airline.SingleCommand
import japicmp.cli.JApiCli
import japicmp.config.Options
import japicmp.exception.JApiCmpException
import japicmp.model.AccessModifier
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class CheckFolder {

	def static void main(String[] args) {
		val SingleCommand<JApiCli.Compare> singleCommand = SingleCommand.singleCommand(JApiCli.Compare);
		val JApiCli.Compare cmd = singleCommand.parse(args);

		val properties = new Properties(createDefaults())
		val propsFile = new File("japicmp.properties")
		if (propsFile.exists)
			properties.load(new FileInputStream(propsFile))

		val from = properties.getProperty("old.version")
		val to = properties.getProperty("new.version")
		if (properties.getProperty("docuName") == null) {
			properties.setProperty("docuName", '''Xtext API Changes («from» - «to»)''')
		}
		if (properties.getProperty("old.location") == null) {
			properties.setProperty("old.location", '''tmf-xtext-Update-«from»/plugins''')
		}
		if (properties.getProperty("new.location") == null) {
			properties.setProperty("new.location", '''tmf-xtext-Update-«to»/plugins''')
		}
		if (properties.getProperty("xmlOutputFile") == null) {
			properties.setProperty("xmlOutputFile", properties.getProperty("htmlOutputFolder") + "/report.xml")
		}
		if (properties.getProperty("htmlOutputFile") == null) {
			properties.setProperty("htmlOutputFile", properties.getProperty("htmlOutputFolder") + "/plain-report.html")
		}
		val options = createOptions(cmd, properties)
		val checkFolder = new FolderComparator()
		checkFolder.createReport(options, properties)
	}

	def static createDefaults() {
		val properties = new Properties
		properties.setProperty("package.exclude", "*.internal.*")
		properties.setProperty("package.include", "")
		properties.setProperty("old.version", "2.8.3")
		properties.setProperty("new.version", "2.9.0")
		properties.setProperty("cpLocation", "eclipse/plugins")
		properties.setProperty("checkOnlyJarsStartWith", "org.eclipse.xt")
		properties.setProperty("htmlOutputFolder", "output")
		return properties
	}

	def private static Options createOptions(JApiCli.Compare it, Properties properties) {
		var Options options = new Options()
		if (pathToNewVersionJar != null)
			options.setNewArchive(new File(pathToNewVersionJar))
		if (pathToOldVersionJar != null) {
			options.setOldArchive(new File(pathToOldVersionJar))
		}
		options.setXmlOutputFile(Optional.fromNullable(pathToXmlOutputFile))
		options.setHtmlOutputFile(Optional.fromNullable(pathToHtmlOutputFile))
		options.setOutputOnlyModifications(true)
		options.setAccessModifier(toModifier(accessModifier))
		options.addIncludeFromArgument(Optional.fromNullable(includes))
		options.addExcludeFromArgument(Optional.fromNullable(excludes))
		options.setOutputOnlyBinaryIncompatibleModifications(onlyBinaryIncompatibleModifications)
		options.setIncludeSynthetic(includeSynthetic)
		val packageExclude = properties.getProperty("package.exclude")
		val packageInclude = properties.getProperty("package.include")
		for (pattern : packageExclude.split(",")) {
			options.addExcludeFromArgument(Optional.of(pattern))
		}
		for (pattern : packageInclude.split(",")) {
			options.addIncludeFromArgument(Optional.of(pattern))
		}
		options.xmlOutputFile = Optional.of(properties.getProperty("xmlOutputFile"))
		options.htmlOutputFile = Optional.of(properties.getProperty("htmlOutputFile"))
		return options
	}

	def private static Optional<AccessModifier> toModifier(String accessModifierArg) {
		var Optional<String> stringOptional = Optional.fromNullable(accessModifierArg)
		if (stringOptional.isPresent()) {
			try {
				return Optional.of(AccessModifier.valueOf(stringOptional.get().toUpperCase()))
			} catch (IllegalArgumentException e) {
				throw new JApiCmpException(JApiCmpException.Reason.CliError,
					String.format("Invalid value for option -a: %s. Possible values are: %s.", accessModifierArg,
						AccessModifier.listOfAccessModifier()))
			}

		} else {
			return Optional.of(AccessModifier.PROTECTED)
		}
	}

}