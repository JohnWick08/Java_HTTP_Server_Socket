import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;


// Each Client Connection will be managed in a dedicated Thread
public class myServer implements Runnable {

    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    // port to listen connection
    static final int PORT = 8889;


    // Client Connection via Socket Class
    private Socket connect;
    public myServer(Socket c) {
        connect = c;
    }

    public static void main(String[] args){
        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
            // we listen until user halts server execution
            while (true) {
                myServer myServer = new myServer(serverConnect.accept());
                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                //start a thread
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // we manage our particular client connection
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            // we read characters from the client via input stream on the socket
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            // we get character output stream to client (for headers)
            out = new PrintWriter(connect.getOutputStream());
            // get binary output stream to client (for requested data)
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            // get first line of the request from the client
            String input = in.readLine();
            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get file requested
            fileRequested = parse.nextToken().toLowerCase();

            
            // GET or HEAD method
            if (fileRequested.endsWith("/")) {
                fileRequested += DEFAULT_FILE;
            }

            File file = new File(WEB_ROOT, fileRequested);
            int fileLength = (int) file.length();
            String content = getContentType(fileRequested);

            if (method.equals("GET")) { // GET method so we return content
                byte[] fileData = readFileData(file, fileLength);
                // send HTTP Headers
                out.println("HTTP/1.1 200 OK");
                out.println("Server: Java HTTP Server from SSaurel : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + content);
                out.println("Content-length: " + fileLength);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer

                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            }
        } catch (Exception e) {
            //if the requested file is not present, 404
            String httpResponse = "HTTP/1.1 200 OK\r\n\r\n <html><body><h1>404 Not Found</h1></body></html>";
            try{
            connect.getOutputStream().write(httpResponse.getBytes("UTF-8"));
            System.out.println(e);
            }catch(Exception ee){

            }
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }
        }

    }

    //get the byte data from file
    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }
        return fileData;
    }

    // return supported MIME Types
    private String getContentType(String fileRequested) {
        if (fileRequested.contains(".html"))
            return "text/html";
        else if(fileRequested.contains(".mp4"))
            return "video/mp4";
        else
            return "text/plain";
    }
}