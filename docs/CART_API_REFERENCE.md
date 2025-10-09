# Cart API Documentation

Complete API reference for the Shopping Cart functionality in Travner.

## Base URL

- Development: `http://localhost:8081` (Note: Updated port)
- Local: `http://localhost:8080`

## Authentication

All cart endpoints require Basic Authentication:

```
Authorization: Basic <base64-encoded-credentials>
```

---

## API Endpoints

### 1. Get User's Cart

Retrieve the current user's shopping cart with all items.

**Endpoint:** `GET /cart`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Response

```json
{
  "success": true,
  "message": "Cart retrieved successfully",
  "data": {
    "id": "cart123",
    "userId": "user456",
    "items": [
      {
        "productId": "prod789",
        "productName": "Vintage Travel Backpack",
        "unitPrice": 89.99,
        "quantity": 2,
        "subtotal": 179.98,
        "sellerId": "seller123",
        "sellerName": "TravelGearShop",
        "productImage": "https://example.com/image.jpg",
        "addedAt": "2025-10-08T10:30:00"
      }
    ],
    "totalAmount": 179.98,
    "totalItems": 2,
    "createdAt": "2025-10-08T10:30:00",
    "updatedAt": "2025-10-08T10:35:00"
  }
}
```

#### Example Request

```bash
curl -X GET "http://localhost:8081/cart" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json"
```

---

### 2. Add Item to Cart

Add a product to the user's shopping cart.

**Endpoint:** `POST /cart/add`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Request Body

```json
{
  "productId": "prod789",
  "quantity": 2
}
```

#### Validation Rules

- `productId`: Required, non-blank string
- `quantity`: Required, minimum value 1

#### Response

Returns the updated cart (same format as GET /cart)

#### Example Request

```bash
curl -X POST "http://localhost:8081/cart/add" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod789",
    "quantity": 2
  }'
```

#### Error Cases

- `400 Bad Request`: Invalid product ID, insufficient stock, product not available
- `401 Unauthorized`: Authentication required
- `404 Not Found`: Product not found

---

### 3. Update Cart Item Quantity

Update the quantity of an existing item in the cart.

**Endpoint:** `PUT /cart/update`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Request Body

```json
{
  "productId": "prod789",
  "quantity": 3
}
```

#### Validation Rules

- `productId`: Required, non-blank string
- `quantity`: Required, minimum value 0 (0 removes the item)

#### Response

Returns the updated cart (same format as GET /cart)

#### Example Request

```bash
curl -X PUT "http://localhost:8081/cart/update" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod789",
    "quantity": 3
  }'
```

#### Special Behavior

- Setting quantity to 0 removes the item from cart
- Quantity validation ensures no negative values

---

### 4. Remove Item from Cart

Remove a specific product from the cart.

**Endpoint:** `DELETE /cart/remove/{productId}`  
**Authentication:** Required

#### Path Parameters

- `productId`: The ID of the product to remove

#### Response

Returns the updated cart (same format as GET /cart)

#### Example Request

```bash
curl -X DELETE "http://localhost:8081/cart/remove/prod789" \
  -H "Authorization: Basic dGVzdDp0ZXN0"
```

---

### 5. Clear Cart

Remove all items from the user's cart.

**Endpoint:** `DELETE /cart/clear`  
**Authentication:** Required

#### Response

Returns the empty cart

#### Example Request

```bash
curl -X DELETE "http://localhost:8081/cart/clear" \
  -H "Authorization: Basic dGVzdDp0ZXN0"
```

---

### 6. Get Cart Item Count

Get the total number of items in the user's cart.

**Endpoint:** `GET /cart/count`  
**Authentication:** Required

#### Response

```json
{
  "success": true,
  "message": "Cart item count retrieved successfully",
  "data": 5
}
```

#### Example Request

```bash
curl -X GET "http://localhost:8081/cart/count" \
  -H "Authorization: Basic dGVzdDp0ZXN0"
```

---

## Error Responses

### Authentication Error (401)

```json
{
  "success": false,
  "message": "Authentication required",
  "data": null
}
```

### Validation Error (400)

```json
{
  "success": false,
  "message": "Product not found: prod789",
  "data": null
}
```

### Server Error (500)

```json
{
  "success": false,
  "message": "Failed to retrieve cart: Internal error",
  "data": null
}
```

---

## Business Logic

### Stock Validation

- When adding items, the system checks if sufficient stock is available
- Products must be marked as available to be added to cart
- Stock quantities are validated against the requested quantity

### Cart Behavior

- Each user has one cart that persists across sessions
- Cart automatically calculates subtotals and total amounts
- Items are identified by product ID - adding same product updates quantity
- Empty carts are automatically created when needed

### Price Calculation

- Unit prices are fetched from current product data
- Subtotals are calculated as: `unitPrice × quantity`
- Total amount is sum of all item subtotals
- Prices are stored as BigDecimal for precision

---

## Integration with Market API

The cart API integrates with the Market API for:

- Product validation and availability checks
- Fetching current product information (name, price, seller details)
- Stock quantity verification
- Product image and metadata retrieval

---

## Status: ✅ Implementation Verified

The cart API implementation is **correct** and includes:

✅ **Proper Authentication**: All endpoints require user authentication  
✅ **Input Validation**: Jakarta validation on request DTOs  
✅ **Error Handling**: Comprehensive error responses with proper HTTP status codes  
✅ **Business Logic**: Stock validation, price calculation, cart persistence  
✅ **RESTful Design**: Standard HTTP methods and resource-based URLs  
✅ **Consistent Response Format**: Follows the established ApiResponse pattern  
✅ **Security Configuration**: Endpoints properly configured in Spring Security  
✅ **Database Integration**: MongoDB persistence with proper repository pattern  
✅ **Logging**: Comprehensive error logging for debugging

The cart API is production-ready and follows Spring Boot best practices!
