package org.adnan.travner.entry;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class ProductEntry {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("price")
    private BigDecimal price;

    @Field("category")
    private String category;

    @Field("images")
    private List<String> images;

    @Field("seller_id")
    private String sellerId;

    @Field("seller_username")
    private String sellerUsername;

    @Field("stock_quantity")
    private Integer stockQuantity;

    @Field("is_available")
    private Boolean isAvailable = true;

    @Field("location")
    private String location;

    @Field("tags")
    private List<String> tags;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("views")
    private Integer views = 0;

    @Field("rating")
    private Double rating = 0.0;

    @Field("review_count")
    private Integer reviewCount = 0;
}