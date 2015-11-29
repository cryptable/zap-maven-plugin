package org.cryptable.zap.mavenplugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;


/**
 * Goal which touches a timestamp file.
 */
@SuppressWarnings("restriction")
@Mojo( name = "process-zap", 
       defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, 
       threadSafe = true )
public class ProcessMojo extends AbstractMojo {

    private ClientApi zapClientAPI;
    private Proxy proxy;

    /**
     * Location of the host of the ZAP proxy
     */
    @Parameter( defaultValue = "localhost", 
    		    required = true)
    private String zapProxyHost;

    /**
     * Location of the host of the ZAP proxy
     */
    @Parameter( required = true)
    private String apiKEY;
    
    /**
     * Location of the port of the ZAP proxy
     */
    @Parameter( defaultValue = "8080", 
		        required = true)
    private int zapProxyPort;

    /**
     * Location of the port of the ZAP proxy
     */
    @Parameter( required=true )
    private String targetURL;

    /**
     * Switch to spider the URL
     */
    @Parameter( defaultValue="true" )
    private boolean spiderURL;

    /**
     * Switch to scan the URL
     */
    @Parameter( defaultValue="true" )
    private boolean scanURL;

    /**
     * Save session of scan
     */
    @Parameter( defaultValue="true" )
    private boolean saveSession;

    /**
     * Switch to shutdown ZAP
     */
    @Parameter( defaultValue="true" )
    private boolean shutdownZAP;

    /**
     * Save session of scan
     */
    @Parameter( defaultValue="true" )
    private boolean reportAlerts;

    /**
     * Location to store the ZAP reports
     */
    @Parameter( defaultValue="${project.build.directory}/zap-reports" )
    private String reportsDirectory;

    /**
     * Reports filename without an extension, extension determined during format
     */
    @Parameter( required=false)
    private String reportsFilenameNoExtension;
    
    /**
     * Set the output format type, in addition to the XML report. Must be one of "none" or "json".
     */
    @Parameter ( defaultValue="none" )
    private String format;

    /**
     * Location of the host of the ZAP proxy
     */
    @Parameter( required = false )
    private String finalizeScript;

    /**
     * A flag to create a jsonp JSON file. It assing the json body to zaproxy_jsonpData variable. Makes only sense when using json data.
     */
    @Parameter( defaultValue="false" )
    private boolean jsonp;

    /**
     * create a Timestamp
     * 
     * @return
     */
    private String dateTimeString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(cal.getTime());
    }

    /**
     * create a temporary filename
     * 
     * @param prefix
     *            if null, then default "temp"
     * @param suffix
     *            if null, then default ".tmp"
     * @return
     */
    private String createTempFilename(String prefix, String suffix) {
        StringBuilder sb = new StringBuilder("");
        if (prefix != null)
            sb.append(prefix);
        else
            sb.append("temp");

        // append date time and random UUID 
        sb.append(dateTimeString()).append("_").append(UUID.randomUUID().toString());

        if (suffix != null)
            sb.append(suffix);
        else
            sb.append(".tmp");

        return sb.toString();
    }

    /**
     * Change the ZAP API status response to an integer
     *
     * @param response the ZAP APIresponse code
     * @return
     */
    private int statusToInt(ApiResponse response) {
        return Integer.parseInt(((ApiResponseElement)response).getValue());
    }

    /**
     * Search for all links and pages on the URL
     *
     * @param url the to investigate URL
     * @throws ClientApiException
     */
    private void spiderURL(String url) throws ClientApiException {
        zapClientAPI.spider.scan(apiKEY, url, "10", "");

        while ( statusToInt(zapClientAPI.spider.status("scanid")) < 100) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                getLog().error(e.toString());
            }
        }
    }

    /**
     * Scan all pages found at url
     *
     * @param url the url to scan
     * @throws ClientApiException
     */
    private void scanURL(String url) throws ClientApiException {
        zapClientAPI.ascan.scan(apiKEY, url, "true", "false", "", "", "");

        while ( statusToInt(zapClientAPI.ascan.status("scanid")) < 100) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                getLog().error(e.toString());
            }
        }
    }

    /**
     * Get all alerts from ZAP proxy
     *
     * @param format specification. It can be : 'xml', 'json', 'html'
     * @return  all alerts from ZAProxy
     * @throws Exception
     */
    private String getAllAlerts(String format) throws Exception {
        URL url;
        String result = "";

        if (format.equalsIgnoreCase("xml") || 
        	format.equalsIgnoreCase("html") ||
        	format.equalsIgnoreCase("json")) {
            url = new URL("http://zap/" + format + "/core/view/alerts");
        } else {
            url = new URL("http://zap/xml/core/view/alerts");
        }

        getLog().info("Open URL: " + url.toString());

        HttpURLConnection uc = (HttpURLConnection) url.openConnection(proxy);
        uc.connect();

        BufferedReader in = new BufferedReader(new InputStreamReader(
                uc.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            result = result + inputLine;
        }

        in.close();
        return result;

    }
    
    /**
     * Get all alerts from ZAP proxy
     *
     * @param format specification. It can be : 'xml', 'json', 'html'
     * @return  all alerts from ZAProxy
     * @throws Exception
     */
    private String getAllAlertsFormat(String format) throws Exception {

    	if (format.equalsIgnoreCase("xml") || 
        	format.equalsIgnoreCase("html") ||
        	format.equalsIgnoreCase("json")) {
            return format;
        } else {
            return "xml";
        }
    	
    }

    /**
     * Property file to update for proxy settings
     */
    @Parameter(alias = "property.file", required = false)
    private String propertyFile;
    
    /**
     * Proxy port setting in the property file
     */
    @Parameter(alias = "property.file.proxy.host", required = false)
    private String propertyFileProxyHost;
    
    /**
     * Proxy host setting in the property file
     */
    @Parameter(alias = "property.file.proxy.port", required = false)
    private String propertyFileProxyPort;
    
    /**
     * 
     * This method overwrites the super class implementation.
     * @throws IOException Throws exception when file access to property file failes
     */
    private void restoreProperties() throws IOException {
        Properties properties = new Properties();
        FileInputStream fi = new FileInputStream(propertyFile);
        properties.load(fi);
        fi.close();
        if ((properties.getProperty("zapperdepap" + propertyFileProxyHost) == null) || properties.getProperty("zapperdepap" + propertyFileProxyHost).isEmpty()) {
            getLog().info("Delete Propery [" + propertyFileProxyHost + "]");
            properties.remove(propertyFileProxyHost);
            getLog().info("Delete Propery [" + propertyFileProxyPort + "]");            
            properties.remove(propertyFileProxyPort);            
        }
        else {
            getLog().info("Restore Propery [" + propertyFileProxyHost + "] with value [" + properties.getProperty(propertyFileProxyHost) + "] to [" + properties.getProperty("zapperdepap" + propertyFileProxyHost) + "]");
            properties.setProperty(propertyFileProxyHost, properties.getProperty("zapperdepap" + propertyFileProxyHost));
            properties.remove("zapperdepap" + propertyFileProxyHost);
            getLog().info("Restore Propery [" + propertyFileProxyPort + "] with value [" + properties.getProperty(propertyFileProxyPort) + "] to [" + properties.getProperty("zapperdepap" + propertyFileProxyPort) + "]");
            properties.setProperty(propertyFileProxyPort, properties.getProperty("zapperdepap" + propertyFileProxyPort));
            properties.remove("zapperdepap" + propertyFileProxyPort);
        }
        
        FileOutputStream fo = new FileOutputStream(propertyFile);
        properties.store(fo, null);
        fo.close();
    }

    /**
     *
     * Execute the whole shabang
     * @throws MojoExecutionException Throws exception when ZAProxy fails
     */
    public void execute() throws MojoExecutionException {
    	System.setProperty("java.net.debug", "all");

        try {

            zapClientAPI = new ClientApi(zapProxyHost, zapProxyPort);
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(zapProxyHost, zapProxyPort));

            if (spiderURL) {
                getLog().info("Spider the site [" + targetURL + "]");
                spiderURL(targetURL);
            } else {
                getLog().info("skip spidering the site [" + targetURL + "]");
            }

            if (scanURL) {
                getLog().info("Scan the site [" + targetURL + "]");
                scanURL(targetURL);
            } else {
                getLog().info("skip scanning the site [" + targetURL + "]");
            }

            // filename to share between the session file and the report file
            String fileName = "";
            if (saveSession) {

                fileName = createTempFilename("ZAP", "");

                zapClientAPI.core.saveSession(fileName, "true","");
            } else {
                getLog().info("skip saveSession");
            }

            if (reportAlerts) {

                // reuse fileName of the session file
                if ((reportsFilenameNoExtension == null) || reportsFilenameNoExtension.isEmpty()) {
                    if ((fileName == null) || (fileName.length() == 0)) {
                        fileName = createTempFilename("ZAP", "");
                    }                    
                }
                else {
                    fileName = reportsFilenameNoExtension;
                }

                String fileName_no_extension = FilenameUtils.concat(reportsDirectory, fileName);

                try {
                    String alerts = getAllAlerts(getAllAlertsFormat(format));
                    String fullFileName = fileName_no_extension + "." + getAllAlertsFormat(format);
                    FileUtils.writeStringToFile(new File(fullFileName), (jsonp?"var zaproxy_jsonpData = ":"") + alerts);
                    getLog().info("File save in format in ["+getAllAlertsFormat(format)+"]");
                    if (format.equals("json")) {
                        Utils.copyResourcesRecursively(getClass().getResource("/zap-reports/"), new File(reportsDirectory));
                    }
                } catch (Exception e) {
                    getLog().error(e.toString());
                    e.printStackTrace();
                }
            }

            if ((propertyFile != null) && !propertyFile.isEmpty()) {
                restoreProperties();
            }

        } catch (Exception e) {
            getLog().error(e.toString());
            throw new MojoExecutionException("Processing with ZAP failed");
        } finally {
            if (shutdownZAP && (zapClientAPI != null)) {
                try {
                    getLog().info("Shutdown ZAProxy");
                    zapClientAPI.core.shutdown(apiKEY);
                } catch (Exception e) {
                    getLog().error(e.toString());
                    e.printStackTrace();
                }
            } else {
                getLog().info("No shutdown of ZAP");
            }
        }
    }

}
