package automation.api.components;

import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A player which is actually an interface to the famous MPlayer.
 * @author Adrian BER - Modifications: Olivier Van Goethem
 */
public class JMPlayer {

    private static Logger logger = Logger.getLogger(JMPlayer.class.getName());

    /** A thread that reads from an input stream and outputs to another line by line. */
    private static class LineRedirecter extends Thread {
        /** The input stream to read from. */
        private InputStream in;
        /** The output stream to write to. */
        private OutputStream out;
        /** The prefix used to prefix the lines when outputting to the logger. */
        private String prefix;

        /**
         * @param in the input stream to read from.
         * @param out the output stream to write to.
         * @param prefix the prefix used to prefix the lines when outputting to the logger.
         */
        LineRedirecter(InputStream in, OutputStream out, String prefix) {
            this.in = in;
            this.out = out;
            this.prefix = prefix;
        }

        public void run()
        {
            try {
                // creates the decorating reader and writer
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                PrintStream printStream = new PrintStream(out);
                String line;

                // read line by line
                while ( (line = reader.readLine()) != null) {
                    logger.info((prefix != null ? prefix : "") + line);
                    printStream.println(line);
                }
            } catch (IOException exc) {
                logger.log(Level.WARNING, "An error has occured while grabbing lines", exc);
            }
        }

    }

    /** The path to the MPlayer executable. */
    private String mplayerPath;
    /** Cookie file location for use with youtube-dl*/
    private String youtubeCookieFile; 
    /** Options passed to MPlayer. */
    private String mplayerOptions;

    /** The process corresponding to MPlayer. */
    private Process mplayerProcess;
    /** The standard input for MPlayer where you can send commands. */
    private PrintStream mplayerIn;
    /** A combined reader for the the standard output and error of MPlayer. Used to read MPlayer responses. */
    private BufferedReader mplayerOutErr;

    public JMPlayer(String mplayerPath, String mplayerOptions, String youtubeCookieFile) {
        this.mplayerPath = mplayerPath;
        this.mplayerOptions = mplayerOptions + " -cache 2048 -cache-min 5";
        this.youtubeCookieFile = youtubeCookieFile;
    }

    /** @return the path to the MPlayer executable. */
    public String getMPlayerPath() {
        return mplayerPath;
    }

    /** Sets the path to the MPlayer executable.
     * @param mplayerPath the new MPlayer path; this will be actually effective
     * after {@link #close() closing} the currently running player.
     */
    public void setMPlayerPath(String mplayerPath) {
        this.mplayerPath = mplayerPath;
    }

    public void open(String file) throws IOException {
        if (mplayerProcess == null) {
            // start MPlayer as an external process
            String command;
            if (file.contains("www.youtube.com")) {
                String youtubeParser = youtubeCookieFile + " $(" + mplayerPath + "youtube-dl -f 22/18 -g --cookies " + youtubeCookieFile + " " + file + ")";
                String mplayerCommand = mplayerPath + "mplayer " + mplayerOptions + " -cookies -cookies-file";
                command = mplayerCommand + " " + youtubeParser;
            } else {
                command = mplayerPath + "mplayer " + mplayerOptions + " " + file;
            }
            logger.info("Starting MPlayer process: " + command);
            mplayerProcess = Runtime.getRuntime().exec(new String[] {"bash", "-c", command});

            // create the piped streams where to redirect the standard output and error of MPlayer
            PipedInputStream  readFrom = new PipedInputStream(1024*1024);
            PipedOutputStream writeTo = new PipedOutputStream(readFrom);
            mplayerOutErr = new BufferedReader(new InputStreamReader(readFrom));

            // create the threads to redirect the standard output and error of MPlayer
            new LineRedirecter(mplayerProcess.getInputStream(), writeTo, "MPlayer says: ").start();
            new LineRedirecter(mplayerProcess.getErrorStream(), writeTo, "MPlayer encountered an error: ").start();

            // the standard input of MPlayer
            mplayerIn = new PrintStream(mplayerProcess.getOutputStream());
        } else {
            // If youtube video - restart MPlayer (avoids having to inject stream with youtube-dl)
            if (file.contains("www.youtube.com")) {
                close();
                open(file);
            }
            // Else just open file normally
            else {
                execute("loadfile " + file + " 0");
            }
        }
        // wait to start playing
        waitForAnswer("Starting playback...");
        logger.info("Started playing file " + file);
    }

    public void close() {
        if (mplayerProcess != null) {
            execute("quit");
            try {
                mplayerProcess.waitFor();
            }
            catch (InterruptedException e) {}
            mplayerProcess = null;
        }
    }

    public String getPlayingFile() {
        String path = getProperty("path");
        return path == null ? "Not in use!" : path;
    }

    public void togglePlay() {
        execute("pause");
    }

    public boolean isPlaying() {
        return mplayerProcess != null;
    }

    public long getTimePosition() {
        return getPropertyAsLong("time_pos");
    }

    public void setTimePosition(long seconds) {
        setProperty("time_pos", seconds);
    }

    public long getTotalTime() {
        return getPropertyAsLong("length");
    }

    public float getVolume() {
        return getPropertyAsFloat("volume");
    }

    public void setVolume(float volume) {
        setProperty("volume", volume);
    }

    public void setFullScreen(boolean value) {
        if (value) {
            execute("vo_fullscreen " + 1);
        } else {
            execute("vo_fullscreen " + 0);
        }
    }

    protected String getProperty(String name) {
        if (name == null || mplayerProcess == null) {
            return null;
        }
        String s = "ANS_" + name + "=";
        String x = execute("get_property " + name, s);
        if (x == null)
            return null;
        if (!x.startsWith(s))
            return null;
        return x.substring(s.length());
    }

    protected long getPropertyAsLong(String name) {
        try {
            return Long.parseLong(getProperty(name));
        }
        catch (NumberFormatException exc) {}
        catch (NullPointerException exc) {}
        return 0;
    }

    protected float getPropertyAsFloat(String name) {
        try {
            return Float.parseFloat(getProperty(name));
        }
        catch (NumberFormatException exc) {}
        catch (NullPointerException exc) {}
        return 0f;
    }

    protected void setProperty(String name, String value) {
        execute("set_property " + name + " " + value);
    }

    protected void setProperty(String name, long value) {
        execute("set_property " + name + " " + value);
    }

    protected void setProperty(String name, float value) {
        execute("set_property " + name + " " + value);
    }

    /** Sends a command to MPlayer..
     * @param command the command to be sent
     */
    private void execute(String command) {
        execute(command, null);
    }

    /** Sends a command to MPlayer and waits for an answer.
     * @param command the command to be sent
     * @param expected the string with which has to start the line; if null don't wait for an answer
     * @return the MPlayer answer
     */
    private String execute(String command, String expected) {
        if (mplayerProcess != null) {
            logger.info("Send to MPlayer the command \"" + command + "\" and expecting "
                    + (expected != null ? "\"" + expected + "\"" : "no answer"));
            mplayerIn.print(command);
            mplayerIn.print("\n");
            mplayerIn.flush();
            logger.info("Command sent");
            if (expected != null) {
                String response = waitForAnswer(expected);
                logger.info("MPlayer command response: " + response);
                return response;
            }
        }
        return null;
    }

    /** Read from the MPlayer standard output and error a line that starts with the given parameter and return it.
     * @param expected the expected starting string for the line
     * @return the entire line from the standard output or error of MPlayer
     */
    private String waitForAnswer(String expected) {
        // todo add the possibility to specify more options to be specified
        // todo use regexp matching instead of the beginning of a string
        String line = null;
        if (expected != null) {
            try {
                while ((line = mplayerOutErr.readLine()) != null) {
                    logger.info("Reading line: " + line);
                    if (line.startsWith(expected)) {
                        return line;
                    }
                }
            }
            catch (IOException e) {
            }
        }
        return line;
    }
}