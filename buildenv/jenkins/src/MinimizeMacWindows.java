import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.Desktop;
import java.io.File;

public class MinimizeMacWindows {
  public static void main(String... args) throws Exception {

    // Minimize all windows...
    System.out.println("MinimizeMacWindows: Issuing Alt-Cmd-H to minimize all 'other' Windows");
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

    System.out.println("MinimizeMacWindows: Issuing Alt-Cmd-M to minimize all 'front' Windows");
    r.keyPress(KeyEvent.VK_META);
    r.delay(250);
    r.keyPress(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyPress(KeyEvent.VK_M);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_M);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_META);
    r.delay(250);

    // Now open and close all Finder windows... so that the Finder is all closed and does not re-open on completion
    System.out.println("MinimizeMacWindows: Issuing Alt-Cmd-Space open 'Finder' so as to become 'front' Window");
    r.keyPress(KeyEvent.VK_META);
    r.delay(250);
    r.keyPress(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyPress(KeyEvent.VK_SPACE);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_SPACE);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_META);
    r.delay(250);

    System.out.println("MinimizeMacWindows: Issuing Alt-Cmd-W to close 'front' 'Finder' Window");
    r.keyPress(KeyEvent.VK_META);
    r.delay(250);
    r.keyPress(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyPress(KeyEvent.VK_W);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_W);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_ALT);
    r.delay(250);
    r.keyRelease(KeyEvent.VK_META);
    r.delay(250);
  }
}
