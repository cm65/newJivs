# ✅ CORRECT Railway Environment Variables

## 🔧 BACKEND Variables (Use These!)

Copy and paste this EXACT block into Railway backend's RAW Editor:

```
SPRING_PROFILES_ACTIVE=production
```

**WHY THIS WORKS:**
- Railway automatically provides `DATABASE_URL` when you add PostgreSQL service
- The `application-production.yml` is configured to use `DATABASE_URL`
- No need to manually configure database connection!
- Just make sure your PostgreSQL service is LINKED to the backend service in Railway

---

## 🎨 FRONTEND Variables (Same as before)

```
VITE_API_URL=https://jivs-backend-production.up.railway.app/api/v1
VITE_WS_URL=wss://jivs-backend-production.up.railway.app/ws
VITE_APP_NAME=JiVS Platform
VITE_APP_VERSION=1.0.0
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_DEBUG=false
VITE_MAX_FILE_SIZE=104857600
VITE_MAX_FILES=10
```

---

## ⚡ Quick Steps:

1. Railway Dashboard → Backend Service → Variables → RAW Editor
2. Paste the BACKEND block above
3. Click "Update Variables"
4. Backend Service → Frontend Service → Variables → RAW Editor
5. Paste the FRONTEND block above
6. Click "Update Variables"
7. Wait 3-5 minutes for both to turn GREEN

---

✅ This will work perfectly!
