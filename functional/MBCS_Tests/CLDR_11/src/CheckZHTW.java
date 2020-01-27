import java.util.*;

public class CheckZHTW {
  private final static boolean result;
  static {
    result = Locale.TAIWAN.equals(Locale.getDefault())
      && "HAST".equals(TimeZone.getTimeZone("US/Aleutian").getDisplayName(false, TimeZone.SHORT));
  }
  public static void main(String[] args) {
    System.exit(result ? 1 : 0);
  }
  private static void setIgnoreData(Set<String> tset) {
   if ("true".equalsIgnoreCase(System.getenv("USE_ZHTW_WORKAROUND"))) {
      tset.add("America/Adak");
      tset.add("America/Atka");
      tset.add("Pacific/Honolulu");
      tset.add("Pacific/Johnston");
      tset.add("SystemV/HST10");
      tset.add("US/Aleutian");
      tset.add("US/Hawaii");
      tset.add("Antarctica/Troll");
      tset.add("Asia/Taipei");
    }
  }
  public static Set<String> getDataInstance() {
    TreeSet<String> tset = new TreeSet<String>();
    if (result) setIgnoreData(tset);
    return tset;
  }
}
