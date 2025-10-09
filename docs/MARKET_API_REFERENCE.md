# Market API Documentation

Complete API reference for the Marketplace functionality in Travner.

## Base URL

- Local: `http://localhost:8080`

## Authentication

Market endpoints are **PUBLIC** - no authentication required for browsing products.

---

## API Endpoints

### 1. Get All Products

Retrieve all available products with pagination and sorting.

**Endpoint:** `GET /market/products`  
**Authentication:** None (Public)  
**Query Parameters:**

- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 10
- `sortBy` (optional): Sort field, default "createdAt"
- `direction` (optional): Sort direction "asc" or "desc", default "desc"

#### Example Request

```bash
curl -X GET "http://localhost:8081/market/products?page=0&size=10&sortBy=price&direction=asc"
```

#### Response

```json
{
  "success": true,
  "message": "Products retrieved successfully",
  "data": [
    {
      "id": "prod123",
      "name": "Vintage Travel Backpack",
      "description": "Durable canvas backpack perfect for adventures",
      "price": 89.99,
      "category": "gear",
      "images": ["https://example.com/image1.jpg"],
      "sellerId": "seller123",
      "sellerUsername": "TravelGearShop",
      "stockQuantity": 15,
      "isAvailable": true,
      "location": "Nepal",
      "tags": ["hiking", "outdoor", "travel"],
      "createdAt": "2025-10-08T10:30:00",
      "updatedAt": "2025-10-08T10:30:00",
      "views": 245,
      "rating": 4.5,
      "reviewCount": 12
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15,
    "first": true,
    "last": false
  }
}
```

---

### 2. Get Product by ID

Retrieve a specific product by its ID.

**Endpoint:** `GET /market/products/{productId}`  
**Authentication:** None (Public)

#### Example Request

```bash
curl -X GET "http://localhost:8081/market/products/prod123"
```

---

### 3. Search Products

Search products by name or description.

**Endpoint:** `GET /market/products/search`  
**Authentication:** None (Public)  
**Query Parameters:**

- `query` (required): Search term
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 10

#### Example Request

```bash
curl -X GET "http://localhost:8081/market/products/search?query=backpack&page=0&size=10"
```

---

### 4. Get Products by Category

Filter products by category.

**Endpoint:** `GET /market/products/category`  
**Authentication:** None (Public)  
**Query Parameters:**

- `category` (required): Product category
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 10

#### Example Request

```bash
curl -X GET "http://localhost:8081/market/products/category?category=gear&page=0&size=10"
```

---

### 5. Get Products by Location

Filter products by location.

**Endpoint:** `GET /market/products/location`  
**Authentication:** None (Public)  
**Query Parameters:**

- `location` (required): Location name (partial match)
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 10

#### Example Request

```bash
curl -X GET "http://localhost:8081/market/products/location?location=Nepal&page=0&size=10"
```

---

### 6. Get Products by Tags

Filter products by tags.

**Endpoint:** `GET /market/products/tags`  
**Authentication:** None (Public)  
**Query Parameters:**

- `tags` (required): Comma-separated list of tags
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 10

#### Example Request

```bash
curl -X GET "http://localhost:8081/market/products/tags?tags=hiking,outdoor&page=0&size=10"
```

---

### 7. Get Products by Seller

Get all products from a specific seller.

**Endpoint:** `GET /market/products/seller/{sellerId}`  
**Authentication:** None (Public)  
**Query Parameters:**

- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 10

#### Example Request

```bash
curl -X GET "http://localhost:8081/market/products/seller/seller123?page=0&size=10"
```

---

## Product Data Model

### ProductDTO Structure

```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "price": "number (BigDecimal)",
  "category": "string",
  "images": ["string array"],
  "sellerId": "string",
  "sellerUsername": "string",
  "stockQuantity": "integer",
  "isAvailable": "boolean",
  "location": "string",
  "tags": ["string array"],
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "views": "integer",
  "rating": "number (Double)",
  "reviewCount": "integer"
}
```

---

## Error Responses

### Product Not Found (404)

```json
{
  "success": false,
  "message": "Product not found with ID: prod123",
  "data": null
}
```

### Server Error (500)

```json
{
  "success": false,
  "message": "Failed to retrieve products: Internal error",
  "data": null
}
```

---

## Features

### Public Access

- All market endpoints are publicly accessible
- No authentication required for product browsing
- Supports guest shopping experience

### Search & Filtering

- Full-text search across product names and descriptions
- Category-based filtering
- Location-based filtering
- Tag-based filtering
- Seller-based product listing

### Pagination

- Consistent pagination across all list endpoints
- Configurable page size
- Total count and page information included

### Sorting

- Sort by creation date, price, popularity
- Ascending/descending order support
- Default sorting by newest products

---

## Status: ✅ Implementation Verified

The market API implementation is **correct** and includes:

✅ **Public Access**: No authentication required for browsing  
✅ **Comprehensive Search**: Multiple search and filter options  
✅ **Pagination**: Consistent pagination across all endpoints  
✅ **Sorting**: Flexible sorting options  
✅ **RESTful Design**: Standard HTTP methods and resource-based URLs  
✅ **Consistent Response Format**: Follows the established ApiResponse pattern  
✅ **Error Handling**: Proper HTTP status codes and error messages  
✅ **Performance**: Efficient MongoDB queries with indexing

The market API provides a complete product browsing experience!
