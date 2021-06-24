# Dokokashira Door

"Enhances" vanilla doors for Modfest 1.17.

## Ok but what does it actually do

A "gateway" is a 3x3 structure: an opaque door, surrounded by opaque blocks on the left, right, and top, including the top corners. If you build at least two identical gateways, then walk close enough to one such that it fills the entire screen, the door will open to a different gateway.

Even on crappy internet connections, the effect happens instantly. (Doesn't mean it's the latest cheat-client arbitrary-teleport vector, though.)

## Note

Since clients need to predict their teleportation destination in order to make the effect work, the list of gateways is sent to all clients. The Venn diagram intersection between "people who want to install this mod" and "people who run servers where 'coordinate leaks' are a big deal" is probably exactly zero people large. But now you know.

# Todo list

* Test more with `clumsy`.
* Enforce that block*states* match, not only blocks. Will need to rotate them.
* Fix door sound effects.

## Blah

Dased off a silly editing gimmick from *PiroPito First Playthrough of Minecraft*, which I'm hearing is a Doraemon reference?

LGPL 3.0 or later