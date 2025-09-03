package org.adnan.travner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Configuration for MongoDB GridFS functionality.
 * GridFS is MongoDB's specification for storing large files such as images and
 * videos.
 */
@Configuration
public class GridFsConfig {

    /**
     * Creates a GridFsTemplate bean for handling GridFS operations
     * 
     * @param mongoTemplate The MongoTemplate instance
     * @return A configured GridFsTemplate
     */
    @Bean
    public GridFsTemplate gridFsTemplate(MongoTemplate mongoTemplate) {
        return new GridFsTemplate(mongoTemplate.getMongoDatabaseFactory(),
                mongoTemplate.getConverter());
    }
}
