import java.awt.GraphicsEnvironment;

// List the System Java fonts installed
public class ListJavaFonts {
    public static void main(String[] args) {
        String java_fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        for(String font : java_fonts) {
            System.out.println(font);
        }
    }
}
