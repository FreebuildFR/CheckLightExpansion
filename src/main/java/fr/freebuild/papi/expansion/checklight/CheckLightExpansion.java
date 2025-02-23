package fr.freebuild.papi.expansion.checklight;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CheckLightExpansion extends PlaceholderExpansion {

  public CheckLightExpansion() {
  }

  @Override
  public String getAuthor() {
    return "Freebuild";
  }

  @Override
  public String getIdentifier() {
    return "checklight";
  }

  @Override
  public String getVersion() {
    return "1.1.0";
  }

  private class ParsingFailedException extends Exception {
    public ParsingFailedException(String message) {
      super(message);
    }
  }

  /**
   * Expose a placeholder to check if the player as a specific light in inventory.
   *
   * The placeholder checklight expose 2 properties :
   * - level : It specify the level of light wanted. It can be a value between 1 and 15 or "no" for no nbt level
   * - amount : The quantity wanted
   * And one action :
   * - remove : Remove it if it match the expectation.
   *
   * Examples :
   *   %checklight_level:2_amount:5% -> Check the player have 5 light with level 2
   *   %checklight_level:2_level:no_amount:5% -> Check the player have 5 light with level 2 or without nbt level
   *   %checklight_remove_level:2_amount:5% -> Check the player have 5 light with level 2 and remove them
   */
  @Override
  public String onPlaceholderRequest(Player player, String param) {
    if (player == null)
      return null;

    try {
      return handleRequest(player, param.split("_"));
    } catch (ParsingFailedException e) {
      return e.getMessage();
    }
  }

  private String handleRequest(Player player, String[] args) throws ParsingFailedException {

    List<Integer> levels = new ArrayList<>();
    Integer amount = 1;
    Boolean remove = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("remove"))
        remove = true;
      if (args[i].startsWith("level:"))
        levels.add(parseIntArg(args[i], "level", 0, 15));
      if (args[i].startsWith("amount:"))
        amount = parseIntArg(args[i], "amount", 1, null);
    }

    ItemStack[] contents = player.getInventory().getContents();

    Integer remaining = amount;
    List<ItemStack> toDelete = new ArrayList<>();

    try {
      for (int i = 0; i < contents.length && remaining > 0;  i++) {
        ItemStack item = contents[i];

        if(item != null && item.getType() == Material.LIGHT) {
          Integer lightLevel = getNbtLevel(item);
          if (levels.stream().anyMatch(level -> lightLevel == level)) {
            Integer amountToRemove = Math.min(remaining, item.getAmount());
            remaining -= amountToRemove;
            ItemStack itemToDelete = item.clone();
            itemToDelete.setAmount(amountToRemove);
            toDelete.add(itemToDelete);
          }
        }
      }

      if (remove && remaining <= 0) {
        toDelete.forEach(it -> player.getInventory().removeItem(it));
      }

      return (remaining > 0) ? "no" : "yes";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }

  private Integer parseIntArg(String arg, String name, Integer start, Integer end) throws ParsingFailedException {
    try {
      String value = arg.substring(name.length() + 1);
      if (value.equals("no"))
        return null;
      Integer num = Integer.parseInt(value);
      if (start != null && num < start) {
        num = start;
      }
      if (end != null && num > end) {
        num = end;
      }
      return num;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ParsingFailedException(String.format("Invalid value for %s. Expecting: %<s:15 Got: %$", name, arg));
    }
  }


  /**
   * NB : With Paper NmsApi is not obfuscate
   */
  private Integer getNbtLevel(ItemStack itemStack) throws Exception {
    // org.bukkit.craftbukkit.inventory.CraftItemStack.asNmsCopy (/!\ Works only for Paper 1.21+)
    Object item = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack").getDeclaredMethod("asNMSCopy", ItemStack.class).invoke(null, itemStack);

    // net.minecraft.core.component.DataComponents.BLOCK_STATE
    Object BLOCK_STATE = Class.forName("net.minecraft.core.component.DataComponents").getField("BLOCK_STATE").get(null);

    // net.minecraft.core.component.DataComponentHolder.get(BLOCK_STATE) => net.minecraft.world.item.component.BlockItemStateProperties
    Class<?> cDataComponentHolder = Class.forName("net.minecraft.core.component.DataComponentHolder");
    Class<?> cDataComponentType = Class.forName("net.minecraft.core.component.DataComponentType");
    Object blockState = cDataComponentHolder.getMethod("get", cDataComponentType).invoke(item, BLOCK_STATE);

    if (blockState != null) {
      // net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL
      Object levelProperty = Class.forName("net.minecraft.world.level.block.state.properties.BlockStateProperties").getField("LEVEL").get(null);

      // net.minecraft.world.item.component.BlockItemStateProperties.get(Property)
      Class<?> cProperty = Class.forName("net.minecraft.world.level.block.state.properties.Property");
      return (Integer) blockState.getClass().getMethod("get", cProperty).invoke(blockState, levelProperty);
    }
    return null;
  }
}

