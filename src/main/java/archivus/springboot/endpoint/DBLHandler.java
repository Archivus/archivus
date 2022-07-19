package archivus.springboot.endpoint;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
public class DBLHandler {
    @RequestMapping(value = "webhook/dbl", consumes = "application/json", produces = "application/json",
            method = RequestMethod.POST)
    public void onVote(@RequestHeader(value = "Authorization") String token, @RequestBody String payload){
        Logger logger = Logger.getAnonymousLogger();
        logger.info(payload);

        try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_secrets.properties"))) {
            Properties prop = new Properties();
            prop.load(input);
            if (!token.equals(prop.getProperty("secrets.dbl_webhook"))) return;

            JSONObject data = new JSONObject(payload);

            // Use payload
        } catch (IOException ex){
            System.err.println("There's an issue with the path given to or data in the " +
                    "archivus_links.properties file");
            ex.printStackTrace();
            System.exit(0);
        }

    }
}