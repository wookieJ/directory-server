import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server
{
    public static void main(String[] args) throws Exception
    {
        int port = 8000;

        String mainDirectoryPath;
        if(args.length > 0)
            mainDirectoryPath = args[0];
        else
            return;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler(mainDirectoryPath));

        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler
    {
        private String path;

        public RootHandler(String path)
        {
            this.path = path;
        }

        public RootHandler()
        {
        }

        public void handle(HttpExchange exchange) throws IOException
        {
            // listing whole directory
            String uriRequest = exchange.getRequestURI().toString().substring(1);
            String directoryPath = path + "/" + uriRequest;
            System.out.println(directoryPath);
            //directoryPath = directoryPath.substring(0,directoryPath.length() - 1);
            File root = new File(directoryPath);
            if (root.exists())
            {
                // Path traversal
                if(root.getCanonicalFile().toPath().toString().length() < path.length())
                {
                    exchange.sendResponseHeaders(403, -1);
                }
                else
                {
                    if (root.isDirectory())
                    {
                        File[] filesList = root.listFiles();

                        StringBuilder files = new StringBuilder("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /></head><body><h1>Directories:</h1>");

                        for (File file : filesList)
                        {
                            String temp = file.getName().substring(0);
                            // if (file.isFile()) {
                            files.append("<a href=\"" + uriRequest + "\\" + file.getName() + "\">" + file.getName() + "</a><br/>");
                            // }
                        }
//                    Files.walk(root.toPath()).forEach(p -> files.append(p + "\n"));
                        files.append("</body></html>");
                        byte[] bytes = files.toString().getBytes();
                        // brak nie przeszkadza - domyślne?
                        exchange.getResponseHeaders().set("Content-Type", "text/html");
                        // brak przeszkadza - "serwer nie wysłał danych", 0 to samo, za mało - "serwer zakończył połączenie"
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(bytes);
                        os.close();
                    } else
                    {
                        byte[] responseFile = Files.readAllBytes(Paths.get(directoryPath));
                        exchange.getResponseHeaders().set("Content-Type", "text/plain");
                        exchange.sendResponseHeaders(200, responseFile.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(responseFile);
                        os.close();
                    }
                }
            }

            exchange.sendResponseHeaders(404, -1);
        }
    }
}