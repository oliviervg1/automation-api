package tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import tests.dummies.DummyAbstractDevice;

public class AbstractDeviceTest {
	
	private DummyAbstractDevice device = new DummyAbstractDevice();
	
	@Test
    public void InvokeMethod() {
		try {
			device.invokeMethod("setCounterToOne");
		} catch (NoSuchMethodException e) { 
			fail("No such method");
		} 
		assertTrue(device.getCounter().equals(new Integer(1)));
    }
	
    @Test
    public void InvokeMethodWithParameters() {
    	Object[] param = {new Integer(2)};
    	try {
    		device.invokeMethod("setCounter", param);
    	} catch (NoSuchMethodException e) { 
			fail("No such method");
		} 
		assertTrue(device.getCounter().equals(new Integer(2)));
    }
    
    @Test
    public void InvokeMethodWithReturn() {
    	Integer test = null;
    	try {
    		test = (Integer) device.invokeMethod("getCounter");
    	} catch (NoSuchMethodException e) { 
			fail("No such method");
		} 
		assertTrue(test.equals(device.getCounter()));
    }
    
	@Test(expected = NoSuchMethodException.class)
	public void InvokeNonExistingMethod() throws NoSuchMethodException {
		device.invokeMethod("NonExistingMethod");
	}
}
