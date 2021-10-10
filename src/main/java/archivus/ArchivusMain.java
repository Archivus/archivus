package archivus;

import archivus.commands.CommandListener;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.stream.Collectors;

public class ArchivusMain {
    public static void main(String[] args) throws LoginException {
        // Input the Bot Token and Mongo Secret Key.
        // Try not to run Program from IDE, instead from the Command Line as running
        // as the command args are saved in a file
        final String token = args[0];
        final Mongo mongo = new Mongo(args[1]);
        mongo.initMongo();
        final CommandListener cmdListener = new CommandListener(mongo);

        JDA jda = JDABuilder.createLight(token,
                GatewayIntent.GUILD_EMOJIS, GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS)
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(cmdListener)
                .build();

        jda.retrieveCommands()
                .queue(list -> {
                    List<CommandData> commandData =
                            cmdListener.retrieveCommandData();
                    commandData = commandData.stream()
                            .filter(c -> list.stream().map(Command::getName)
                                .collect(Collectors.toList()).contains(c.getName()))
                            .collect(Collectors.toList());
                    if(commandData.isEmpty()) return;

                    jda.updateCommands().addCommands(commandData)
                            .queue();
                });

        /*
         TODO:
            Startup Tasks
            Bot List Authentication
         */
    }
}
