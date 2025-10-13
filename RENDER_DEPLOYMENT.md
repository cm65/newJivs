# Deploy JiVS Platform to Render.com

Get your JiVS Platform live in 15 minutes with a public URL!

## 📋 Prerequisites

- GitHub account with access to https://github.com/cm65/newJivs
- Render.com account (free) - Sign up at https://render.com

---

## 🚀 Step-by-Step Deployment

### Step 1: Create Render Account

1. Go to https://render.com
2. Click **"Get Started for Free"**
3. Sign up with your GitHub account
4. Authorize Render to access your repositories

---

### Step 2: Deploy PostgreSQL Database

1. From Render Dashboard, click **"New +"** → **"PostgreSQL"**
2. Configure:
   - **Name**: `jivs-postgres`
   - **Database**: `jivs`
   - **User**: `jivs_user`
   - **Region**: Choose closest to you
   - **Plan**: **Free** (sufficient for demo)
3. Click **"Create Database"**
4. Wait 2-3 minutes for provisioning
5. **Save the Internal Database URL** (you'll need this for the backend)

---

### Step 3: Deploy Backend (Spring Boot API)

1. Click **"New +"** → **"Web Service"**
2. Connect to repository: **"cm65/newJivs"**
3. Configure:
   - **Name**: `jivs-backend`
   - **Region**: Same as database
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Environment**: **Docker**
   - **Plan**: **Free**

4. **Environment Variables** - Click "Add Environment Variable" for each:
   ```
   SPRING_PROFILES_ACTIVE=production
   SPRING_DATASOURCE_URL=<paste your postgres Internal Database URL>
   SPRING_DATASOURCE_USERNAME=jivs_user
   SPRING_DATASOURCE_PASSWORD=<paste from postgres database page>
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   SERVER_PORT=8080
   ```

5. **Advanced Settings**:
   - Health Check Path: `/actuator/health`
   - Docker Command: (leave default)

6. Click **"Create Web Service"**
7. Wait 5-10 minutes for build and deploy
8. Once deployed, copy the **service URL** (e.g., `https://jivs-backend.onrender.com`)

---

### Step 4: Deploy Frontend (React)

1. Click **"New +"** → **"Web Service"**
2. Connect to repository: **"cm65/newJivs"**
3. Configure:
   - **Name**: `jivs-frontend`
   - **Region**: Same as backend
   - **Branch**: `main`
   - **Root Directory**: `frontend`
   - **Environment**: **Docker**
   - **Plan**: **Free**

4. **Environment Variables**:
   ```
   VITE_API_URL=<paste your backend URL from Step 3>
   ```

5. Click **"Create Web Service"**
6. Wait 5-10 minutes for build and deploy

---

### Step 5: Access Your Live Application! 🎉

Once both services show **"Live"** status:

**Your JiVS Platform is live at:**
```
https://jivs-frontend.onrender.com
```

**Login with:**
- Username: `admin`
- Password: `password`

---

## ✅ Test the Deployment

1. Go to your frontend URL
2. Login with credentials above
3. Navigate to **Documents** section
4. Upload a test document
5. Archive it and search for it

---

## 🔧 Troubleshooting

### Backend Won't Start
- Check Environment Variables are correct
- Check database connection in backend logs
- Verify DATABASE_URL format: `postgresql://user:password@host:port/database`

### Frontend Can't Connect to Backend
- Verify `VITE_API_URL` points to your backend URL
- Check CORS settings in backend
- Look at browser console for errors

### Database Connection Errors
- Ensure database is in **"Available"** state
- Check username/password are correct
- Use **Internal Database URL** not External

### Free Plan Limitations
- Services sleep after 15 min of inactivity
- First request after sleep takes ~30 seconds to wake up
- Database limited to 1GB storage

---

## 📊 Monitor Your Deployment

**Render Dashboard shows:**
- Real-time logs
- Build history
- Resource usage
- Error alerts

Access logs: Click on service → **"Logs"** tab

---

## 🔐 Production Security Checklist

Before sharing publicly:

1. **Change Default Password**:
   - Login as admin
   - Go to Settings → Change password

2. **Add Environment Variables** for secrets:
   ```
   JWT_SECRET=<generate-random-string>
   ENCRYPTION_KEY=<generate-random-string>
   ```

3. **Enable HTTPS** (automatic on Render)

4. **Set up custom domain** (optional):
   - Go to service → Settings → Custom Domain
   - Add your domain (e.g., `jivs.yourdomain.com`)

---

## 💰 Cost Estimate

**Free Tier (Perfect for demos):**
- PostgreSQL: Free (1GB storage)
- Backend Service: Free (750 hours/month)
- Frontend Service: Free (750 hours/month)
- **Total: $0/month**

**Upgrade when needed:**
- More storage: $7/month (10GB)
- Always-on services: $7/month per service
- Custom domains: Free

---

## 🎯 Share Your Live App

Once deployed, share this:

```
🚀 Check out JiVS Platform!

Live Demo: https://jivs-frontend.onrender.com
Login: admin / password

Features:
✅ Document upload & archiving
✅ Full-text search
✅ Data quality management
✅ Compliance (GDPR/CCPA)

GitHub: https://github.com/cm65/newJivs
```

---

## 📞 Need Help?

- Render Docs: https://render.com/docs
- GitHub Issues: https://github.com/cm65/newJivs/issues
- Render Community: https://community.render.com

---

**Next Steps:**
- Set up monitoring with Render alerts
- Configure custom domain
- Set up automated database backups
- Add SSL certificate (automatic)

🎉 **Congratulations! Your app is live!**
