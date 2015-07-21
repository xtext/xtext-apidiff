package de.dhuebner.japicmp;

import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public class ReporterInformation {
  private String documentationName;
  
  private String outputFolder;
  
  @Pure
  public String getDocumentationName() {
    return this.documentationName;
  }
  
  public void setDocumentationName(final String documentationName) {
    this.documentationName = documentationName;
  }
  
  @Pure
  public String getOutputFolder() {
    return this.outputFolder;
  }
  
  public void setOutputFolder(final String outputFolder) {
    this.outputFolder = outputFolder;
  }
}
