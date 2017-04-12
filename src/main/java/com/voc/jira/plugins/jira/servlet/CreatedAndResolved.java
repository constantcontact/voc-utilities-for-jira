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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
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
import com.voc.jira.plugins.jira.util.AvgDays;
import com.voc.jira.plugins.jira.util.AvgDaysData;
import com.voc.jira.plugins.jira.util.Cache;
import com.voc.jira.plugins.jira.util.Jql;
import com.voc.jira.plugins.jira.util.Json;
import com.voc.jira.plugins.jira.util.Param;
import com.voc.jira.plugins.jira.util.Projects;
import com.voc.jira.plugins.jira.util.RequestError;
import com.voc.jira.plugins.jira.util.ServletResponse;
import com.voc.jira.plugins.jira.util.Time;

public class CreatedAndResolved extends HttpServlet implements IErrorKeeper {
	private static final String TREND_HEIGHT = "trendHeight";
	private static final String N = "n";
	private static final String AVERAGE = "average";
	private static final String JQL_CURRENT = "created-resolved-servlet.jql.current";
	private static final String JQL_CURRENT_ALIVE = "created-resolved-servlet.jql.current.alive";
	private static final String JQL_CURRENT_UNRESOLVED = "created-resolved-servlet.jql.current.unresolved";
	private static final String JQL_RECENT_UNRESOVLED = "created-resolved-servlet.jql.recent.unresolved";
	private static final String JQL_CURRENT_RESOLVED = "created-resolved-servlet.jql.current.resolved";
	private static final String JQL_RECENT_RESOVLED = "created-resolved-servlet.jql.recent.resolved";
	private static final String JQL_RECENT = "created-resolved-servlet.jql.recent";
	private static final String JQL_OLD_RESOLVED = "created-resolved-servlet.jql.old.resolved";
	private static final String JQL_OLD_UNRESOLVED = "created-resolved-servlet.jql.old.unresolved";	
	private static final String JQL_ALL_RESOLVED = "created-resolved-servlet.jql.all.resolved";	
	private static final String DATA = "data";
	private static final String PROJECTS = "projectsJqlClause";
	private static final String BASE_TITLE = "VOC Defects Created vs. Resolved Aging";
	private static final String BAR = "created-resolved-servlet.vm.bar";
	// NOTE: instance variables are shared by all threads -- there is only one
	// instance of Charts in the server as is the case with any such servlet in
	// JIRA.
	private static final long serialVersionUID = -101869979013349976L;
	private static final Logger log = LoggerFactory.getLogger(CreatedAndResolved.class);
	private final TemplateRenderer renderer;
	private final User user;
	private final ProjectManager projectManager;
	private final I18nResolver i18n;
	private final JiraWebResourceManager webResourceManager;
	private final SearchService searchService;
	private final String baseUrl;
	private static final String TITLE = "title";
	public CreatedAndResolved(JiraAuthenticationContext jiraAuthenticationContext,
			com.atlassian.jira.user.util.UserManager jiraUserManager,
			UserManager userManager, TemplateRenderer templateRenderer,
			ApplicationProperties applicationProperties, I18nResolver i18n,
			JiraWebResourceManager webResourceManager,
			WebResourceUrlProvider webResourceUrlProvider,
			SearchService searchService) {
		this.renderer = templateRenderer;
		this.i18n = i18n;
		this.webResourceManager = webResourceManager;
		ApplicationUser applicationUser = jiraAuthenticationContext.getUser();
		this.projectManager = ComponentAccessor.getProjectManager();
		this.user = applicationUser.getDirectoryUser();
		this.searchService = searchService;
		this.baseUrl = applicationProperties.getBaseUrl();
	}

	/**
	 * GET method for servlet handles * loading resources * JSON data as a
	 * variable * render page with velocity template
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
        	List<String> projectInfo = Projects.getProjects(req,projectManager);
        	final String projectClause = projectInfo.get(0);
        	final String projectTitle = projectInfo.get(1);
        	context.put(PROJECTS, projectClause);
        	context.put(TITLE, BASE_TITLE + projectTitle);
	        RequestError.checkForErrorMessage(req, context,this);
	        Param.getCustomJql(req,context,this,searchService,user);
	        Time.getScope(req,context,this,Time.START_DATE,Time.END_DATE);
	        Param.pass(N,"6",req,context,new IntegerValidator(2, 120, "Age threshold"),this);
        	Param.getHC(context, req, this);
        	Param.getLatest(context, req, this);
        	Param.pass(TREND_HEIGHT,"80",req,context,new IntegerValidator(80, 1000, "Trend Height"),this);
	        Param.pass("w","500",req,context,new IntegerValidator(250, 5000, "Chart Width"),this);
        	Param.pass("h","600",req,context,new IntegerValidator(425, 5000, "Chart Height"),this);
	        addData(context);
	        Param.putRefreshUrl(req, context, this,baseUrl + "/plugins/servlet/created-and-resolved");
			renderer.render(i18n.getText(BAR),context,resp.getWriter());
		} catch (NullPointerException e) {
			logExceptionAndEatIt(e, "NullPointerException");
		} catch (UnsupportedEncodingException e) {
			logExceptionAndEatIt(e, "UnsupportedEncodingException");
		} catch (UnsupportedOperationException e) {
			logExceptionAndEatIt(e, "UnsupportedOperationException");
		} catch (SSLPeerUnverifiedException e) {
			logExceptionAndEatIt(e, "SSLPeerUnverifiedException");
		}
	}
	private static void logExceptionAndEatIt(Exception e,String msg) {
		e.printStackTrace();
		log.error(msg + ": " + e);
	}

	private void addData(Map<String, Object> context) {
		context.put(DATA, Json.get(getData(context)));		
	}

    private List<Map<String, Object>> getData(Map<String, Object> context) {
    	int totalAllNetAdds = 0;
    	int monthCount = 0;
    	List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    	if (!Param.useHardCodedData(context) && !RequestError.happened(context)) {
        	int n = Integer.parseInt((String)context.get(N));
        	LinkedHashMap<String,Date> months = Time.getMonthEnds((Date)context.get(Time.START_DATE),(Date)context.get(Time.END_DATE));
        	for (Map.Entry<String,Date> month : months.entrySet()) {
            	final String monthEnd = month.getKey();
            	final String monthStart = Time.format(Time.getMonthBeginning(monthEnd));
            	final String nextMonthStart = Time.format(Time.getMonthBeginning(Time.getMonthsLater(monthEnd,1)));
            	final String recentStart = Time.format(Time.getMonthBeginning(Time.getMonthsEarlier(monthEnd,n)));
        		final String projectsClause = (String) context.get(PROJECTS);
        		final String customClause = (String) context.get(Param.CUSTOM_JQL);
            	// current created
        		final String qcc = String.format(i18n.getText(JQL_CURRENT),projectsClause,customClause,monthStart,nextMonthStart);
           		String keyBase = String.format("cra:CURRENT:%s:%s:%s:%s",projectsClause,customClause,monthStart,nextMonthStart);
            	int ncc = Jql.getSearchCount(qcc,context,searchService,user,this,this.baseUrl,keyBase);
            	if (RequestError.happened(context)) {
            		break;
            	}
            	final String ccUrl = Jql.getQueryUrl(qcc, baseUrl);
            	// current unresolved
            	final String qcu = String.format(i18n.getText(JQL_CURRENT_UNRESOLVED),projectsClause,customClause,monthStart,nextMonthStart,nextMonthStart); 
           		keyBase = String.format("cra:CURRENT_UNRESOLVED:%s:%s:%s:%s",projectsClause,customClause,monthStart,nextMonthStart);
            	int ncu = Jql.getSearchCount(qcu,context,searchService,user,this,this.baseUrl,keyBase);
            	if (RequestError.happened(context)) {
            		break;
            	}
            	final String cuUrl = Jql.getQueryUrl(qcu, baseUrl);
            	// recent unresolved
            	final String qru = String.format(i18n.getText(JQL_RECENT_UNRESOVLED),projectsClause,customClause,recentStart,monthStart,nextMonthStart);
           		keyBase = String.format("cra:RECENT_UNRESOVLED:%s:%s:%s:%s",projectsClause,customClause,monthStart,nextMonthStart);
            	int nru = Jql.getSearchCount(qru,context,searchService,user,this,this.baseUrl,keyBase);
            	if (RequestError.happened(context)) {
            		break;
            	}
            	final String ruUrl = Jql.getQueryUrl(qru, baseUrl);
            	// recent
            	final String qr = String.format(i18n.getText(JQL_RECENT),projectsClause,customClause,recentStart,monthStart,monthStart);
           		keyBase = String.format("cra:RECENT:%s:%s:%s:%s",projectsClause,customClause,monthStart,nextMonthStart);
            	int nr = Jql.getSearchCount(qr,context,searchService,user,this,this.baseUrl,keyBase);
            	if (RequestError.happened(context)) {
            		break;
            	}
            	final String rUrl = Jql.getQueryUrl(qr, baseUrl);
            	// old unresolved
            	final String qou = String.format(i18n.getText(JQL_OLD_UNRESOLVED),projectsClause,customClause,recentStart,nextMonthStart);
           		keyBase = String.format("cra:OLD_UNRESOLVED:%s:%s:%s:%s",projectsClause,customClause,monthStart,nextMonthStart);
            	int nou = Jql.getSearchCount(qou,context,searchService,user,this,this.baseUrl,keyBase);
            	if (RequestError.happened(context)) {
            		break;
            	}
            	final String ouUrl = Jql.getQueryUrl(qou, baseUrl);
            	// old resolved
            	final String qor = String.format(i18n.getText(JQL_OLD_RESOLVED),projectsClause,customClause,recentStart,monthStart,nextMonthStart);
           		keyBase = String.format("cra:OLD_RESOLVED:%s:%s:%s:%s",projectsClause,customClause,monthStart,nextMonthStart);
            	int nor = Jql.getSearchCount(qor,context,searchService,user,this,this.baseUrl,keyBase);
            	if (RequestError.happened(context)) {
            		break;
            	}
            	// all resolved
            	final String qar = String.format(i18n.getText(JQL_ALL_RESOLVED),projectsClause,customClause,monthStart,nextMonthStart);
            	final String arUrl = Jql.getQueryUrl(qar, baseUrl);
           		keyBase = String.format("cra:ALL_RESOLVED:%s:%s:%s:%s",projectsClause,customClause,monthStart,nextMonthStart);
            	AvgDays avgDaysRequest = new AvgDays(qar, context, searchService, user, this,this.baseUrl,keyBase);
            	AvgDaysData avgDaysData = (AvgDaysData)Cache.get(avgDaysRequest);
            	if (RequestError.happened(context)) {
            		break;
            	}
            	String avDays = avgDaysData.averageDays;
            	final String orUrl = Jql.getQueryUrl(qor, baseUrl);
            	final String qcr = String.format(i18n.getText(JQL_CURRENT_RESOLVED),projectsClause,customClause,monthStart,nextMonthStart,nextMonthStart);
            	final String crUrl = Jql.getQueryUrl(qcr, baseUrl);
            	final String qrr = String.format(i18n.getText(JQL_RECENT_RESOVLED),projectsClause,customClause,recentStart,monthStart,monthStart,nextMonthStart);
            	final String rrUrl = Jql.getQueryUrl(qrr, baseUrl);
            	final String qsa = String.format(i18n.getText(JQL_CURRENT_ALIVE),projectsClause,customClause,monthStart,nextMonthStart);
            	final String saUrl = Jql.getQueryUrl(qsa, baseUrl);
            	int nar = avgDaysData.issueCount;
    			add(data,context,monthEnd,ncc,ccUrl,ncu,cuUrl,nru,ruUrl,nr,rUrl,nor,orUrl,nou,ouUrl,saUrl,crUrl,rrUrl,arUrl,avDays,nar);
    			totalAllNetAdds += ncu - (nr-nru) - nor;
    			monthCount++;
        	}
    	}
    	double averageNetAddPerMonth = monthCount > 0 ? (double)totalAllNetAdds / (double)monthCount : 0;
    	context.put(AVERAGE, averageNetAddPerMonth);
		return data;
	}

    private void add(List<Map<String, Object>> data,
			Map<String, Object> context, String monthStr, int ncc,
			String ccUrl, int ncu, String cuUrl, int nru, String ruUrl, int nr,
			String rUrl, int nor, String orUrl, int nou, String ouUrl,
			String saUrl, String crUrl, String rrUrl, String arUrl,
			String avDays, int nar) {
		Map<String,Object> m = new HashMap<String, Object>();
    	m.put("month",monthStr);
    	m.put("c",ncc);
    	m.put("cu",ncu);
    	m.put("ru",nru);
    	m.put("r",nr);
    	m.put("or",nor);
    	m.put("ou",nou);
    	m.put("saU",saUrl);
    	m.put("cU",ccUrl); // URL for current
    	m.put("cuU",cuUrl); // URL for current unresolved
    	m.put("ruU",ruUrl); // URL for recent unresolved
    	m.put("rU",rUrl); // URL for recent
    	m.put("orU",orUrl); // URL for old resolved
    	m.put("ouU",ouUrl); // URL for old unresolved
    	m.put("add",ncc-nar); // net add
    	m.put("rrU",rrUrl); // URL for recent resolved
    	m.put("crU",crUrl); // URL for current resolved
    	m.put("arU",arUrl);// URL for all resolved in that month
    	m.put("ad",avDays);// average days to resolve
    	m.put("ar",nar);// count of defects resolved in that month
    	data.add(m);
	}

	@Override
	public void insertErrorMessage(Map<String, Object> context, String message) {
		RequestError.insertErrorMessage(context, TITLE, message);
	}
}