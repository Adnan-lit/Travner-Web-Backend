# Intentionally Disabled Files

Only one file is intentionally disabled in this repository:

| File | Why | How to re-enable |
|---|---|---|
| `src/main/java/org/adnan/travner/config/CacheConfig.java.disabled` | Depends on Redis, which is not a declared dependency. Enabling it without Redis breaks startup. | Add the Spring Data Redis dependency, configure a Redis instance, then rename to `CacheConfig.java`. |

## AI features are enabled

`AIService` and `AIController` are **active, tracked source files** and compile
as part of the normal build. Older revisions of this document claimed they had
been removed because of a Netty conflict; that is no longer true. Do not delete
them.

## IDE note

If IntelliJ restores `CacheConfig.java.disabled` as a compilable `.java` file
from local history, either delete it again or use **File → Reload All from Disk**.
