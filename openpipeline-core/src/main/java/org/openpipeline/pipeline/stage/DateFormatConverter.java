package org.openpipeline.pipeline.stage;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;
/**
 * Converts one date format into another
 *
 */
public class DateFormatConverter extends Stage {

	private Calendar calendar;
	private DateFormat inputFormatter;
	private DateFormat outputFormatter;
	private String dateAttribute;
	private String newDateAttribute;
	private String dateInputFormat;
	private String dateOutputFormat;

	@Override
	public void processItem(Item item) throws PipelineException {

		try {
			String date = item.getRootNode().getChildValue(dateAttribute);
			if (date != null) {
				String standardDate = convert(date);

				if (standardDate != null) {
					item.getRootNode().addNode(newDateAttribute)
							.setValue(standardDate);
				}
			}
		} catch (Throwable e) {
			throw new PipelineException(e);
		}
		super.pushItemDownPipeline(item);
	}

	private String convert(String date) throws ParseException {

		String result = null;

		if ("long".equals(dateInputFormat)) {

			calendar.setTimeInMillis(Long.parseLong(date));
			result = outputFormatter.format(calendar.getTime());

		} else {

			Date input = inputFormatter.parse(date);
			result = outputFormatter.format(input);
		}
		return result;
	}

	@Override
	public void initialize() {
		dateAttribute = params.getProperty("date-attribute");
		newDateAttribute = params.getProperty("new-date-attribute");
		dateInputFormat = params.getProperty("date-input-format");
		dateOutputFormat = params.getProperty("date-output-format");

		outputFormatter = new SimpleDateFormat(dateOutputFormat);

		if (!"long".equals(dateInputFormat)) {
			inputFormatter = new SimpleDateFormat(dateInputFormat);
		} else {
			calendar = Calendar.getInstance();
		}
	}

	@Override
	public String getDescription() {
		return "Converts one date format to another.";
	}

	@Override
	public String getDisplayName() {
		return "Date Format Converter";
	}

	@Override
	public String getConfigPage() {
		return "stage_date_format_converter.jsp";
	}
}
