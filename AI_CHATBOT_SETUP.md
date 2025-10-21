# 🤖 AI Chatbot Setup Guide

## ✅ Status

The AI chatbot is now **fully enabled and functional**! All Netty dependency issues have been resolved.

## 🔑 Get Your OpenRouter API Key

### Step 1: Visit OpenRouter
Go to: **https://openrouter.ai/**

### Step 2: Sign Up / Login
- Click "Sign In" or "Get Started"
- Sign up with Google, GitHub, or email

### Step 3: Get API Key
1. Go to your dashboard: https://openrouter.ai/keys
2. Click **"Create Key"**
3. Give it a name (e.g., "Travner App")
4. Copy the API key (starts with `sk-or-v1-...`)

### Step 4: Add Free Credits (Optional)
- OpenRouter offers $1 free credit for new users
- For testing, this is enough for ~100-200 AI chat messages
- Add payment method for continued usage

## 🛠️ Configure in IntelliJ IDEA

### Method 1: Environment Variables (Recommended)

1. **Open Run Configuration**:
   - Go to **Run** → **Edit Configurations**
   - Select **TravnerApplication**

2. **Add Environment Variable**:
   - Find "Environment variables" section
   - Click the folder icon 📁
   - Add new variable:
     ```
     Name: OPENROUTER_API_KEY
     Value: sk-or-v1-your-actual-key-here
     ```

3. **Save and Run**:
   - Click **Apply** → **OK**
   - Run the application

### Method 2: application.yml (NOT Recommended for production)

Edit `src/main/resources/application.yml`:

```yaml
app:
  ai:
    openrouter:
      api-key: sk-or-v1-your-actual-key-here  # NOT recommended - use environment variable
      base-url: https://openrouter.ai/api/v1
      model: openai/gpt-3.5-turbo
```

⚠️ **Warning**: Don't commit API keys to git!

## 🎯 AI Chatbot Features

### 1. Travel Recommendations
**Endpoint**: `POST /api/ai/recommendations`

**Request**:
```json
{
  "destination": "Paris",
  "interests": "art, history, food",
  "budget": "moderate"
}
```

**Response**:
```json
{
  "destination": "Paris",
  "recommendations": "Here are personalized recommendations for Paris..."
}
```

### 2. AI Chat Assistant
**Endpoint**: `POST /api/ai/chat`

**Request**:
```json
{
  "message": "What are the best places to visit in Tokyo?",
  "history": [
    {"role": "user", "content": "Hi!"},
    {"role": "assistant", "content": "Hello! How can I help you plan your trip?"}
  ]
}
```

**Response**:
```json
{
  "message": "What are the best places to visit in Tokyo?",
  "response": "Tokyo has many amazing places to visit..."
}
```

### 3. Itinerary Generator
**Endpoint**: `POST /api/ai/itinerary`

**Request**:
```json
{
  "destination": "Rome",
  "days": 5,
  "preferences": "ancient history, authentic Italian food"
}
```

**Response**:
```json
{
  "destination": "Rome",
  "days": 5,
  "itinerary": "Day 1: Arrive in Rome and explore the Colosseum..."
}
```

### 4. Health Check
**Endpoint**: `GET /api/ai/health`

**Response**:
```json
{
  "status": "ok",
  "service": "AI Assistant",
  "message": "AI service is available"
}
```

## 🧪 Testing the AI Chatbot

### Using curl (Command Line):

```bash
# Health check
curl http://localhost:8080/api/ai/health

# Get recommendations
curl -X POST http://localhost:8080/api/ai/recommendations \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Barcelona",
    "interests": "architecture, beaches",
    "budget": "moderate"
  }'

# Chat
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tell me about Barcelona",
    "history": []
  }'
```

### Using Postman:

1. Create a new POST request
2. URL: `http://localhost:8080/api/ai/chat`
3. Headers: `Content-Type: application/json`
4. Body (raw JSON):
   ```json
   {
     "message": "What should I pack for Iceland in winter?",
     "history": []
   }
   ```
5. Click **Send**

### Using the Frontend:

Once the frontend is running at `http://localhost:4200`:
1. Navigate to the AI Chat section
2. Type your message
3. Get instant AI-powered travel advice!

## 💰 Cost Estimation

Using OpenRouter with GPT-3.5-turbo:
- **Cost**: ~$0.0015 per 1K tokens
- **Average chat message**: ~100-200 tokens
- **$1 free credit**: ~500-1000 messages
- **$10 credit**: ~5,000-10,000 messages

Very affordable for testing and production!

## 🔒 Security Notes

1. **Never commit API keys** to version control
2. Always use **environment variables** in production
3. Add `.env` to `.gitignore`
4. Rotate keys periodically
5. Monitor usage in OpenRouter dashboard

## 🐛 Troubleshooting

### Issue: "AI recommendations are not available"
**Solution**: Make sure `OPENROUTER_API_KEY` environment variable is set

### Issue: "Unable to connect to OpenRouter"
**Solution**: 
- Check your internet connection
- Verify API key is correct
- Check OpenRouter status: https://status.openrouter.ai/

### Issue: "Rate limit exceeded"
**Solution**:
- Wait a few seconds between requests
- Add rate limiting in your code
- Upgrade OpenRouter plan

### Issue: Backend won't start
**Solution**: Make sure all these are set in IntelliJ:
1. `JAVA_HOME` pointing to JDK 21
2. `MONGODB_URI` with valid connection string
3. `OPENROUTER_API_KEY` (optional, but recommended)

## 📚 Available AI Models

You can change the model in `application.yml`:

```yaml
app:
  ai:
    openrouter:
      model: openai/gpt-3.5-turbo    # Default, fast and affordable
      # model: openai/gpt-4           # More powerful, more expensive
      # model: anthropic/claude-2     # Good alternative
      # model: meta-llama/llama-2-70b # Open source option
```

## 🎉 You're Ready!

Your AI chatbot is fully configured and ready to use. Start the backend with the OpenRouter API key configured, and enjoy AI-powered travel recommendations!

---

**Questions?** Check the main `STARTUP_INSTRUCTIONS.md` or `README.md`




