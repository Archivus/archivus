package archivus.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public interface  ButtonAction {
    void buttonExecute(ButtonClickEvent event);
}
