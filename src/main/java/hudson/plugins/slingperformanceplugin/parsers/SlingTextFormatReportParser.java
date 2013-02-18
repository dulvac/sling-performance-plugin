package hudson.plugins.slingperformanceplugin.parsers;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.slingperformanceplugin.SlingReportSample;
import hudson.plugins.slingperformanceplugin.reports.PerformanceReport;

/**
 * Parser for Sling Performance test reports.
 */
public class SlingTextFormatReportParser extends GenericReportParser {

    public enum Columns {
        TEST_SUITE("Test Suite"),
        TEST_CASE("Test Case"),
        TEST_CLASS("Test Class"),
        TEST_METHOD("Test Method"),
        DATETIME("DateTime"),
        MIN("min"),
        TEN_PERCENT("10%"),
        FIFTY_PERCENT("50%"),
        NINETY_PERCENT("90%"),
        MAX("max");


        private Columns(final String text) {
            this.text = text;
        }

        private final String text;

        @Override
        public String toString() {
            return text;
        }
    }

    @Extension
    public static final class DescriptorImpl extends PerformanceReportParserDescriptor {

        @Override
        public String getDisplayName() {
            return "Sling text format performance report";
        }
    }

    @DataBoundConstructor
    public SlingTextFormatReportParser(String glob) {
        super(glob);
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.txt";
    }

    @Override
    public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build,
            Collection<File> reports, TaskListener listener) throws IOException {

        List<PerformanceReport> result = new ArrayList<PerformanceReport>();
        for (File f : reports) {

            PrintStream logger = listener.getLogger();
            BufferedReader fis = new BufferedReader(new FileReader(f));
            logger.println("Performance: Parsing Sling Performance report file " + f.getName());
            logger.println("Report path:" + f.getAbsolutePath());

            boolean firstRow = true;
            boolean parsingFailed = false;
            String resultRow;
            ArrayList<String> columns = new ArrayList<String>();

            // Read results
            while ((resultRow = fis.readLine()) != null) {
                if (firstRow) {
                    if (!resultRow.contains(Columns.TEST_SUITE.toString())) {
                        parsingFailed = true;
                        break;
                    }

                    // get columns
                    for (String column : Arrays.asList(resultRow.split("\\|"))) {
                        columns.add(column.trim());
                    }

                    // done parsing header columns
                    firstRow = false;
                    continue;

                }
                // columns parsed, following result lines
                // parse results
                String[] results = resultRow.split("\\|");
                for (int i = 0; i < results.length; i++) {
                    results[i] = results[i].trim();
                }

                final PerformanceReport r = new PerformanceReport();
                r.setReportFileName(f.getName());
                SlingReportSample sample = new SlingReportSample();

                if (results.length != columns.size()) {
                    sample.setSuccessful(false);
                    parsingFailed = true;
                } else {
                    if (columns.contains(Columns.FIFTY_PERCENT.toString())) {
                        sample.setMedian(Long.valueOf(results[columns.indexOf(Columns.FIFTY_PERCENT.toString())]));
                    }
                    if (columns.contains(Columns.MIN.toString())) {
                        sample.setMin(Long.valueOf(results[columns.indexOf(Columns.MIN.toString())]));
                    }
                    if (columns.contains(Columns.MAX.toString())) {
                        sample.setMax(Long.valueOf(results[columns.indexOf(Columns.MAX.toString())]));
                    }
                    if (columns.contains(Columns.TEN_PERCENT.toString())) {
                        sample.set10Percentile(Long.valueOf(results[columns.indexOf(Columns.TEN_PERCENT.toString())]));
                    }
                    if (columns.contains(Columns.NINETY_PERCENT.toString())) {
                        sample.set90Percentile(
                                Long.valueOf(results[columns.indexOf(Columns.NINETY_PERCENT.toString())]));
                    }

                    // set test suite and test case names
                    if (columns.contains(Columns.TEST_SUITE.toString())) {
                        sample.setTestSuite(results[columns.indexOf(Columns.TEST_SUITE.toString())]);
                    }
                    if (columns.contains(Columns.TEST_CASE.toString())) {
                        sample.setTestCase(results[columns.indexOf(Columns.TEST_CASE.toString())]);
                    }
                    if (columns.contains(Columns.TEST_CLASS.toString())) {
                        sample.setTestClass(results[columns.indexOf(Columns.TEST_CLASS.toString())]);
                    }

                    sample.setTestName(r.getReportFileName() + " | " + sample.getTestSuite() + " | "
                            + sample.getTestClass() + " | " + sample.getTestCase());
                    sample.setSuccessful(true);
                }
                if (!parsingFailed) {
                    r.setReportSample(sample);
                    r.addSample(sample);
                    // set the report name
                    r.setReportName(sample.getTestName());
                    result.add(r);
                    logger.println("Performance: Parsing Sling Performance report " + r.getReportName() +
                            " succeeded. (File: " + r.getReportFileName() + ")");
                } else {
                    r.addSample(sample);
                    logger.println("Performance: Parsing Sling Performance report " + r.getReportName() +
                            " failed. (File: " + r.getReportFileName() + ")");
                }
            }
        }
        return result;
    }
}
