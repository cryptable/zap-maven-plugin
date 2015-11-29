package org.cryptable.zap.mavenplugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.zaproxy.clientapi.core.ClientApi;

/**
 * Goal which will start ZAP proxy.
 */
@SuppressWarnings("restriction")
@Mojo(name = "start-zap", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class StartMojo extends AbstractMojo {
    
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
     * Location of the host of the ZAP proxy
     */
    @Parameter( required = true)
    private String apiKEY;
    
    /**
     * Location of the ZAProxy program.
     */
    @Parameter(required = false)
    private String preparationScript = null;

    /**
     * Location of the ZAProxy program.
     */
    @Parameter(required = true)
    private String zapProgram;

    /**
     * Location of the host of the ZAP proxy
     */
    @Parameter(defaultValue = "localhost", required = true)
    private String zapProxyHost;

    /**
     * Location of the port of the ZAP proxy
     */
    @Parameter(defaultValue = "8080", required = true)
    private int zapProxyPort;

    /**
     * New session when you don't want to start ZAProxy.
     */
    @Parameter(defaultValue = "false")
    private boolean newSession;

    /**
     * Sleep to wait to start ZAProxy
     */
    @Parameter(defaultValue = "20000")
    private int zapSleep;

    @Parameter(defaultValue = "false")
    private boolean daemon;

    private void changeProperties() throws IOException {
        Properties properties = new Properties();
        FileInputStream fi = new FileInputStream(propertyFile);
        properties.load(fi);
        fi.close();
        if ((properties.getProperty(propertyFileProxyHost) == null) || properties.getProperty(propertyFileProxyHost).isEmpty()) {
            getLog().info("Set Propery [" + propertyFileProxyHost + "] to [" + zapProxyHost + "]");
            properties.setProperty(propertyFileProxyHost, zapProxyHost);
            getLog().info("Set Propery [" + propertyFileProxyPort + "] to [" + zapProxyPort + "]");            
            properties.setProperty(propertyFileProxyPort, String.valueOf(zapProxyPort));            
        }
        else {
            getLog().info("Change Propery [" + propertyFileProxyHost + "] with value [" + properties.getProperty(propertyFileProxyHost) + "] to [" + zapProxyHost + "]");
            properties.setProperty("zapperdepap" + propertyFileProxyHost, properties.getProperty(propertyFileProxyHost));
            properties.setProperty(propertyFileProxyHost, zapProxyHost);
            getLog().info("Change Propery [" + properties.getProperty(propertyFileProxyPort) + "] to [" + zapProxyPort + "]");
            properties.setProperty("zapperdepap" + propertyFileProxyPort, properties.getProperty(propertyFileProxyPort));
            properties.setProperty(propertyFileProxyPort, String.valueOf(zapProxyPort));            
        }
        FileOutputStream fo = new FileOutputStream(propertyFile);
        properties.store(fo, null);
        fo.close();
    }
    
    public void execute() throws MojoExecutionException {

        try {

            if ((propertyFile != null) && (!propertyFile.isEmpty())) {
                changeProperties();
            }

            if (newSession) {
                ClientApi zapClient = new ClientApi(zapProxyHost, zapProxyPort);
                File tempFile = File.createTempFile("ZAP", null);
                getLog().info("Create Session with temporary file [" + tempFile.getPath() + "]");
                zapClient.core.newSession(apiKEY, tempFile.getPath(), "true");
            } else {
                File pf = new File(zapProgram);
                Runtime runtime = java.lang.Runtime.getRuntime();
                getLog().info("Start ZAProxy [" + zapProgram + "]");
                getLog().info("Using working directory [" + pf.getParentFile().getPath() + "]");
                String[] command = { zapProgram, "" };
                if (daemon) {
                    command[1] = "-daemon";
                }
                final Process ps = runtime.exec(command, null, pf.getParentFile());

                // Consommation de la sortie standard de l'application externe dans un Thread separe
                new Thread() {
                    public void run() {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
                            String line = "";
                            try {
                                while ((line = reader.readLine()) != null) {
                                    // Traitement du flux de sortie de l'application si besoin est
                                    getLog().info(line);
                                }
                            } finally {
                                reader.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

                // Consommation de la sortie d'erreur de l'application externe dans un Thread separe
                new Thread() {
                    public void run() {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
                            String line = "";
                            try {
                                while ((line = reader.readLine()) != null) {
                                    // Traitement du flux d'erreur de l'application si besoin est
                                    getLog().info(line);
                                }
                            } finally {
                                reader.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

            }
            Thread.currentThread();
            Thread.sleep(zapSleep);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unable to start ZAP [" + zapProgram + "]");
        }

    }
}
