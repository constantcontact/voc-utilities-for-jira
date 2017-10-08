package com.voc.jira.plugins.jira.jql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.sal.api.search.SearchProvider;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.plugin.jql.function.JqlFunctionModuleDescriptor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.query.Query;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;
import com.voc.jira.plugins.jira.jql.LinkedIssueTypeCreatedBefore;

public class LinkedIssueTypeCreatedBeforeQuery extends AbstractJqlFunction {
	private static final Logger log = LoggerFactory.getLogger(LinkedIssueTypeCreatedBefore.class);
	private IssueTypeManager issueTypeManager = 
			ComponentAccessor.getComponent(IssueTypeManager.class);
    private SearchService searchService;
	private IssueLinkManager issueLinkManager =
			ComponentAccessor.getComponent(IssueLinkManager.class);
	private LinkCollection linkCollection;
	@SuppressWarnings("unused") private volatile JqlFunctionModuleDescriptor descriptor; //initialized by JIRA
	private I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
	
	
	public LinkedIssueTypeCreatedBeforeQuery (
			IssueTypeManager issueTypeManager,
			IssueLinkManager issueLinkManager,
    		SearchService searchService, 
			SearchRequestService searchRequestService) {
		this.issueTypeManager = issueTypeManager;
		this.issueLinkManager = issueLinkManager;
		this.searchService = searchService;
	}
	
	
	/**
	 * Used to find plugin resources.
	 */
	public void init(final JqlFunctionModuleDescriptor descriptor) {
	    this.descriptor = descriptor;
	}
	
	/**
	 * Returns the name that can be used in JQL to invoke the function.
	 */
	public String getFunctionName() {
	    return "linkedCreatedBeforeQuery";
	}
	
	/**
	 * Should return true if the function returns a list (IN or NOT IN operators)
	 * or false if it returns a scalar (=, !=, <, >, <=, >=, IS and IS NOT).
	 * e.g. issues in linkedIssuetypeCreatedBefore("Support Request")
	 */
	public boolean isList() {
	    return true;
	}

	@Override
	@Nonnull
	public JiraDataType getDataType() {
		return JiraDataTypes.ISSUE;
	}

	@Override
	public int getMinimumNumberOfExpectedArguments() {
		return 2;
	}

	@Override
	@Nonnull
	public List<QueryLiteral> getValues(@Nonnull final QueryCreationContext queryCreationContext,
			@Nonnull final FunctionOperand operand, @Nonnull final TerminalClause terminalClause) {
		//System.out.println("==== START getValues ====");
		final List<QueryLiteral> literals = new LinkedList<QueryLiteral>();
		
		final List<String> arguments = operand.getArgs();
		//Can't do anything when no argument is specified. This is an error so return empty list.
	    if (arguments.isEmpty() || arguments.size() != 2) {
	    	//System.out.println("---- empty values ----");
	        return Collections.emptyList();
	    }
	    
	    if (queryCreationContext.getApplicationUser() != null) {
		    //Get all linked issues for filter and issueType where linked issue is created before issue.
		    final List<Long> issueIDs;
		    //System.out.println("---- Start getIssueIDs ----");
		    issueIDs = getIssueIDs(queryCreationContext.getApplicationUser(), queryCreationContext, arguments);
		    //System.out.println("---- End getIssueIDs ----");
		    //Covert all the issues to query literals.
		    //System.out.println("---- Start adding literals ----");
		    for (Long issueID : issueIDs) {
		        literals.add(new QueryLiteral(operand, issueID));
		    }
		    //System.out.println("---- End adding literals ----");
	    } else {
	    	log.warn("---- User NULL, adding literals skipped ----");
	    }
	    //System.out.println("==== END getValues ====");
		return literals;
	}
	
	private List<Long> getIssueIDs(ApplicationUser user, QueryCreationContext queryCreationContext, 
			List<String> arguments) {
		/* Given a filter and an issueType,
		 * When applying the filter to get a list of issues
		 * Then iterate through the list of filtered issues if the target issueType, 
		 * testing links for linked issue Created before issue Created,
		 * And then add the issues found to a Collection of issues and return.
		 */
		final String queryStr = arguments.get(0);
		final String issueType = arguments.get(1);
		List<Long> linkedIssueIDs = new ArrayList<Long>();
		//Search on filter and iterate through issues
		//Collection<Issue> filteredIssues = Collections.emptyList();
		final Query query = getQuery(user, queryStr);
		SearchProvider searchProvider = (SearchProvider) ComponentAccessor.getComponent(SearchProvider.class);
		@SuppressWarnings("unchecked")
		final Iterable<Issue> filteredIssues = (Iterable<Issue>) 
				searchProvider.search(user.getDisplayName(), query.toString()); 
						//PagerFilter.getUnlimitedFilter()).getIssues();
		if (filteredIssues.iterator().hasNext()) {
			final Issue issue = filteredIssues.iterator().next();
			// System.out.println("In parsing search list: issue ID = " + issue.getId());
			linkedIssueIDs.addAll(getLinkedIssueIDs(user, issue, issueType));
		}
		/*
		ArrayList<Issue> issuesList = new ArrayList<Issue>();
		issuesList = (ArrayList<Issue>) searchProvider.search(query, applicationUser, 
				PagerFilter.getUnlimitedFilter()).getIssues();
		//System.out.println("issue list size: " + issuesList.size());
		for (final Issue issue : issuesList) {
			// System.out.println("In parsing search list: issue ID = " + issue.getId());
			linkedIssueIDs.addAll(getLinkedIssueIDs(user, issue, issueType));
		}
		*/
		return linkedIssueIDs;
	}
	
	private Query getQuery(ApplicationUser user, String queryStr) {
		if (queryStr == null) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBeforeQuery.query.null", queryStr));
			return JqlQueryBuilder.newBuilder().buildQuery();
		}
		if (queryStr.isEmpty()) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBeforeQuery.query.empty", queryStr));
			return JqlQueryBuilder.newBuilder().buildQuery();
		}
        SearchService.ParseResult parseResult = searchService.parseQuery(user, queryStr);
        if(!parseResult.isValid()) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBeforeQuery.query.invalid", queryStr));
        	return JqlQueryBuilder.newBuilder().buildQuery();	
        }
        return parseResult.getQuery();        
    }
	
	private ArrayList<Long> getLinkedIssueIDs(ApplicationUser user, Issue issue, String issueType) {
		Collection<Issue> linkedIssues = new LinkedList<Issue>();
		ArrayList<Long> linkedIssueIDs = new ArrayList<Long>();
		try {
	        linkCollection = issueLinkManager.getLinkCollection(issue, user);
	        Set<IssueLinkType> linkTypes = linkCollection.getLinkTypes();
	        for (IssueLinkType linkType : linkTypes) {
			    // Get the outward linked issues
	            List<Issue> outwardIssues = linkCollection.getOutwardIssues(linkType.getName());
	            if (outwardIssues != null) {
	            	for (Issue outwardIssue : outwardIssues) {
	                    if (outwardIssue.getCreated().before(issue.getCreated()) 
	                    		&& outwardIssue.getIssueType().getName().contains(issueType)) {
	                    	//System.out.println("issue ID = " + issue.getId());
	                    	linkedIssues.add(issue);
	                    	linkedIssueIDs.add(issue.getId());
	                    }
	            	}
	            }
	            // And the inward linked issues
	            List<Issue> inwardIssues = linkCollection.getInwardIssues(linkType.getName());
	            if (inwardIssues != null) {
	            	for (Issue inwardIssue : inwardIssues) {
	            		if (inwardIssue.getCreated().before(issue.getCreated()) 
	            				&& inwardIssue.getIssueType().getName().contains(issueType)) {
	            			//System.out.println("issue ID = " + issue.getId());
	            			linkedIssues.add(issue);
	            			linkedIssueIDs.add(issue.getId());
		                }
		            }
	            }
	        }
	    } catch(NullPointerException e) {
			log.error("NullPointerException: ",e);
			e.printStackTrace();
	    } catch(UnsupportedOperationException e) {
			log.error("NullPointerException: ",e);
			e.printStackTrace();
		}
		
		return linkedIssueIDs;
	}

	@Override
	@Nonnull
	public MessageSet validate(ApplicationUser searcherApplicationUser, @Nonnull FunctionOperand operand,
			@Nonnull TerminalClause terminalClause) {
		
		//System.out.println("==== IN validate ====");
		
		MessageSet messages = new MessageSetImpl();
		final List<String> arguments = operand.getArgs();
		
		//Make sure we have the correct number of arguments before validating.
	    if (arguments.isEmpty() || arguments.size() < 2) {
	        messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBeforeQuery.missing.arguments", operand.getName()));
	        return messages;
	    } else if (arguments.size() > 2) {
	        messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBeforeQuery.extra.arguments", operand.getName()));
	        return messages;
	    }
	    
	    final String query = arguments.get(0);
	    MessageSet queryMessages = new MessageSetImpl();
	    queryMessages = validateQuery(searcherApplicationUser, query, messages);
	    if(queryMessages.hasAnyErrors()) {
	    	messages.addMessageSet(queryMessages);
	    }
	    final String issueType = arguments.get(1);
	    MessageSet issueTypeMessages = new MessageSetImpl();
	    issueTypeMessages = validateIssueType(issueType, messages);
	    if(issueTypeMessages.hasAnyErrors()) {
	    	messages.addMessageSet(issueTypeMessages);
	    }
	    //System.out.println("==== OUT validate ==== > error messages count: " + messages.getErrorMessages().size());
	    return messages;
	}

	private MessageSet validateQuery(ApplicationUser searcher, String query, MessageSet messages) {
		if (query == null) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBeforeQuery.query.null", query));
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBeforeQuery.query.null", query));
			return messages;
		}
		if (query.isEmpty()) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBeforeQuery.query.empty", query));
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBeforeQuery.query.empty", query));
			return messages;
		}
		return messages;
	}	

	private MessageSet validateIssueType(String issueType, MessageSet messages) {
		if (issueType == null) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.issueType.null", issueType));
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.issueType.null", issueType));
			return messages;
		}
		try {
			for (IssueType issueTypeFound : issueTypeManager.getIssueTypes()) {
				if (issueType.equals(issueTypeFound.getName())) {
					return messages;
				}
			}
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.issueType.invalid",issueType));
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.issueType.invalid",issueType));
			// System.out.println(i18n.getText("linkedIssuetypeCreatedBefore.issueType.invalid", issueType));
		} catch (NullPointerException e) {
			log.error("NullPointerException: ", e);
			e.printStackTrace();
			messages.addErrorMessage("issueTypeManager null pointer exception error.");
		}
		return messages;
	}
}
