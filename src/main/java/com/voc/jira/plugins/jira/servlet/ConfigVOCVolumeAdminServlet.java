package com.voc.jira.plugins.jira.servlet;

import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.customfield.SelectSeverityField;
import com.voc.jira.plugins.jira.util.Jql;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//Note: if build fails with cannot find org.apache.commons.collections.MultiMap
//      then remove and re-add /lib/usercompatibliity-jira-0.19-SNAPSHOT.jar from buildpath

import static com.google.common.base.Preconditions.checkNotNull;

public class ConfigVOCVolumeAdminServlet extends HttpServlet implements ActionListener {
	private static final Logger log = LoggerFactory
			.getLogger(ConfigVOCVolumeAdminServlet.class);
	private static final String ADMIN_TEMPLATE = "/templates/admin/admin.vm";
	private static final String HIGH_GUIDANCE_DEFAULT = "Many customers have reported.";
	private static final String MED_GUIDANCE_DEFAULT = "Several customers have reported.";
	private static final String LOW_GUIDANCE_DEFAULT = "A few customers have reported.";
	private static final long serialVersionUID = 42L;
	private final UserManager userManager;
	private final TemplateRenderer templateRenderer;
	private final LoginUriProvider loginUriProvider;
	private final ConfigurationManager configurationManager;
	private final CustomFieldManager customFieldManager;
	private final OptionsManager optionsManager;
	//private SelectSeverityField<?, ?> selectSeverityField;
	private String logoPath = "";
	private String baseUrl = "";

	public ConfigVOCVolumeAdminServlet(UserManager userManager,
			TemplateRenderer templateRenderer,
			LoginUriProvider loginUriProvider,
			ConfigurationManager configurationManager,
			CustomFieldManager customFieldManager,
			OptionsManager optionsManager,
			ApplicationProperties applicationProperties,
			WebResourceUrlProvider webResourceUrlProvider) {
		this.userManager = checkNotNull(userManager, "userManager");
		this.templateRenderer = checkNotNull(templateRenderer,
				"templateRenderer");
		this.loginUriProvider = checkNotNull(loginUriProvider,
				"loginUriProvider");
		this.configurationManager = checkNotNull(configurationManager,
				"configurationManager");
		this.customFieldManager = checkNotNull(customFieldManager,
				"customFieldManager");
		this.optionsManager = checkNotNull(optionsManager, "optionsManager");
		this.logoPath = webResourceUrlProvider.getStaticPluginResourceUrl(
				"com.voc.jira.plugins.VOC-Utilities:resources",
				"voc-volume-logo", UrlMode.ABSOLUTE);
		this.baseUrl = applicationProperties.getBaseUrl();
	}

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String username = userManager.getRemoteUsername(req);
		if (username == null || !userManager.isSystemAdmin(username)) {
			redirectToLogin(req, resp);
			return;
		}

		User user = UserUtils.getUser(username);

		resp.setContentType("text/html;charset=utf-8");
		Map<String, Object> context = Maps.newHashMap();
		context.put("highGuidanceDefault", HIGH_GUIDANCE_DEFAULT);
		context.put("medGuidanceDefault", MED_GUIDANCE_DEFAULT);
		context.put("lowGuidanceDefault", LOW_GUIDANCE_DEFAULT);
		if (configurationManager.getIsVisible().contains("yes")) {
			context.put("isVisible", "yes");
		} else {
			context.put("isVisible", "no");
		}
		if (isCustomFieldPresent("Uservoice")) {
			context.put("cfUservoice", "success");
		}
		if (isCustomFieldPresent("Support Tracks")) {
			context.put("cfSupportTracks", "success");
		}
		if (isCustomFieldPresent("Stand-ups")) {
			context.put("cfStand-ups", "success");
		}
		if (isCustomFieldPresent("Support Tracks Weekly Avg")) {
			context.put("cfSupportTracksWeeklyAvg", "success");
		}
		if (isCustomFieldPresent("Feedback Forums")) {
			context.put("cfFeedbackforums", "success");
		}
		if (isCustomFieldPresent("Salesforce")) {
			context.put("cfSalesforce", "success");
		}
		if (isCustomFieldPresent("VOC Volume Other")) {
			context.put("cfVOCVolumeOther",
					getCustomFieldOptions("VOC Volume Other"));
		}
		if (isCustomFieldPresent("VOC Volume Other Value")) {
			context.put("cfVOCVolumeOtherValue", "success");
		}
		if (isCustomFieldPresent("VOC Volume")) {
			context.put("cfVOCVolume", getCustomFieldOptions("VOC Volume"));
		}
		System.out.println("just before if (isCustomFieldPresent(selectSeverityField.getSeverityFieldName()))");
		if (isCustomFieldPresent(SelectSeverityField.getSeverityFieldName())) {
			context.put("cfSeverity", getCustomFieldOptions(SelectSeverityField.getSeverityFieldName()));
			context.put("cfSeverityName", SelectSeverityField.getSeverityFieldName());
		}
		context.put("createSeverity", configurationManager.getCreateSeverity());
		if (configurationManager.getHighGuidance().length() > 0) {
			context.put("highGuidance", configurationManager.getHighGuidance());
		} else {
			context.put("highGuidance", HIGH_GUIDANCE_DEFAULT);
		}
		if (configurationManager.getMedGuidance().length() > 0) {
			context.put("medGuidance", configurationManager.getMedGuidance());
		} else {
			context.put("medGuidance", MED_GUIDANCE_DEFAULT);
		}
		if (configurationManager.getLowGuidance().length() > 0) {
			context.put("lowGuidance", configurationManager.getLowGuidance());
		} else {
			context.put("lowGuidance", LOW_GUIDANCE_DEFAULT);
		}
		context.put("logoPath", logoPath);
		context.put("baseUrl", baseUrl);
		context.put("SMTPServer", configurationManager.getSMTPServer());
		context.put("issuetypesJQL", configurationManager.getIssuetypesJQL());
		context.put("jql", configurationManager.getJQL());

		if (configurationManager.getIssuetypesJQL().length() > 0) {
			context.put("validIssueTypes",
					validateJql(configurationManager.getIssuetypesJQL(), user));
		}

		if (configurationManager.getJQL().length() > 0) {
			context.put("validJql",
					validateJql(configurationManager.getJQL(), user));
		}

		templateRenderer.render(ADMIN_TEMPLATE, context, resp.getWriter());
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String username = userManager.getRemoteUsername(req);
		if (username == null || !userManager.isSystemAdmin(username)) {
			redirectToLogin(req, resp);
			return;
		}

		User user = UserUtils.getUser(username);

		resp.setContentType("text/html;charset=utf-8");
		Map<String, Object> context = Maps.newHashMap();
		context.put("highGuidanceDefault", HIGH_GUIDANCE_DEFAULT);
		context.put("medGuidanceDefault", MED_GUIDANCE_DEFAULT);
		context.put("lowGuidanceDefault", LOW_GUIDANCE_DEFAULT);
		context.put("logoPath", logoPath);
		context.put("baseUrl", baseUrl);
		configurationManager.updateConfiguration(
				req.getParameter("SMTPServer"),
				req.getParameter("issuetypesJQL"), req.getParameter("jql"),
				req.getParameter("isVisible"),
				req.getParameter("createSeverity"),
				req.getParameter("highGuidance"),
				req.getParameter("medGuidance"),
				req.getParameter("lowGuidance"));
		context.put("SMTPServer", configurationManager.getSMTPServer());
		if (configurationManager.getIssuetypesJQL().length() > 0) {
			context.put("issuetypesJQL",
					configurationManager.getIssuetypesJQL());
			context.put("validIssueTypes",
					validateJql(configurationManager.getIssuetypesJQL(), user));
		} else {
			context.put("issuetypesJQL", "");
		}
		if (configurationManager.getJQL().length() > 0) {
			context.put("jql", configurationManager.getJQL());
			context.put("validJql",
					validateJql(configurationManager.getJQL(), user));
		} else {
			context.put("jql", "");
		}
		// System.out.println("doPost:CM:getIsVisible = " +
		// configurationManager.getIsVisible());
		if (configurationManager.getIsVisible().contains("yes")) {
			context.put("isVisible", "yes");
		} else {
			context.put("isVisible", "no");
		}
		if (isCustomFieldPresent("Uservoice")) {
			context.put("cfUservoice", "success");
		}
		if (isCustomFieldPresent("Support Tracks")) {
			context.put("cfSupportTracks", "success");
		}
		if (isCustomFieldPresent("Stand-ups")) {
			context.put("cfStand-ups", "success");
		}
		if (isCustomFieldPresent("Support Tracks Weekly Avg")) {
			context.put("cfSupportTracksWeeklyAvg", "success");
		}
		if (isCustomFieldPresent("Feedback Forums")) {
			context.put("cfFeedbackforums", "success");
		}
		if (isCustomFieldPresent("Salesforce")) {
			context.put("cfSalesforce", "success");
		}
		if (isCustomFieldPresent("VOC Volume Other")) {
			context.put("cfVOCVolumeOther",
					getCustomFieldOptions("VOC Volume Other"));
		}
		if (isCustomFieldPresent("VOC Volume Other Value")) {
			context.put("cfVOCVolumeOtherValue", "success");
		}
		if (isCustomFieldPresent("VOC Volume")) {
			context.put("cfVOCVolume", getCustomFieldOptions("VOC Volume"));
		}
		
		if (isCustomFieldPresent(SelectSeverityField.getSeverityFieldName())) {
			context.put("cfSeverity", configurationManager
					.getSeverityOptionsString(getCustomField(SelectSeverityField.getSeverityFieldName())));
			context.put("cfSeverityName", SelectSeverityField.getSeverityFieldName());
		}
		if (configurationManager.getCreateSeverity().contains("yes")) {
			context.put("createSeverity", "yes");
		} else {
			context.put("createSeverity", "no");
		}
		
		if (configurationManager.getHighGuidance().length() > 0) {
			// System.out.println("getHighGuidance: " +
			// configurationManager.getHighGuidance());
			context.put("highGuidance", configurationManager.getHighGuidance());
		} else {
			context.put("highGuidance", HIGH_GUIDANCE_DEFAULT);
		}
		if (configurationManager.getMedGuidance().length() > 0) {
			context.put("medGuidance", configurationManager.getMedGuidance());
		} else {
			context.put("medGuidance", MED_GUIDANCE_DEFAULT);
		}
		if (configurationManager.getLowGuidance().length() > 0) {
			context.put("lowGuidance", configurationManager.getLowGuidance());
		} else {
			context.put("lowGuidance", LOW_GUIDANCE_DEFAULT);
		}
		templateRenderer.render(ADMIN_TEMPLATE, context, resp.getWriter());
	}
	
	public void doCreateSeverity() {
		System.out.println("in doCreateSeverity");;
	}

	private boolean isCustomFieldPresent(String cf) {
		//System.out.println("inside isCustomFieldPresent for " + cf);
		try {
			if (customFieldManager.getCustomFieldObjectByName(cf) != null) {
				return true;
			} else {
				System.out.println("custom field " + cf + " not found.");
			}
		} catch (NullPointerException e) {
			log.error("NullPointerException: ", e);
			return false;
		}
		return false;
	}

	private String getCustomFieldOptions(String cf) {
		String strVocVolumeOptions = "";
		List<Option> optsVocVolume = new ArrayList<Option>();
		CustomField cfVOCVolume = getCustomField(cf);
		if(cfVOCVolume.getConfigurationSchemes().size() > 0) {
			FieldConfigScheme cfs = (FieldConfigScheme) cfVOCVolume
					.getConfigurationSchemes().get(0);
			@SuppressWarnings("unchecked")
			Map<FieldConfig, GenericValue> configs = cfs.getConfigsByConfig();
			FieldConfig config = (FieldConfig) configs.keySet().iterator().next();
			optsVocVolume = optionsManager.getOptions(config);
			for (Iterator<Option> iterator = optsVocVolume.iterator(); iterator.hasNext();) {
				Option option = iterator.next();
				strVocVolumeOptions += option;
				if (iterator.hasNext()) {
					strVocVolumeOptions += ", ";
				}
			}
		}
		return strVocVolumeOptions;
	}
	
	private CustomField getCustomField(String cf) {
		return customFieldManager.getCustomFieldObjectByName(cf);
	}

	private void redirectToLogin(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		log.info("redirect to login");
		response.sendRedirect(loginUriProvider.getLoginUri(getUri(request))
				.toASCIIString());
	}

	private URI getUri(HttpServletRequest request) {
		StringBuffer builder = request.getRequestURL();
		if (request.getQueryString() != null) {
			builder.append("?");
			builder.append(request.getQueryString());
		}
		return URI.create(builder.toString());
	}

	private String validateJql(String jql, User user) {
		SearchService searchService = ComponentAccessor
				.getComponentOfType(SearchService.class);
		SearchService.ParseResult parseResult = searchService.parseQuery(user,
				jql);
		if (Jql.isQueryValid(jql, searchService, user)) {
			Query query = parseResult.getQuery();
			try {
				if (searchService.searchCount(user, query) > 0) {
					return "yes";
				} else {
					return "zero results";
				}
			} catch (SearchException e) {
				log.error("Error processing JQL: " + e.getMessage(), e);
			}
		}

		return "no";
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}