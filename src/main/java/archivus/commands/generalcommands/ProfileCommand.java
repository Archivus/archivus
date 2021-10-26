package archivus.commands.generalcommands;

import archivus.commands.SlashCommand;
import archivus.commands.Type;
import archivus.mongo.Mongo;
import archivus.user.UserProfile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class ProfileCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo){
        OptionMapping userOption = event.getOption("user");
        OptionMapping userIDOption = event.getOption("userID");
        if(event.getOptions().isEmpty()){
            UserProfile profile = new UserProfile();
            profile.retrieveProfile(mongo, event.getUser());
            profile.userEmbed().queue();
        } else if(userOption != null){
            User user = userOption.getAsUser();
            UserProfile userProfile = new UserProfile();
            userProfile.retrieveProfile(mongo, user);
            userProfile.userEmbed().queue();
        } else if(userIDOption != null){
            String userID = userIDOption.getAsString();
            event.getJDA().retrieveUserById(userID).queue(user -> {
                UserProfile userProfile = new UserProfile();
                userProfile.retrieveProfile(mongo, user);
                userProfile.userEmbed().queue();
            }, e -> event.reply(
                    "This user does not exist! Please confirm that the ID given is that of the user" +
                    " you are attempting to view!").queue());
        }
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        String[] buttonId = event.getComponentId().split(":");
        if(!buttonId[0].equals(event.getUser().getId()))
            return;
        UserProfile profile = new UserProfile();
        profile.retrieveProfile(mongo, event.getUser());
        profile.userEmbed().queue();
    }

    @Override
    public CommandData getData() {
        return new CommandData("profile", "Allows you to view your own or another users profile")
                .addOption(OptionType.USER, "user", "Mention a user in the server")
                .addOption(OptionType.STRING, "userID", "Enter the ID of a user.");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Profile Command", event.getJDA().getSelfUser().getAvatarUrl())
             .setDescription("This command allows you to view your own or another persons profile.")
             .addField("/profile @mention", "Mention a user in the server", false)
             .addField("/profile userID", "Enter a users ID and view their profile", false)
             .addField("/profile", "View your own profile", false)
             .setFooter("Click the button below to view your own profile");
        return event.replyEmbeds(embed.build())
                .addActionRow(Button.success(event.getUser().getId() + ":myprofile_profile", "My Profile")
                    .withEmoji(Emoji.fromUnicode("U+1F9D1")));
    }

    @Override
    public Type getType() {
        return Type.ACCOUNT;
    }
}
