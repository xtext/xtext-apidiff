package de.dhuebner.japicmp;

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

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import japicmp.cmp.JarArchiveComparatorExt;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.exception.JApiCmpException;
import japicmp.model.JApiClass;
import javassist.ClassPool;
import javassist.CtClass;

public class FolderComparator {
	public void createReport(Options options, Properties properties) throws IOException {
		System.out.println("Properties used:");
		properties.list(System.out);
		File oldVersion = new File(properties.getProperty("old.location"));
		File newVersion = new File(properties.getProperty("new.location"));
		File cpFolder = new File(properties.getProperty("cpLocation"));
		FileFilter xtextFilter = (File it) -> {
			return it.getName().endsWith(".jar")
					&& it.getName().startsWith(properties.getProperty("checkOnlyJarsStartWith"));
		};
		File outputFolder = new File(properties.getProperty("htmlOutputFolder"));
		outputFolder.mkdirs();
		Stopwatch watch = Stopwatch.createStarted();
		System.out.println("Compare Started");
		JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(options);
		Files.walkFileTree(FileSystems.getDefault().getPath(cpFolder.getAbsolutePath()),
				EnumSet.noneOf(FileVisitOption.class), 3, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.getFileName().toString().endsWith(".jar")) {
							comparatorOptions.getClassPathEntries().add(file.toString());
						}
						return super.visitFile(file, attrs);
					}
				});
		System.out.println(watch + " Collecting files");
		JarArchiveComparatorExt jarArchiveComparator = new JarArchiveComparatorExt(comparatorOptions);
		List<CtClass> oldClasses = Lists.<CtClass>newArrayList();
		List<CtClass> newClasses = Lists.<CtClass>newArrayList();
		for (File f : oldVersion.listFiles(xtextFilter)) {
			oldClasses.addAll(createListOfCtClasses(f, jarArchiveComparator.getClassPool(), comparatorOptions));
		}
		for (File f : newVersion.listFiles(xtextFilter)) {
			newClasses.addAll(createListOfCtClasses(f, jarArchiveComparator.getClassPool(), comparatorOptions));
		}
		System.out.println("Old version classes: " + oldClasses.size());
		System.out.println("New version classes: " + newClasses.size());
		System.out.println(watch + " Collecting classes");
		List<JApiClass> compareResult = jarArchiveComparator.compareClassLists(comparatorOptions, oldClasses,
				newClasses);
		System.out.println("Compare results: " + compareResult.size());
		System.out.println(watch + " Analyse finished");
		ReporterInformation info = new ReporterInformation();
		info.setDocumentationName(properties.getProperty("docuName"));
		info.setOutputFolder(outputFolder.getAbsolutePath());
		MultiPageHtmlReport htmlGenerator = new MultiPageHtmlReport(info, oldVersion.getAbsolutePath(),
				newVersion.getAbsolutePath(), compareResult, options);
		htmlGenerator.generate();
		System.out.println("Output in: " + outputFolder.getAbsolutePath());
		System.out.println(watch + " Finished...");
	}

	private List<CtClass> createListOfCtClasses(File archive, ClassPool classPool,
			JarArchiveComparatorOptions options) {
		List<CtClass> classes = new LinkedList<CtClass>();
		try (JarFile jarFile = new JarFile(archive)) {
			Enumeration<JarEntry> entryEnumeration = jarFile.entries();
			while (entryEnumeration.hasMoreElements()) {
				JarEntry jarEntry = entryEnumeration.nextElement();
				String name = jarEntry.getName();
				if (name.endsWith(".class")) {
					CtClass ctClass = null;
					try {
						ctClass = classPool.makeClass(jarFile.getInputStream(jarEntry));
					} catch (Exception e) {
						throw new JApiCmpException(JApiCmpException.Reason.IoException, String.format(
								"Failed to load file from jar '%s' as class file: %s.", name, e.getMessage()), e);
					}
					classes.add(ctClass);
				}
			}
		} catch (IOException e) {
			throw new JApiCmpException(JApiCmpException.Reason.IoException,
					String.format("Processing of jar file %s failed: %s", archive.getAbsolutePath(), e.getMessage()),
					e);
		}
		return classes;
	}
}
