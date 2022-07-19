package archivus.tasks;

import archivus.mongo.Mongo;
import net.dv8tion.jda.api.JDA;

import java.util.TimerTask;

public interface ScheduledTask{
    void task(Mongo mongo, JDA jda);
    void print();
}
