# ⚡ Railway Quick Setup - 2 Minutes

Railway dashboard should be open. Follow these steps EXACTLY:

---

## 🔧 BACKEND CONFIGURATION (90 seconds)

### Step 1: Find Backend Service
1. In Railway dashboard, find the service named **"newJivs backend"** or **"backend"**
2. **CLICK** on it

### Step 2: Add Variables
1. **CLICK** the **"Variables"** tab (near the top)
2. **CLICK** the **"New Variable"** button
3. Add **ONE** variable:
   - Name: `SPRING_PROFILES_ACTIVE`
   - Value: `production`
4. **CLICK** "Add" or "Save"
5. **IMPORTANT**: Make sure the PostgreSQL database is **LINKED** to this service
   - Go to **"Settings"** tab → **"Service Variables"**
   - You should see `DATABASE_URL` in the list (provided by PostgreSQL)
6. Backend will start redeploying automatically (yellow icon)

**Note:** Railway automatically provides `DATABASE_URL` when PostgreSQL is linked!

---

## 🎨 FRONTEND CONFIGURATION (60 seconds)

### Step 1: Go Back and Find Frontend
1. **CLICK** the back arrow (top left) to go back to project view
2. Find the service named **"newJivs frontend"** or **"frontend"**
3. **CLICK** on it

### Step 2: Add Variables
1. **CLICK** the **"Variables"** tab
2. **CLICK** the **"RAW Editor"** button
3. **DELETE** everything in the editor
4. **COPY** this entire block:

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

5. **PASTE** into the Raw Editor
6. **CLICK** "Update Variables"
7. Frontend will start redeploying automatically

---

## ⏰ WAIT (3-5 minutes)

Both services will show **yellow** (building), then turn **green** (deployed).

**Backend**: 3-5 minutes to rebuild
**Frontend**: 2-3 minutes to rebuild

---

## ✅ DONE!

When both are **GREEN**, tell Claude and he'll test the login immediately!

Your JiVS Platform will be fully working at:
**https://jivs-frontend-production.up.railway.app**

Login credentials:
- Username: `admin`
- Password: `password`
