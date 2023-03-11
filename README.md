
# CheckLightExpansion

Expose a placeholder to check if the player as a specific light in inventory.

## Properties and action

The placeholder checklight expose 2 properties :
- level : It specify the level of light wanted. It can be a value between 1 and 15 or "no" for no nbt level
- amount : The quantity wanted

And one action :
- remove : Remove the items matching the expectation.

## Output

The placeholder will output "no" if the placeholder can't find the wanted lights and "yes" if it can;

## Examples

`%checklight_level:2_amount:5%` -> Check the player have 5 light with level 2
`%checklight_level:2_level:no_amount:5%` -> Check the player have 5 light with level 2 or without nbt level
`%checklight_remove_level:2_amount:5%` -> Check the player have 5 light with level 2 and remove them
