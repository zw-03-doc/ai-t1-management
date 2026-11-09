package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.*;
import java.util.List;

@InterceptorIgnore(tenantLine = "true")
public interface FitnessVenueMapper {
    @Select("SELECT version FROM fit_venue_gallery WHERE tenant_id=#{tenant}")
    int version(String tenant);

    @Update("UPDATE fit_venue_gallery SET version=version+1 WHERE tenant_id=#{tenant} AND version=#{version}")
    int claim(@Param("tenant") String tenant, @Param("version") int version);

    @Select("SELECT photo_id FROM fit_venue_photo WHERE tenant_id=#{tenant} ORDER BY sort_order,photo_id")
    List<String> ids(String tenant);

    record StoredImage(byte[] jpeg) {}

    @Select("SELECT image_jpeg AS jpeg FROM fit_venue_photo WHERE tenant_id=#{tenant} AND photo_id=#{id}")
    StoredImage image(@Param("tenant") String tenant, @Param("id") String id);

    @Insert("INSERT INTO fit_venue_photo(tenant_id,photo_id,sort_order,image_jpeg) VALUES (#{tenant},#{id},#{position},#{image})")
    void insert(@Param("tenant") String tenant, @Param("id") String id, @Param("position") int position, @Param("image") byte[] image);

    @Update("UPDATE fit_venue_photo SET photo_id=#{newId}, image_jpeg=#{image} WHERE tenant_id=#{tenant} AND photo_id=#{id}")
    int replace(@Param("tenant") String tenant, @Param("id") String id, @Param("newId") String newId, @Param("image") byte[] image);

    @Select("SELECT COALESCE(MAX(sort_order),0)+1 FROM fit_venue_photo WHERE tenant_id=#{tenant}")
    int nextPosition(String tenant);

    @Update("UPDATE fit_venue_photo SET sort_order=#{position} WHERE tenant_id=#{tenant} AND photo_id=#{id}")
    void move(@Param("tenant") String tenant, @Param("id") String id, @Param("position") int position);

    @Delete("DELETE FROM fit_venue_photo WHERE tenant_id=#{tenant} AND photo_id=#{id}")
    int remove(@Param("tenant") String tenant, @Param("id") String id);
}
