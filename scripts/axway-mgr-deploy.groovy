import hudson.model.*
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import java.net.URL
import groovy.transform.Field

@Field private buildNumber = "00"
def frontendSvcName = ""
def frontendSvcId = ""

def deploy(envName, buildNumber) {
    echo "\nStarting...."
	println("Deploy to: "+envName)
	println("Build Number: "+buildNumber)
	
	def apimConf = readProperties  file:  "./conf/apim.conf"
	def curlStr = "";

	sh "mkdir -p /tmp/axway-mgr"
	curlStr = "curl --insecure -X GET --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+" https://"+apimConf['DEV']+"/api/portal/v1.3/organizations"
	sh "${curlStr} > /tmp/axway-mgr/json.out"
	
	def jsonContents = readFile "/tmp/axway-mgr/json.out" 
	def jsonObject = readJSON text: jsonContents;

	def svrName = apimConf['DEV'];
	if (envName == "UAT") 
		svrName = apimConf['UAT']
	else if (envName == "PROD") 
		svrName = apimConf['PRD']

	for (i = 0; i <jsonObject.name.size(); i++) {
		//echo "ORG Name: "+jsonObject.name[i];
		//echo "ORG IDs: "+jsonObject.id[i];
		
		if(jsonObject.name[i]==apimConf['organizationId']){
		  echo "FOUND: "+jsonObject.name[i]+":"+jsonObject.id[i];
		  
		  //curlStr = "curl --insecure -X POST --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+" -F file=@./apim/"+apimConf['backendService']+" -F 'type=swagger' -F 'organizationId="+jsonObject.id[i]+"' -F 'uploadType=html5' -F 'integral=false' -F 'name="+apimConf['assetName']+"_"+buildNumber+"' https://"+svrName+":"+apimConf['PORT']+"/api/portal/v1.3/apirepo/import"
		  //echo curlStr;
		  //sh "${curlStr}"
		  
		  def fname = "./apim/"+apimConf['frontendService'];
		  def frontendServiceJsonContents = readFile(fname); 
		  def frontendServiceJsonObject = readJSON text: frontendServiceJsonContents;
		
		  Map prJsonData = readJSON text: '{}'
		  prJsonData.title = apimConf['assetName']+"_"+buildNumber as String
		  prJsonData.descriptionType = "manual" as String
		  prJsonData.descriptionManual = "Sample Petstore" as String
		  prJsonData.description = "Sample Petstore" as String
		  prJsonData.version = "v"+buildNumber as String
		  prJsonData.organization = jsonObject.id[i] as String

		  frontendServiceJsonObject.info = prJsonData
		
		  writeJSON(file: '/tmp/axway-mgr/tempFrontendService.json', json: frontendServiceJsonObject)
		  
		  sh "cat /tmp/axway-mgr/tempFrontendService.json | python -m json.tool"					  
		  //sh "cat /tmp/axway-mgr/tempFrontendService.json"
							  
		  curlStr = "curl --insecure -X POST --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+" -F file=@./apim/"+apimConf['backendService']+" -F fe-desc=@/tmp/axway-mgr/tempFrontendService.json -F 'type=swagger' -F 'organizationId="+jsonObject.id[i]+"' -F 'uploadType=html5' -F 'integral=false' -F 'name="+apimConf['assetName']+"_"+buildNumber+"' https://"+svrName+":"+apimConf['PORT']+"/registerAPI"
		  
		  //echo curlStr;
		  sh "${curlStr}"
		  
		  curlStr = "curl --insecure -X GET --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+" \"https://"+svrName+":"+apimConf['PORT']+"/api/portal/v1.3/proxies?field=name&op=like&value="+apimConf['assetName']+"_"+buildNumber+"\""
		  
		  def response = sh returnStdout: true, script: "${curlStr} > /tmp/axway-mgr/jsonStdout.json"
		  
		  print "********************Service INFO:********************\n"
		  jsonContents = readFile "/tmp/axway-mgr/jsonStdout.json" 
		  jsonObject = readJSON text: jsonContents;

		  echo "Number of returned size: "+jsonObject.name.size()
		  if (jsonObject.name.size() == 1) {
			echo "Service Name: "+jsonObject.name[0];
			echo "Service ID: "+jsonObject.id[0];
			echo "Service ORG ID: "+jsonObject.organizationId[0]
			echo "Service app ID: "+jsonObject.apiId[0]
			echo "Service version: "+jsonObject.version[0]
			if (jsonObject.vhost[0] instanceof String)
				echo "Service vhost: "+jsonObject.vhost[0]
			echo "Service path: "+jsonObject.path[0]
			if (jsonObject.state[0] instanceof String)
				echo "Service STATUS: "+jsonObject.state[0]
			if (jsonObject.state[0] instanceof String && jsonObject.state[0] == "unpublished") {
				echo "retrieve client application app ID from app name"
				curlStr = "curl --insecure -X GET --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+" \"https://"+svrName+":"+apimConf['PORT']+"/api/portal/v1.3/applications?field=name&op=like&value="+apimConf['applicationName']+"\""		  
			    response = sh returnStdout: true, script: "${curlStr} > /tmp/axway-mgr/jsonAppInfoStdout.json"
				def jsonAppContents = readFile "/tmp/axway-mgr/jsonAppInfoStdout.json" 
				def jsonAppObject = readJSON text: jsonAppContents;
				
				echo "Add permission for app"
				curlStr = "curl --insecure -X POST --header \"Content-Type: application/json\" --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+ " -d '{\"apiId\":\""+jsonObject.id[0]+"\",\"enabled\":true}' \"https://"+svrName+":"+apimConf['PORT']+"/api/portal/v1.3/applications/"+jsonAppObject.id[0]+"/apis\""				
				echo curlStr;
				sh "${curlStr}"					
				
				frontendSvcName = jsonObject.name[0]
				frontendSvcId = jsonObject.id[0]
		  
			}
		  } else
			
		  print "\n********************END*******************\n"
		  
		  		  
		  break;
		}else{
		  
		}
	}
	
	return [frontendSvcName, frontendSvcId, svrName]
}

return this