package de.dhuebner.japicmp

import japicmp.config.Options
import japicmp.model.AccessModifier
import japicmp.model.JApiAnnotation
import japicmp.model.JApiAnnotationElement
import japicmp.model.JApiChangeStatus
import japicmp.model.JApiClass
import japicmp.model.JApiCompatibility
import japicmp.model.JApiHasChangeStatus
import japicmp.model.JApiModifier
import japicmp.model.JApiParameter
import japicmp.model.JApiReturnType
import japicmp.output.xml.XmlOutputGenerator
import japicmp.output.xml.XmlOutputGeneratorOptions
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.List
import java.util.Map

class MultiPageHtmlReport extends XmlOutputGenerator {

	static enum MenuKind {
		OVERVIEW,
		REMOVED,
		ADDED,
		BREAKING
	}

	ReporterInformation info

	new(List<JApiClass> jApiClasses, Options options, XmlOutputGeneratorOptions xmlOptions) {
		super(jApiClasses, options, xmlOptions)
	}

	new(ReporterInformation info, List<JApiClass> jApiClasses,
		Options options, XmlOutputGeneratorOptions xmlOptions) {
		this(jApiClasses, options, xmlOptions)
		this.info = info;
	}

	override generate() {
		// need to call super to get a filtered list of jApiClasses
		val result = super.generate()

		val outputFolder = new File(info.outputFolder);
		copyResources(outputFolder)

		val byPackage = jApiClasses.groupBy[newClass.or(oldClass).get.packageName]

		// main frameset
		new PrintWriter(outputFolder + "/changes.html").append(createStartPage()).close

		new PrintWriter(outputFolder + "/package-overview.html").append(createMenu(byPackage, MenuKind.OVERVIEW, [
			changeStatus == JApiChangeStatus.MODIFIED
		])).close

		new PrintWriter(outputFolder + "/removed-overview.html").append(createMenu(byPackage, MenuKind.REMOVED, [
			changeStatus == JApiChangeStatus.REMOVED
		])).close

		new PrintWriter(outputFolder + "/added-overview.html").append(createMenu(byPackage, MenuKind.ADDED, [
			changeStatus == JApiChangeStatus.NEW
		])).close

		new PrintWriter(outputFolder + "/breaking-overview.html").append(createMenu(byPackage, MenuKind.BREAKING, [
			!binaryCompatible
		])).close

		new PrintWriter(outputFolder + "/statistics.html").append(createStatistics(byPackage)).close

		new File(outputFolder, "packages").mkdirs

		for (packageName : byPackage.keySet) {
			val writer = new PrintWriter(outputFolder + "/packages/" + packageName + ".html")
			writer.append(createPackageSiteContent(packageName, byPackage)).close
		}
		return result
	}

	def copyResources(File outputRoot) {
		val resourcesOutFolder = new File(outputRoot, "resources")
		resourcesOutFolder.mkdirs
		resourcesOutFolder.copyResourceUsingClassloder("stylesheet.css")
		resourcesOutFolder.copyResourceUsingClassloder("background.gif")
		resourcesOutFolder.copyResourceUsingClassloder("tab.gif")
		resourcesOutFolder.copyResourceUsingClassloder("titlebar.gif")
		resourcesOutFolder.copyResourceUsingClassloder("titlebar_end.gif")
	}

	def copyResourceUsingClassloder(File destFolder, String fileName) {
		Files.copy(class.classLoader.getResourceAsStream("html/" + fileName), new File(destFolder, fileName).toPath,
			StandardCopyOption.REPLACE_EXISTING)
	}

	def createStartPage() {
		'''
			<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Frameset//EN""http://www.w3.org/TR/REC-html40/frameset.dtd">
			<HTML>
			<HEAD>
			<TITLE>«info.documentationName»</TITLE>
			</HEAD>
			<FRAMESET COLS="25%,75%">
				<FRAME SRC="package-overview.html" SCROLLING="auto" NAME="leftframe">
				<FRAME SRC="statistics.html"
					SCROLLING="auto" NAME="rightframe">
			</FRAMESET>
			<NOFRAMES>
				<H2>Frame Alert</H2>
			</NOFRAMES>
			</HTML>
		'''
	}

	def createMenu(Map<String, List<JApiClass>> byPackage, MenuKind menuKind, (JApiClass)=>Boolean filter) {
		val depth = "./"
		'''
			<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
			<!-- NewPage -->
			<html>
			<head>
			<title>Overview («info.documentationName»)</title>
			<link rel="stylesheet" type="text/css" href="«depth»resources/stylesheet.css" title="Style">
			</head>
			<body>
			<!-- ========= START OF TOP NAVBAR ======= -->
			<div class="topNav"><a name="navbar_top">
			<!--   -->
			</a><a href="#skip-navbar_top" title="Skip navigation links"></a><a name="navbar_top_firstrow">
			<!--   -->
			</a>
			<ul class="navList" title="Navigation">
			«IF menuKind == MenuKind.REMOVED»
				<li><a href="package-overview.html" target="leftframe">Overview</a></li>
				<li class="navBarCell1Rev">Removed</li>
				<li><a href="added-overview.html" target="leftframe">Added</a></li>
				<li><a href="breaking-overview.html" target="leftframe">Critical</a></li>
			«ELSEIF menuKind == MenuKind.ADDED»
				<li><a href="package-overview.html" target="leftframe">Overview</a></li>
				<li><a href="removed-overview.html" target="leftframe">Removed</a></li>
				<li class="navBarCell1Rev">Added</li>
				<li><a href="breaking-overview.html" target="leftframe">Critical</a></li>
			«ELSEIF menuKind == MenuKind.OVERVIEW»
				<li class="navBarCell1Rev">Overview</li>
				<li><a href="removed-overview.html" target="leftframe">Removed</a></li>
				<li><a href="added-overview.html" target="leftframe">Added</a></li>
				<li><a href="breaking-overview.html" target="leftframe">Critical</a></li>
			«ELSEIF menuKind == MenuKind.BREAKING»
				<li><a href="package-overview.html" target="leftframe">Overview</a></li>
				<li><a href="removed-overview.html" target="leftframe">Removed</a></li>
				<li><a href="added-overview.html" target="leftframe">Added</a></li>
				<li class="navBarCell1Rev">Critical</li>
			«ENDIF»
			<li><a href="statistics.html" target="rightframe">Statistic</a></li>
			</ul>
			</div>
			<div class="subNav">
			<ul class="navList">
			</ul>
			<ul class="navList" id="allclasses_navbar_top">
			</ul>
			<div>
			</div>
			<a name="skip-navbar_top">
			<!--   -->
			</a></div>
			<!-- ========= END OF TOP NAVBAR ========= -->
			<div class="contentContainer">
			<table class="overviewSummary" border="0" cellpadding="3" cellspacing="0" summary="Packages table, listing packages, and an explanation">
			<tr>
			<th class="colOne" scope="col">Package</th>
			</tr>
			<tbody>
				«FOR packageName : byPackage.keySet»
					«val removed = byPackage.get(packageName).filter(filter)»
					«IF removed.size>0»
						<tr class="rowColor">
						<td class="colOne"><a href="packages/«packageName + ".html"»" target="rightframe">«packageName»</a></td>
						</tr>
						«FOR clazz:removed»
							<tr class="rowColor">
							<td class="colOne colMenu«clazz.changeStatus»"><a href="packages/«packageName».html#«clazz.fullyQualifiedName»_summary" target="rightframe">«clazz.toStatusLable»</a></td>
							</tr>
						«ENDFOR»
					«ENDIF»
				«ENDFOR» 
			</tbody>
			</table>
			</div>
			</body>
			</html>
		'''
	}

	def createPackageSiteContent(String packageName, Map<String, List<JApiClass>> byPackage) {
		packageFileBody(
			packageName, [
				'''
					<table class="packageSummary" border="0" cellpadding="3" cellspacing="0" summary="Class Summary table, listing classes, and an explanation">
					<caption><span>Changed Classes</span><span class="tabEnd">&nbsp;</span></caption>
					<tr>
					<th class="colFirst colStatus" scope="col">Status</th>
					<th class="colLast" scope="col">Class</th>
					</tr>
					<tbody>
					«FOR clazzReport : byPackage.get(packageName)»
						<tr class="rowColor">
						<td class="colFirst colStatus col«clazzReport.changeStatus»">«clazzReport.changeStatusLabel»</td>
						<td class="colLast"><a href="#«clazzReport.fullyQualifiedName»_summary" title="class in «packageName»">«clazzReport.newClass.or(clazzReport.oldClass).get.simpleName»</a></td>
						</tr>
					«ENDFOR»
					</tbody>
					</table>
					
					«FOR clazzReport : byPackage.get(packageName)»
						«val clazz = clazzReport.newClass.or(clazzReport.oldClass).get»
						<ul class="blockList">
						<li class="blockList">
						<h2 title="Class «clazz.simpleName»" class="title">Class «clazz.simpleName» («clazzReport.changeStatus.toString.toLowerCase»)</h2>
						«IF clazzReport.serialVersionUid.serialVersionUidDefaultOldAsString!=clazzReport.serialVersionUid.serialVersionUidDefaultNewAsString»
							<h3><font color="red">(Serializable incompatible(!): default serialVersionUID changed)</font></h3>
							Old value: «clazzReport.serialVersionUid.serialVersionUidDefaultOldAsString»<br/>
							New value: «clazzReport.serialVersionUid.serialVersionUidDefaultNewAsString»<br/>
						«ENDIF»
						<div class="summary">
						<a name="«clazzReport.fullyQualifiedName»_summary">
						«IF !clazzReport.annotations.empty»
							<!-- ======== annotations SUMMARY ======== -->
							<ul class="blockList">
								<li class="blockList"><a name="annotation_summary">
								<!--   -->
								</a>
								<h3>Annotations Summary</h3>
								<table class="overviewSummary" border="0" cellpadding="3" cellspacing="0" summary="annotations Summary table, listing annotations, and an explanation">
								<tbody><tr>
								<th class="colFirst colStatus" scope="col">Status</th>
								<th class="colFirst" scope="col">Name</th>
								<th class="colLast" scope="col">Elements</th>
								</tr>
								«FOR annoChange:clazzReport.annotations»
									<tr class="rowColor">
									<td class="colFirst colStatus col«annoChange.changeStatus»">«annoChange.changeStatusLabel»</code></td>
									<td class="colFirst"><code>«annoChange.fullyQualifiedName»</code></td>
									<td class="colLast"><code>«annoChange.elements.join(", ",[toHtml])»</code>&nbsp;</td>
									</tr>
								«ENDFOR»
								</tbody></table>
								</li>
							</ul>
						«ENDIF»
						«IF !clazzReport.interfaces.empty»
							<!-- ======== interfaces SUMMARY ======== -->
							<ul class="blockList">
								<li class="blockList"><a name="annotation_summary">
								<!--   -->
								</a>
								<h3>Interfaces Summary</h3>
								<table class="overviewSummary" border="0" cellpadding="3" cellspacing="0" summary="interfaces Summary table, listing interfaces, and an explanation">
								<tbody><tr>
								<th class="colFirst colStatus" scope="col">Status</th>
								<th class="colFirst" scope="col">Name</th>
								</tr>
								«FOR annoChange:clazzReport.interfaces»
									<tr class="rowColor">
									<td class="colFirst colStatus col«annoChange.changeStatus»">«annoChange.changeStatusLabel»</code></td>
									<td class="colLast"><code>«annoChange.fullyQualifiedName»</code></td>
									</tr>
								«ENDFOR»
								</tbody></table>
								</li>
							</ul>
						«ENDIF»
						«IF clazzReport.superclass.changeStatus!=JApiChangeStatus.UNCHANGED»
							<!-- ======== superclass SUMMARY ======== -->
							<ul class="blockList">
								<li class="blockList">
								<h3>Superclass</h3>
								<table class="overviewSummary" border="0" cellpadding="3" cellspacing="0" summary="superclass Summary table, listing interfaces, and an explanation">
								<tbody><tr>
								<th class="colFirst colStatus" scope="col">Status</th>
								<th class="colFirst" scope="col">Name</th>
								</tr>
								<tr class="rowColor">
									<td class="colFirst colStatus col«clazzReport.superclass.changeStatus»">«clazzReport.superclass.changeStatusLabel»</code></td>
									<td class="colLast"><code>«clazzReport.superclass.newSuperclass.or(clazzReport.superclass.oldSuperclass).get»</code></td>
								</tr>
								</tbody></table>
								</li>
							</ul>
						«ENDIF»
						«IF !clazzReport.fields.empty»
							<!-- ======== FIELDS SUMMARY ======== -->
							<ul class="blockList">
								<li class="blockList"><a name="field_summary">
								<!--   -->
								</a>
								<h3>Field Summary</h3>
								<table class="overviewSummary" border="0" cellpadding="3" cellspacing="0" summary="Constructor Summary table, listing constructors, and an explanation">
								<tbody><tr>
								<th class="colFirst colStatus" scope="col">Status</th>
								<th class="colFirst" scope="col">Modifier</th>
								<th class="colLast" scope="col">Field</th>
								</tr>
								«FOR fieldChange:clazzReport.fields»
									<tr class="rowColor">
									<td class="colFirst colStatus col«fieldChange.changeStatus»">«fieldChange.changeStatusLabel»</code></td>
									<td class="colFirst"><code>«fieldChange.accessModifier.toHtml» </code></td>
									<td class="colLast"><code><strong>«fieldChange.name»</strong></code>&nbsp;</td>
									</tr>
								«ENDFOR»
								</tbody></table>
								</li>
							</ul>
						«ENDIF»
						«IF !clazzReport.constructors.empty»
							<!-- ======== CONSTRUCTOR SUMMARY ======== -->
							<ul class="blockList">
								<li class="blockList"><a name="constructor_summary">
								<!--   -->
								</a>
								<h3>Constructor Summary</h3>
								<table class="overviewSummary" border="0" cellpadding="3" cellspacing="0" summary="Constructor Summary table, listing constructors, and an explanation">
								<tbody><tr>
								<th class="colFirst colStatus">Status</code></th>
								<th class="colFirst" scope="col">Modifier</th>
								<th class="colLast" scope="col">Constructor and Description</th>
								</tr>
								«FOR constructorChange:clazzReport.constructors»
									<tr class="rowColor">
									<td class="colFirst colStatus col«constructorChange.changeStatus»">«constructorChange.changeStatusLabel»</code></td>
									<td class="colFirst"><code>«constructorChange.accessModifier.toHtml» </code></td>
									<td class="colLast"><code><strong>«constructorChange.name»</strong>(«constructorChange.parameters.join(', ',[toHtml])»)</code>&nbsp;</td>
									</tr>
								«ENDFOR»
								</tbody></table>
								</li>
							</ul>
						«ENDIF»
						«IF !clazzReport.methods.empty»
							<!-- ========== METHOD SUMMARY =========== -->
							<ul class="blockList">
								<li class="blockList"><a name="method_summary">
								<!--   -->
								</a>
								<h3>Method Summary</h3>
								<table class="overviewSummary" border="0" cellpadding="3" cellspacing="0" summary="Method Summary table, listing methods, and an explanation">
								<tbody><tr>
								<th class="colFirst colStatus">Status</code></th>
								<th class="colFirst" scope="col">Modifier and Type</th>
								<th class="colLast" scope="col">Method and Description</th>
								</tr>
								«FOR methodeChange:clazzReport.methods»
									<tr class="rowColor">
									<td class="colFirst colStatus col«methodeChange.changeStatus»">«methodeChange.changeStatusLabel»</td>
									<td class="colFirst"><code><strong>«methodeChange.accessModifier.toHtml»</strong> «methodeChange.returnType.toHtml»</code></td>
									<td class="colLast">
										<code><strong>«methodeChange.name»</strong>(«methodeChange.parameters.join(', ',[toHtml])»)</code>&nbsp;
										«IF !methodeChange.annotations.empty»
											- annotations: 	«methodeChange.annotations.join(', ',[toHtml])»
										«ENDIF»
									</td>
									</tr>
								«ENDFOR»
								</tbody></table>
								</li>
							</ul>
						«ENDIF»
						</div>
						</li>
						</ul>
					«ENDFOR»
				'''
			])
	}

	def <T extends JApiCompatibility & JApiHasChangeStatus> String changeStatusLabel(T japiType) {
		return japiType.changeStatus + if(!japiType.binaryCompatible) " (!)" else ""
	}

	def createStatistics(Map<String, List<JApiClass>> byPackage) {
		'''
			<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
			<!-- NewPage -->
			<html>
			<head>
			<title>Statistics («info.documentationName»)</title>
			<link rel="stylesheet" type="text/css" href="resources/stylesheet.css" title="Style">
			</head>
			<body>
			<div class="topNav">
				<ul><Strong>
				«info.documentationName»
				</strong></ul>
			</div>
			<div class="subNav">
				<div></div>
			</div>
			
			<div class="header">
				<h1 class="title">Statistics of collected API changes</h1>
			</div>
			<div class="contentContainer">
			<div class="description">
			«val allClasses = byPackage.values.flatten»
			<ul class="blocklist">
			<li class="blockList">
			<dl>
			<dt>Packages changed:</dt>
			<dd>«byPackage.keySet.size»</dd>
			<dt>Classes changed:</dt>
			<dd>«allClasses.size»</dd>
			<dt>Binary incompatible changes:</dt>
			<dd>«allClasses.filter[!binaryCompatible].size»</dd>
			<dt>Classes added:</dt>
			<dd>«allClasses.filter[changeStatus==JApiChangeStatus.NEW].size»</dd>
			<dt>Classes removed:</dt>
			<dd>«allClasses.filter[changeStatus==JApiChangeStatus.REMOVED].size»</dd>
			</dl>
			</li>
			</ul>
			</div>
			<br>
			<hr>
			<a href="plain-report.html" target="_blank">Plain html report</a>&nbsp;&nbsp;&nbsp;&nbsp;
			<a href="report.xml" target="_blank">Plain xml report</a>
			</div>
			</body>
			</html>
		'''
	}

	def toStatusLable(JApiClass clazz) {
		var lable = clazz.newClass.or(clazz.oldClass).get.simpleName
		if (!clazz.binaryCompatible) {
			lable = lable + "&nbsp;(!)"
		}
		return lable
	}

	def dispatch toHtml(JApiModifier<AccessModifier> modifier) {
		if (modifier.changeStatus == JApiChangeStatus.
			MODIFIED) {
			'''<strike>«modifier.oldModifier.get.toString.toLowerCase»</strike>&nbsp;«modifier.newModifier.get.toString.toLowerCase»'''
		} else {
			'''<code>«modifier.newModifier.or(modifier.oldModifier).get().toString.toLowerCase»</code>'''
		}
	}

	def dispatch toHtml(JApiParameter param) {
		'''<span title="«param.type»">«param.type.cutQualifier»</span>'''
	}

	def dispatch toHtml(JApiReturnType retType) {
		if (retType.changeStatus == JApiChangeStatus.
			MODIFIED) {
			'''<strike title="«retType.oldReturnType»">
			«retType.oldReturnType.cutQualifier»</strike>&nbsp;<span title="«retType.newReturnType»">«retType.newReturnType.cutQualifier»</span>'''
		} else {
			if ("n.a." != retType.
				newReturnType) '''<span title="«retType.newReturnType»">«retType.newReturnType.cutQualifier»</span>''' else '''<span title="«retType.oldReturnType»">«retType.oldReturnType.cutQualifier»</span>'''
		}
	}

	def cutQualifier(String string) {
		val lastDot = string.lastIndexOf(".")
		if (lastDot > 0) {
			return string.substring(lastDot + 1)
		}
		return string
	}

	def dispatch CharSequence toHtml(JApiAnnotationElement it) {
		if (changeStatus == JApiChangeStatus.MODIFIED) {
			'''<strike>«oldValue.get.toString»</strike>&nbsp;«newValue.get.toString»'''
		} else {
			if ("n.a." != newValue) {
				if (newValue.present)
					newValue.get.toString
			} else
				oldValue.get.toString
		}
	}

	def dispatch CharSequence toHtml(JApiAnnotation it) {
		if (changeStatus == JApiChangeStatus.MODIFIED) {
			'''<strike>«oldAnnotation.get.typeName»</strike>&nbsp;«newAnnotation.get.typeName»'''
		} else {
			if (newAnnotation.isPresent)
				"<font color='green'>added</font> @" + newAnnotation.get.typeName
			else
				"<font color='red'>removed</font> @" + oldAnnotation.get.typeName
		}
	}

	def packageFileBody(String packageName, ()=>CharSequence sequence) {
		val depth = "../"
		'''		
			<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
			<!-- NewPage -->
			<html>
				<head>
					<title>«packageName» («info.documentationName»)</title>
					<link rel="stylesheet" type="text/css" href="«depth»resources/stylesheet.css" title="Style">
				</head>
				
				<body>
				<!-- ========= START OF TOP NAVBAR ======= -->
				<div class="topNav">
					<ul><Strong>
					«info.documentationName»
					</strong></ul>
				</div>
				<div class="subNav">
					<div></div>
				</div>
				<!-- ========= END OF TOP NAVBAR ========= -->
				<div class="header">
				<h1 title="Package" class="title">Package&nbsp;«packageName»</h1>
				</div>
				<div class="contentContainer">
				<ul class="blockList">
				<li class="blockList">
				«sequence.apply»
				</li>
				</ul>
				</div>
				</body>
			</html>
		'''
	}

}