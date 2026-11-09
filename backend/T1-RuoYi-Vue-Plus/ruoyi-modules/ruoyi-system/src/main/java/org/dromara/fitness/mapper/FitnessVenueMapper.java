package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.*;
import java.util.List;

@InterceptorIgnore(tenantLine = "true")
public interface FitnessVenueMapper {
    record Store(Long storeId, String storeName, String status) {}

    @Select("SELECT store_id,store_name,status FROM fit_store WHERE tenant_id=#{tenant} AND del_flag='0' ORDER BY create_time,store_id")
    List<Store> stores(String tenant);

    @Select("SELECT store_id FROM fit_store_venue_gallery WHERE tenant_id=#{tenant} AND legacy_owner=1")
    Long defaultStore(String tenant);

    @Select("SELECT store_id,store_name,status FROM fit_store WHERE tenant_id=#{tenant} AND store_id=#{store} AND del_flag='0'")
    Store store(@Param("tenant") String tenant, @Param("store") long store);

    @Select("SELECT store_id,store_name,status FROM fit_store WHERE tenant_id=#{tenant} AND store_id=#{store} AND del_flag='0' FOR UPDATE")
    Store lockStore(@Param("tenant") String tenant, @Param("store") long store);

    @Insert("INSERT IGNORE INTO fit_store_venue_gallery (tenant_id,store_id,version,legacy_owner) SELECT tenant_id,#{store},version,1 FROM fit_venue_gallery WHERE tenant_id=#{tenant}")
    int adoptDefault(@Param("tenant") String tenant, @Param("store") long store);

    @Insert("INSERT INTO fit_store_venue_photo (tenant_id,store_id,photo_id,sort_order,image_jpeg) SELECT tenant_id,#{store},photo_id,sort_order,image_jpeg FROM fit_venue_photo WHERE tenant_id=#{tenant}")
    void copyLegacyPhotos(@Param("tenant") String tenant, @Param("store") long store);

    @Insert("INSERT IGNORE INTO fit_store_venue_gallery (tenant_id,store_id,version) VALUES (#{tenant},#{store},0)")
    void ensureGallery(@Param("tenant") String tenant, @Param("store") long store);

    @Select("SELECT version FROM fit_store_venue_gallery WHERE tenant_id=#{tenant} AND store_id=#{store}")
    Integer version(@Param("tenant") String tenant, @Param("store") long store);

    @Update("UPDATE fit_store_venue_gallery SET version=version+1 WHERE tenant_id=#{tenant} AND store_id=#{store} AND version=#{version}")
    int claim(@Param("tenant") String tenant, @Param("store") long store, @Param("version") int version);

    @Select("SELECT photo_id FROM fit_store_venue_photo WHERE tenant_id=#{tenant} AND store_id=#{store} ORDER BY sort_order,photo_id")
    List<String> ids(@Param("tenant") String tenant, @Param("store") long store);

    record StoredImage(byte[] jpeg) {}

    @Select("SELECT image_jpeg AS jpeg FROM fit_store_venue_photo WHERE tenant_id=#{tenant} AND store_id=#{store} AND photo_id=#{id}")
    StoredImage image(@Param("tenant") String tenant, @Param("store") long store, @Param("id") String id);

    @Insert("INSERT INTO fit_store_venue_photo(tenant_id,store_id,photo_id,sort_order,image_jpeg) VALUES (#{tenant},#{store},#{id},#{position},#{image})")
    void insert(@Param("tenant") String tenant, @Param("store") long store, @Param("id") String id, @Param("position") int position, @Param("image") byte[] image);

    @Update("UPDATE fit_store_venue_photo SET photo_id=#{newId}, image_jpeg=#{image} WHERE tenant_id=#{tenant} AND store_id=#{store} AND photo_id=#{id}")
    int replace(@Param("tenant") String tenant, @Param("store") long store, @Param("id") String id, @Param("newId") String newId, @Param("image") byte[] image);

    @Select("SELECT COALESCE(MAX(sort_order),0)+1 FROM fit_store_venue_photo WHERE tenant_id=#{tenant} AND store_id=#{store}")
    int nextPosition(@Param("tenant") String tenant, @Param("store") long store);

    @Update("UPDATE fit_store_venue_photo SET sort_order=#{position} WHERE tenant_id=#{tenant} AND store_id=#{store} AND photo_id=#{id}")
    void move(@Param("tenant") String tenant, @Param("store") long store, @Param("id") String id, @Param("position") int position);

    @Delete("DELETE FROM fit_store_venue_photo WHERE tenant_id=#{tenant} AND store_id=#{store} AND photo_id=#{id}")
    int remove(@Param("tenant") String tenant, @Param("store") long store, @Param("id") String id);
}
