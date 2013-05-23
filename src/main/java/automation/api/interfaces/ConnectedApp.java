package automation.api.interfaces;

import java.io.File;

import javax.xml.namespace.QName;

public interface ConnectedApp {
	void uploadFile(String fileName, File fileData) throws Exception;
	void connectToRemoteDevice(String WS_URL, QName qname);
	boolean isDeviceAvailable();
	Object invokeMethod(String methodName) throws Exception;
	Object invokeMethod(String methodName, Object[] parametersArray) throws Exception;
}