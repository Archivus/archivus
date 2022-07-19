package archivus.commands.interfacecommands.feedcommands;

import archivus.Archivus;
import archivus.commands.CommandType;
import archivus.commands.SlashCommand;
import archivus.mongo.Mongo;
import archivus.user.AccountDoesNotExistException;
import archivus.user.UserProfile;
import archivus.user.interaction.conversation.Conversation;
import archivus.user.interaction.conversation.ConversationException;
import archivus.user.interaction.feed.Feed;
import archivus.user.interaction.feed.FeedData;
import archivus.user.interaction.posting.Post;
import archivus.user.interaction.posting.PostTopic;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CreateFeedCommand implements SlashCommand {
    public static final ExpiringMap<MessageChannel, FeedData> feedDataMap = ExpiringMap.builder()
            .expiration(1, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .asyncExpirationListener((k, v) -> {
                MessageChannel channel = (MessageChannel) k;
                channel.sendMessage("This interaction will end, as there has been no reply for 1 minutes").queue();
            })
            .build();
    public static final ExpiringMap<MessageChannel, Feed> feedMap = ExpiringMap.builder()
            .expiration(5, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .asyncExpirationListener((k, v) -> {
                MessageChannel channel = (MessageChannel) k;
                channel.sendMessage("This interaction will end, as there has been no reply for 1 minutes").queue();
            })
            .build();

    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        if(feedDataMap.containsKey(event.getMessageChannel()) || feedMap.containsKey(event.getMessageChannel())){
            event.reply("A feed is already being created or is in use in this channel! \n\n" +
                    "Use a different channel or try again later!").queue();
            return;
        }
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Archivus.colorPicker());
        embed.setAuthor("Choose feed topics", null, event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTitle("Time to create a feed! Choose the feed topics you'd like.");
        embed.setDescription("You can view all the posts on the Archivus database through this feed.\n\n" +
                "Here are different ways to filter through Archivus posts!");
        embed.addField("Archive",
                "This is where you can see the most popular votes, which have been " +
                        "saved to the Archive. You can filter through topics inside the Archive. Note that " +
                        "as Archivus is in its early stages, the Archive may not have many posts!", false);
        embed.addField("Personal",
                "This shows your personal feed, containing posts from those who you follow " +
                        "and the topics you selected. (These can be changed in your profile menu).", false);
        embed.addField("Custom",
                "Create a custom feed with the topics you would like to have in it.", false);
        try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_links.properties"))){
            Properties prop = new Properties();
            // load a properties file
            prop.load(input);
            embed.setThumbnail(prop.getProperty("archivus.typing_gif"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            event.reply("It seems as though you are already in a communication with Archivus, Click the " +
                    "button below to end it").addActionRow(
                            Button.primary(event.getUser().getId() + ":create-feed_archive",
                    "Archive").withEmoji(Emoji.fromUnicode("U+1F511")),
                    Button.primary(event.getUser().getId() + ":create-feed_personal",
                            "Personal").withEmoji(Emoji.fromUnicode("U+1F468")),
                    Button.primary(event.getUser().getId() + ":create-feed_custom",
                            "Custom").withEmoji(Emoji.fromUnicode("U+1F58A"))).queue();

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
            FeedData feedData = feedDataMap.get(event.getMessageChannel());
            feedData.addTopic(topic);


            feedDataMap.replace(event.getMessageChannel(), feedData);
            event.editButton(Button.secondary(event.getUser().getId() + ":create-feed_topicCLICKED-" + data[1],
                    data[1].substring(0, 1).toUpperCase() + data[1].substring(1))).queue();
        } else if(data[0].endsWith("topicCLICKED")){
            PostTopic topic = PostTopic.toTopicByString(data[1]);
            FeedData feedData = feedDataMap.get(event.getMessageChannel());
            feedData.removeTopic(topic);


            feedDataMap.replace(event.getMessageChannel(), feedData);
            event.editButton(Button.success(event.getUser().getId() + ":create-feed_topic-" + data[1],
                    data[1].substring(0, 1).toUpperCase() + data[1].substring(1))).queue();
        } else if(buttonId[1].contains("archive")){
            mongo.useClient(client ->{
                // Create feed
                Feed feed = new Feed(new FeedData(new ArrayList<>(), false), event.getUser().getId(),
                        client.getDatabase("post")
                                .getCollection("availableposts"));
                if(feedDataMap.containsKey(event.getMessageChannel()) || feedMap.containsKey(event.getMessageChannel())){
                    event.reply("A feed is already being created or is in use in this channel! \n\n" +
                            "Use a different channel or try again later!").queue();
                    return;
                }
                //feed.queryPost(mongo);
                feedMap.put(event.getMessageChannel(), feed);

                Post post = feed.currentPost;
                EmbedBuilder embed = post.postEmbed(event.getUser().getId(), true);
                event.replyEmbeds(embed.build()).addActionRow(
                        Button.success(event.getUser().getId() +
                                ":create-feed_upvote", "Upvote(0)").withEmoji(Emoji.fromUnicode("U+2B06")),
                        Button.danger(event.getUser().getId() +
                                ":create-feed_downvote", "Downvote(0)").withEmoji(Emoji.fromUnicode("U+2B07")),
                        Button.danger(event.getUser().getId() +
                                ":create-feed_report", "Report").withEmoji(Emoji.fromUnicode("U+1F5D1"))
                ).queue();
            }, event.getHook());
        } else if(buttonId[1].contains("personal")){
            // Create feed
            mongo.useClient(client -> {
                try {
                    Feed feed = new Feed(new FeedData(new ArrayList<>(new UserProfile(client.getDatabase("account")
                            .getCollection("userdata"), event.getUser()).getTopics()
                            .stream().map(PostTopic::toTopicByDoc).collect(Collectors.toList())), false),
                            event.getUser().getId(), client.getDatabase("post")
                            .getCollection("availableposts"));
                    if (feedDataMap.containsKey(event.getMessageChannel()) || feedMap.containsKey(event.getMessageChannel())) {
                        event.reply("A feed is already being created or is in use in this channel! \n\n" +
                                "Use a different channel or try again later!").queue();
                        return;
                    }
                    //feed.queryPost(mongo);
                    feedMap.put(event.getMessageChannel(), feed);

                    Post post = feed.currentPost;
                    EmbedBuilder embed = post.postEmbed(event.getUser().getId(), true);
                    event.replyEmbeds(embed.build()).addActionRow(
                            Button.success(event.getUser().getId() +
                                    ":create-feed_upvote", "Upvote(0)").withEmoji(Emoji.fromUnicode("U+2B06")),
                            Button.danger(event.getUser().getId() +
                                    ":create-feed_downvote", "Downvote(0)").withEmoji(Emoji.fromUnicode("U+2B07")),
                            Button.danger(event.getUser().getId() +
                                    ":create-feed_report", "Report").withEmoji(Emoji.fromUnicode("U+1F5D1"))
                    ).queue();
                } catch (AccountDoesNotExistException e) {
                    InteractionHook hook = event.getHook();
                    hook.setEphemeral(true);
                    hook.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Error ⛔")
                            .setColor(Archivus.colorPicker())
                            .setDescription("You do not have a registered Archivus account")
                            .addField("Enter `/createaccount create` to make one!",
                                    "", false)
                            .build()).queue();
                }
            }, event.getHook());
        } else if(buttonId[1].contains("custom")){
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Archivus.colorPicker());
            embed.setAuthor("Set Feed Topics", null, event.getJDA().getSelfUser().getAvatarUrl());
            embed.setTitle("Set your custom feed topics! Click 'Done' when you have finished");
            embed.setDescription("Add topics to your custom feed!")
                    .addField("Shitpost", PostTopic.SHITPOST.data, true)
                    .addField("Dark", PostTopic.DARK.data, true)
                    .addField("Gaming", PostTopic.GAMING.data, true)
                    .addField("Anime", PostTopic.ANIME.data, true)
                    .addField("Relatable", PostTopic.RELATABLE.data, true);
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
                                ":create-feed_topic-shitpost", "Shitpost"),
                        Button.success(event.getUser().getId() +
                                ":create-feed_topic-dark", "Dark"),
                        Button.success(event.getUser().getId() +
                                ":create-feed_topic-gaming", "Gaming"),
                        Button.success(event.getUser().getId() +
                                ":create-feed_topic-anime", "Anime"),
                        Button.success(event.getUser().getId() +
                                ":create-feed_topic-relatable", "Relatable"),
                        Button.primary(event.getUser().getId() +
                                ":create-feed_done", "Done")).queue();
            }
        } else if(buttonId[1].contains("done")){
            mongo.useClient(client -> {
                // Create Feed
                FeedData feedData = feedDataMap.get(event.getMessageChannel());
                Feed feed = new Feed(feedData, event.getUser().getId(), client.getDatabase("post")
                        .getCollection("availableposts"));
                if (feedDataMap.containsKey(event.getMessageChannel()) || feedMap.containsKey(event.getMessageChannel())) {
                    event.reply("A feed is already being created or is in use in this channel! \n\n" +
                            "Use a different channel or try again later!").queue();
                    return;
                }
                //feed.queryPost(mongo);
                feedMap.put(event.getMessageChannel(), feed);

                Post post = feed.currentPost;
                EmbedBuilder embed = post.postEmbed(event.getUser().getId(), true);
                event.replyEmbeds(embed.build()).addActionRow(
                        Button.success(event.getUser().getId() +
                                ":create-feed_upvote", "Upvote(0)").withEmoji(Emoji.fromUnicode("U+2B06")),
                        Button.danger(event.getUser().getId() +
                                ":create-feed_downvote", "Downvote(0)").withEmoji(Emoji.fromUnicode("U+2B07")),
                        Button.danger(event.getUser().getId() +
                                ":create-feed_report", "Report").withEmoji(Emoji.fromUnicode("U+1F5D1"))
                ).queue();
            }, event.getHook());
        } else if(buttonId[1].contains("upvote")){
            Feed feed = feedMap.get(event.getMessageChannel());
            if(feed.members.contains(event.getUser().getId())){
                InteractionHook hook = event.getHook();
                hook.setEphemeral(true);
                hook.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Error ⛔")
                        .setColor(Archivus.colorPicker())
                        .setDescription("You are not a part of this feed")
                        .addField("Ask the feed creator or an admin to add you.",
                                "", false)
                        .build()).queue();
            }

            event.getJDA().retrieveUserById(feed.currentPost.userId).queue( user -> {
                mongo.useClient(client -> {
                    try {
                        feed.upvotes++;
                        UserProfile profile = new UserProfile(client.getDatabase("account")
                                .getCollection("userdata"),
                                user);
                        profile.setReputation(2);
                        profile.updateProfile(client.getDatabase("account").getCollection("userdata"),
                                event.getHook());
                        feed.currentPost.vote++;
                        feed.currentPost.updatePost(client.getDatabase("account").getCollection("userdata"));
                        if(feed.upvotes + feed.downvotes == feed.members.size()) {
                            feed.queryPost(mongo);
                        }
                        feedMap.replace(event.getChannel(), feed);
                        Post post = feed.currentPost;

                        EmbedBuilder embed = post.postEmbed(event.getUser().getId(), true);
                        embed.setFooter(feed.members.size() - feed.upvotes + feed.downvotes +
                                " more people need to vote ⦁ Post ID: " + post.postId);
                        event.replyEmbeds(embed.build()).addActionRow(
                                Button.success(event.getUser().getId() +
                                        ":create-feed_upvote", "Upvote(" + feed.upvotes + ")")
                                        .withEmoji(Emoji.fromUnicode("U+2B06")),
                                Button.danger(event.getUser().getId() +
                                        ":create-feed_downvote", "Downvote(" + feed.downvotes + ")")
                                        .withEmoji(Emoji.fromUnicode("U+2B07")),
                                Button.danger(event.getUser().getId() +
                                        ":create-feed_report", "Report").withEmoji(Emoji.fromUnicode("U+1F5D1"))
                        ).queue();
                    } catch (AccountDoesNotExistException e) {
                        throw new RuntimeException(e);
                    }
                }, event.getHook());
            });

        } else if(buttonId[1].contains("downvote")){
            Feed feed = feedMap.get(event.getMessageChannel());
            if(feed.members.contains(event.getUser().getId())){
                InteractionHook hook = event.getHook();
                hook.setEphemeral(true);
                hook.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Error ⛔")
                        .setColor(Archivus.colorPicker())
                        .setDescription("You are not a part of this feed")
                        .addField("Ask the feed creator or an admin to add you.",
                                "", false)
                        .build()).queue();
            }

            event.getJDA().retrieveUserById(feed.currentPost.userId).queue( user -> {
                mongo.useClient(client -> {
                    try {
                        feed.downvotes++;
                        UserProfile profile = new UserProfile(client.getDatabase("account")
                                .getCollection("userdata"),
                                user);
                        profile.setReputation(-2);
                        profile.updateProfile(client.getDatabase("account").getCollection("userdata"),
                                event.getHook());
                        feed.currentPost.vote--;
                        feed.currentPost.updatePost(client.getDatabase("account").getCollection("userdata"));
                        if(feed.upvotes + feed.downvotes == feed.members.size()) {
                            feed.queryPost(mongo);
                        }
                        feedMap.replace(event.getChannel(), feed);
                        Post post = feed.currentPost;

                        EmbedBuilder embed = post.postEmbed(event.getUser().getId(), true);
                        embed.setFooter(feed.members.size() - feed.upvotes + feed.downvotes +
                                " more people need to vote ⦁ Post ID: " + post.postId);
                        event.replyEmbeds(embed.build()).addActionRow(
                                Button.success(event.getUser().getId() +
                                                ":create-feed_upvote", "Upvote(" + feed.upvotes + ")")
                                        .withEmoji(Emoji.fromUnicode("U+2B06")),
                                Button.danger(event.getUser().getId() +
                                                ":create-feed_downvote", "Downvote(" + feed.downvotes + ")")
                                        .withEmoji(Emoji.fromUnicode("U+2B07")),
                                Button.danger(event.getUser().getId() +
                                        ":create-feed_report", "Report").withEmoji(Emoji.fromUnicode("U+1F5D1"))
                        ).queue();
                    } catch (AccountDoesNotExistException e) {
                        throw new RuntimeException(e);
                    }
                }, event.getHook());
            });
        } else if(buttonId[1].contains("report")){
            Feed feed = feedMap.get(event.getMessageChannel());
            if(feed.members.contains(event.getUser().getId())){
                InteractionHook hook = event.getHook();
                hook.setEphemeral(true);
                hook.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Error ⛔")
                        .setColor(Archivus.colorPicker())
                        .setDescription("You are not a part of this feed")
                        .addField("Ask the feed creator or an admin to add you, or to report this post.",
                                "", false)
                        .build()).queue();
            }
            Post post = feed.currentPost;


            Guild guild = event.getJDA().getGuildById("");
            if(guild == null)
                return;
            guild.getTextChannelsByName("reports", true).get(0)
                            .sendMessageEmbeds(new EmbedBuilder().setTitle("Report made on post: " + post.postId)
                                    .setDescription("Post title: " + post.title)
                                    .setImage(post.imageUrl)
                                    .setFooter("Post creator: " + post.userTag).build()).queue();
            event.reply("The content has been reported, we will need to receive multiple reports before" +
                    " we can take action.").queue();
            post.reports++;
            feed.currentPost = post;
            feedMap.replace(event.getChannel(), feed);
        }
    }

    @Override
    public CommandData getData() {
        return new CommandData("create-feed", "Allows you to begin a feed");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Create Feed Command", event.getJDA().getSelfUser().getAvatarUrl())
                .setDescription("This command allows you to start a feed on archivus")
                .addField("/create-feed", "Opens the feed setup menu", false)
                .setFooter("Click to go to begin the feed setup!").setColor(Archivus.colorPicker());
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
