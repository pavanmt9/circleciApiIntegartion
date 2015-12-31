package com.pavan.circleci;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* @author Pavan Kumar M T
* 
*/

public class CircleCiIntegration {

	private static String token = "circlecitoken";

	private static String propFile = "%s/circleciproperty.txt";

	private static boolean alreadyLoaded= false;

	private static String BUILD_NO="buildNo";

	private static String APK_FILE_NAME="ApkFileName";

	private static String BUILD_URL = "buildUrl";

	private static String COMMITTER_DATE = "committerDate";

	private static String COMMITTER_NAME = "committer_name";

	private static String SUBJECT = "subject";

	private static String REPONAME = "reponame";

	private static String PROP_DELIM=":";

	static File file = null;

	static PrintWriter printWriter = null;

	// https://circleci.com/api/v1/project/:username/:project/:build_num/artifacts?circle-token=:token

	static{

		String envCiToken = System.getProperty("env.citoken");

		if(envCiToken!=null)
			token=envCiToken;

	}

	public static void addToPropFile(String content,boolean append){

		String circleCiCodeBase = System.getProperty("env.circlecibase");

		if(circleCiCodeBase==null)
			circleCiCodeBase = System.getProperty("user.dir");

		propFile = String.format(propFile, circleCiCodeBase);

		try {

			if(!alreadyLoaded && !append){

				file = new File(propFile);

				file.getParentFile().mkdirs();

				printWriter = new PrintWriter(file);

				alreadyLoaded=true;

			}

			if(append)
				printWriter.append(content+"\n");
			else
				printWriter.write(content+"\n");

			printWriter.flush();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}

	public static void closeAllResources(){

		if(printWriter!=null){

			printWriter.close();

		}

	}

	public static void downloadAndroidBuild(String downloadUrl,String downloadTo){

		System.out.println("Downloading the build, please wait, this might take some time depending upon file size...");

		System.out.println("URL To Download "+downloadUrl);

		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(downloadUrl+"?circle-token="+token);
		HttpResponse response = null;
		InputStream inputStream = null;
		try {
			response = httpclient.execute(httpget);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			long len = entity.getContentLength();
			try {
				inputStream = entity.getContent();
			} catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// write the file to where you want it.

			FileOutputStream outputStream=null;
			try {
				outputStream = new FileOutputStream(downloadTo);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			int bytesRead = -1;
			byte[] buffer = new byte[4096];
			try {
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				outputStream.close();
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Successfully downloaded the build to path : "+downloadTo);

		}else{

			throw new RuntimeException("Nothing to download, Seems like the link is broken! url : "+downloadUrl);

		}

		File apkFile = new File(downloadTo);

		if(!apkFile.exists())
			throw new RuntimeException("Failed download the apk file.. File not exists "+downloadTo);


		Path p = Paths.get(downloadUrl);

		addToPropFile(APK_FILE_NAME+PROP_DELIM+p.getFileName(), true);

		closeAllResources();

	}

	public static String getHttpReqEngine(String url){

		HttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(url);

		System.out.println("Request Url : "+url);

		request.setHeader("Accept", "application/json");

		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Status Code : "+response.getStatusLine().getStatusCode());

		if(response.getStatusLine().getStatusCode()!=200)
			throw new RuntimeException("Resource Not found!");

		StringBuilder resp = new StringBuilder();

		// Get the response
		BufferedReader rd = null;
		try {
			rd = new BufferedReader
					(new InputStreamReader(response.getEntity().getContent()));
		} catch (IllegalStateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String line = "";
		try {
			while ((line = rd.readLine()) != null) {
				resp.append(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		System.out.println("Response :: "+resp);

		return resp.toString();

	}


	public static String logBuildDataRequestedByUser(String buildNo,boolean recordToProp) throws JSONException{

		///project/:username/:project/:build_num

		String url = String.format("https://circleci.com/api/v1/project/orgName/repoName/%s?circle-token=%s",buildNo,token);

		String resp = getHttpReqEngine(url);

		JSONObject jobj = new JSONObject(resp);

		//String buildNo = jobj.get("build_num").toString();

		String buildUrl = jobj.get("build_url").toString();

		String committerDate = jobj.get("committer_date").toString();

		String committer_name = jobj.get("committer_name").toString();

		String subject = jobj.get("subject").toString();

		String reponame = jobj.get("reponame").toString();

		if(recordToProp){
			addToPropFile(BUILD_NO+PROP_DELIM+buildNo, false);
			addToPropFile(BUILD_URL+PROP_DELIM+buildUrl, true);
			addToPropFile(COMMITTER_DATE+PROP_DELIM+committerDate, true);
			addToPropFile(COMMITTER_NAME+PROP_DELIM+committer_name, true);
			addToPropFile(SUBJECT+PROP_DELIM+subject, true);
			addToPropFile(REPONAME+PROP_DELIM+reponame, true);
		}


		return buildNo;


	}

	public static String getCircleCIBuildNo(boolean recordToProp) throws JSONException{

		String url = String.format("https://circleci.com/api/v1/project/orgName/repoName?circle-token=%s&limit=1&filter=successful", token);

		String resp = getHttpReqEngine(url);

		JSONArray jsonArray  = new JSONArray(resp);

		JSONObject jobj = jsonArray.getJSONObject(0);

		String buildNo = jobj.get("build_num").toString();

		String buildUrl = jobj.get("build_url").toString();

		String committerDate = jobj.get("committer_date").toString();

		String committer_name = jobj.get("committer_name").toString();

		String subject = jobj.get("subject").toString();

		String reponame = jobj.get("reponame").toString();

		if(recordToProp){
			addToPropFile(BUILD_NO+PROP_DELIM+buildNo, false);
			addToPropFile(BUILD_URL+PROP_DELIM+buildUrl, true);
			addToPropFile(COMMITTER_DATE+PROP_DELIM+committerDate, true);
			addToPropFile(COMMITTER_NAME+PROP_DELIM+committer_name, true);
			addToPropFile(SUBJECT+PROP_DELIM+subject, true);
			addToPropFile(REPONAME+PROP_DELIM+reponame, true);
		}

		return buildNo;

	}

	public static String getBuildArtifactsResponse() throws JSONException{

		String userMentionedBuild = System.getProperty("env.userBuild");

		String buildNum = "";

		if(userMentionedBuild!=null && !userMentionedBuild.trim().isEmpty() && isValidNo(userMentionedBuild)){

			buildNum = userMentionedBuild;

			logBuildDataRequestedByUser(buildNum, true);

		}else{

			buildNum = getCircleCIBuildNo(true);

		}

		String url = String.format("https://circleci.com/api/v1/project/orgName/repoName/%s/artifacts?circle-token=%s",buildNum, token);

		return getHttpReqEngine(url);

	}


	public static String getDownloadableApkUrl(String desiredApk) throws JSONException{

		String artifactsResp = getBuildArtifactsResponse();

		JSONArray jsonArray = new JSONArray(artifactsResp);

		JSONObject jsonObj = null;

		String url = null;

		boolean buildFound = false;

		if(desiredApk!=null && desiredApk.length()>5){

			for(int i=0;i<jsonArray.length();i++){

				jsonObj = jsonArray.getJSONObject(i);

				url = jsonObj.get("url").toString();

				Path p = Paths.get(url);

				if(p.getFileName().toString().equalsIgnoreCase(desiredApk))
				{

					buildFound =true;

					break;

				}

			}

			if(!buildFound)
				throw new RuntimeException("Failed to find apk : "+desiredApk);

		}else {

			jsonObj = jsonArray.getJSONObject(0);

			url = jsonObj.get("url").toString();


		}

		return url;
	}

	static Properties properties;

	static boolean propLoaded = false;

	private static Properties getBuildPropFromFile(){

		String circleCiCodeBase = System.getProperty("env.circlecibase");

		if(circleCiCodeBase==null)
			circleCiCodeBase = System.getProperty("user.dir");

		propFile = String.format(propFile, circleCiCodeBase);

		if(!propLoaded){
			properties = new Properties();
			propLoaded=true;
			try {
				properties.load(new FileInputStream(propFile));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}



		return properties;

	}

	private static boolean isFileParsingError(){

		Properties pr = getBuildPropFromFile();

		if(pr.get(BUILD_NO)==null || pr.get(APK_FILE_NAME)==null)
			return true;

		else
			return false;

	}


	private static boolean isValidNo(String buildno){

		try{

			Integer.parseInt(buildno);

			return true;

		}catch(NumberFormatException e){

			return false;
		}

	}

	public static boolean userBuildDownload=false;

	public static boolean isDownloadNeededBasedOnUserReq() throws JSONException{

		System.out.println("File parsing error : "+isFileParsingError());

		if(!isFileParsingError()){

			Properties pr = getBuildPropFromFile();

			String buildNo = pr.get(BUILD_NO).toString();

			String apkFileName = pr.get(APK_FILE_NAME).toString();

			String apkTypes = System.getProperty("env.apkFile");

			String userMentionedBuild = System.getProperty("env.userBuild");

			String actBuildNo = "";

			if(userMentionedBuild!=null && !userMentionedBuild.trim().isEmpty() && isValidNo(userMentionedBuild)){

				System.out.println("User has mentioned the build no: "+userMentionedBuild);

				actBuildNo = userMentionedBuild;

				userBuildDownload=true;

			}else{

				actBuildNo = getCircleCIBuildNo(false);

			}

			if(!actBuildNo.equalsIgnoreCase(buildNo)){

				System.out.println(String.format("Build Missmatch, so going to download apk , propFile : %s, url : %s ", buildNo,actBuildNo));

				return true;
			}

			if(!apkFileName.equalsIgnoreCase(apkTypes)){

				System.out.println(String.format("Apk Missmatch, so going to download apk, propFile : %s, url : %s ", apkFileName,apkTypes));

				return true; 

			}


			return false;

		}else{

			return true;

		}


	}



	public static void main(String[] args) throws JSONException  {

		if(isDownloadNeededBasedOnUserReq()){

			String apkTypes = System.getProperty("env.apkFile");

			String downloadUrl = getDownloadableApkUrl(apkTypes);

			String pathToDownload = null;

			if(System.getProperty("env.circlecibase")!=null){

				pathToDownload = String.format("%s/Goibibo_prod.apk", System.getProperty("env.circlecibase"));

			}else{

				pathToDownload = "/Users/pooja/automationCode/circleCiGoIbibo/apks/Goibibo_prod.apk";
				//pathToDownload = "/Users/pavan/Desktop/Goibibo_prod.apk";

			}

			downloadAndroidBuild(downloadUrl, pathToDownload);

			System.exit(0);

		}else{

			System.out.println("Apk file is upto date!");

			String exitWithStatusCode=System.getProperty("env.exitStatusCode");

			if(exitWithStatusCode!=null && exitWithStatusCode.equalsIgnoreCase("true")){

				System.out.println("Exiting with status code!");
				
				System.exit(1);

			}

		}


	}


}

