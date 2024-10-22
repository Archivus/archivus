package archivus;

import archivus.commands.CommandListener;
import archivus.commands.CommandType;
import archivus.commands.generalcommands.CreateAccountCommand;
import archivus.mongo.Mongo;
import archivus.springboot.SpringApp;
import archivus.tasks.ScheduledTask;
import archivus.tasks.UpdatePostTask;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Archivus {
    private final static ArrayList<ScheduledTask> tasks = new ArrayList<>(){
        {
            add(new UpdatePostTask());
        }
    };
    public static void main(String[] args) throws LoginException {
        // Input the Bot Token and Mongo Secret Key.
        // Try not to run Program from IDE, instead from the Command Line as running
        // as the command args are saved in a file

        try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_secrets.properties"))){
            Properties prop = new Properties();
            prop.load(input);



            SpringApp.init(args);
            final Mongo mongo = new Mongo(prop.getProperty("mongokey"));
            final CommandListener cmdListener = new CommandListener(mongo);
            final JDA jda = JDABuilder.createLight(prop.getProperty("botkey"),
                            GatewayIntent.GUILD_EMOJIS, GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGES)
                    .setStatus(OnlineStatus.ONLINE)
                    .addEventListeners(cmdListener)
                    .build();


            jda.updateCommands().addCommands(cmdListener.retrieveCommandData())
                    .queue();

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    tasks.forEach(t -> {
                        t.task(mongo, jda);
                        t.print();
                    });
                }
            };
            timerTask.run();
            Calendar tomorrow = new GregorianCalendar();
            tomorrow.add(Calendar.DATE, 1);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(timerTask, new GregorianCalendar(tomorrow.get(Calendar.YEAR),
                    tomorrow.get(Calendar.MONTH), tomorrow.get(Calendar.DATE), 1, 0).getTime(),
                    1000 * 60 * 60 * 24);

        } catch (IOException ex){
            System.err.println("There's an issue with the path given to or data in the " +
                    "archivus_links.properties file");
            ex.printStackTrace();
            System.exit(0);
        }

        /*
         TODO:
            Startup Tasks
            Bot List Authentication
         */

    }

    public static int colorPicker(){
        int[] archivusColorScheme = new int[]{0xfc9c46, 0xff9152, 0xff8161, 0xff8361};

        return archivusColorScheme[new Random().nextInt(archivusColorScheme.length)];
    }

    public static String funFact(){
        String[] facts =
                new String[]{"The first meme was actually a smiley, like this :-). (For all you NORMIES out there)",
                            };

        return facts[new Random().nextInt(facts.length)];
    }
}
