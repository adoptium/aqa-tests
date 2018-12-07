/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
/*
 * DateFormatTest.java
 */
import java.util.*;
import java.text.*;

public class DateFormatTest {
    private Locale locale;
    private ResourceBundle resource;
    private Calendar calendar;
    private String version;

    public DateFormatTest(Locale locale) {
        this.locale = locale;
	calendar = Calendar.getInstance();
	version = System.getProperty("java.version");
	int index1 = version.indexOf('.');
	if (index1 >= 0 && index1+1 < version.length()){
		int index2 = version.indexOf('.',index1+1);
		if (index2 > 0){
			version = version.substring(index1+1, index2);
		}
	}
        resource = ResourceBundle.getBundle("ResourceBundleTest", locale);
	if (resource == null){
		System.err.println("Cannot get resource for "+locale);
		System.exit(-1);
	}
	if (resource.getLocale().getLanguage().length() == 0){
		System.out.println("Warning: Default resource file was selected. You may need to create a resource file for "+locale);
	}
    }
    
    public DateFormatTest() {
        this(Locale.getDefault());
    }
    
    public void printDateTime(int dateStyle, int timeStyle) {
        DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
        System.out.print(df.format(calendar.getTime()));
        SimpleDateFormat sdf = (SimpleDateFormat)df;              // cast to SimpleDateFormat
        System.out.println("  (" + sdf.toPattern() + ")");
    }
    
    public void printDate(int dateStyle) {
        DateFormat df = DateFormat.getDateInstance(dateStyle, locale);
        System.out.print(df.format(calendar.getTime()));
        SimpleDateFormat sdf = (SimpleDateFormat)df;              // cast to SimpleDateFormat
        System.out.println("  (" + sdf.toPattern() + ")");
    }
    
    public void printTime(int timeStyle) {
        DateFormat df = DateFormat.getTimeInstance(timeStyle, locale);
        System.out.print(df.format(calendar.getTime()));
        SimpleDateFormat sdf = (SimpleDateFormat)df;              // cast to SimpleDateFormat
        System.out.println("  (" + sdf.toPattern() + ")");
    }

    private boolean checkFormat(String resourceKey, int style) {
	String expectedString = getResource(resourceKey);
	SimpleDateFormat sdf = (SimpleDateFormat)(DateFormat.getDateTimeInstance(style, style, locale));
	String format = sdf.toPattern();
	if (expectedString.compareTo(format) != 0) {
		System.out.println("Format Mismatching: "+expectedString+" - "+format);
		return false;
	}
	return true;
    }

    private boolean checkParse(String resourceKey, int style) {
	String expectedString = parse(getResource(resourceKey));
	DateFormat df = DateFormat.getDateTimeInstance(style, style, locale);
	String formattedString = df.format(calendar.getTime());
	if (expectedString.compareTo(formattedString) != 0){
		System.out.println("Mismatching: "+expectedString+" - "+formattedString);
		return false;
	}
	return true;
    }

    private String getResource(String resourceKey){
	Enumeration keys = resource.getKeys();
	String resourceKeyWithVersion = resourceKey+"_"+version;
	while(keys.hasMoreElements()) {
		String str = (String)(keys.nextElement());
		if (str.compareTo(resourceKeyWithVersion) == 0) {
			resourceKey = resourceKeyWithVersion;
			break;
		}
	}
	return resource.getString(resourceKey);
    }

    public String parse(String templ) {
	StringBuffer buffer = new StringBuffer();
	int i = 0;
	while (i < templ.length()) {
		char c = templ.charAt(i);
		int followingCount = getFollowingCharCount(templ, i+1, c);
		int val;
		switch(c) {
		case 'y':
		{
			if (followingCount == 3 || followingCount == 0){ /* yyyy or y*/
				buffer.append(calendar.get(Calendar.YEAR));
			}else if (followingCount == 1 ){ /* yy */
				int year = calendar.get(Calendar.YEAR);
				year %= 100;
				buffer.append(Integer.toString(year));
			}
			break;
		}
		case 'M':
		{
			val = calendar.get(Calendar.MONTH)+1;
			if (followingCount >= 3) { /* MMMM */
				String month = resource.getString("Month"+val+"F");
				buffer.append(month);
			}else if (followingCount == 2) { /* MM */
				String month = resource.getString("Month"+val);
				buffer.append(month);
			}else if (followingCount == 1) {
				if (val < 10) {
					buffer.append("0");
				}
				buffer.append(val);
			}else{
				buffer.append(val);
			}
			break;
		}
		case 'd':
		{
			val = calendar.get(Calendar.DAY_OF_MONTH);
			if (followingCount == 1 && val < 10) {
				buffer.append("0");
			}
			buffer.append(val);
			break;
		}
		case 'H': /* Hour 0-23 */
		{
			val = calendar.get(Calendar.HOUR_OF_DAY);
			if (followingCount == 1 && val < 10) {
				buffer.append("0");
			}
			buffer.append(val);
			break;
		}
		case 'k': /* Hour 1-24 */
		{
			val = calendar.get(Calendar.HOUR_OF_DAY); /* return 0-23 */
			if (val == 0) val = 24;
			if (followingCount == 1 && val < 10) {
				buffer.append("0");
			}
			buffer.append(val);
			break;
		}
		case 'h': /* Hour 1-12 */
		{
			val = calendar.get(Calendar.HOUR); /* return 0-11 */
			if (val == 0) val = 12;
			if (followingCount == 1 && val < 10) {
				buffer.append("0");
			}
			buffer.append(val);
			break;
		}
		case 'K': /* Hour 0-11 */
		{
			val = calendar.get(Calendar.HOUR);
			if (followingCount == 1 && val < 10) {
				buffer.append("0");
			}
			buffer.append(val);
			break;
		}
		case 'm':
		{
			val = calendar.get(Calendar.MINUTE);
			if (followingCount == 1 && val < 10) {
				buffer.append("0");
			}
			buffer.append(val);
			break;
		}
		case 's':
		{
			val = calendar.get(Calendar.SECOND);
			if (followingCount == 1 && val < 10) {
				buffer.append("0");
			}
			buffer.append(val);
			break;
		}
		case 'E':
		{
			val = calendar.get(Calendar.DAY_OF_WEEK);
			if (followingCount >= 3) {
				String dayOfWeek = resource.getString("Day"+val+"F");
				buffer.append(dayOfWeek);
			}else{
				String dayOfWeek = resource.getString("Day"+val);
				buffer.append(dayOfWeek);
			}
			break;
		}
		case 'a':
		{
			val = calendar.get(Calendar.AM_PM);
			if (val == Calendar.AM){
				buffer.append(resource.getString("AM"));
			}else{
				buffer.append(resource.getString("PM"));
			}
			break;
		}
		case 'z':
		{
			TimeZone tz = calendar.getTimeZone();
			if (followingCount >= 3) {
				buffer.append(tz.getDisplayName(tz.useDaylightTime(), TimeZone.LONG));
			}else{
				buffer.append(tz.getDisplayName(tz.useDaylightTime(), TimeZone.SHORT));
			}
			break;
		}
		case '\'':
		{
			char next;
			while(++i < templ.length() && (next = templ.charAt(i)) != '\'') {
				buffer.append(next);	
			}
			break;
		}
		default:
			buffer.append(c);
		}
		i += (followingCount + 1);
        }
	return buffer.toString();
    }
    
    private int getFollowingCharCount(String target, int index, char c){
	int len = target.length();
	int i = 0;
	while (len > index+i && target.charAt(index+i) == c){
		i++;
        }
	return i;
    }

    public static void main(String[] args) {
        DateFormatTest test = new DateFormatTest();
        
        // full format
        System.out.print("FULL:FULL:     ");
        test.printDateTime(DateFormat.FULL, DateFormat.FULL);
        
        // long format
        System.out.print("LONG:LONG:     ");
        test.printDateTime(DateFormat.LONG, DateFormat.LONG);
        
        // medium format
        System.out.print("MEDIUM:MEDIUM: ");
        test.printDateTime(DateFormat.MEDIUM, DateFormat.MEDIUM);
        
        // short format
        System.out.print("SHORT:SHORT:   ");
        test.printDateTime(DateFormat.SHORT, DateFormat.SHORT);

	//AutoCheck
	boolean result = true;
	result &= test.checkFormat("FullFormat",   DateFormat.FULL);
	result &= test.checkFormat("LongFormat",   DateFormat.LONG);
	result &= test.checkFormat("MediumFormat", DateFormat.MEDIUM);
	result &= test.checkFormat("ShortFormat",  DateFormat.SHORT);

	result &= test.checkParse("FullFormat",    DateFormat.FULL);
	result &= test.checkParse("LongFormat",    DateFormat.LONG);
	result &= test.checkParse("MediumFormat",  DateFormat.MEDIUM);
	result &= test.checkParse("ShortFormat",   DateFormat.SHORT);

	System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
	System.out.println("Test result for i18n DateFormatTest ...");
	if (result){
		System.out.println("OK");
	}else{
		System.out.println("FAILED");
	}
	System.out.println("--------------------------------------------------");
    }
}


