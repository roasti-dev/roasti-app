## Patterns

Concrete recipes. For the dependency rules see [ARCHITECTURE.md](ARCHITECTURE.md).

### Aggregate domain layout

```
features/<aggregate>/
  model/                                      # value objects + entities + read-models
    <Aggregate>Id.kt
    <Aggregate>.kt
    <Aggregate>View.kt                        # if domain has enrichment
  <Aggregate>Repository.kt
  <Aggregate>Table.kt
  <Aggregate>Resources.kt                     # Ktor typed Resources
  <Aggregate>Routes.kt                        # route definitions
  <Aggregate>Enricher.kt                      # if domain has enrichment
  <Aggregate><Polymorphic>TargetResolver.kt   # one per polymorphic op the aggregate hosts
  usecase/
    Create<Aggregate>.kt
    Get<Aggregate>.kt
    List<Aggregate>.kt
    Update<Aggregate>.kt
    Delete<Aggregate>.kt
    ...                                       # domain-specific operations
```

Each use-case is a class with `suspend operator fun invoke(...)`. Constructor injects only the dependencies that use-case needs. Routes call `useCase(...)` directly — no orchestrator in between.

### Polymorphic domain layout

```
features/<polymorphic>/
  <Polymorphic>.kt                            # entity + target sealed class
  <Polymorphic>Repository.kt
  <Polymorphic>Table.kt
  <Polymorphic>Service.kt                     # all operations
  <Polymorphic>Resources.kt
  <Polymorphic>Routes.kt
  <Polymorphic>TargetResolver.kt              # abstraction implemented by aggregates
```

No `usecase/` for polymorphic domains — operations are CRUD-on-target and share resolver + table logic. A single service is the honest shape.

### Polymorphic target modeling

`<Polymorphic>Target` is a sealed class with typed ids per aggregate variant:

```kotlin
sealed class CommentTarget {
    abstract val type: CommentTargetType
    data class Post(val id: PostId) : CommentTarget() { override val type = CommentTargetType.POST }
    data class Recipe(val id: RecipeId) : CommentTarget() { override val type = CommentTargetType.RECIPE }
}
```

`type` is the persistence/logging discriminator. Raw id is exposed via extension `rawId` for DB writes — typed access stays inside `when` branches.

### Target resolvers

Each polymorphic operation defines its own resolver (`CommentTargetResolver`, `VoteTargetResolver`, `LikeTargetResolver`), not a shared one. Resolvers handle:

- Existence check.
- Visibility / permission check for the requesting user.
- Operation-specific rules (e.g. "comments disabled on this post", "cannot vote on own post").

Resolver returns `Either<<Op>TargetError, <Op>ResolvedTarget>`. Errors live in the polymorphic package, alongside the contract.

### Enrichment (read-side aggregation)

Enrichment is per-use-case, not per-domain. A use-case returns `Post` (raw entity) or `PostView` (enriched read-model) depending on what the caller needs. Don't enrich speculatively.

Domain entities are immutable. Enrichment doesn't mutate — it composes the entity with derived/joined data into a `*View` read-model:

```kotlin
data class PostView(
    val post: Post,
    val commentCount: Int,
    val voteInfo: VoteInfo,
)
```

A dedicated `<Aggregate>Enricher` is the only place that fetches polymorphic data for an aggregate. Batch is the primary API; single-entity delegates to batch:

```kotlin
class PostEnricher(
    private val commentService: CommentService,
    private val voteService: VoteService,
) {
    suspend fun enrich(post: Post, viewerId: UserId): PostView =
        enrich(listOf(post), viewerId).single()

    suspend fun enrich(posts: List<Post>, viewerId: UserId): List<PostView> = coroutineScope {
        val targets = posts.map { CommentTarget.Post(it.id) }
        val counts = async { commentService.countForTargets(targets) }
        val votes = async { voteService.getInfoForTargets(targets, viewerId) }
        posts.map { p ->
            PostView(
                post = p,
                commentCount = counts.await()[p.id] ?: 0,
                voteInfo = votes.await()[p.id] ?: VoteInfo.empty(),
            )
        }
    }
}
```

Use-cases that need enriched data inject the enricher:

```kotlin
class GetPost(
    private val postRepository: PostRepository,
    private val enricher: PostEnricher,
) {
    suspend operator fun invoke(id: PostId, viewerId: UserId): PostView? =
        postRepository.findById(id)?.let { enricher.enrich(it, viewerId) }
}
```

Polymorphic fetches run in parallel via `async { ... }` inside `coroutineScope`.

On Create, construct the `*View` directly with known-zero polymorphic values — don't call the enricher. On Update, calling the enricher is fine for consistency, even though it adds queries.

### Adding a new aggregate domain

1. Create `features/<name>/` per the aggregate layout.
2. Put value objects in `model/` (always include `<Name>Id`).
3. Define repository interface, implementation, and table.
4. For each polymorphic operation this aggregate should host, implement the corresponding `*TargetResolver`. Aggregates pick which polymorphic ops they host — not every aggregate needs comments, votes, or likes.
5. Add use-cases for the domain's operations.
6. Register `*Resources` and `*Routes`.
7. Wire DI in `plugins/DI.kt`.

No changes needed in polymorphic packages.

### Adding a new polymorphic domain

1. Create `features/<name>/` per the polymorphic layout.
2. Define `<Name>TargetResolver` interface in the package.
3. Each aggregate that should host the new polymorphic implements the resolver in its own package.
4. Service handles all operations directly.
