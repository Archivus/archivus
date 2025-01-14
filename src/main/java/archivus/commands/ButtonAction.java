package archivus.commands;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

@FunctionalInterface
public interface  ButtonAction {
    // Functional interface used run an action triggered by clicking
    // Use in HashMap to query the name of the button to the command
    void buttonExecute(ButtonClickEvent event);
}
