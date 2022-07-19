package archivus.commands;

import archivus.commands.generalcommands.CreateAccountCommand;
import archivus.commands.generalcommands.HelpCommand;
import archivus.commands.generalcommands.ProfileCommand;
import archivus.commands.interfacecommands.PostCommand;
import archivus.commands.interfacecommands.ViewPostCommand;
import archivus.commands.interfacecommands.feedcommands.CreateFeedCommand;
import archivus.mongo.Mongo;
import archivus.user.interaction.conversation.Conversation;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CommandListener extends ListenerAdapter {
    public static final HashMap<String, SlashCommand> commands = new HashMap<>(){
        // Value: Command
        // Key: Name of Command
        {
            put("help", new HelpCommand());
            put("profile", new ProfileCommand());
            put("create-account", new CreateAccountCommand());

            put("post", new PostCommand());
            put("view-post", new ViewPostCommand());

            put("create-feed", new CreateFeedCommand());
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
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;
        if(event.getMessage().getContentRaw().equals("amazing"))
            event.getGuild().updateCommands().addCommands(retrieveCommandData())
                    .queue();

        Conversation c = Conversation.conversations.get(event.getAuthor().getId());
        assert c != null;
        if(!c.hasrun) c.onResponse(event);
    }



    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        //Component ID structure userID:nameOfButton_customIdentifier
        String compID = event.getComponentId();
        String command = compID.substring(compID.indexOf(':')+1);
        if(command.contains("_"))
            command = command.substring(0, command.indexOf('_'));

        if(compID.endsWith("quit")) {
            Conversation.conversations.remove(event.getUser().getId());
            event.reply("Conversation ended!").queue();
            return;
        }
        else if(command.contains("conv")){
            Conversation c = Conversation.conversations.get(event.getUser().getId());
            if (c != null )c.conversationButton(event, mongo);
            return;
        }
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
