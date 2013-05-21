package tests.dummies;

import automation.api.AbstractDevice;

public class DummyAbstractDevice extends AbstractDevice {

	private Integer counter;
	
	@Override
	final protected void onStartup() {
		counter = 0;
	}
	
	public void setCounterToOne() {
		setCounter(1);
	}
	
	public void setCounter(Integer value) {
		counter = value;
	}
	
	public Integer getCounter() {
		return counter;
	}

}
