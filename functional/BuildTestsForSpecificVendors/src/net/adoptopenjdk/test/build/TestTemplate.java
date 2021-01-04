package net.adoptopenjdk.test.build;

import net.adoptopenjdk.test.build.common.BuildIs;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

/*
 * Test description goes here.
 */
@Test(groups={ "level.sanity" })
public class TestTemplate {

    private static Logger logger = Logger.getLogger(TestTemplate.class);

    /**
     * Returns true if, and only if, we're running on a build & platform this test
     * is relevant to.
     */
    private boolean rightEnvForTest() {
        // BuildIs methods are used like asserts.
        return BuildIs.createdByThisVendor("AdoptOpenJDK")
               && BuildIs.thisMajorVersion(8)
               && BuildIs.moreRecentThanOrEqualToVersion("1.8.0_250+b20");
    }

    @Test
    public void testTemplateExample() {
        if(!rightEnvForTest()) {
        	logger.info("Wrong environment for test. Skipped!");
            return;
        }
        logger.info("Test template test starting.");
        String whatWeGot = "aaa";
        String whatWeWant = "aaa";
        Assert.assertEquals(whatWeGot, whatWeWant, "Print this message if those two strings don't match.");
        logger.info("Test completed successfully.");
    }

}