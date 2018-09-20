package com.datafest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {

	//folder containing recognized pages
	private static final String RECOGNIZED_PAGES_FOLDER = "C:\\Users\\ssaganowski\\Desktop\\text_auto";
	//SQL params
	private static final String DB_NAME = "DataFest";
	private static final String TABLE_NAME = "wines";
	
	public static void main(String[] args) {

		File dir = new File(RECOGNIZED_PAGES_FOLDER);
		File[] pages = null;

		//list all files
		if (!dir.exists()) {
			System.err.println("Missing \"" + RECOGNIZED_PAGES_FOLDER + "\" folder");
		} else {
			pages = dir.listFiles();
		}
		
		//go through all files
		if (pages != null) {
			System.out.println("Inserting " + pages.length + " pages.");
			for (int i = 0; i < pages.length; i++) {
				parseSinglePage(pages[i]);
			}
		}
	}
	
	/**
	 * Parse single page:
	 * -look for potential records using regex, we assume format of "digits (id) text price1 price2"
	 * -parse each record and extract info
	 * -insert results to SQL
	 * 
	 * Next steps:
	 * -trim the records from the raw text
	 * -look for records without id
	 * -check the remaining text if it is header (e.g. Wines of Spain)
	 * -use header to extract more info (country, color, year, etc.)
	 * 
	 * @param page
	 */
	private static void parseSinglePage(File page) {
		System.out.println("Parsing file: " + page.getName());
		//try {
			
			//load text
			String pageText = null;
			try {
				pageText = new String(Files.readAllBytes(Paths.get(page.getAbsolutePath())), StandardCharsets.UTF_8);
			} catch (IOException e) {
				System.out.println("Couldn't load file: " + page.getName());
				e.printStackTrace();
			}
			//System.out.println(pageText);
			
			//look for wine records, we assume they start with number (id) and end with two prices
			Pattern recordPattern = Pattern.compile("^(\\d+.{5,}?\\n?.*\\d{1,3}[.,_](\\d\\d|\\d\\S|\\S\\d)\\s+.{0,5}\\d{1,3}[.,_](\\d\\d|\\d\\S|\\S\\d))", Pattern.MULTILINE | Pattern.UNIX_LINES);
			Matcher recordMatcher = recordPattern.matcher(pageText);
			
			Set<Record> records = new LinkedHashSet<>();
			int count = 0;
			while (recordMatcher.find()) {

				Record record = new Record();
				String matchedText = recordMatcher.group(1);
				
				//if record has two lines it may contain header - check for single line match
				//FIXME: sometimes the record fits but shouldn't be trimmed! look for better solution
				if (matchedText.contains("\n")) {
					record.setLines(2);
					/*Pattern singleLinePattern = Pattern.compile("^(\\d+.*\\d{1,3}[.,_](\\d\\d|\\d\\S|\\S\\d)\\s+.{0,5}\\d{1,3}[.,_](\\d\\d|\\d\\S|\\S\\d))", Pattern.MULTILINE | Pattern.UNIX_LINES);
					Matcher singleLineMatcher = singleLinePattern.matcher(matchedText);
					if (singleLineMatcher.find()) {
						matchedText = singleLineMatcher.group(1);
						record.setComment(record.getComment() + "[cut to 1 line]");
					}*/
				} else {
					record.setLines(1);
				}
				
				count++;
				record.setRawText(matchedText);
				//clean the record - remove dots before prices and new lines
				record.setCleanedText(cleanRecord(matchedText));
				records.add(record);
				//System.out.println(m.group(1));
				//System.out.println(record.getCleanedText());
				
				//TODO: save meta info: start and end position in the page, does it have year, color and other info?
			}
			
			System.out.println("Wines found: " + count);
			
			//iterate over all records and extract info
			for(Record record : records) {
				try {
					record = (Record) parseRecord(record).clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}

			//put data to SQL
			insertToSQL(records, page.getName().replaceFirst(".txt", ""));
	}
	
	
	/**
	 * Parse record. Get raw text from the getRawText().
	 * Extract as much info as you can: id, prices, year, color, name, etc.
	 * 
	 * @param record Record that should be parsed.
	 * @return Record populated with parsed info
	 */
	private static Record parseRecord(Record record) {
		String recordTxt = record.getCleanedText();
		
		//get id
		String idTxt = recordTxt.substring(0, recordTxt.indexOf(" "));
		long id = -1;
		try {
			id = Long.parseLong(idTxt);
		} catch (NumberFormatException e) {
			//TODO: go over the number, digit by digit and try to correct errors, or use machine learning :)
			record.setComment(record.getComment() + "[raw id: " + idTxt + "]");
		}
		record.setId(id);
		//trim id
		recordTxt = recordTxt.substring(recordTxt.indexOf(" ") + 1);
		
		//get case price with regex
		Pattern casePricePattern = Pattern.compile("(\\d{1,3}[.,_](\\d\\d|\\d\\S|\\S\\d)\\s*$)");
		Matcher casePriceMatcher = casePricePattern.matcher(recordTxt);
		String casePriceTxt = "";
		float casePrice = 0f;
		while (casePriceMatcher.find()) {
			casePriceTxt = casePriceMatcher.group(1);
		}
		//try to parse it to float
		try {
			casePrice = Float.parseFloat(casePriceTxt);
		} catch (NumberFormatException e) {
			//TODO: go over the price, digit by digit and try to correct errors, or use machine learning :)
			record.setComment(record.getComment() + "[raw casePrice: " + casePriceTxt + "]");
		}
		record.setCasePrice(casePrice);
		//trim case price
		recordTxt = recordTxt.substring(0, recordTxt.lastIndexOf(casePriceTxt));
		
		//get per price with regex
		Pattern perPricePattern = Pattern.compile("(\\d{1,3}[.,_](\\d\\d|\\d\\S|\\S\\d)\\s*.{0,5}$)");
		Matcher perPriceMatcher = perPricePattern.matcher(recordTxt);
		String perPriceTxt = "";
		float perPrice = 0f;
		while (perPriceMatcher.find()) {
			perPriceTxt = perPriceMatcher.group(1);
		}
		//try to parse it to float
		try {
			perPrice = Float.parseFloat(perPriceTxt);
		} catch (NumberFormatException e) {
			//TODO: go over the price, digit by digit and try to correct errors, or use machine learning :)
			record.setComment(record.getComment() + "[raw perPrice: " + perPriceTxt + "]");
		}
		record.setPerPrice(perPrice);
		//trim per price
		recordTxt = recordTxt.substring(0, recordTxt.lastIndexOf(perPriceTxt));
		
		//get year - look for number 18xx lub 19xx
		Pattern yearPattern = Pattern.compile("(1[8|9]\\d\\d)");
		Matcher yearMatcher = yearPattern.matcher(recordTxt);
		String year = "0";
		while (yearMatcher.find()) {
			year = yearMatcher.group(1);
		}
		record.setYear(Integer.parseInt(year));
		//trim year
		if (!year.equals(0)) {
			recordTxt = recordTxt.replaceFirst(year, "");
		}
		
		//get color - look for key-words: 
		Pattern colorPattern = Pattern.compile("(blanc|rouge|rose|red|white|pink)");
		Matcher colorMatcher = colorPattern.matcher(recordTxt.toLowerCase());
		String color = "";
		while (colorMatcher.find()) {
			color = colorMatcher.group(1);
			record.setType("wine");
		}
		if (color.equals("blanc") || color.equals("white")) {
			record.setColor("white");
		} else if (color.equals("rose") || color.equals("pink")) {
			record.setColor("rose");
		} else if (color.equals("red") || color.equals("rouge")) {
			record.setColor("red");
		} else {
			record.setColor("");
		}
		
		//get anything that is in brackets, put it in comment
		Pattern bracketsPattern = Pattern.compile("([\\[\\(].*?[\\]\\)])");
		Matcher bracketsMatcher = bracketsPattern.matcher(recordTxt);
		String bracketsTxt = "";
		while (bracketsMatcher.find()) {
			bracketsTxt = bracketsMatcher.group(1);
			record.setComment(record.getComment() + "[bracket: " + bracketsTxt + "]");
		}
		//trim bracket content
		recordTxt = recordTxt.replace(bracketsTxt, "");
		
		//put rest as a name
		record.setName(cleanName(recordTxt));

		//System.out.println(recordTxt);
		
		//TODO: get section
		//TODO: get color from section
		//TODO: create dictionary of names to look for typos
		//get other info
		
		return record;
	}
	
	/**
	 * Insert records to SQL table.
	 * 
	 * @param records Records to insert.
	 * @param pageFileName File name of the page the records come from.
	 */
	private static void insertToSQL(Set<Record> records, String pageFileName) {
		Connection connection;
		PreparedStatement pst = null;
		try {
			connection = connectWithSQL();
		
			pst = connection.prepareStatement("INSERT INTO " + TABLE_NAME + 
					//page_id
					" (page_id, "	//1
					+ "xy, "		//2
					+ "type, "		//3
					+ "wine_type, "	//4
					+ "color, "		//5
					+ "country, "	//6
					+ "name, "		//7
					+ "section, "	//8
					+ "vintage, "	//9
					+ "bottle_type, "	//10
					+ "perprice, "		//11
					+ "caseprice, "		//12
					+ "id, "			//13
					+ "comment, "		//14
					+ "raw_record, "	//15
					+ "cleaned_record)"	//16
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			System.out.println("Error connecting to SQL");
			e.printStackTrace();
		}
		
		try {
			for(Record record : records) {
			
				pst.setString(1, pageFileName);
				pst.setNull(2, Types.NULL);
				pst.setString(3, record.getType());
				pst.setNull(4, Types.NULL);
				pst.setString(5, record.getColor());
				pst.setNull(6, Types.NULL);
				pst.setString(7, record.getName());
				pst.setNull(8, Types.NULL);
				pst.setInt(9, record.getYear());
				pst.setNull(10, Types.NULL);
				pst.setFloat(11, record.getPerPrice());
				pst.setFloat(12, record.getCasePrice());
				pst.setLong(13, record.getId());
				pst.setString(14, record.getComment());
				pst.setString(15, record.getRawText());
				pst.setString(16, record.getCleanedText());

				pst.addBatch();	
			}
			int[] results = pst.executeBatch();
		
		} catch (SQLException e) {
			System.out.println("Error inserting data to SQL");
			e.printStackTrace();
		}
	}
	
	/**
	 * Clean recognized record. Remove line breaks, multiple white spaces, dots
	 * before prices, etc.
	 * 
	 * @param rawRecord
	 * @return cleaned record
	 */
	private static String cleanRecord(String rawRecord) {
		//dots
		String cleanedRecord = rawRecord.replaceAll("[\\.|,|\\s]{3,}", " ");
		//white spaces
		cleanedRecord = cleanedRecord.replaceAll("[\\t\\n\\r\\s]+", " ");

		return cleanedRecord;
	}
	
	/**
	 * Clean the final version of the record from commas, widow letter,
	 * redundant spaces. The final version is what's left after trimming
	 * all the information (year, color, brackets, prices, id). The output will
	 * be used as the record name.
	 * 
	 * @param rawName
	 *            String that we have after trimming all the recognized
	 *            information from the record.
	 * @return cleaned string that will be used as a name
	 */
	private static String cleanName(String rawName) {
		//remove commas, dots and widow chars
		String cleanedString =  rawName.replaceAll("[\\.|:|,|\\s.\\s]", " ");
		//remove redundant spaces
		cleanedString = cleanedString.trim().replaceAll(" +", " ");
		
		return cleanedString;
	}
	
	/**
	 * Connect with SQL.
	 * 
	 * @return connection
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 */
	private static Connection connectWithSQL() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection connection = null;

		String url = "jdbc:sqlserver://";
		String serverName = "localhost";
		String portNumber = "1433";
		
		String connectionUrl = url + serverName + ":" + portNumber
				+ ";databaseName=" + DB_NAME + ";integratedSecurity=true;";
		
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
		connection = java.sql.DriverManager.getConnection(connectionUrl);
		
		if (connection != null) {
			//System.out.println("Connection to SQL Successful");
			
			return connection;
		} else {
			return null;
		}
	}
	

}
