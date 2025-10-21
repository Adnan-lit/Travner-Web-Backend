# PowerShell script to create community posts through the API
# Make sure the backend is running on localhost:8080

Write-Host "Creating community posts through API..." -ForegroundColor Green

# Function to create a post
function Create-Post {
    param(
        [string]$Title,
        [string]$Content,
        [string]$Location,
        [array]$Tags,
        [bool]$Published
    )
    
    $body = @{
        title = $Title
        content = $Content
        location = $Location
        tags = $Tags
        published = $Published
    } | ConvertTo-Json -Depth 3
    
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/posts" -Method POST -Body $body -ContentType "application/json"
        Write-Host "✓ Post created: $Title" -ForegroundColor Green
        return $response
    }
    catch {
        Write-Host "✗ Failed to create post: $Title" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Create posts
Write-Host "`nCreating community posts..." -ForegroundColor Yellow

Create-Post -Title "Amazing Sunset in Santorini" -Content "Just witnessed the most breathtaking sunset in Santorini! The colors were absolutely incredible. This place truly lives up to its reputation as one of the most beautiful islands in Greece. The white buildings against the orange sky created a perfect contrast. Highly recommend visiting during the golden hour!" -Location "Santorini, Greece" -Tags @("sunset", "greece", "santorini", "photography", "travel") -Published $true

Create-Post -Title "Hidden Gems in Tokyo" -Content "Found some incredible hidden spots in Tokyo that most tourists never discover! From tiny ramen shops in narrow alleys to secret gardens in the middle of the city. The local culture here is so rich and diverse. Can't wait to share more about my discoveries!" -Location "Tokyo, Japan" -Tags @("japan", "tokyo", "hidden-gems", "culture", "food") -Published $true

Create-Post -Title "Budget Travel Tips for Southeast Asia" -Content "Just completed a 3-month backpacking trip through Southeast Asia on a tight budget! Here are my top tips: 1) Stay in hostels and guesthouses, 2) Eat street food (it's amazing and cheap!), 3) Use local transportation, 4) Travel during off-season. Managed to spend only 30 dollars per day including accommodation and food!" -Location "Southeast Asia" -Tags @("budget-travel", "backpacking", "southeast-asia", "tips", "adventure") -Published $true

Create-Post -Title "Luxury Resort Experience in Maldives" -Content "Just spent a week at an overwater villa in the Maldives and it was absolutely magical! The crystal clear waters, pristine beaches, and world-class service made this a once-in-a-lifetime experience. The marine life was incredible - saw dolphins, manta rays, and even a whale shark!" -Location "Maldives" -Tags @("luxury", "maldives", "resort", "marine-life", "paradise") -Published $true

Create-Post -Title "Street Food Adventure in Bangkok" -Content "Embarked on a culinary journey through Bangkok's street food scene! From pad thai to mango sticky rice, every dish was a flavor explosion. The night markets are absolutely incredible - so much variety and everything is so affordable. My favorite was the grilled squid with spicy sauce!" -Location "Bangkok, Thailand" -Tags @("food", "bangkok", "street-food", "culinary", "thailand") -Published $true

Create-Post -Title "Hiking the Inca Trail to Machu Picchu" -Content "Just completed the 4-day Inca Trail trek to Machu Picchu and it was absolutely incredible! The ancient ruins, stunning mountain views, and sense of accomplishment made this one of the most rewarding experiences of my life. The altitude was challenging but totally worth it for the sunrise at the Sun Gate!" -Location "Cusco, Peru" -Tags @("hiking", "machu-picchu", "peru", "adventure", "ancient-ruins") -Published $true

Create-Post -Title "Northern Lights in Iceland" -Content "Finally saw the Northern Lights in Iceland and it was absolutely magical! The green and purple lights dancing across the sky were like nothing I've ever seen. We drove to a remote location away from city lights and waited for hours, but it was completely worth it. Nature's most spectacular light show!" -Location "Reykjavik, Iceland" -Tags @("northern-lights", "iceland", "aurora", "nature", "photography") -Published $true

Create-Post -Title "Safari Adventure in Kenya" -Content "Just returned from an incredible safari in Kenya's Masai Mara! Saw the Big Five - lions, elephants, buffalo, leopards, and rhinos. The Great Migration was happening and we witnessed thousands of wildebeest crossing the Mara River. The local Maasai guides were amazing and taught us so much about the wildlife and culture." -Location "Masai Mara, Kenya" -Tags @("safari", "kenya", "wildlife", "big-five", "africa") -Published $true

Write-Host "`nAll community posts created successfully!" -ForegroundColor Green
Write-Host "You can now check the posts at: http://localhost:8080/api/posts" -ForegroundColor Cyan