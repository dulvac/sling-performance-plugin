package hudson.plugins.slingperformanceplugin;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.text.TextUtilities;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.awt.*;
import java.awt.geom.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.plugins.slingperformanceplugin.reports.PerformanceReport;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;


public final class SlingPerformanceProjectAction implements Action {

    private static final String CONFIGURE_LINK = "configure";
    private static final String TRENDREPORT_LINK = "trendReport";
    private static final String TESTSUITE_LINK = "testsuiteReport";

    private static final String PLUGIN_NAME = "performance";


    private static final long serialVersionUID = 1L;

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SlingPerformanceProjectAction.class.getName());

    public final AbstractProject<?, ?> project;

    private transient List<String> performanceReportFileList;

    public String urlEncodeParameter(String parameter) throws UnsupportedEncodingException {
        return URLEncoder.encode(parameter, "UTF-8");
    }

    public String urlDecodeParameter(String parameter) throws UnsupportedEncodingException {
        return URLDecoder.decode(parameter, "UTF-8");
    }

    public String getDisplayName() {
        return Messages.ProjectAction_DisplayName();
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getUrlName() {
        return PLUGIN_NAME;
    }

    public SlingPerformanceProjectAction(AbstractProject project) {
        this.project = project;
    }

    protected static JFreeChart createRespondingTimeChart(List<CategoryDataset> datasets) {
        final CombinedDomainCategoryPlot finalPlot = new CombinedDomainCategoryPlot();
        finalPlot.setGap(10.0);

        for (CategoryDataset dataset: datasets) {
            // Now create each plot
            final CategoryItemRenderer renderer = new LineAndShapeRenderer();
            final NumberAxis rangeAxis = new NumberAxis("Response Time");
            final CategoryPlot subplot = new CategoryPlot(dataset, null, rangeAxis, renderer);
            subplot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
            CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
            subplot.setDomainAxis(domainAxis);
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            domainAxis.setCategoryMargin(0.0);

            // Add query text
            final CategoryAnnotation annotation = new CategoryAnnotation() {
                public void draw(Graphics2D g2, CategoryPlot plot, Rectangle2D dataArea, CategoryAxis domainAxis,
                        ValueAxis rangeAxis) {
                    TextUtilities.drawRotatedString("some long query string here", g2, 0, 0f, 0f);
                }
            };
            subplot.addAnnotation(annotation);

            // add to final plot
            finalPlot.add(subplot);
        }
        finalPlot.setOrientation(PlotOrientation.VERTICAL);
        // return a new chart containing the overlaid plot...
        return new JFreeChart("Response Times", JFreeChart.DEFAULT_TITLE_FONT, finalPlot, true);


//            final JFreeChart chart = ChartFactory.createLineChart(
//                    Messages.ProjectAction_RespondingTime(), // charttitle
//                    null, // unused
//                    "ms", // range axis label
//                    dataset, // data
//                    PlotOrientation.VERTICAL, // orientation
//                    true, // include legend
//                    true, // tooltips
//                    false // urls
//            );
//
//
//        final LegendTitle legend = chart.getLegend();
//        legend.setPosition(RectangleEdge.BOTTOM);
//
//        chart.setBackgroundPaint(Color.white);
//
//        final CategoryPlot plot = chart.getCategoryPlot();
//
//        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
//        plot.setBackgroundPaint(Color.WHITE);
//        plot.setOutlinePaint(null);
//        plot.setRangeGridlinesVisible(true);
//        plot.setRangeGridlinePaint(Color.black);
//
//        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
//        plot.setDomainAxis(domainAxis);
//        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
//        domainAxis.setLowerMargin(0.0);
//        domainAxis.setUpperMargin(0.0);
//        domainAxis.setCategoryMargin(0.0);
//
//        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
//        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
//
//        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
//        renderer.setBaseStroke(new BasicStroke(4.0f));
//        ColorPalette.apply(renderer);
//
//        // crop extra space around the graph
//        plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));
//
//        return chart;
    }


    public void doRespondingTimeGraphMin(StaplerRequest request,
            StaplerResponse response) throws IOException {
        doRespondingTimeGraphCustom(request, response, Arrays.asList(DataFilter.MIN));
    }


    public void doRespondingTimeGraphMedian(StaplerRequest request,
            StaplerResponse response) throws IOException {
        doRespondingTimeGraphCustom(request, response, Arrays.asList(DataFilter.MEDIAN));
    }


    public void doRespondingTimeGraphMax(StaplerRequest request,
            StaplerResponse response) throws IOException {
        doRespondingTimeGraphCustom(request, response, Arrays.asList(DataFilter.MAX));
    }

    public void doRespondingTimeGraphPercentile10(StaplerRequest request,
            StaplerResponse response) throws IOException {
        doRespondingTimeGraphCustom(request, response, Arrays.asList(DataFilter.TEN_PERCENT));
    }

    public void doRespondingTimeGraphPercentile90(StaplerRequest request,
            StaplerResponse response) throws IOException {
        doRespondingTimeGraphCustom(request, response, Arrays.asList(DataFilter.NINENTY_PERCENT));
    }

    public void doRespondingTimeGraph(StaplerRequest request,
            StaplerResponse response) throws IOException {
        doRespondingTimeGraphCustom(request, response, Arrays.asList(DataFilter.values()));
    }


    public void doRespondingTimeGraphMatrix(StaplerRequest request,
            StaplerResponse response) throws IOException {

        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }
        List<DataFilter> dataFilter = Arrays.asList(DataFilter.NINENTY_PERCENT);
        Map<String, DataSetBuilder<String, CustomNumberOnlyBuildLabel>> datasetBuilders =
                new HashMap<String, DataSetBuilder<String, CustomNumberOnlyBuildLabel>>();

        // Add data to dataset builders for each configuration
        Collection<MatrixConfiguration> configs = ((MatrixProject) this.project).getActiveConfigurations();
        for (MatrixConfiguration config : configs) {
            // the builds to use
            List<? extends AbstractBuild<?, ?>> builds = config.getBuilds();
            // add items to the dataset builders; use project names as suffix for legend labels
            addToDatasetBuilders(request, response, dataFilter, builds, datasetBuilders, true);
        }
        List<CategoryDataset> datasetList = new ArrayList<CategoryDataset>();
        for (DataSetBuilder dataSetBuilder : datasetBuilders.values()) {
            datasetList.add(dataSetBuilder.build());
        }
        ChartUtil.generateGraph(request, response,
                createRespondingTimeChart(datasetList), 900, datasetList.size() * 400);
    }

    public void doRespondingTimeGraphCustom(StaplerRequest request,
            StaplerResponse response, List<DataFilter> dataFilter) throws IOException {

        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }

        Map<String, DataSetBuilder<String, CustomNumberOnlyBuildLabel>> datasetBuilders =
                new HashMap<String, DataSetBuilder<String, CustomNumberOnlyBuildLabel>>();

        // the builds to use; in this case, all project builds
        List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();

        // add items to the dataset builders; don't use project names for legend labels
        addToDatasetBuilders(request, response, dataFilter, builds, datasetBuilders, false);

        List<CategoryDataset> datasetList = new ArrayList<CategoryDataset>();
        for (DataSetBuilder dataSetBuilder : datasetBuilders.values()) {
            datasetList.add(dataSetBuilder.build());
        }
        ChartUtil.generateGraph(request, response,
                createRespondingTimeChart(datasetList), 900, datasetList.size() * 400);
    }

    /**
     * Add items to data set builders based on builds results
     */
    private void addToDatasetBuilders(StaplerRequest request,
            StaplerResponse response, List<DataFilter> dataFilter, List<? extends AbstractBuild<?, ?>> builds,
            Map<String, DataSetBuilder<String, CustomNumberOnlyBuildLabel>> datasetBuilders, boolean useCustomLabelSuffix) {
        Range buildsLimits = getFirstAndLastBuild(request, builds);
        int nbBuildsToAnalyze = builds.size();

        PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        String performanceReportNameFile = performanceReportPosition.getPerformanceReportPosition();
        String reportName = performanceReportPosition.getReportName();
        if (performanceReportNameFile == null) {
            if (getPerformanceReportFileList().size() == 1) {
                performanceReportNameFile = getPerformanceReportFileList().get(0);
            } else {
                return;
            }
        }

        // create dataSet for each build
        for (AbstractBuild<?, ?> build : builds) {
            if (buildsLimits.in(nbBuildsToAnalyze)) {

                if (!buildsLimits.includedByStep(build.number)) {
                    continue;
                }

                CustomNumberOnlyBuildLabel label = new CustomNumberOnlyBuildLabel(build);
                PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }

                PerformanceReport performanceReport = performanceBuildAction.getPerformanceReportMap()
                        .getPerformanceReport(reportName);
                if (null == performanceReport) {
                    nbBuildsToAnalyze--;
                    continue;
                }

                // For each report in this report file, add to the respective dataset builder
                DataSetBuilder<String, CustomNumberOnlyBuildLabel> dataSetBuilder =
                        datasetBuilders.get(performanceReport.getReportName());
                if (dataSetBuilder == null) {
                    dataSetBuilder = new DataSetBuilder<String, CustomNumberOnlyBuildLabel>();
                    datasetBuilders.put(performanceReport.getReportName(), dataSetBuilder);
                }

                // use an optional custom label suffix with project name
                String customLabelSuffix = (useCustomLabelSuffix) ? " [" + build.getProject().getName() + "]" : "";

                if (dataFilter.contains(DataFilter.MEDIAN)) {
                    if (performanceReport.isMultipleSampleReport()) {
                        for (SlingReportSample sample : performanceReport.getReportSamples()) {
                            dataSetBuilder
                                    .add(sample.getMedian(), Messages.ProjectAction_Median() + customLabelSuffix,
                                            label);
                        }
                    } else {
                        dataSetBuilder
                                .add(performanceReport.getMedian(), Messages.ProjectAction_Median() + customLabelSuffix,
                                        label);
                    }
                }
                if (dataFilter.contains(DataFilter.MAX)) {
                    if (performanceReport.isMultipleSampleReport()) {
                        for (SlingReportSample sample : performanceReport.getReportSamples()) {
                            dataSetBuilder
                                    .add(sample.getMax(), Messages.ProjectAction_Maximum() + customLabelSuffix, label);
                        }
                    } else {
                        dataSetBuilder
                                .add(performanceReport.getMax(), Messages.ProjectAction_Maximum() + customLabelSuffix,
                                        label);
                    }
                }
                if (dataFilter.contains(DataFilter.MIN)) {
                    if (performanceReport.isMultipleSampleReport()) {
                        for (SlingReportSample sample : performanceReport.getReportSamples()) {
                            dataSetBuilder
                                    .add(sample.getMin(), Messages.ProjectAction_Minimum() + customLabelSuffix, label);
                        }
                    } else {
                        dataSetBuilder
                                .add(performanceReport.getMin(), Messages.ProjectAction_Minimum() + customLabelSuffix,
                                        label);
                    }
                }
                if (dataFilter.contains(DataFilter.TEN_PERCENT)) {
                    if (performanceReport.isMultipleSampleReport()) {
                        for (SlingReportSample sample : performanceReport.getReportSamples()) {
                            dataSetBuilder
                                    .add(sample.get10Percentile(), Messages.ProjectAction_Line10() + customLabelSuffix,
                                            label);
                        }
                    } else {
                        dataSetBuilder
                                .add(performanceReport.get10Percentile(),
                                        Messages.ProjectAction_Line10() + customLabelSuffix, label);
                    }
                }
                if (dataFilter.contains(DataFilter.NINENTY_PERCENT)) {
                    if (performanceReport.isMultipleSampleReport()) {
                        for (SlingReportSample sample : performanceReport.getReportSamples()) {
                            dataSetBuilder
                                    .add(sample.get90Percentile(), Messages.ProjectAction_Line90() + customLabelSuffix,
                                            label);
                        }
                    } else {
                        dataSetBuilder
                                .add(performanceReport.get90Percentile(),
                                        Messages.ProjectAction_Line90() + customLabelSuffix, label);
                    }
                }

            }
            nbBuildsToAnalyze--;
            continue;
        }
    }


    /**
     * <p> give a list of two Integer : the smallest build to use and the biggest. </p>
     *
     * @return outList
     */
    private Range getFirstAndLastBuild(StaplerRequest request, List<?> builds) {
        Range range = new Range();
        GraphConfigurationDetail graphConf = (GraphConfigurationDetail) createUserConfiguration(request);

        if (graphConf.isNone()) {
            return all(builds);
        }

        if (graphConf.isBuildCount()) {
            if (graphConf.getBuildCount() <= 0) {
                return all(builds);
            } else {
                int first = builds.size() - graphConf.getBuildCount();
                return new Range(first > 0 ? first + 1 : 1,
                        builds.size());
            }
        } else if (graphConf.isBuildNth()) {
            if (graphConf.getBuildStep() <= 0) {
                return all(builds);
            } else {
                return new Range(1, builds.size(), graphConf.getBuildStep());
            }
        } else if (graphConf.isDate()) {
            if (graphConf.isDefaultDates()) {
                return all(builds);
            } else {
                int firstBuild = -1;
                int lastBuild = -1;
                int var = builds.size();
                GregorianCalendar firstDate = null;
                GregorianCalendar lastDate = null;
                try {
                    firstDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getFirstDayCount());
                    lastDate = GraphConfigurationDetail.getGregorianCalendarFromString(graphConf.getLastDayCount());
                    lastDate.set(GregorianCalendar.HOUR_OF_DAY, 23);
                    lastDate.set(GregorianCalendar.MINUTE, 59);
                    lastDate.set(GregorianCalendar.SECOND, 59);
                } catch (ParseException e) {
                    LOGGER.log(Level.SEVERE, "Error during the manage of the Calendar", e);
                }
                for (Iterator<?> iterator = builds.iterator(); iterator.hasNext(); ) {
                    AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) iterator.next();
                    GregorianCalendar buildDate = new GregorianCalendar();
                    buildDate.setTime(currentBuild.getTimestamp().getTime());
                    if (firstDate.getTime().before(buildDate.getTime())) {
                        firstBuild = var;
                    }
                    if (lastBuild < 0 && lastDate.getTime().after(buildDate.getTime())) {
                        lastBuild = var;
                    }
                    var--;
                }
                return new Range(firstBuild, lastBuild);
            }
        }
        throw new IllegalArgumentException("unsupported configType + " + graphConf.getConfigType());
    }

    public Range all(List<?> builds) {
        return new Range(1, builds.size());
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }

    public String getLastBuild() {
        return this.project.getLastBuild().getDisplayName();
    }

    public List<String> getPerformanceReportFileList() {
        this.performanceReportFileList = new ArrayList<String>();
        if (null == this.project) {
            return performanceReportFileList;
        }
        if (null == this.project.getSomeBuildWithWorkspace()) {
            return performanceReportFileList;
        }

        if (this.project instanceof MatrixProject) {

            Collection<MatrixConfiguration> configs = ((MatrixProject) this.project).getActiveConfigurations();
            for (MatrixConfiguration config : configs) {

                MatrixRun matrixRun = config.getLastSuccessfulBuild();
                if (matrixRun == null) {
                    return performanceReportFileList;
                }

                File rootDir = matrixRun.getRootDir();

                if (rootDir == null) {
                    return performanceReportFileList;
                }

                File file = new File(rootDir, SlingPerformanceReportMap.getPerformanceReportDirRelativePath());

                if (!file.isDirectory()) {
                    return performanceReportFileList;
                }

                for (File entry : file.listFiles()) {
                    if (entry.isDirectory()) {
                        for (File e : entry.listFiles()) {
                            if (!this.performanceReportFileList.contains(e.getName())) {
                                this.performanceReportFileList.add(e.getName());
                            }
                        }
                    } else {
                        if (!this.performanceReportFileList.contains(entry.getName())) {
                            this.performanceReportFileList.add(entry.getName());
                        }
                    }
                }

            }
        } else {

            File file = new File(this.project.getLastSuccessfulBuild().getRootDir(),
                    SlingPerformanceReportMap.getPerformanceReportDirRelativePath());

            if (!file.isDirectory()) {
                return performanceReportFileList;
            }

            for (File entry : file.listFiles()) {
                if (entry.isDirectory()) {
                    for (File e : entry.listFiles()) {
                        this.performanceReportFileList.add(e.getName());
                    }
                } else {
                    this.performanceReportFileList.add(entry.getName());
                }

            }

        }

        Collections.sort(performanceReportFileList);
        return this.performanceReportFileList;
    }

    public void setPerformanceReportFileList(List<String> performanceReportFileList) {
        this.performanceReportFileList = performanceReportFileList;
    }

    public List<String> getPerformanceReportList(String performanceReportFile) {
        Set<String> performanceReportNames = new LinkedHashSet<String>();

        // if this is a matrix project, get all the report names from all configurations
        if (this.project instanceof MatrixProject) {
            Collection<MatrixConfiguration> configs = ((MatrixProject) this.project).getActiveConfigurations();
            for (MatrixConfiguration config : configs) {
                MatrixRun matrixRun = config.getLastSuccessfulBuild();
                if (matrixRun == null) {
                    continue;
                }
                PerformanceBuildAction performanceBuildAction = matrixRun.getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                List<PerformanceReport> performanceReports = performanceBuildAction.getPerformanceReportMap().
                        getPerformanceReportsByFilename(performanceReportFile);

                for (PerformanceReport report : performanceReports) {
                    performanceReportNames.add(report.getReportName());
                }
            }
        } else {
            AbstractBuild build = this.project.getLastSuccessfulBuild();
            PerformanceBuildAction performanceBuildAction = build.getAction(PerformanceBuildAction.class);
            if (performanceBuildAction == null) {
                return new ArrayList(performanceReportNames);
            }
            List<PerformanceReport> performanceReports = performanceBuildAction.getPerformanceReportMap().
                    getPerformanceReportsByFilename(performanceReportFile);

            for (PerformanceReport report : performanceReports) {
                performanceReportNames.add(report.getReportName());
            }
        }

        return new ArrayList(performanceReportNames);
    }

    public boolean isTrendVisibleOnProjectDashboard() {
        if (getPerformanceReportFileList() != null
                && getPerformanceReportFileList().size() == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the graph configuration for this project.
     *
     * @param link     not used
     * @param request  Stapler request
     * @param response Stapler response
     * @return the dynamic result of the analysis (detail page).
     */
    public Object getDynamic(final String link, final StaplerRequest request,
            final StaplerResponse response) {
        if (CONFIGURE_LINK.equals(link)) {
            return createUserConfiguration(request);
        } else if (TRENDREPORT_LINK.equals(link)) {
            return createTrendReport(request);
        } else {
            return null;
        }
    }

    /**
     * Creates a view to configure the trend graph for the current user.
     *
     * @param request Stapler request
     * @return a view to configure the trend graph for the current user
     */
    private Object createUserConfiguration(final StaplerRequest request) {
        GraphConfigurationDetail graph = new GraphConfigurationDetail(project,
                PLUGIN_NAME, request);
        return graph;
    }

    /**
     * Creates a view to configure the trend graph for the current user.
     *
     * @param request Stapler request
     * @return a view to configure the trend graph for the current user
     */
    private Object createTrendReport(final StaplerRequest request) {
        String reportName = getTrendReportName(request);
        String filename = getTestSuiteReportFilename(request);
        CategoryDataset dataSet = getTrendReportData(request, reportName).build();
        TrendReportDetail report = new TrendReportDetail(project, PLUGIN_NAME, request, filename, reportName, dataSet);
        return report;
    }

    private String getTrendReportName(final StaplerRequest request) {
        PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        return performanceReportPosition.getReportName();
    }

    private String getTestSuiteReportFilename(final StaplerRequest request) {
        PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);
        return performanceReportPosition.getPerformanceReportPosition();
    }

    private DataSetBuilder getTrendReportData(final StaplerRequest request,
            String performanceReportName) {

        DataSetBuilder<String, CustomNumberOnlyBuildLabel> dataSet = new DataSetBuilder<String, CustomNumberOnlyBuildLabel>();
        List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();
        Range buildsLimits = getFirstAndLastBuild(request, builds);

        int nbBuildsToAnalyze = builds.size();
        for (AbstractBuild<?, ?> currentBuild : builds) {
            if (buildsLimits.in(nbBuildsToAnalyze)) {
                CustomNumberOnlyBuildLabel label = new CustomNumberOnlyBuildLabel(currentBuild);
                PerformanceBuildAction performanceBuildAction = currentBuild.getAction(PerformanceBuildAction.class);
                if (performanceBuildAction == null) {
                    continue;
                }
                PerformanceReport report = null;
                report = performanceBuildAction.getPerformanceReportMap().getPerformanceReport(performanceReportName);
                if (report == null) {
                    nbBuildsToAnalyze--;
                    continue;
                }

                dataSet.add(Math.round(report.getMedian()), Messages.ProjectAction_Median(), label);
                dataSet.add(Math.round(report.getMin()), Messages.ProjectAction_Minimum(), label);
                dataSet.add(Math.round(report.getMax()), Messages.ProjectAction_Maximum(), label);
                dataSet.add(Math.round(report.get10Percentile()), Messages.ProjectAction_Line10(), label);
                dataSet.add(Math.round(report.get90Percentile()), Messages.ProjectAction_Line90(), label);
            }
            nbBuildsToAnalyze--;
        }
        return dataSet;
    }

    public boolean ifSlingPerformanceParserUsed(String fileName) {
        return true;
    }

    public boolean ifMatrixProject() {
        return (this.project instanceof MatrixProject ? true : false);
    }

    public static class Range {

        public int first;

        public int last;

        public int step;

        private Range() {
        }

        public Range(int first, int last) {
            this.first = first;
            this.last = last;
            this.step = 1;
        }

        public Range(int first, int last, int step) {
            this(first, last);
            this.step = step;
        }

        public boolean in(int nbBuildsToAnalyze) {
            return nbBuildsToAnalyze <= last
                    && first <= nbBuildsToAnalyze;
        }

        public boolean includedByStep(int buildNumber) {
            if (buildNumber % step == 0) {
                return true;
            }
            return false;
        }

    }
}
