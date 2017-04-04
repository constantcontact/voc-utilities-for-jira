package com.voc.jira.plugins.jira.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.charts.util.ChartReportUtils;
import com.atlassian.jira.project.ProjectManager;

public class Projects {

    public static List<String> getProjects(HttpServletRequest req,ProjectManager projectManager) {
    	ArrayList<String> result = new ArrayList<String>();
		String param = req.getParameter("projects");
		if (param == null) {
			result.add("");
			result.add("");
			return result;
		}
		String[] ps = param.split("\\|");
		String token = "";
		for (String p : ps) {
			if(ChartReportUtils.isValidProjectParamFormat(p)) {
				p = getProjNameFromGadgetProjectsParam(p,projectManager);
			}
			token += ",\"" + p + "\"";
		}
		token = token.substring(1);
		final String template = " AND project in (%s)";
		result.add(String.format(template,token));
		String projectsTitle = " in " + token.replace("\"", "");
		if("true".equals(req.getParameter("hc"))) {
			projectsTitle += " (test data)";
		}
		result.add(projectsTitle);
		return result;
    }   

    private static String getProjNameFromGadgetProjectsParam(String p, ProjectManager projectManager) {
    	Long pid = Long.valueOf(ChartReportUtils.extractProjectOrFilterId(p));
    	if(pid != null && pid.toString().length() > 0) {
			p = projectManager.getProjectObj(pid).getName().trim();
		}
    	return p;
    }    
}
