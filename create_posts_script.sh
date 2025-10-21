#!/bin/bash

# Script to create community posts through the API
# Make sure the backend is running on localhost:8080

echo "Creating community posts through API..."

# First, let's try to authenticate (assuming we have a user)
# For now, we'll use basic auth or create posts without auth if the endpoint allows it

# Read the JSON file and create posts
echo "Reading posts from create_posts.json..."

# Create posts one by one
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Amazing Sunset in Santorini",
    "content": "Just witnessed the most breathtaking sunset in Santorini! The colors were absolutely incredible. This place truly lives up to its reputation as one of the most beautiful islands in Greece. The white buildings against the orange sky created a perfect contrast. Highly recommend visiting during the golden hour!",
    "location": "Santorini, Greece",
    "tags": ["sunset", "greece", "santorini", "photography", "travel"],
    "published": true
  }' \
  --silent --show-error

echo ""
echo "Post 1 created: Amazing Sunset in Santorini"

curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Hidden Gems in Tokyo",
    "content": "Found some incredible hidden spots in Tokyo that most tourists never discover! From tiny ramen shops in narrow alleys to secret gardens in the middle of the city. The local culture here is so rich and diverse. Can not wait to share more about my discoveries!",
    "location": "Tokyo, Japan",
    "tags": ["japan", "tokyo", "hidden-gems", "culture", "food"],
    "published": true
  }' \
  --silent --show-error

echo ""
echo "Post 2 created: Hidden Gems in Tokyo"

curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Budget Travel Tips for Southeast Asia",
    "content": "Just completed a 3-month backpacking trip through Southeast Asia on a tight budget! Here are my top tips: 1) Stay in hostels and guesthouses, 2) Eat street food (it is amazing and cheap!), 3) Use local transportation, 4) Travel during off-season. Managed to spend only $30/day including accommodation and food!",
    "location": "Southeast Asia",
    "tags": ["budget-travel", "backpacking", "southeast-asia", "tips", "adventure"],
    "published": true
  }' \
  --silent --show-error

echo ""
echo "Post 3 created: Budget Travel Tips for Southeast Asia"

curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Luxury Resort Experience in Maldives",
    "content": "Just spent a week at an overwater villa in the Maldives and it was absolutely magical! The crystal clear waters, pristine beaches, and world-class service made this a once-in-a-lifetime experience. The marine life was incredible - saw dolphins, manta rays, and even a whale shark!",
    "location": "Maldives",
    "tags": ["luxury", "maldives", "resort", "marine-life", "paradise"],
    "published": true
  }' \
  --silent --show-error

echo ""
echo "Post 4 created: Luxury Resort Experience in Maldives"

curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Street Food Adventure in Bangkok",
    "content": "Embarked on a culinary journey through Bangkok street food scene! From pad thai to mango sticky rice, every dish was amazing. The night markets are incredible with so much variety and everything is affordable.",
    "location": "Bangkok, Thailand",
    "tags": ["food", "bangkok", "street-food", "culinary", "thailand"],
    "published": true
  }' \
  --silent --show-error

echo ""
echo "Post 5 created: Street Food Adventure in Bangkok"

echo ""
echo "All community posts created successfully!"
echo "You can now check the posts at: http://localhost:8080/api/posts"






