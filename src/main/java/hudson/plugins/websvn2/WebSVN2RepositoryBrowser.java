/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.websvn2;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionRepositoryBrowser;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

/**
 * {@link SubversionRepositoryBrowser} that produces links to http://www.websvn.info/ for SVN compatible with Version
 * 2.3.1 of WebSVN.
 *
 * @author Christian Möller, based on work by Andreas Mandel, based on ViewVC plugin by Mike Salnikov, based on Polarion
 *         plug-in by Jonny Wray
 * @version $Revision: 83023 $ ($Date: 2016-06-16 15:48:53 +0200 (Do, 16 Jun 2016) $) by $Author: christian $
 */
public class WebSVN2RepositoryBrowser extends SubversionRepositoryBrowser {

  private static final long serialVersionUID = 6649912705777392101L;

  private static final String CHANGE_SET_FORMAT = "%1s?op=revision&rev=%2d";
  private static final String DIFF_FORMAT = "%1s/%2s?op=diff&rev=%3d";
  private static final String FILE_FORMAT = "%1s/%2s?rev=%3d";
  private static final Pattern WEBSVN_URL_PATTERN = Pattern.compile("(.*/wsvn)(/[^/]*)?/?");
  private static final int URL_PATTERN_BASE_URL_GROUP = 1;
  private static final int URL_PATTERN_REPNAME_GROUP = 2;

  public final URL url;
  private final URL baseUrl;
  private final String nameOfSvnRepository;

  @DataBoundConstructor
  public WebSVN2RepositoryBrowser(final URL url) throws MalformedURLException {
    final Matcher webSVNurl = WEBSVN_URL_PATTERN.matcher(url.toString());
    if (webSVNurl.matches()) {
      this.url = url;
      this.baseUrl = new URL(webSVNurl.group(URL_PATTERN_BASE_URL_GROUP) + "/");
      this.nameOfSvnRepository = webSVNurl.group(URL_PATTERN_REPNAME_GROUP).substring("/".length());
    } else {
      throw new MalformedURLException(
          "Please set a WebSVN2 url in the form http://<i>server</i>/<i>optional-path</i>/wsvn/<i>Name-of-SVN-repo</i>.");
    }
  }

  public String getRepname() {
    return nameOfSvnRepository;
  }

  @Override
  public URL getDiffLink(final Path path) throws IOException {
    return new URL(baseUrl, String.format(DIFF_FORMAT, nameOfSvnRepository, URLEncoder.encode(path.getValue(), "UTF-8"),
        path.getLogEntry().getRevision()));
  }

  @Override
  public URL getFileLink(final Path path) throws IOException {
    // TODO: If this is a dir we should rather use listing
    return new URL(baseUrl, String.format(FILE_FORMAT, nameOfSvnRepository, URLEncoder.encode(path.getValue(), "UTF-8"),
        path.getLogEntry().getRevision()));
  }

  @Override
  public URL getChangeSetLink(final LogEntry changeSet) throws IOException {
    return new URL(baseUrl, String.format(CHANGE_SET_FORMAT, nameOfSvnRepository, changeSet.getRevision()));
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {

    public DescriptorImpl() {
      super(WebSVN2RepositoryBrowser.class);
    }

    @Override
    public String getDisplayName() {
      return "WebSVN2";
    }

    public FormValidation doCheckReposUrl(@QueryParameter final String queryParameterValue) {
      if (queryParameterValue == null || queryParameterValue.isEmpty()) {
        return FormValidation.errorWithMarkup(
            "Please set a WebSVN2 url in the form http://<i>server</i>/<i>optional-path</i>/wsvn/<i>Name-of-SVN-repo</i>.");
      }

      final Matcher queryParameterAsUrlPatternMatcher = WEBSVN_URL_PATTERN.matcher(queryParameterValue);
      if (queryParameterAsUrlPatternMatcher.matches()) {
        try {
          @SuppressWarnings("unused")
          final URL syntaxCheckOnly = new URL(queryParameterAsUrlPatternMatcher.group(URL_PATTERN_BASE_URL_GROUP));
        } catch (final MalformedURLException ex) {
          return FormValidation.error("The entered url is not accepted: " + ex.getLocalizedMessage());
        }

        final String nameOfSvnRepository = queryParameterAsUrlPatternMatcher.group(URL_PATTERN_REPNAME_GROUP);
        if (nameOfSvnRepository == null || nameOfSvnRepository.isEmpty() || nameOfSvnRepository.equals("/")) {
          return FormValidation.errorWithMarkup(
              "Please set a WebSVN2 url containing SVN repository in the form http://<i>server</i>/<i>optional-path</i>/wsvn/<i>Name-of-SVN-repo</i>.");
        }

        return FormValidation.ok();
      }

      return FormValidation.errorWithMarkup(
          "Please set a WebSVN2 url containing wsvn path as well as SVN repository in the form http://<i>server</i>/<i>optional-path</i>/wsvn/<i>Name-of-SVN-repo</i>.");
    }

    @Override
    public WebSVN2RepositoryBrowser newInstance(final StaplerRequest staplerRequest, final JSONObject formData)
        throws FormException {
      return staplerRequest.bindParameters(WebSVN2RepositoryBrowser.class, "webSVN2.");
    }
  }
}
