
package archivus.commands.generalcommands;

import archivus.Archivus;
import archivus.commands.SlashCommand;
import archivus.commands.CommandType;
import archivus.mongo.Mongo;
import archivus.user.AccountDoesNotExistException;
import archivus.user.UserProfile;
import archivus.user.interaction.conversation.Call;
import archivus.user.interaction.conversation.Conversation;
import archivus.user.interaction.conversation.ConversationException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

public class ProfileCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo){
        // Have a user ID
        OptionMapping userOption = event.getOption("user");
        OptionMapping userIDOption = event.getOption("id");
        String userId = "";

        event.deferReply().queue();

        // Assign user based on...
        // If there was no option given, i.e, the user wants to view their own profile
        if(event.getOptions().isEmpty())
            userId = event.getUser().getId();
        // If a user was given as a selected member of a server
        else if(userOption != null)
            userId = userOption.getAsMentionable().getId();
        // If a user ID was given
        else if(userIDOption != null)
            userId = userIDOption.getAsString();


        // Query user data from user ID and display the account given
        event.getJDA().retrieveUserById(userId).queue(user -> {
            mongo.useClient(client -> {
                try {
                    InteractionHook hook = event.getHook();
                    hook.setEphemeral(true);
                    UserProfile userProfile = new UserProfile(client.getDatabase("account")
                            .getCollection("userdata"), user);
                    hook.sendMessageEmbeds(userProfile.userEmbed(event.getJDA().getSelfUser().getAvatarUrl(),
                            user.getAvatarUrl())).addActionRow(
                            Button.success(event.getUser().getId() + ":profile_changedesc", "Change Description")
                                    .withEmoji(Emoji.fromUnicode("U+1F504"))).queue();
                } catch(AccountDoesNotExistException e) {
                    // Assign error message based on option
                    if (event.getOptions().isEmpty()) {
                        // This is a special webhook that allows you to send messages without
                        // having permissions in the channel and also allows ephemeral messages
                        InteractionHook hook = event.getHook();
                        hook.setEphemeral(true);
                        hook.sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("Error ⛔")
                                .setColor(Archivus.colorPicker())
                                .setDescription("You do not have a registered Archivus account")
                                .addField("Enter `/createaccount create` to make one!",
                                        "", false)
                                .build()).queue();
                    } else {
                        InteractionHook hook = event.getHook();
                        hook.setEphemeral(true);
                        hook.sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("Error ⛔")
                                        .setColor(Archivus.colorPicker())
                                .setDescription("This user does not have a registered Archivus account. " +
                                        "User: " + e.getTag() + " ~ ID: " + e.getUserID())
                                .addField("They can enter `/createaccount` to make one!",
                                        "", false)
                                .build()).queue();
                    }
                }
            }, event.getHook());
        }, e -> event.reply(
                "This discord user does not exist! Please confirm that the ID given is that of the user" +
                        " you are attempting to view!").queue());
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        // Verify the interaction is coming from the user who ran the help command
        String[] buttonId = event.getComponentId().split(":");
        if(!buttonId[0].equals(event.getUser().getId()))
            return;


        if(buttonId[1].contains("changedesc")){
                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Archivus.colorPicker());
                embed.setTitle("Reply to this message with your description!  (100 character limit)")
                    .setDescription("Express yourself with a short, snappy description. \n" +
                            "Here is some inspiration to give you ideas!")
                    .addField("CCP Loyalty",
                            "I do not recall anything significant happening on June 4th 1989", false)
                    .addField("Stuck in 2021",
                            "Lamp oil? Rope? Bombs? You want it? It's yours my friend. As long as" +
                                    " have enough rupees.", false)
                    .setFooter("Make sure to only include what you wish to have on your profile! " +
                            "No NSFW, discrimination or harmful material.");
                try {
                    new Conversation((e, c, m) ->

                        m.useClient(client -> {
                            try {
                                UserProfile userProfile = new UserProfile(client.getDatabase("account")
                                        .getCollection("userdata"), event.getUser());
                                userProfile.setDescription(c.conversationDataDocument.getString("desc"));
                                userProfile.updateProfile(client.getDatabase("account")
                                        .getCollection("userdata"), e.getHook());
                                e.reply("Description successfully changed! ✅").queue();
                            }catch (AccountDoesNotExistException ex) {
                                InteractionHook hook = event.getHook();
                                hook.setEphemeral(true);
                                EmbedBuilder embed1 = new EmbedBuilder();
                                embed1.setAuthor("Error ⛔", null, event.getJDA().getSelfUser().getAvatarUrl());
                                embed1.setTitle("There seems to be an error in your attempt to communicate with Archivus! ⛔");
                                embed1.setDescription("The cause seems to be that you don't have an account with " +
                                        "Archivus. Please press the button below to begin the process " +
                                        "to create an account");
                                embed1.setFooter("User ID: " + event.getUser().getId());
                                Path path = Paths.get("src/main/resources/archivus_links.properties");
                                try(InputStream input = Files.newInputStream(path)) {
                                    Properties prop = new Properties();
                                    prop.load(input);


                                    embed1.setThumbnail(prop.getProperty("archivus.error1_gif"));
                                } catch (IOException e1){
                                    System.err.println("Error in links properties file. archivus.error3_gif");
                                } finally {
                                    event.replyEmbeds(embed1.build()).addActionRow(
                                            Button.success(event.getUser().getId() + ":create-account_account",
                                                    "Create Account")
                                                    .withEmoji(Emoji.fromUnicode("U+1F9D1"))).queue();
                                }
                            }
                        }, event.getHook())
                    , event.getUser().getId(), new ArrayList<>(Collections.singleton(new Call(){
                        @Override
                        public void call(ButtonClickEvent event, Document doc, Mongo mongo) {
                            EmbedBuilder embed = new EmbedBuilder();
                            embed.setColor(Archivus.colorPicker());
                            embed.setTitle("Reply to this message with your description!  (100 character limit)")
                                    .setDescription("Express yourself with a short, snappy description. \n" +
                                            "Here is some inspiration to give you ideas!")
                                    .addField("CCP Loyalty",
                                            "I do not recall anything significant happening on June 4th 1989", false)
                                    .addField("Stuck in 2021",
                                            "Lamp oil? Rope? Bombs? You want it? It's yours my friend. As long as" +
                                                    " have enough rupees.", false)
                                    .setFooter("Make sure to only include what you wish to have on your profile! " +
                                            "No NSFW, discrimination or harmful material.");
                            try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_links.properties"))) {
                                Properties prop = new Properties();
                                // load a properties file
                                prop.load(input);

                                embed.setAuthor("Set Your Description", prop.getProperty("archivus.pfp"))
                                        .setThumbnail(prop.getProperty("archivus.typing_gif"));
                            } catch (IOException ex) {
                                embed.setAuthor("Set Your Description");

                                System.err.println("There's an issue with the path given to or data in the " +
                                        "archivus_links.properties file");
                                ex.printStackTrace();
                            } finally {
                                event.replyEmbeds(embed.build()).queue();
                            }
                        }

                        @Override
                         public Document confirmation(GuildMessageReceivedEvent event, Document doc, Mongo mongo) {
                            String desc = event.getMessage().getContentRaw();
                            Path path = Paths.get("src/main/resources/archivus_links.properties");
                            if(event.getMessage().getReferencedMessage() == null
                                    || !event.getMessage().getReferencedMessage().getAuthor().getId().
                                    equals(event.getJDA().getSelfUser().getId())) {
                                System.out.println(event.getMessage().getReferencedMessage().getAuthor().getId());
                                return null;
                            }
                            if(desc.toCharArray().length > 100){
                                EmbedBuilder embed = new EmbedBuilder();
                                embed.setColor(Archivus.colorPicker());
                                embed.setTitle("Bruh. You messed up with your description, it seems it exceeded the limit " +
                                                "of 100 characters by " + (100 - desc.toCharArray().length))
                                        .setDescription("Try removing a few words or letters, or maybe try somerthing " +
                                                "new altogether? \nHere are some more ideas for inspiration!")
                                        .addField("Willem Dripfoe",
                                                "This is, uh, some sport thing, I don't know",
                                                false)
                                        .addField("Cursed Mr. Beast",
                                                "Today, we are forcing 100 DISEASED children to fight to the death for MEDICAL TREATMENT!",
                                                false)
                                        .setFooter("Click the 'Redo' button to remake your description.");


                                try(InputStream input = Files.newInputStream(path)) {
                                    Properties prop = new Properties();
                                    // load a properties file
                                    prop.load(input);

                                    embed.setAuthor("Error In Description", prop.getProperty("archivus.pfp"))
                                            .setThumbnail(prop.getProperty("archivus.error1_gif"));
                                } catch (IOException ex) {
                                    embed.setAuthor("Error In Description");
                                    System.err.println("There's an issue with the path given to or data in the " +
                                            "archivus_links.properties file");
                                    ex.printStackTrace();
                                } finally {

                                    event.getMessage().replyEmbeds(embed.build())
                                            .setActionRow(Button.primary(event.getAuthor().getId() + ":conv_no",
                                                    "Redo").withEmoji(Emoji.fromUnicode("U+1F504"))).queue();
                                }
                                return doc;
                            }

                            doc.put("desc", event.getMessage().getContentRaw());

                            EmbedBuilder embed = new EmbedBuilder();
                            embed.setColor(Archivus.colorPicker());
                            embed.setTitle("Confirm that this is the description you would like to have on your profile!")
                                    .setDescription(event.getMessage().getContentRaw())
                                    .setFooter("Press a button below to continue! If you later change your mind, who can " +
                                            "always change your description in the Manage Account Menu!")
                                    .addField("Fun Fact!",
                                            Archivus.funFact(),
                                            false);

                            try(InputStream input = Files.newInputStream(path)){
                                Properties prop = new Properties();
                                prop.load(input);

                                embed.setAuthor("Confirm Your Description", prop.getProperty("archivus.pfp"))
                                        .setThumbnail(prop.getProperty("archivus.confirm2_gif"));
                            } catch (IOException ex){
                                embed.setAuthor("Confirm Your Description");
                                System.err.println("There's an issue with the path given to or data in the " +
                                        "archivus_links.properties file");
                                ex.printStackTrace();
                            } finally {
                                event.getMessage().replyEmbeds(embed.build())
                                        .setActionRow(Button.success(event.getAuthor().getId() + ":conv_yes",
                                                        "Yeah!").withEmoji(Emoji.fromUnicode("U+1F44D")),
                                                Button.danger(event.getAuthor().getId() + ":conv_no",
                                                        "Nah!").withEmoji(Emoji.fromUnicode("U+1F44E"))).queue();
                            }
                            return doc;
                        }
                    })), mongo, event.getChannel());
                    event.replyEmbeds(embed.build()).queue();
                } catch (ConversationException e){
                    Path path = Paths.get("src/main/resources/archivus_links.properties");
                    EmbedBuilder embed1 = new EmbedBuilder();
                    embed1.setAuthor("Error ⛔", null, event.getJDA().getSelfUser().getAvatarUrl());
                    embed1.setTitle("There seems to be an error in your attempt to communicate with Archivus! ⛔");
                    embed1.setDescription("The cause seems to be that you're already in a communication with " +
                            "Archivus. Please press the button below to end the communication and restart the " +
                            "account creation process.");
                    embed1.setFooter("User ID: " + event.getUser().getId());
                    try(InputStream input = Files.newInputStream(path)) {
                        Properties prop = new Properties();
                        prop.load(input);

                        embed1.setThumbnail(prop.getProperty("archivus.error3_gif"));
                    } catch (IOException e1){
                        System.err.println("Error in links properties file. archivus.error3_gif");
                    }
                    event.replyEmbeds(embed1.build()).addActionRow(
                            Button.primary(event.getUser().getId() + ":create-account_quit",
                                    "End Communication").withEmoji(Emoji.fromUnicode("U+274C"))).queue();

                }
        } else if(buttonId[1].contains("quit")){
            Conversation.conversations.remove(event.getUser().getId());
            event.reply("Your communication with Archivus has ended!").queue();
        } else {
            mongo.useClient(client -> {
                try {
                    // Show profile of the user
                    UserProfile userProfile = new UserProfile(client.getDatabase("account")
                            .getCollection("userdata"), event.getUser());
                    event.replyEmbeds(userProfile.userEmbed(event.getJDA().getSelfUser().getAvatarUrl(),
                            event.getUser().getAvatarUrl())).queue();
                } catch (AccountDoesNotExistException e) {
                    Path path = Paths.get("src/main/resources/archivus_links.properties");
                    InteractionHook hook = event.getHook();
                    hook.setEphemeral(true);

                    EmbedBuilder embed1 = new EmbedBuilder();
                    embed1.setAuthor("Error ⛔", null, event.getJDA().getSelfUser().getAvatarUrl());
                    embed1.setTitle("There seems to be an error in your attempt to communicate with Archivus! ⛔");
                    embed1.setDescription("The cause seems to be that you don't have an account with " +
                            "Archivus. Please press the button below to begin the process " +
                            "to create an account");
                    embed1.setFooter("User ID: " + event.getUser().getId());
                    try(InputStream input = Files.newInputStream(path)) {
                        Properties prop = new Properties();
                        prop.load(input);
                        embed1.setThumbnail(prop.getProperty("archivus.error1_gif"));
                    } catch (IOException e1){
                        System.err.println("Error in links properties file. archivus.error3_gif");
                    } finally {
                        hook.sendMessageEmbeds(embed1.build()).queue();
                    }

                }
            }, event.getHook());
        }
    }

    @Override
    public CommandData getData() {
        return new CommandData("profile", "Allows you to view your own or another users profile")
                .addOption(OptionType.MENTIONABLE, "user", "Mention a user in the server")
                .addOption(OptionType.STRING, "id", "Enter the ID of a user.");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Profile Command", null, event.getJDA().getSelfUser().getAvatarUrl())
             .setColor(Archivus.colorPicker())
             .setDescription("This command allows you to view your own or another persons profile.")
             .addField("/profile @mention", "Mention a user in the server", false)
             .addField("/profile userID", "Enter a users ID and view their profile", false)
             .addField("/profile", "View your own profile", false)
             .setFooter("Click the button below to view your own profile");
        return event.replyEmbeds(embed.build())
                .addActionRow(Button.success(event.getUser().getId() + ":profile_myprofile", "My Profile")
                    .withEmoji(Emoji.fromUnicode("U+1F9D1")));
    }

    @Override
    public CommandType getType() {
        return CommandType.ACCOUNT;
    }
}
