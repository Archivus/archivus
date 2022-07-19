package archivus.commands.interfacecommands;

import archivus.Archivus;
import archivus.commands.SlashCommand;
import archivus.commands.CommandType;
import archivus.mongo.Mongo;
import archivus.user.interaction.conversation.Call;
import archivus.user.interaction.conversation.Conversation;
import archivus.user.interaction.conversation.ConversationAction;
import archivus.user.interaction.conversation.ConversationException;
import archivus.user.interaction.posting.Post;
import archivus.user.interaction.posting.PostTopic;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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
import java.util.List;
import java.util.Properties;

public class PostCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        // Request Post Title
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Archivus.colorPicker());
        embed.setAuthor("Set Post Image", null, event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTitle("Send an image for your post!");
        embed.setDescription("The post must be a copied image or image file. Archivus is in Alpha release and " +
                "cannot yet securely process links. \n\n" +
                "Here are some tips to help you make a great Archivus post!");
        embed.addField("Try not to follow Trends",
                "Following the memes that are trendy is ok, but being as original (but still funny) " +
                        "as possible will give you a better chance of being Archived!", false);
        embed.addField("Post relevant memes",
                "Try to post memes that most people will understand, or that is relevant to the current topic " +
                        "you've posted in. Avoid posting niche memes if your goal is to be archived.", false);
        embed.addField("Do you like it?",
                "Ask yourself. Is this funny? Is this appropriate? If you saw this, would you upvote it?" +
                        " If you say yes to these types of questions, chances are your post might do well.", false);
        try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_links.properties"))){
            Properties prop = new Properties();
            // load a properties file
            prop.load(input);
            embed.setThumbnail(prop.getProperty("archivus.crying_gif"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                new Conversation(finalAction, event.getUser().getId(), calls, mongo, event.getChannel());
                event.replyEmbeds(embed.build()).queue();
            } catch (ConversationException e) {
                event.reply("It seems as though you are already in a communication with Archivus, Click the " +
                        "button below to end it").addActionRow(
                        Button.primary(event.getUser().getId() + ":post_quit",
                                "End Communication").withEmoji(Emoji.fromUnicode("U+274C"))).queue();
            }
        }

    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        String[] buttonId = event.getComponentId().split(":");
        if(!buttonId[0].equals(event.getUser().getId()))
            return;

        String[] data = buttonId[1].substring(buttonId[1].indexOf('_')+1).split("-");
        if(data[0].endsWith("topic")){
            PostTopic topic = PostTopic.toTopicByString(data[1]);
            Conversation con = Conversation.conversations.get(event.getUser().getId());
            ArrayList<Document> doc = con.tempDataDocument.containsKey("topics") ?
                    new ArrayList<>(con.tempDataDocument.getList("topics", Document.class)) : new ArrayList<>();
            doc.add(topic.toDoc());
            if (con.tempDataDocument.containsKey("topics"))
                con.tempDataDocument.replace("topics", doc);
            else con.tempDataDocument.put("topics", doc);


            Conversation.conversations.replace(event.getUser().getId(), con);
            event.editButton(Button.secondary(event.getUser().getId() + ":post_topicCLICKED-" + data[1],
                    data[1].substring(0, 1).toUpperCase() + data[1].substring(1))).queue();
        } else if(data[0].endsWith("topicCLICKED")){
            PostTopic topic = PostTopic.toTopicByString(data[1]);
            Conversation con = Conversation.conversations.get(event.getUser().getId());
            ArrayList<Document> doc = new ArrayList<>(con.tempDataDocument.getList("topics", Document.class));
            doc.remove(topic.toDoc());
            con.tempDataDocument.replace("topics", doc);

            System.out.println(con.tempDataDocument);
            Conversation.conversations.replace(event.getUser().getId(), con);
            event.editButton(Button.success(event.getUser().getId() + ":post_topic-" + data[1],
                    data[1].substring(0, 1).toUpperCase() + data[1].substring(1))).queue();
        }
    }

    @Override
    public CommandData getData() {
        return new CommandData("post", "Allows you to post to the Archivus community");

    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Post Command", event.getJDA().getSelfUser().getAvatarUrl())
                .setDescription("This command allows you to begin the process to post on archivus")
                .addField("/post", "Opens the post setup menu", false)
                .setFooter("Click to go to open the menu or Create your own meme!");
        return event.replyEmbeds(embed.build())
                .addActionRow(Button.success(event.getUser().getId()
                                + ":post_post", "Open Menu")
                                .withEmoji(Emoji.fromUnicode("U+1F4DC")),
                        Button.link("https://imgflip.com/generatorcreator",
                                "Create Meme").withEmoji(Emoji.fromUnicode("U+1F923")));
    }

    @Override
    public CommandType getType() {
        return CommandType.POSTING;
    }

    ConversationAction finalAction = (event, conversation, mongo) -> mongo.useClient(client -> {
        Document doc = conversation.conversationDataDocument;
        Post post = new Post(doc.getString("imgUrl"), doc.getString("title"), event.getUser().getId(),
                doc.getList("topics", Document.class), event.getUser().getAsTag());


        post.listPost(client.getDatabase("post")
                .getCollection("availableposts"));
        event.replyEmbeds(post.postEmbed(event.getUser().getId(), true).build()).queue();
    }, event.getHook());

    private final ArrayList<Call> calls = new ArrayList<>() {
        {
            add(new Call() {
                @Override
                public void call(ButtonClickEvent event, Document doc, Mongo mongo) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker());
                    embed.setAuthor("Set Post Image", null, event.getJDA().getSelfUser().getAvatarUrl());
                    embed.setTitle("Send an image for your post!");
                    embed.setDescription("The post must be a copied image or image file. Archivus is in Alpha release and " +
                            "cannot yet securely process links. \n\n" +
                            "Here are some tips to help you make a great Archivus post!");
                    embed.addField("Try not to follow Trends",
                            "Following the memes that are trendy is ok, but being as original (but still funny) " +
                                    "as possible will give you a better chance of being Archived!", false);
                    embed.addField("Post relevant memes",
                            "Try to post memes that most people will understand, or that is relevant to the current topic " +
                                    "you've posted in. Avoid posting niche memes if your goal is to be archived.", false);
                    embed.addField("Do you like it?",    
                            "Ask yourself. Is this funny? Is this appropriate? If you saw this, would you upvote it?" +
                                    " If you say yes to these types of questions, chances are your post might do well.", false);
                    try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_links.properties"))){
                        Properties prop = new Properties();
                        // load a properties file
                        prop.load(input);
                        embed.setThumbnail(prop.getProperty("archivus.crying_gif"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        event.replyEmbeds(embed.build()).queue();
                    }
                }

                @Override
                public Document confirmation(GuildMessageReceivedEvent event, Document doc, Mongo mongo) {
                    List<Message.Attachment> attachments = event.getMessage().getAttachments();
                    Path path = Paths.get("src/main/resources/archivus_links.properties");
                    if (attachments.isEmpty() || !attachments.get(0).isImage()){
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setAuthor("Error In Image", event.getJDA().getSelfUser().getAvatarUrl());
                        embed.setColor(Archivus.colorPicker());
                        embed.setTitle("Bruh. You need to give a valid attachment.")
                                .setDescription("Archivus is currently in Alpha release, so links do not work. Make " +
                                        "sure you are giving a valid image.")
                                .setFooter("Click the 'Redo' button to redo your image.");


                        try(InputStream input = Files.newInputStream(path)) {
                            Properties prop = new Properties();
                            // load a properties file
                            prop.load(input);

                            embed.setThumbnail(prop.getProperty("archivus.error1_gif"));
                        } catch (IOException ex) {
                            embed.setAuthor("Error In Image");

                            System.err.println("There's an issue with the path given to or data in the " +
                                    "archivus_links.properties file");
                            ex.printStackTrace();
                        } finally {
                            Conversation con = Conversation.conversations.get(event.getAuthor().getId());
                            con.hasrun = true;
                            Conversation.conversations.replace(event.getAuthor().getId(), con);
                            event.getMessage().replyEmbeds(embed.build())
                                    .setActionRow(Button.primary(event.getAuthor().getId() + ":conv_no",
                                            "Redo").withEmoji(Emoji.fromUnicode("U+1F504"))).queue();
                        }
                        return doc;
                    }

                    Message.Attachment attachment = attachments.get(0);

                    doc.put("imgUrl", attachment.getUrl());

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker());
                    embed.setTitle("Confirm that this is the image you would like to use for your post!")
                            .setImage(attachment.getUrl())
                            .setFooter("Press a button below to continue!")
                            .addField("Fun Fact!",
                                    Archivus.funFact(),
                                    false);

                    try(InputStream input = Files.newInputStream(path)){
                        Properties prop = new Properties();
                        prop.load(input);

                        embed.setAuthor("Confirm Your Image", prop.getProperty("archivus.pfp"))
                                .setThumbnail(prop.getProperty("archivus.confirm1_gif"));
                    } catch (IOException ex){
                        embed.setAuthor("Confirm Your Image");
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
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker());
                    embed.setAuthor("Set Post Title", null, event.getJDA().getSelfUser().getAvatarUrl());
                    embed.setTitle("Create a title for your post!");
                    embed.setDescription("Try to make your title relevant to your image post! \n\n" +
                            "Here are some tips for a good title");
                    embed.addField("As much info in as little as possible",
                            "Try to put as much information with as little words as " +
                                    "possible.", false);
                    embed.setImage(doc.getString("imgUrl"));
                    embed.addField("Create a punchline",
                            "Try to setup your meme with a funny, unique title.", false);
                    try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_links.properties"))){
                        Properties prop = new Properties();
                        // load a properties file
                        prop.load(input);
                        embed.setThumbnail(prop.getProperty("archivus.crying_gif"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        event.replyEmbeds(embed.build()).queue();
                    }
                }

                @Override
                public Document confirmation(GuildMessageReceivedEvent event, Document doc, Mongo mongo) {
                    String title = event.getMessage().getContentDisplay();
                    Path path = Paths.get("src/main/resources/archivus_links.properties");
                    if (title.length() > 150){
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setAuthor("Error In Title", event.getJDA().getSelfUser().getAvatarUrl());
                        embed.setColor(Archivus.colorPicker());
                        embed.setTitle("Bruh. You need to give a valid title, make it shorter!")
                                .setDescription("Archivus does not allow Titles to be longer than " +
                                        "150 characters.")
                                .setFooter("Click the 'Redo' button to remake your title.");


                        try(InputStream input = Files.newInputStream(path)) {
                            Properties prop = new Properties();
                            // load a properties file
                            prop.load(input);

                            embed.setThumbnail(prop.getProperty("archivus.error1_gif"));
                        } catch (IOException ex) {
                            System.err.println("There's an issue with the path given to or data in the " +
                                    "archivus_links.properties file");
                            ex.printStackTrace();
                        } finally {
                            Conversation con = Conversation.conversations.get(event.getAuthor().getId());
                            con.hasrun = true;
                            Conversation.conversations.replace(event.getAuthor().getId(), con);
                            event.getMessage().replyEmbeds(embed.build())
                                    .setActionRow(Button.primary(event.getAuthor().getId() + ":conv_no",
                                            "Redo").withEmoji(Emoji.fromUnicode("U+1F504"))).queue();
                        }
                        return doc;
                    }

                    doc.put("title", title);

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker());
                    embed.setTitle("Confirm that this is the title you would like to use for your post!")
                            .setDescription(title)
                            .setFooter("Press a button below to continue!")
                            .addField("Fun Fact!",
                                    Archivus.funFact(),
                                    false);

                    try(InputStream input = Files.newInputStream(path)){
                        Properties prop = new Properties();
                        prop.load(input);

                        embed.setThumbnail(prop.getProperty("archivus.confirm1_gif"));
                    } catch (IOException ex){
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
                    doc.remove("topics");

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Archivus.colorPicker());
                    embed.setAuthor("Set Post Topics", null, event.getJDA().getSelfUser().getAvatarUrl());
                    embed.setTitle("Enter 'Done' in the chat when you are finished! Set topics for your post!");
                    embed.setDescription("Add topics to your post so the people who like this content can see it" +
                            "\nPost Title: " + doc.getString("title"));
                    embed.setImage(doc.getString("imgUrl"));
                    embed.addField("Make sure your topics are relevant",
                            "Try to make sure your post is relevant, if it isn't, it risks being " +
                                    "removed and you will lose reputation", false);
                    try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_links.properties"))){
                        Properties prop = new Properties();
                        // load a properties file
                        prop.load(input);
                        embed.setThumbnail(prop.getProperty("archivus.crying_gif"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        event.replyEmbeds(embed.build()).addActionRow(
                                Button.success(event.getUser().getId() +
                                        ":post_topic-shitpost", "Shitpost"),
                                Button.success(event.getUser().getId() +
                                        ":post_topic-dark", "Dark"),
                                Button.success(event.getUser().getId() +
                                        ":post_topic-gaming", "Gaming"),
                                Button.success(event.getUser().getId() +
                                        ":post_topic-anime", "Anime"),
                                Button.success(event.getUser().getId() +
                                        ":post_topic-relatable", "Relatable")).queue();
                    }
                }

                @Override
                public Document confirmation(GuildMessageReceivedEvent event, Document doc, Mongo mongo) {
                    ArrayList<Document> topics = new ArrayList<>(doc.getList("topics", Document.class));
                    if (!event.getMessage().getContentRaw().toLowerCase().contains("done"))
                        return null;
                    Path path = Paths.get("src/main/resources/archivus_links.properties");
                    if(topics.size() > 3 || topics.size() < 1){
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setColor(Archivus.colorPicker());
                        embed.setTitle("Bruh. You've gotta choose 2 topics not " + topics.size() + ".")
                                .setDescription("You need to choose either 1 to 3 topics which you would like " +
                                        "to user")
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
                            .setDescription("You've chosen the topic(s) you would like, now confirm that they are" +
                                    " your favourite topics");

                    for(int i = 0; i < topics.size(); i++){
                        PostTopic topic = PostTopic.toTopicByDoc(topics.get(i));
                        embed.addField((i+1) + ". " + topic.toString().charAt(0) +
                                        topic.toString().substring(1).toLowerCase(),
                                topic.data, false);
                    }

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
        }
    };
}
