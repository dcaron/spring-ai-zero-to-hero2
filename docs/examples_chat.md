# Chat Examples

All Chat examples are packaged in `chat` module, e.g.
* Chat Example 01 — Basic ChatModel
* Chat Example 02 — ChatClient vs ChatModel
* Chat Example 03 — Prompt Templates
* Chat Example 04 — Structured Output
* Chat Example 05 — Tool/Function Calling
* Chat Example 06 — System Roles
* Chat Example 07 — Multimodal
* Chat Example 08 — Streaming

## Chat Example 01

Open the `BasicPromptController.java` and explore the contents.

Test The Endpoint:

<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/01/joke
```

</td>
<td>

```
http://localhost:8080/chat/01/joke
```

</td>
</tr>
</table>


## Chat Example 02

Open the `ChatClientController.java` and `ChatModelController.java` and explore the contents.

Test The Endpoints:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/02/client/joke
```
```
http localhost:8080/chat/02/client/threeJokes
```
```
http localhost:8080/chat/02/model/joke
```
```
http localhost:8080/chat/02/model/threeJokes
```
</td>
<td>

```
http://localhost:8080/chat/02/client/joke
```
```
http://localhost:8080/chat/02/client/threeJokes
```
```
http://localhost:8080/chat/02/model/joke
```
```
http://localhost:8080/chat/02/model/threeJokes
```
</td>
</tr>
</table>

## Chat Example 03

Open the `PromptTemplateController.java` and explore the contents.

Test The Endpoint:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/03/joke
```
```
http localhost:8080/chat/03/plays
```
</td>
<td>

```
http://localhost:8080/chat/03/joke
```
```
http://localhost:8080/chat/03/plays
```
</td>
</tr>
</table>


## Chat Example 04

Open the `StructuredOutputConverterController.java` and explore the contents.

Test The Endpoint:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/04/plays
```
```
http localhost:8080/chat/04/plays/list
```
```
http localhost:8080/chat/04/plays/map
```
```
http localhost:8080/chat/04/plays/object
```
</td>
<td>

```
http://localhost:8080/chat/04/plays
```
```
http://localhost:8080/chat/04/plays/list
```
```
http://localhost:8080/chat/04/plays/map
```
```
http://localhost:8080/chat/04/plays/object
```
</td>
</tr>
</table>


## Chat Example 05

Open the `ToolController.java` and explore the contents.

Test The Endpoint:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/05/time
```
```
http localhost:8080/chat/05/dayOfWeek
```
```
http localhost:8080/chat/05/weather
```
```
http localhost:8080/chat/05/pack
```
```
http localhost:8080/chat/05/search
```
</td>
<td>

```
http://localhost:8080/chat/05/time
```
```
http://localhost:8080/chat/05/dayOfWeek
```
```
http://localhost:8080/chat/05/weather
```
```
http://localhost:8080/chat/05/pack
```
```
http://localhost:8080/chat/05/search
```
</td>
</tr>
</table>


## Chat Example 06

Open the `RoleController.java` and explore the contents.

Test The Endpoint:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/06/fruit
```
```
http localhost:8080/chat/06/veg
```
</td>
<td>

```
http://localhost:8080/chat/06/fruit
```
```
http://localhost:8080/chat/06/veg
```
</td>
</tr>
</table>

## Chat Example 07

Open the `MultiModalController.java` and explore the contents.

Test The Endpoint:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/07/explain
```
</td>
<td>

```
http://localhost:8080/chat/07/explain
```
</td>
</tr>
</table>

## Chat Example 08

Open the `StreamingChatModelController.java` and explore the contents.

Test The Endpoint:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/chat/08/essay
```
</td>
<td>

```
http://localhost:8080/chat/08/essay
```
</td>
</tr>
</table>
