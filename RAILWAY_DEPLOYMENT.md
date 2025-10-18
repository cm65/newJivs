# 🚂 Railway Deployment Guide for JiVS Platform

## Quick Start (5 minutes)

### Step 1: Push Latest Code to GitHub

All Railway configuration files are now committed. Just make sure your GitHub repo is up to date:

```bash
git push origin main
```

### Step 2: Create Railway Project

1. Go to https://railway.app
2. Click **"Start a New Project"**
3. Click **"Deploy from GitHub repo"**
4. Select **cm65/newJivs** repository
5. Railway will ask you to authorize GitHub access (if first time)

### Step 3: Add PostgreSQL Database

1. In your Railway project, click **"+ New"**
2. Select **"Database"**
3. Choose **"PostgreSQL"**
4. Railway will provision the database automatically

### Step 4: Deploy Backend Service

1. Click **"+ New"** in your Railway project
2. Select **"GitHub Repo"**
3. Choose **cm65/newJivs**
4. Railway will ask which service to deploy
5. Set **Root Directory** to `backend`
6. Railway will auto-detect Spring Boot and start building!

**Configure Backend Environment Variables:**
Click on the backend service → **Variables** tab → Add these:

```
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=${{Postgres.DATABASE_URL}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
PORT=${{PORT}}
```

**Railway Magic:** `${{Postgres.DATABASE_URL}}` automatically references your PostgreSQL service!

### Step 5: Deploy Frontend Service

1. Click **"+ New"** in your Railway project again
2. Select **"GitHub Repo"**
3. Choose **cm65/newJivs** again
4. Set **Root Directory** to `frontend`
5. Railway will auto-detect Vite/React!

**Configure Frontend Environment Variables:**
Click on the frontend service → **Variables** tab → Add:

```
VITE_API_URL=https://${{backend.RAILWAY_PUBLIC_DOMAIN}}/api/v1
VITE_WS_URL=wss://${{backend.RAILWAY_PUBLIC_DOMAIN}}/ws
```

**Railway Magic:** `${{backend.RAILWAY_PUBLIC_DOMAIN}}` automatically uses your backend's URL!

### Step 6: Generate Public Domains

For **both** backend and frontend services:

1. Click on the service
2. Go to **Settings** tab
3. Scroll to **Networking** section
4. Click **"Generate Domain"**
5. Railway will give you a public HTTPS URL!

---

## ✅ You're Done!

Your URLs will be:
- **Backend:** `https://jivs-backend-production.up.railway.app`
- **Frontend:** `https://jivs-frontend-production.up.railway.app`

**Login Credentials:**
- Username: `admin`
- Password: `password`

---

## 🔧 What We Configured

### Backend (`backend/railway.toml` + `backend/nixpacks.toml`)
- ✅ Builds with Maven
- ✅ Uses Java 21
- ✅ Runs with production profile
- ✅ Health checks at `/actuator/health`
- ✅ Auto-restarts on failure

### Frontend (`frontend/railway.toml` + `frontend/nixpacks.toml`)
- ✅ Builds with npm/Vite
- ✅ Serves with nginx
- ✅ Health checks at `/health`
- ✅ Static assets optimized

### Database
- ✅ PostgreSQL 15
- ✅ Automatic connection from backend
- ✅ Environment variables injected

---

## 🎯 Advantages Over Render

| Feature | Railway | Render Free |
|---------|---------|-------------|
| **Sleep/Spin-down** | ❌ No (always on) | ✅ Yes (15 min) |
| **Build Speed** | ⚡ Fast (~2-3 min) | 🐌 Slow (~5-10 min) |
| **PostgreSQL** | ✅ Included | ✅ Included |
| **Auto-deploy** | ✅ Yes | ✅ Yes |
| **Price** | $5/month | Free (limited) |
| **Custom domains** | ✅ Yes | ✅ Yes |

---

## 🔄 Auto-Deploy on Push

Railway automatically redeploys when you push to `main` branch!

```bash
git add .
git commit -m "your changes"
git push origin main
# Railway rebuilds and redeploys automatically! 🚀
```

---

## 🐛 Troubleshooting

### Backend won't start
1. Check logs: Click backend service → **Deployments** tab → Click latest deployment → **View Logs**
2. Common issues:
   - Database not connected: Check environment variables
   - Port binding: Railway sets `$PORT` automatically

### Frontend shows blank page
1. Check logs: Click frontend service → **Deployments** → **View Logs**
2. Check environment variables have correct backend URL
3. Verify CORS settings in backend allow frontend domain

### Database connection failed
1. Make sure PostgreSQL service is running (green status)
2. Verify backend has correct `DATABASE_URL` variable
3. Check if Flyway migrations ran successfully in backend logs

---

## 📊 Monitoring

**View Logs:**
- Click any service → **Deployments** tab → Click deployment → **View Logs**

**View Metrics:**
- Click any service → **Metrics** tab
- See CPU, Memory, Network usage

**Health Checks:**
- Railway automatically monitors your health check endpoints
- Backend: `/actuator/health`
- Frontend: `/health` (from nginx)

---

## 💰 Cost Estimate

**Development (Hobby Plan - $5/month):**
- 3 services (Backend, Frontend, PostgreSQL)
- 500 execution hours/month
- 100 GB bandwidth
- Enough for development and testing!

**Production (Pro Plan - $20/month):**
- Unlimited services
- Priority support
- Custom domains
- High availability

---

## 🔐 Security

**Automatic HTTPS:**
- ✅ SSL certificates automatically provisioned
- ✅ HTTP → HTTPS redirect enabled

**Environment Variables:**
- ✅ Encrypted at rest
- ✅ Injected at runtime
- ✅ Not visible in logs

**Database:**
- ✅ Encrypted connections
- ✅ Automatic backups
- ✅ Private networking between services

---

## 📚 Additional Resources

- Railway Docs: https://docs.railway.app
- Railway Discord: https://discord.gg/railway
- GitHub Deployments: https://docs.railway.app/deploy/deployments

---

**Need Help?**
Check Railway logs first, then Railway Discord community is very responsive!

🚀 **Happy Deploying!**
