# Travner Backend Startup Script
# This script sets required environment variables and starts the backend server

Write-Host "🚀 Travner Backend Startup Script" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# Check if MONGODB_URI is set
if (-not $env:MONGODB_URI) {
    Write-Host "❌ ERROR: MONGODB_URI environment variable is not set!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please set MongoDB URI first:" -ForegroundColor Yellow
    Write-Host '  $env:MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/"' -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Or edit this script and add your MongoDB URI in the section below." -ForegroundColor Yellow
    Write-Host ""
    
    # Uncomment and add your MongoDB URI here:
    # $env:MONGODB_URI="mongodb+srv://username:password@cluster0.5os88dy.mongodb.net/?retryWrites=true&w=majority"
    
    if (-not $env:MONGODB_URI) {
        Write-Host "Press any key to exit..."
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit 1
    }
}

Write-Host "✅ MongoDB URI is set" -ForegroundColor Green

# Set default environment variables
if (-not $env:MONGODB_DATABASE) {
    $env:MONGODB_DATABASE = "TravnerDB"
    Write-Host "✅ Set MONGODB_DATABASE=TravnerDB" -ForegroundColor Green
}

if (-not $env:PORT) {
    $env:PORT = "8080"
    Write-Host "✅ Set PORT=8080" -ForegroundColor Green
}

# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
Write-Host "✅ Set JAVA_HOME" -ForegroundColor Green

Write-Host ""
Write-Host "🔧 Starting backend server..." -ForegroundColor Cyan
Write-Host "   MongoDB: $env:MONGODB_DATABASE" -ForegroundColor Gray
Write-Host "   Port: $env:PORT" -ForegroundColor Gray
Write-Host ""

# Change to backend directory
Set-Location "D:\Travner V2\Travner-Web-Backend"

# Start the server
& .\mvnw.cmd spring-boot:run




