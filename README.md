# Dokokashira Door

"Enhances" vanilla doors.

For Modfest 1.17.

(based off a silly editing gimmick from *PiroPito First Playthrough of Minecraft*, which I'm hearing is a Doraemon reference)

## Protocol

#### S -> C `dokokashira_door:full_gateway_update`

* `RegistryKey<World>` dimension
* Nbt compound with one child element, `full_update`
	* Serialized GatewayMap, read it back with the codec

Server overwrites the client's GatewayMap in that dimension. No response.

#### S -> C `dokokashira_door:delta_gateway_update`

* `RegistryKey<World>` dimension
* Nbt compound with two child elements, `additions` and `removals`
	* Serialized GatewayMaps, read them back with the codec

Server applies changes to the client's GatewayMap in that dimension. Respond with the following:

#### C -> S `dokokashira_door:delta_gateway_ack`

* `RegistryKey<World>` dimension
* int checksum

Server verifies that the checksum is correct for this dimension's GatewayMap. If the checksum is not correct, server sends a full update.

## Todo

* Lotsa log messages to remove.
* If the server/client ever disagree on gateway-map checksums, the server will try to update the client every single tick.
	* Which means if the server/client disagree on the *method* used to calculate checksums, this never *ever* succeeds.
	* (It does succeed in saturating the network connection though.)
	* Possibly throttle checksum stuff, or only send it once.
	* Possibly a way for the server to go "something isn't right. disable prediction on your end"
	* Test more with `clumsy`.
* Test circumstances where the client is not ready to perform prediction.
* Send the next two random-seeds?
* Fix door sound effects.
* Add the actual "is the player close to the door" check.