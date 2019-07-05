package de.dhuebner.japicmp;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import de.dhuebner.japicmp.MultiPageHtmlReport;
import de.dhuebner.japicmp.ReporterInformation;
import japicmp.cmp.JarArchiveComparatorExt;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.exception.JApiCmpException;
import japicmp.model.JApiClass;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javassist.ClassPool;
import javassist.CtClass;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class FolderComparator {
  public String createReport(final Options options, final Properties properties) {
    try {
      String _xblockexpression = null;
      {
        InputOutput.<String>println("Properties used:");
        properties.list(System.out);
        String _property = properties.getProperty("old.location");
        final File oldVersion = new File(_property);
        String _property_1 = properties.getProperty("new.location");
        final File newVersion = new File(_property_1);
        String _property_2 = properties.getProperty("cpLocation");
        final File cpFolder = new File(_property_2);
        final FileFilter _function = (File it) -> {
          return (it.getName().endsWith(".jar") && it.getName().startsWith(properties.getProperty("checkOnlyJarsStartWith")));
        };
        final FileFilter xtextFilter = _function;
        String _property_3 = properties.getProperty("htmlOutputFolder");
        final File outputFolder = new File(_property_3);
        outputFolder.mkdirs();
        final Stopwatch watch = Stopwatch.createStarted();
        InputOutput.<String>println("Compare Started");
        final JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(options);
        Path _path = FileSystems.getDefault().getPath(cpFolder.getAbsolutePath());
        EnumSet<FileVisitOption> _noneOf = EnumSet.<FileVisitOption>noneOf(FileVisitOption.class);
        Files.walkFileTree(_path, _noneOf, 3, 
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
              boolean _endsWith = file.getFileName().toString().endsWith(".jar");
              if (_endsWith) {
                comparatorOptions.getClassPathEntries().add(file.toString());
              }
              return super.visitFile(file, attrs);
            }
          });
        String _plus = (watch + " Collecting files");
        InputOutput.<String>println(_plus);
        JarArchiveComparatorExt jarArchiveComparator = new JarArchiveComparatorExt(comparatorOptions);
        List<CtClass> oldClasses = Lists.<CtClass>newArrayList();
        List<CtClass> newClasses = Lists.<CtClass>newArrayList();
        File[] _listFiles = oldVersion.listFiles(xtextFilter);
        for (final File file : _listFiles) {
          oldClasses.addAll(this.createListOfCtClasses(file, jarArchiveComparator.getClassPool(), comparatorOptions));
        }
        File[] _listFiles_1 = newVersion.listFiles(xtextFilter);
        for (final File file_1 : _listFiles_1) {
          newClasses.addAll(this.createListOfCtClasses(file_1, jarArchiveComparator.getClassPool(), comparatorOptions));
        }
        int _size = oldClasses.size();
        String _plus_1 = ("Old version classes: " + Integer.valueOf(_size));
        InputOutput.<String>println(_plus_1);
        int _size_1 = newClasses.size();
        String _plus_2 = ("New version classes: " + Integer.valueOf(_size_1));
        InputOutput.<String>println(_plus_2);
        String _plus_3 = (watch + " Collecting classes");
        InputOutput.<String>println(_plus_3);
        final List<JApiClass> compareResult = jarArchiveComparator.compareClassLists(comparatorOptions, oldClasses, newClasses);
        int _size_2 = compareResult.size();
        String _plus_4 = ("Compare results: " + Integer.valueOf(_size_2));
        InputOutput.<String>println(_plus_4);
        String _plus_5 = (watch + " Analyse finished");
        InputOutput.<String>println(_plus_5);
        ReporterInformation _reporterInformation = new ReporterInformation();
        final Procedure1<ReporterInformation> _function_1 = (ReporterInformation it) -> {
          it.setDocumentationName(properties.getProperty("docuName"));
          it.setOutputFolder(outputFolder.getAbsolutePath());
        };
        final ReporterInformation info = ObjectExtensions.<ReporterInformation>operator_doubleArrow(_reporterInformation, _function_1);
        String _absolutePath = oldVersion.getAbsolutePath();
        String _absolutePath_1 = newVersion.getAbsolutePath();
        MultiPageHtmlReport htmlGenerator = new MultiPageHtmlReport(info, _absolutePath, _absolutePath_1, compareResult, options);
        htmlGenerator.generate();
        String _absolutePath_2 = outputFolder.getAbsolutePath();
        String _plus_6 = ("Output in: " + _absolutePath_2);
        InputOutput.<String>println(_plus_6);
        String _plus_7 = (watch + " Finished...");
        _xblockexpression = InputOutput.<String>println(_plus_7);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private List<CtClass> createListOfCtClasses(final File archive, final ClassPool classPool, final JarArchiveComparatorOptions options) {
    List<CtClass> classes = new LinkedList<CtClass>();
    try {
      final JarFile jarFile = new JarFile(archive);
      Enumeration<JarEntry> entryEnumeration = jarFile.entries();
      while (entryEnumeration.hasMoreElements()) {
        {
          JarEntry jarEntry = entryEnumeration.nextElement();
          String name = jarEntry.getName();
          boolean _endsWith = name.endsWith(".class");
          if (_endsWith) {
            CtClass ctClass = null;
            try {
              ctClass = classPool.makeClass(jarFile.getInputStream(jarEntry));
            } catch (final Throwable _t) {
              if (_t instanceof Exception) {
                final Exception e = (Exception)_t;
                String _format = String.format("Failed to load file from jar \'%s\' as class file: %s.", name, e.getMessage());
                throw new JApiCmpException(JApiCmpException.Reason.IoException, _format, e);
              } else {
                throw Exceptions.sneakyThrow(_t);
              }
            }
            classes.add(ctClass);
          } else {
          }
        }
      }
    } catch (final Throwable _t) {
      if (_t instanceof IOException) {
        final IOException e = (IOException)_t;
        String _format = String.format("Processing of jar file %s failed: %s", archive.getAbsolutePath(), e.getMessage());
        throw new JApiCmpException(JApiCmpException.Reason.IoException, _format, e);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    return classes;
  }
}
