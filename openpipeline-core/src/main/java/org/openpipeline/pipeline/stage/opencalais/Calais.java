/*******************************************************************************
 * Copyright 2010 Dieselpoint, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 */

package org.openpipeline.pipeline.stage.opencalais;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "calais", 
                  wsdlLocation = "http://api.opencalais.com/enlighten/?wsdl",
                  targetNamespace = "http://clearforest.com/") 
public class Calais extends Service {

    public final static URL WSDL_LOCATION;
    public final static QName SERVICE = new QName("http://clearforest.com/", "calais");
    public final static QName CalaisHttpGet = new QName("http://clearforest.com/", "calaisHttpGet");
    public final static QName CalaisSoap12 = new QName("http://clearforest.com/", "calaisSoap12");
    public final static QName CalaisSoap = new QName("http://clearforest.com/", "calaisSoap");
    public final static QName CalaisHttpPost = new QName("http://clearforest.com/", "calaisHttpPost");
    static {
        URL url = null;
        try {
            url = new URL("http://api.opencalais.com/enlighten/?wsdl");
        } catch (MalformedURLException e) {
            System.err.println("Can not initialize the default wsdl from http://api.opencalais.com/enlighten/?wsdl");
            // e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public Calais(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public Calais(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Calais() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     * 
     * @return
     *     returns CalaisHttpGet
     */
    @WebEndpoint(name = "calaisHttpGet")
    public CalaisHttpGet getCalaisHttpGet() {
        return super.getPort(CalaisHttpGet, CalaisHttpGet.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CalaisHttpGet
     */
    @WebEndpoint(name = "calaisHttpGet")
    public CalaisHttpGet getCalaisHttpGet(WebServiceFeature... features) {
        return super.getPort(CalaisHttpGet, CalaisHttpGet.class, features);
    }
    /**
     * 
     * @return
     *     returns CalaisSoap
     */
    @WebEndpoint(name = "calaisSoap12")
    public CalaisSoap getCalaisSoap12() {
        return super.getPort(CalaisSoap12, CalaisSoap.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CalaisSoap
     */
    @WebEndpoint(name = "calaisSoap12")
    public CalaisSoap getCalaisSoap12(WebServiceFeature... features) {
        return super.getPort(CalaisSoap12, CalaisSoap.class, features);
    }
    /**
     * 
     * @return
     *     returns CalaisSoap
     */
    @WebEndpoint(name = "calaisSoap")
    public CalaisSoap getCalaisSoap() {
        return super.getPort(CalaisSoap, CalaisSoap.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CalaisSoap
     */
    @WebEndpoint(name = "calaisSoap")
    public CalaisSoap getCalaisSoap(WebServiceFeature... features) {
        return super.getPort(CalaisSoap, CalaisSoap.class, features);
    }
    /**
     * 
     * @return
     *     returns CalaisHttpPost
     */
    @WebEndpoint(name = "calaisHttpPost")
    public CalaisHttpPost getCalaisHttpPost() {
        return super.getPort(CalaisHttpPost, CalaisHttpPost.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CalaisHttpPost
     */
    @WebEndpoint(name = "calaisHttpPost")
    public CalaisHttpPost getCalaisHttpPost(WebServiceFeature... features) {
        return super.getPort(CalaisHttpPost, CalaisHttpPost.class, features);
    }

}
