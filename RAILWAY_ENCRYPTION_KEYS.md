# Railway Encryption Keys Configuration

**Date**: October 21, 2025
**Status**: ⚠️ READY TO DEPLOY

---

## 🔑 Generated Encryption Keys

### Master Key (ENCRYPTION_MASTER_KEY)
```
euiTn1A/BqbmSEjntK7WY/MtXKTXynI2fVHEbmw68YA=
```

### Encryption Key (ENCRYPTION_KEY)
```
xboRPNDKbjyNqyjvZLP8vECOOyNpa7nGn6e3eSOPGsw=
```

**⚠️ IMPORTANT**: These are 256-bit AES keys. Keep them secure!

---

## 🚀 Set in Railway (Required Before Deployment)

Run these commands in Railway CLI or use the Railway dashboard:

### Option 1: Railway CLI
```bash
railway variables set ENCRYPTION_MASTER_KEY="euiTn1A/BqbmSEjntK7WY/MtXKTXynI2fVHEbmw68YA="
railway variables set ENCRYPTION_KEY="xboRPNDKbjyNqyjvZLP8vECOOyNpa7nGn6e3eSOPGsw="
```

### Option 2: Railway Dashboard
1. Go to: https://railway.app/dashboard
2. Select `jivs-backend-production` service
3. Go to **Variables** tab
4. Click **+ New Variable**
5. Add:
   - Name: `ENCRYPTION_MASTER_KEY`
   - Value: `euiTn1A/BqbmSEjntK7WY/MtXKTXynI2fVHEbmw68YA=`
6. Click **+ New Variable** again
7. Add:
   - Name: `ENCRYPTION_KEY`
   - Value: `xboRPNDKbjyNqyjvZLP8vECOOyNpa7nGn6e3eSOPGsw=`
8. Click **Deploy** to apply changes

---

## ✅ After Setting Keys

1. **Redeploy** the backend service
2. **Test encryption** by uploading a file
3. **Verify** file is encrypted on disk:
   ```bash
   # File should be gibberish (encrypted)
   cat /var/jivs/storage/documents/<storage-id>
   ```
4. **Test download** - should decrypt automatically

---

## 🔒 Security Notes

- ✅ Keys are 256-bit (32 bytes) - military-grade encryption
- ✅ Keys are Base64-encoded for safe storage
- ⚠️ **Never commit these keys to Git** (this file is for reference only)
- ⚠️ **Store keys in a password manager** (1Password, LastPass, etc.)
- ⚠️ **Rotate keys periodically** (every 90 days recommended)

---

## 🔄 Key Rotation (Future)

To rotate keys without downtime:

1. Generate new keys:
   ```bash
   NEW_MASTER_KEY=$(openssl rand -base64 32)
   NEW_ENCRYPTION_KEY=$(openssl rand -base64 32)
   ```

2. Set new keys in Railway

3. Redeploy application

4. Re-encrypt existing files (if needed)

---

## ⚠️ DO NOT DELETE THIS FILE

Keep this file for reference. It contains the keys needed for:
- Decrypting existing files
- Disaster recovery
- Key rotation procedures

**Store securely**: Add this file to `.gitignore` if not already ignored.

---

**Generated**: October 21, 2025, 17:30
**Algorithm**: AES-256-GCM
**Key Size**: 256 bits (32 bytes)
**Encoding**: Base64
