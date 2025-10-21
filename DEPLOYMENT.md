# Travner Backend - Deployment Guide

## Quick Deploy to Railway

### Step 1: Prerequisites
- Railway account (https://railway.app/)
- GitHub repository with your code
- MongoDB Atlas database (https://www.mongodb.com/cloud/atlas)
- OpenRouter API key (https://openrouter.ai/)

### Step 2: Setup MongoDB Atlas
1. Create a cluster at https://cloud.mongodb.com/
2. Create a database user
3. Whitelist all IP addresses (0.0.0.0/0) or configure Network Access
4. Get your connection string:
   ```
   mongodb+srv://username:password@cluster.mongodb.net/TravnerDB?retryWrites=true&w=majority
   ```

### Step 3: Deploy to Railway
1. Go to https://railway.app/
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Choose your repository
5. Railway will auto-detect the Maven project

### Step 4: Configure Environment Variables
In Railway project settings, add these variables:

```env
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/TravnerDB?retryWrites=true&w=majority
MONGODB_DATABASE=TravnerDB
OPENROUTER_API_KEY=sk-or-v1-your-api-key-here
SPRING_PROFILES_ACTIVE=prod
ADMIN_USERNAME=admin
ADMIN_PASSWORD=YourStrongPasswordHere123!
LOG_LEVEL=WARN
PORT=8080
```

### Step 5: Configure Build Settings
Railway should auto-detect, but verify:
- **Build Command**: `mvn clean package -DskipTests`
- **Start Command**: `java -jar target/Travner-0.0.1-SNAPSHOT.jar`

### Step 6: Deploy
1. Click "Deploy" button
2. Wait for build to complete
3. Railway will provide a public URL: `https://your-app.railway.app`

---

## Alternative: Deploy to Heroku

### Step 1: Install Heroku CLI
```bash
# Windows
choco install heroku-cli

# macOS
brew tap heroku/brew && brew install heroku

# Linux
curl https://cli-assets.heroku.com/install.sh | sh
```

### Step 2: Login and Create App
```bash
heroku login
cd Travner-Web-Backend
heroku create your-app-name
```

### Step 3: Set Environment Variables
```bash
heroku config:set MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/TravnerDB"
heroku config:set MONGODB_DATABASE=TravnerDB
heroku config:set OPENROUTER_API_KEY=sk-or-v1-your-api-key-here
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set ADMIN_USERNAME=admin
heroku config:set ADMIN_PASSWORD=YourStrongPasswordHere123!
heroku config:set LOG_LEVEL=WARN
```

### Step 4: Create Procfile
Create a file named `Procfile` in the root directory:
```
web: java -jar target/Travner-0.0.1-SNAPSHOT.jar --server.port=$PORT
```

### Step 5: Deploy
```bash
git add .
git commit -m "Deploy to Heroku"
git push heroku main
```

### Step 6: Open Application
```bash
heroku open
```

---

## Alternative: Docker Deployment

### Step 1: Build Docker Image
```bash
cd Travner-Web-Backend
docker build -t travner-backend .
```

### Step 2: Run Container
```bash
docker run -d \
  -p 8080:8080 \
  -e MONGODB_URI="mongodb://host.docker.internal:27017" \
  -e MONGODB_DATABASE=TravnerDB \
  -e OPENROUTER_API_KEY=sk-or-v1-your-api-key-here \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=YourStrongPassword123! \
  --name travner-backend \
  travner-backend
```

### Step 3: Verify
```bash
curl http://localhost:8080/actuator/health
```

---

## Alternative: AWS Elastic Beanstalk

### Step 1: Install EB CLI
```bash
pip install awsebcli
```

### Step 2: Initialize EB Application
```bash
cd Travner-Web-Backend
eb init -p java-21 travner-backend --region us-east-1
```

### Step 3: Create Environment
```bash
eb create travner-production
```

### Step 4: Set Environment Variables
```bash
eb setenv MONGODB_URI="mongodb+srv://..." \
          MONGODB_DATABASE=TravnerDB \
          OPENROUTER_API_KEY=sk-or-v1-... \
          SPRING_PROFILES_ACTIVE=prod \
          ADMIN_USERNAME=admin \
          ADMIN_PASSWORD=YourStrongPassword123! \
          LOG_LEVEL=WARN
```

### Step 5: Deploy
```bash
mvn clean package
eb deploy
```

---

## Post-Deployment Configuration

### 1. Verify Health
```bash
curl https://your-backend-url/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "ping": {"status": "UP"},
    "mongo": {"status": "UP"}
  }
}
```

### 2. Test AI Endpoint
```bash
curl -X POST https://your-backend-url/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, how are you?"}'
```

### 3. Test Authentication
```bash
curl -X POST https://your-backend-url/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userName": "admin", "password": "YourStrongPassword123!"}'
```

### 4. Update Frontend Configuration
Update `Travner-Web-Frontend/src/environments/environment.prod.ts`:
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://your-backend-url/api',
  wsUrl: 'wss://your-backend-url/ws',
  // ...
};
```

---

## Monitoring

### Application Logs
- **Railway**: View logs in Railway dashboard
- **Heroku**: `heroku logs --tail`
- **Docker**: `docker logs travner-backend -f`
- **AWS EB**: `eb logs`

### Health Check Endpoint
Set up monitoring for: `https://your-backend-url/actuator/health`

Recommended monitoring services:
- UptimeRobot (free)
- Pingdom
- New Relic
- Datadog

---

## Troubleshooting

### Application Won't Start
1. Check environment variables are set correctly
2. Verify MongoDB connection string
3. Check logs for errors:
   ```bash
   # Railway: Check dashboard logs
   # Heroku: heroku logs --tail
   # Docker: docker logs travner-backend
   ```

### MongoDB Connection Failed
1. Verify MongoDB URI is correct
2. Check network access settings in MongoDB Atlas
3. Ensure IP whitelist includes deployment platform IPs

### AI Chatbot Not Working
1. Verify `OPENROUTER_API_KEY` is set
2. Test API key at https://openrouter.ai/
3. Check logs for API errors
4. Verify internet connectivity from server

### Out of Memory Errors
1. Increase heap size:
   ```bash
   # In Railway/Heroku config:
   JAVA_OPTS=-Xmx512m -Xms256m
   ```
2. Upgrade instance size
3. Optimize application memory usage

---

## Scaling

### Horizontal Scaling
- **Railway**: Configure auto-scaling in settings
- **Heroku**: `heroku ps:scale web=2`
- **AWS**: Configure auto-scaling group

### Database Scaling
- Upgrade MongoDB Atlas cluster tier
- Enable read replicas
- Implement caching (Redis)

### CDN Configuration
- Use CloudFlare for static assets
- Configure CloudFront for AWS deployments
- Enable gzip compression

---

## Security Best Practices

1. **Change Default Credentials**
   - Set strong `ADMIN_PASSWORD`
   - Use unique `ADMIN_USERNAME`

2. **Enable HTTPS**
   - Most platforms provide this automatically
   - Configure SSL certificates for custom domains

3. **Restrict CORS**
   - Update CORS configuration to allow only production frontend URL

4. **Secure Environment Variables**
   - Never commit secrets to Git
   - Use platform's secrets management

5. **Regular Updates**
   - Update dependencies regularly
   - Apply security patches promptly

---

## Backup Strategy

### Database Backups
1. Enable automated backups in MongoDB Atlas
2. Configure retention period (30 days recommended)
3. Test restore procedure monthly

### Application Backups
1. Tag releases in Git
2. Keep previous 3 deployment artifacts
3. Document rollback procedure

---

## Cost Optimization

### Free Tier Options
- **Railway**: $5 free credits per month
- **Heroku**: Free tier available (with limitations)
- **MongoDB Atlas**: 512MB free tier
- **OpenRouter**: Free models available

### Production Recommendations
- **Backend**: 1GB RAM minimum ($10-20/month)
- **Database**: Shared cluster ($9/month) or Dedicated M10 ($57/month)
- **CDN**: CloudFlare free tier
- **Monitoring**: UptimeRobot free tier

### Total Estimated Cost
- **Development**: $0-10/month (using free tiers)
- **Production**: $20-100/month (depending on traffic)

---

## Support

For deployment issues:
1. Check platform-specific documentation
2. Review application logs
3. Verify environment variables
4. Test health check endpoint
5. Contact support if needed

---

**Deployment Complete!** 🚀

Your Travner backend is now live and ready to serve requests.
