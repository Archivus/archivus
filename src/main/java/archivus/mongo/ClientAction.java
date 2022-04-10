package archivus.mongo;

import com.mongodb.client.MongoClient;

@FunctionalInterface
public interface ClientAction {
    void clientAction(MongoClient client);
}
