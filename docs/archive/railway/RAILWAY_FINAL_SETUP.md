# 🚂 Railway Final Setup - CORRECTED

## ✅ Current Status

**Both services are deployed and running!**
- ✅ Frontend: HTTP 200 (https://jivs-frontend-production.up.railway.app)
- ✅ Backend: HTTP 200 (https://jivs-backend-production.up.railway.app)
- ✅ Database: Connected and healthy
- ✅ Build timeout: Fixed with .dockerignore optimization

## ❌ Remaining Issue

**Login fails** because environment variables need to be configured in Railway dashboard.

---

## 🔧 STEP 1: Backend Variables (1 minute)

### What You Need to Do:

1. **Open Railway Dashboard** → Go to your backend service
2. **Click "Variables"** tab
3. **Click "New Variable"** button
4. Add this **ONE** variable:
   - Name: `SPRING_PROFILES_ACTIVE`
   - Value: `production`
5. **Click "Add"** or **"Save"**

### Verify PostgreSQL is Linked:

6. **Click "Settings"** tab
7. Scroll to **"Service Variables"** section
8. You should see `DATABASE_URL` in the list
   - If you DON'T see it, go back to your project → Click "+" → "Database" → Link PostgreSQL to backend

**That's it for backend!** Railway automatically provides `DATABASE_URL` when PostgreSQL is linked.

---

## 🎨 STEP 2: Frontend Variables (2 minutes)

### What You Need to Do:

1. **Go back to project view** → Click your frontend service
2. **Click "Variables"** tab
3. **Click "RAW Editor"** button (easier for bulk paste)
4. **DELETE** everything in the editor
5. **COPY** this entire block:

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

6. **PASTE** into the Raw Editor
7. **Click "Update Variables"**

---

## ⏰ STEP 3: Wait for Redeployment (3-5 minutes)

Both services will automatically redeploy:
- **Backend**: 3-5 minutes (yellow → green)
- **Frontend**: 2-3 minutes (yellow → green)

---

## ✅ STEP 4: Test Login

Once both services are **GREEN**:

1. Open: **https://jivs-frontend-production.up.railway.app**
2. Login with:
   - Username: `admin`
   - Password: `password`

**It should work!**

---

## 🔍 Why This Works

### Backend:
- `SPRING_PROFILES_ACTIVE=production` → Uses `application-production.yml`
- `DATABASE_URL` → Automatically provided by Railway's PostgreSQL service
- The Spring Boot app reads `DATABASE_URL` from environment (configured in application-production.yml line 19)

### Frontend:
- All `VITE_*` variables are read at **build time** (not runtime)
- Vite embeds these values into the JavaScript bundle
- The app knows where to find the backend API

---

## 🐛 If Login Still Fails

1. **Check backend logs:**
   - Railway Dashboard → Backend Service → "Deployments" tab → Click latest deployment → View logs
   - Look for Flyway migration errors or database connection errors

2. **Check if admin user was created:**
   - Flyway migration V106 should create admin user automatically
   - If not, there might be a migration error in the logs

3. **Check CORS:**
   - Backend logs should show if CORS is blocking requests
   - The `application-production.yml` already has correct CORS config for Railway URLs

---

## 📋 Summary

**What you need to add in Railway:**

| Service | Variables |
|---------|-----------|
| Backend | `SPRING_PROFILES_ACTIVE=production` |
| Frontend | All 8 VITE_* variables (copy from block above) |

**PostgreSQL:** Should be automatically linked and provide `DATABASE_URL`

---

**Once you've added these variables, let me know and I'll test the login!**
