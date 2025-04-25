import java.awt.Robot;
import java.awt.event.KeyEvent;

public class MinimizeMacWindows {
  public static void main(String... args) throws Exception {

    System.out.println("MinimizeMacWindows: Minimizing all other app windows Cmd+Alt+H");

    Robot r = new Robot();
    r.keyPress(KeyEvent.VK_META);
    r.delay(250);
    r.keyPress(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyPress(KeyEvent.VK_H);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_H);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_META);
    r.delay(250);
  }
}
