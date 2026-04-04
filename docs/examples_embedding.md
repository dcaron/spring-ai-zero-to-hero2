# Embedding Examples

All Embedding examples are packaged in `embedding` module, e.g.
* Embedding Example 01 — Basic Embeddings
* Embedding Example 02 — Cosine Similarity
* Embedding Example 03 — Large Documents and Chunking
* Embedding Example 04 — Document Readers (JSON, Text, PDF)

## Embedding Example 01

Open the `BasicEmbeddingController.java` and explore the contents.

Test The Endpoints:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/embed/01/text
```
```
http localhost:8080/embed/01/dimension
```
</td>
<td>

```
http://localhost:8080/embed/01/text
```
```
http://localhost:8080/embed/01/dimension
```
</td>
</tr>
</table>

## Embedding Example 02

Open the `SimilarityController.java` and explore the contents.

Test The Endpoints:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/embed/02/words
```
```
http localhost:8080/embed/02/quotes
```
</td>
<td>

```
http://localhost:8080/embed/02/words
```
```
http://localhost:8080/embed/02/quotes
```
</td>
</tr>
</table>

## Embedding Example 03

Open the `EmbeddingRequestController.java` and explore the contents.

Test The Endpoints:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/embed/03/big
```
```
http localhost:8080/embed/03/chunk
```
</td>
<td>

```
http://localhost:8080/embed/03/big
```
```
http://localhost:8080/embed/03/chunk
```
</td>
</tr>
</table>


## Embedding Example 04

Open the `DocumentController.java` and explore the contents.

Test The Endpoints:
<table>
<tr>
<th>Command</th>
<th>URL</th>
</tr>
<tr>
<td>

```
http localhost:8080/embed/04/json/bikes
```
```
http localhost:8080/embed/04/text/works
```
```
http localhost:8080/embed/04/pdf/pages
```
```
http localhost:8080/embed/04/pdf/para
```
</td>
<td>

```
http://localhost:8080/embed/04/json/bikes
```
```
http://localhost:8080/embed/04/text/works
```
```
http://localhost:8080/embed/04/pdf/pages
```
```
http://localhost:8080/embed/04/pdf/para
```
</td>
</tr>
</table>
