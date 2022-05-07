package com.general_hello.commands.OtherEvents;

import com.general_hello.commands.Database.DataUtils;
import com.general_hello.commands.Objects.Emojis.RPGEmojis;
import com.general_hello.commands.Objects.Items.Object;
import com.general_hello.commands.Objects.Map.Grid;
import com.general_hello.commands.Objects.Map.Map;
import com.general_hello.commands.Objects.Trade.Trade;
import com.general_hello.commands.Objects.User.Player;
import com.general_hello.commands.Utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.general_hello.commands.commands.stage3.PlayCommand.userLocations;
import static com.general_hello.commands.commands.stage3.PlayCommand.userToLocations;

public class OnButtonClick extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        // users can spoof this id so be careful what you do with this
        String[] id = event.getComponentId().split(":"); // this is the custom id we specified in our button
        String authorId = id[0];

        if (id.length == 1) {
            return;
        }

        String type = id[1];

        // If the button id is profile it'll send the message that the user liked the profile and increment the like count by 1
        if (type.equals("profile")) {
            if (authorId.equals(event.getUser().getId())) {
                event.reply("Seems like " + event.getUser().getAsMention() + " is a Narcissist 🤪").queue();
                return;
            }
            Player player = DataUtils.getPlayer(event.getJDA().getUserById(authorId));
            player.setLikes(player.getLikes()+1);
            event.reply("Liked the profile").setEphemeral(true).queue();
            return;
        }

        // When storing state like this is it is highly recommended doing some kind of verification that it was generated by you, for instance a signature or local cache
        if (!authorId.equals("0000") && !authorId.equals(event.getUser().getId())) {
            event.reply("You can't press this button").setEphemeral(true).queue();
            return;
        }

        User author = event.getUser();

        switch (type) {
            // Sensei buttons
            case "sensei" -> {
                // If its yes it'll add the user as a sensei for the other user
                if (id[2].equals("yes")) {
                    User user = event.getJDA().getUserById(id[3]);
                    Player player = DataUtils.getPlayer(user);
                    player.setSenseiId(author.getIdLong());
                    DataUtils.getPlayer(author).setSenseiId(user.getIdLong());
                    event.reply(author.getAsMention() + " is now your Sensei.").queue((interactionHook -> {
                        interactionHook.deleteOriginal().queueAfter(3, TimeUnit.SECONDS);
                    }));
                    event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
                } else {
                    // Send the denied msg
                    event.reply(author.getAsMention() + " denied the request.").queue((interactionHook -> {
                        interactionHook.deleteOriginal().queueAfter(3, TimeUnit.SECONDS);
                    }));
                    event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
                }
            // Trade buttons
            } case "switch" -> {
                // Switch the editing trade
                User user = Trade.loadTrade(event.getUser().getIdLong()).switchFocusedOffer().save().getFocusedOfferTarget();
                event.reply("You are now editing " + user.getAsMention() + "'s offer.").setEphemeral(true).queue();
            } case "reset" -> {
                // Reset the trade
                EmbedBuilder embedBuilder = Trade.loadTrade(event.getUser().getIdLong()).resetTrade().buildEmbed();
                event.reply("Resetted the trade.").setEphemeral(true).queue();
                SelectMenu menu = SelectMenu.create("trade:" + event.getUser().getId())
                        .setPlaceholder("Choose the option")
                        .setRequiredRange(1, 1)
                        .addOption("Add Items", "additems", "Adds items for the trade", Emoji.fromMarkdown("<:egyptian_broken_weapon:905969748582469682>"))
                        .addOption("Change Items", "changeitems", "Resets your offer of items and adds items", Emoji.fromMarkdown("<a:loading:870870083285712896>"))
                        .addOption("Add Berri", "addberri", "Resets the berri and place your offer for it", Emoji.fromMarkdown(RPGEmojis.berri))
                        .addOption("Add Rainbow Shards", "addrainbowshards", "Resets the rainbow shards and place your offer for it", Emoji.fromMarkdown(RPGEmojis.rainbowShards))
                        .build();
                event.getMessage().editMessageEmbeds(embedBuilder.build())
                        .setActionRows(
                                ActionRow.of(menu),
                                ActionRow.of(
                                        Button.secondary(event.getUser().getId() + ":switch", "Switch Editing Offer").withEmoji(Emoji.fromMarkdown("<:right:915425310592356382>")),
                                        Button.danger(event.getUser().getId() + ":reset", "Reset Trade").withEmoji(Emoji.fromMarkdown("<:xmark:957420842898292777>")),
                                        Button.success(event.getUser().getId() + ":submit", "Submit Trade").withEmoji(Emoji.fromMarkdown("<:check:957420541256531969>")),
                                        Button.danger(event.getUser().getId() + ":cancel", "Cancel Trade").withEmoji(Emoji.fromMarkdown("<:xmark:957420842898292777>"))
                                )
                        ).queue();
            } case "submit" -> {
                // Submit the trade and send an embed to accept or deny the trade
                Trade trade = Trade.loadTrade(event.getUser().getIdLong());
                EmbedBuilder embedBuilder = trade.buildEmbed();
                event.getMessage().editMessage(trade.getTargetUser().getAsMention()).setEmbeds(embedBuilder.build()).setActionRow(
                        Button.danger(trade.getTargetUserId() + ":denytrade", "Deny Trade")
                                .withEmoji(Emoji.fromMarkdown("<:xmark:957420842898292777>")),
                        Button.success(trade.getTargetUserId() + ":accepttrade", "Accept Trade")
                                .withEmoji(Emoji.fromMarkdown("<:check:957420541256531969>"))).queue();
                event.reply("Submitted the offer").setEphemeral(true).queue();
            } case "denytrade" -> {
                // Deny and delete the trade
                Trade trade = Trade.loadTrade(event.getUser().getIdLong());
                event.getMessage().editMessageEmbeds(trade.buildEmbed().build()).setActionRow(Button.secondary("ignore", "Trade Denied").asDisabled()).queue();
                trade.deleteTrade();
            } case "cancel" -> {
                // Delete the trade
                Trade trade = Trade.loadTrade(event.getUser().getIdLong());
                event.getMessage().editMessageEmbeds(trade.buildEmbed().build()).setActionRow(Button.secondary("ignore", "Trade Canceled").asDisabled()).queue();
                trade.deleteTrade();
            } case "accepttrade" -> {
                // Accept the trade and give or remove the items or currency
                Trade trade = Trade.loadTrade(event.getUser().getIdLong());
                Player player = DataUtils.getPlayer(trade.getProposerUser());
                player.setBerri(player.getBerri() - trade.getProposerOffer().getBerri());
                player.setRainbowShards(player.getRainbowShards() - trade.getProposerOffer().getRainbowShards());
                if (!trade.getProposerOffer().getItems().isEmpty()) {
                    List<Object> itemsProposer = trade.getProposerOffer().getItems();
                    for (Object object : itemsProposer) {
                        Player.addItem(trade.getProposerUserId(), -1, object.getName());
                    }
                }

                player = DataUtils.getPlayer(trade.getTargetUser());
                player.setBerri(player.getBerri() - trade.getTargetToGiveOffer().getBerri());
                player.setRainbowShards(player.getRainbowShards() - trade.getTargetToGiveOffer().getRainbowShards());
                if (!trade.getTargetToGiveOffer().getItems().isEmpty()) {
                    List<Object> itemsProposer = trade.getTargetToGiveOffer().getItems();
                    for (Object object : itemsProposer) {
                        Player.addItem(trade.getTargetUserId(), -1, object.getName());
                    }
                }
                event.getMessage().editMessageEmbeds(trade.buildEmbed().build()).setActionRow(Button.secondary("ignore", "Trade Accepted").asDisabled()).queue();
                trade.deleteTrade();
            } case "up" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()).get(prevGrid.getY() - 2);
                sendMap(event, userid, prevGrid, newGrid);
            } case "upleft" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()-1).get(prevGrid.getY() - 2);
                sendMap(event, userid, prevGrid, newGrid);
            } case "upright" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()+1).get(prevGrid.getY() - 2);
                sendMap(event, userid, prevGrid, newGrid);
            } case "left" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()-1).get(prevGrid.getY()-1);
                sendMap(event, userid, prevGrid, newGrid);
            } case "right" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()+1).get(prevGrid.getY()-1);
                sendMap(event, userid, prevGrid, newGrid);
            } case "downleft" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()-1).get(prevGrid.getY());
                sendMap(event, userid, prevGrid, newGrid);
            } case "downright" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()+1).get(prevGrid.getY());
                sendMap(event, userid, prevGrid, newGrid);
            } case "down" -> {
                long userid = event.getUser().getIdLong();
                Grid prevGrid = userToLocations.get(userid);
                Grid newGrid = Grid.gridsX.get(prevGrid.getX()).get(prevGrid.getY());
                sendMap(event, userid, prevGrid, newGrid);
            } case "nada" -> {
                event.reply("Fight message here").queue();
            }
        }
    }

    private void sendMap(@NotNull ButtonInteractionEvent event, long userid, Grid prevGrid, Grid newGrid) {
        MessageEmbed messageEmbed = EmbedUtil.defaultEmbed(Map.buildMap(newGrid));
        if (userToLocations.containsKey(event.getUser().getIdLong())) {
            Grid oldGrid = userToLocations.get(event.getUser().getIdLong());
            ArrayList<Long> userOld = userLocations.get(oldGrid);
            userOld.remove(event.getUser().getIdLong());
            userLocations.put(oldGrid, userOld);
        }

        if (userLocations.containsKey(prevGrid)) {
            ArrayList<Long> usersOld = userLocations.get(prevGrid);
            usersOld.remove(event.getUser().getIdLong());
            userLocations.put(prevGrid, usersOld);
        }

        ArrayList<Long> userFromGrids = new ArrayList<>();
        if (userLocations.containsKey(newGrid)) {
            userFromGrids = userLocations.get(newGrid);
        }
        userFromGrids.add(event.getUser().getIdLong());
        userLocations.put(newGrid, userFromGrids);
        userToLocations.put(event.getUser().getIdLong(), newGrid);
        boolean canFight = false;
        if (userLocations.get(newGrid).size() > 1) {
            canFight = true;
        }
        event.getMessage().editMessageEmbeds(messageEmbed).setActionRows(
                ActionRow.of(
                        Button.secondary(userid + ":upleft", Emoji.fromMarkdown("↖")).withDisabled(newGrid.getY() - 1 <= 0 || newGrid.getX() -1 == 0),
                        Button.secondary(userid + ":up", Emoji.fromMarkdown("⬆")).withDisabled(newGrid.getY() - 1 == 0),
                        Button.secondary(userid + ":upright", Emoji.fromMarkdown("↗")).withDisabled(newGrid.getY() -1 <= 0 || newGrid.getX() +1 > 15)
                ), ActionRow.of(
                        Button.secondary(userid + ":left", Emoji.fromMarkdown("⬅")).withDisabled(newGrid.getX() -1 <= 0),
                        Button.secondary("empty", "\u200E").asDisabled(),
                        Button.secondary(userid + ":right", Emoji.fromMarkdown("➡")).withDisabled(newGrid.getX() +1 > 15)
                ), ActionRow.of(
                        Button.secondary(userid + ":downleft", Emoji.fromMarkdown("↙")).withDisabled(newGrid.getY() +1 > 15 || newGrid.getX() -1 == 0),
                        Button.secondary(userid + ":down", Emoji.fromMarkdown("⬇")).withDisabled(newGrid.getY() + 1 > 15),
                        Button.secondary(userid + ":downright", Emoji.fromMarkdown("↘")).withDisabled(newGrid.getY() + 1 > 15 || newGrid.getX() +1 > 15),
                        Button.secondary("nada", canFight ? "Fight" : "\u200E").withDisabled(!canFight)
                )
        ).queue();
        event.deferEdit().queue();
    }
}