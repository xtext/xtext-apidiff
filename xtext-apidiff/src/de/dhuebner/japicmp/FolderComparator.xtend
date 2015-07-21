package de.dhuebner.japicmp

import com.google.common.base.Stopwatch
import com.google.common.collect.Lists
import japicmp.cmp.JarArchiveComparatorExt
import japicmp.cmp.JarArchiveComparatorOptions
import japicmp.config.Options
import japicmp.exception.JApiCmpException
import japicmp.exception.JApiCmpException.Reason
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.EnumSet
import java.util.Enumeration
import java.util.LinkedList
import java.util.List
import java.util.Properties
import java.util.jar.JarEntry
import java.util.jar.JarFile
import javassist.ClassPool
import javassist.CtClass

class FolderComparator {

	def createReport(Options options, Properties properties) {
		println("Properties used:")
		properties.list(System.out)

		val oldVersion = new File(properties.getProperty("old.location"))
		val newVersion = new File(properties.getProperty("new.location"))
		val cpFolder = new File(properties.getProperty("cpLocation"))
		val FileFilter xtextFilter = [
			name.endsWith(".jar") && name.startsWith(properties.getProperty("checkOnlyJarsStartWith"))
		]
		val outputFolder = new File(properties.getProperty("htmlOutputFolder"))
		outputFolder.mkdirs

		val watch = Stopwatch.createStarted
		println("Compare Started")
		val comparatorOptions = JarArchiveComparatorOptions.of(options)
		Files.walkFileTree(FileSystems.^default.getPath(cpFolder.absolutePath), EnumSet.noneOf(FileVisitOption), 3,
			new SimpleFileVisitor<Path>() {

				override visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.fileName.toString.endsWith(".jar")) {
						comparatorOptions.classPathEntries.add(file.toString)
					}
					return super.visitFile(file, attrs)
				}

			})
		println(watch + " Collecting files")
		var jarArchiveComparator = new JarArchiveComparatorExt(comparatorOptions)
		var List<CtClass> oldClasses = Lists.newArrayList
		var List<CtClass> newClasses = Lists.newArrayList

		for (file : oldVersion.listFiles(xtextFilter)) {
			oldClasses.addAll(createListOfCtClasses(file, jarArchiveComparator.classPool, comparatorOptions))
		}
		for (file : newVersion.listFiles(xtextFilter)) {
			newClasses.addAll(createListOfCtClasses(file, jarArchiveComparator.classPool, comparatorOptions))
		}
		println("Old version classes: " + oldClasses.size)
		println("New version classes: " + newClasses.size)
		println(watch + " Collecting classes")
		val compareResult = jarArchiveComparator.compareClassLists(comparatorOptions, oldClasses, newClasses)
		println("Compare results: " + compareResult.size)
		println(watch + " Analyse finished")

		val info = new ReporterInformation() => [
			documentationName = properties.getProperty("docuName")
			outputFolder = outputFolder.absolutePath
		]
		var htmlGenerator = new MultiPageHtmlReport(info, oldVersion.absolutePath, newVersion.getAbsolutePath(),
			compareResult, options)
		htmlGenerator.generate()
		println("Output in: " + outputFolder.absolutePath)
		println(watch + " Finished...")
	}

	def private List<CtClass> createListOfCtClasses(File archive, ClassPool classPool,
		JarArchiveComparatorOptions options) {
		var List<CtClass> classes = new LinkedList<CtClass>()
		try {
			val JarFile jarFile = new JarFile(archive)
			var Enumeration<JarEntry> entryEnumeration = jarFile.entries()
			while (entryEnumeration.hasMoreElements()) {
				var JarEntry jarEntry = entryEnumeration.nextElement()
				var String name = jarEntry.getName()
				if (name.endsWith(".class")) {
					var CtClass ctClass
					try {
						ctClass = classPool.makeClass(jarFile.getInputStream(jarEntry))
					} catch (Exception e) {
						throw new JApiCmpException(Reason.IoException,
							String.format("Failed to load file from jar '%s' as class file: %s.", name, e.getMessage()),
							e)
						}
						classes.add(ctClass)

					} else {
					}
				}

			} catch (IOException e) {
				throw new JApiCmpException(Reason.IoException,
					String.format("Processing of jar file %s failed: %s", archive.getAbsolutePath(), e.getMessage()), e)
			}
			return classes
		}

	}