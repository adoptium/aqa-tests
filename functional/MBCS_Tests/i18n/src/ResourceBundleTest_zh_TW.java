import java.util.ListResourceBundle;

public class ResourceBundleTest_zh_TW extends ListResourceBundle {
   public Object[][] getContents(){
      return contents;
   }

   static final Object[][] contents = {
      {"locale","\u5730\u533A"},
      {"timezone","\u65F6\u533A"},
      {"date","\u65E5\u671F"},
      {"number","\u6570\u5B57"},
      {"currency","\u8D27\u5E01"},
      {"percent","\u767E\u5206\u6BD4"},
      {"FullFormat","y\u5E74M\u6708d\u65E5 EEEE ah:mm:ss [zzzz]"},
      {"LongFormat","y\u5E74M\u6708d\u65E5 ah:mm:ss [z]"},
      {"MediumFormat","y\u5E74M\u6708d\u65E5 ah:mm:ss"},
      {"ShortFormat","y/M/d ah:mm"},
      /* for E,EE,EEE */
      {"Day1","\u661F\u671F\u65E5"},
      {"Day2","\u661F\u671F\u4E00"},
      {"Day3","\u661F\u671F\u4E8C"},
      {"Day4","\u661F\u671F\u4E09"},
      {"Day5","\u661F\u671F\u56DB"},
      {"Day6","\u661F\u671F\u4E94"},
      {"Day7","\u661F\u671F\u516D"},
      /* for EEEE */
      {"Day1F","\u661F\u671F\u65E5"},
      {"Day2F","\u661F\u671F\u4E00"},
      {"Day3F","\u661F\u671F\u4E8C"},
      {"Day4F","\u661F\u671F\u4E09"},
      {"Day5F","\u661F\u671F\u56DB"},
      {"Day6F","\u661F\u671F\u4E94"},
      {"Day7F","\u661F\u671F\u516D"},
      /* for MMM */
      {"Month1","\u0031\u6708"},
      {"Month2","\u0032\u6708"},
      {"Month3","\u0033\u6708"},
      {"Month4","\u0034\u6708"},
      {"Month5","\u0035\u6708"},
      {"Month6","\u0036\u6708"},
      {"Month7","\u0037\u6708"},
      {"Month8","\u0038\u6708"},
      {"Month9","\u0039\u6708"},
      {"Month10","\u0031\u0030\u6708"},
      {"Month11","\u0031\u0031\u6708"},
      {"Month12","\u0031\u0032\u6708"},
      /* for MMMM */
      {"Month1F","\u0031\u6708"},
      {"Month2F","\u0032\u6708"},
      {"Month3F","\u0033\u6708"},
      {"Month4F","\u0034\u6708"},
      {"Month5F","\u0035\u6708"},
      {"Month6F","\u0036\u6708"},
      {"Month7F","\u0037\u6708"},
      {"Month8F","\u0038\u6708"},
      {"Month9F","\u0039\u6708"},
      {"Month10F","\u0031\u0030\u6708"},
      {"Month11F","\u0031\u0031\u6708"},
      {"Month12F","\u0031\u0032\u6708"},
      /* for a */
      {"AM","\u4E0A\u5348"},
      {"PM","\u4E0B\u5348"},
   };
}
