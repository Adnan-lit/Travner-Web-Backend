# Files Intentionally Removed

The following files have been **intentionally removed** or renamed because they depend on optional services (Redis, Elasticsearch) that are disabled for local development:

## Removed Files:

1. **`src/main/java/org/adnan/travner/config/CacheConfig.java`**
   - Depends on: Redis
   - Reason: Redis dependency is disabled in pom.xml
   - Backup location: `CacheConfig.java.disabled`

2. **`src/main/java/org/adnan/travner/service/AIService.java`**
   - Depends on: WebFlux/Netty
   - Reason: Netty dependency conflicts
   - Backup location: `AIService.java.disabled`

3. **`src/main/java/org/adnan/travner/controller/AIController.java`**
   - Depends on: WebFlux/Netty, AIService
   - Reason: Netty dependency conflicts
   - Backup location: `AIController.java.disabled`

## ⚠️ Important for IntelliJ IDEA Users:

If IntelliJ restores these files from local history or git:
1. **DO NOT** keep them
2. **DELETE** them again
3. Or **Reload** the project: File → Reload All from Disk

## To Re-enable These Features:

See `STARTUP_INSTRUCTIONS.md` section "AI Features Temporarily Disabled" for instructions on how to re-enable Redis, Elasticsearch, and AI features.

## Current Status:

✅ Application works perfectly without these files
✅ All core features are functional
✅ Backend compiles and runs successfully





