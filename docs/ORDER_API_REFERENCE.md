# Order API Documentation

Complete API reference for Order Management functionality in Travner.

## Base URL

- Development: `http://localhost:8081`
- Local: `http://localhost:8080`

## Authentication

All order endpoints require Basic Authentication:

```
Authorization: Basic <base64-encoded-credentials>
```

---

## API Endpoints

### 1. Create Order (Checkout)

Create an order from the user's cart with shipping and payment details.

**Endpoint:** `POST /api/orders`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Request Body

```json
{
  "fullName": "John Doe",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apt 4B",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "country": "USA",
  "phoneNumber": "+1-555-0123",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Please deliver before 5 PM"
}
```

#### Validation Rules

- `fullName`: Required, non-blank
- `addressLine1`: Required, non-blank
- `city`: Required, non-blank
- `state`: Required, non-blank
- `zipCode`: Required, non-blank
- `country`: Required, non-blank
- `phoneNumber`: Required, must match pattern `^[0-9+\\-\\s()]+$`
- `paymentMethod`: Required (CREDIT_CARD, DEBIT_CARD, PAYPAL, COD)
- `notes`: Optional

#### Response (201 Created)

```json
{
  "success": true,
  "message": "Order created successfully",
  "data": {
    "id": "order123",
    "orderNumber": "ORD-20251013143022-A1B2C3D4",
    "userId": "testuser",
    "userEmail": "test@example.com",
    "items": [
      {
        "productId": "prod789",
        "productName": "Vintage Travel Backpack",
        "unitPrice": 89.99,
        "quantity": 2,
        "subtotal": 179.98,
        "sellerId": "seller123",
        "sellerName": "TravelGearShop",
        "productImage": "https://example.com/image.jpg"
      }
    ],
    "totalAmount": 179.98,
    "totalItems": 2,
    "status": "PENDING",
    "shippingAddress": {
      "fullName": "John Doe",
      "addressLine1": "123 Main Street",
      "addressLine2": "Apt 4B",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA",
      "phoneNumber": "+1-555-0123"
    },
    "paymentInfo": {
      "paymentMethod": "CREDIT_CARD",
      "transactionId": null,
      "paymentStatus": "PENDING",
      "paidAt": null
    },
    "orderedAt": "2025-10-13T14:30:22",
    "updatedAt": "2025-10-13T14:30:22",
    "deliveredAt": null,
    "notes": "Please deliver before 5 PM"
  },
  "pagination": null
}
```

#### Order Status Values

- `PENDING`: Order placed, awaiting confirmation
- `CONFIRMED`: Order confirmed by seller
- `PROCESSING`: Order is being prepared
- `SHIPPED`: Order has been shipped
- `DELIVERED`: Order delivered to customer
- `CANCELLED`: Order cancelled
- `REFUNDED`: Order refunded

#### Payment Status Values

- `PENDING`: Payment not yet processed
- `COMPLETED`: Payment successful
- `FAILED`: Payment failed
- `REFUNDED`: Payment refunded

#### Example Request

```bash
curl -X POST "http://localhost:8081/api/orders" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA",
    "phoneNumber": "+1-555-0123",
    "paymentMethod": "CREDIT_CARD",
    "notes": "Please deliver before 5 PM"
  }'
```

#### Error Cases

- `400 Bad Request`: Validation failed, cart is empty
- `401 Unauthorized`: Authentication required
- `500 Internal Server Error`: Failed to create order

---

### 2. Get All Orders

Retrieve all orders for the authenticated user, sorted by order date (newest first).

**Endpoint:** `GET /api/orders`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Response

```json
{
  "success": true,
  "message": "Orders retrieved successfully",
  "data": [
    {
      "id": "order123",
      "orderNumber": "ORD-20251013143022-A1B2C3D4",
      "userId": "testuser",
      "userEmail": "test@example.com",
      "items": [
        {
          "productId": "prod789",
          "productName": "Vintage Travel Backpack",
          "unitPrice": 89.99,
          "quantity": 2,
          "subtotal": 179.98,
          "sellerId": "seller123",
          "sellerName": "TravelGearShop",
          "productImage": "https://example.com/image.jpg"
        }
      ],
      "totalAmount": 179.98,
      "totalItems": 2,
      "status": "SHIPPED",
      "shippingAddress": { ... },
      "paymentInfo": { ... },
      "orderedAt": "2025-10-13T14:30:22",
      "updatedAt": "2025-10-13T15:00:00",
      "deliveredAt": null,
      "notes": "Please deliver before 5 PM"
    }
  ],
  "pagination": null
}
```

#### Example Request

```bash
curl -X GET "http://localhost:8081/api/orders" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json"
```

---

### 3. Get Order by ID

Retrieve a specific order by its ID.

**Endpoint:** `GET /api/orders/{orderId}`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Path Parameters

- `orderId`: The ID of the order to retrieve

#### Response

Returns a single order object (same format as in the list response).

#### Example Request

```bash
curl -X GET "http://localhost:8081/api/orders/order123" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json"
```

#### Error Cases

- `401 Unauthorized`: Authentication required
- `404 Not Found`: Order not found or doesn't belong to user
- `500 Internal Server Error`: Failed to retrieve order

---

### 4. Get Order by Order Number

Retrieve a specific order by its order number (e.g., ORD-20251013143022-A1B2C3D4).

**Endpoint:** `GET /api/orders/number/{orderNumber}`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Path Parameters

- `orderNumber`: The order number to retrieve (e.g., ORD-20251013143022-A1B2C3D4)

#### Response

Returns a single order object (same format as in the list response).

#### Example Request

```bash
curl -X GET "http://localhost:8081/api/orders/number/ORD-20251013143022-A1B2C3D4" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json"
```

#### Error Cases

- `401 Unauthorized`: Authentication required
- `404 Not Found`: Order not found or doesn't belong to user
- `500 Internal Server Error`: Failed to retrieve order

---

### 5. Cancel Order

Cancel a pending or confirmed order.

**Endpoint:** `POST /api/orders/{orderId}/cancel`  
**Authentication:** Required  
**Content-Type:** `application/json`

#### Path Parameters

- `orderId`: The ID of the order to cancel

#### Response

Returns the updated order with status changed to `CANCELLED`.

#### Example Request

```bash
curl -X POST "http://localhost:8081/api/orders/order123/cancel" \
  -H "Authorization: Basic dGVzdDp0ZXN0" \
  -H "Content-Type: application/json"
```

#### Cancellation Rules

- Only orders with status `PENDING` or `CONFIRMED` can be cancelled
- Orders that are `PROCESSING`, `SHIPPED`, or `DELIVERED` cannot be cancelled
- Already cancelled orders cannot be cancelled again

#### Error Cases

- `400 Bad Request`: Order cannot be cancelled (wrong status)
- `401 Unauthorized`: Authentication required
- `404 Not Found`: Order not found
- `500 Internal Server Error`: Failed to cancel order

---

## Customer Use Cases

### 1. View Order History

**Customer wants to see all their past orders:**

```bash
GET /api/orders
```

This returns all orders sorted by date, showing order number, status, total amount, and order date.

---

### 2. Track Order Status

**Customer wants to check the status of a specific order:**

```bash
GET /api/orders/{orderId}
```

or

```bash
GET /api/orders/number/{orderNumber}
```

Both return complete order details including:
- Current status (PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED)
- Shipping address
- Payment status
- Order items
- Timestamps for when order was placed and last updated

---

### 3. View Order Details

**Customer wants to see full details of an order (items, shipping, payment):**

```bash
GET /api/orders/{orderId}
```

Returns:
- All items in the order with prices and quantities
- Shipping address
- Payment method and status
- Order total
- Order and delivery timestamps

---

### 4. Cancel Order

**Customer wants to cancel a recent order:**

```bash
POST /api/orders/{orderId}/cancel
```

This cancels the order if it's still in `PENDING` or `CONFIRMED` status.

---

## Integration with Cart

The order creation process integrates with the cart system:

1. Customer adds items to cart using cart APIs
2. Customer proceeds to checkout and provides shipping/payment details
3. System creates order from cart items
4. Cart is automatically cleared after successful order creation
5. Customer receives order number for tracking

---

## Complete Customer Order Flow

1. **Browse & Add to Cart**
   - `POST /api/cart/items` - Add products to cart
   - `GET /api/cart` - View cart

2. **Checkout**
   - `POST /api/orders` - Create order with shipping/payment info
   - Cart is cleared automatically
   - Order number is generated

3. **Track Order**
   - `GET /api/orders` - View all orders
   - `GET /api/orders/{orderId}` - View specific order details
   - `GET /api/orders/number/{orderNumber}` - Search by order number

4. **Manage Order**
   - `POST /api/orders/{orderId}/cancel` - Cancel if needed

---

## Notes

- All endpoints require authentication
- Orders can only be accessed by the user who created them
- Order numbers are unique and generated automatically in format: `ORD-{timestamp}-{random}`
- Cart is automatically cleared when an order is successfully created
- Order history is maintained indefinitely

