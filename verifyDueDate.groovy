//import required modules
import org.apache.log4j.Category
import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.*;
import com.atlassian.jira.issue.*
import com.atlassian.jira.issue.worklog.WorklogImpl
import com.atlassian.jira.issue.managers.DefaultIssueManager
import java.sql.Timestamp;

// importing further required modules
import groovy.json.*
import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*
import org.apache.commons.codec.binary.Base64;

@Grab('com.github.groovy-wslite:groovy-wslite:1.1.2')
import wslite.http.auth.*
import wslite.rest.*
import groovy.json.JsonBuilder

import java.nio.charset.StandardCharsets;


def Category log = Category.getInstance("com.onresolve.jira.groovy")
// log.setLevel(org.apache.log4j.Level.INFO)
log.setLevel(org.apache.log4j.Level.DEBUG)

// get JIRA base URL:
def baseurl = com.atlassian.jira.component.ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
log.debug("baseurl: "+baseurl)

host=baseurl
if (verifyDueDate.hasProperty('issue') && verifyDueDate.issue) {
  log.debug("no action required, as issue object exists")
} else {
  log.debug("action required, loading issue object")
  def issueManager = ComponentAccessor.getComponent(DefaultIssueManager)
  def issue = issueManager.getIssueObject("STAUB-365") as Issue
}
log.debug("issue: "+issue)
def urlGetEndDate = host+"/rest/tempo-planning/1/allocation?planItemType=ISSUE&planItemId="+issue.getId();
log.debug("url: "+urlGetEndDate)

def getEndDate = new HttpGet(urlGetEndDate)

byte[] credentials = Base64.encodeBase64(("techuser" + ":" + "techuser").getBytes(StandardCharsets.UTF_8));
getEndDate.setHeader("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));

def clientEndDate = HttpClientBuilder.create().build()

def responseEndDate = clientEndDate.execute(getEndDate)

log.debug("Response code: "+responseEndDate.getStatusLine().getStatusCode())

def bufferedReader = new BufferedReader(new InputStreamReader(responseEndDate.getEntity().getContent()))
def jsonResponseEndDate = bufferedReader.getText()
// log.debug("jsonResponseEndDate: "+jsonResponseEndDate)

def slurper = new JsonSlurper()
def mapEndDate = slurper.parseText(jsonResponseEndDate)

// log.debug("mapEndDate: "+mapEndDate)

def endDate = mapEndDate.end
log.debug("end: "+endDate)

def dueDate = issue.getDueDate()
log.debug("due date: "+dueDate)

//Define the date format as per your input
def dfd = "yyyy-MM-dd HH:mm:ss.S"
def dfe = "[yyyy-MM-dd]"

//Parse the date string with above date format
def compareDueDate = new Date().parse(dfd, dueDate.toString())
def compareEndDate = new Date().parse(dfe, endDate.toString())

log.debug("compareDueDate: "+compareDueDate)
log.debug("compareEndDate: "+compareEndDate)

//Compare both date times
if(compareEndDate>compareDueDate) {
  log.debug("'Due Date' is before 'End Date' - This is not allowed...")
} else {
  log.debug("'Due Date' is after or equals 'End Date' - Everything OK")
}

