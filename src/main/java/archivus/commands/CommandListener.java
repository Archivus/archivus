package archivus.commands;

import archivus.commands.generalcommands.AccountCreationCommand;
import archivus.commands.generalcommands.HelpCommand;
import archivus.commands.generalcommands.ProfileCommand;
import archivus.commands.interfacecommands.PostCommand;
import archivus.commands.interfacecommands.ViewPostCommand;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CommandListener extends ListenerAdapter {
    public static final HashMap<String, SlashCommand> commands = new HashMap<String, SlashCommand>(){
        // Value: Command
        // Key: Name of Command
        {
            put("help", new HelpCommand());
            put("profile", new ProfileCommand());
            put("createaccount", new AccountCreationCommand());

            put("post", new PostCommand());
            put("viewpost", new ViewPostCommand());
        }
    };

    private final Mongo mongo;

    public CommandListener(Mongo mongo){
        this.mongo = mongo;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getGuild() == null)
            return;
        SlashCommand command = commands.get(event.getName());
        if(command == null) return;

        command.execute(event, mongo);
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        //Component ID structure nameOfCommand_customIdentifier
        String compID = event.getComponentId();
        String command = compID.substring(0, compID.indexOf('_'));

        SlashCommand slashCommand = commands.get(command);
        slashCommand.executeWithButton(event, mongo);
    }


    public List<CommandData> retrieveCommandData(){
        return commands.values()
                .stream()
                .map(SlashCommand::getData)
                .collect(Collectors.toList());
    }
}
