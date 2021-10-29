package archivus.commands.generalcommands;

import archivus.commands.SlashCommand;
import archivus.commands.Type;
import archivus.mongo.Mongo;
import archivus.user.AccountDoesNotExistException;
import archivus.user.UserProfile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
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
        String userId = "";
        if(event.getOptions().isEmpty())
            userId = event.getUser().getId();
        else if(userOption != null)
            userId = userOption.getAsUser().getId();
        else if(userIDOption != null)
            userId = userIDOption.getAsString();

        event.getJDA().retrieveUserById(userId).queue(user -> {
            try {
                UserProfile userProfile = new UserProfile(mongo, user);
                userProfile.userEmbed().queue();
            } catch(AccountDoesNotExistException e){
                if(event.getOptions().isEmpty())
                    event.replyEmbeds(new EmbedBuilder().setTitle("Error ⛔")
                                                        .setDescription("You do not have a registered Archivus account")
                                                        .addField("Enter `/createaccount` to make one!",
                                                                "", false)
                                                        .build()).queue();
                else event.replyEmbeds(new EmbedBuilder().setTitle("Error ⛔")
                            .setDescription("This user does not have a registered Archivus account. " +
                                    "User: " + e.getTag() + " ~ ID: " + e.getUserID())
                            .addField("They can enter `/createaccount` to make one!",
                                    "", false)
                            .build()).queue();
            }
        }, e -> event.reply(
                "This discord user does not exist! Please confirm that the ID given is that of the user" +
                        " you are attempting to view!").queue());
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        String[] buttonId = event.getComponentId().split(":");
        if(!buttonId[0].equals(event.getUser().getId()))
            return;
        try {
            UserProfile userProfile = new UserProfile(mongo, event.getUser());
            userProfile.userEmbed().queue();
        } catch(AccountDoesNotExistException e){
                event.replyEmbeds(new EmbedBuilder().setTitle("Error ⛔")
                        .setDescription("You do not have a registered Archivus account")
                        .addField("Enter `/createaccount` to make one!",

                                "", false)
                        .build()).queue();
        }
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
