package archivus.mongo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.slf4j.LoggerFactory;

public class Mongo {
    String secretKey;
    MongoClient client;

    public Mongo(String secretKey){
        this.secretKey = secretKey;
    }

    public void initMongo(){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.OFF);

        client = new MongoClient(new MongoClientURI(secretKey));
    }

    public MongoClient getClient(){
        return client;
    }
}
