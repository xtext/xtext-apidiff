package de.dhuebner.japicmp;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import de.dhuebner.japicmp.ReporterInformation;
import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiAnnotationElement;
import japicmp.model.JApiBinaryCompatibility;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiHasChangeStatus;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiModifier;
import japicmp.model.JApiParameter;
import japicmp.model.JApiReturnType;
import japicmp.model.JApiSuperclass;
import japicmp.output.xml.XmlOutputGenerator;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.CtClass;
import javassist.bytecode.annotation.MemberValue;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class MultiPageHtmlReport extends XmlOutputGenerator {
  public enum MenuKind {
    OVERVIEW,
    
    REMOVED,
    
    ADDED,
    
    BREAKING;
  }
  
  private ReporterInformation info;
  
  public MultiPageHtmlReport(final String oldArchivePath, final String newArchivePath, final List<JApiClass> jApiClasses, final Options options) {
    super(oldArchivePath, newArchivePath, jApiClasses, options);
  }
  
  public MultiPageHtmlReport(final ReporterInformation info, final String oldArchivePath, final String newArchivePath, final List<JApiClass> jApiClasses, final Options options) {
    this(oldArchivePath, newArchivePath, jApiClasses, options);
    this.info = info;
  }
  
  @Override
  public Void generate() {
    try {
      super.generate();
      String _outputFolder = this.info.getOutputFolder();
      final File outputFolder = new File(_outputFolder);
      this.copyResources(outputFolder);
      final Function1<JApiClass, String> _function = (JApiClass it) -> {
        return it.getNewClass().or(it.getOldClass()).get().getPackageName();
      };
      final Map<String, List<JApiClass>> byPackage = IterableExtensions.<String, JApiClass>groupBy(this.jApiClasses, _function);
      String _plus = (outputFolder + "/changes.html");
      new PrintWriter(_plus).append(this.createStartPage()).close();
      String _plus_1 = (outputFolder + "/package-overview.html");
      final Function1<JApiClass, Boolean> _function_1 = (JApiClass it) -> {
        JApiChangeStatus _changeStatus = it.getChangeStatus();
        return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.MODIFIED));
      };
      new PrintWriter(_plus_1).append(this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.OVERVIEW, _function_1)).close();
      String _plus_2 = (outputFolder + "/removed-overview.html");
      final Function1<JApiClass, Boolean> _function_2 = (JApiClass it) -> {
        JApiChangeStatus _changeStatus = it.getChangeStatus();
        return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.REMOVED));
      };
      new PrintWriter(_plus_2).append(this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.REMOVED, _function_2)).close();
      String _plus_3 = (outputFolder + "/added-overview.html");
      final Function1<JApiClass, Boolean> _function_3 = (JApiClass it) -> {
        JApiChangeStatus _changeStatus = it.getChangeStatus();
        return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.NEW));
      };
      new PrintWriter(_plus_3).append(this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.ADDED, _function_3)).close();
      String _plus_4 = (outputFolder + "/breaking-overview.html");
      final Function1<JApiClass, Boolean> _function_4 = (JApiClass it) -> {
        boolean _isBinaryCompatible = it.isBinaryCompatible();
        return Boolean.valueOf((!_isBinaryCompatible));
      };
      new PrintWriter(_plus_4).append(this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.BREAKING, _function_4)).close();
      String _plus_5 = (outputFolder + "/statistics.html");
      new PrintWriter(_plus_5).append(this.createStatistics(byPackage)).close();
      new File(outputFolder, "packages").mkdirs();
      Set<String> _keySet = byPackage.keySet();
      for (final String packageName : _keySet) {
        {
          String _plus_6 = (outputFolder + "/packages/");
          String _plus_7 = (_plus_6 + packageName);
          String _plus_8 = (_plus_7 + ".html");
          final PrintWriter writer = new PrintWriter(_plus_8);
          writer.append(this.createPackageSiteContent(packageName, byPackage)).close();
        }
      }
      return null;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public long copyResources(final File outputRoot) {
    long _xblockexpression = (long) 0;
    {
      final File resourcesOutFolder = new File(outputRoot, "resources");
      resourcesOutFolder.mkdirs();
      this.copyResourceUsingClassloder(resourcesOutFolder, "stylesheet.css");
      this.copyResourceUsingClassloder(resourcesOutFolder, "background.gif");
      this.copyResourceUsingClassloder(resourcesOutFolder, "tab.gif");
      this.copyResourceUsingClassloder(resourcesOutFolder, "titlebar.gif");
      _xblockexpression = this.copyResourceUsingClassloder(resourcesOutFolder, "titlebar_end.gif");
    }
    return _xblockexpression;
  }
  
  public long copyResourceUsingClassloder(final File destFolder, final String fileName) {
    try {
      return Files.copy(this.getClass().getClassLoader().getResourceAsStream(("html/" + fileName)), new File(destFolder, fileName).toPath(), 
        StandardCopyOption.REPLACE_EXISTING);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public CharSequence createStartPage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Frameset//EN\"\"http://www.w3.org/TR/REC-html40/frameset.dtd\">");
    _builder.newLine();
    _builder.append("<HTML>");
    _builder.newLine();
    _builder.append("<HEAD>");
    _builder.newLine();
    _builder.append("<TITLE>");
    String _documentationName = this.info.getDocumentationName();
    _builder.append(_documentationName);
    _builder.append("</TITLE>");
    _builder.newLineIfNotEmpty();
    _builder.append("</HEAD>");
    _builder.newLine();
    _builder.append("<FRAMESET COLS=\"25%,75%\">");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<FRAME SRC=\"package-overview.html\" SCROLLING=\"auto\" NAME=\"leftframe\">");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<FRAME SRC=\"statistics.html\"");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("SCROLLING=\"auto\" NAME=\"rightframe\">");
    _builder.newLine();
    _builder.append("</FRAMESET>");
    _builder.newLine();
    _builder.append("<NOFRAMES>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<H2>Frame Alert</H2>");
    _builder.newLine();
    _builder.append("</NOFRAMES>");
    _builder.newLine();
    _builder.append("</HTML>");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence createMenu(final Map<String, List<JApiClass>> byPackage, final MultiPageHtmlReport.MenuKind menuKind, final Function1<? super JApiClass, ? extends Boolean> filter) {
    CharSequence _xblockexpression = null;
    {
      final String depth = "./";
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
      _builder.newLine();
      _builder.append("<!-- NewPage -->");
      _builder.newLine();
      _builder.append("<html>");
      _builder.newLine();
      _builder.append("<head>");
      _builder.newLine();
      _builder.append("<title>Overview (");
      String _documentationName = this.info.getDocumentationName();
      _builder.append(_documentationName);
      _builder.append(")</title>");
      _builder.newLineIfNotEmpty();
      _builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
      _builder.append(depth);
      _builder.append("resources/stylesheet.css\" title=\"Style\">");
      _builder.newLineIfNotEmpty();
      _builder.append("</head>");
      _builder.newLine();
      _builder.append("<body>");
      _builder.newLine();
      _builder.append("<!-- ========= START OF TOP NAVBAR ======= -->");
      _builder.newLine();
      _builder.append("<div class=\"topNav\"><a name=\"navbar_top\">");
      _builder.newLine();
      _builder.append("<!--   -->");
      _builder.newLine();
      _builder.append("</a><a href=\"#skip-navbar_top\" title=\"Skip navigation links\"></a><a name=\"navbar_top_firstrow\">");
      _builder.newLine();
      _builder.append("<!--   -->");
      _builder.newLine();
      _builder.append("</a>");
      _builder.newLine();
      _builder.append("<ul class=\"navList\" title=\"Navigation\">");
      _builder.newLine();
      {
        boolean _equals = Objects.equal(menuKind, MultiPageHtmlReport.MenuKind.REMOVED);
        if (_equals) {
          _builder.append("<li><a href=\"package-overview.html\" target=\"leftframe\">Overview</a></li>");
          _builder.newLine();
          _builder.append("<li class=\"navBarCell1Rev\">Removed</li>");
          _builder.newLine();
          _builder.append("<li><a href=\"added-overview.html\" target=\"leftframe\">Added</a></li>");
          _builder.newLine();
          _builder.append("<li><a href=\"breaking-overview.html\" target=\"leftframe\">Critical</a></li>");
          _builder.newLine();
        } else {
          boolean _equals_1 = Objects.equal(menuKind, MultiPageHtmlReport.MenuKind.ADDED);
          if (_equals_1) {
            _builder.append("<li><a href=\"package-overview.html\" target=\"leftframe\">Overview</a></li>");
            _builder.newLine();
            _builder.append("<li><a href=\"removed-overview.html\" target=\"leftframe\">Removed</a></li>");
            _builder.newLine();
            _builder.append("<li class=\"navBarCell1Rev\">Added</li>");
            _builder.newLine();
            _builder.append("<li><a href=\"breaking-overview.html\" target=\"leftframe\">Critical</a></li>");
            _builder.newLine();
          } else {
            boolean _equals_2 = Objects.equal(menuKind, MultiPageHtmlReport.MenuKind.OVERVIEW);
            if (_equals_2) {
              _builder.append("<li class=\"navBarCell1Rev\">Overview</li>");
              _builder.newLine();
              _builder.append("<li><a href=\"removed-overview.html\" target=\"leftframe\">Removed</a></li>");
              _builder.newLine();
              _builder.append("<li><a href=\"added-overview.html\" target=\"leftframe\">Added</a></li>");
              _builder.newLine();
              _builder.append("<li><a href=\"breaking-overview.html\" target=\"leftframe\">Critical</a></li>");
              _builder.newLine();
            } else {
              boolean _equals_3 = Objects.equal(menuKind, MultiPageHtmlReport.MenuKind.BREAKING);
              if (_equals_3) {
                _builder.append("<li><a href=\"package-overview.html\" target=\"leftframe\">Overview</a></li>");
                _builder.newLine();
                _builder.append("<li><a href=\"removed-overview.html\" target=\"leftframe\">Removed</a></li>");
                _builder.newLine();
                _builder.append("<li><a href=\"added-overview.html\" target=\"leftframe\">Added</a></li>");
                _builder.newLine();
                _builder.append("<li class=\"navBarCell1Rev\">Critical</li>");
                _builder.newLine();
              }
            }
          }
        }
      }
      _builder.append("<li><a href=\"statistics.html\" target=\"rightframe\">Statistic</a></li>");
      _builder.newLine();
      _builder.append("</ul>");
      _builder.newLine();
      _builder.append("</div>");
      _builder.newLine();
      _builder.append("<div class=\"subNav\">");
      _builder.newLine();
      _builder.append("<ul class=\"navList\">");
      _builder.newLine();
      _builder.append("</ul>");
      _builder.newLine();
      _builder.append("<ul class=\"navList\" id=\"allclasses_navbar_top\">");
      _builder.newLine();
      _builder.append("</ul>");
      _builder.newLine();
      _builder.append("<div>");
      _builder.newLine();
      _builder.append("</div>");
      _builder.newLine();
      _builder.append("<a name=\"skip-navbar_top\">");
      _builder.newLine();
      _builder.append("<!--   -->");
      _builder.newLine();
      _builder.append("</a></div>");
      _builder.newLine();
      _builder.append("<!-- ========= END OF TOP NAVBAR ========= -->");
      _builder.newLine();
      _builder.append("<div class=\"contentContainer\">");
      _builder.newLine();
      _builder.append("<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Packages table, listing packages, and an explanation\">");
      _builder.newLine();
      _builder.append("<tr>");
      _builder.newLine();
      _builder.append("<th class=\"colOne\" scope=\"col\">Package</th>");
      _builder.newLine();
      _builder.append("</tr>");
      _builder.newLine();
      _builder.append("<tbody>");
      _builder.newLine();
      {
        Set<String> _keySet = byPackage.keySet();
        for(final String packageName : _keySet) {
          _builder.append("\t");
          final Iterable<JApiClass> removed = IterableExtensions.<JApiClass>filter(byPackage.get(packageName), ((Function1<? super JApiClass, Boolean>)filter));
          _builder.newLineIfNotEmpty();
          {
            int _size = IterableExtensions.size(removed);
            boolean _greaterThan = (_size > 0);
            if (_greaterThan) {
              _builder.append("\t");
              _builder.append("<tr class=\"rowColor\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<td class=\"colOne\"><a href=\"packages/");
              _builder.append((packageName + ".html"), "\t");
              _builder.append("\" target=\"rightframe\">");
              _builder.append(packageName, "\t");
              _builder.append("</a></td>");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              {
                for(final JApiClass clazz : removed) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colOne colMenu");
                  JApiChangeStatus _changeStatus = clazz.getChangeStatus();
                  _builder.append(_changeStatus, "\t");
                  _builder.append("\"><a href=\"packages/");
                  _builder.append(packageName, "\t");
                  _builder.append(".html#");
                  String _fullyQualifiedName = clazz.getFullyQualifiedName();
                  _builder.append(_fullyQualifiedName, "\t");
                  _builder.append("_summary\" target=\"rightframe\">");
                  String _statusLable = this.toStatusLable(clazz);
                  _builder.append(_statusLable, "\t");
                  _builder.append("</a></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("</tr>");
                  _builder.newLine();
                }
              }
            }
          }
        }
      }
      _builder.append("</tbody>");
      _builder.newLine();
      _builder.append("</table>");
      _builder.newLine();
      _builder.append("</div>");
      _builder.newLine();
      _builder.append("</body>");
      _builder.newLine();
      _builder.append("</html>");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence createPackageSiteContent(final String packageName, final Map<String, List<JApiClass>> byPackage) {
    final Function0<CharSequence> _function = () -> {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<table class=\"packageSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Class Summary table, listing classes, and an explanation\">");
      _builder.newLine();
      _builder.append("<caption><span>Changed Classes</span><span class=\"tabEnd\">&nbsp;</span></caption>");
      _builder.newLine();
      _builder.append("<tr>");
      _builder.newLine();
      _builder.append("<th class=\"colFirst colStatus\" scope=\"col\">Status</th>");
      _builder.newLine();
      _builder.append("<th class=\"colLast\" scope=\"col\">Class</th>");
      _builder.newLine();
      _builder.append("</tr>");
      _builder.newLine();
      _builder.append("<tbody>");
      _builder.newLine();
      {
        List<JApiClass> _get = byPackage.get(packageName);
        for(final JApiClass clazzReport : _get) {
          _builder.append("<tr class=\"rowColor\">");
          _builder.newLine();
          _builder.append("<td class=\"colFirst colStatus col");
          JApiChangeStatus _changeStatus = clazzReport.getChangeStatus();
          _builder.append(_changeStatus);
          _builder.append("\">");
          String _changeStatusLabel = this.<JApiClass>changeStatusLabel(clazzReport);
          _builder.append(_changeStatusLabel);
          _builder.append("</td>");
          _builder.newLineIfNotEmpty();
          _builder.append("<td class=\"colLast\"><a href=\"#");
          String _fullyQualifiedName = clazzReport.getFullyQualifiedName();
          _builder.append(_fullyQualifiedName);
          _builder.append("_summary\" title=\"class in ");
          _builder.append(packageName);
          _builder.append("\">");
          String _simpleName = clazzReport.getNewClass().or(clazzReport.getOldClass()).get().getSimpleName();
          _builder.append(_simpleName);
          _builder.append("</a></td>");
          _builder.newLineIfNotEmpty();
          _builder.append("</tr>");
          _builder.newLine();
        }
      }
      _builder.append("</tbody>");
      _builder.newLine();
      _builder.append("</table>");
      _builder.newLine();
      _builder.newLine();
      {
        List<JApiClass> _get_1 = byPackage.get(packageName);
        for(final JApiClass clazzReport_1 : _get_1) {
          final CtClass clazz = clazzReport_1.getNewClass().or(clazzReport_1.getOldClass()).get();
          _builder.newLineIfNotEmpty();
          _builder.append("<ul class=\"blockList\">");
          _builder.newLine();
          _builder.append("<li class=\"blockList\">");
          _builder.newLine();
          _builder.append("<h2 title=\"Class ");
          String _simpleName_1 = clazz.getSimpleName();
          _builder.append(_simpleName_1);
          _builder.append("\" class=\"title\">Class ");
          String _simpleName_2 = clazz.getSimpleName();
          _builder.append(_simpleName_2);
          _builder.append(" (");
          String _lowerCase = clazzReport_1.getChangeStatus().toString().toLowerCase();
          _builder.append(_lowerCase);
          _builder.append(")</h2>");
          _builder.newLineIfNotEmpty();
          {
            String _serialVersionUidDefaultOldAsString = clazzReport_1.getSerialVersionUid().getSerialVersionUidDefaultOldAsString();
            String _serialVersionUidDefaultNewAsString = clazzReport_1.getSerialVersionUid().getSerialVersionUidDefaultNewAsString();
            boolean _notEquals = (!Objects.equal(_serialVersionUidDefaultOldAsString, _serialVersionUidDefaultNewAsString));
            if (_notEquals) {
              _builder.append("<h3><font color=\"red\">(Serializable incompatible(!): default serialVersionUID changed)</font></h3>");
              _builder.newLine();
              _builder.append("Old value: ");
              String _serialVersionUidDefaultOldAsString_1 = clazzReport_1.getSerialVersionUid().getSerialVersionUidDefaultOldAsString();
              _builder.append(_serialVersionUidDefaultOldAsString_1);
              _builder.append("<br/>");
              _builder.newLineIfNotEmpty();
              _builder.append("New value: ");
              String _serialVersionUidDefaultNewAsString_1 = clazzReport_1.getSerialVersionUid().getSerialVersionUidDefaultNewAsString();
              _builder.append(_serialVersionUidDefaultNewAsString_1);
              _builder.append("<br/>");
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.append("<div class=\"summary\">");
          _builder.newLine();
          _builder.append("<a name=\"");
          String _fullyQualifiedName_1 = clazzReport_1.getFullyQualifiedName();
          _builder.append(_fullyQualifiedName_1);
          _builder.append("_summary\">");
          _builder.newLineIfNotEmpty();
          {
            boolean _isEmpty = clazzReport_1.getAnnotations().isEmpty();
            boolean _not = (!_isEmpty);
            if (_not) {
              _builder.append("<!-- ======== annotations SUMMARY ======== -->");
              _builder.newLine();
              _builder.append("<ul class=\"blockList\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<li class=\"blockList\"><a name=\"annotation_summary\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<!--   -->");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</a>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<h3>Annotations Summary</h3>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"annotations Summary table, listing annotations, and an explanation\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<tbody><tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst colStatus\" scope=\"col\">Status</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst\" scope=\"col\">Name</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colLast\" scope=\"col\">Elements</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              {
                List<JApiAnnotation> _annotations = clazzReport_1.getAnnotations();
                for(final JApiAnnotation annoChange : _annotations) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_1 = annoChange.getChangeStatus();
                  _builder.append(_changeStatus_1, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_1 = this.<JApiAnnotation>changeStatusLabel(annoChange);
                  _builder.append(_changeStatusLabel_1, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst\"><code>");
                  String _fullyQualifiedName_2 = annoChange.getFullyQualifiedName();
                  _builder.append(_fullyQualifiedName_2, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colLast\"><code>");
                  final Function1<JApiAnnotationElement, CharSequence> _function_1 = (JApiAnnotationElement it) -> {
                    return this.toHtml(it);
                  };
                  String _join = IterableExtensions.<JApiAnnotationElement>join(annoChange.getElements(), ", ", _function_1);
                  _builder.append(_join, "\t");
                  _builder.append("</code>&nbsp;</td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("</tr>");
                  _builder.newLine();
                }
              }
              _builder.append("\t");
              _builder.append("</tbody></table>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</li>");
              _builder.newLine();
              _builder.append("</ul>");
              _builder.newLine();
            }
          }
          {
            boolean _isEmpty_1 = clazzReport_1.getInterfaces().isEmpty();
            boolean _not_1 = (!_isEmpty_1);
            if (_not_1) {
              _builder.append("<!-- ======== interfaces SUMMARY ======== -->");
              _builder.newLine();
              _builder.append("<ul class=\"blockList\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<li class=\"blockList\"><a name=\"annotation_summary\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<!--   -->");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</a>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<h3>Interfaces Summary</h3>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"interfaces Summary table, listing interfaces, and an explanation\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<tbody><tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst colStatus\" scope=\"col\">Status</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst\" scope=\"col\">Name</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              {
                List<JApiImplementedInterface> _interfaces = clazzReport_1.getInterfaces();
                for(final JApiImplementedInterface annoChange_1 : _interfaces) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_2 = annoChange_1.getChangeStatus();
                  _builder.append(_changeStatus_2, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_2 = this.<JApiImplementedInterface>changeStatusLabel(annoChange_1);
                  _builder.append(_changeStatusLabel_2, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colLast\"><code>");
                  String _fullyQualifiedName_3 = annoChange_1.getFullyQualifiedName();
                  _builder.append(_fullyQualifiedName_3, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("</tr>");
                  _builder.newLine();
                }
              }
              _builder.append("\t");
              _builder.append("</tbody></table>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</li>");
              _builder.newLine();
              _builder.append("</ul>");
              _builder.newLine();
            }
          }
          {
            JApiChangeStatus _changeStatus_3 = clazzReport_1.getSuperclass().getChangeStatus();
            boolean _notEquals_1 = (!Objects.equal(_changeStatus_3, JApiChangeStatus.UNCHANGED));
            if (_notEquals_1) {
              _builder.append("<!-- ======== superclass SUMMARY ======== -->");
              _builder.newLine();
              _builder.append("<ul class=\"blockList\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<li class=\"blockList\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<h3>Superclass</h3>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"superclass Summary table, listing interfaces, and an explanation\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<tbody><tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst colStatus\" scope=\"col\">Status</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst\" scope=\"col\">Name</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<tr class=\"rowColor\">");
              _builder.newLine();
              _builder.append("\t\t");
              _builder.append("<td class=\"colFirst colStatus col");
              JApiChangeStatus _changeStatus_4 = clazzReport_1.getSuperclass().getChangeStatus();
              _builder.append(_changeStatus_4, "\t\t");
              _builder.append("\">");
              String _changeStatusLabel_3 = this.<JApiSuperclass>changeStatusLabel(clazzReport_1.getSuperclass());
              _builder.append(_changeStatusLabel_3, "\t\t");
              _builder.append("</code></td>");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("<td class=\"colLast\"><code>");
              String _get_2 = clazzReport_1.getSuperclass().getNewSuperclass().or(clazzReport_1.getSuperclass().getOldSuperclass()).get();
              _builder.append(_get_2, "\t\t");
              _builder.append("</code></td>");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</tbody></table>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</li>");
              _builder.newLine();
              _builder.append("</ul>");
              _builder.newLine();
            }
          }
          {
            boolean _isEmpty_2 = clazzReport_1.getFields().isEmpty();
            boolean _not_2 = (!_isEmpty_2);
            if (_not_2) {
              _builder.append("<!-- ======== FIELDS SUMMARY ======== -->");
              _builder.newLine();
              _builder.append("<ul class=\"blockList\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<li class=\"blockList\"><a name=\"field_summary\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<!--   -->");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</a>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<h3>Field Summary</h3>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Constructor Summary table, listing constructors, and an explanation\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<tbody><tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst colStatus\" scope=\"col\">Status</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst\" scope=\"col\">Modifier</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colLast\" scope=\"col\">Field</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              {
                List<JApiField> _fields = clazzReport_1.getFields();
                for(final JApiField fieldChange : _fields) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_5 = fieldChange.getChangeStatus();
                  _builder.append(_changeStatus_5, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_4 = this.<JApiField>changeStatusLabel(fieldChange);
                  _builder.append(_changeStatusLabel_4, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst\"><code>");
                  CharSequence _html = this.toHtml(fieldChange.getAccessModifier());
                  _builder.append(_html, "\t");
                  _builder.append(" </code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colLast\"><code><strong>");
                  String _name = fieldChange.getName();
                  _builder.append(_name, "\t");
                  _builder.append("</strong></code>&nbsp;</td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("</tr>");
                  _builder.newLine();
                }
              }
              _builder.append("\t");
              _builder.append("</tbody></table>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</li>");
              _builder.newLine();
              _builder.append("</ul>");
              _builder.newLine();
            }
          }
          {
            boolean _isEmpty_3 = clazzReport_1.getConstructors().isEmpty();
            boolean _not_3 = (!_isEmpty_3);
            if (_not_3) {
              _builder.append("<!-- ======== CONSTRUCTOR SUMMARY ======== -->");
              _builder.newLine();
              _builder.append("<ul class=\"blockList\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<li class=\"blockList\"><a name=\"constructor_summary\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<!--   -->");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</a>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<h3>Constructor Summary</h3>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Constructor Summary table, listing constructors, and an explanation\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<tbody><tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst colStatus\">Status</code></th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst\" scope=\"col\">Modifier</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colLast\" scope=\"col\">Constructor and Description</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              {
                List<JApiConstructor> _constructors = clazzReport_1.getConstructors();
                for(final JApiConstructor constructorChange : _constructors) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_6 = constructorChange.getChangeStatus();
                  _builder.append(_changeStatus_6, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_5 = this.<JApiConstructor>changeStatusLabel(constructorChange);
                  _builder.append(_changeStatusLabel_5, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst\"><code>");
                  CharSequence _html_1 = this.toHtml(constructorChange.getAccessModifier());
                  _builder.append(_html_1, "\t");
                  _builder.append(" </code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colLast\"><code><strong>");
                  String _name_1 = constructorChange.getName();
                  _builder.append(_name_1, "\t");
                  _builder.append("</strong>(");
                  final Function1<JApiParameter, CharSequence> _function_2 = (JApiParameter it) -> {
                    return this.toHtml(it);
                  };
                  String _join_1 = IterableExtensions.<JApiParameter>join(constructorChange.getParameters(), ", ", _function_2);
                  _builder.append(_join_1, "\t");
                  _builder.append(")</code>&nbsp;</td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("</tr>");
                  _builder.newLine();
                }
              }
              _builder.append("\t");
              _builder.append("</tbody></table>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</li>");
              _builder.newLine();
              _builder.append("</ul>");
              _builder.newLine();
            }
          }
          {
            boolean _isEmpty_4 = clazzReport_1.getMethods().isEmpty();
            boolean _not_4 = (!_isEmpty_4);
            if (_not_4) {
              _builder.append("<!-- ========== METHOD SUMMARY =========== -->");
              _builder.newLine();
              _builder.append("<ul class=\"blockList\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<li class=\"blockList\"><a name=\"method_summary\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<!--   -->");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</a>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<h3>Method Summary</h3>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Method Summary table, listing methods, and an explanation\">");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<tbody><tr>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst colStatus\">Status</code></th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colFirst\" scope=\"col\">Modifier and Type</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("<th class=\"colLast\" scope=\"col\">Method and Description</th>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</tr>");
              _builder.newLine();
              {
                List<JApiMethod> _methods = clazzReport_1.getMethods();
                for(final JApiMethod methodeChange : _methods) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_7 = methodeChange.getChangeStatus();
                  _builder.append(_changeStatus_7, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_6 = this.<JApiMethod>changeStatusLabel(methodeChange);
                  _builder.append(_changeStatusLabel_6, "\t");
                  _builder.append("</td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst\"><code><strong>");
                  CharSequence _html_2 = this.toHtml(methodeChange.getAccessModifier());
                  _builder.append(_html_2, "\t");
                  _builder.append("</strong> ");
                  CharSequence _html_3 = this.toHtml(methodeChange.getReturnType());
                  _builder.append(_html_3, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colLast\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("\t");
                  _builder.append("<code><strong>");
                  String _name_2 = methodeChange.getName();
                  _builder.append(_name_2, "\t\t");
                  _builder.append("</strong>(");
                  final Function1<JApiParameter, CharSequence> _function_3 = (JApiParameter it) -> {
                    return this.toHtml(it);
                  };
                  String _join_2 = IterableExtensions.<JApiParameter>join(methodeChange.getParameters(), ", ", _function_3);
                  _builder.append(_join_2, "\t\t");
                  _builder.append(")</code>&nbsp;");
                  _builder.newLineIfNotEmpty();
                  {
                    boolean _isEmpty_5 = methodeChange.getAnnotations().isEmpty();
                    boolean _not_5 = (!_isEmpty_5);
                    if (_not_5) {
                      _builder.append("\t");
                      _builder.append("\t");
                      _builder.append("- annotations: \t");
                      final Function1<JApiAnnotation, CharSequence> _function_4 = (JApiAnnotation it) -> {
                        return this.toHtml(it);
                      };
                      String _join_3 = IterableExtensions.<JApiAnnotation>join(methodeChange.getAnnotations(), ", ", _function_4);
                      _builder.append(_join_3, "\t\t");
                      _builder.newLineIfNotEmpty();
                    }
                  }
                  _builder.append("\t");
                  _builder.append("</td>");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("</tr>");
                  _builder.newLine();
                }
              }
              _builder.append("\t");
              _builder.append("</tbody></table>");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("</li>");
              _builder.newLine();
              _builder.append("</ul>");
              _builder.newLine();
            }
          }
          _builder.append("</div>");
          _builder.newLine();
          _builder.append("</li>");
          _builder.newLine();
          _builder.append("</ul>");
          _builder.newLine();
        }
      }
      return _builder.toString();
    };
    return this.packageFileBody(packageName, _function);
  }
  
  public <T extends JApiBinaryCompatibility & JApiHasChangeStatus> String changeStatusLabel(final T japiType) {
    JApiChangeStatus _changeStatus = japiType.getChangeStatus();
    String _xifexpression = null;
    boolean _isBinaryCompatible = japiType.isBinaryCompatible();
    boolean _not = (!_isBinaryCompatible);
    if (_not) {
      _xifexpression = " (!)";
    } else {
      _xifexpression = "";
    }
    return (_changeStatus + _xifexpression);
  }
  
  public CharSequence createStatistics(final Map<String, List<JApiClass>> byPackage) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
    _builder.newLine();
    _builder.append("<!-- NewPage -->");
    _builder.newLine();
    _builder.append("<html>");
    _builder.newLine();
    _builder.append("<head>");
    _builder.newLine();
    _builder.append("<title>Statistics (");
    String _documentationName = this.info.getDocumentationName();
    _builder.append(_documentationName);
    _builder.append(")</title>");
    _builder.newLineIfNotEmpty();
    _builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"resources/stylesheet.css\" title=\"Style\">");
    _builder.newLine();
    _builder.append("</head>");
    _builder.newLine();
    _builder.append("<body>");
    _builder.newLine();
    _builder.append("<div class=\"topNav\">");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<ul><Strong>");
    _builder.newLine();
    _builder.append("\t");
    String _documentationName_1 = this.info.getDocumentationName();
    _builder.append(_documentationName_1, "\t");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("</strong></ul>");
    _builder.newLine();
    _builder.append("</div>");
    _builder.newLine();
    _builder.append("<div class=\"subNav\">");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<div></div>");
    _builder.newLine();
    _builder.append("</div>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("<div class=\"header\">");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<h1 class=\"title\">Statistics of collected API changes</h1>");
    _builder.newLine();
    _builder.append("</div>");
    _builder.newLine();
    _builder.append("<div class=\"contentContainer\">");
    _builder.newLine();
    _builder.append("<div class=\"description\">");
    _builder.newLine();
    final Iterable<JApiClass> allClasses = Iterables.<JApiClass>concat(byPackage.values());
    _builder.newLineIfNotEmpty();
    _builder.append("<ul class=\"blocklist\">");
    _builder.newLine();
    _builder.append("<li class=\"blockList\">");
    _builder.newLine();
    _builder.append("<dl>");
    _builder.newLine();
    _builder.append("<dt>Packages changed:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    int _size = byPackage.keySet().size();
    _builder.append(_size);
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Classes changed:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    int _size_1 = IterableExtensions.size(allClasses);
    _builder.append(_size_1);
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Binary incompatible changes:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    final Function1<JApiClass, Boolean> _function = (JApiClass it) -> {
      boolean _isBinaryCompatible = it.isBinaryCompatible();
      return Boolean.valueOf((!_isBinaryCompatible));
    };
    int _size_2 = IterableExtensions.size(IterableExtensions.<JApiClass>filter(allClasses, _function));
    _builder.append(_size_2);
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Classes added:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    final Function1<JApiClass, Boolean> _function_1 = (JApiClass it) -> {
      JApiChangeStatus _changeStatus = it.getChangeStatus();
      return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.NEW));
    };
    int _size_3 = IterableExtensions.size(IterableExtensions.<JApiClass>filter(allClasses, _function_1));
    _builder.append(_size_3);
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Classes removed:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    final Function1<JApiClass, Boolean> _function_2 = (JApiClass it) -> {
      JApiChangeStatus _changeStatus = it.getChangeStatus();
      return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.REMOVED));
    };
    int _size_4 = IterableExtensions.size(IterableExtensions.<JApiClass>filter(allClasses, _function_2));
    _builder.append(_size_4);
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("</dl>");
    _builder.newLine();
    _builder.append("</li>");
    _builder.newLine();
    _builder.append("</ul>");
    _builder.newLine();
    _builder.append("</div>");
    _builder.newLine();
    _builder.append("<br>");
    _builder.newLine();
    _builder.append("<hr>");
    _builder.newLine();
    _builder.append("<a href=\"plain-report.html\" target=\"_blank\">Plain html report</a>&nbsp;&nbsp;&nbsp;&nbsp;");
    _builder.newLine();
    _builder.append("<a href=\"report.xml\" target=\"_blank\">Plain xml report</a>");
    _builder.newLine();
    _builder.append("</div>");
    _builder.newLine();
    _builder.append("</body>");
    _builder.newLine();
    _builder.append("</html>");
    _builder.newLine();
    return _builder;
  }
  
  public String toStatusLable(final JApiClass clazz) {
    String lable = clazz.getNewClass().or(clazz.getOldClass()).get().getSimpleName();
    boolean _isBinaryCompatible = clazz.isBinaryCompatible();
    boolean _not = (!_isBinaryCompatible);
    if (_not) {
      lable = (lable + "&nbsp;(!)");
    }
    return lable;
  }
  
  protected CharSequence _toHtml(final JApiModifier<AccessModifier> modifier) {
    CharSequence _xifexpression = null;
    JApiChangeStatus _changeStatus = modifier.getChangeStatus();
    boolean _equals = Objects.equal(_changeStatus, JApiChangeStatus.MODIFIED);
    if (_equals) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<strike>");
      String _lowerCase = modifier.getOldModifier().get().toString().toLowerCase();
      _builder.append(_lowerCase);
      _builder.append("</strike>&nbsp;");
      String _lowerCase_1 = modifier.getNewModifier().get().toString().toLowerCase();
      _builder.append(_lowerCase_1);
      _xifexpression = _builder;
    } else {
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("<code>");
      String _lowerCase_2 = modifier.getNewModifier().or(modifier.getOldModifier()).get().toString().toLowerCase();
      _builder_1.append(_lowerCase_2);
      _builder_1.append("</code>");
      _xifexpression = _builder_1;
    }
    return _xifexpression;
  }
  
  protected CharSequence _toHtml(final JApiParameter param) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<span title=\"");
    String _type = param.getType();
    _builder.append(_type);
    _builder.append("\">");
    String _cutQualifier = this.cutQualifier(param.getType());
    _builder.append(_cutQualifier);
    _builder.append("</span>");
    return _builder;
  }
  
  protected CharSequence _toHtml(final JApiReturnType retType) {
    CharSequence _xifexpression = null;
    JApiChangeStatus _changeStatus = retType.getChangeStatus();
    boolean _equals = Objects.equal(_changeStatus, JApiChangeStatus.MODIFIED);
    if (_equals) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<strike title=\"");
      String _oldReturnType = retType.getOldReturnType();
      _builder.append(_oldReturnType);
      _builder.append("\">");
      _builder.newLineIfNotEmpty();
      _builder.append("\t\t\t");
      String _cutQualifier = this.cutQualifier(retType.getOldReturnType());
      _builder.append(_cutQualifier, "\t\t\t");
      _builder.append("</strike>&nbsp;<span title=\"");
      String _newReturnType = retType.getNewReturnType();
      _builder.append(_newReturnType, "\t\t\t");
      _builder.append("\">");
      String _cutQualifier_1 = this.cutQualifier(retType.getNewReturnType());
      _builder.append(_cutQualifier_1, "\t\t\t");
      _builder.append("</span>");
      _xifexpression = _builder;
    } else {
      CharSequence _xifexpression_1 = null;
      String _newReturnType_1 = retType.getNewReturnType();
      boolean _notEquals = (!Objects.equal("n.a.", _newReturnType_1));
      if (_notEquals) {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("<span title=\"");
        String _newReturnType_2 = retType.getNewReturnType();
        _builder_1.append(_newReturnType_2);
        _builder_1.append("\">");
        String _cutQualifier_2 = this.cutQualifier(retType.getNewReturnType());
        _builder_1.append(_cutQualifier_2);
        _builder_1.append("</span>");
        _xifexpression_1 = _builder_1;
      } else {
        StringConcatenation _builder_2 = new StringConcatenation();
        _builder_2.append("<span title=\"");
        String _oldReturnType_1 = retType.getOldReturnType();
        _builder_2.append(_oldReturnType_1);
        _builder_2.append("\">");
        String _cutQualifier_3 = this.cutQualifier(retType.getOldReturnType());
        _builder_2.append(_cutQualifier_3);
        _builder_2.append("</span>");
        _xifexpression_1 = _builder_2;
      }
      _xifexpression = _xifexpression_1;
    }
    return _xifexpression;
  }
  
  public String cutQualifier(final String string) {
    final int lastDot = string.lastIndexOf(".");
    if ((lastDot > 0)) {
      return string.substring((lastDot + 1));
    }
    return string;
  }
  
  protected CharSequence _toHtml(final JApiAnnotationElement it) {
    CharSequence _xifexpression = null;
    JApiChangeStatus _changeStatus = it.getChangeStatus();
    boolean _equals = Objects.equal(_changeStatus, JApiChangeStatus.MODIFIED);
    if (_equals) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<strike>");
      String _string = it.getOldValue().get().toString();
      _builder.append(_string);
      _builder.append("</strike>&nbsp;");
      String _string_1 = it.getNewValue().get().toString();
      _builder.append(_string_1);
      _xifexpression = _builder;
    } else {
      String _xifexpression_1 = null;
      Optional<MemberValue> _newValue = it.getNewValue();
      boolean _notEquals = (!Objects.equal("n.a.", _newValue));
      if (_notEquals) {
        String _xifexpression_2 = null;
        boolean _isPresent = it.getNewValue().isPresent();
        if (_isPresent) {
          _xifexpression_2 = it.getNewValue().get().toString();
        }
        _xifexpression_1 = _xifexpression_2;
      } else {
        _xifexpression_1 = it.getOldValue().get().toString();
      }
      _xifexpression = _xifexpression_1;
    }
    return _xifexpression;
  }
  
  protected CharSequence _toHtml(final JApiAnnotation it) {
    CharSequence _xifexpression = null;
    JApiChangeStatus _changeStatus = it.getChangeStatus();
    boolean _equals = Objects.equal(_changeStatus, JApiChangeStatus.MODIFIED);
    if (_equals) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<strike>");
      String _typeName = it.getOldAnnotation().get().getTypeName();
      _builder.append(_typeName);
      _builder.append("</strike>&nbsp;");
      String _typeName_1 = it.getNewAnnotation().get().getTypeName();
      _builder.append(_typeName_1);
      _xifexpression = _builder;
    } else {
      String _xifexpression_1 = null;
      boolean _isPresent = it.getNewAnnotation().isPresent();
      if (_isPresent) {
        String _typeName_2 = it.getNewAnnotation().get().getTypeName();
        _xifexpression_1 = ("<font color=\'green\'>added</font> @" + _typeName_2);
      } else {
        String _typeName_3 = it.getOldAnnotation().get().getTypeName();
        _xifexpression_1 = ("<font color=\'red\'>removed</font> @" + _typeName_3);
      }
      _xifexpression = _xifexpression_1;
    }
    return _xifexpression;
  }
  
  public CharSequence packageFileBody(final String packageName, final Function0<? extends CharSequence> sequence) {
    CharSequence _xblockexpression = null;
    {
      final String depth = "../";
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
      _builder.newLine();
      _builder.append("<!-- NewPage -->");
      _builder.newLine();
      _builder.append("<html>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<head>");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("<title>");
      _builder.append(packageName, "\t\t");
      _builder.append(" (");
      String _documentationName = this.info.getDocumentationName();
      _builder.append(_documentationName, "\t\t");
      _builder.append(")</title>");
      _builder.newLineIfNotEmpty();
      _builder.append("\t\t");
      _builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
      _builder.append(depth, "\t\t");
      _builder.append("resources/stylesheet.css\" title=\"Style\">");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("</head>");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<body>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<!-- ========= START OF TOP NAVBAR ======= -->");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<div class=\"topNav\">");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("<ul><Strong>");
      _builder.newLine();
      _builder.append("\t\t");
      String _documentationName_1 = this.info.getDocumentationName();
      _builder.append(_documentationName_1, "\t\t");
      _builder.newLineIfNotEmpty();
      _builder.append("\t\t");
      _builder.append("</strong></ul>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("</div>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<div class=\"subNav\">");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("<div></div>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("</div>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<!-- ========= END OF TOP NAVBAR ========= -->");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<div class=\"header\">");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<h1 title=\"Package\" class=\"title\">Package&nbsp;");
      _builder.append(packageName, "\t");
      _builder.append("</h1>");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("</div>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<div class=\"contentContainer\">");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<ul class=\"blockList\">");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("<li class=\"blockList\">");
      _builder.newLine();
      _builder.append("\t");
      CharSequence _apply = sequence.apply();
      _builder.append(_apply, "\t");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("</li>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("</ul>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("</div>");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("</body>");
      _builder.newLine();
      _builder.append("</html>");
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public CharSequence toHtml(final Object it) {
    if (it instanceof JApiAnnotation) {
      return _toHtml((JApiAnnotation)it);
    } else if (it instanceof JApiAnnotationElement) {
      return _toHtml((JApiAnnotationElement)it);
    } else if (it instanceof JApiModifier) {
      return _toHtml((JApiModifier<AccessModifier>)it);
    } else if (it instanceof JApiParameter) {
      return _toHtml((JApiParameter)it);
    } else if (it instanceof JApiReturnType) {
      return _toHtml((JApiReturnType)it);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(it).toString());
    }
  }
}
