package org.openpipeline.pipeline.connector.webservice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.FastStringBuffer;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

/**
 * Connector for Drupal services using JSON and REST
 */
public class DrupalServicesConnector extends Connector {
	
	// TODO don't use AWS json parser. switch to jackson or gson. jackson is faster
	// also, add importJSON() to Item and use that
	// Add a rate limiter to so we don't hit a server too hard

	private static final String DATA_FIELD = "data";
	private static final String ERROR_FIELD = "error";

	private Item item = new Item();
	private FastStringBuffer buf = new FastStringBuffer();
	
	private URL url;
	private String method;
	private int docsProcessed = 0;

	@Override
	public void execute() {

		super.getLogger().info("Starting " + this.getShortName() + "...");
		super.setLastMessage("Running");

		try {
			int page = 0;
			while (true) {

				HttpURLConnection con = (HttpURLConnection) url
						.openConnection();
				con.setRequestMethod("POST");
				con.setDoOutput(true);
				con.setDoInput(true);

				String content = "method="
						+ URLEncoder.encode(method, "UTF-8") + "&page=" + Integer.toString(page);
				page++;

				DataOutputStream out = new DataOutputStream(
						con.getOutputStream());
				out.writeBytes(content);

				InputStream in = con.getInputStream();

				try {
					if (!processData(in)) {
						break;
					}
				} finally {
					in.close();
				}
				
				super.getLogger().info(this.getShortName() + " docs processed:" + docsProcessed);

				if (super.getInterrupted()) {
					break;
				}
			}
			String msg = "Finished. Total docs processed: " + docsProcessed;
			super.getLogger().info(msg);
			super.setLastMessage(msg);

		} catch (Throwable e) {
			super.error(e.getMessage(), e);
			super.setLastMessage("Error: " + e.toString());
		}

	}

	public boolean processData(InputStream in) throws IOException,
			PipelineException, JSONException {

		buf.clear();
		buf.append(new InputStreamReader(in, "UTF-8"));
		JSONObject jsonObject = new JSONObject(buf.toString());

		boolean error = (Boolean) jsonObject.get(ERROR_FIELD);
		if (error) {
			// no data returned
			throw new PipelineException("Error retrieving data. "
					+ jsonObject.getString(ERROR_FIELD));
		}

		JSONArray documents = (JSONArray) jsonObject.get(DATA_FIELD);
		if (documents.length() == 0) {
			return false;
		}

		for (int i = 0; i < documents.length(); i++) {
			
			// Read one document. 
			item.clear();
			JSONObject doc = (JSONObject) documents.get(i);
			
			String id = doc.getString("id");
			if (id != null && id.length() > 0) {
				item.setItemId(id);
			}
			
			addValue(item.getRootNode(), doc);

			// Push the item into the pipeline
			super.getStageList().processItem(item);

			docsProcessed++;
		}
		return true;
	}


	/**
	 * Add this object to the node. It could be a hash, an array, or a string.
	 * @throws JSONException 
	 */
	private void addValue(Node node, Object value) throws JSONException {
			
		// if it's a hash
		if (value instanceof JSONObject) {
			JSONObject hash = (JSONObject) value;
			String[] attrIds = JSONObject.getNames(hash);
			
			for (String attrId: attrIds) {
				Object subValue = hash.get(attrId);
				
				// normalize the attribute name to get rid of bad chars
				String fixedAttrId = attrId.replaceAll("[^\\w]", "_");
				
				Node subNode = node.addNode(fixedAttrId);
				addValue(subNode, subValue);
			}
			
		} else if (value instanceof JSONArray) {
			
			// array elements do not have names. just add them
			// as multi-valued attributes to the main node. this
			// doesn't work very well for cascading arrays
			JSONArray array = (JSONArray) value;
			for (int i = 0; i < array.length(); i++) {
				Object subValue = array.get(i);
				addValue(node, subValue);
			}
			
		} else {
			// it's just a value
			node.setValue(value.toString());
		}

	}
	
	
	@Override
	public void initialize() throws PipelineException {

		super.initialize();

		// String username = super.getParams().getProperty("username");
		// String password = super.getParams().getProperty("password");
		String webserviceUrl = super.getParams().getProperty("webservice-url");
		method = super.getParams().getProperty("method");

		try {
			url = new URL(webserviceUrl);
		} catch (Throwable e) {
			throw new PipelineException(e);
		}
	}

	public String getDescription() {
		return "Crawls a Drupal server using Drupal services with JSON and REST";
	}

	public String getDisplayName() {
		return "Drupal Services Connector";
	}

	public String getLastMessage() {
		String msg = super.getLastMessage();
		if (docsProcessed > 0) {
			msg += "/docs processed=" + docsProcessed;
		}
		return msg;
	}

	public String getPageName() {
		return "connector_drupal_services.jsp";
	}

	public String getShortName() {
		return "DrupalServicesConnector";
	}

	public String getLogLink() {
		return "log_viewer.jsp";
	}

}
