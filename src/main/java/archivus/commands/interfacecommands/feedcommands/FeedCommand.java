package archivus.commands.interfacecommands.feedcommands;

import archivus.commands.CommandType;
import archivus.commands.SlashCommand;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class FeedCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {

    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {

    }

    @Override
    public CommandData getData() {
        return new CommandData("feed", "Allows you to begin a feed");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Feed Command", event.getJDA().getSelfUser().getAvatarUrl())
                .setDescription("This command allows you to start a feed on archivus")
                .addField("/feed", "Opens the feed setup menu", false)
                .setFooter("Click to go to begin the feed setup!");
        return event.replyEmbeds(embed.build())
                .addActionRow(Button.success(event.getUser().getId()
                                        + ":post_post", "Open Menu")
                                .withEmoji(Emoji.fromUnicode("U+1F4DC")),
                        Button.link("https://imgflip.com/generatorcreator",
                                "Create Meme").withEmoji(Emoji.fromUnicode("U+1F923")));
    }

    @Override
    public CommandType getType() {
        return CommandType.FEED;
    }
}
