/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.javadoc.internal.doclets.formats.html;

import java.net.*;
import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.source.util.DocTreePath;
import com.sun.tools.doclint.DocLint;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTag;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlVersion;
import jdk.javadoc.internal.doclets.toolkit.Configuration;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.Resources;
import jdk.javadoc.internal.doclets.toolkit.WriterFactory;
import jdk.javadoc.internal.doclets.toolkit.util.DocFile;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

import static javax.tools.Diagnostic.Kind.*;

/**
 * Configure the output based on the command line options.
 * <p>
 * Also determine the length of the command line option. For example,
 * for a option "-header" there will be a string argument associated, then the
 * the length of option "-header" is two. But for option "-nohelp" no argument
 * is needed so it's length is 1.
 * </p>
 * <p>
 * Also do the error checking on the options used. For example it is illegal to
 * use "-helpfile" option when already "-nohelp" option is used.
 * </p>
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @author Robert Field.
 * @author Atul Dambalkar.
 * @author Jamie Ho
 * @author Bhavesh Patel (Modified)
 */
public class ConfigurationImpl extends Configuration {

    /**
     * The build date.  Note: For now, we will use
     * a version number instead of a date.
     */
    public static final String BUILD_DATE = System.getProperty("java.version");

    /**
     * Argument for command line option "-header".
     */
    public String header = "";

    /**
     * Argument for command line option "-packagesheader".
     */
    public String packagesheader = "";

    /**
     * Argument for command line option "-footer".
     */
    public String footer = "";

    /**
     * Argument for command line option "-doctitle".
     */
    public String doctitle = "";

    /**
     * Argument for command line option "-windowtitle".
     */
    public String windowtitle = "";

    /**
     * Argument for command line option "-top".
     */
    public String top = "";

    /**
     * Argument for command line option "-bottom".
     */
    public String bottom = "";

    /**
     * Argument for command line option "-helpfile".
     */
    public String helpfile = "";

    /**
     * Argument for command line option "-stylesheetfile".
     */
    public String stylesheetfile = "";

    /**
     * Argument for command line option "-Xdocrootparent".
     */
    public String docrootparent = "";

    public boolean sortedMethodDetails = false;

    /**
     * True if command line option "-nohelp" is used. Default value is false.
     */
    public boolean nohelp = false;

    /**
     * True if command line option "-splitindex" is used. Default value is
     * false.
     */
    public boolean splitindex = false;

    /**
     * False if command line option "-noindex" is used. Default value is true.
     */
    public boolean createindex = true;

    /**
     * True if command line option "-use" is used. Default value is false.
     */
    public boolean classuse = false;

    /**
     * False if command line option "-notree" is used. Default value is true.
     */
    public boolean createtree = true;

    /**
     * True if command line option "-nodeprecated" is used. Default value is
     * false.
     */
    public boolean nodeprecatedlist = false;

    /**
     * True if command line option "-nonavbar" is used. Default value is false.
     */
    public boolean nonavbar = false;

    /**
     * True if command line option "-nooverview" is used. Default value is
     * false
     */
    private boolean nooverview = false;

    /**
     * The overview path specified with "-overview" flag.
     */
    public String overviewpath = null;

    /**
     * This is true if option "-overview" is used or option "-overview" is not
     * used and number of packages is more than one.
     */
    public boolean createoverview = false;

    /**
     * Specifies whether or not frames should be generated.
     * Defaults to true; can be set by --frames; can be set to false by --no-frames; last one wins.
     */
    public boolean frames = true;

    /**
     * This is the HTML version of the generated pages. HTML 4.01 is the default output version.
     */
    public HtmlVersion htmlVersion = HtmlVersion.HTML4;

    /**
     * Collected set of doclint options
     */
    public Map<Doclet.Option, String> doclintOpts = new LinkedHashMap<>();

    public final Resources resources;

    /**
     * First file to appear in the right-hand frame in the generated
     * documentation.
     */
    public DocPath topFile = DocPath.empty;

    /**
     * The TypeElement for the class file getting generated.
     */
    public TypeElement currentTypeElement = null;  // Set this TypeElement in the ClassWriter.

    protected List<SearchIndexItem> memberSearchIndex = new ArrayList<>();

    protected List<SearchIndexItem> moduleSearchIndex = new ArrayList<>();

    protected List<SearchIndexItem> packageSearchIndex = new ArrayList<>();

    protected List<SearchIndexItem> tagSearchIndex = new ArrayList<>();

    protected List<SearchIndexItem> typeSearchIndex = new ArrayList<>();

    protected Map<Character,List<SearchIndexItem>> tagSearchIndexMap = new HashMap<>();

    protected Set<Character> tagSearchIndexKeys;

    protected Contents contents;

    protected Messages messages;

    /**
     * Constructor. Initializes resource for the
     * {@link com.sun.tools.doclets.internal.toolkit.util.MessageRetriever MessageRetriever}.
     */
    public ConfigurationImpl() {
        resources = new Resources(this,
                Configuration.sharedResourceBundleName,
                "jdk.javadoc.internal.doclets.formats.html.resources.standard");

        messages = new Messages(this);
        contents = new Contents(this);
    }

    private final String versionRBName = "jdk.javadoc.internal.tool.resources.version";
    private ResourceBundle versionRB;

    /**
     * Return the build date for the doclet.
     * @return the build date
     */
    @Override
    public String getDocletSpecificBuildDate() {
        if (versionRB == null) {
            try {
                versionRB = ResourceBundle.getBundle(versionRBName, getLocale());
            } catch (MissingResourceException e) {
                return BUILD_DATE;
            }
        }

        try {
            return versionRB.getString("release");
        } catch (MissingResourceException e) {
            return BUILD_DATE;
        }
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public Messages getMessages() {
        return messages;
    }

    protected boolean validateOptions() {
        // check shared options
        if (!generalValidOptions()) {
            return false;
        }
        boolean helpfileSeen = false;
        // otherwise look at our options
        for (Doclet.Option opt : optionsProcessed) {
            if (opt.matches("-helpfile")) {
                if (nohelp == true) {
                    reporter.print(ERROR, getText("doclet.Option_conflict",
                        "-helpfile", "-nohelp"));
                    return false;
                }
                if (helpfileSeen) {
                    reporter.print(ERROR, getText("doclet.Option_reuse",
                        "-helpfile"));
                    return false;
                }
                helpfileSeen = true;
                DocFile help = DocFile.createFileForInput(this, helpfile);
                if (!help.exists()) {
                    reporter.print(ERROR, getText("doclet.File_not_found", helpfile));
                    return false;
                }
            } else  if (opt.matches("-nohelp")) {
                if (helpfileSeen) {
                    reporter.print(ERROR, getText("doclet.Option_conflict",
                        "-nohelp", "-helpfile"));
                    return false;
                }
            } else if (opt.matches("-xdocrootparent")) {
                try {
                    URL ignored = new URL(docrootparent);
                } catch (MalformedURLException e) {
                    reporter.print(ERROR, getText("doclet.MalformedURL", docrootparent));
                    return false;
                }
            } else if (opt.matches("-overview")) {
                if (nooverview == true) {
                    reporter.print(ERROR, getText("doclet.Option_conflict",
                        "-overview", "-nooverview"));
                    return false;
                }
            } else  if (opt.matches("-nooverview")) {
                if (overviewpath != null) {
                    reporter.print(ERROR, getText("doclet.Option_conflict",
                        "-nooverview", "-overview"));
                    return false;
                }
            } else if (opt.matches("-splitindex")) {
                if (createindex == false) {
                    reporter.print(ERROR, getText("doclet.Option_conflict",
                        "-splitindex", "-noindex"));
                    return false;
                }
            } else if (opt.matches("-noindex")) {
                if (splitindex == true) {
                    reporter.print(ERROR, getText("doclet.Option_conflict",
                        "-noindex", "-splitindex"));
                    return false;
                }
            } else if (opt.matches("-xdoclint:")) {
                String dopt = doclintOpts.get(opt);
                if (dopt == null) {
                    continue;
                }
                if (dopt.contains("/")) {
                    reporter.print(ERROR, getText("doclet.Option_doclint_no_qualifiers"));
                    return false;
                }
                if (!DocLint.isValidOption(dopt)) {
                    reporter.print(ERROR, getText("doclet.Option_doclint_invalid_arg"));
                    return false;
                }
            } else if (opt.matches("-xdoclint/package:")) {
                 String dopt = doclintOpts.get(opt);
                    if (!DocLint.isValidOption(dopt)) {
                        reporter.print(ERROR, getText("doclet.Option_doclint_package_invalid_arg"));
                        return false;
                    }
                }
        }
        return true;
    }

    @Override
    public boolean finishOptionSettings() {
        if (!validateOptions()) {
            return false;
        }
        if (!docEnv.getSpecifiedElements().isEmpty()) {
            Map<String, PackageElement> map = new HashMap<>();
            PackageElement pkg;
            List<TypeElement> classes = new ArrayList<>(docEnv.getIncludedTypeElements());
            for (TypeElement aClass : classes) {
                pkg = utils.containingPackage(aClass);
                if (!map.containsKey(utils.getPackageName(pkg))) {
                    map.put(utils.getPackageName(pkg), pkg);
                }
            }
        }
        setCreateOverview();
        setTopFile(docEnv);
        workArounds.initDocLint(doclintOpts.values(), tagletManager.getCustomTagNames(),
                Utils.toLowerCase(htmlVersion.name()));
        return true;
    }

    /**
     * Return true if the generated output is HTML5.
     */
    public boolean isOutputHtml5() {
        return htmlVersion == HtmlVersion.HTML5;
    }

    /**
     * Return true if the tag is allowed for this specific version of HTML.
     */
    public boolean allowTag(HtmlTag htmlTag) {
        return htmlTag.allowTag(this.htmlVersion);
    }

    /**
     * Decide the page which will appear first in the right-hand frame. It will
     * be "overview-summary.html" if "-overview" option is used or no
     * "-overview" but the number of packages is more than one. It will be
     * "package-summary.html" of the respective package if there is only one
     * package to document. It will be a class page(first in the sorted order),
     * if only classes are provided on the command line.
     *
     * @param docEnv the doclet environment
     */
    protected void setTopFile(DocletEnvironment docEnv) {
        if (!checkForDeprecation(docEnv)) {
            return;
        }
        if (createoverview) {
            topFile = DocPaths.overviewSummary(frames);
        } else {
            if (showModules) {
                topFile = DocPath.empty.resolve(DocPaths.moduleSummary(modules.first()));
            } else if (packages.size() == 1 && packages.first().isUnnamed()) {
                List<TypeElement> classes = new ArrayList<>(docEnv.getIncludedTypeElements());
                if (!classes.isEmpty()) {
                    TypeElement te = getValidClass(classes);
                    topFile = DocPath.forClass(utils, te);
                }
            } else if (!packages.isEmpty()) {
                topFile = DocPath.forPackage(packages.first()).resolve(DocPaths.PACKAGE_SUMMARY);
            }
        }
    }

    protected TypeElement getValidClass(List<TypeElement> classes) {
        if (!nodeprecated) {
            return classes.get(0);
        }
        for (TypeElement te : classes) {
            if (!utils.isDeprecated(te)) {
                return te;
            }
        }
        return null;
    }

    protected boolean checkForDeprecation(DocletEnvironment docEnv) {
        for (TypeElement te : docEnv.getIncludedTypeElements()) {
            if (isGeneratedDoc(te)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate "overview.html" page if option "-overview" is used or number of
     * packages is more than one. Sets {@link #createoverview} field to true.
     */
    protected void setCreateOverview() {
        if ((overviewpath != null || packages.size() > 1) && !nooverview) {
            createoverview = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriterFactory getWriterFactory() {
        return new WriterFactoryImpl(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
        if (locale == null)
            return Locale.getDefault();
        return locale;
    }

    /**
     * Return the path of the overview file or null if it does not exist.
     *
     * @return the path of the overview file or null if it does not exist.
     */
    @Override
    public JavaFileObject getOverviewPath() {
        if (overviewpath != null && getFileManager() instanceof StandardJavaFileManager) {
            StandardJavaFileManager fm = (StandardJavaFileManager) getFileManager();
            return fm.getJavaFileObjects(overviewpath).iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JavaFileManager getFileManager() {
        return docEnv.getJavaFileManager();
    }

    @Override
    public boolean showMessage(DocTreePath path, String key) {
        return (path == null || workArounds.haveDocLint());
    }

    @Override
    public boolean showMessage(Element e, String key) {
        return (e == null || workArounds.haveDocLint());
    }

    @Override
    public String getText(String key) {
        return resources.getText(key);
    }

    @Override
    public String getText(String key, String... args) {
        return resources.getText(key, (Object[]) args);
    }

   /**
     * {@inheritdoc}
     */
    @Override
    public Content getContent(String key) {
        return contents.getContent(key);
    }

    /**
     * Get the configuration string as a content.
     *
     * @param key the key to look for in the configuration file
     * @param o   string or content argument added to configuration text
     * @return a content tree for the text
     */
    @Override
    public Content getContent(String key, Object o) {
        return contents.getContent(key, o);
    }

    /**
     * Get the configuration string as a content.
     *
     * @param key the key to look for in the configuration file
     * @param o1 resource argument
     * @param o2 resource argument
     * @return a content tree for the text
     */
    @Override
    public Content getContent(String key, Object o1, Object o2) {
        return contents.getContent(key, o1, o2);
    }

    /**
     * Get the configuration string as a content.
     *
     * @param key the key to look for in the configuration file
     * @param o0  string or content argument added to configuration text
     * @param o1  string or content argument added to configuration text
     * @param o2  string or content argument added to configuration text
     * @return a content tree for the text
     */
    @Override
    public Content getContent(String key, Object o0, Object o1, Object o2) {
        return contents.getContent(key, o0, o1, o2);
    }

    protected void buildSearchTagIndex() {
        for (SearchIndexItem sii : tagSearchIndex) {
            String tagLabel = sii.getLabel();
            Character unicode = (tagLabel.length() == 0)
                    ? '*'
                    : Character.toUpperCase(tagLabel.charAt(0));
            List<SearchIndexItem> list = tagSearchIndexMap.get(unicode);
            if (list == null) {
                list = new ArrayList<>();
                tagSearchIndexMap.put(unicode, list);
            }
            list.add(sii);
        }
        tagSearchIndexKeys = tagSearchIndexMap.keySet();
    }

    @Override
    public Set<Doclet.Option> getSupportedOptions() {
        Resources resources = getResources();
        Doclet.Option[] options = {
            new Option(resources, "-bottom", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    bottom = args.next();
                    return true;
                }
            },
            new Option(resources, "-charset", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    charset = args.next();
                    return true;
                }
            },
            new Option(resources, "-doctitle", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    doctitle = args.next();
                    return true;
                }
            },
            new Option(resources, "-footer", 1) {
                @Override
                public boolean process(String opt, ListIterator<String> args) {
                    optionsProcessed.add(this);
                    footer = args.next();
                    return true;
                }
            },
            new Option(resources, "-header", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    header = args.next();
                    return true;
                }
            },
            new Option(resources, "-helpfile", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    helpfile = args.next();
                    return true;
                }
            },
            new Option(resources, "-html4") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    htmlVersion = HtmlVersion.HTML4;
                    return true;
                }
            },
            new Option(resources, "-html5") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    htmlVersion = HtmlVersion.HTML5;
                    return true;
                }
            },
            new Option(resources, "-nohelp") {
                @Override
                public boolean process(String opt, ListIterator<String> args) {
                    optionsProcessed.add(this);
                    nohelp = true;
                    return true;
                }
            },
            new Option(resources, "-nodeprecatedlist") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    nodeprecatedlist = true;
                    return true;
                }
            },
            new Option(resources, "-noindex") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    createindex = false;
                    return true;
                }
            },
            new Option(resources, "-nonavbar") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    nonavbar = true;
                    return true;
                }
            },
            new Hidden(resources, "-nooverview") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    nooverview = true;
                    return true;
                }
            },
            new Option(resources, "-notree") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    createtree = false;
                    return true;
                }
            },
            new Option(resources, "-overview", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    overviewpath = args.next();
                    return true;
                }
            },
            new Option(resources, "--frames") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    frames = true;
                    return true;
                }
            },
            new Option(resources, "--no-frames") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    frames = false;
                    return true;
                }
            },
            new Hidden(resources, "-packagesheader", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    packagesheader = args.next();
                    return true;
                }
            },
            new Option(resources, "-splitindex") {
                @Override
                public boolean process(String opt, ListIterator<String> args) {
                    optionsProcessed.add(this);
                    splitindex = true;
                    return true;
                }
            },
            new Option(resources, "-stylesheetfile", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    stylesheetfile = args.next();
                    return true;
                }
            },
            new Option(resources, "-top", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    top = args.next();
                    return true;
                }
            },
            new Option(resources, "-use") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    classuse = true;
                    return true;
                }
            },
            new Option(resources, "-windowtitle", 1) {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    windowtitle = args.next().replaceAll("\\<.*?>", "");
                    return true;
                }
            },
            new XOption(resources, "-Xdoclint") {
                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    doclintOpts.put(this, DocLint.XMSGS_OPTION);
                    return true;
                }
            },
            new XOption(resources, "-Xdocrootparent", 1) {
                @Override
                public boolean process(String opt, ListIterator<String> args) {
                    optionsProcessed.add(this);
                    docrootparent = args.next();
                    return true;
                }
            },
            new XOption(resources, "doclet.usage.xdoclint-extended", "-Xdoclint:", 0) {
                @Override
                public boolean matches(String option) {
                    return option.toLowerCase().startsWith(getName().toLowerCase());
                }

                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    doclintOpts.put(this, opt.replace("-Xdoclint:", DocLint.XMSGS_CUSTOM_PREFIX));
                    return true;
                }
            },
            new XOption(resources, "doclet.usage.xdoclint-package", "-Xdoclint/package:", 0) {
                @Override
                public boolean matches(String option) {
                    return option.toLowerCase().startsWith(getName().toLowerCase());
                }

                @Override
                public boolean process(String opt,  ListIterator<String> args) {
                    optionsProcessed.add(this);
                    doclintOpts.put(this, opt.replace("-Xdoclint/package:", DocLint.XCHECK_PACKAGE));
                    return true;
                }
            }
        };
        Set<Doclet.Option> oset = new TreeSet<>();
        oset.addAll(Arrays.asList(options));
        oset.addAll(super.getSupportedOptions());
        return oset;
    }
}
