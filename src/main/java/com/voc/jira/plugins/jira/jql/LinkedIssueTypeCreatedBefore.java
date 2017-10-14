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
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.sal.api.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.plugin.jql.function.JqlFunctionModuleDescriptor;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters.TextSearchMode;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.sharing.search.SharedEntitySearchResult;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;

/**
 * LinkedIssueTypeCreatedBefore
 * e.g. issues in linkedIssueTypeCreatedBefore("Project Team Filter","Support Request")
 * @author djelliso
 *
 */
public class LinkedIssueTypeCreatedBefore extends AbstractJqlFunction {	
	private static final Logger log = LoggerFactory.getLogger(LinkedIssueTypeCreatedBefore.class);
	private IssueTypeManager issueTypeManager = 
			ComponentAccessor.getComponent(IssueTypeManager.class);
	private SearchRequestService searchRequestService = 
			ComponentAccessor.getComponent(SearchRequestService.class);
	private IssueLinkManager issueLinkManager =
			ComponentAccessor.getComponent(IssueLinkManager.class);
	private LinkCollection linkCollection;
	@SuppressWarnings("unused") private volatile JqlFunctionModuleDescriptor descriptor; //initialized by JIRA
	private I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
	
	public LinkedIssueTypeCreatedBefore (
			IssueTypeManager issueTypeManager,
			IssueLinkManager issueLinkManager,
			SearchRequestService searchRequestService) {
		this.issueTypeManager = issueTypeManager;
		this.issueLinkManager = issueLinkManager;
		this.searchRequestService = searchRequestService;
	}
	
	
	/**
	 * Used to find plugin resources.
	 */
	public void init(final JqlFunctionModuleDescriptor descriptor) {
		//System.out.println("In Init");
	    this.descriptor = descriptor;
	}
	
	/**
	 * Returns the name that can be used in JQL to invoke the function.
	 */
	public String getFunctionName() {
	    return "linkedCreatedBefore";
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
	
	private List<Long> getIssueIDs(ApplicationUser applicationUser, QueryCreationContext queryCreationContext, 
			List<String> arguments) {
		/* Given a filter and an issueType,
		 * When applying the filter to get a list of issues
		 * Then iterate through the list of filtered issues if the target issueType, 
		 * testing links for linked issue Created before issue Created,
		 * And then add the issues found to a Collection of issues and return.
		 */
		final String filtername = arguments.get(0);
		final String issueType = arguments.get(1);
		List<Long> linkedIssueIDs = new ArrayList<Long>();
		//Search on filter and iterate through issues
		//Collection<Issue> filteredIssues = Collections.emptyList();
		SearchRequest ofilter = getFilterByName(applicationUser, filtername);
		SearchProvider searchProvider = (SearchProvider) ComponentAccessor.getComponent(SearchProvider.class);
		//SearchResults issueResults;
		@SuppressWarnings("unchecked")
		final Iterable<Issue> filteredIssues = (Iterable<Issue>) searchProvider.search(applicationUser.toString(), ofilter.getQuery().toString()); 
		if (filteredIssues.iterator().hasNext()) {
			final Issue issue = filteredIssues.iterator().next();
			// System.out.println("In parsing search list: issue ID = " + issue.getId());
			linkedIssueIDs.addAll(getLinkedIssueIDs(applicationUser, issue, issueType));
		}
		return linkedIssueIDs;
	}
	
	private SearchRequest getFilterByName(ApplicationUser searcher, String filtername) {
		if (filtername == null) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filtername.null", filtername));
			return new SearchRequest();
		}
		if (filtername.isEmpty()) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filtername.empty", filtername));
			return new SearchRequest();
		}
		SharedEntitySearchResult<SearchRequest> oFilter = getAllFiltersByName(searcher, filtername); 
		if(oFilter.getTotalResultCount() == 0) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filter.invalid", filtername));
			return new SearchRequest();
		}
		if(oFilter.getTotalResultCount() > 1) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filter.many", filtername));
			return new SearchRequest();
		}
		return oFilter.getResults().get(0);
	}
	
	private ArrayList<Long> getLinkedIssueIDs(ApplicationUser applicationUser, Issue issue, String issueType) {
		Collection<Issue> linkedIssues = new LinkedList<Issue>();
		ArrayList<Long> linkedIssueIDs = new ArrayList<Long>();
		try {
	        linkCollection = issueLinkManager.getLinkCollection(issue, applicationUser);
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

	@Nonnull
	public MessageSet validate(ApplicationUser searcherApplicationUser, @Nonnull FunctionOperand operand,
			@Nonnull TerminalClause terminalClause) {
		
		//System.out.println("==== IN validate ====");
		
		MessageSet messages = new MessageSetImpl();
		final List<String> arguments = operand.getArgs();
		
		//Make sure we have the correct number of arguments before validating.
	    if (arguments.isEmpty() || arguments.size() < 2) {
	        messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.missing.arguments", operand.getName()));
	        //messages.addErrorMessage("missing arguments (\"Filter\", \"IssueType\")");
	        return messages;
	    } else if (arguments.size() > 2) {
	        messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.extra.arguments", operand.getName()));
	        //messages.addErrorMessage("too many arguments (\"Filter\", \"IssueType\")");
	        return messages;
	    }
	    
	    final String filter = arguments.get(0);
	    MessageSet filterMessages = new MessageSetImpl();
	    filterMessages = validateFiltername(searcherApplicationUser, filter, messages);
	    if(filterMessages.hasAnyErrors()) {
	    	messages.addMessageSet(filterMessages);
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

	private MessageSet validateFiltername(ApplicationUser searcher, String filtername, MessageSet messages) {
		if (filtername == null) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filtername.null", filtername));
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.filtername.null", filtername));
			return messages;
		}
		if (filtername.isEmpty()) {
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filtername.empty", filtername));
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.filtername.empty", filtername));
			return messages;
		}
		SharedEntitySearchResult<SearchRequest> oFilter = getAllFiltersByName(searcher, filtername); 
		if(oFilter.getTotalResultCount() == 0) {
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.filter.invalid", filtername));
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filter.invalid", filtername));
		}
		if(oFilter.getTotalResultCount() > 1) {
			messages.addErrorMessage(i18n.getText("linkedIssuetypeCreatedBefore.filter.many", filtername));
			log.warn(i18n.getText("linkedIssuetypeCreatedBefore.filter.many", filtername));
		}
		return messages;
	}	

	/**
	 * Gets all filters that match filtername. NOTE: assumes all callers have
	 * verified that filtername is not null and not empty. This is because
	 * callers do different things when it is null/empty so they will have
	 * already checked before calling this.
	 * 
	 * @param searcher
	 * @param filtername
	 * @return
	 */
	private SharedEntitySearchResult<SearchRequest> getAllFiltersByName(ApplicationUser searcher, String filtername) {
		filtername = filtername.replace(" ", "_").trim();
		SimpleErrorCollection errorCollection = new SimpleErrorCollection();
		JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(searcher, errorCollection);
		final SharedEntitySearchParametersBuilder searchParameters = new SharedEntitySearchParametersBuilder();
		searchParameters.setEntitySearchContext(SharedEntitySearchContext.USE);
		searchParameters.setSortColumn(SharedEntityColumn.NAME, false);
		searchParameters.setTextSearchMode(TextSearchMode.EXACT);
		searchParameters.setName(filtername);
		if (searcher != null && !"".equals(searcher.getUsername())) {
			searchParameters.setUserName(searcher.getUsername());
		}
		SharedEntitySearchParameters params = searchParameters.toSearchParameters();
		SharedEntitySearchResult<SearchRequest> filter = searchRequestService.search(jiraServiceContext, params, 0, 50);
		return filter;
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
