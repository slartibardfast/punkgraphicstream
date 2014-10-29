/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.connolly.david.pgs;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author slarti
 */
public class FrameRateTest {

    public FrameRateTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of values method, of class FrameRate.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        FrameRate[] expResult = null;
        FrameRate[] result = FrameRate.values();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of valueOf method, of class FrameRate.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String name = "";
        FrameRate expResult = null;
        FrameRate result = FrameRate.valueOf(name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of startFrame method, of class FrameRate.
     */
    @Test
    public void testStartFrame() {
        System.out.println("startFrame");
        double start = 0.0;
        FrameRate instance = null;
        long expResult = 0L;
        long result = instance.startFrame(start);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of endFrame method, of class FrameRate.
     */
    @Test
    public void testEndFrame() {
        System.out.println("endFrame");
        double end = 0.0;
        FrameRate instance = null;
        long expResult = 0L;
        long result = instance.endFrame(end);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of milliseconds method, of class FrameRate.
     */
    @Test
    public void testMilliseconds() {
        System.out.println("milliseconds");
        FrameRate instance = null;
        long expResult = 0L;
        long result = instance.milliseconds();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of ticks method, of class FrameRate.
     */
    @Test
    public void testTicks() {
        System.out.println("ticks");
        FrameRate instance = null;
        double expResult = 0.0;
        double result = instance.ticks();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of fps method, of class FrameRate.
     */
    @Test
    public void testFps() {
        System.out.println("fps");
        FrameRate instance = null;
        double expResult = 0.0;
        double result = instance.fps();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}