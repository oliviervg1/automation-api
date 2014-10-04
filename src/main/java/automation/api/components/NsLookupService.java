package automation.api.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NsLookupService {

    public static String findDeviceIp(String deviceName) throws IOException {

        for (Integer j = 0; j<255; j++) {
            for (Integer i = 0; i<255; i++) {

                String ipToTest = "192.168." + j.toString() + "." + i.toString();
                ProcessBuilder builder = new ProcessBuilder(
                    new String[] {"nslookup", ipToTest}
                );

                final Process process = builder.start();
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("name = " + deviceName)) {
                        br.close();
                        isr.close();
                        process.destroy();
                        return ipToTest;
                    }
                }

                br.close();
                isr.close();
                process.destroy();
            }
        }

        return "Not found";
    }
}
