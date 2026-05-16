## Architecture

Load-bearing rules. For concrete layouts and recipes see [PATTERNS.md](PATTERNS.md).

### Module layout

- `features/` — domain packages organized by feature
- `infrastructure/` — external integrations (Firebase, file storage, DB driver setup)
- `plugins/` — Ktor plugin wire-up only
- `common/` — base value types, shared transport types, generic utilities
- `config/` — app configuration

### Domain kinds

- **Aggregate** (`auth`, `posts`, `recipes`, `users`, `uploads`) — own primary entities. Operations live in `usecase/` (one class per operation).
- **Polymorphic** (`comments`, `votes`, `likes`) — attach to multiple aggregates via `(target_type, target_id)`. Operations live in a single `*Service`.

Rule of thumb: aggregate use-cases scale into richer workflows (clone, publish, copy media, fire events) without bloating a god-service. Polymorphic services stay thin because their operations are CRUD-on-target and share resolver + table logic.

### Dependency rules

```
features/<aggregate>  ──reads──>           features/<polymorphic>   (for enrichment)
features/<aggregate>  ──implements──>      <polymorphic>'s *TargetResolver
features/<polymorphic> ──depends on──>     abstractions in own package only
features/*            ──depends on──>      infrastructure/* via interfaces
infrastructure/*      ──MUST NOT depend on── features/*
```

- Aggregates MUST NOT expose write methods for polymorphic entities. ❌ `createPost.addComment(...)` ✅ `commentService.create(target = CommentTarget.Post(id), ...)`.
- Polymorphic services do NOT know concrete aggregate types — they consume `*TargetResolver` abstractions defined in their own package.
- Enrichment is read-only and uses batch APIs in parallel.
- Use-cases depend only on abstractions from `features/*` (own or other feature packages). They MUST NOT depend on `infrastructure/*` directly — infrastructure is wrapped by feature-level abstractions.

### What goes in `common`

OK: base value types (`Id`, `Page<T>`), generic utilities, shared transport types.
Not OK: domain-specific errors (live with their contract), cross-domain business logic.

### REST routing for polymorphic resources

- Create/list via parent: `POST /posts/{postId}/comments`, `GET /posts/{postId}/comments`. Controller lives in the aggregate package, delegates to the polymorphic service.
- Direct ops via polymorphic id: `DELETE /comments/{id}`, `PATCH /comments/{id}`.
