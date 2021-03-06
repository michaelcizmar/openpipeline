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
package org.openpipeline.pipeline.stage.opencalais;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 2.2.1
 * Tue May 05 12:59:21 CDT 2009
 * Generated source version: 2.2.1
 * 
 */
 
@WebService(targetNamespace = "http://clearforest.com/", name = "calaisHttpPost")
@XmlSeeAlso({ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface CalaisHttpPost {

    @WebResult(name = "string", targetNamespace = "http://clearforest.com/", partName = "Body")
    @WebMethod(operationName = "Enlighten")
    public java.lang.String enlighten(
        @WebParam(partName = "licenseID", name = "licenseID", targetNamespace = "")
        java.lang.String licenseID,
        @WebParam(partName = "content", name = "content", targetNamespace = "")
        java.lang.String content,
        @WebParam(partName = "paramsXML", name = "paramsXML", targetNamespace = "")
        java.lang.String paramsXML
    );
}
