/*
 * =====================================================================
 *  Charts servlet provides JSON data object for chart rendering
 * =====================================================================
 */
package com.voc.jira.plugins.jira.servlet;
//TODO: Create a data builder object for all charts and include here

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webresource.JiraWebResourceManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.util.Cache;
import com.voc.jira.plugins.jira.util.Issues;
import com.voc.jira.plugins.jira.util.Jql;
import com.voc.jira.plugins.jira.util.Json;
import com.voc.jira.plugins.jira.util.Param;
import com.voc.jira.plugins.jira.util.Projects;
import com.voc.jira.plugins.jira.util.RequestError;
import com.voc.jira.plugins.jira.util.ServletResponse;
import com.voc.jira.plugins.jira.util.Time;
import com.voc.jira.plugins.jira.util.TrendChart;

public class Charts extends HttpServlet implements IErrorKeeper {
	private static final String AVERAGE_FOUND = "averageFound";
	private static final String BC_TITLE = "bcTitle";
	private static final String CF_TITLE = "cfTitle";
	private static final String BOTH = "both";
	private static final String WHICH_CHARTS = "whichCharts";
	// NOTE: instance variables are shared by all threads -- there is only one instance of Charts in the server as is the case with any such servlet in JIRA.
	private static final String BUSINESS_CRITICAL = "charts-servlet.jql.business.critical";
	private static final String CUSTOMER_FACING_ALL = "charts-servlet.jql.customer.facing.all"; 
	private static final String CUSTOMER_FACING = "charts-servlet.jql.customer.facing"; 
	private static final String CUSTOMER_FOUND = "charts-servlet.jql.customer.found"; 
	private static final String BOTH_FOUND = "charts-servlet.jql.both.found"; 
	private static final String ESCAPING = "charts-servlet.jql.escaping"; 
	private static final String ALL_ESCAPING = "charts-servlet.jql.all.escaping"; 
	private static final String ALL_FOUND = "charts-servlet.jql.all.found"; 
	private static final long serialVersionUID = -1468563646927779149L;
	private static final Logger log = LoggerFactory.getLogger(Charts.class);
    private final UserManager userManager;
    private final TemplateRenderer renderer;
    private final ApplicationUser user;
    private final com.atlassian.jira.user.util.UserManager jiraUserManager;
    private final ProjectManager projectManager;
    private final I18nResolver i18n;
    private final JiraWebResourceManager webResourceManager;
    private final SearchService searchService;
    private final ConfigurationManager configMgr;
	private final String colorCFEscaping = "#4C6C9C";
	private final String colorCFCustFound = "#9DB1CF";
	private final String colorCFCustFacing = "#DFE5EF";
	private final String colorBCEscaping = "#990099";
	private final String colorBCCustFound = "#FF66FF";
	private final String colorBCCustFacing = "#FFCCFF";
	private final String baseUrl;

    private HashSet<Long> getIssues(String keyBase, String key, Map<String, Object> context) {
		Issues request = new Issues(getJqlQuery(key),context, searchService, user, this, this.baseUrl,keyBase, this.configMgr);
    	return getIssuesHelper(request);
	}
    
    @SuppressWarnings("unchecked")
	private HashSet<Long> getIssuesHelper(Issues request) {
		return (HashSet<Long>)Cache.get(request);
	}

	private String getJqlQuery(String key) {
		String query = i18n.getText(key);
		query = processQuotes(query);
		return query;
	}
	
    public Charts( 
            JiraAuthenticationContext jiraAuthenticationContext,
            com.atlassian.jira.user.util.UserManager jiraUserManager,
            UserManager userManager,
            TemplateRenderer templateRenderer,
            ApplicationProperties applicationProperties,
            I18nResolver i18n,
            JiraWebResourceManager webResourceManager,
            WebResourceUrlProvider webResourceUrlProvider,
    		SearchService searchService,
    		ConfigurationManager configMgr) {
        this.renderer = templateRenderer;
        this.jiraUserManager = jiraUserManager;
        this.i18n = i18n;
        this.webResourceManager = webResourceManager;
        this.projectManager = ComponentAccessor.getProjectManager();
        this.user = jiraAuthenticationContext.getLoggedInUser();
        this.searchService = searchService;
	    this.baseUrl = applicationProperties.getBaseUrl();
	    this.userManager = userManager;
	    this.configMgr = configMgr;
    }

    private List<Map<String, Object>> getDataSetWithSeparateQueries(boolean businessCritical,StringBuilder timings,String projects,Date begin,Date end,boolean useHardCodedData, final String customClause, Map<String, Object> context,boolean includeChart) {
    	int totalFound = 0;
    	List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    	if (useHardCodedData || !includeChart || RequestError.happened(context)) {
        	context.put(AVERAGE_FOUND + businessCritical, 0);
    		return data;
    	}
    	HashSet<Long> allFound = getIssues("c&t:fnd",ALL_FOUND,context);
    	HashSet<Long> allEscaping = getIssues("c&t:esc",ALL_ESCAPING,context);
    	LinkedHashMap<String,Date> months = Time.getMonthEnds(begin,end);
    	for (Map.Entry<String,Date> month : months.entrySet()) {
        	final String monthStr = month.getKey();
        	final String facingQ = getQueryAsString(CUSTOMER_FACING_ALL, monthStr, businessCritical,projects,customClause);
        	timestamp(timings);
        	context.put(BUSINESS_CRITICAL, businessCritical);
        	String keyBase = String.format("c&t:cFac:%s:%s:%s:%s",monthStr,businessCritical,projects,customClause);
        	HashSet<Long> all = getIssues(keyBase,facingQ,context);
        	if (RequestError.happened(context)) {
        		break;
        	}
        	timestamp("get month " + monthStr + " bc:" + businessCritical,timings);
        	HashSet<Long> facingNotFound = new HashSet<Long>(all);
        	facingNotFound.removeAll(allFound);
        	int nFac = facingNotFound.size(); 
        	HashSet<Long> foundNotEscaping = new HashSet<Long>(all);
        	foundNotEscaping.retainAll(allFound);
        	foundNotEscaping.removeAll(allEscaping);
        	int nFnd = foundNotEscaping.size();
        	HashSet<Long> escaping = new HashSet<Long>(all);
        	escaping.retainAll(allEscaping);
        	int nEsc = escaping.size();
        	timestamp("segment month into three parts",timings);
        	final String escQuery = getQueryAsString(ESCAPING, monthStr, businessCritical,projects, customClause);
        	final String fndQuery = getQueryAsString(CUSTOMER_FOUND, monthStr, businessCritical,projects, customClause);
    		final String facQuery = getQueryAsString(CUSTOMER_FACING, monthStr, businessCritical,projects, customClause);
        	Map<String,Object> m = new HashMap<String, Object>();
        	m.put("month",monthStr);
        	m.put("linkBoth", Jql.getQueryUrl(getQueryAsString(BOTH_FOUND, monthStr, businessCritical,projects, customClause),baseUrl));
        	addDefects(m, nEsc, escQuery, nFnd, fndQuery, nFac, facQuery, businessCritical, monthStr,projects, customClause);
        	data.add(m);
        	totalFound += nFnd + nEsc;
        	log("adding " + (nFnd + nEsc),context);
        	log("total now " +totalFound,context);
    	}
    	log("data.size() " + data.size(),context);
        
    	double averageFound = data.size() > 0 ? (double)totalFound / (double)data.size() : 0;
    	context.put(AVERAGE_FOUND + businessCritical, averageFound);
    	log("averageFound " + averageFound,context);
        
    	return data;
	}
    
	public static final String LOG = "log";
    private void log(String m, Map<String, Object> context) {
    	if (context.containsKey(LOG)) {
    		context.put(LOG, context.get(LOG) + "\n" + m);
    	} else {
    		context.put(LOG, m);
    	}
    }
	private void addDefects(Map<String, Object> m, int esc, String escQ, int fnd, String fndQ, int fac, String cfQ, boolean businessCritical,String month,String projects, final String customClause) {
		int middle = esc+fnd;
		int top = middle+fac;
		ArrayList<Map<String,Object>> defects = new ArrayList<Map<String,Object>>();
		String escapingUrl = Jql.getQueryUrl(escQ,baseUrl);
		String customerFoundUrl = Jql.getQueryUrl(fndQ,baseUrl);
		String customerFacingUrl = Jql.getQueryUrl(cfQ,baseUrl);
		addDefect("Escaping",businessCritical?colorBCEscaping:colorCFEscaping,0,esc,escapingUrl,defects);
		addDefect("Cust Found",businessCritical?colorBCCustFound:colorCFCustFound,esc,middle,customerFoundUrl,defects);
		addDefect("Cust Facing",businessCritical?colorBCCustFacing:colorCFCustFacing,middle,top,customerFacingUrl,defects);
		addDefect("total","white",top,top+25,Jql.getQueryUrl(getQueryAsString(CUSTOMER_FACING_ALL, month, businessCritical, projects, customClause),baseUrl),defects);
		m.put("defects", defects);
		m.put("total", top);
	}
	private void addDefect(String name, String color, int y0, int y1, String url, ArrayList<Map<String, Object>> defects) {
		Map<String,Object> defect = new HashMap<String, Object>();
		defect.put("name", name);
		defect.put("y0", y0);
		defect.put("y1", y1);
		defect.put("color", color);
		defect.put("url", url);
		defects.add(defect);
	}
	
	private static String processQuotes(String q) {
		return q.replace("\"\"\"","\\\"");
	}
	
	private String getQueryAsString(final String key, final String endDate, final boolean businessCritical, final String projectClause, final String customClause) {
		final String bc = getBusinessCriticalClause(businessCritical);
		final String projectsClause = ESCAPING.equals(key) 
				? projectClause.replace("\"", "\\\"") : projectClause; 
		String customJqlClause = ESCAPING.equals(key) 
				? customClause.replace("\"", "\"\"\"") : customClause;
		customJqlClause = " " + customJqlClause;		
		final String tmp = String.format(i18n.getText(key),endDate,endDate,projectsClause,customJqlClause,bc);
		final String result = processQuotes(tmp);
		return result;
    }

	private String getBusinessCriticalClause(final boolean businessCritical) {
		return businessCritical ? " " + i18n.getText(BUSINESS_CRITICAL) : "";
    }

	public void insertErrorMessage(Map<String, Object> context, String message) {
		final String which = (String)context.get(WHICH_CHARTS); 
		if (context.containsKey(BUSINESS_CRITICAL)) {
			if ((Boolean)context.get(BUSINESS_CRITICAL)) {
				if (doBusinessCritical(which)) {
					insertErrorMessage(context, true, message);
				}
			} else {
				if (doCustomerFacing(which)) {
					insertErrorMessage(context, false, message);
				}
			}
		} else {
		if (doBusinessCritical(which)) {
			insertErrorMessage(context, true, message);
		}
		if (doCustomerFacing(which)) {
			insertErrorMessage(context, false, message);
		}
		}
	}
	
	private void insertErrorMessage(Map<String, Object> context, boolean businessCritical, String message) {
		String key = businessCritical?BC_TITLE:CF_TITLE;
		RequestError.insertErrorMessage(context, key, message);
	}
	
	private Instant last;
    
    private void timestamp(String msg,StringBuilder timings) {
		Instant now = Instant.now();
		Duration d = new Duration(last,now);
    	timings.append("\n" + d.getMillis() + " msecs later, " + msg);
    	last = now;
    }
	
    private void timestamp(StringBuilder timings) {
		timestamp("",timings);
    }

    /**
     * GET method for servlet handles
     * * loading resources
     * * JSON data as a variable
     * * render page with velocity template
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
    						throws ServletException, IOException {
       try {
    	    Map<String, Object> context = ServletResponse.setup(resp, webResourceManager);
	    	StringBuilder timings = new StringBuilder();
        	List<String> projectInfo = Projects.getProjects(req,projectManager);
	    	final String projectClause = projectInfo.get(0);
	    	final String projectTitle = projectInfo.get(1);
        	String which = Param.pass(WHICH_CHARTS,BOTH,req,context,this);
	        context.put(CF_TITLE, getTitle("Customer Facing Defects",projectTitle,doCustomerFacing(which)));
	        context.put(BC_TITLE, getTitle("Business Critical Defects",projectTitle,doBusinessCritical(which)));
	        RequestError.checkForErrorMessage(req, context,this);
	        Time.getScope(req,context,this,Time.START_DATE,Time.END_DATE);
        	Date beginDate = (Date) context.get(Time.START_DATE);
        	Date endDate = (Date) context.get(Time.END_DATE);
	    	//TODO: add process searcher and chart JSON data based on parameters
	    	//      NOTE: follow example with VOCVolume and VOCVolumeBuilder classes
	    	context.put("p",projectClause);
        	String w = Param.pass("w","500",req,context,new IntegerValidator(340, 5000, "Chart Width"),this);
	    	context.put("wbc", doBusinessCritical(which) ? w : "0");
	    	context.put("wcf", doCustomerFacing(which) ? w : "0");
        	Param.pass("h","400",req,context,new IntegerValidator(250, 5000, "Chart Height"),this);
        	Param.pass("trendHeight","80",req,context,new IntegerValidator(TrendChart.MIN_TREND_HEIGHT, 1000, "Trend Height"),this);
        	Param.getHC(context, req, this);
        	Param.getLatest(context, req, this);
	        boolean useHardCodedData = Param.useHardCodedData(context);
			context.put(BUSINESS_CRITICAL, true);
	    	final String customJql = Param.getCustomJql(req,context,this,searchService,user);
           	final String cfData = Json.get(getDataSetWithSeparateQueries(false,timings,projectClause,beginDate,endDate,useHardCodedData,customJql,context,doCustomerFacing(which)));
    	    //System.out.println("IN CHARTS DOGET after data string cfData");
           	final String bcData = Json.get(getDataSetWithSeparateQueries(true,timings,projectClause,beginDate,endDate,useHardCodedData,customJql,context,doBusinessCritical(which)));
           	context.put("cfData", cfData);
	    	context.put("bcData", bcData);
	        String[] cfColors = {colorCFEscaping,colorCFCustFound,colorCFCustFacing};
	        context.put("cfColors", Json.get(cfColors));
	        String[] bcColors = {colorBCEscaping,colorBCCustFound,colorBCCustFacing};
	        context.put("bcColors", Json.get(bcColors));
        	context.put("time", timings.toString());
	        Param.putRefreshUrl(req, context, this, baseUrl + "/plugins/servlet/charts");
        	//TODO: add parser for parameters for type of chart and pick appropriate template
	        renderer.render(i18n.getText("charts-servlet.defectgraphs.vm.bar"), context, resp.getWriter());
        } catch (NullPointerException e) {
        	log.error("NullPointerException: ",e);
    		e.printStackTrace();
    	} catch (UnsupportedEncodingException e) {
    		log.error("UnsupportedEncodingException: ",e);
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			log.error("UnsupportedOperationException: ",e);
			e.printStackTrace();
		} catch (SSLPeerUnverifiedException e) {
			log.error("SSLPeerUnverifiedException: ", e);
		}
    }

	/**Whether to do business critical chart
	 * @param which - param from gadget "both" or "bc" or "cf"
	 * @return
	 */
	private boolean doBusinessCritical(String which) {
		return BOTH.equals(which) || "bc".equals(which);
	}
	
	/**Whether to do business critical chart
	 * @param which - param from gadget "both" or "bc" or "cf"
	 * @return
	 */
	private boolean doCustomerFacing(String which) {
		return BOTH.equals(which) || "cf".equals(which);
	}

	/**Get the title of the chart.
	 * @param base The main part of the title. This fn appends any specified projects to it.
	 * @param which 
	 * @return
	 */
	private Object getTitle(String base,String projectsTitle, boolean includeChart) {
		return includeChart ? base + projectsTitle : "";
	}
	
	/**
     * Helper method for getting the current user
     * @param req
     * @return
     */
    @SuppressWarnings("unused")
	private ApplicationUser getCurrentUser(HttpServletRequest req) {
        // To get the current user, we first get the username from the session.
        // Then we pass that over to the jiraUserManager in order to get an actual User object.
        return (ApplicationUser) jiraUserManager.getUserByName(
        		userManager.getRemoteUsername(req)).getDirectoryUser();
    }
}