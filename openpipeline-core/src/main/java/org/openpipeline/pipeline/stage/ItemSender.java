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
package org.openpipeline.pipeline.stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.ByteArray;
import org.openpipeline.util.FastStringBuffer;

/**
 * Use this class to send items to one or more remote machines for further processing.
 * To use this stage, ensure that 
 * {@link org.openpipeline.pipeline.connector.ItemReceiverConnector} instance(s) are 
 * running on the remote machines.
 */
public class ItemSender extends Stage {
	
	private FastStringBuffer charBuf = new FastStringBuffer();
	private ByteArray byteBuf = new ByteArray();
	private SizedByteArrayEntity byteEntity = new SizedByteArrayEntity();
	private String [] addresses;
	private DefaultHttpClient client;
	


	@Override
	public void processItem(Item item) throws PipelineException {

		charBuf.clear();
		item.appendXMLtoBuffer(charBuf, true);
		
		byteBuf.clear();
		byteBuf.appendModifiedUTF(charBuf.getArray(), 0, charBuf.size(), false);
		
		byteEntity.setBuf(byteBuf);
		
		// send the item to all the remote machines in the list
		for (String addr: addresses) {
			
			// the address should look like this:
			// http://hostname:port/servletname
			// typically http://myhost:8080/rest
			
			try {
				sendItem(item, addr);
			} catch (Exception e) {
				// a checked exception should be handled as a failure for
				// this address only. An unchecked exception should stop
				// sends to all addresses
				super.error("Error sending item from ItemSender to " + addr, e);
			}
		}
		
		super.pushItemDownPipeline(item);
	}

	
	
	/**
	 * Send the item to the address.
	 */
	private void sendItem(Item item, String addr) throws ClientProtocolException, IOException {
		HttpPost httpPost = new HttpPost(addr);
		httpPost.setEntity(byteEntity);
		
		HttpResponse response = client.execute(httpPost);
		HttpEntity resEnt = response.getEntity();

		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() != 200) {
			String content = getContent(resEnt);
			super.error("Failed to send itemId " + item.getItemId() + 
					" to " + addr + " http code=" + status.getStatusCode() + 
					" " + status.getReasonPhrase() +
					" message=" + content);
		}
		
		if (resEnt != null) {
			resEnt.consumeContent();
		}
	}


	private String getContent(HttpEntity resEnt) throws IllegalStateException, IOException {
		if (resEnt == null) {
			return "";
		}
		ByteArray arr = new ByteArray();
		arr.append(resEnt.getContent());
		return arr.getModifiedUTF();
	}


	@Override
	public void initialize() {
		List<String> addrs = super.params.getValues("server_addresses");
		addresses = new String[addrs.size()];
		addrs.toArray(addresses);
		
		client = new DefaultHttpClient();
	    client.getParams().setParameter("http.useragent", "OpenPipeline ItemSender");
	    
        byteEntity.setContentType("binary/octet-stream");
	}

	@Override
	public String getConfigPage() {
		return "stage_item_sender.jsp";
	}
	
	@Override
	public String getDescription() {
		return "Sends item to one or more remote machines.";
	}

	@Override
	public String getDisplayName() {
		return "Item Sender";
	}


	@Override
	public void close() throws PipelineException {
		super.close();
        client.getConnectionManager().shutdown(); 
	}

	/**
	 * ByteArrayEntity only takes a byte[] by default, and
	 * not a length. This one is sized.
	 */
	class SizedByteArrayEntity extends AbstractHttpEntity implements Cloneable {
		private ByteArray buf;

	    public void setBuf(ByteArray buf) {
	    	this.buf = buf;
	    }

	    public boolean isRepeatable() {
	        return true;
	    }

	    public long getContentLength() {
	    	return buf.size();
	    }

	    public InputStream getContent() {
	        return new ByteArrayInputStream(buf.getArray(), 0, buf.size());
	    }
	    
	    public void writeTo(final OutputStream outstream) throws IOException {
	        if (outstream == null) {
	            throw new IllegalArgumentException("Output stream may not be null");
	        }
	        outstream.write(buf.getArray(), 0, buf.size());
	        outstream.flush();
	    }

	    public boolean isStreaming() {
	        return false;
	    }

	    public Object clone() throws CloneNotSupportedException {
	        return super.clone();
	    }
		
		
	}
	
	
	
}


