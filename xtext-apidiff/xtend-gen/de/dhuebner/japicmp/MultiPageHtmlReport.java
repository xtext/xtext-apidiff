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
import japicmp.model.JApiSerialVersionUid;
import japicmp.model.JApiSuperclass;
import japicmp.output.xml.XmlOutputGenerator;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.CtClass;
import javassist.bytecode.annotation.Annotation;
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
        Optional<CtClass> _newClass = it.getNewClass();
        Optional<CtClass> _oldClass = it.getOldClass();
        Optional<CtClass> _or = _newClass.or(_oldClass);
        CtClass _get = _or.get();
        return _get.getPackageName();
      };
      final Map<String, List<JApiClass>> byPackage = IterableExtensions.<String, JApiClass>groupBy(this.jApiClasses, _function);
      String _plus = (outputFolder + "/changes.html");
      PrintWriter _printWriter = new PrintWriter(_plus);
      CharSequence _createStartPage = this.createStartPage();
      PrintWriter _append = _printWriter.append(_createStartPage);
      _append.close();
      String _plus_1 = (outputFolder + "/package-overview.html");
      PrintWriter _printWriter_1 = new PrintWriter(_plus_1);
      final Function1<JApiClass, Boolean> _function_1 = (JApiClass it) -> {
        JApiChangeStatus _changeStatus = it.getChangeStatus();
        return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.MODIFIED));
      };
      CharSequence _createMenu = this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.OVERVIEW, _function_1);
      PrintWriter _append_1 = _printWriter_1.append(_createMenu);
      _append_1.close();
      String _plus_2 = (outputFolder + "/removed-overview.html");
      PrintWriter _printWriter_2 = new PrintWriter(_plus_2);
      final Function1<JApiClass, Boolean> _function_2 = (JApiClass it) -> {
        JApiChangeStatus _changeStatus = it.getChangeStatus();
        return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.REMOVED));
      };
      CharSequence _createMenu_1 = this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.REMOVED, _function_2);
      PrintWriter _append_2 = _printWriter_2.append(_createMenu_1);
      _append_2.close();
      String _plus_3 = (outputFolder + "/added-overview.html");
      PrintWriter _printWriter_3 = new PrintWriter(_plus_3);
      final Function1<JApiClass, Boolean> _function_3 = (JApiClass it) -> {
        JApiChangeStatus _changeStatus = it.getChangeStatus();
        return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.NEW));
      };
      CharSequence _createMenu_2 = this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.ADDED, _function_3);
      PrintWriter _append_3 = _printWriter_3.append(_createMenu_2);
      _append_3.close();
      String _plus_4 = (outputFolder + "/breaking-overview.html");
      PrintWriter _printWriter_4 = new PrintWriter(_plus_4);
      final Function1<JApiClass, Boolean> _function_4 = (JApiClass it) -> {
        boolean _isBinaryCompatible = it.isBinaryCompatible();
        return Boolean.valueOf((!_isBinaryCompatible));
      };
      CharSequence _createMenu_3 = this.createMenu(byPackage, MultiPageHtmlReport.MenuKind.BREAKING, _function_4);
      PrintWriter _append_4 = _printWriter_4.append(_createMenu_3);
      _append_4.close();
      String _plus_5 = (outputFolder + "/statistics.html");
      PrintWriter _printWriter_5 = new PrintWriter(_plus_5);
      CharSequence _createStatistics = this.createStatistics(byPackage);
      PrintWriter _append_5 = _printWriter_5.append(_createStatistics);
      _append_5.close();
      File _file = new File(outputFolder, "packages");
      _file.mkdirs();
      Set<String> _keySet = byPackage.keySet();
      for (final String packageName : _keySet) {
        {
          String _plus_6 = (outputFolder + "/packages/");
          String _plus_7 = (_plus_6 + packageName);
          String _plus_8 = (_plus_7 + ".html");
          final PrintWriter writer = new PrintWriter(_plus_8);
          CharSequence _createPackageSiteContent = this.createPackageSiteContent(packageName, byPackage);
          PrintWriter _append_6 = writer.append(_createPackageSiteContent);
          _append_6.close();
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
      Class<? extends MultiPageHtmlReport> _class = this.getClass();
      ClassLoader _classLoader = _class.getClassLoader();
      InputStream _resourceAsStream = _classLoader.getResourceAsStream(("html/" + fileName));
      File _file = new File(destFolder, fileName);
      Path _path = _file.toPath();
      return Files.copy(_resourceAsStream, _path, 
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
    _builder.append(_documentationName, "");
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
      _builder.append(_documentationName, "");
      _builder.append(")</title>");
      _builder.newLineIfNotEmpty();
      _builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
      _builder.append(depth, "");
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
          List<JApiClass> _get = byPackage.get(packageName);
          final Iterable<JApiClass> removed = IterableExtensions.<JApiClass>filter(_get, ((Function1<? super JApiClass, Boolean>)filter));
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
          _builder.append(_changeStatus, "");
          _builder.append("\">");
          String _changeStatusLabel = this.<JApiClass>changeStatusLabel(clazzReport);
          _builder.append(_changeStatusLabel, "");
          _builder.append("</td>");
          _builder.newLineIfNotEmpty();
          _builder.append("<td class=\"colLast\"><a href=\"#");
          String _fullyQualifiedName = clazzReport.getFullyQualifiedName();
          _builder.append(_fullyQualifiedName, "");
          _builder.append("_summary\" title=\"class in ");
          _builder.append(packageName, "");
          _builder.append("\">");
          Optional<CtClass> _newClass = clazzReport.getNewClass();
          Optional<CtClass> _oldClass = clazzReport.getOldClass();
          Optional<CtClass> _or = _newClass.or(_oldClass);
          CtClass _get_1 = _or.get();
          String _simpleName = _get_1.getSimpleName();
          _builder.append(_simpleName, "");
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
        List<JApiClass> _get_2 = byPackage.get(packageName);
        for(final JApiClass clazzReport_1 : _get_2) {
          Optional<CtClass> _newClass_1 = clazzReport_1.getNewClass();
          Optional<CtClass> _oldClass_1 = clazzReport_1.getOldClass();
          Optional<CtClass> _or_1 = _newClass_1.or(_oldClass_1);
          final CtClass clazz = _or_1.get();
          _builder.newLineIfNotEmpty();
          _builder.append("<ul class=\"blockList\">");
          _builder.newLine();
          _builder.append("<li class=\"blockList\">");
          _builder.newLine();
          _builder.append("<h2 title=\"Class ");
          String _simpleName_1 = clazz.getSimpleName();
          _builder.append(_simpleName_1, "");
          _builder.append("\" class=\"title\">Class ");
          String _simpleName_2 = clazz.getSimpleName();
          _builder.append(_simpleName_2, "");
          _builder.append(" (");
          JApiChangeStatus _changeStatus_1 = clazzReport_1.getChangeStatus();
          String _string = _changeStatus_1.toString();
          String _lowerCase = _string.toLowerCase();
          _builder.append(_lowerCase, "");
          _builder.append(")</h2>");
          _builder.newLineIfNotEmpty();
          {
            JApiSerialVersionUid _serialVersionUid = clazzReport_1.getSerialVersionUid();
            String _serialVersionUidDefaultOldAsString = _serialVersionUid.getSerialVersionUidDefaultOldAsString();
            JApiSerialVersionUid _serialVersionUid_1 = clazzReport_1.getSerialVersionUid();
            String _serialVersionUidDefaultNewAsString = _serialVersionUid_1.getSerialVersionUidDefaultNewAsString();
            boolean _notEquals = (!Objects.equal(_serialVersionUidDefaultOldAsString, _serialVersionUidDefaultNewAsString));
            if (_notEquals) {
              _builder.append("<h3><font color=\"red\">(Serializable incompatible(!): default serialVersionUID changed)</font></h3>");
              _builder.newLine();
              _builder.append("Old value: ");
              JApiSerialVersionUid _serialVersionUid_2 = clazzReport_1.getSerialVersionUid();
              String _serialVersionUidDefaultOldAsString_1 = _serialVersionUid_2.getSerialVersionUidDefaultOldAsString();
              _builder.append(_serialVersionUidDefaultOldAsString_1, "");
              _builder.append("<br/>");
              _builder.newLineIfNotEmpty();
              _builder.append("New value: ");
              JApiSerialVersionUid _serialVersionUid_3 = clazzReport_1.getSerialVersionUid();
              String _serialVersionUidDefaultNewAsString_1 = _serialVersionUid_3.getSerialVersionUidDefaultNewAsString();
              _builder.append(_serialVersionUidDefaultNewAsString_1, "");
              _builder.append("<br/>");
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.append("<div class=\"summary\">");
          _builder.newLine();
          _builder.append("<a name=\"");
          String _fullyQualifiedName_1 = clazzReport_1.getFullyQualifiedName();
          _builder.append(_fullyQualifiedName_1, "");
          _builder.append("_summary\">");
          _builder.newLineIfNotEmpty();
          {
            List<JApiAnnotation> _annotations = clazzReport_1.getAnnotations();
            boolean _isEmpty = _annotations.isEmpty();
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
                List<JApiAnnotation> _annotations_1 = clazzReport_1.getAnnotations();
                for(final JApiAnnotation annoChange : _annotations_1) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_2 = annoChange.getChangeStatus();
                  _builder.append(_changeStatus_2, "\t");
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
                  List<JApiAnnotationElement> _elements = annoChange.getElements();
                  final Function1<JApiAnnotationElement, CharSequence> _function_1 = (JApiAnnotationElement it) -> {
                    return this.toHtml(it);
                  };
                  String _join = IterableExtensions.<JApiAnnotationElement>join(_elements, ", ", _function_1);
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
            List<JApiImplementedInterface> _interfaces = clazzReport_1.getInterfaces();
            boolean _isEmpty_1 = _interfaces.isEmpty();
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
                List<JApiImplementedInterface> _interfaces_1 = clazzReport_1.getInterfaces();
                for(final JApiImplementedInterface annoChange_1 : _interfaces_1) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_3 = annoChange_1.getChangeStatus();
                  _builder.append(_changeStatus_3, "\t");
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
            JApiSuperclass _superclass = clazzReport_1.getSuperclass();
            JApiChangeStatus _changeStatus_4 = _superclass.getChangeStatus();
            boolean _notEquals_1 = (!Objects.equal(_changeStatus_4, JApiChangeStatus.UNCHANGED));
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
              JApiSuperclass _superclass_1 = clazzReport_1.getSuperclass();
              JApiChangeStatus _changeStatus_5 = _superclass_1.getChangeStatus();
              _builder.append(_changeStatus_5, "\t\t");
              _builder.append("\">");
              JApiSuperclass _superclass_2 = clazzReport_1.getSuperclass();
              String _changeStatusLabel_3 = this.<JApiSuperclass>changeStatusLabel(_superclass_2);
              _builder.append(_changeStatusLabel_3, "\t\t");
              _builder.append("</code></td>");
              _builder.newLineIfNotEmpty();
              _builder.append("\t\t");
              _builder.append("<td class=\"colLast\"><code>");
              JApiSuperclass _superclass_3 = clazzReport_1.getSuperclass();
              Optional<String> _newSuperclass = _superclass_3.getNewSuperclass();
              JApiSuperclass _superclass_4 = clazzReport_1.getSuperclass();
              Optional<String> _oldSuperclass = _superclass_4.getOldSuperclass();
              Optional<String> _or_2 = _newSuperclass.or(_oldSuperclass);
              String _get_3 = _or_2.get();
              _builder.append(_get_3, "\t\t");
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
            List<JApiField> _fields = clazzReport_1.getFields();
            boolean _isEmpty_2 = _fields.isEmpty();
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
                List<JApiField> _fields_1 = clazzReport_1.getFields();
                for(final JApiField fieldChange : _fields_1) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_6 = fieldChange.getChangeStatus();
                  _builder.append(_changeStatus_6, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_4 = this.<JApiField>changeStatusLabel(fieldChange);
                  _builder.append(_changeStatusLabel_4, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst\"><code>");
                  JApiModifier<AccessModifier> _accessModifier = fieldChange.getAccessModifier();
                  CharSequence _html = this.toHtml(_accessModifier);
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
            List<JApiConstructor> _constructors = clazzReport_1.getConstructors();
            boolean _isEmpty_3 = _constructors.isEmpty();
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
                List<JApiConstructor> _constructors_1 = clazzReport_1.getConstructors();
                for(final JApiConstructor constructorChange : _constructors_1) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_7 = constructorChange.getChangeStatus();
                  _builder.append(_changeStatus_7, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_5 = this.<JApiConstructor>changeStatusLabel(constructorChange);
                  _builder.append(_changeStatusLabel_5, "\t");
                  _builder.append("</code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst\"><code>");
                  JApiModifier<AccessModifier> _accessModifier_1 = constructorChange.getAccessModifier();
                  CharSequence _html_1 = this.toHtml(_accessModifier_1);
                  _builder.append(_html_1, "\t");
                  _builder.append(" </code></td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colLast\"><code><strong>");
                  String _name_1 = constructorChange.getName();
                  _builder.append(_name_1, "\t");
                  _builder.append("</strong>(");
                  List<JApiParameter> _parameters = constructorChange.getParameters();
                  final Function1<JApiParameter, CharSequence> _function_2 = (JApiParameter it) -> {
                    return this.toHtml(it);
                  };
                  String _join_1 = IterableExtensions.<JApiParameter>join(_parameters, ", ", _function_2);
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
            List<JApiMethod> _methods = clazzReport_1.getMethods();
            boolean _isEmpty_4 = _methods.isEmpty();
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
                List<JApiMethod> _methods_1 = clazzReport_1.getMethods();
                for(final JApiMethod methodeChange : _methods_1) {
                  _builder.append("\t");
                  _builder.append("<tr class=\"rowColor\">");
                  _builder.newLine();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst colStatus col");
                  JApiChangeStatus _changeStatus_8 = methodeChange.getChangeStatus();
                  _builder.append(_changeStatus_8, "\t");
                  _builder.append("\">");
                  String _changeStatusLabel_6 = this.<JApiMethod>changeStatusLabel(methodeChange);
                  _builder.append(_changeStatusLabel_6, "\t");
                  _builder.append("</td>");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("<td class=\"colFirst\"><code><strong>");
                  JApiModifier<AccessModifier> _accessModifier_2 = methodeChange.getAccessModifier();
                  CharSequence _html_2 = this.toHtml(_accessModifier_2);
                  _builder.append(_html_2, "\t");
                  _builder.append("</strong> ");
                  JApiReturnType _returnType = methodeChange.getReturnType();
                  CharSequence _html_3 = this.toHtml(_returnType);
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
                  List<JApiParameter> _parameters_1 = methodeChange.getParameters();
                  final Function1<JApiParameter, CharSequence> _function_3 = (JApiParameter it) -> {
                    return this.toHtml(it);
                  };
                  String _join_2 = IterableExtensions.<JApiParameter>join(_parameters_1, ", ", _function_3);
                  _builder.append(_join_2, "\t\t");
                  _builder.append(")</code>&nbsp;");
                  _builder.newLineIfNotEmpty();
                  {
                    List<JApiAnnotation> _annotations_2 = methodeChange.getAnnotations();
                    boolean _isEmpty_5 = _annotations_2.isEmpty();
                    boolean _not_5 = (!_isEmpty_5);
                    if (_not_5) {
                      _builder.append("\t");
                      _builder.append("\t");
                      _builder.append("- annotations: \t");
                      List<JApiAnnotation> _annotations_3 = methodeChange.getAnnotations();
                      final Function1<JApiAnnotation, CharSequence> _function_4 = (JApiAnnotation it) -> {
                        return this.toHtml(it);
                      };
                      String _join_3 = IterableExtensions.<JApiAnnotation>join(_annotations_3, ", ", _function_4);
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
    _builder.append(_documentationName, "");
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
    _builder.append("<h1 class=\"title\">Stistic of collected API changes</h1>");
    _builder.newLine();
    _builder.append("</div>");
    _builder.newLine();
    _builder.append("<div class=\"contentContainer\">");
    _builder.newLine();
    _builder.append("<div class=\"description\">");
    _builder.newLine();
    Collection<List<JApiClass>> _values = byPackage.values();
    final Iterable<JApiClass> allClasses = Iterables.<JApiClass>concat(_values);
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
    Set<String> _keySet = byPackage.keySet();
    int _size = _keySet.size();
    _builder.append(_size, "");
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Classes changed:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    int _size_1 = IterableExtensions.size(allClasses);
    _builder.append(_size_1, "");
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Binary incompatible changes:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    final Function1<JApiClass, Boolean> _function = (JApiClass it) -> {
      boolean _isBinaryCompatible = it.isBinaryCompatible();
      return Boolean.valueOf((!_isBinaryCompatible));
    };
    Iterable<JApiClass> _filter = IterableExtensions.<JApiClass>filter(allClasses, _function);
    int _size_2 = IterableExtensions.size(_filter);
    _builder.append(_size_2, "");
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Classes added:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    final Function1<JApiClass, Boolean> _function_1 = (JApiClass it) -> {
      JApiChangeStatus _changeStatus = it.getChangeStatus();
      return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.NEW));
    };
    Iterable<JApiClass> _filter_1 = IterableExtensions.<JApiClass>filter(allClasses, _function_1);
    int _size_3 = IterableExtensions.size(_filter_1);
    _builder.append(_size_3, "");
    _builder.append("</dd>");
    _builder.newLineIfNotEmpty();
    _builder.append("<dt>Classes removed:</dt>");
    _builder.newLine();
    _builder.append("<dd>");
    final Function1<JApiClass, Boolean> _function_2 = (JApiClass it) -> {
      JApiChangeStatus _changeStatus = it.getChangeStatus();
      return Boolean.valueOf(Objects.equal(_changeStatus, JApiChangeStatus.REMOVED));
    };
    Iterable<JApiClass> _filter_2 = IterableExtensions.<JApiClass>filter(allClasses, _function_2);
    int _size_4 = IterableExtensions.size(_filter_2);
    _builder.append(_size_4, "");
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
    Optional<CtClass> _newClass = clazz.getNewClass();
    Optional<CtClass> _oldClass = clazz.getOldClass();
    Optional<CtClass> _or = _newClass.or(_oldClass);
    CtClass _get = _or.get();
    String lable = _get.getSimpleName();
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
      Optional<AccessModifier> _oldModifier = modifier.getOldModifier();
      AccessModifier _get = _oldModifier.get();
      String _string = _get.toString();
      String _lowerCase = _string.toLowerCase();
      _builder.append(_lowerCase, "");
      _builder.append("</strike>&nbsp;");
      Optional<AccessModifier> _newModifier = modifier.getNewModifier();
      AccessModifier _get_1 = _newModifier.get();
      String _string_1 = _get_1.toString();
      String _lowerCase_1 = _string_1.toLowerCase();
      _builder.append(_lowerCase_1, "");
      _xifexpression = _builder;
    } else {
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("<code>");
      Optional<AccessModifier> _newModifier_1 = modifier.getNewModifier();
      Optional<AccessModifier> _oldModifier_1 = modifier.getOldModifier();
      Optional<AccessModifier> _or = _newModifier_1.or(_oldModifier_1);
      AccessModifier _get_2 = _or.get();
      String _string_2 = _get_2.toString();
      String _lowerCase_2 = _string_2.toLowerCase();
      _builder_1.append(_lowerCase_2, "");
      _builder_1.append("</code>");
      _xifexpression = _builder_1;
    }
    return _xifexpression;
  }
  
  protected CharSequence _toHtml(final JApiParameter param) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<span title=\"");
    String _type = param.getType();
    _builder.append(_type, "");
    _builder.append("\">");
    String _type_1 = param.getType();
    String _cutQualifier = this.cutQualifier(_type_1);
    _builder.append(_cutQualifier, "");
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
      _builder.append(_oldReturnType, "");
      _builder.append("\">");
      _builder.newLineIfNotEmpty();
      _builder.append("\t\t\t");
      String _oldReturnType_1 = retType.getOldReturnType();
      String _cutQualifier = this.cutQualifier(_oldReturnType_1);
      _builder.append(_cutQualifier, "\t\t\t");
      _builder.append("</strike>&nbsp;<span title=\"");
      String _newReturnType = retType.getNewReturnType();
      _builder.append(_newReturnType, "\t\t\t");
      _builder.append("\">");
      String _newReturnType_1 = retType.getNewReturnType();
      String _cutQualifier_1 = this.cutQualifier(_newReturnType_1);
      _builder.append(_cutQualifier_1, "\t\t\t");
      _builder.append("</span>");
      _xifexpression = _builder;
    } else {
      CharSequence _xifexpression_1 = null;
      String _newReturnType_2 = retType.getNewReturnType();
      boolean _notEquals = (!Objects.equal("n.a.", _newReturnType_2));
      if (_notEquals) {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("<span title=\"");
        String _newReturnType_3 = retType.getNewReturnType();
        _builder_1.append(_newReturnType_3, "");
        _builder_1.append("\">");
        String _newReturnType_4 = retType.getNewReturnType();
        String _cutQualifier_2 = this.cutQualifier(_newReturnType_4);
        _builder_1.append(_cutQualifier_2, "");
        _builder_1.append("</span>");
        _xifexpression_1 = _builder_1;
      } else {
        StringConcatenation _builder_2 = new StringConcatenation();
        _builder_2.append("<span title=\"");
        String _oldReturnType_2 = retType.getOldReturnType();
        _builder_2.append(_oldReturnType_2, "");
        _builder_2.append("\">");
        String _oldReturnType_3 = retType.getOldReturnType();
        String _cutQualifier_3 = this.cutQualifier(_oldReturnType_3);
        _builder_2.append(_cutQualifier_3, "");
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
      Optional<MemberValue> _oldValue = it.getOldValue();
      MemberValue _get = _oldValue.get();
      String _string = _get.toString();
      _builder.append(_string, "");
      _builder.append("</strike>&nbsp;");
      Optional<MemberValue> _newValue = it.getNewValue();
      MemberValue _get_1 = _newValue.get();
      String _string_1 = _get_1.toString();
      _builder.append(_string_1, "");
      _xifexpression = _builder;
    } else {
      String _xifexpression_1 = null;
      Optional<MemberValue> _newValue_1 = it.getNewValue();
      boolean _notEquals = (!Objects.equal("n.a.", _newValue_1));
      if (_notEquals) {
        Optional<MemberValue> _newValue_2 = it.getNewValue();
        MemberValue _get_2 = _newValue_2.get();
        _xifexpression_1 = _get_2.toString();
      } else {
        Optional<MemberValue> _oldValue_1 = it.getOldValue();
        MemberValue _get_3 = _oldValue_1.get();
        _xifexpression_1 = _get_3.toString();
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
      Optional<Annotation> _oldAnnotation = it.getOldAnnotation();
      Annotation _get = _oldAnnotation.get();
      String _typeName = _get.getTypeName();
      _builder.append(_typeName, "");
      _builder.append("</strike>&nbsp;");
      Optional<Annotation> _newAnnotation = it.getNewAnnotation();
      Annotation _get_1 = _newAnnotation.get();
      String _typeName_1 = _get_1.getTypeName();
      _builder.append(_typeName_1, "");
      _xifexpression = _builder;
    } else {
      String _xifexpression_1 = null;
      Optional<Annotation> _newAnnotation_1 = it.getNewAnnotation();
      boolean _isPresent = _newAnnotation_1.isPresent();
      if (_isPresent) {
        Optional<Annotation> _newAnnotation_2 = it.getNewAnnotation();
        Annotation _get_2 = _newAnnotation_2.get();
        String _typeName_2 = _get_2.getTypeName();
        _xifexpression_1 = ("<font color=\'green\'>added</font> @" + _typeName_2);
      } else {
        Optional<Annotation> _oldAnnotation_1 = it.getOldAnnotation();
        Annotation _get_3 = _oldAnnotation_1.get();
        String _typeName_3 = _get_3.getTypeName();
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
