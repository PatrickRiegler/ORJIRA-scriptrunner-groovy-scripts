//import required modules
import org.apache.log4j.Category
import com.atlassian.jira.component.*;

// for getting due date from issue
import com.atlassian.jira.event.*
import com.atlassian.jira.issue.*
import com.atlassian.jira.component.*;
import com.atlassian.jira.issue.managers.DefaultIssueManager

// getting the tempo stuff
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.tempoplugin.core.workattribute.api.WorkAttributeService
import com.tempoplugin.core.workattribute.api.WorkAttributeValueService
import com.tempoplugin.core.*


@WithPlugin("is.origo.jira.tempo-plugin")

@PluginModule
WorkAttributeService workAttributeService

def Category log = Category.getInstance("com.onresolve.jira.groovy")
// log.setLevel(org.apache.log4j.Level.INFO)
log.setLevel(org.apache.log4j.Level.DEBUG)

// get JIRA base URL:
def baseurl = com.atlassian.jira.component.ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
log.debug("baseurl: "+baseurl)

/* debugging stuff
*/
def i = 0
event.properties.each { log.debug("key: " + it.key + " --- "); log.debug("value: " + it.value + "<br><br>\n\n"); i++;  }
log.debug("i: "+i)

issueId = event.allocation.planItemId
log.debug("issueId: "+issueId)

// getting due date from current issue
def issueManager = ComponentAccessor.getComponent(DefaultIssueManager)
def issue = issueManager.getIssueObject(issueId) as Issue
log.debug("issuekey: "+issue.getKey())
log.debug("issuesummary: "+issue.summary)
log.debug("due date: "+issue.getDueDate())


