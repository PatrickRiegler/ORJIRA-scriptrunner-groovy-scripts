//import required modules
import org.apache.log4j.Category
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.worklog.WorklogImpl
import java.sql.Timestamp;

// importing further required modules
import groovy.json.JsonSlurper;
import groovy.json.StreamingJsonBuilder;
import org.apache.commons.codec.binary.Base64;

@Grab('com.github.groovy-wslite:groovy-wslite:1.1.2')
import wslite.http.auth.*
import wslite.rest.*
import groovy.json.JsonBuilder

def Category log = Category.getInstance("com.onresolve.jira.groovy")
// log.setLevel(org.apache.log4j.Level.INFO)
log.setLevel(org.apache.log4j.Level.DEBUG)

// Get the current logged in user
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

// get JIRA base URL:
def baseurl = com.atlassian.jira.component.ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
log.debug("baseurl: "+baseurl)

// Get the current Issue
def SubjectIssue = issue

// debugging and define the ID
log.debug("key: "+issue.getKey())
def getId = issue.getId()
log.debug("getId: "+getId)

def getStatus = issue.getStatusObject().getName();
log.debug("getStatus: "+getStatus)
if(getStatus != "Closed" && getStatus != "Resolved") {
  log.debug("Status != Resolved or Closed - Therefore no further action required")
} else {

// Get the Manager classes required
def issueManager = ComponentAccessor.getIssueManager()
def worklogManager = ComponentAccessor.getWorklogManager()

// Get the work log data
def IssueWorkLog = worklogManager.getByIssue(SubjectIssue)

// Work log deleting options
Long newEstimate = 0 // the new estimate for the issue
Boolean announceBool = 1

log.debug("original: "+issue.getOriginalEstimate())
log.debug("before: "+issue.getEstimate())

host=baseurl
def urlApi = host+"/rest/tempo-planning/1/allocation?planItemType=ISSUE&planItemId="+issue.getId();
log.debug("url: "+urlApi)
username = "techuser"
password = "techuser"
String authStr = username + ":" + password;
// encode data on your side using BASE64
    byte[] bytesEncoded = Base64.encodeBase64(authStr.getBytes());
    String authEncoded = new String(bytesEncoded);

def body_req = [
        "user": "techuser",
        "password": "techuser" 
]

URL url;
url = new URL(urlApi);

URLConnection connection = url.openConnection();
connection.requestMethod = "GET"
connection.setRequestProperty("Authorization", "Basic "+authEncoded);
connection.doOutput = true
connection.setRequestProperty("Content-Type", "application/json")
connection.connect();

String result
String restxt
connection.content.withReader{ result = new JsonSlurper().parseText( it.readLine() ); restxt = it.readLine(); }

log.debug("url: " + url);
log.debug("Content:" + connection.getContent())
log.debug("ResponseCode:" + connection.getResponseCode())
log.debug("getResponseMessage:" + connection.getResponseMessage())
log.debug("result:" + result)
log.debug("restxt:" + restxt)

issue.setEstimate(newEstimate)
log.debug("after: "+issue.getEstimate())
Date today = new Date();
Calendar MyDueDate = Calendar.getInstance();
MyDueDate.add(Calendar.DATE,0)
def dueTimestamp = new Timestamp(MyDueDate.getTimeInMillis())
issue.setDueDate(dueTimestamp);
log.debug("new due date: "+issue.getDueDate())


// update the issue
host=baseurl

def client = new RESTClient(host+"/rest")
client.authorization = new HTTPBasicAuthorization("techuser", "techuser")
def response = client.get(path:'/tempo-planning/1/allocation?planItemType=ISSUE&planItemId='+issue.getId(),
        headers:['Content-Type':'application/json; charset=UTF-8'])
log.debug(response.statusCode)
log.debug(response.contentAsString)
def allid = response.json.id[0] 
log.debug("allid: "+allid)

if(allid!=null) {
def start_before_txt = response.json.start[0]
log.debug("start_before_txt: "+start_before_txt);
def start_before = new Date().parse("yyyy-MM-dd",start_before_txt)
log.debug("start_before: "+start_before);
log.debug("today: "+today);
def yesterday = today -1
log.debug("yesterday: "+yesterday);
start=(start_before < today) ? start_before_txt : yesterday.format( 'yyyy-MM-dd' );
} else {
start = new Date().format( 'yyyy-MM-dd' )
}

log.debug("start date: "+start)

// zusammensetzen des json für den PUT
def builder = new JsonBuilder()
def root = builder {
	"id" allid 
	"assignee" response.json.assignee[0]
        "planItem" response.json.planItem[0]
        "scope" response.json.scope[0]
        "start" start
        "end" new Date().format( 'yyyy-MM-dd' )
	"seconds" 0
}

if(allid!=null) {

log.debug("jsonbuilder: "+builder.toPrettyString());

try {
    log.debug("TRY");
    def responseI = client.put(path: '/tempo-planning/1/allocation/'+allid,
            headers: ['Content-Type': 'application/json; charset=UTF-8', 'Accept': 'application/json'])

            {
                text builder.toPrettyString()
            }

    log.debug("TRY2");
    log.debug(responseI.contentAsString)
    log.debug(responseI.json);
    log.debug(responseI.headers);
    log.debug("TRY3");
    log.info("Tempo Plan for Issue "+issue.getKey()+" updated successfully")
} catch(ex) {
    log.debug("ERROR");
    log.debug(ex);
    log.info("There were errors updating the Tempo Plan for Issue "+issue.getKey())
    log.info(ex)
}

} else {

log.debug("allid was 'null'")
log.info("No update required for Issue "+issue.getKey()+" as no Tempo plan was found...")

}

} // else if status != resolved or Closed
