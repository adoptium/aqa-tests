package net.adoptopenjdk.test.build.freetype;

import java.io.File;
import net.adoptopenjdk.test.build.common.BuildIs;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

/*
 * Tests for the (intentional and correct) absence of a bundled Freetype
 * library in this JDK.
 */
@Test(groups={ "level.sanity" })
public class FreetypeAbsenceTest {

    private static Logger logger = Logger.getLogger(FreetypeAbsenceTest.class);

    /**
     * Returns true if, and only if, we're running on a build & platform this test
     * is relevant to.
     */
    private boolean rightEnvForTest() {
        // BuildIs methods are used like asserts.
        return BuildIs.createdByThisVendor("AdoptOpenJDK")
               && "Linux".contains(System.getProperty("os.name").split(" ")[0]);
    }

    @Test
    public void testTemplateExample() {
        logger.info("Log message for the template test.");
        if(!rightEnvForTest()) return;
        if(BuildIs.thisMajorVersion(8)) {
            Assert.assertFalse((new File(System.getProperty("java.home") + "/jre/lib/amd64/libfreetype.so.6")).exists());
        } else {
            Assert.assertFalse((new File(System.getProperty("java.home") + "/lib/libfreetype.so")).exists());
        }
    }

}