# AI Chatbot Setup Guide

## Overview
The Travner AI Chatbot uses OpenRouter API to provide travel recommendations and assistance to users. This guide will help you set up the AI chatbot feature.

## Prerequisites
1. OpenRouter API Key (Get it from https://openrouter.ai/)
2. MongoDB URI configured
3. Java 21 installed
4. Maven installed

## Setup Instructions

### 1. Get OpenRouter API Key
1. Visit https://openrouter.ai/
2. Sign up for a free account
3. Navigate to API Keys section
4. Create a new API key
5. Copy the API key (it starts with `sk-or-v1-...`)

### 2. Configure Environment Variables

#### For IntelliJ IDEA:
1. Open Run/Debug Configurations
2. Select your Spring Boot application configuration
3. Add the following environment variables:
   ```
   OPENROUTER_API_KEY=your_api_key_here
   MONGODB_URI=your_mongodb_uri_here
   ```

#### For Command Line:
```bash
export OPENROUTER_API_KEY=your_api_key_here
export MONGODB_URI=your_mongodb_uri_here
```

#### For Windows PowerShell:
```powershell
$env:OPENROUTER_API_KEY="your_api_key_here"
$env:MONGODB_URI="your_mongodb_uri_here"
```

### 3. Available Models
The AI service uses these free models from OpenRouter:
- `mistralai/mistral-7b-instruct:free` (Default)
- `microsoft/phi-3-mini-128k-instruct:free`
- `meta-llama/llama-3.2-3b-instruct:free`
- `google/gemma-2-2b-it:free`

### 4. Features
The AI chatbot provides:
- ✅ Travel recommendations based on preferences
- ✅ Itinerary suggestions
- ✅ Travel buddy matching advice
- ✅ Food recommendations
- ✅ Budget travel tips
- ✅ Cultural insights
- ✅ Weather information
- ✅ Safety tips
- ✅ Transportation information
- ✅ Accommodation recommendations
- ✅ Comprehensive destination guides

### 5. API Endpoints

#### Chat Endpoint
```
POST /api/ai/chat
Content-Type: application/json

{
  "message": "What are the best places to visit in Bangladesh?",
  "model": "mistralai/mistral-7b-instruct:free"  // Optional
}
```

#### Travel Recommendations
```
POST /api/ai/recommendations
Content-Type: application/json

{
  "destination": "Cox's Bazar",
  "budget": 500,
  "duration": 3,
  "interests": ["beach", "photography", "food"],
  "travelStyle": "adventure",
  "ageGroup": "25-35"
}
```

#### Itinerary Suggestions
```
GET /api/ai/itinerary?destination=Dhaka&duration=2&interests=culture,history,food
```

### 6. Frontend Integration
The frontend AI chat component is available at:
```
http://localhost:4200/ai-chat
```

Features:
- Real-time chat interface
- Model selection (choose between different AI models)
- Quick action buttons for common queries
- Message history saved in browser localStorage
- Markdown formatting support

### 7. Testing

#### Test Backend API:
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Tell me about Cox'\''s Bazar"}'
```

#### Health Check:
```bash
curl http://localhost:8080/api/ai/health
```

### 8. Production Deployment

#### Railway.app / Similar Platforms:
Add environment variables in the platform's dashboard:
```
OPENROUTER_API_KEY=your_api_key_here
MONGODB_URI=your_mongodb_uri_here
```

#### Docker:
```dockerfile
ENV OPENROUTER_API_KEY=your_api_key_here
ENV MONGODB_URI=your_mongodb_uri_here
```

### 9. Caching
The AI service uses Spring Cache to cache responses and reduce API calls:
- Travel recommendations cached by preferences
- Itinerary suggestions cached by destination + duration + interests
- Cultural insights, weather info, safety tips cached by destination
- Cache duration: 1 hour (configurable via `app.ai.openrouter.cache-ttl`)

### 10. Rate Limiting & Retry Logic
- Automatic retry on network errors (3 attempts with exponential backoff)
- Timeout: 60 seconds per request
- Error handling with user-friendly messages

### 11. Troubleshooting

#### Issue: "AI service is not configured"
**Solution:** Ensure `OPENROUTER_API_KEY` environment variable is set correctly.

#### Issue: "AI service is currently unavailable"
**Solution:** 
- Check your internet connection
- Verify OpenRouter API key is valid
- Check if OpenRouter service is operational: https://status.openrouter.ai/

#### Issue: Backend not responding
**Solution:**
- Ensure backend is running on port 8080
- Check MongoDB connection is successful
- Review backend logs for errors

#### Issue: CORS errors in frontend
**Solution:**
- Verify proxy configuration in `proxy.conf.json`
- Ensure backend CORS is configured to allow frontend origin

### 12. Cost Management
- All models used are **FREE** tier models from OpenRouter
- No credit card required for basic usage
- Monitor usage at: https://openrouter.ai/usage
- Consider implementing additional rate limiting for production

### 13. Security Best Practices
✅ **Implemented:**
- API key stored as environment variable (not in code)
- Backend handles all API calls (frontend never exposes API key)
- HTTPS required for production
- Input sanitization and validation

⚠️ **Recommendations:**
- Rotate API keys regularly
- Monitor API usage for anomalies
- Implement user-based rate limiting in production
- Use secrets management service in production

## Support
For issues or questions:
- Check logs in `logs/travner.log`
- Review OpenRouter documentation: https://openrouter.ai/docs
- Check Travner project documentation

## License
This AI chatbot integration is part of the Travner project.



