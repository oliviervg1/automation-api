package automation.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jws.WebService;
import automation.api.interfaces.ConnectedDevice;

@WebService(endpointInterface = "automation.api.interfaces.ConnectedDevice")
public abstract class AbstractDevice implements ConnectedDevice{
	
	private Method method;
	
	public AbstractDevice() {
		method = null;
		onStartup();
	}
	
	protected abstract void onStartup();
	
	@Override
	final public Object invokeMethod(String methodName) throws NoSuchMethodException {
		return invokeMethod(methodName, null);
	}
	
	@Override
	final public Object invokeMethod(String methodName, Object[] parametersArray) throws NoSuchMethodException {
		Object ret = 0;
		try {
			if (parametersArray == null || parametersArray.length == 0) {
				method = this.getClass().getDeclaredMethod(methodName);
			}
			else {
				method = this.getClass().getDeclaredMethod(methodName, findParameterTypes(parametersArray));
			}			
			ret = method.invoke(this, parametersArray);
			// Cannot pass null over SOAP, so force to 0 if method has set ret to null
			if (ret == null) {
				ret = 0;
			}			
		} catch (SecurityException | IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		return ret;
	}
	
	@SuppressWarnings("rawtypes")
	final private Class[] findParameterTypes(Object[] parameters) {
		Class[] parameterTypes = new Class[parameters.length];
		for (int i=0; i<parameters.length; i++) {
			parameterTypes[i] = parameters[i].getClass();
		}
		return parameterTypes;
	}
}
