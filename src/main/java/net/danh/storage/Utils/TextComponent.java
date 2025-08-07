package net.danh.storage.Utils;

import net.danh.storage.NMS.NMSAssistant;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TextComponent {
    private static final NMSAssistant nms = new NMSAssistant();

    public static void sendClickableMessage(Player player, String message, String hoverText, String command) {
        if (nms.isVersionGreaterThanOrEqualTo(8)) {
            try {
                sendSpigotTextComponent(player, message, hoverText, command);
            } catch (Exception e) {
                player.sendMessage(Chat.colorizewp(message));
            }
        } else {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    public static void sendHoverMessage(Player player, String message, String hoverText) {
        if (nms.isVersionGreaterThanOrEqualTo(8)) {
            try {
                sendSpigotHoverComponent(player, message, hoverText);
            } catch (Exception e) {
                // Fallback to regular message
                player.sendMessage(Chat.colorizewp(message));
            }
        } else {
            // For very old versions, just send regular message
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    private static void sendSpigotTextComponent(Player player, String message, String hoverText, String command) throws Exception {
        // Use Spigot's TextComponent for 1.8+
        Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
        Class<?> hoverEventClass = Class.forName("net.md_5.bungee.api.chat.HoverEvent");
        Class<?> clickEventClass = Class.forName("net.md_5.bungee.api.chat.ClickEvent");
        Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");

        // Create main text component
        Constructor<?> textConstructor = textComponentClass.getConstructor(String.class);
        Object textComponent = textConstructor.newInstance(Chat.colorizewp(message));

        // Create hover event if hover text is provided
        if (hoverText != null && !hoverText.isEmpty()) {
            try {
                // Try new HoverEvent constructor (1.16+)
                Class<?> contentClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Text");
                Constructor<?> contentConstructor = contentClass.getConstructor(String.class);
                Object content = contentConstructor.newInstance(Chat.colorizewp(hoverText));

                Constructor<?> hoverConstructor = hoverEventClass.getConstructor(
                        Class.forName("net.md_5.bungee.api.chat.HoverEvent$Action"),
                        Class.forName("net.md_5.bungee.api.chat.hover.content.Content")
                );
                Object hoverAction = hoverEventClass.getDeclaredField("SHOW_TEXT").get(null);
                Object hoverEvent = hoverConstructor.newInstance(hoverAction, content);
                Method setHoverEvent = textComponentClass.getMethod("setHoverEvent", hoverEventClass);
                setHoverEvent.invoke(textComponent, hoverEvent);
            } catch (Exception e) {
                // Fallback to old HoverEvent constructor (1.8-1.15)
                try {
                    Constructor<?> hoverTextConstructor = textComponentClass.getConstructor(String.class);
                    Object hoverTextComponent = hoverTextConstructor.newInstance(Chat.colorizewp(hoverText));

                    Constructor<?> hoverConstructor = hoverEventClass.getConstructor(
                            Class.forName("net.md_5.bungee.api.chat.HoverEvent$Action"),
                            baseComponentClass.arrayType()
                    );
                    Object hoverAction = hoverEventClass.getDeclaredField("SHOW_TEXT").get(null);
                    Object hoverEvent = hoverConstructor.newInstance(hoverAction, new Object[]{hoverTextComponent});
                    Method setHoverEvent = textComponentClass.getMethod("setHoverEvent", hoverEventClass);
                    setHoverEvent.invoke(textComponent, hoverEvent);
                } catch (Exception ignored) {
                    // If both fail, continue without hover
                }
            }
        }

        // Create click event if command is provided
        if (command != null && !command.isEmpty()) {
            Constructor<?> clickConstructor = clickEventClass.getConstructor(
                    Class.forName("net.md_5.bungee.api.chat.ClickEvent$Action"),
                    String.class
            );
            Object clickAction = clickEventClass.getDeclaredField("RUN_COMMAND").get(null);
            Object clickEvent = clickConstructor.newInstance(clickAction, command);
            Method setClickEvent = textComponentClass.getMethod("setClickEvent", clickEventClass);
            setClickEvent.invoke(textComponent, clickEvent);
        }

        // Send the component to player
        Method spigot = Player.class.getMethod("spigot");
        Object spigotPlayer = spigot.invoke(player);
        Method sendMessage = spigotPlayer.getClass().getMethod("sendMessage", baseComponentClass);
        sendMessage.invoke(spigotPlayer, textComponent);
    }

    private static void sendSpigotHoverComponent(Player player, String message, String hoverText) throws Exception {
        sendSpigotTextComponent(player, message, hoverText, null);
    }

    public static void sendNavigationLine(Player player, String prevText, String prevHover, String prevCommand,
                                          String nextText, String nextHover, String nextCommand,
                                          boolean hasPrev, boolean hasNext) {
        if (nms.isVersionGreaterThanOrEqualTo(8)) {
            try {
                sendSpigotNavigationLine(player, prevText, prevHover, prevCommand, nextText, nextHover, nextCommand, hasPrev, hasNext);
            } catch (Exception e) {
                // Fallback to regular messages
                if (hasPrev) {
                    player.sendMessage(Chat.colorizewp(prevText + "     " + (hasNext ? nextText : "&8Next ▶")));
                } else {
                    player.sendMessage(Chat.colorizewp("&8◀ Previous     " + (hasNext ? nextText : "&8Next ▶")));
                }
            }
        } else {
            // For very old versions, just send regular message
            if (hasPrev) {
                player.sendMessage(Chat.colorizewp(prevText + "     " + (hasNext ? nextText : "&8Next ▶")));
            } else {
                player.sendMessage(Chat.colorizewp("&8◀ Previous     " + (hasNext ? nextText : "&8Next ▶")));
            }
        }
    }

    private static void sendSpigotNavigationLine(Player player, String prevText, String prevHover, String prevCommand,
                                                 String nextText, String nextHover, String nextCommand,
                                                 boolean hasPrev, boolean hasNext) throws Exception {
        Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
        Class<?> hoverEventClass = Class.forName("net.md_5.bungee.api.chat.HoverEvent");
        Class<?> clickEventClass = Class.forName("net.md_5.bungee.api.chat.ClickEvent");
        Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");

        // Create main component
        Constructor<?> textConstructor = textComponentClass.getConstructor(String.class);
        Object mainComponent = textConstructor.newInstance("");

        // Add previous component
        Object prevComponent;
        if (hasPrev) {
            prevComponent = createClickableComponent(textComponentClass, hoverEventClass, clickEventClass, baseComponentClass,
                    Chat.colorizewp(prevText), Chat.colorizewp(prevHover), prevCommand);
        } else {
            prevComponent = textConstructor.newInstance(Chat.colorizewp("&8◀ Previous"));
        }

        // Add spacing
        Object spacingComponent = textConstructor.newInstance(Chat.colorizewp("     "));

        // Add next component
        Object nextComponent;
        if (hasNext) {
            nextComponent = createClickableComponent(textComponentClass, hoverEventClass, clickEventClass, baseComponentClass,
                    Chat.colorizewp(nextText), Chat.colorizewp(nextHover), nextCommand);
        } else {
            nextComponent = textConstructor.newInstance(Chat.colorizewp("&8Next ▶"));
        }

        // Combine components
        Method addExtra = textComponentClass.getMethod("addExtra", baseComponentClass);
        addExtra.invoke(mainComponent, prevComponent);
        addExtra.invoke(mainComponent, spacingComponent);
        addExtra.invoke(mainComponent, nextComponent);
        Method spigot = Player.class.getMethod("spigot");
        Object spigotPlayer = spigot.invoke(player);
        Method sendMessage = spigotPlayer.getClass().getMethod("sendMessage", baseComponentClass);
        sendMessage.invoke(spigotPlayer, mainComponent);
    }

    private static Object createClickableComponent(Class<?> textComponentClass, Class<?> hoverEventClass,
                                                   Class<?> clickEventClass, Class<?> baseComponentClass,
                                                   String text, String hoverText, String command) throws Exception {
        Constructor<?> textConstructor = textComponentClass.getConstructor(String.class);
        Object component = textConstructor.newInstance(text);

        // Add hover event
        if (hoverText != null && !hoverText.isEmpty()) {
            try {
                // Try new HoverEvent constructor (1.16+)
                Class<?> contentClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Text");
                Constructor<?> contentConstructor = contentClass.getConstructor(String.class);
                Object content = contentConstructor.newInstance(hoverText);
                Constructor<?> hoverConstructor = hoverEventClass.getConstructor(
                        Class.forName("net.md_5.bungee.api.chat.HoverEvent$Action"),
                        Class.forName("net.md_5.bungee.api.chat.hover.content.Content")
                );
                Object hoverAction = hoverEventClass.getDeclaredField("SHOW_TEXT").get(null);
                Object hoverEvent = hoverConstructor.newInstance(hoverAction, content);
                Method setHoverEvent = textComponentClass.getMethod("setHoverEvent", hoverEventClass);
                setHoverEvent.invoke(component, hoverEvent);
            } catch (Exception e) {
                // Fallback to old HoverEvent constructor (1.8-1.15)
                try {
                    Constructor<?> hoverTextConstructor = textComponentClass.getConstructor(String.class);
                    Object hoverTextComponent = hoverTextConstructor.newInstance(hoverText);
                    Constructor<?> hoverConstructor = hoverEventClass.getConstructor(
                            Class.forName("net.md_5.bungee.api.chat.HoverEvent$Action"),
                            baseComponentClass.arrayType()
                    );
                    Object hoverAction = hoverEventClass.getDeclaredField("SHOW_TEXT").get(null);
                    Object hoverEvent = hoverConstructor.newInstance(hoverAction, new Object[]{hoverTextComponent});
                    Method setHoverEvent = textComponentClass.getMethod("setHoverEvent", hoverEventClass);
                    setHoverEvent.invoke(component, hoverEvent);
                } catch (Exception ignored) {
                    // If both fail, continue without hover
                }
            }
        }

        // Add click event
        if (command != null && !command.isEmpty()) {
            Constructor<?> clickConstructor = clickEventClass.getConstructor(
                    Class.forName("net.md_5.bungee.api.chat.ClickEvent$Action"),
                    String.class
            );
            Object clickAction = clickEventClass.getDeclaredField("RUN_COMMAND").get(null);
            Object clickEvent = clickConstructor.newInstance(clickAction, command);
            Method setClickEvent = textComponentClass.getMethod("setClickEvent", clickEventClass);
            setClickEvent.invoke(component, clickEvent);
        }

        return component;
    }
}
