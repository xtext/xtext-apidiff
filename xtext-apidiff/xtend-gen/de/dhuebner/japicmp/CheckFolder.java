package de.dhuebner.japicmp;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import de.dhuebner.japicmp.FolderComparator;
import io.airlift.airline.SingleCommand;
import japicmp.cli.JApiCli;
import japicmp.config.Options;
import japicmp.exception.JApiCmpException;
import japicmp.model.AccessModifier;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;

@SuppressWarnings("all")
public class CheckFolder {
  public static void main(final String[] args) {
    try {
      final SingleCommand<JApiCli.Compare> singleCommand = SingleCommand.<JApiCli.Compare>singleCommand(JApiCli.Compare.class);
      final JApiCli.Compare cmd = singleCommand.parse(args);
      Properties _createDefaults = CheckFolder.createDefaults();
      final Properties properties = new Properties(_createDefaults);
      final File propsFile = new File("japicmp.properties");
      boolean _exists = propsFile.exists();
      if (_exists) {
        FileInputStream _fileInputStream = new FileInputStream(propsFile);
        properties.load(_fileInputStream);
      }
      final String from = properties.getProperty("old.version");
      final String to = properties.getProperty("new.version");
      String _property = properties.getProperty("docuName");
      boolean _equals = Objects.equal(_property, null);
      if (_equals) {
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("Xtext API Changes (");
        _builder.append(from);
        _builder.append(" - ");
        _builder.append(to);
        _builder.append(")");
        properties.setProperty("docuName", _builder.toString());
      }
      String _property_1 = properties.getProperty("old.location");
      boolean _equals_1 = Objects.equal(_property_1, null);
      if (_equals_1) {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("tmf-xtext-Update-");
        _builder_1.append(from);
        _builder_1.append("/plugins");
        properties.setProperty("old.location", _builder_1.toString());
      }
      String _property_2 = properties.getProperty("new.location");
      boolean _equals_2 = Objects.equal(_property_2, null);
      if (_equals_2) {
        StringConcatenation _builder_2 = new StringConcatenation();
        _builder_2.append("tmf-xtext-Update-");
        _builder_2.append(to);
        _builder_2.append("/plugins");
        properties.setProperty("new.location", _builder_2.toString());
      }
      String _property_3 = properties.getProperty("xmlOutputFile");
      boolean _equals_3 = Objects.equal(_property_3, null);
      if (_equals_3) {
        String _property_4 = properties.getProperty("htmlOutputFolder");
        String _plus = (_property_4 + "/report.xml");
        properties.setProperty("xmlOutputFile", _plus);
      }
      String _property_5 = properties.getProperty("htmlOutputFile");
      boolean _equals_4 = Objects.equal(_property_5, null);
      if (_equals_4) {
        String _property_6 = properties.getProperty("htmlOutputFolder");
        String _plus_1 = (_property_6 + "/plain-report.html");
        properties.setProperty("htmlOutputFile", _plus_1);
      }
      final Options options = CheckFolder.createOptions(cmd, properties);
      final FolderComparator checkFolder = new FolderComparator();
      checkFolder.createReport(options, properties);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public static Properties createDefaults() {
    final Properties properties = new Properties();
    properties.setProperty("package.exclude", "*.internal.*");
    properties.setProperty("package.include", "");
    properties.setProperty("old.version", "2.8.3");
    properties.setProperty("new.version", "2.9.0");
    properties.setProperty("cpLocation", "eclipse/plugins");
    properties.setProperty("checkOnlyJarsStartWith", "org.eclipse.xt");
    properties.setProperty("htmlOutputFolder", "output");
    return properties;
  }
  
  private static Options createOptions(final JApiCli.Compare it, final Properties properties) {
    Options options = new Options();
    boolean _notEquals = (!Objects.equal(it.pathToNewVersionJar, null));
    if (_notEquals) {
      File _file = new File(it.pathToNewVersionJar);
      options.setNewArchive(_file);
    }
    boolean _notEquals_1 = (!Objects.equal(it.pathToOldVersionJar, null));
    if (_notEquals_1) {
      File _file_1 = new File(it.pathToOldVersionJar);
      options.setOldArchive(_file_1);
    }
    options.setXmlOutputFile(Optional.<String>fromNullable(it.pathToXmlOutputFile));
    options.setHtmlOutputFile(Optional.<String>fromNullable(it.pathToHtmlOutputFile));
    options.setOutputOnlyModifications(true);
    options.setAccessModifier(CheckFolder.toModifier(it.accessModifier));
    options.addIncludeFromArgument(Optional.<String>fromNullable(it.includes));
    options.addExcludeFromArgument(Optional.<String>fromNullable(it.excludes));
    options.setOutputOnlyBinaryIncompatibleModifications(it.onlyBinaryIncompatibleModifications);
    options.setIncludeSynthetic(it.includeSynthetic);
    final String packageExclude = properties.getProperty("package.exclude");
    final String packageInclude = properties.getProperty("package.include");
    String[] _split = packageExclude.split(",");
    for (final String pattern : _split) {
      options.addExcludeFromArgument(Optional.<String>of(pattern));
    }
    String[] _split_1 = packageInclude.split(",");
    for (final String pattern_1 : _split_1) {
      options.addIncludeFromArgument(Optional.<String>of(pattern_1));
    }
    options.setXmlOutputFile(Optional.<String>of(properties.getProperty("xmlOutputFile")));
    options.setHtmlOutputFile(Optional.<String>of(properties.getProperty("htmlOutputFile")));
    return options;
  }
  
  private static Optional<AccessModifier> toModifier(final String accessModifierArg) {
    Optional<String> stringOptional = Optional.<String>fromNullable(accessModifierArg);
    boolean _isPresent = stringOptional.isPresent();
    if (_isPresent) {
      try {
        return Optional.<AccessModifier>of(AccessModifier.valueOf(stringOptional.get().toUpperCase()));
      } catch (final Throwable _t) {
        if (_t instanceof IllegalArgumentException) {
          String _format = String.format("Invalid value for option -a: %s. Possible values are: %s.", accessModifierArg, 
            AccessModifier.listOfAccessModifier());
          throw new JApiCmpException(JApiCmpException.Reason.CliError, _format);
        } else {
          throw Exceptions.sneakyThrow(_t);
        }
      }
    } else {
      return Optional.<AccessModifier>of(AccessModifier.PROTECTED);
    }
  }
}
