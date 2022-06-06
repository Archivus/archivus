package archivus.commands.generalcommands;

import archivus.Archivus;
import archivus.commands.CommandType;
import archivus.commands.SlashCommand;
import archivus.mongo.Mongo;
import archivus.user.UserProfile;
import archivus.user.interaction.conversation.Call;
import archivus.user.interaction.conversation.Conversation;
import archivus.user.interaction.conversation.ConversationAction;
import archivus.user.interaction.conversation.ConversationException;
import archivus.user.interaction.posting.PostTopic;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class CreateAccountCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        mongo.useClient(a -> {
            boolean exists = UserProfile.profileExists(a.getDatabase("account").getCollection("userdata"),
                    event.getUser().getId());
            if(exists)
                event.reply("I think you already have an account!").queue();
            else {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Archivus.colorPicker());
                embed.setTitle("Type your description with no Command Prefix or Ping! (100 character limit)")
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
                    new Conversation(finalAction, event.getUser().getId(), calls, mongo);
                } catch (IOException ex) {
                    embed.setAuthor("Set Your Description");
                    try {
                        new Conversation(finalAction, event.getUser().getId(), calls, mongo);
                    } catch (ConversationException e) {
                        event.reply("It seems as though you are already in a communication with Archivus, Click the " +
                                "button below to end it").addActionRow(
                                Button.primary(event.getUser().getId() + ":create-account_quit",
                                        "End Communication").withEmoji(Emoji.fromUnicode("U+274C"))).queue();
                    }

                    System.err.println("There's an issue with the path given to or data in the " +
                            "archivus_links.properties file");
                    ex.printStackTrace();
                } catch (ConversationException e){
                    event.reply("It seems as though you are already in a communication with Archivus, Click the " +
                            "button below to end it").addActionRow(
                            Button.primary(event.getUser().getId() + ":create-account_quit",
                                    "End Communication").withEmoji(Emoji.fromUnicode("U+274C"))).queue();
                } finally {
                    event.replyEmbeds(embed.build()).queue();
                }
            }
        }, event.getHook());
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        String[] buttonId = event.getComponentId().split(":");
        System.out.println(Arrays.toString(buttonId));
        if(!buttonId[0].equals(event.getUser().getId()))
            return;

        String[] data = buttonId[1].substring(buttonId[1].indexOf('_')+1).split("-");
        System.out.println(Arrays.toString(data));
        if(data[0].endsWith("topic")){
            PostTopic topic = PostTopic.toTopicByString(data[1]);
            Conversation con = Conversation.conversations.get(event.getUser().getId());
            ArrayList<Document> doc = con.tempDoc.containsKey("topics") ?
                    new ArrayList<>(con.tempDoc.getList("topics", Document.class)) : new ArrayList<>();
            doc.add(topic.toDoc());
            if (con.tempDoc.containsKey("topics"))
                con.tempDoc.replace("topics", doc);
            else con.tempDoc.put("topics", doc);


            Conversation.conversations.replace(event.getUser().getId(), con);
            event.editButton(Button.secondary(event.getUser().getId() + ":create-account_topicCLICKED-" + data[1],
                            data[1].substring(0, 1).toUpperCase() + data[1].substring(1))).queue();
        } else if(data[0].endsWith("topicCLICKED")){
            PostTopic topic = PostTopic.toTopicByString(data[1]);
            Conversation con = Conversation.conversations.get(event.getUser().getId());
            ArrayList<Document> doc = new ArrayList<>(con.tempDoc.getList("topics", Document.class));
            doc.remove(topic.toDoc());
            con.tempDoc.replace("topics", doc);

            System.out.println(con.tempDoc);
            Conversation.conversations.replace(event.getUser().getId(), con);
            event.editButton(Button.success(event.getUser().getId() + ":create-account_topic-" + data[1],
                    data[1].substring(0, 1).toUpperCase() + data[1].substring(1))).queue();
        } else if(data[0].endsWith("user")){
            Conversation con = Conversation.conversations.get(event.getUser().getId());
            ArrayList<String> doc = con.tempDoc.containsKey("following") ?
                    new ArrayList<>(con.tempDoc.getList("following", String.class)) : new ArrayList<>();
            doc.add(data[0]);



            con.tempDoc.replace("following", doc);

            Conversation.conversations.replace(event.getUser().getId(), con);
            event.editButton(Button.secondary(event.getUser().getId() + ":create-account_userCLICKED-" +
                    event.getUser().getId(), event.getUser().getAsTag())).queue();
        } else if(data[0].endsWith("userCLICKED")){
            Conversation con = Conversation.conversations.get(event.getUser().getId());
            ArrayList<String> doc = con.tempDoc.containsKey("following") ?
                    new ArrayList<>(con.tempDoc.getList("following", String.class)) : new ArrayList<>();
            doc.remove(data[0]);

            con.tempDoc.replace("following", doc);

            Conversation.conversations.replace(event.getUser().getId(), con);
            event.editButton(Button.success(event.getUser().getId() + ":create-account_user-" +
                    event.getUser().getId(), event.getUser().getAsTag())).queue();
        }

    }

    @Override
    public CommandData getData() {
        return new CommandData("create-account",
                "Allows you to create an account on Archivus");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Create Account Command", event.getJDA().getSelfUser().getAvatarUrl())
                .setDescription("This command allows you to create an account")
                .addField("/create-account", "Begin the process of creating an account", false)
                .setFooter("Click the button below to create your account");
        return event.replyEmbeds(embed.build())
                .addActionRow(Button.success(event.getUser().getId() + ":create-account_account",
                        "Create an Account")
                        .withEmoji(Emoji.fromUnicode("U+1F9D1")));
    }

    @Override
    public CommandType getType() {
        return CommandType.ACCOUNT;
    }

    private final ArrayList<Call> calls = new ArrayList<>(){
        {
            Path path = Paths.get("src/main/resources/archivus_links.properties");
            add(new Call(){
                @Override
                public void call(ButtonClickEvent event, Document doc, Mongo mongo) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker());
                    embed.setTitle("Type your description with no Command Prefix or @mention! (100 character limit)")
                            .setDescription("Express yourself with a short, snappy description. \n" +
                                    "Here is some inspiration to give you ideas!")
                            .addField("CCP Loyalty",
                                    "I do not recall anything significant happening on June 4th 1989", false)
                            .addField("Stuck in 2021",
                                    "Lamp oil? Rope? Bombs? You want it? It's yours my friend. As long as" +
                                            " have enough rupees.", false)
                            .setFooter("Make sure to only include what you wish to have on your profile! " +
                                    "No NSFW, discrimination or harmful material.");
                    try(InputStream input = Files.newInputStream(path)) {
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
                                .setThumbnail(prop.getProperty("archivus.confirm1_gif"));
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
            });

            add(new Call() {
                @Override
                public void call(ButtonClickEvent event, Document doc, Mongo mongo) {
                    doc.replace("topics", new ArrayList<>());
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker())
                            .setTitle("Choose 3 meme topics you like! Type 'Done' once you have finished " +
                                    "selecting!")
                            .setDescription("Your description: " + doc.getString("desc") + "\n\n" +
                                    "Tell us and the community the memes you most prefer! Your selections will" +
                                    " make up your personal feeds, and you will earn extra reputation for posting" +
                                    " memes with those topics!")
                            .addField("Shitpost", PostTopic.SHITPOST.data, true)
                            .addField("Dark", PostTopic.DARK.data, true)
                            .addField("Gaming", PostTopic.GAMING.data, true)
                            .addField("Anime", PostTopic.ANIME.data, true)
                            .addField("Relatable", PostTopic.RELATABLE.data, true)
                            .setFooter("Remember to chose 3 memes and type 'Done' only once you have finished");

                    try(InputStream input = Files.newInputStream(path)){
                        Properties prop = new Properties();
                        prop.load(input);

                        embed.setAuthor("Select 3 Meme Topics", prop.getProperty("archivus.pfp"))
                                .setThumbnail(prop.getProperty("archivus.doom"));
                    } catch (IOException ex){
                        embed.setAuthor("Select 3 meme Topics");
                        System.err.println("There's an issue with the path given to or data in the " +
                                "archivus_links.properties file");
                        ex.printStackTrace();
                    } finally {

                        event.getMessage().replyEmbeds(embed.build())
                                .setActionRow(
                                        Button.success(event.getUser().getId() +
                                                ":create-account_topic-shitpost", "Shitpost"),
                                        Button.success(event.getUser().getId() +
                                                ":create-account_topic-dark", "Dark"),
                                        Button.success(event.getUser().getId() +
                                                ":create-account_topic-gaming", "Gaming"),
                                        Button.success(event.getUser().getId() +
                                                ":create-account_topic-anime", "Anime"),
                                        Button.success(event.getUser().getId() +
                                                ":create-account_topic-relatable", "Relatable")).queue();
                    }
                }

                @Override
                public Document confirmation(GuildMessageReceivedEvent event, Document doc, Mongo mongo) {
                    ArrayList<Document> topics = new ArrayList<>(doc.getList("topics", Document.class));
                    if (!event.getMessage().getContentRaw().toLowerCase().contains("done"))
                        return null;
                    if(topics.size()!= 3){
                        StringBuilder str = new StringBuilder();
                        for (Document doc1: topics)
                            str.append(doc1.getString("name")).append(" ");
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setColor(Archivus.colorPicker());
                        embed.setTitle("Bruh. You've gotta choose 3 topics not only " + topics.size() + ".")
                                .setDescription("You chose " + str + ", you need to choose " +
                                        (3 - topics.size()) + " more topic(s) in order to continue the process.")
                                .addField("Fun Fact", Archivus.funFact(), false)
                                .setFooter("Select the button below to re-choose!");

                        try(InputStream input = Files.newInputStream(path)){
                            Properties prop = new Properties();
                            prop.load(input);

                            embed.setAuthor("Error in Selecting Topics", prop.getProperty("archivus.pfp"))
                                    .setThumbnail(prop.getProperty("archivus.error2_gif"));
                        } catch (IOException ex){
                            embed.setAuthor("Error in Selecting Topics");
                            System.err.println("There's an issue with the path given to or data in the " +
                                    "archivus_links.properties file");
                            ex.printStackTrace();
                        } finally {
                            doc.replace("topics", new ArrayList<Document>());
                            event.getMessage().replyEmbeds(embed.build())
                                    .setActionRow(Button.primary(event.getAuthor().getId() + ":conv_no",
                                            "Redo").withEmoji(Emoji.fromUnicode("U+1F504"))).queue();
                        }

                        return doc;
                    }
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker());
                    embed.setTitle("Confirm the topics you've chosen!")
                            .setDescription("You've chosen 3 topics you would like, now confirm that they are" +
                                    " your favourite topics");

                    PostTopic topic1 = PostTopic.toTopicByDoc(topics.get(0));
                    embed.addField("1." + topic1.toString().charAt(0) +
                                    topic1.toString().substring(1).toLowerCase(),
                            topic1.data, false);
                    PostTopic topic2 = PostTopic.toTopicByDoc(topics.get(1));
                    embed.addField("2." + topic2.toString().charAt(0) +
                                    topic2.toString().substring(1).toLowerCase(),
                            topic2.data, false);
                    PostTopic topic3 = PostTopic.toTopicByDoc(topics.get(2));
                    embed.addField("3." + topic3.toString().charAt(0) +
                                    topic3.toString().substring(1).toLowerCase(),
                            topic3.data, false);
                    embed.setFooter("You can always change this later in the account management menu.");


                    try(InputStream input = Files.newInputStream(path)){
                        Properties prop = new Properties();
                        prop.load(input);

                        embed.setAuthor("Confirm Topics", prop.getProperty("archivus.pfp"))
                                .setThumbnail(prop.getProperty("archivus.confirm2_gf"));
                    } catch (IOException ex){
                        embed.setAuthor("Error in Selecting Topics");
                        System.err.println("There's an issue with the path given to or data in the " +
                                "archivus_links.properties file");
                        ex.printStackTrace();
                    } finally {
                        event.getMessage().replyEmbeds(embed.build())
                                .setActionRow(Button.success(event.getAuthor().getId() + ":conv_yes",
                                                "Sure").withEmoji(Emoji.fromUnicode("U+1F44D")),
                                        Button.danger(event.getAuthor().getId() + ":conv_no",
                                                "Sumn Ain't Right").withEmoji(Emoji.fromUnicode("U+1F44E"))).queue();
                    }
                    return doc;
                }
            });

//            add(new Call() {
//                @Override
//                public void call(ButtonClickEvent event, Document doc, Mongo mongo) {
//
//                    mongo.useClient(client -> {
//                        EmbedBuilder embed = new EmbedBuilder();
//                        embed.setColor(Archivus.colorPicker());
//                        embed.setTitle("Almost Done! Now choose who to follow from this list of Archivus users!")
//                                .setDescription("Here is a list of people of popular Archivus users!");
//
//                        MongoCollection<Document> docs = client
//                                .getDatabase("account")
//                                .getCollection("userdata");
//
//                        List<Button> components = new ArrayList<>();
//                        for (Document docUser : docs.find().sort(Sorts.descending("reputation"))) {
//                            UserProfile user = new UserProfile(docUser);
//                            embed.addField(user.getTag(), user.getDescription(), true);
//                            components.add(Button.success(event.getUser().getId() +
//                                    ":create-account_user-" + user.getUserId(), user.getTag()));
//                            if(embed.getFields().size() >= 5) break;
//                        }
//
//                        embed.setFooter("There are many more people to follow in the Archivus Community!");
//
//                        try(InputStream input = new FileInputStream("src/main/resources/archivus_links.properties")){
//                            Properties prop = new Properties();
//                            prop.load(input);
//
//                            embed.setAuthor("Select Users To Follow", prop.getProperty("archivus.pfp"))
//                                    .setThumbnail(prop.getProperty("archivus.crying_gif"));
//                        } catch (IOException ex){
//                            embed.setAuthor("Select Users To Follow");
//                            System.err.println("There's an issue with the path given to or data in the " +
//                                    "archivus_links.properties file");
//                            ex.printStackTrace();
//                        } finally {
//                            event.getMessage().replyEmbeds(embed.build())
//                                    .setActionRow(components).queue();
//                        }
//                    }, event.getHook());
//
//                }
//
//                @Override
//                public Document confirmation(GuildMessageReceivedEvent event, Document doc, Mongo mongo) {
//                    ArrayList<Document> followers = new ArrayList<>(doc.getList("followers", Document.class));
//
//                    EmbedBuilder embed = new EmbedBuilder();
//                    embed.setColor(Archivus.colorPicker());
//                    embed.setTitle("Confirm the people you've followed!")
//                            .setDescription("You've chosen people you would like to follow, now confirm that they are" +
//                                    " you actually do want to follow them!");
//
//                    embed.addField("1. " + followers.get(0).getString("tag"),
//                            followers.get(0).getString("desc"), false);
//                    embed.addField("2. " + followers.get(1).getString("tag"),
//                            followers.get(1).getString("desc"), false);
//                    embed.addField("3. " + followers.get(2).getString("tag"),
//                            followers.get(2).getString("desc"), false);
//
//                    embed.setFooter("You can always unfollow these posers, *scoffs*.");
//
//
//                    try(InputStream input = new FileInputStream("src/main/resources/archivus_links.properties")){
//                        Properties prop = new Properties();
//                        prop.load(input);
//
//                        embed.setAuthor("Confirm Followers", prop.getProperty("archivus.pfp"))
//                                .setThumbnail(prop.getProperty("archivus.confirm1_gif"));
//                    } catch (IOException ex){
//                        embed.setAuthor("Confirm Followers");
//                        System.err.println("There's an issue with the path given to or data in the " +
//                                "archivus_links.properties file");
//                        ex.printStackTrace();
//                    } finally {
//                        event.getMessage().replyEmbeds(embed.build())
//                                .setActionRow(Button.success(event.getAuthor().getId() + ":conv_yes",
//                                        "Yeah, they cool").withEmoji(Emoji.fromUnicode("U+1F44D")),
//                                        Button.danger(event.getAuthor().getId() + ":conv_no",
//                                        "Hol' up!").withEmoji(Emoji.fromUnicode("U+1F44E"))).queue();
//                    }
//                    return doc;
//                }
//            });
        }
    };

    private final ConversationAction finalAction = (event, conversation, mongo) -> {
        Document doc = conversation.doc;

        Document userDoc = new Document("userId", event.getUser().getId())
                .append("guildId", event.getUser().getId())
                .append("tag", event.getUser().getAsTag())
                .append("desc", doc.getString("desc"))
                .append("reputation", 0)
                .append("archives", 0)
                .append("topics", doc.getList("topics", Document.class));

        mongo.useClient(client -> {
            UserProfile userProfile = new UserProfile(userDoc,
                    client.getDatabase("account")
                            .getCollection("userdata"));
            event.replyEmbeds(userProfile.userEmbed(event.getJDA().getSelfUser().getAvatarUrl(),
                    event.getUser().getAvatarUrl())).queue();
        }, event.getHook());

    };
}
