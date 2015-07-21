package japicmp.cmp;

import japicmp.model.JApiClass;

import java.util.List;

import javassist.CtClass;

public class JarArchiveComparatorExt extends JarArchiveComparator {

	public JarArchiveComparatorExt(JarArchiveComparatorOptions options) {
		super(options);
	}

	@Override
	public List<JApiClass> compareClassLists(JarArchiveComparatorOptions options, List<CtClass> oldClasses,
			List<CtClass> newClasses) {
		return super.compareClassLists(options, oldClasses, newClasses);
	}
}
