package de.dhuebner.japicmp;

import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiAnnotationElement;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibility;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiHasChangeStatus;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiModifier;
import japicmp.model.JApiParameter;
import japicmp.model.JApiReturnType;
import japicmp.output.xml.XmlOutput;
import japicmp.output.xml.XmlOutputGenerator;
import japicmp.output.xml.XmlOutputGeneratorOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Renders the japicmp result as a multi page html report.
 *
 * <p>Written as plain Java text blocks. Templates are assembled with
 * {@link String#formatted}, so a literal percent sign in the markup has to be
 * written as {@code %%}.
 *
 * <p>Some lines contain nothing but an interpolated value. The Xtend original
 * emitted those with a conditional newline so an empty value did not leave a
 * blank line behind; {@link #value(String)} reproduces that.
 */
@SuppressWarnings("unchecked")
public class MultiPageHtmlReport extends XmlOutputGenerator {

	public enum MenuKind {
		OVERVIEW,
		REMOVED,
		ADDED,
		BREAKING
	}

	private final ReporterInformation info;

	public MultiPageHtmlReport(List<JApiClass> jApiClasses, Options options, XmlOutputGeneratorOptions xmlOptions) {
		super(jApiClasses, options, xmlOptions);
		this.info = null;
	}

	public MultiPageHtmlReport(ReporterInformation info, List<JApiClass> jApiClasses, Options options,
			XmlOutputGeneratorOptions xmlOptions) {
		super(jApiClasses, options, xmlOptions);
		this.info = info;
	}

	@Override
	public XmlOutput generate() {
		// need to call super to get a filtered list of jApiClasses
		XmlOutput result = super.generate();

		File outputFolder = new File(info.getOutputFolder());
		copyResources(outputFolder);

		// LinkedHashMap keeps the encounter order the Xtend groupBy used, so the
		// package order in the report is unchanged.
		Map<String, List<JApiClass>> byPackage = jApiClasses.stream()
				.collect(Collectors.groupingBy(c -> c.getNewClass().or(c.getOldClass()).get().getPackageName(),
						LinkedHashMap::new, Collectors.toList()));

		// main frameset
		write(outputFolder, "changes.html", createStartPage());

		write(outputFolder, "package-overview.html",
				createMenu(byPackage, MenuKind.OVERVIEW, c -> c.getChangeStatus() == JApiChangeStatus.MODIFIED));

		write(outputFolder, "removed-overview.html",
				createMenu(byPackage, MenuKind.REMOVED, c -> c.getChangeStatus() == JApiChangeStatus.REMOVED));

		write(outputFolder, "added-overview.html",
				createMenu(byPackage, MenuKind.ADDED, c -> c.getChangeStatus() == JApiChangeStatus.NEW));

		write(outputFolder, "breaking-overview.html",
				createMenu(byPackage, MenuKind.BREAKING, c -> !c.isBinaryCompatible()));

		write(outputFolder, "statistics.html", createStatistics(byPackage));

		new File(outputFolder, "packages").mkdirs();
		for (String packageName : byPackage.keySet()) {
			write(new File(outputFolder, "packages"), packageName + ".html", createPackageSiteContent(packageName, byPackage));
		}
		return result;
	}

	private static void write(File dir, String name, CharSequence content) {
		try (PrintWriter writer = new PrintWriter(new File(dir, name))) {
			writer.append(content);
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException("failed to write " + new File(dir, name), e);
		}
	}

	private void copyResources(File outputRoot) {
		File resourcesOutFolder = new File(outputRoot, "resources");
		resourcesOutFolder.mkdirs();
		copyResourceUsingClassloder(resourcesOutFolder, "stylesheet.css");
		copyResourceUsingClassloder(resourcesOutFolder, "background.gif");
		copyResourceUsingClassloder(resourcesOutFolder, "tab.gif");
		copyResourceUsingClassloder(resourcesOutFolder, "titlebar.gif");
		copyResourceUsingClassloder(resourcesOutFolder, "titlebar_end.gif");
	}

	private void copyResourceUsingClassloder(File destFolder, String fileName) {
		try (var in = getClass().getClassLoader().getResourceAsStream("html/" + fileName)) {
			Files.copy(in, new File(destFolder, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException("failed to copy " + fileName, e);
		}
	}

	/**
	 * A template line holding only a value. An empty value must not leave a blank
	 * line behind, which a plain {@code %s} placeholder would do.
	 */
	private static String value(String text) {
		return text.isEmpty() ? "" : text + "\n";
	}

	private String createStartPage() {
		return """
				<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Frameset//EN""http://www.w3.org/TR/REC-html40/frameset.dtd">
				<HTML>
				<HEAD>
				<TITLE>%s</TITLE>
				</HEAD>
				<FRAMESET COLS="25%%,75%%">
				<FRAME SRC="package-overview.html" SCROLLING="auto" NAME="leftframe">
				<FRAME SRC="statistics.html"
					SCROLLING="auto" NAME="rightframe">
				</FRAMESET>
				<NOFRAMES>
					<H2>Frame Alert</H2>
				</NOFRAMES>
				</HTML>
				""".formatted(info.getDocumentationName());
	}

	private String createMenu(Map<String, List<JApiClass>> byPackage, MenuKind menuKind, Predicate<JApiClass> filter) {
		String depth = "./";

		StringBuilder classes = new StringBuilder();
		for (String packageName : byPackage.keySet()) {
			List<JApiClass> matched = byPackage.get(packageName).stream().filter(filter).collect(Collectors.toList());
			if (!matched.isEmpty()) {
				classes.append("""
								<tr class="rowColor">
									<td class="colOne"><a href="packages/%s.html" target="rightframe">%s</a></td>
									</tr>
								""".formatted(packageName, packageName));
				for (JApiClass clazz : matched) {
					classes.append("""
									<tr class="rowColor">
										<td class="colOne colMenu%s"><a href="packages/%s.html#%s_summary" target="rightframe">%s</a></td>
									</tr>
								""".formatted(clazz.getChangeStatus(), packageName, clazz.getFullyQualifiedName(),
								toStatusLable(clazz)));
				}
			}
		}

		return """
				<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
				<!-- NewPage -->
				<html>
				<head>
				<title>Overview (%s)</title>
				<link rel="stylesheet" type="text/css" href="%sresources/stylesheet.css" title="Style">
				</head>
				<body>
				<!-- ========= START OF TOP NAVBAR ======= -->
				<div class="topNav"><a name="navbar_top">
				<!--   -->
				</a><a href="#skip-navbar_top" title="Skip navigation links"></a><a name="navbar_top_firstrow">
				<!--   -->
				</a>
				<ul class="navList" title="Navigation">
				%s
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
				%s
				</tbody>
				</table>
				</div>
				</body>
				</html>
				""".formatted(info.getDocumentationName(), depth, createNavigation(menuKind), classes);
	}

	/**
	 * The four navigation entries. The entry for the current page is rendered as
	 * plain text, every other one as a link.
	 */
	private static String createNavigation(MenuKind menuKind) {
		return menuItem(menuKind, MenuKind.OVERVIEW, "Overview")
				+ menuItem(menuKind, MenuKind.REMOVED, "Removed")
				+ menuItem(menuKind, MenuKind.ADDED, "Added")
				+ menuItem(menuKind, MenuKind.BREAKING, "Critical");
	}

	private static String menuItem(MenuKind current, MenuKind item, String label) {
		String target = item == MenuKind.OVERVIEW ? "package-overview" : item.name().toLowerCase() + "-overview";
		if (current == item) {
			return "<li class=\"navBarCell1Rev\">" + label + "</li>\n";
		}
		return "<li><a href=\"" + target + ".html\" target=\"leftframe\">" + label + "</a></li>\n";
	}

	private String createPackageSiteContent(String packageName, Map<String, List<JApiClass>> byPackage) {
		return packageFileBody(packageName, () -> packageFileBody(packageName, byPackage));
	}

	private String packageFileBody(String packageName, Map<String, List<JApiClass>> byPackage) {
		String depth = "../";
		StringBuilder rows = new StringBuilder();

		rows.append("""
				<table class="packageSummary" border="0" cellpadding="3" cellspacing="0" summary="Class Summary table, listing classes, and an explanation">
				<caption><span>Changed Classes</span><span class="tabEnd">&nbsp;</span></caption>
				<tr>
				<th class="colFirst colStatus" scope="col">Status</th>
				<th class="colLast" scope="col">Class</th>
				</tr>
				<tbody>
				""");
		for (JApiClass clazzReport : byPackage.get(packageName)) {
			rows.append("""
						<tr class="rowColor">
							<td class="colFirst colStatus col%s">%s</td>
							<td class="colLast"><a href="#%s_summary" title="class in %s">%s</a></td>
						</tr>
					""".formatted(clazzReport.getChangeStatus(), changeStatusLabel(clazzReport),
					clazzReport.getFullyQualifiedName(), packageName,
					clazzReport.getNewClass().or(clazzReport.getOldClass()).get().getSimpleName()));
		}
		rows.append("</tbody></table>\n");

		for (JApiClass clazzReport : byPackage.get(packageName)) {
			var clazz = clazzReport.getNewClass().or(clazzReport.getOldClass()).get();
			rows.append("<ul class=\"blockList\">\n<li class=\"blockList\">\n");
			rows.append("<h2 title=\"Class %s\" class=\"title\">Class %s (%s)</h2>\n".formatted(clazz.getSimpleName(),
					clazz.getSimpleName(), clazzReport.getChangeStatus().toString().toLowerCase()));

			appendSerialVersionUid(rows, clazzReport);
			rows.append("<div class=\"summary\">\n");
			rows.append("<a name=\"%s_summary\">\n".formatted(clazzReport.getFullyQualifiedName()));
			appendAnnotations(rows, clazzReport);
			appendInterfaces(rows, clazzReport);
			appendSuperclass(rows, clazzReport);
			appendFields(rows, clazzReport);
			appendConstructors(rows, clazzReport);
			appendMethods(rows, clazzReport);

			rows.append("</div>\n</li>\n</ul>\n");
		}
		return rows.toString();
	}

	private void appendSerialVersionUid(StringBuilder rows, JApiClass clazzReport) {
		var uid = clazzReport.getSerialVersionUid();
		if (!Objects.equals(uid.getSerialVersionUidDefaultOldAsString(), uid.getSerialVersionUidDefaultNewAsString())) {
			rows.append("<h3><font color=\"red\">(Serializable incompatible(!): default serialVersionUID changed)</font></h3>\n");
			rows.append("Old value: %s<br/>\n".formatted(uid.getSerialVersionUidDefaultOldAsString()));
			rows.append("New value: %s<br/>\n".formatted(uid.getSerialVersionUidDefaultNewAsString()));
		}
	}

	private void appendAnnotations(StringBuilder rows, JApiClass clazzReport) {
		if (clazzReport.getAnnotations().isEmpty()) {
			return;
		}
		rows.append("""
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
				""");
		for (JApiAnnotation annoChange : clazzReport.getAnnotations()) {
			rows.append("""
							<tr class="rowColor">
							<td class="colFirst colStatus col%s">%s</code></td>
							<td class="colFirst"><code>%s</code></td>
							<td class="colLast"><code>%s</code>&nbsp;</td>
							</tr>
						""".formatted(annoChange.getChangeStatus(), changeStatusLabel(annoChange),
					annoChange.getFullyQualifiedName(),
					annoChange.getElements().stream().map(this::toHtml).collect(Collectors.joining(", "))));
		}
		rows.append("</tbody></table>\n</li>\n</ul>\n");
	}

	private void appendInterfaces(StringBuilder rows, JApiClass clazzReport) {
		if (clazzReport.getInterfaces().isEmpty()) {
			return;
		}
		rows.append("""
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
				""");
		for (JApiImplementedInterface annoChange : clazzReport.getInterfaces()) {
			rows.append("""
							<tr class="rowColor">
							<td class="colFirst colStatus col%s">%s</code></td>
							<td class="colLast"><code>%s</code></td>
							</tr>
						""".formatted(annoChange.getChangeStatus(), changeStatusLabel(annoChange),
					annoChange.getFullyQualifiedName()));
		}
		rows.append("</tbody></table>\n</li>\n</ul>\n");
	}

	private void appendSuperclass(StringBuilder rows, JApiClass clazzReport) {
		var superclass = clazzReport.getSuperclass();
		if (superclass.getChangeStatus() == JApiChangeStatus.UNCHANGED) {
			return;
		}
		rows.append("""
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
						<td class="colFirst colStatus col%s">%s</code></td>
						<td class="colLast"><code>%s</code></td>
					</tr>
					</tbody></table>
					</li>
				</ul>
				""".formatted(superclass.getChangeStatus(), changeStatusLabel(superclass),
				superclass.getNewSuperclass().or(superclass.getOldSuperclass()).get().getName()));
	}

	private void appendFields(StringBuilder rows, JApiClass clazzReport) {
		if (clazzReport.getFields().isEmpty()) {
			return;
		}
		rows.append("""
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
				""");
		for (JApiField fieldChange : clazzReport.getFields()) {
			rows.append("""
							<tr class="rowColor">
							<td class="colFirst colStatus col%s">%s</code></td>
							<td class="colFirst"><code>%s </code></td>
							<td class="colLast"><code><strong>%s</strong></code>&nbsp;</td>
							</tr>
						""".formatted(fieldChange.getChangeStatus(), changeStatusLabel(fieldChange),
					toHtml(fieldChange.getAccessModifier()), fieldChange.getName()));
		}
		rows.append("</tbody></table>\n</li>\n</ul>\n");
	}

	private void appendConstructors(StringBuilder rows, JApiClass clazzReport) {
		if (clazzReport.getConstructors().isEmpty()) {
			return;
		}
		rows.append("""
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
				""");
		for (JApiConstructor constructorChange : clazzReport.getConstructors()) {
			rows.append("""
							<tr class="rowColor">
							<td class="colFirst colStatus col%s">%s</code></td>
							<td class="colFirst"><code>%s </code></td>
							<td class="colLast"><code><strong>%s</strong>(%s)</code>&nbsp;</td>
							</tr>
						""".formatted(constructorChange.getChangeStatus(), changeStatusLabel(constructorChange),
					toHtml(constructorChange.getAccessModifier()), constructorChange.getName(),
					constructorChange.getParameters().stream().map(this::toHtml).collect(Collectors.joining(", "))));
		}
		rows.append("</tbody></table>\n</li>\n</ul>\n");
	}

	private void appendMethods(StringBuilder rows, JApiClass clazzReport) {
		if (clazzReport.getMethods().isEmpty()) {
			return;
		}
		rows.append("""
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
				""");
		for (JApiMethod methodeChange : clazzReport.getMethods()) {
			StringBuilder annotations = new StringBuilder();
			if (!methodeChange.getAnnotations().isEmpty()) {
				annotations.append("- annotations: \t%s\n".formatted(
						methodeChange.getAnnotations().stream().map(this::toHtml).collect(Collectors.joining(", "))));
			}
			rows.append("""
							<tr class="rowColor">
							<td class="colFirst colStatus col%s">%s</td>
							<td class="colFirst"><code><strong>%s</strong> %s</code></td>
							<td class="colLast">
								<code><strong>%s</strong>(%s)</code>&nbsp;
								%s
							</td>
							</tr>
						""".formatted(methodeChange.getChangeStatus(), changeStatusLabel(methodeChange),
					toHtml(methodeChange.getAccessModifier()), toHtml(methodeChange.getReturnType()),
					methodeChange.getName(),
					methodeChange.getParameters().stream().map(this::toHtml).collect(Collectors.joining(", ")),
					annotations));
		}
		rows.append("</tbody></table>\n</li>\n</ul>\n");
	}

	private String createStatistics(Map<String, List<JApiClass>> byPackage) {
		List<JApiClass> allClasses = byPackage.values().stream().flatMap(List::stream).collect(Collectors.toList());
		return """
				<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
				<!-- NewPage -->
				<html>
				<head>
				<title>Statistics (%s)</title>
				<link rel="stylesheet" type="text/css" href="resources/stylesheet.css" title="Style">
				</head>
				<body>
				<div class="topNav">
					<ul><Strong>
					%s
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
				<ul class="blocklist">
				<li class="blockList">
				<dl>
				<dt>Packages changed:</dt>
				<dd>%s</dd>
				<dt>Classes changed:</dt>
				<dd>%s</dd>
				<dt>Binary incompatible changes:</dt>
				<dd>%s</dd>
				<dt>Classes added:</dt>
				<dd>%s</dd>
				<dt>Classes removed:</dt>
				<dd>%s</dd>
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
				""".formatted(info.getDocumentationName(), value(info.getDocumentationName()),
				byPackage.keySet().size(), allClasses.size(),
				allClasses.stream().filter(c -> !c.isBinaryCompatible()).count(),
				allClasses.stream().filter(c -> c.getChangeStatus() == JApiChangeStatus.NEW).count(),
				allClasses.stream().filter(c -> c.getChangeStatus() == JApiChangeStatus.REMOVED).count());
	}

	private static String toStatusLable(JApiClass clazz) {
		String lable = clazz.getNewClass().or(clazz.getOldClass()).get().getSimpleName();
		if (!clazz.isBinaryCompatible()) {
			lable = lable + "&nbsp;(!)";
		}
		return lable;
	}

	private String toHtml(JApiModifier<AccessModifier> modifier) {
		if (modifier.getChangeStatus() == JApiChangeStatus.MODIFIED) {
			return "<strike>%s</strike>&nbsp;%s".formatted(modifier.getOldModifier().get().toString().toLowerCase(),
					modifier.getNewModifier().get().toString().toLowerCase());
		}
		return "<code>%s</code>".formatted(modifier.getNewModifier().or(modifier.getOldModifier()).get().toString().toLowerCase());
	}

	private String toHtml(JApiParameter param) {
		return "<span title=\"%s\">%s</span>".formatted(param.getType(), cutQualifier(param.getType()));
	}

	private String toHtml(JApiReturnType retType) {
		if (retType.getChangeStatus() == JApiChangeStatus.MODIFIED) {
			return "<strike title=\"%s\">%s</strike>&nbsp;<span title=\"%s\">%s</span>".formatted(retType.getOldReturnType(),
					cutQualifier(retType.getOldReturnType()), retType.getNewReturnType(),
					cutQualifier(retType.getNewReturnType()));
		}
		if (!"n.a.".equals(retType.getNewReturnType())) {
			return "<span title=\"%s\">%s</span>".formatted(retType.getNewReturnType(),
					cutQualifier(retType.getNewReturnType()));
		}
		return "<span title=\"%s\">%s</span>".formatted(retType.getOldReturnType(),
				cutQualifier(retType.getOldReturnType()));
	}

	private static String cutQualifier(String string) {
		int lastDot = string.lastIndexOf('.');
		if (lastDot > 0) {
			return string.substring(lastDot + 1);
		}
		return string;
	}

	private String toHtml(JApiAnnotationElement it) {
		if (it.getChangeStatus() == JApiChangeStatus.MODIFIED) {
			return "<strike>%s</strike>&nbsp;%s".formatted(it.getOldValue().get().toString(),
					it.getNewValue().get().toString());
		}
		if (!"n.a.".equals(it.getNewValue())) {
			if (it.getNewValue().isPresent()) {
				return it.getNewValue().get().toString();
			}
			return null;
		}
		return it.getOldValue().get().toString();
	}

	private String toHtml(JApiAnnotation it) {
		if (it.getChangeStatus() == JApiChangeStatus.MODIFIED) {
			return "<strike>%s</strike>&nbsp;%s".formatted(it.getOldAnnotation().get().getTypeName(),
					it.getNewAnnotation().get().getTypeName());
		}
		if (it.getNewAnnotation().isPresent()) {
			return "<font color='green'>added</font> @" + it.getNewAnnotation().get().getTypeName();
		}
		return "<font color='red'>removed</font> @" + it.getOldAnnotation().get().getTypeName();
	}

	/**
	 * Single entry point replacing the Xtend {@code dispatch} declarations.
	 */
	private String toHtml(Object value) {
		if (value instanceof JApiAnnotation) {
			return toHtml((JApiAnnotation) value);
		}
		if (value instanceof JApiAnnotationElement) {
			return toHtml((JApiAnnotationElement) value);
		}
		if (value instanceof JApiModifier) {
			return toHtml((JApiModifier<AccessModifier>) value);
		}
		if (value instanceof JApiParameter) {
			return toHtml((JApiParameter) value);
		}
		if (value instanceof JApiReturnType) {
			return toHtml((JApiReturnType) value);
		}
		throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.asList(value));
	}

	private static <T extends JApiCompatibility & JApiHasChangeStatus> String changeStatusLabel(T japiType) {
		return japiType.getChangeStatus() + (!japiType.isBinaryCompatible() ? " (!)" : "");
	}

	private String packageFileBody(String packageName, Supplier<CharSequence> sequence) {
		String depth = "../";
		return """
				<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
				<!-- NewPage -->
				<html>
					<head>
						<title>%s (%s)</title>
						<link rel="stylesheet" type="text/css" href="%sresources/stylesheet.css" title="Style">
					</head>

					<body>
					<!-- ========= START OF TOP NAVBAR ======= -->
					<div class="topNav">
						<ul><Strong>
						%s
						</strong></ul>
					</div>
					<div class="subNav">
						<div></div>
					</div>
					<!-- ========= END OF TOP NAVBAR ========= -->
					<div class="header">
						<h1 title="Package" class="title">Package&nbsp;%s</h1>
					</div>
					<div class="contentContainer">
					<ul class="blockList">
						<li class="blockList">
						%s
						</li>
					</ul>
					</div>
					</body>
				</html>
				""".formatted(packageName, info.getDocumentationName(), depth,
				value(info.getDocumentationName()), packageName, sequence.get());
	}
}