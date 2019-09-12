import hudson.model.*
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import java.net.URL
import groovy.transform.Field

@Field private buildNumber = "00"

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
		  
		  //Register Backend service
		  curlStr = "curl --insecure -X POST --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+" -F file=@./apim/"+apimConf['backendService']+" -F 'type=swagger' -F 'organizationId="+jsonObject.id[i]+"' -F 'uploadType=html5' -F 'integral=false' -F 'name="+apimConf['assetName']+"_"+buildNumber+"' https://"+svrName+":"+apimConf['PORT']+"/api/portal/v1.3/apirepo/import"
		  echo curlStr;
		  def response = sh returnStdout: true, script: "${curlStr} > /tmp/axway-mgr/jsonRegBackendStdout.json"
		  def jsonBSContents = readFile "/tmp/axway-mgr/jsonRegBackendStdout.json" 
		  def jsonBSObject = readJSON text: jsonBSContents;
		  sh "cat /tmp/axway-mgr/jsonRegBackendStdout.json | python -m json.tool"
		  
		  //Register Frontend service
		  curlStr = "curl --insecure -X POST --header \"Content-Type: application/json\" --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+ " -d '{\"apiId\":\""+jsonBSObject.id+"\",\"organizationId\":\""+jsonObject.id[0]+"\"}' \"https://"+svrName+":"+apimConf['PORT']+"/api/portal/v1.3/proxies/\""
		  echo curlStr;
		  response = sh returnStdout: true, script: "${curlStr} > /tmp/axway-mgr/jsonFS1Stdout.json"
		  def jsonFS1Contents = readFile "/tmp/axway-mgr/jsonFS1Stdout.json" 
		  def jsonFS1Object = readJSON text: jsonFS1Contents;
		  //sh "cat /tmp/axway-mgr/jsonFS1Stdout.json | python -m json.tool"
		  Map devProps = readJSON text: '{}'
		  devProps.subjectIdFieldName = "Pass Through"
		  devProps.removeCredentialsOnSuccess = "true"
		  Map devices = readJSON text: '{}'
		  devices.name = "Pass Through"
		  devices.type = "passThrough"
		  devices.order = 1
		  devices.properties = devProps		  
		  Map secProfiles = readJSON text: '{}'
		  secProfiles.name = "_default" as String
		  secProfiles.isDefault = true
		  secProfiles.devices = devices
		  		  
		  jsonFS1Object.securityProfiles = secProfiles as String[]
		  jsonFS1Object.path = "/v2/"+jsonFS1Object.name as String
		  writeJSON(file: '/tmp/axway-mgr/jsonFS1Stdout.json', json: jsonFS1Object)
		  sh "cat /tmp/axway-mgr/jsonFS1Stdout.json"
		  //sh "cat /tmp/axway-mgr/jsonFS1Stdout.json | python -m json.tool"		  
	  
		  curlStr = "curl --insecure -X PUT --header \"Content-Type: application/json\" --user "+apimConf['axwayUser']+":"+apimConf['axwayPasswd']+ " -d '@/tmp/axway-mgr/jsonFS1Stdout.json' \"https://"+svrName+":"+apimConf['PORT']+"/api/portal/v1.3/proxies/"+jsonBSObject.id+"\""
		  echo curlStr;
		  response = sh returnStdout: true, script: "${curlStr} > /tmp/axway-mgr/jsonFS2Stdout.json"
		  def jsonFS2Contents = readFile "/tmp/axway-mgr/jsonFS2Stdout.json" 
		  def jsonFS2Object = readJSON text: jsonFS2Contents;
		  sh "cat /tmp/axway-mgr/jsonFS2Contents.json | python -m json.tool"
		  
		  print "\n********************END*******************\n"
		  
		  		  
		  break;
		}else{
		  
		}
	}
}

return this