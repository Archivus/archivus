package archivus.commands.generalcommands.funcommands;

import archivus.Archivus;
import archivus.commands.CommandType;
import archivus.commands.SlashCommand;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ShootCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        Path path = Paths.get("src/main/resources/funcommandgifs/shoot.properties");
        try (InputStream input = Files.newInputStream(path)){
            Properties prop = new Properties();
            prop.load(input);

            //Gen Random Property
            int randProp = (int)Math.floor(Math.random()*(3-1+1)+1);

            event.reply(prop.getProperty("shoot." + randProp)).queue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {

    }

    @Override
    public CommandData getData() {
        return new CommandData("shoot", "blow those mf brains out");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Shoot Command", null, event.getJDA().getSelfUser().getAvatarUrl())
                .setColor(Archivus.colorPicker())
                .setDescription("Get a random anime girl to shoot someone")
                .addField("/shoot", "", false);
        return event.replyEmbeds(embed.build());
    }

    @Override
    public CommandType getType() {
        return CommandType.FUN;
    }
}
