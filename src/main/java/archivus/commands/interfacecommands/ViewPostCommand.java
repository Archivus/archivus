package archivus.commands.interfacecommands;

import archivus.Archivus;
import archivus.commands.SlashCommand;
import archivus.commands.CommandType;
import archivus.mongo.Mongo;
import archivus.user.interaction.posting.Post;
import archivus.user.interaction.posting.PostTopic;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ViewPostCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        OptionMapping id = event.getOption("id");
        OptionMapping title = event.getOption("title");

        if(title != null){
            mongo.useClient(action -> {
                MongoCollection<Document> collection = action.getDatabase("post")
                        .getCollection("availableposts");
                List<Document> posts = collection.find().into(new ArrayList<>())
                        .parallelStream()
                        .filter(doc -> doc.getString("title").toLowerCase()
                                .contains(title.getAsString().toLowerCase()))
                        .collect(Collectors.toList());
                if(posts.size() == 0){
                    event.reply("Your search for `" + title.getAsString() + "` returned no results!").queue();
                    return;
                }
                EmbedBuilder embed = new EmbedBuilder();
                embed.setAuthor("Search results for \"" + title.getAsString() + "\"",
                        null, event.getJDA().getSelfUser().getAvatarUrl())
                        .setTitle("Your search results returned " + posts.size() + " post(s).")
                        .setDescription("Not all posts may be shown for display purposes. Press the corresponding button " +
                                "to view the full post!")
                        .setColor(Archivus.colorPicker());
                List<Button> buttons = new ArrayList<>();
                for(int i = 0; i < 3; i++){
                    if(i == posts.size()) break;
                    Post post = new Post(posts.get(i));
                    StringBuilder str = new StringBuilder("Tag(s): ");
                    for(int j = 0; j < post.tags.size(); j++){
                        PostTopic topic = PostTopic.toTopicByDoc(post.tags.get(j));
                        str.append(topic.toString().charAt(0))
                                .append(topic.toString().substring(1).toLowerCase());
                        if(j != post.tags.size() -1) str.append(", ");
                    }
                    embed.addField("Post " + (i + 1) + ": " + post.title, "Votes: " + post.vote + "\n" +
                            str, false);
                    buttons.add(Button.primary(event.getUser().getId() + ":view-post_post=" + post.postId,
                            "Post " + (i  + 1)));
                }
                event.replyEmbeds(embed.build()).addActionRow(buttons).queue();

            }, event.getHook());
        } else if(id != null){
            mongo.useClient(action -> {
                MongoCollection<Document> collection = action.getDatabase("post")
                        .getCollection("availableposts");
                Document doc = collection.find(new Document("postId", id.getAsString())).first();
                if(doc == null){
                    event.reply("\"" + id.getAsString() + "\" is not a valid post ID, please check that " +
                            "the post is correct, or it may just not exist.").queue();
                } else {
                    Post post = new Post(doc);
                    EmbedBuilder embed = post.postEmbed(event.getUser().getId(), false);
                    if(post.viewedUsers.contains(event.getUser().getId())){
                        embed.setFooter("You've already seen this post, so you cannot vote on it! ⦁ Post ID: " + post.postId);
                        event.replyEmbeds(embed.build()).queue();
                    } else {
                        embed.setFooter("This is your only chance to upvote or downvote this post! So do it! ⦁ Post ID: " + post.postId);
                        event.replyEmbeds(embed.build()).addActionRow(
                                Button.success(event.getUser().getId() + ":view-post_upvote=" + post.postId, "Upvote"),
                                Button.danger(event.getUser().getId() + ":view-post_downvote=" + post.postId, "Downvote")).queue();
                    }
                    post.updatePost(collection);
                }
            }, event.getHook());
        }  else {
            event.reply("You need to give an argument!").queue();
        }
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        String[] buttonId = event.getComponentId().split(":");
        if (!buttonId[0].equals(event.getUser().getId()))
            return;
        String[] data = buttonId[1].replace("view-post_", "").split("=");
        mongo.useClient(client -> {
            Document doc = client.getDatabase("post")
                    .getCollection("availableposts").find(new Document("postId", data[1])).first();
            if(doc == null){
                event.reply("Error in Document!").queue();
                return;
            }
            Post post = new Post(doc);
            if(post.viewedUsers.contains(event.getUser().getId())){
                EmbedBuilder embed = post.postEmbed(event.getUser().getId(), false);
                embed.setFooter("You've already seen this post, you can't vote on it ⦁ Post ID: " + post.postId);

                event.replyEmbeds(embed.build()).queue();
            } else if(data[0].contains("upvote")){
                post.vote++;

                EmbedBuilder embed = post.postEmbed(event.getUser().getId(), true);
                embed.setFooter("Thank you for your vote! ⦁ Post ID: " + post.postId);

                event.editMessageEmbeds(embed.build()).setActionRows().queue();
                post.updatePost(client.getDatabase("post")
                        .getCollection("availableposts"));
            } else if(data[0].contains("downvote")){
                post.vote--;
                post.updatePost(client.getDatabase("post")
                        .getCollection("availableposts"));
                EmbedBuilder embed = post.postEmbed(event.getUser().getId(), true);
                embed.setFooter("Thank you for your vote! ⦁ Post ID: " + post.postId);

                event.editMessageEmbeds(embed.build()).setActionRows().queue();
                post.updatePost(client.getDatabase("post")
                        .getCollection("availableposts"));
            } else if(data[0].contains("post")){
                EmbedBuilder embed = post.postEmbed(event.getUser().getId(), false);
                embed.setFooter("This is your only chance to upvote or downvote this post! So do it! ⦁ Post ID: " + post.postId);
                if(post.viewedUsers.contains(event.getUser().getId())){
                    embed.setFooter("You've already seen this post, you can't vote on it ⦁ Post ID: " + post.postId);
                    event.replyEmbeds(embed.build()).queue();
                } else event.replyEmbeds(embed.build()).addActionRow(
                        Button.success(event.getUser().getId() + ":view-post_upvote=" + post.postId, "Upvote"),
                        Button.danger(event.getUser().getId() + ":view-post_downvote=" + post.postId, "Downvote"))
                        .queue();
            }
        }, event.getHook());
    }

    @Override
    public CommandData getData() {
        return new CommandData("view-post", "Allows you to view a given post")
                .addOption(OptionType.STRING, "id", "The ID of the post you wish to view")
                .addOption(OptionType.STRING, "title", "Title of the post you wish to view");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("View Post Command", event.getJDA().getSelfUser().getAvatarUrl())
                .setDescription("This command allows you to view a given post")
                .addField("/post POST_TITLE", "Queries top 3 post from the given title, try to use keywords instead of a full title", false)
                .addField("/post POSTID", "Queries the post from the given ID", false);
        return event.replyEmbeds(embed.build());
    }

    @Override
    public CommandType getType() {
        return CommandType.POSTING;
    }
}
